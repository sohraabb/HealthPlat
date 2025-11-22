package com.bonyad.healthplat.ui.dashboard

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.blesdk.model.ConnectionState
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
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
    private val userPreferences: UserPreferencesDataStore
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
        observeRealTimeData()
        observeBatteryLevel()
        autoConnectDevice()

        viewModelScope.launch {
            deviceManager.connectionState.collect { state ->
                if (state == ConnectionState.CONNECTED) {
                    Timber.i("✅ Device Connected - Triggering Sync")
                    delay(2000)
                    syncDeviceHistory()
                }
            }
        }
    }

    private fun syncDeviceHistory() {
        viewModelScope.launch {
            Timber.i("🔄 Syncing history data (UI + Server)...")

            // 1. Call Repository (It fetches from Ring AND Uploads to Server)
            val result = healthRepository.syncDashboardData()

            // 2. Update UI with the returned result
            if (result is RecordDataResult.Success) {
                processHistoryData(result)
                Timber.i("✅ UI updated with history data")
            } else {
                Timber.w("⚠️ Sync failed or no data found")
            }
        }
    }

    private fun processHistoryData(data: RecordDataResult.Success) {
        _healthOverview.update { current ->
            current.copy(
                // Calculate Sleep Duration (in hours)
                // The SDK returns sleep as a list of states. You usually need to sum up the minutes.
                // For now, let's assume you just want to display if data exists.
                // You would typically loop through data.sleep?.sourceList to count minutes.
                sleepDurationHours = calculateSleepHours(data.sleep?.sourceList),

                // Get last non-zero SpO2 value
                bloodOxygen = data.spo2?.sourceList?.lastOrNull { it > 0 } ?: current.bloodOxygen,

                // Get average or last stress value
                stressLevel = data.stress?.stressSource?.lastOrNull { it > 0 } ?: current.stressLevel,

                // Get HRV
                hrv = data.hrv?.hrvSource?.lastOrNull { it > 0 } ?: current.hrv
            )
        }
    }

    private fun calculateSleepHours(sleepData: List<Int>?): Float {
        if (sleepData.isNullOrEmpty()) return 0f
        // SDK logic: Count slots that are NOT 'awake'.
        // Note: Real implementation depends on how SDK encodes time (usually 1 min or 5 min per slot)
        // Assuming 1 slot = 1 minute for simplicity based on generic BLE SDKs
        val sleepMinutes = sleepData.count { it != 0 && it != 3 } // Assuming 3 is awake [cite: 223]
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
                    // Start real-time monitoring
                    deviceManager.openRealTimeHeartRate()
                    delay(800) // Small delay before reading battery
                    deviceManager.readBatteryLevel()
                } else if (state == ConnectionState.DISCONNECTED) {
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
                _healthOverview.update { overview ->
                    overview.copy(
                        heartRate = data.heart ?: overview.heartRate,
                        steps = data.step ?: overview.steps
                    )
                }
                Timber.d("💓 Real-time data: HR=${data.heart}, Steps=${data.step}")
            }
        }
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

    override fun onCleared() {
        super.onCleared()
        Timber.i("🧹 DashboardViewModel cleared - stopping monitoring")
        deviceManager.closeRealTimeHeartRate()
    }
}
