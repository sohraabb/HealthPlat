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
    // ─────────────────────────────────────────────────────────────────────────
    // CHANGE D: Added a distinct "ReadyToNavigate" state separate from Connected.
    //
    // WHY: Previously Connected immediately triggered onDeviceConnected() with
    // zero visual feedback. The user just saw the screen vanish. Now we show
    // a success indicator for 1 second, THEN navigate. ReadyToNavigate is what
    // the screen uses to fire navigation — Connected shows the success UI.
    // ─────────────────────────────────────────────────────────────────────────
    object Connected : DeviceConnectionUiState()       // show success UI
    object ReadyToNavigate : DeviceConnectionUiState() // trigger navigation
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

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: ScanDeviceInfo) {
        val mac = device.bluetoothDevice?.address
        val name = device.bluetoothDevice?.name

        if (mac == null) {
            _uiState.value = DeviceConnectionUiState.Error("آدرس دستگاه نامعتبر است")
            return
        }

        connectionJob?.cancel()
        stopScan()
        _uiState.value = DeviceConnectionUiState.Connecting
        Timber.i("🔌 Connecting to: $mac ($name)")

        connectionJob = viewModelScope.launch {
            try {
                withTimeout(90_000) {

                    // ── Step 1: Initiate BLE connection ───────────────────
                    deviceManager.connect(mac)

                    // ── Step 2: Wait for BLE CONNECTED state ──────────────
                    deviceManager.connectionState
                        .filter { it == ConnectionState.CONNECTED }
                        .first()
                    Timber.i("✅ BLE layer connected")

                    // ─────────────────────────────────────────────────────
                    // CHANGE E: Replace pairingState.collect{} with a
                    // targeted first{} call that terminates cleanly.
                    //
                    // BEFORE: We used collect{} which is an infinite suspending
                    // loop. It never returns on its own — it only exits when
                    // the coroutine is cancelled (timeout or ViewModel cleared).
                    // After setting Connected inside collect, the coroutine kept
                    // running and any future pairingState emission could re-enter
                    // the when branches unexpectedly.
                    //
                    // AFTER: We use first{} which suspends until exactly one
                    // terminal state arrives (Paired or PairingFailed), then
                    // returns. The coroutine continues linearly after that —
                    // no infinite loop, no surprise re-entries.
                    // ─────────────────────────────────────────────────────

                    // ── Step 3: Wait for pairing to resolve ───────────────
                    Timber.i("⏳ Waiting for pairing state...")
                    val pairing = deviceManager.pairingState.first { state ->
                        state is PairingState.PairingRequested ||
                                state is PairingState.Paired ||
                                state is PairingState.PairingFailed
                    }

                    when (pairing) {
                        is PairingState.PairingRequested -> {
                            // User needs to confirm the system pairing dialog
                            Timber.i("📲 Pairing dialog shown — waiting for user")
                            _uiState.value = DeviceConnectionUiState.WaitingForPairing

                            // Now wait for the final outcome
                            val finalPairing = deviceManager.pairingState.first { state ->
                                state is PairingState.Paired ||
                                        state is PairingState.PairingFailed
                            }
                            if (finalPairing is PairingState.PairingFailed) {
                                throw Exception("Pairing failed: ${finalPairing.error}")
                            }
                            Timber.i("✅ Pairing confirmed by user")
                        }
                        is PairingState.PairingFailed -> {
                            throw Exception("Pairing failed: ${pairing.error}")
                        }
                        else -> {
                            Timber.i("✅ Already paired, no dialog needed")
                        }
                    }

                    // ── Step 4: Wait for device initialization ────────────
                    _uiState.value = DeviceConnectionUiState.Initializing
                    Timber.i("⏳ Waiting for device initialization...")
                    deviceManager.initializationComplete.first()
                    Timber.i("✅ Device initialized")

                    // ── Step 5: Register with backend ─────────────────────
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
                        }
                        is AuthResult.Error -> {
                            // Non-fatal: save locally and continue
                            Timber.w("⚠️ Backend registration failed: ${result.message}")
                            userPreferences.saveDeviceInfo(mac, name)
                        }
                    }

                    // ─────────────────────────────────────────────────────
                    // CHANGE D (continued): Show Connected state for 1 second
                    // before triggering navigation. This gives the user visible
                    // feedback that the process completed successfully.
                    // ─────────────────────────────────────────────────────
                    _uiState.value = DeviceConnectionUiState.Connected
                    Timber.i("🎉 Connection complete — showing success UI")
                    delay(1200)
                    _uiState.value = DeviceConnectionUiState.ReadyToNavigate
                }
            } catch (e: TimeoutCancellationException) {
                Timber.e("⏱️ Connection timeout after 90 seconds")
                deviceManager.disconnect()
                _uiState.value = DeviceConnectionUiState.Error("زمان اتصال تمام شد. لطفا دوباره تلاش کنید")
            } catch (e: Exception) {
                Timber.e(e, "❌ Connection error")
                deviceManager.disconnect()
                _uiState.value = DeviceConnectionUiState.Error(
                    "خطا در اتصال: ${e.message ?: "نامشخص"}"
                )
            }
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
}