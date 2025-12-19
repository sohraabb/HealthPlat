package com.bonyad.healthplat.ui.dashboard

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.blesdk.model.ConnectionState
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.SignalRManager
import com.bonyad.healthplat.data.network.TokenRefresher
import com.bonyad.healthplat.data.repository.AuthResult
import com.bonyad.healthplat.data.repository.HealthDataRepository
import com.bonyad.healthplat.data.repository.UserRepository
import com.bonyad.healthplat.domain.model.RecordDataResult
import com.bonyad.healthplat.ui.utils.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
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
    private val healthRepository: HealthDataRepository,
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferencesDataStore,
    private val tokenRefresher: TokenRefresher,
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

    val healthInsights = mutableStateListOf(
        "امروزت عالی بوده، ادامه بده",
        "دیشب فقط ۵ ساعت خوابیدی",
        "ضربان قلبت بالاست، بهتره قدم بزنی"
    )


    init {
        Timber.i("📱 DashboardViewModel initialized")

        viewModelScope.launch {
            startTokenRefreshLoop()
            ensureUserData()
            loadUserData()
            observeDeviceConnection()
            observeInitializationComplete()
            observeRealTimeData()

            // Connect SignalR after user data is loaded
            srManager.observeToken(viewModelScope)
            srManager.connect()

            // Check permissions and auto-connect if available
            checkPermissionsAndConnect()
        }
    }

    private suspend fun checkPermissionsAndConnect() {
        delay(500) // Small delay to ensure UI is ready

        if (PermissionUtils.hasBluetoothPermissions(context)) {
            Timber.i("✅ Bluetooth permissions granted, attempting auto-connect")
            _needsBluetoothPermissions.value = false
            autoConnectDevice()
        } else {
            val missingPermissions = PermissionUtils.getMissingBluetoothPermissions(context)
            Timber.w("⚠️ Missing Bluetooth permissions: $missingPermissions")
            _needsBluetoothPermissions.value = true
        }
    }

    fun onPermissionsGranted() {
        Timber.i("✅ Permissions granted, attempting device connection")
        _needsBluetoothPermissions.value = false
        viewModelScope.launch {
            autoConnectDevice()
        }
    }

    fun onPermissionsDenied() {
        Timber.w("⚠️ User denied Bluetooth permissions")
        _needsBluetoothPermissions.value = false
        // You might want to show an explanation dialog here
    }

    private suspend fun ensureUserData() {
        try {
            _isLoading.value = true

            val result = userRepository.ensureUserDataAvailable()

            when (result) {
                is AuthResult.Success -> {
                    Timber.i("✅ User data is available")
                }
                is AuthResult.Error -> {
                    Timber.e("❌ Failed to ensure user data: ${result.message}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error in ensureUserData")
        } finally {
            _isLoading.value = false
        }
    }

    private fun observeInitializationComplete() {
        viewModelScope.launch {
            deviceManager.initializationComplete.collect {
                Timber.i("🎯🎯🎯 Device initialization complete - starting history sync 🎯🎯🎯")
                delay(2000) // Extra safety margin
                syncDeviceHistory()
            }
        }
    }

    fun syncDeviceHistory() {
        viewModelScope.launch {
            Timber.i("🔄 Syncing history data (UI + Server)...")

            deviceManager.closeRealTimeHeartRate()
            delay(1000)

            // 1. Call Repository (It fetches from Ring AND Uploads to Server)
            val result = healthRepository.syncDashboardData(6)

            // 2. Update UI with the returned result
            if (result is RecordDataResult.Success) {
                processHistoryData(result)
                Timber.i("✅ UI updated with history data")
            } else {
                Timber.w("⚠️ Sync failed or no data found")
            }

            delay(1000)
            Timber.i("🔄 History sync done, restarting real-time monitoring")
            deviceManager.openRealTimeHeartRate()

        }
    }

    private fun processHistoryData(data: RecordDataResult.Success) {
        _healthOverview.update { current ->
            current.copy(
                // 1. Sleep: Use the new RecordSleepBean
                // SDK v1.4: 0=Activity, 1=Deep, 2=Light, 3=Awake, 4=REM
                sleepDurationHours = calculateSleepHours(data.sleep?.sourceList),

                // 2. SpO2: Use RecordSpo2Bean -> sourceList [cite: 260]
                // Get last non-zero value. Note: SpO2 is recorded every 30 mins [cite: 257]
                bloodOxygen = data.spo2?.sourceList?.lastOrNull { it > 0 } ?: current.bloodOxygen,

                // 3. Stress: Use RecordStressBean -> stressSource [cite: 252]
                stressLevel = data.stress?.stressSource?.lastOrNull { it > 0 }
                    ?: current.stressLevel,

                // 4. HRV: Use RecordHrvBean -> hrvSource [cite: 266]
                hrv = data.hrv?.hrvSource?.lastOrNull { it > 0 } ?: current.hrv,

                // 5. Steps: Use RecordStepBean -> stepSource
                // If history sync provides a more accurate total than real-time, update it here
                steps = data.steps?.stepSource?.sum() ?: current.steps
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

        // SDK v1.4 Definition :
        // 0: activities
        // 1: deep sleep
        // 2: light sleep
        // 3: awake sleep
        // 4: rem

        // Count minutes where status is Deep(1), Light(2), or REM(4).
        // Usually, we exclude 'Awake'(3) and 'Activity'(0) from total duration.
        val sleepMinutes = sleepData.count { it == 1 || it == 2 || it == 4 }

        return sleepMinutes / 60f
    }

    private fun autoConnectDevice() {
        viewModelScope.launch {
            try {
                // Double-check permissions before connecting
                if (!PermissionUtils.hasBluetoothPermissions(context)) {
                    Timber.w("⚠️ Cannot auto-connect: Missing Bluetooth permissions")
                    _needsBluetoothPermissions.value = true
                    return@launch
                }

                delay(800)

                val deviceMac = userPreferences.getDeviceMac().first()
                if (!deviceMac.isNullOrEmpty()) {
                    Timber.i("🔄 Auto-connecting to saved device: $deviceMac")
                    deviceManager.connect(deviceMac)
                } else {
                    Timber.w("⚠️ No saved device MAC found for auto-connect")
                }
            } catch (e: SecurityException) {
                Timber.e(e, "❌ SecurityException during auto-connect - Missing permissions")
                _needsBluetoothPermissions.value = true
            } catch (e: Exception) {
                Timber.e(e, "❌ Error during auto-connect")
            }
        }
    }


    private fun loadUserData() {
        viewModelScope.launch {
            userPreferences.getUserName().collect { name ->
                _userName.value = name
                Timber.d("👤 User name loaded: $name")
            }
        }
    }

    private fun observeDeviceConnection() {
        viewModelScope.launch {
            deviceManager.connectionState.collect { state ->
                Timber.d("🔵 ViewModel: Connection state changed to: $state")

                val isConnected = state == ConnectionState.CONNECTED
                _healthOverview.update {
                    Timber.d("🔵 ViewModel: Updating isDeviceConnected to: $isConnected")
                    it.copy(isDeviceConnected = isConnected)
                }

                if (state == ConnectionState.CONNECTED) {
                    Timber.i("✅ Device connected - starting monitoring")
//                    deviceManager.openRealTimeHeartRate()
//                    delay(800)
//                    deviceManager.readBatteryLevel()

                } else if (state == ConnectionState.DISCONNECTED) {
                    autoConnectDevice()
                    Timber.w("⚠️ Device disconnected")
                    // Clear battery level on disconnect
                    _healthOverview.update {
                        it.copy(batteryLevel = null)
                    }
                }
            }
        }
    }

    private fun observeRealTimeData() {
        viewModelScope.launch {
            deviceManager.realTimeData.collect { data ->
                val heartRate = data.heart
                _healthOverview.update { overview ->
                    overview.copy(
                        heartRate = data.heart ?: overview.heartRate,
                        steps = data.step ?: overview.steps
                    )
                }
                sendHeartRateWebSocket(heartRate)
                Timber.d("💓 Real-time data: HR=${data.heart}, Steps=${data.step}")
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
                Timber.d("🔋 ViewModel: Battery level received: $battery%")
                _healthOverview.update {
                    it.copy(batteryLevel = battery)
                }
            }
        }
    }

    fun refreshData() {
        Timber.i("🔄 Manual refresh requested")
        if (_healthOverview.value.isDeviceConnected) {
            deviceManager.readBatteryLevel()
            syncDeviceHistory() // <--- ADD THIS
        } else {
            Timber.w("⚠️ Cannot refresh - device not connected")
        }
    }

    fun connectDevice() {
        Timber.i("🔌 Manual connection requested")

        // Check permissions first
        if (!PermissionUtils.hasBluetoothPermissions(context)) {
            Timber.w("⚠️ Cannot connect: Missing Bluetooth permissions")
            _needsBluetoothPermissions.value = true
            return
        }

        viewModelScope.launch {
            try {
                val deviceMac = userPreferences.getDeviceMac().first()
                if (!deviceMac.isNullOrEmpty()) {
                    Timber.i("🔌 Connecting to device: $deviceMac")
                    deviceManager.connect(deviceMac)
                } else {
                    Timber.w("⚠️ No device MAC found in preferences")
                }
            } catch (e: SecurityException) {
                Timber.e(e, "❌ SecurityException - Missing permissions")
                _needsBluetoothPermissions.value = true
            } catch (e: Exception) {
                Timber.e(e, "❌ Error getting device MAC from preferences")
            }
        }
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
                tokenRefresher.refreshIfNeeded()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.i("🧹 DashboardViewModel cleared - stopping monitoring")
        deviceManager.closeRealTimeHeartRate()
    }
}
