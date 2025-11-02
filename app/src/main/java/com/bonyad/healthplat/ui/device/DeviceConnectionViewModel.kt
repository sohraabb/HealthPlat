package com.bonyad.healthplat.ui.device

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonlala.bonlalable.bean.ScanDeviceInfo
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.blesdk.model.ConnectionState
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber


sealed class DeviceConnectionUiState {
    object Idle : DeviceConnectionUiState()
    object Scanning : DeviceConnectionUiState()
    object Connecting : DeviceConnectionUiState()
    object Connected : DeviceConnectionUiState()
    data class Error(val message: String) : DeviceConnectionUiState()
}

@HiltViewModel
class DeviceConnectionViewModel @Inject constructor(
    private val deviceManager: HealthDeviceManager,
    private val userPreferences: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<DeviceConnectionUiState>(DeviceConnectionUiState.Idle)
    val uiState: StateFlow<DeviceConnectionUiState> = _uiState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<ScanDeviceInfo>>(emptyList())
    val scannedDevices: StateFlow<List<ScanDeviceInfo>> = _scannedDevices.asStateFlow()

    private val _scanDuration = MutableStateFlow(0)
    val scanDuration: StateFlow<Int> = _scanDuration.asStateFlow()

    private var scanTimerJob: Job? = null

    init {
        // Observe connection state from device manager
        viewModelScope.launch {
            deviceManager.connectionState.collect { state ->
                when (state) {
                    ConnectionState.DISCONNECTED -> {
                        if (_uiState.value is DeviceConnectionUiState.Connecting) {
                            _uiState.value = DeviceConnectionUiState.Error("اتصال ناموفق بود")
                        }
                    }
                    ConnectionState.SCANNING -> {
                        _uiState.value = DeviceConnectionUiState.Scanning
                    }
                    ConnectionState.CONNECTING -> {
                        _uiState.value = DeviceConnectionUiState.Connecting
                    }
                    ConnectionState.CONNECTED -> {
                        _uiState.value = DeviceConnectionUiState.Connected
                        stopScanTimer()
                    }
                }
            }
            // Collect scanned devices
            viewModelScope.launch {
                deviceManager.scannedDevices.collect { devices ->
                    _scannedDevices.value = devices
                }
            }
        }
    }

    fun startScan() {
        Timber.i("Starting BLE scan")
        _uiState.value = DeviceConnectionUiState.Scanning
        _scannedDevices.value = emptyList()
        _scanDuration.value = 0

        try {
            deviceManager.startScan()
            startScanTimer()

            // Auto-stop after 60 seconds
            viewModelScope.launch {
                delay(60000)
                if (_uiState.value is DeviceConnectionUiState.Scanning) {
                    stopScan()
                    if (_scannedDevices.value.isEmpty()) {
                        _uiState.value = DeviceConnectionUiState.Error("هیچ دستگاهی پیدا نشد")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to start scan")
            _uiState.value = DeviceConnectionUiState.Error("خطا در جستجوی دستگاه")
        }
    }

    fun stopScan() {
        Timber.i("Stopping BLE scan")
        deviceManager.stopScan()
        stopScanTimer()
        if (_uiState.value is DeviceConnectionUiState.Scanning) {
            _uiState.value = DeviceConnectionUiState.Idle
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: ScanDeviceInfo) {
        val mac = device.bluetoothDevice?.address
        if (mac == null) {
            _uiState.value = DeviceConnectionUiState.Error("آدرس دستگاه نامعتبر است")
            return
        }

        Timber.i("Connecting to device: $mac")
        stopScan()
        _uiState.value = DeviceConnectionUiState.Connecting

        try {
            deviceManager.connect(mac)

            // Save device info
            viewModelScope.launch {
                delay(1000)
                userPreferences.saveDeviceInfo(
                    mac = mac,
                    name = device.bluetoothDevice?.name
                )
            }

            // Timeout after 15 seconds
            viewModelScope.launch {
                delay(15000)
                if (_uiState.value is DeviceConnectionUiState.Connecting) {
                    _uiState.value = DeviceConnectionUiState.Error("اتصال ناموفق بود. لطفا دوباره تلاش کنید")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect")
            _uiState.value = DeviceConnectionUiState.Error("خطا در اتصال به دستگاه")
        }
    }

    fun retryConnection() {
        _uiState.value = DeviceConnectionUiState.Idle
    }

    fun resetError() {
        if (_uiState.value is DeviceConnectionUiState.Error) {
            _uiState.value = DeviceConnectionUiState.Idle
        }
    }

    // For testing: Add mock devices
    fun addMockDevices() {
        // This is for testing when you don't have a physical ring
        // Remove in production
        _scannedDevices.value = listOf(
            // Mock device - you can't actually create ScanDeviceInfo
            // This is just for UI testing
        )
    }

    private fun startScanTimer() {
        scanTimerJob?.cancel()
        scanTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _scanDuration.value += 1
            }
        }
    }

    private fun stopScanTimer() {
        scanTimerJob?.cancel()
        _scanDuration.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        stopScan()
    }
}