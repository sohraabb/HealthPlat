package com.bonyad.healthplat.blesdk.manager

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresPermission
import com.bonlala.bonlalable.BleConstant
import com.bonlala.bonlalable.BonlalaOperateManager
import com.bonlala.bonlalable.bean.DeviceInfoBean
import com.bonlala.bonlalable.bean.RecordHeartRateBean
import com.bonlala.bonlalable.bean.RecordHrvBean
import com.bonlala.bonlalable.bean.RecordSleepActivityBean
import com.bonlala.bonlalable.bean.RecordSleepBean
import com.bonlala.bonlalable.bean.RecordSleepStepBean
import com.bonlala.bonlalable.bean.RecordSpo2Bean
import com.bonlala.bonlalable.bean.RecordStepBean
import com.bonlala.bonlalable.bean.RecordStressBean
import com.bonlala.bonlalable.bean.ScanDeviceInfo
import com.bonlala.bonlalable.listener.ConnStatusListener
import com.bonlala.bonlalable.listener.OnRecordDataListener
import com.bonlala.bonlalable.listener.OnScanListener
import com.bonyad.healthplat.blesdk.model.ConnectionState
import com.bonyad.healthplat.blesdk.model.PairingState
import com.bonyad.healthplat.blesdk.model.RealTimeData
import com.bonyad.healthplat.domain.model.RecordDataResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume


class RingDeviceManager(private val context: Context) : HealthDeviceManager {

    private val manager = BonlalaOperateManager.getInstance()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _realTimeData = MutableSharedFlow<RealTimeData>(replay = 1)
    override val realTimeData: Flow<RealTimeData> = _realTimeData.asSharedFlow()

    private val _scannedDevices = MutableStateFlow<List<ScanDeviceInfo>>(emptyList())
    override val scannedDevices: StateFlow<List<ScanDeviceInfo>> = _scannedDevices.asStateFlow()

    private val _batteryLevel = MutableStateFlow<Int?>(null)
    override val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    private val _firmwareVersion = MutableStateFlow<String?>(null)
    override val firmwareVersion: StateFlow<String?> = _firmwareVersion.asStateFlow()

    private val _initializationComplete = MutableSharedFlow<Unit>(replay = 1)
    override val initializationComplete: SharedFlow<Unit> = _initializationComplete.asSharedFlow()

    private val _isPaired = MutableStateFlow(false)
    override val isPaired: StateFlow<Boolean> = _isPaired.asStateFlow()

    private val _pairingState = MutableStateFlow<PairingState>(PairingState.NotPaired)
    override val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    private var connectedDevice: BluetoothDevice? = null
    private var connectionReceiver: BroadcastReceiver? = null
    private var batteryUpdateJob: Job? = null
    private var initWatchdogJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isInitializingOrDone = AtomicBoolean(false)
    private val isConnecting = AtomicBoolean(false)
    private val disconnectedDuringPairing = AtomicBoolean(false)
    private val isRealTimeHrActive = AtomicBoolean(false)

    init {
        try {
            manager.initContext(context)
        } catch (t: Throwable) {
            Timber.w(t, "Ring initContext from RingDeviceManager")
        }
        setupConnectionBroadcastReceiver()
        setupRealTimeDataListener()
    }

    // ══════════════════════════════════════════════════════════════════════
    // FIX 1: Reset initializationComplete replay cache on new connection.
    //
    // WHY: replay=1 caches the last emission. When the user returns to
    // the connection screen after a previous success, the ViewModel calls
    //     initializationComplete.first()
    // which IMMEDIATELY returns the stale cached value — before the new
    // connection has even started. Then connectionState is still
    // DISCONNECTED and the ViewModel throws
    // "Device not connected after initialization".
    //
    // resetReplayCache() clears the buffer so first() properly waits.
    // ══════════════════════════════════════════════════════════════════════
    private fun prepareForNewConnection() {
        _initializationComplete.resetReplayCache()
        isInitializingOrDone.set(false)
        isConnecting.set(true)
        disconnectedDuringPairing.set(false)
        _pairingState.value = PairingState.NotPaired
        startInitWatchdog()
    }

