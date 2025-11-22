package com.bonyad.healthplat.ui.device

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonlala.bonlalable.bean.ScanDeviceInfo
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.blesdk.model.ConnectionState
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.repository.AuthResult
import com.bonyad.healthplat.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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
    private val deviceRepository: DeviceRepository
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
                            //_uiState.value = DeviceConnectionUiState.Error("اتصال ناموفق بود")
                        }
                    }
                    ConnectionState.SCANNING -> {
                        _uiState.value = DeviceConnectionUiState.Scanning
                    }
                    ConnectionState.CONNECTING -> {
                        _uiState.value = DeviceConnectionUiState.Connecting
                    }
                    ConnectionState.CONNECTED -> {
//                        _uiState.value = DeviceConnectionUiState.Connected
                        stopScanTimer()
                    }
                }
            }
        }
        viewModelScope.launch {
            deviceManager.scannedDevices.collect { devices ->
                _scannedDevices.value = devices
                Timber.d("ViewModel: Received ${devices.size} devices")
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
        val name = device.bluetoothDevice?.name

        if (mac == null) {
            _uiState.value = DeviceConnectionUiState.Error("آدرس دستگاه نامعتبر است")
            return
        }

        _uiState.value = DeviceConnectionUiState.Connecting
        stopScan()

        try {
            // Step 1: Connect via Bluetooth
            deviceManager.connect(mac)

            // Step 2: Wait for stable connection and collect device info
            viewModelScope.launch {
                // Wait for connection to be established
                deviceManager.connectionState
                    .filter { it == ConnectionState.CONNECTED }
                    .first()

                // Wait a bit more for initialization to complete
                //delay(3000) // ← INCREASE THIS from 2000

                delay(4000)
                // Get firmware version from device
                val firmwareVersion = deviceManager.firmwareVersion.value ?: "Unknown"

                Timber.i("Device connected, firmware: $firmwareVersion")

                // Step 3: Register device to backend
                when (val result = deviceRepository.addUserDevice(
                    deviceMac = mac,
                    deviceName = name,
                    firmwareVersion = firmwareVersion
                )) {
                    is AuthResult.Success -> {
                        Timber.i("Device registered to backend: ${result.data.id}")
                        _uiState.value = DeviceConnectionUiState.Connected
                    }
                    is AuthResult.Error -> {
                        Timber.w("Failed to register device to backend: ${result.message}")
                        // Still mark as connected since BLE is connected
                        _uiState.value = DeviceConnectionUiState.Connected
                    }
                }
            }

            // Timeout after 30 seconds (increased from 15)
            viewModelScope.launch {
                delay(40000)
                if (_uiState.value is DeviceConnectionUiState.Connecting) {
                    _uiState.value = DeviceConnectionUiState.Error("اتصال ناموفق بود")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect")
            _uiState.value = DeviceConnectionUiState.Error("خطا در اتصال به دستگاه")
        }
    }

    /*
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

     */

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
                delay(2000)
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