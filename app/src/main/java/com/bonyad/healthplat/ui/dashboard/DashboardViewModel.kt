package com.bonyad.healthplat.ui.dashboard

import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.blesdk.model.ConnectionState
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.AIAnalysisApiService
import com.bonyad.healthplat.data.network.ArrhythmiaApiService
import com.bonyad.healthplat.data.network.SignalRManager
import com.bonyad.healthplat.data.network.TokenManager
import com.bonyad.healthplat.data.repository.HealthDataRepository
import com.bonyad.healthplat.data.repository.UserRepository
import com.bonyad.healthplat.domain.model.ApiErrorType
import com.bonyad.healthplat.domain.model.RecordDataResult

import com.bonyad.healthplat.ui.utils.PermissionUtils
import com.bonyad.healthplat.ui.utils.rtl
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class SyncStatus { Idle, Syncing, Success, Failed }

data class HealthOverview(
    // Real-time data (Updates constantly)
    val heartRate: Int = 0,
    val steps: Int = 0,

    // Synced Data (Updates on sync)
    val bloodOxygen: Int = 0, // SpO2
    val stressLevel: Int = 0,
    val sleepDurationHours: Float = 0f,
    val hrv: Int = 0,

    val heartRateHistory: List<Int> = emptyList(),
    val stepsHistory: List<Int> = emptyList(),
    val sleepHistory: List<Int> = emptyList(),
    val spo2History: List<Int> = emptyList(),

    // Arrhythmia
    val arrhythmiaAfibPercent: Float = -1f, // -1 = not loaded yet

    // Device State
    val batteryLevel: Int? = null,
    val isDeviceConnected: Boolean = false
)


