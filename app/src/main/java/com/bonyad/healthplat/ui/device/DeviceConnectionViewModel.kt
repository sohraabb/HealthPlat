package com.bonyad.healthplat.ui.device

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonlala.bonlalable.bean.ScanDeviceInfo
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.blesdk.model.ConnectionState
import com.bonyad.healthplat.blesdk.model.PairingState
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.repository.AuthResult
import com.bonyad.healthplat.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber

sealed class DeviceConnectionUiState {
    object Idle : DeviceConnectionUiState()
    object Scanning : DeviceConnectionUiState()
    object Connecting : DeviceConnectionUiState()
    object WaitingForPairing : DeviceConnectionUiState()
    object Initializing : DeviceConnectionUiState()
    object Connected : DeviceConnectionUiState()
    data class Error(val message: String) : DeviceConnectionUiState()
}

@HiltViewModel
class DeviceConnectionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceManager: HealthDeviceManager,
    private val deviceRepository: DeviceRepository,
    private val userPreferences: UserPreferencesDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DeviceConnectionUiState>(DeviceConnectionUiState.Idle)
    val uiState: StateFlow<DeviceConnectionUiState> = _uiState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<ScanDeviceInfo>>(emptyList())
    val scannedDevices: StateFlow<List<ScanDeviceInfo>> = _scannedDevices.asStateFlow()

    private val _scanDuration = MutableStateFlow(0)
    val scanDuration: StateFlow<Int> = _scanDuration.asStateFlow()

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothManager?.adapter
    }

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    val isBluetoothSupported: Boolean
        get() = bluetoothAdapter != null

    private var scanTimerJob: Job? = null
    private var connectionJob: Job? = null


    init {

        observeScannedDevices()


        // Observe connection state from device manager
//        viewModelScope.launch {
//            deviceManager.connectionState.collect { state ->
//                when (state) {
//                    ConnectionState.DISCONNECTED -> {
//                        if (_uiState.value is DeviceConnectionUiState.Connecting) {
//                            //_uiState.value = DeviceConnectionUiState.Error("اتصال ناموفق بود")
//                        }
//                    }
//                    ConnectionState.SCANNING -> {
//                        _uiState.value = DeviceConnectionUiState.Scanning
//                    }
//                    ConnectionState.CONNECTING -> {
//                        _uiState.value = DeviceConnectionUiState.Connecting
//                    }
//                    ConnectionState.CONNECTED -> {
////                        _uiState.value = DeviceConnectionUiState.Connected
//                        stopScanTimer()
//                    }
//                }
//            }
//        }
//
//        viewModelScope.launch {
//            deviceManager.isPaired.collect { isPaired ->
//                Timber.d("🔐 Pairing state changed: $isPaired")
//
//                // Show pairing state in UI
//                if (deviceManager.connectionState.value == ConnectionState.CONNECTED &&
//                    !isPaired &&
//                    _uiState.value is DeviceConnectionUiState.Connecting) {
//                    _uiState.value = DeviceConnectionUiState.Pairing
//                }
//            }
//        }
//
//
//        viewModelScope.launch {
//            deviceManager.scannedDevices.collect { devices ->
//                _scannedDevices.value = devices
//                Timber.d("ViewModel: Received ${devices.size} devices")
//            }
//        }
    }


    private fun observeScannedDevices() {
        viewModelScope.launch {
            deviceManager.scannedDevices.collect { devices ->
                _scannedDevices.value = devices
                Timber.d("📱 ViewModel: Received ${devices.size} devices")
            }
        }
    }

    fun startScan() {
        Timber.i("🔍 Starting BLE scan")
        _uiState.value = DeviceConnectionUiState.Scanning
        _scannedDevices.value = emptyList()
        _scanDuration.value = 0

        try {
            deviceManager.startScan()
            startScanTimer()

            // Auto-stop after 30 seconds (SDK default is 6 seconds per scan cycle)
            viewModelScope.launch {
                delay(30000)
                if (_uiState.value is DeviceConnectionUiState.Scanning) {
                    stopScan()
                    if (_scannedDevices.value.isEmpty()) {
                        _uiState.value = DeviceConnectionUiState.Error("هیچ دستگاهی پیدا نشد")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to start scan")
            _uiState.value = DeviceConnectionUiState.Error("خطا در جستجوی دستگاه")
        }
    }

    fun stopScan() {
        Timber.i("⏹️ Stopping BLE scan")
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

        // Cancel any existing connection attempt
        connectionJob?.cancel()

        Timber.i("🔌 Starting connection to: $mac")
        stopScan()
        _uiState.value = DeviceConnectionUiState.Connecting

        connectionJob = viewModelScope.launch {
            try {
                // Total timeout: 90 seconds (pairing happens automatically)
                withTimeout(90000) {

                    // Step 1: Start BLE connection (non-blocking)
                    deviceManager.connect(mac)

                    // Step 2: Wait for CONNECTED state
                    Timber.i("⏳ Waiting for BLE connection...")
                    deviceManager.connectionState
                        .filter { it == ConnectionState.CONNECTED }
                        .first()

                    Timber.i("✅ BLE Connected")

                    // Step 3: Wait for pairing (if needed)
                    Timber.i("⏳ Checking pairing status...")

                    // Observe pairing state
                    deviceManager.pairingState.collect { pairingState ->
                        when (pairingState) {
                            is PairingState.PairingRequested -> {
                                Timber.i("📲 Pairing requested - waiting for user")
                                _uiState.value = DeviceConnectionUiState.WaitingForPairing
                            }
                            is PairingState.Paired -> {
                                Timber.i("✅ Device paired")

                                // Step 4: Wait for initialization
                                _uiState.value = DeviceConnectionUiState.Initializing
                                Timber.i("⏳ Waiting for device initialization...")

                                deviceManager.initializationComplete.first()

                                Timber.i("✅ Device fully initialized")

                                // Step 5: Register with backend
                                val firmwareVersion = deviceManager.firmwareVersion.value
                                val result = deviceRepository.addUserDevice(
                                    deviceMac = mac,
                                    deviceName = name,
                                    firmwareVersion = firmwareVersion
                                )

                                when (result) {
                                    is AuthResult.Success -> {
                                        Timber.i("☁️ Device registered: ID=${result.data.id}")
                                        userPreferences.saveDeviceInfo(mac, name)
                                        userPreferences.saveDeviceId(result.data.id)
                                        _uiState.value = DeviceConnectionUiState.Connected
                                    }
                                    is AuthResult.Error -> {
                                        Timber.w("⚠️ Backend registration failed: ${result.message}")
                                        userPreferences.saveDeviceInfo(mac, name)
                                        _uiState.value = DeviceConnectionUiState.Connected
                                    }
                                }
                            }
                            is PairingState.PairingFailed -> {
                                Timber.e("❌ Pairing failed: ${pairingState.error}")
                                throw Exception("Pairing failed: ${pairingState.error}")
                            }
                            else -> {
                                // NotPaired or other state - continue waiting
                            }
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Timber.e("⏱️ Connection timeout after 90 seconds")
                deviceManager.disconnect()
                _uiState.value = DeviceConnectionUiState.Error(
                    "زمان اتصال تمام شد. لطفا دوباره تلاش کنید"
                )
            } catch (e: Exception) {
                Timber.e(e, "❌ Connection error")
                deviceManager.disconnect()
                _uiState.value = DeviceConnectionUiState.Error(
                    "خطا در اتصال: ${e.message ?: "نامشخص"}"
                )
            }
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
        connectionJob?.cancel()
        stopScan()
    }
}