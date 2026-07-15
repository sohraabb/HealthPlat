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
    object ReadyToNavigate : DeviceConnectionUiState()
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
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    val isBluetoothEnabled: Boolean get() = bluetoothAdapter?.isEnabled == true
    val isBluetoothSupported: Boolean get() = bluetoothAdapter != null

    private var scanTimerJob: Job? = null
    private var connectionJob: Job? = null
    private var connectAttempt = 0

    init {
        observeScannedDevices()
    }

    private fun observeScannedDevices() {
        viewModelScope.launch {
            deviceManager.scannedDevices.collect { devices ->
                _scannedDevices.value = devices
                Timber.d("📱 Received ${devices.size} devices")
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
        deviceManager.stopScan()
        stopScanTimer()
        if (_uiState.value is DeviceConnectionUiState.Scanning) {
            _uiState.value = DeviceConnectionUiState.Idle
        }
    }

    fun connectToDevice(device: ScanDeviceInfo) {
        connectAttempt = 1
        attemptConnection(device)
    }

    @SuppressLint("MissingPermission")
    private fun attemptConnection(device: ScanDeviceInfo) {
        val btDevice = device.bluetoothDevice
        val mac = btDevice?.address
        val name = btDevice?.name

        if (btDevice == null || mac == null) {
            _uiState.value = DeviceConnectionUiState.Error("آدرس دستگاه نامعتبر است")
            return
        }

        connectionJob?.cancel()
        stopScan()
        _uiState.value = DeviceConnectionUiState.Connecting
        Timber.i("🔌 Connecting to: $mac ($name) [attempt $connectAttempt/$MAX_CONNECT_ATTEMPTS]")

        connectionJob = viewModelScope.launch {
            try {
                withTimeout(CONNECTION_TIMEOUT_MS) {

                    // ── Step 1: Initiate BLE connection ──
                    deviceManager.connect(btDevice)

                    // ── Step 2: Monitor pairing state for UI ──
                    // Side-job: purely visual feedback for the user.
                    // The actual pairing logic is inside RingDeviceManager.
                    val pairingUiJob = launch {
                        deviceManager.pairingState.collect { state ->
                            when (state) {
                                is PairingState.PairingRequested -> {
                                    Timber.i("📲 Pairing dialog shown")
                                    _uiState.value = DeviceConnectionUiState.WaitingForPairing
                                }
                                is PairingState.PairingFailed -> {
                                    Timber.w("❌ Pairing failed: ${state.error}")
                                    // Don't throw — let main flow handle via initializationComplete
                                }
                                else -> { /* no UI change needed */ }
                            }
                        }
                    }

                    // ── Step 3: Wait for initialization complete ──
                    // initializationComplete.first() now PROPERLY waits because
                    // RingDeviceManager resets the replay cache on new connection.
                    // It fires AFTER:
                    //   setNoticeStatus → pair check → (optional pairing) → initializeDevice
                    Timber.i("⏳ Waiting for device initialization...")
                    deviceManager.initializationComplete.first()
                    Timber.i("✅ initializationComplete received")

                    // Cancel pairing UI observer
                    pairingUiJob.cancel()

                    // ── Step 4: Verify we're actually connected AND paired ──
                    // If pairing failed, initializationComplete still fires
                    // (to unblock us), but isPaired will be false.
                    val isConnected = deviceManager.connectionState.value == ConnectionState.CONNECTED
                    val isPaired = deviceManager.isPaired.value
                    val pairState = deviceManager.pairingState.value

                    Timber.i("📊 Post-init state: connected=$isConnected, paired=$isPaired, pairingState=$pairState")

                    if (!isConnected) {
                        throw Exception("دستگاه متصل نشد")
                    }

                    if (!isPaired) {
                        // Pairing was needed but failed
                        val reason = if (pairState is PairingState.PairingFailed) {
                            pairState.error
                        } else {
                            "جفت‌سازی انجام نشد"
                        }
                        throw Exception(reason)
                    }

                    // ── Step 5: Register with backend ──
                    _uiState.value = DeviceConnectionUiState.Initializing

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

                            // Set userId on ring immediately after registration —
                            // ring is guaranteed fresh after pairing so no need to check first.
                            val userIdForRing = result.data.id.toLong().coerceIn(1L, 4294967294L)
                            deviceManager.setUserId(userIdForRing)
                            Timber.i("👤 Ring userId set right after pairing: $userIdForRing")
                        }
                        is AuthResult.Error -> {
                            Timber.w("⚠️ Backend registration failed: ${result.message}")
                            userPreferences.saveDeviceInfo(mac, name)
                        }
                    }

                    // ── Step 6: Show success, then navigate ──
                    _uiState.value = DeviceConnectionUiState.Connected
                    Timber.i("🎉 Connection complete!")
                    delay(1200)
                    _uiState.value = DeviceConnectionUiState.ReadyToNavigate
                }
            } catch (e: TimeoutCancellationException) {
                Timber.e("⏱️ Connection timeout")
                handleConnectionFailure(device, "زمان اتصال تمام شد. لطفا دوباره تلاش کنید")
            } catch (e: Exception) {
                Timber.e(e, "❌ Connection error")
                handleConnectionFailure(device, "خطا در اتصال: ${e.message ?: "نامشخص"}")
            }
        }
    }

    /**
     * On failure, disconnect cleanly then auto-retry once before surfacing an error.
     * Pairing very often drops the BLE link, and a second attempt right after the
     * bond exists usually succeeds — so we keep the spinner alive across one silent
     * retry instead of dropping the user back to the list with a flashing error.
     */
    private fun handleConnectionFailure(device: ScanDeviceInfo, message: String) {
        deviceManager.disconnect()
        if (connectAttempt < MAX_CONNECT_ATTEMPTS) {
            connectAttempt++
            Timber.w("🔁 Connection failed — auto-retry $connectAttempt/$MAX_CONNECT_ATTEMPTS")
            viewModelScope.launch {
                delay(RETRY_DELAY_MS)
                attemptConnection(device)
            }
        } else {
            Timber.e("❌ Connection failed after $MAX_CONNECT_ATTEMPTS attempts")
            _uiState.value = DeviceConnectionUiState.Error(message)
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

    companion object {
        // Per-attempt cap. The RingDeviceManager has its own internal watchdogs
        // (≤90 s) that unblock us well before this, so this is just a backstop.
        private const val CONNECTION_TIMEOUT_MS = 100_000L
        // On some phones the bond only "sticks" after the pairing attempt fails;
        // the next attempt then finds the ring already bonded and connects cleanly.
        // 3 attempts lets that recovery happen automatically instead of the user
        // having to back out and re-tap the device a third time.
        private const val MAX_CONNECT_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1_500L
    }
}