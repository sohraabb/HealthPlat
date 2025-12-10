package com.bonyad.healthplat.ui.dashboard

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.blesdk.model.ConnectionState
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.SignalRManager
import com.bonyad.healthplat.data.repository.HealthDataRepository
import com.bonyad.healthplat.domain.model.RecordDataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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

    // Device State
    val batteryLevel: Int? = null,
    val isDeviceConnected: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val deviceManager: HealthDeviceManager,
    private val healthRepository: HealthDataRepository,
    private val userPreferences: UserPreferencesDataStore,
    private val srManager: SignalRManager
) : ViewModel() {

    private val _healthOverview = MutableStateFlow(HealthOverview())
    val healthOverview: StateFlow<HealthOverview> = _healthOverview.asStateFlow()

    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName.asStateFlow()

    val healthInsights = mutableStateListOf(
        "امروزت عالی بوده، ادامه بده",
        "دیشب فقط ۵ ساعت خوابیدی",
        "ضربان قلبت بالاست، بهتره قدم بزنی"
    )


    init {
        Timber.i("📱 DashboardViewModel initialized")
        loadUserData()
        observeDeviceConnection()

        observeInitializationComplete()


        viewModelScope.launch {
            srManager.connect()
        }
        observeRealTimeData()
//        observeBatteryLevel()



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
            val result = healthRepository.syncDashboardData(0)

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
                // Small delay to ensure everything is initialized
                delay(800)

                val deviceMac = userPreferences.getDeviceMac().first()
                if (!deviceMac.isNullOrEmpty()) {
                    Timber.i("🔄 Auto-connecting to saved device: $deviceMac")
                    deviceManager.connect(deviceMac)
                } else {
                    Timber.w("⚠️ No saved device MAC found for auto-connect")
                }
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
        viewModelScope.launch {
            try {
                val deviceMac = userPreferences.getDeviceMac().first()
                if (!deviceMac.isNullOrEmpty()) {
                    Timber.i("🔌 Connecting to device: $deviceMac")
                    deviceManager.connect(deviceMac)
                } else {
                    Timber.w("⚠️ No device MAC found in preferences")
                }
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

    override fun onCleared() {
        super.onCleared()
        Timber.i("🧹 DashboardViewModel cleared - stopping monitoring")
        deviceManager.closeRealTimeHeartRate()
    }
}