@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceManager: HealthDeviceManager,
    private val aiApiService: AIAnalysisApiService,
    private val arrhythmiaApiService: ArrhythmiaApiService,
    private val healthRepository: HealthDataRepository,
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferencesDataStore,
    private val tokenManager: TokenManager,
    private val srManager: SignalRManager
) : ViewModel() {

    private val _healthOverview = MutableStateFlow(HealthOverview())
    val healthOverview: StateFlow<HealthOverview> = _healthOverview.asStateFlow()

    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _needsBluetoothPermissions = MutableStateFlow(false)
    val needsBluetoothPermissions: StateFlow<Boolean> = _needsBluetoothPermissions.asStateFlow()


    // Bluetooth enable request — UI observes this to launch the system enable dialog
    private val _requestBluetoothEnable = MutableSharedFlow<Unit>(replay = 0)
    val requestBluetoothEnable: SharedFlow<Unit> = _requestBluetoothEnable.asSharedFlow()

    // Location services enable request — UI observes this to open location settings
    // Required for BLE scanning on Samsung + OEM devices
    private val _requestLocationServicesEnable = MutableSharedFlow<Unit>(replay = 0)
    val requestLocationServicesEnable: SharedFlow<Unit> = _requestLocationServicesEnable.asSharedFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // --- NEW AI STATE ---
    private val _readinessScore = MutableStateFlow(0)
    val readinessScore: StateFlow<Int> = _readinessScore.asStateFlow()

    // Each pair is (insightText, flagScore) where flagScore 0-3 drives the icon
    private val _healthInsights = MutableStateFlow(
        listOf("در حال تحلیل وضعیت شما...".rtl() to 1)
    )
    val healthInsights: StateFlow<List<Pair<String, Int>>> = _healthInsights.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncServerTime = MutableStateFlow<String?>(null)
    val lastSyncServerTime: StateFlow<String?> = _lastSyncServerTime.asStateFlow()

    private var historySyncJob: Job? = null
    private var initObserverJob: Job? = null
    private var connectingTimeoutJob: Job? = null
    private var hasCompletedInitialSync = false
    private var suppressNextAutoReconnect = false

    init {
        Timber.i("📱 DashboardViewModel initialized")

        viewModelScope.launch {
            userPreferences.getLastSyncServerTime().collect { _lastSyncServerTime.value = it }
        }

        viewModelScope.launch {
            // OPTIMIZATION 1: Run independent tasks in parallel
            val userDataJob = async { ensureUserData() }
            val loadUserJob = async { loadUserData() }

            // Start observing immediately - no delays
            observeDeviceConnection()
            observeRealTimeData()
            observeBatteryLevel()

            // Wait for user data before connecting
            userDataJob.await()

            // OPTIMIZATION 2: Reduced initial delay
            delay(200) // Minimal delay for UI setup

            checkPermissionsAndConnect()

            // These can run in parallel with device connection
            launch { fetchReadinessData() }
            launch { fetchArrhythmiaForCard() }
            launch {
                startTokenRefreshLoop()
                srManager.observeToken(viewModelScope)
                srManager.connect()
            }
        }
    }

    private suspend fun fetchReadinessData() {
        try {
            val userId = userPreferences.getUserId().first() ?: return

            val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

            Timber.d("Fetching readiness for today: $today")

            val response = aiApiService.getReadinessScore(userId, today)

            if (!response.isSuccessful) {
                fallbackGenericError()
                return
            }

            val body = response.body() ?: run {
                fallbackGenericError()
                return
            }

            if (!body.isSuccess) {
                _readinessScore.value = 0
                _healthInsights.value =
                    listOf("داده‌ای برای امروز ثبت نشده است" to 1)
                return
            }

            val data = body.data ?: run {
                _readinessScore.value = 0
                _healthInsights.value =
                    listOf("داده‌ای برای امروز ثبت نشده است" to 1)
                return
            }

            _readinessScore.value =
                data.absReadinessScore.coerceIn(0, 100)

            // Map English flag keys to Persian note keys
            val flagKeyToPersianKey = mapOf(
                "activity" to "فعالیت",
                "heart"    to "قلبی",
                "stress"   to "استرس",
                "sleep"    to "خواب"
            )

            _healthInsights.value =
                if (data.perAspectNotes.isNotEmpty()) {
                    data.perAspectNotes.entries.map { (persianKey, noteText) ->
                        val englishKey = flagKeyToPersianKey.entries
                            .firstOrNull { it.value == persianKey }?.key
                        val flagScore = englishKey?.let { data.perAspectFlags[it] } ?: 1
                        noteText to flagScore
                    }
                } else {
                    listOf("وضعیت کلی شما نرمال است" to 2)
                }

        } catch (e: Exception) {
            Timber.e(e, "Error fetching readiness")
            _readinessScore.value = 0
            _healthInsights.value =
                listOf("عدم دسترسی به سرویس" to 0)
        }
    }

    private fun fallbackGenericError() {
        _readinessScore.value = 0
        _healthInsights.value = listOf("خطا در دریافت تحلیل" to 0)
    }

    private suspend fun fetchArrhythmiaForCard() {
        try {
            val userId = userPreferences.getUserId().first() ?: return
            val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            val response = arrhythmiaApiService.predictArrhythmia(userId, today, today)
            if (!response.isSuccessful || response.body()?.isSuccess != true) return

            val dataList = response.body()?.data ?: return
            val data = dataList.firstOrNull() ?: return
            val predictions = data.predictions
            if (predictions.isEmpty()) return

            val afibCount = predictions.count { it == 1 }
            val afibPercent = (afibCount.toFloat() / predictions.size) * 100f

            _healthOverview.update { it.copy(arrhythmiaAfibPercent = afibPercent) }
            Timber.d("Arrhythmia card: ${afibPercent}% AFib")
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch arrhythmia for home card")
        }
    }

    private suspend fun checkPermissionsAndConnect() {
        // OPTIMIZATION: Reduced from 500ms
        delay(100)

        if (PermissionUtils.hasBluetoothPermissions(context)) {
            Timber.i("✅ Bluetooth permissions granted")
            _needsBluetoothPermissions.value = false
            autoConnectDevice()
        } else {
            Timber.w("⚠️ Missing Bluetooth permissions")
            _needsBluetoothPermissions.value = true
        }
    }

    fun onPermissionsGranted() {
        _needsBluetoothPermissions.value = false
        viewModelScope.launch { autoConnectDevice() }
    }

    fun onPermissionsDenied() {
        Timber.w("⚠️ User denied Bluetooth permissions")
        _needsBluetoothPermissions.value = false
    }

    private suspend fun ensureUserData() {
        try {
            _isLoading.value = true
            userRepository.ensureUserDataAvailable()
        } finally {
            _isLoading.value = false
        }
    }

    private fun observeInitializationComplete() {
        initObserverJob?.cancel()
        initObserverJob = viewModelScope.launch {
            deviceManager.initializationComplete.collect {
                Timber.i("🎯 Device initialization complete")

                // DeviceInfoBean.deviceId reflects the userId stored on the ring via setUserInfo().
                // If it's 0 the ring has never been assigned a user — set it now.
                try {
                    val ringDeviceId = deviceManager.getDeviceInfo().getOrNull()?.deviceId ?: 0
                    if (ringDeviceId == 0) {
                        val storedDeviceId = userPreferences.getDeviceId().first()
                        val userIdForRing = storedDeviceId?.toLong()?.coerceIn(1L, 4294967294L) ?: 1L
                        deviceManager.setUserId(userIdForRing)
                        Timber.i("👤 Ring had no userId — set to device ID: $userIdForRing")
                    } else {
                        Timber.i("👤 Ring already has userId=$ringDeviceId — skipping setUserId")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "⚠️ Could not read ring device info for userId check")
                }

                delay(500)

                if (!hasCompletedInitialSync) {
                    syncDeviceHistoryOptimized()
                }
            }
        }
    }

    fun syncDeviceHistoryOptimized() {
        // Cancel any existing sync job
        historySyncJob?.cancel()

        historySyncJob = viewModelScope.launch {
            if (!deviceManager.isPaired.value) {
                Timber.w("⚠️ Cannot sync: Device not paired")
                return@launch
            }

            if (deviceManager.connectionState.value != ConnectionState.CONNECTED) {
                Timber.w("⚠️ Cannot sync: Device not connected")
                return@launch
            }

            Timber.i("🔄 Starting optimized history sync (with gap detection)...")
            _syncStatus.value = SyncStatus.Syncing

            // OPTIMIZATION: Minimal delay (reduced from 2500ms)
            delay(300)

            try {
                val result = healthRepository.syncAllMissingDays()

                if (result is RecordDataResult.Success) {
                    processHistoryData(result)
                    hasCompletedInitialSync = true
                    _syncStatus.value = SyncStatus.Success
                    Timber.i("✅ History sync complete (all missing days synced)")
                } else if (result is RecordDataResult.Error) {
                    _syncStatus.value = SyncStatus.Failed
                    Timber.w("⚠️ Sync failed: ${result.message}")
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Failed
                Timber.e(e, "❌ Sync exception")
            }
        }
    }

    fun syncDeviceHistory() {
        syncDeviceHistoryOptimized()
    }

    private fun processHistoryData(data: RecordDataResult.Success) {
        _healthOverview.update { current ->
            current.copy(
                sleepDurationHours = calculateSleepHours(data.sleep?.sourceList),
                bloodOxygen = data.spo2?.sourceList?.lastOrNull { it > 0 } ?: 0,
                stressLevel = data.stress?.stressSource?.lastOrNull { it > 0 } ?: 0,
                hrv = data.hrv?.hrvSource?.lastOrNull { it > 0 } ?: 0,
                steps = data.steps?.stepSource?.sum()?.takeIf { it > current.steps } ?: current.steps,

                // Update history for charts
                heartRateHistory = data.heartRate?.heartRateSource ?: current.heartRateHistory,
                stepsHistory = data.steps?.stepSource ?: current.stepsHistory,
                sleepHistory = data.sleep?.sourceList ?: emptyList(),
                spo2History = data.spo2?.sourceList ?: emptyList()
            )
        }
    }

    private fun calculateSleepHours(sleepData: List<Int>?): Float {
        if (sleepData.isNullOrEmpty()) return 0f
        val sleepMinutes = sleepData.count { it == 1 || it == 2 || it == 4 }
        return sleepMinutes / 60f
    }

    // ══════════════════════════════════════════════════════════════════════
    // FIX: Uses reconnect(mac) instead of connect(mac).
    // reconnect() does: quick BLE scan → find device by MAC → connect
    // using BluetoothDevice object (matching demo's toReconDevice pattern).
    // ══════════════════════════════════════════════════════════════════════
    private fun autoConnectDevice() {
        viewModelScope.launch {
            val currentState = deviceManager.connectionState.value
            if (currentState == ConnectionState.CONNECTED) {
                Timber.d("🚫 Already connected")
                return@launch
            }
            if (currentState == ConnectionState.CONNECTING) {
                Timber.w("⚠️ Stuck in CONNECTING — forcing disconnect and retrying")
                deviceManager.disconnect()
                delay(500)
            }

            try {
                if (!PermissionUtils.hasBluetoothPermissions(context)) {
                    _needsBluetoothPermissions.value = true
                    return@launch
                }

                // Check Bluetooth is actually ON
                val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val bluetoothAdapter = bluetoothManager?.adapter
                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                    Timber.w("⚠️ Bluetooth is OFF, cannot auto-connect")
                    return@launch
                }

                // Check location services switch — required for BLE on Samsung + OEM devices
                if (!PermissionUtils.isLocationServicesEnabled(context)) {
                    Timber.w("⚠️ Location services are OFF, cannot auto-connect on this device")
                    return@launch
                }

                delay(200)

                val deviceMac = userPreferences.getDeviceMac().first()
                if (!deviceMac.isNullOrEmpty()) {
                    Timber.i("🔄 Auto-reconnecting to: $deviceMac")
                    deviceManager.reconnect(deviceMac)
                } else {
                    Timber.w("⚠️ No saved device MAC")
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Auto-connect error")
            }
        }
    }

    private suspend fun loadUserData() {
        userPreferences.getUserName().collect { name ->
            _userName.value = name
        }
    }

    private var autoReconnectJob: Job? = null

    private fun observeDeviceConnection() {
        viewModelScope.launch {
            deviceManager.connectionState.collect { state ->
                Timber.d("🔵 Connection state: $state")

                val isConnected = state == ConnectionState.CONNECTED
                _healthOverview.update { it.copy(isDeviceConnected = isConnected) }

                when (state) {
                    ConnectionState.CONNECTED -> {
                        _isConnecting.value = false
                        connectingTimeoutJob?.cancel()
                        suppressNextAutoReconnect = false
                        autoReconnectJob?.cancel()
                        Timber.i("✅ Device connected")
                        observeInitializationComplete()
                    }
                    ConnectionState.DISCONNECTED -> {
                        _isConnecting.value = false
                        connectingTimeoutJob?.cancel()
                        hasCompletedInitialSync = false
                        initObserverJob?.cancel()
                        _healthOverview.update { it.copy(batteryLevel = null) }

                        autoReconnectJob?.cancel()
                        if (!suppressNextAutoReconnect) {
                            autoReconnectJob = launch {
                                delay(2000)
                                autoConnectDevice()
                            }
                        }
                        suppressNextAutoReconnect = false
                    }
                    ConnectionState.CONNECTING -> {
                        _isConnecting.value = true
                        autoReconnectJob?.cancel()
                        startConnectingTimeout()
                    }
                    ConnectionState.SCANNING -> {
                        _isConnecting.value = true
                        startConnectingTimeout()
                    }
                }
            }
        }
    }

    private fun startConnectingTimeout() {
        if (connectingTimeoutJob?.isActive == true) return // countdown already running
        connectingTimeoutJob = viewModelScope.launch {
            delay(60_000)
            if (_isConnecting.value) {
                Timber.w("⏰ BLE connection timed out after 60s — aborting")
                suppressNextAutoReconnect = true
                _isConnecting.value = false
                deviceManager.disconnect()
            }
        }
    }

    private fun observeRealTimeData() {
        viewModelScope.launch {
            deviceManager.realTimeData.collect { data ->
                _healthOverview.update { overview ->
                    overview.copy(
                        heartRate = data.heart ?: overview.heartRate,
                        steps = data.step ?: overview.steps
                    )
                }

                // Send to WebSocket
                val deviceId = userPreferences.getDeviceId().first() ?: return@collect
                srManager.sendHeartRate(deviceId = deviceId, value = data.heart)

                Timber.d("💓 Real-time: HR=${data.heart}, Steps=${data.step}")
            }
        }
    }

    private suspend fun sendHeartRateWebSocket(heart: Int?) {
        val deviceId = userPreferences.getDeviceId().first() ?: return
        srManager.sendHeartRate(deviceId = deviceId, value = heart)

    }

    private fun observeBatteryLevel() {
        viewModelScope.launch {
            deviceManager.batteryLevel.collect { battery ->
                _healthOverview.update { it.copy(batteryLevel = battery) }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _syncStatus.value = SyncStatus.Syncing
            try {
                fetchReadinessData()
                if (_healthOverview.value.isDeviceConnected) {
                    deviceManager.readBatteryLevel()
                    historySyncJob?.cancel()
                    historySyncJob = launch {
                        try {
                            val result = healthRepository.syncAllMissingDays()
                            if (result is RecordDataResult.Success) {
                                processHistoryData(result)
                                _syncStatus.value = SyncStatus.Success
                            } else {
                                _syncStatus.value = SyncStatus.Failed
                            }
                        } catch (e: Exception) {
                            _syncStatus.value = SyncStatus.Failed
                            Timber.e(e, "Refresh sync failed")
                        }
                    }
                    historySyncJob?.join()
                } else {
                    _syncStatus.value = SyncStatus.Idle
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Failed
                Timber.e(e, "Refresh failed")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun connectDevice() {
        suppressNextAutoReconnect = false
        if (!PermissionUtils.hasBluetoothPermissions(context)) {
            _needsBluetoothPermissions.value = true
            return
        }

        // Check if Bluetooth is enabled
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            // Request user to enable Bluetooth
            viewModelScope.launch {
                _requestBluetoothEnable.emit(Unit)
            }
            return
        }

        if (!PermissionUtils.isLocationServicesEnabled(context)) {
            Timber.w("⚠️ Location services are OFF — BLE scan will fail on Samsung/OEM devices")
            viewModelScope.launch {
                _requestLocationServicesEnable.emit(Unit)
            }
            return
        }

        viewModelScope.launch { autoConnectDevice() }
    }

    fun onBluetoothEnabled() {
        // Called from UI after user enables Bluetooth
        viewModelScope.launch {
            delay(500)
            if (!PermissionUtils.isLocationServicesEnabled(context)) {
                Timber.w("⚠️ Bluetooth enabled but location services are OFF")
                _requestLocationServicesEnable.emit(Unit)
                return@launch
            }
            autoConnectDevice()
        }
    }

    fun disconnectDevice() {
        deviceManager.disconnect()
    }

    private fun getDeviceInfo() {
        viewModelScope.launch {
            try {
                deviceManager.getDeviceInfo().onSuccess { deviceInfo ->
                    Timber.i("📱 Device Info (Device ID): ${deviceInfo.deviceId}")
                    Timber.i("📱 Device Info (Device State): ${deviceInfo.deviceState}")

                    when (deviceInfo.deviceState) {
                        1 -> Timber.i("Invalid Message")
                        2 -> Timber.i("Battery is low")
                        3 -> Timber.i("Data channel is not open")
                        4 -> Timber.i("The device is requesting a communication interval")
                        5 -> Timber.i("Device busy")
                        6 -> Timber.i("No data")
                        else -> Timber.i("📱 Device Info (Device State): Unknown")
                    }

                    Timber.i("📱 Device Info (Power On time): ${deviceInfo.powerOnTime}")
                    Timber.i("📱 Device Info (History Day): ${deviceInfo.historyDay}")
                    Timber.i("📱 Device info (Time): ${deviceInfo.time}")
                }.onFailure {
                    Timber.e(it, "❌ Error getting device info")
                }

            } catch (e: Exception) {

            }
        }
    }

    private fun startTokenRefreshLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                try {
                    val refreshed = tokenManager.ensureValidToken()
                    if (refreshed && tokenManager.isTokenExpiringSoon()) {
                        srManager.reconnectWithFreshToken()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Token refresh error")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        historySyncJob?.cancel()
        initObserverJob?.cancel()
        autoReconnectJob?.cancel()
        connectingTimeoutJob?.cancel()
        deviceManager.closeRealTimeHeartRate()
    }
}