    // ══════════════════════════════════════════════════════════════════════
    // Global init watchdog — guarantees the connect/pair/reconnect flow can
    // never leave the caller (ViewModel) hanging on the spinner forever.
    //
    // Several paths (notably the post-pairing reconnect) rely on a *later*
    // SDK callback (setNoticeStatus) to drive initialization and ultimately
    // emit initializationComplete. If that callback never arrives (ring went
    // out of range / stopped advertising while bonding), nothing would unblock
    // the caller. This watchdog force-emits initializationComplete after a
    // bound, so the ViewModel sees we're not connected/paired and surfaces a
    // recoverable error instead of an endless spinner.
    // ══════════════════════════════════════════════════════════════════════
    private fun startInitWatchdog() {
        initWatchdogJob?.cancel()
        initWatchdogJob = scope.launch {
            delay(INIT_WATCHDOG_MS)
            if (_connectionState.value != ConnectionState.CONNECTED || !_isPaired.value) {
                Timber.w("⏰ Init watchdog fired (${INIT_WATCHDOG_MS / 1000}s) — unblocking caller")
                isConnecting.set(false)
                _initializationComplete.emit(Unit)
            }
        }
    }

    private fun setupConnectionBroadcastReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(BleConstant.CONNECTED_ACTION)
            addAction(BleConstant.DIS_CONNECTED_ACTION)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }

        connectionReceiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BleConstant.DIS_CONNECTED_ACTION -> {
                        // ── FIX 2: Guard disconnect during connection/pairing ──
                        // Check BOTH the atomic flag AND the connection state.
                        // Broadcasts from previous attempts can arrive late on
                        // the main thread message queue.
                        if (isConnecting.get() || _connectionState.value == ConnectionState.CONNECTING) {
                            Timber.i("⚠️ Disconnect broadcast during connection — IGNORING (isConnecting=${isConnecting.get()}, state=${_connectionState.value})")
                            disconnectedDuringPairing.set(true)
                            return
                        }
                        _connectionState.value = ConnectionState.DISCONNECTED
                        _batteryLevel.value = null
                        _isPaired.value = false
                        _pairingState.value = PairingState.NotPaired
                        isInitializingOrDone.set(false)
                        Timber.i("Device disconnected (broadcast)")
                    }

                    BleConstant.CONNECTED_ACTION -> {
                        _connectionState.value = ConnectionState.CONNECTED
                        Timber.i("Device connected (broadcast)")
                    }

                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                        val bondState = intent.getIntExtra(
                            BluetoothDevice.EXTRA_BOND_STATE,
                            BluetoothDevice.BOND_NONE
                        )
                        val previousBondState = intent.getIntExtra(
                            BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                            BluetoothDevice.BOND_NONE
                        )
                        val device: BluetoothDevice? =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }

                        device?.let {
                            Timber.i("🔐 Bond state changed: $previousBondState → $bondState for ${it.address}")
                            when (bondState) {
                                BluetoothDevice.BOND_BONDING -> {
                                    _pairingState.value = PairingState.PairingRequested
                                }
                                BluetoothDevice.BOND_BONDED -> {
                                    Timber.i("✅ Device paired via bond broadcast!")
                                    _pairingState.value = PairingState.Paired
                                    _isPaired.value = true
                                }
                                BluetoothDevice.BOND_NONE -> {
                                    // Only treat as failure if we were actually bonding (BONDING → NONE).
                                    // The initial NONE → NONE broadcast fires immediately when
                                    // toPairDevice() is called, before the user sees the dialog.
                                    if (previousBondState == BluetoothDevice.BOND_BONDING) {
                                        Timber.w("❌ Pairing failed (BONDING → NONE)")
                                        _pairingState.value = PairingState.PairingFailed("Pairing cancelled or failed")
                                        _isPaired.value = false
                                    } else {
                                        Timber.i("ℹ️ Bond NONE with previous=$previousBondState — ignoring (not a pairing failure)")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(connectionReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(connectionReceiver, intentFilter)
        }
    }

    private fun setupRealTimeDataListener() {
        try {
            manager.setRealTimeData { heart, countStep ->
                scope.launch {
                    _realTimeData.emit(
                        RealTimeData(
                            heart = heart,
                            step = countStep,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (t: Throwable) {
            Timber.e(t, "Failed to setup real-time data listener")
        }
    }

    // Shared global listener — called by both connect() and reconnect()
    private fun setupGlobalListenerForConnection() {
        try {
            BonlalaOperateManager.getInstance().setBleConnStatusListener { connMac, status ->
                when (status) {
                    0 -> {
                        if (isConnecting.get() || _connectionState.value == ConnectionState.CONNECTING) {
                            Timber.i("⚠️ Global listener disconnect during connection — ignoring")
                            disconnectedDuringPairing.set(true)
                            return@setBleConnStatusListener
                        }
                        _connectionState.value = ConnectionState.DISCONNECTED
                        _batteryLevel.value = null
                        _firmwareVersion.value = null
                        _isPaired.value = false
                        _pairingState.value = PairingState.NotPaired
                        isInitializingOrDone.set(false)
                        Timber.i("Global listener: Disconnected from $connMac")
                    }
                    1 -> {
                        _connectionState.value = ConnectionState.CONNECTED
                        Timber.i("Global listener: Connected to $connMac")
                    }
                }
            }
        } catch (t: Throwable) {
            Timber.e(t, "Failed to setup global connection listener")
        }
    }

    // Shared ConnStatusListener for both connect() and reconnect()
    private fun createConnStatusListener(): ConnStatusListener {
        return object : ConnStatusListener {
            override fun connStatus(status: Int) {
                Timber.i("connStatus callback: $status")
                if (status == 1 && _connectionState.value != ConnectionState.CONNECTED) {
                    _connectionState.value = ConnectionState.CONNECTED
                }
            }

            override fun setNoticeStatus(noticeStatus: Int) {
                Timber.i("📢 setNoticeStatus: $noticeStatus — BLE data channel ready!")

                if (_connectionState.value != ConnectionState.CONNECTED) {
                    _connectionState.value = ConnectionState.CONNECTED
                }

                // ── FIX 3: Check pairing, trigger if needed ──
                if (isInitializingOrDone.compareAndSet(false, true)) {
                    initializeDeviceWithPairCheck()
                } else {
                    Timber.i("🛑 setNoticeStatus: init already in progress/done")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun startScan() {
        _connectionState.value = ConnectionState.SCANNING
        _scannedDevices.value = emptyList()

        try {
            manager.scanBleDevice(object : OnScanListener {
                override fun onSearchStarted() {
                    Timber.i("Scan started")
                }

                override fun onDeviceFounded(device: ScanDeviceInfo) {
                    val deviceAddress = device.bluetoothDevice?.address
                    val deviceName = device.bluetoothDevice?.name

                    if (deviceAddress == null) return

                    Timber.i("Scan found: [$deviceAddress] Name: [$deviceName]")

                    _scannedDevices.update { currentList ->
                        if (currentList.none { it.bluetoothDevice?.address == deviceAddress }) {
                            currentList + device
                        } else {
                            currentList
                        }
                    }
                }

                override fun onSearchStopped() {
                    Timber.i("Scan stopped")
                    if (_connectionState.value == ConnectionState.SCANNING) {
                        _connectionState.value = ConnectionState.DISCONNECTED
                    }
                }

                override fun onSearchCanceled() {
                    Timber.i("Search canceled")
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            }, 6000, 1)
        } catch (t: Throwable) {
            Timber.e(t, "Error starting scan")
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    override fun stopScan() {
        try {
            manager.stopScanDevice()
        } catch (t: Throwable) {
            Timber.w(t, "stopScanDevice failed")
        }
        if (_connectionState.value == ConnectionState.SCANNING) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun connect(device: BluetoothDevice) {
        if (_connectionState.value == ConnectionState.CONNECTING ||
            _connectionState.value == ConnectionState.CONNECTED
        ) {
            Timber.i("🛑 Ignoring connect: Already ${_connectionState.value}")
            return
        }

        val mac = device.address
        Timber.i("🔵 connect() called with device: $mac (${device.name})")
        _connectionState.value = ConnectionState.CONNECTING
        connectedDevice = device
        prepareForNewConnection()
        stopScan()
        setupGlobalListenerForConnection()

        try {
            manager.connDevice(device, createConnStatusListener())
        } catch (t: Throwable) {
            Timber.e(t, "❌ Error connecting to device $mac")
            isConnecting.set(false)
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    @SuppressLint("MissingPermission")
    override fun reconnect(mac: String) {
        if (_connectionState.value == ConnectionState.CONNECTING ||
            _connectionState.value == ConnectionState.CONNECTED
        ) {
            Timber.i("🛑 Ignoring reconnect: Already ${_connectionState.value}")
            return
        }

        Timber.i("🔄 Reconnecting to saved device: $mac")
        _connectionState.value = ConnectionState.CONNECTING
        prepareForNewConnection()
        setupGlobalListenerForConnection()

        // ── Fast path: device is already bonded ──────────────────────────
        // getBondedDevices() returns all paired devices instantly, no scan.
        // The ring only advertises intermittently, so a scan can take 40-60s
        // across multiple 20s windows. Connecting directly via the bond table
        // makes reconnect almost instant.
        val bondedDevice = runCatching {
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)
                ?.adapter
                ?.bondedDevices
                ?.find { it.address == mac }
        }.getOrNull()

        if (bondedDevice != null) {
            Timber.i("⚡ Device already bonded — connecting directly (no scan needed)")
            try {
                manager.connDevice(bondedDevice, createConnStatusListener())
            } catch (t: Throwable) {
                Timber.e(t, "❌ Direct reconnect failed — falling back to scan")
                scanForDevice(mac)
            }
            return
        }

        // ── Slow path: not in bond table yet — scan ───────────────────────
        Timber.i("🔍 Device not bonded — scanning for $mac")
        scanForDevice(mac)
    }

    @SuppressLint("MissingPermission")
    private fun scanForDevice(mac: String) {
        val deviceFound = AtomicBoolean(false)

        try {
            manager.scanBleDevice(object : OnScanListener {
                override fun onSearchStarted() {
                    Timber.i("🔍 Reconnect scan started for $mac")
                }

                @SuppressLint("MissingPermission")
                override fun onDeviceFounded(scanDevice: ScanDeviceInfo) {
                    val device = scanDevice.bluetoothDevice ?: return
                    if (device.address == mac && deviceFound.compareAndSet(false, true)) {
                        Timber.i("✅ Found target device $mac during reconnect scan")
                        manager.stopScanDevice()
                        manager.connDevice(device, createConnStatusListener())
                    }
                }

                override fun onSearchStopped() {
                    if (_connectionState.value == ConnectionState.CONNECTING && !deviceFound.get()) {
                        isConnecting.set(false)
                        _connectionState.value = ConnectionState.DISCONNECTED
                        Timber.w("⚠️ Device $mac not found during reconnect scan")
                    }
                }

                override fun onSearchCanceled() {
                    if (_connectionState.value == ConnectionState.CONNECTING && !deviceFound.get()) {
                        isConnecting.set(false)
                        _connectionState.value = ConnectionState.DISCONNECTED
                    }
                }
            }, 20_000, 1)
        } catch (t: Throwable) {
            Timber.e(t, "❌ Reconnect scan failed")
            isConnecting.set(false)
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    override fun disconnect() {
        isConnecting.set(false)
        isRealTimeHrActive.set(false)
        try {
            initWatchdogJob?.cancel()
            batteryUpdateJob?.cancel()
            closeRealTimeHeartRate()
            manager.disConnDevice()
        } catch (t: Throwable) {
            Timber.w(t, "disConnDevice failed")
        }
        _connectionState.value = ConnectionState.DISCONNECTED
        _batteryLevel.value = null
        _firmwareVersion.value = null
        _isPaired.value = false
        _pairingState.value = PairingState.NotPaired
        isInitializingOrDone.set(false)
        connectedDevice = null
    }

    override fun openRealTimeHeartRate() {
        try {
            isRealTimeHrActive.set(true)
            manager.openRealTimeHeartRateSwitch { isSuccess ->
                Timber.i(if (isSuccess) "Real-time HR started" else "Failed to start real-time HR")
            }
        } catch (t: Throwable) {
            isRealTimeHrActive.set(false)
            Timber.e(t, "openRealTimeHeartRate failed")
        }
    }

    override fun closeRealTimeHeartRate() {
        try {
            isRealTimeHrActive.set(false)
            manager.closeRealTimeHeartRateSwitch { isSuccess ->
                Timber.i(if (isSuccess) "Real-time HR stopped" else "Failed to stop real-time HR")
            }
        } catch (t: Throwable) {
            Timber.e(t, "closeRealTimeHeartRate failed")
        }
    }

    override fun readBatteryLevel() {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            Timber.w("🔋 Cannot read battery: not connected")
            return
        }
        try {
            BonlalaOperateManager.getInstance().getDeviceBattery { battery ->
                if (battery >= 0) {
                    _batteryLevel.value = battery
                    Timber.i("🔋 Battery: $battery%")
                } else {
                    Timber.w("🔋 Invalid battery value: $battery")
                }
            }
        } catch (t: Throwable) {
            Timber.e(t, "🔋 readBattery failed")
        }
    }

    override fun startMeasureSpo2() {
        try {
            manager.startMeasureSpo2()
        } catch (t: Throwable) {
            Timber.e(t, "startMeasureSpo2 failed")
        }
    }

    override suspend fun getFirmwareVersion(): String? {
        return try {
            withTimeout(5_000) {
                suspendCancellableCoroutine { cont ->
                    BonlalaOperateManager.getInstance().getDeviceFirmwareVersion { version ->
                        _firmwareVersion.value = version
                        if (cont.isActive) cont.resume(version)
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Timber.w("getFirmwareVersion timed out")
            null
        } catch (t: Throwable) {
            Timber.e(t, "getFirmwareVersion failed")
            null
        }
    }

    override suspend fun getDeviceInfo(): Result<DeviceInfoBean> =
        suspendCancellableCoroutine { cont ->
            try {
                BonlalaOperateManager.getInstance().getDeviceInfoData { deviceInfo ->
                    if (cont.isActive) {
                        if (deviceInfo != null) {
                            _isPaired.value = deviceInfo.isPair
                            cont.resume(Result.success(deviceInfo))
                        } else {
                            cont.resume(Result.failure(Exception("SDK returned null device info")))
                        }
                    }
                }
            } catch (t: Throwable) {
                if (cont.isActive) cont.resume(Result.failure(t))
            }
        }

    override suspend fun getRecordData(day: Int): RecordDataResult {
        if (!_isPaired.value) return RecordDataResult.Error(-3)
        if (_connectionState.value != ConnectionState.CONNECTED) return RecordDataResult.Error(-1)

        // The ring returns error 4 ("communication interval") when real-time HR
        // streaming is active at the same time as getRecordByData. Pause the
        // stream, fetch, then resume — all transparent to the caller.
        val wasHrActive = isRealTimeHrActive.get()
        if (wasHrActive) {
            Timber.i("⏸ Pausing real-time HR before record fetch (day=$day)")
            closeRealTimeHeartRate()
            delay(500) // let ring close the HR stream before accepting the next command
        }

        return try {
            retryRecordData(day, attempt = 1)
        } finally {
            if (wasHrActive) {
                Timber.i("▶ Resuming real-time HR after record fetch (day=$day)")
                openRealTimeHeartRate()
            }
        }
    }

    private suspend fun retryRecordData(day: Int, attempt: Int): RecordDataResult {
        if (attempt > MAX_SYNC_ATTEMPTS) return RecordDataResult.Error(-99)

        Timber.i("🔄 getRecordData day=$day attempt=$attempt/$MAX_SYNC_ATTEMPTS (timeout=${SYNC_TIMEOUT_MS / 1000}s)")

        val result = try {
            withTimeout(SYNC_TIMEOUT_MS) {
                suspendCancellableCoroutine<RecordDataResult> { continuation ->
                    BonlalaOperateManager.getInstance().getRecordByData(
                        day,
                        object : OnRecordDataListener {
                            override fun isNoResponseData(dayStr: String, stateCode: Int) {
                                Timber.w("⚠️ isNoResponseData day=$dayStr code=$stateCode (attempt $attempt) — ${describeErrorCode(stateCode)}")
                                if (continuation.isActive) continuation.resume(RecordDataResult.Error(stateCode))
                            }

                            override fun backRecordData(
                                recordHeartBean: RecordHeartRateBean?,
                                recordStepBean: RecordStepBean?,
                                recordSleepBean: RecordSleepBean?,
                                recordSleepStepBean: RecordSleepStepBean?,
                                recordStressBean: RecordStressBean?,
                                recordSpo2Bean: RecordSpo2Bean?,
                                recordHrvBean: RecordHrvBean?,
                                recordSleepActivityBean: RecordSleepActivityBean?
                            ) {
                                val hasAnyData = recordHeartBean != null || recordStepBean != null ||
                                        recordSleepBean != null || recordStressBean != null ||
                                        recordSpo2Bean != null || recordHrvBean != null

                                if (!hasAnyData) {
                                    if (continuation.isActive) continuation.resume(RecordDataResult.Error(6))
                                    return
                                }

                                if (continuation.isActive) {
                                    continuation.resume(
                                        RecordDataResult.Success(
                                            heartRate = recordHeartBean,
                                            steps = recordStepBean,
                                            sleep = recordSleepBean,
                                            stress = recordStressBean,
                                            spo2 = recordSpo2Bean,
                                            hrv = recordHrvBean
                                        )
                                    )
                                }
                            }
                        }
                    )
                }
            }
        } catch (e: TimeoutCancellationException) {
            RecordDataResult.Error(ERROR_TIMEOUT)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            RecordDataResult.Error(-1)
        }

        return when {
            result is RecordDataResult.Success -> {
                Timber.i("✅ getRecordData day=$day succeeded on attempt $attempt")
                result
            }
            result is RecordDataResult.Error && result.code in RETRIABLE_ERROR_CODES -> {
                val delayMs = retryDelayFor(result.code, attempt)
                Timber.w("🔁 Retrying day=$day after ${delayMs}ms (code=${result.code}: ${describeErrorCode(result.code)})")
                delay(delayMs)
                retryRecordData(day, attempt + 1)
            }
            else -> {
                Timber.e("❌ getRecordData day=$day failed permanently: code=${(result as? RecordDataResult.Error)?.code}")
                result
            }
        }
    }

    override fun setUserId(userId: Long) {
        try {
            manager.setUserInfo(userId.toInt()) { isSuccess ->
                Timber.i(if (isSuccess) "User ID set: $userId" else "Failed to set user ID")
            }
        } catch (t: Throwable) {
            Timber.e(t, "setUserId failed")
        }
    }

    override fun syncDeviceTime() {
        try {
            manager.setDeviceTime { isSuccess ->
                Timber.i(if (isSuccess) "Time synced" else "Time sync failed")
            }
        } catch (t: Throwable) {
            Timber.e(t, "syncDeviceTime failed")
        }
    }

    override suspend fun readCharacteristic(uuid: UUID): Result<ByteArray> =
        Result.failure(NotImplementedError("readCharacteristic not implemented"))

    override suspend fun writeCharacteristic(uuid: UUID, data: ByteArray): Result<Unit> =
        Result.failure(NotImplementedError("writeCharacteristic not implemented"))

    override fun cleanup() {
        if (!scope.isActive) return
        try {
            batteryUpdateJob?.cancel()
            closeRealTimeHeartRate()
            manager.clearRealTimeDataListener()
            try {
                connectionReceiver?.let {
                    context.unregisterReceiver(it)
                    connectionReceiver = null
                }
            } catch (e: IllegalArgumentException) {
                Timber.w("Receiver already unregistered")
            }
            disconnect()
        } catch (t: Throwable) {
            Timber.w(t, "Cleanup failed")
        } finally {
            scope.cancel()
        }
    }

    private fun startPeriodicBatteryUpdates() {
        batteryUpdateJob?.cancel()
        batteryUpdateJob = scope.launch {
            while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                delay(60_000)
                readBatteryLevel()
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // FIX 3: Check pairing AFTER BLE connection is ready.
    //
    // Called from setNoticeStatus (BLE data channel open).
    //
    // Scenario A — Already bonded (normal reconnect, or second attempt):
    //   getDeviceInfo().isPair == true → skip to initializeDevice()
    //
    // Scenario B — NOT bonded (user "forgot" device, or first time):
    //   getDeviceInfo().isPair == false → trigger toPairDevice()
    //   → user sees system pairing dialog → BOND_BONDED fires
    //   → we detect Paired state → proceed to initializeDevice()
    //
    // This matches the demo where pairing is a POST-CONNECTION step.
    // ══════════════════════════════════════════════════════════════════════
    private fun initializeDeviceWithPairCheck() {
        scope.launch {
            try {
                Timber.i("🚀 setNoticeStatus received — checking pair status...")
                delay(300)

                val deviceInfo = try {
                    withTimeout(5_000) { getDeviceInfo().getOrNull() }
                } catch (e: TimeoutCancellationException) {
                    Timber.w("getDeviceInfo timed out, assuming not paired")
                    null
                }

                if (deviceInfo != null && deviceInfo.isPair) {
                    // ── Scenario A: Already paired ──
                    Timber.i("✅ Device already paired (isPair=true)")
                    _isPaired.value = true
                    _pairingState.value = PairingState.Paired
                    isConnecting.set(false)
                    initializeDevice()
                } else {
                    // ── Scenario B: Not paired — trigger pairing ──
                    Timber.i("⚠️ Device NOT paired — initiating pairing...")
                    _pairingState.value = PairingState.PairingRequested

                    try {
                        manager.readDeviceMac { macBytes ->
                            Timber.i("📱 Got device MAC bytes, calling toPairDevice...")
                            manager.toPairDevice(macBytes)
                        }
                    } catch (t: Throwable) {
                        Timber.e(t, "Failed to initiate pairing")
                        _pairingState.value = PairingState.PairingFailed("Failed to start pairing: ${t.message}")
                        isConnecting.set(false)
                        _initializationComplete.emit(Unit)
                        return@launch
                    }

                    // Wait for BOND_BONDED or failure (up to 60s for user dialog).
                    //
                    // NOTE: BLE disconnects during bonding are NORMAL — many devices
                    // drop the link when the bond table changes. When we detect a
                    // disconnect in the poll loop we break early and reconnect
                    // immediately rather than waiting out the full 60 s timeout.
                    Timber.i("⏳ Waiting for user to confirm pairing...")
                    var pairingSucceeded = false
                    var bleDroppedDuringWait = false
                    try {
                        withTimeout(60_000) {
                            while (true) {
                                // Early-exit: BLE dropped — reconnect immediately instead
                                // of burning through the full 60-second timeout.
                                if (disconnectedDuringPairing.get()) {
                                    Timber.i("🔄 BLE dropped during pairing wait — reconnecting immediately")
                                    bleDroppedDuringWait = true
                                    break
                                }
                                when (val state = _pairingState.value) {
                                    is PairingState.Paired -> {
                                        Timber.i("✅ Pairing confirmed!")
                                        pairingSucceeded = true
                                        break
                                    }
                                    is PairingState.PairingFailed -> {
                                        Timber.w("❌ Pairing failed: ${state.error}")
                                        break
                                    }
                                    else -> {
                                        delay(200)
                                    }
                                }
                            }
                        }
                    } catch (e: TimeoutCancellationException) {
                        Timber.w("⏰ Pairing timed out (60s)")
                        _pairingState.value = PairingState.PairingFailed("Pairing timed out")
                    }

                    // BLE dropped at any point during bonding (before or after BOND_BONDED).
                    // Reconnect — setNoticeStatus will fire again and find isPair=true (Scenario A).
                    if (bleDroppedDuringWait || (pairingSucceeded && disconnectedDuringPairing.get())) {
                        Timber.i("🔄 BLE disconnected during bonding (normal). Reconnecting...")
                        isInitializingOrDone.set(false)
                        disconnectedDuringPairing.set(false)

                        val device = connectedDevice
                        if (device != null) {
                            try {
                                manager.connDevice(device, createConnStatusListener())
                                // Watchdog: the reconnect relies on setNoticeStatus firing
                                // again to drive initialization. If the data channel never
                                // re-opens within the timeout, unblock the caller so the UI
                                // can retry instead of hanging on the spinner.
                                launch {
                                    delay(RECONNECT_AFTER_PAIR_TIMEOUT_MS)
                                    if (!isInitializingOrDone.get() &&
                                        _connectionState.value != ConnectionState.CONNECTED
                                    ) {
                                        Timber.w("⏰ Reconnect-after-pairing timed out (${RECONNECT_AFTER_PAIR_TIMEOUT_MS / 1000}s) — unblocking")
                                        isConnecting.set(false)
                                        _initializationComplete.emit(Unit)
                                    }
                                }
                            } catch (t: Throwable) {
                                Timber.e(t, "Failed to reconnect after pairing")
                                isConnecting.set(false)
                                _initializationComplete.emit(Unit)
                            }
                        } else {
                            Timber.e("❌ No device reference for reconnect after pairing")
                            isConnecting.set(false)
                            _initializationComplete.emit(Unit)
                        }
                        return@launch
                    }

                    if (!pairingSucceeded) {
                        isConnecting.set(false)
                        _initializationComplete.emit(Unit)
                        return@launch
                    }

                    // BLE link survived pairing — proceed normally
                    isConnecting.set(false)
                    initializeDevice()
                }
            } catch (t: Throwable) {
                Timber.e(t, "❌ initializeDeviceWithPairCheck failed")
                isConnecting.set(false)
                _initializationComplete.emit(Unit)
            }
        }
    }

    private fun initializeDevice() {
        scope.launch {
            try {
                Timber.i("🚀 Starting device initialization")
                delay(200)

                val jobs = listOf(
                    launch {
                        try { syncDeviceTime() } catch (t: Throwable) {
                            Timber.w(t, "syncDeviceTime failed during init")
                        }
                    },
                    launch {
                        try { readBatteryLevel() } catch (t: Throwable) {
                            Timber.w(t, "readBatteryLevel failed during init")
                        }
                    },
                    launch {
                        try { getFirmwareVersion() } catch (t: Throwable) {
                            Timber.w(t, "getFirmwareVersion failed during init")
                        }
                    }
                )
                jobs.forEach { it.join() }

                delay(200)
                openRealTimeHeartRate()
                delay(100)

                startPeriodicBatteryUpdates()

                Timber.i("✅✅✅ Device fully initialized")
            } catch (t: Throwable) {
                Timber.e(t, "❌ Error during device initialization")
            } finally {
                _initializationComplete.emit(Unit)
            }
        }
    }

    private fun retryDelayFor(errorCode: Int, attempt: Int): Long {
        return when (errorCode) {
            4    -> 3000L + (attempt * 500L)
            5    -> 2000L
            ERROR_TIMEOUT -> minOf(2000L * attempt, 10_000L)
            else -> 1500L
        }
    }

    companion object {
        // Hard upper bound on the whole connect → pair → reconnect → init flow.
        // If initializationComplete hasn't fired by now, force it so the caller
        // never hangs. Kept comfortably above the 60 s user-pairing-dialog wait.
        private const val INIT_WATCHDOG_MS = 90_000L
        // Bound on the BLE re-link that happens after the bond completes/drops.
        private const val RECONNECT_AFTER_PAIR_TIMEOUT_MS = 25_000L

        private const val MAX_SYNC_ATTEMPTS = 5
        // 45 s gives the ring enough time to transfer ~25 BLE packets even on noisy
        // connections. The SDK demo uses no timeout at all; 25 s was too aggressive
        // and caused spurious retries / the final -99 error on slow transfers.
        private const val SYNC_TIMEOUT_MS  = 45_000L
        private const val ERROR_TIMEOUT    = -98
        private val RETRIABLE_ERROR_CODES = setOf(4, 5, ERROR_TIMEOUT)

        private fun describeErrorCode(code: Int) = when (code) {
            1 -> "invalid message"
            2 -> "battery low"
            3 -> "data channel not open"
            4 -> "device busy — requesting interval"
            5 -> "device busy"
            6 -> "no data"
            else -> "unknown ($code)"
        }
    }
}