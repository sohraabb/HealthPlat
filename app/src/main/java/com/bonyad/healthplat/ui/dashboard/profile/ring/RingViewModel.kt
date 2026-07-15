package com.bonyad.healthplat.ui.dashboard.profile.ring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.blesdk.model.ConnectionState
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.repository.HealthDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class RingUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val batteryLevel: Int? = null,
    val firmwareVersion: String? = null,
    val deviceMac: String? = null,
    val deviceName: String? = null,
    val lastSyncTime: String? = null,
    val isSyncing: Boolean = false,
    val isReconnecting: Boolean = false
)

@HiltViewModel
class RingViewModel @Inject constructor(
    private val deviceManager: HealthDeviceManager,
    private val userPreferences: UserPreferencesDataStore,
    private val healthDataRepository: HealthDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RingUiState())
    val uiState: StateFlow<RingUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        collectDeviceState()
        collectPreferences()
    }

    private fun collectDeviceState() {
        viewModelScope.launch {
            deviceManager.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
                if (state == ConnectionState.CONNECTED) {
                    _uiState.update { it.copy(isReconnecting = false) }
                }
                if (state == ConnectionState.DISCONNECTED) {
                    _uiState.update { it.copy(isReconnecting = false) }
                }
            }
        }
        viewModelScope.launch {
            deviceManager.batteryLevel.collect { battery ->
                _uiState.update { it.copy(batteryLevel = battery) }
            }
        }
        viewModelScope.launch {
            deviceManager.firmwareVersion.collect { firmware ->
                _uiState.update { it.copy(firmwareVersion = firmware) }
            }
        }
    }

    private fun collectPreferences() {
        viewModelScope.launch {
            userPreferences.getDeviceMac().collect { mac ->
                _uiState.update { it.copy(deviceMac = mac) }
            }
        }
        viewModelScope.launch {
            userPreferences.getLastSyncServerTime().collect { time ->
                _uiState.update { it.copy(lastSyncTime = time) }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            try {
                deviceManager.disconnect()
                Timber.d("Ring disconnected by user")
            } catch (e: Exception) {
                Timber.e(e, "Failed to disconnect ring")
                _toastMessage.emit("خطا در قطع اتصال")
            }
        }
    }

    fun reconnect() {
        val mac = _uiState.value.deviceMac
        if (mac.isNullOrBlank()) {
            viewModelScope.launch {
                _toastMessage.emit("آدرس دستگاه یافت نشد")
            }
            return
        }
        _uiState.update { it.copy(isReconnecting = true) }
        viewModelScope.launch {
            try {
                deviceManager.reconnect(mac)
                Timber.d("Reconnect initiated for MAC: $mac")
            } catch (e: Exception) {
                Timber.e(e, "Failed to reconnect ring")
                _uiState.update { it.copy(isReconnecting = false) }
                _toastMessage.emit("خطا در اتصال مجدد")
            }
        }
    }

    fun syncData() {
        if (_uiState.value.isSyncing) return
        _uiState.update { it.copy(isSyncing = true) }
        viewModelScope.launch {
            try {
                healthDataRepository.syncAllMissingDays()
                Timber.d("Manual sync completed successfully")
                _toastMessage.emit("همگام‌سازی با موفقیت انجام شد")
            } catch (e: Exception) {
                Timber.e(e, "Manual sync failed")
                _toastMessage.emit("خطا در همگام‌سازی")
            } finally {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }
    }
}
