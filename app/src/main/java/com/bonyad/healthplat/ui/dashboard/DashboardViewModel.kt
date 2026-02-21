package com.bonyad.healthplat.ui.dashboard

import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.blesdk.model.ConnectionState
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.AIAnalysisApiService
import com.bonyad.healthplat.data.network.SignalRManager
import com.bonyad.healthplat.data.network.TokenManager
import com.bonyad.healthplat.data.repository.HealthDataRepository
import com.bonyad.healthplat.data.repository.UserRepository
import com.bonyad.healthplat.domain.model.ApiErrorType
import com.bonyad.healthplat.domain.model.RecordDataResult

import com.bonyad.healthplat.ui.utils.PermissionUtils
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

    // Device State
    val batteryLevel: Int? = null,
    val isDeviceConnected: Boolean = false
)


@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceManager: HealthDeviceManager,
    private val aiApiService: AIAnalysisApiService,
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

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // --- NEW AI STATE ---
    private val _readinessScore = MutableStateFlow(0)
    val readinessScore: StateFlow<Int> = _readinessScore.asStateFlow()

    private val _healthInsights = MutableStateFlow(
        listOf("در حال تحلیل وضعیت شما...")
    )
    val healthInsights: StateFlow<List<String>> = _healthInsights.asStateFlow()

    private var historySyncJob: Job? = null
    private var hasCompletedInitialSync = false

    init {
        Timber.i("📱 DashboardViewModel initialized")

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

            val datesToTry = (1..3).map { daysAgo ->
                LocalDate.now().minusDays(daysAgo.toLong())
                    .format(DateTimeFormatter.ISO_DATE)
            }

            for (date in datesToTry) {

                Timber.d("Trying readiness date: $date")

                val response = aiApiService.getReadinessScore(userId, date)

                if (!response.isSuccessful) {
                    if (response.code() == 404 || response.code() == 500) {
                        continue
                    } else {
                        fallbackGenericError()
                        return
                    }
                }

                val body = response.body() ?: run {
                    fallbackGenericError()
                    return
                }

                if (!body.ok) {
                    if (body.error?.type == ApiErrorType.NO_METRICS_FOUND) {
                        continue
                    } else {
                        fallbackGenericError()
                        return
                    }
                }

                val data = body.data ?: continue

                // ✅ SUCCESS → stop fallback loop
                _readinessScore.value =
                    data.absReadinessScore.coerceIn(0, 100)

                _healthInsights.value =
                    if (data.perAspectNotes.isNotEmpty())
                        data.perAspectNotes.values.toList()
                    else
                        listOf("وضعیت کلی شما نرمال است")

                return
            }

            _readinessScore.value = 0
            _healthInsights.value =
                listOf("داده‌ای برای چند روز اخیر ثبت نشده است")

        } catch (e: Exception) {
            Timber.e(e, "Error fetching readiness")
            _readinessScore.value = 0
            _healthInsights.value =
                listOf("عدم دسترسی به سرویس")
        }
    }

    private fun fallbackGenericError() {
        _readinessScore.value = 0
        _healthInsights.value = listOf("خطا در دریافت تحلیل")
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
        // You might want to show an explanation dialog here
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
        viewModelScope.launch {
            deviceManager.initializationComplete.collect {
                Timber.i("🎯 Device initialization complete")

                // OPTIMIZATION: Reduced from 2000ms to 500ms
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

            // OPTIMIZATION: Minimal delay (reduced from 2500ms)
            delay(300)

            try {
                val result = healthRepository.syncAllMissingDays()

                if (result is RecordDataResult.Success) {
                    processHistoryData(result)
                    hasCompletedInitialSync = true
                    Timber.i("✅ History sync complete (all missing days synced)")
                } else if (result is RecordDataResult.Error) {
                    Timber.w("⚠️ Sync failed: ${result.message}")
                }
            } catch (e: Exception) {
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
                bloodOxygen = data.spo2?.sourceList?.lastOrNull { it > 0 } ?: current.bloodOxygen,
                stressLevel = data.stress?.stressSource?.lastOrNull { it > 0 } ?: current.stressLevel,
                hrv = data.hrv?.hrvSource?.lastOrNull { it > 0 } ?: current.hrv,
                steps = data.steps?.stepSource?.sum()?.takeIf { it > current.steps } ?: current.steps,

                // Update history for charts
                heartRateHistory = data.heartRate?.heartRateSource ?: current.heartRateHistory,
                stepsHistory = data.steps?.stepSource ?: current.stepsHistory,
                sleepHistory = data.sleep?.sourceList ?: current.sleepHistory,
                spo2History = data.spo2?.sourceList ?: current.spo2History
            )
        }
    }

    private fun processHistoryDataUpdated(data: RecordDataResult.Success) {
        _healthOverview.update { current ->
            // 1. Calculate Total Sleep (Hours)
            val sleepSource = data.sleep?.sourceList ?: emptyList()
            val sleepDuration = calculateSleepHours(sleepSource)

            // 2. Get Last SpO2
            val spo2List = data.spo2?.sourceList ?: emptyList()
            val lastSpo2 = spo2List.lastOrNull { it > 0 } ?: current.bloodOxygen

            // 3. Total Steps (Sum of the day)
            val stepSource = data.steps?.stepSource ?: emptyList()
            val totalSteps = if (stepSource.isNotEmpty()) stepSource.sum() else current.steps

            // 4. Heart Rate History
            val hrSource = data.heartRate?.heartRateSource ?: emptyList()

            current.copy(
                // Update Values
                sleepDurationHours = sleepDuration,
                bloodOxygen = lastSpo2,
                steps = totalSteps,

                // Update History Lists for Charts
                heartRateHistory = hrSource,
                stepsHistory = stepSource,
                sleepHistory = sleepSource,
                spo2History = spo2List
            )
        }
    }

    private fun calculateSleepHours(sleepData: List<Int>?): Float {
        if (sleepData.isNullOrEmpty()) return 0f
        val sleepMinutes = sleepData.count { it == 1 || it == 2 || it == 4 }
        return sleepMinutes / 60f
    }

    private fun autoConnectDevice() {
        viewModelScope.launch {
            val currentState = deviceManager.connectionState.value
            if (currentState == ConnectionState.CONNECTED || currentState == ConnectionState.CONNECTING) {
                Timber.d("🚫 Already ${currentState}")
                return@launch
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

                // OPTIMIZATION: Reduced from 800ms
                delay(200)

                val deviceMac = userPreferences.getDeviceMac().first()
                if (!deviceMac.isNullOrEmpty()) {
                    Timber.i("🔄 Auto-connecting to: $deviceMac")
                    deviceManager.connect(deviceMac)
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

    private fun observeDeviceConnection() {
        viewModelScope.launch {
            deviceManager.connectionState.collect { state ->
                Timber.d("🔵 Connection state: $state")

                val isConnected = state == ConnectionState.CONNECTED
                _healthOverview.update { it.copy(isDeviceConnected = isConnected) }

                when (state) {
                    ConnectionState.CONNECTED -> {
                        Timber.i("✅ Device connected")
                        // Start observing initialization after connection
                        observeInitializationComplete()
                    }
                    ConnectionState.DISCONNECTED -> {
                        hasCompletedInitialSync = false
                        _healthOverview.update { it.copy(batteryLevel = null) }

                        // OPTIMIZATION: Faster reconnect attempt
                        delay(1000) // Reduced from default
                        autoConnectDevice()
                    }
                    else -> {}
                }
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
            try {
                fetchReadinessData()
                if (_healthOverview.value.isDeviceConnected) {
                    deviceManager.readBatteryLevel()
                    // wait for sync to actually finish
                    historySyncJob?.cancel()
                    historySyncJob = launch {
                        try {
                            val result = healthRepository.syncAllMissingDays()
                            if (result is RecordDataResult.Success) {
                                processHistoryData(result)
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Refresh sync failed")
                        }
                    }
                    historySyncJob?.join()
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun connectDevice() {
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

        viewModelScope.launch { autoConnectDevice() }
    }

    fun onBluetoothEnabled() {
        // Called from UI after user enables Bluetooth
        viewModelScope.launch {
            delay(500)
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
        deviceManager.closeRealTimeHeartRate()
    }
}