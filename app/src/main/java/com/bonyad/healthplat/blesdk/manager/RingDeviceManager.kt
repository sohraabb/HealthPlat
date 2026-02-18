package com.bonyad.healthplat.blesdk.manager

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
import com.bonlala.bonlalable.listener.OnRealTimeDataListener
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
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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

    // ─────────────────────────────────────────────────────────────────────────
    // CHANGE A: replay = 1 instead of replay = 0
    //
    // WHY: With replay = 0, the "init complete" emission is fire-and-forget.
    // If initializeDevice() finishes BEFORE the ViewModel's coroutine reaches
    // `initializationComplete.first()`, the emission is permanently lost and
    // the ViewModel suspends forever — causing the 90-second timeout hang.
    //
    // With replay = 1, the last emission is cached. Any late subscriber
    // (like the ViewModel) still receives it immediately. This closes the race
    // condition entirely.
    // ─────────────────────────────────────────────────────────────────────────
    private val _initializationComplete = MutableSharedFlow<Unit>(replay = 1)
    override val initializationComplete: SharedFlow<Unit> = _initializationComplete.asSharedFlow()

    private val _isPaired = MutableStateFlow(false)
    override val isPaired: StateFlow<Boolean> = _isPaired.asStateFlow()

    private val _pairingState = MutableStateFlow<PairingState>(PairingState.NotPaired)
    override val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    private var connectionReceiver: BroadcastReceiver? = null
    private var batteryUpdateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var initializationJob: Job? = null

    // ─────────────────────────────────────────────────────────────────────────
    // CHANGE B: AtomicBoolean guard to prevent double-initialization
    //
    // WHY: Both `setNoticeStatus` callback (in connect()) AND the bond
    // broadcast receiver (BOND_BONDED) can trigger initializeDeviceWithPairing()
    // or initializeDevice(). On some devices both fire in close succession,
    // running initialization twice — causing duplicate battery reads, double
    // openRealTimeHeartRate() calls, and double emissions on initializationComplete.
    //
    // AtomicBoolean is used (not just a var) because these callbacks arrive
    // on different threads (BLE callback thread vs. main thread broadcast).
    // ─────────────────────────────────────────────────────────────────────────
    private val isInitializingOrDone = AtomicBoolean(false)

    init {
        try {
            manager.initContext(context)
        } catch (t: Throwable) {
            Timber.w(t, "Ring initContext from RingDeviceManager")
        }
        setupConnectionBroadcastReceiver()
        setupRealTimeDataListener()
        setupGlobalConnectionListener()
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
                        _connectionState.value = ConnectionState.DISCONNECTED
                        _batteryLevel.value = null
                        _isPaired.value = false
                        _pairingState.value = PairingState.NotPaired
                        // ─────────────────────────────────────────────────
                        // CHANGE B (continued): Reset the guard on disconnect
                        // so the next connection attempt can initialize again.
                        // ─────────────────────────────────────────────────
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
                        val device: BluetoothDevice? =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }

                        device?.let {
                            Timber.i("🔐 Bond state changed: $bondState for ${it.address}")
                            when (bondState) {
                                BluetoothDevice.BOND_BONDING -> {
                                    _pairingState.value = PairingState.PairingRequested
                                }
                                BluetoothDevice.BOND_BONDED -> {
                                    Timber.i("✅ Device paired successfully!")
                                    _pairingState.value = PairingState.Paired
                                    _isPaired.value = true

                                    // ─────────────────────────────────────
                                    // CHANGE B (continued): Only call
                                    // initializeDevice() if we haven't
                                    // already started. The AtomicBoolean
                                    // compareAndSet returns true only once,
                                    // so parallel triggers are safely ignored.
                                    // ─────────────────────────────────────
                                    if (isInitializingOrDone.compareAndSet(false, true)) {
                                        scope.launch {
                                            delay(300)
                                            initializeDevice()
                                        }
                                    } else {
                                        Timber.i("🛑 Bond bonded received but init already in progress/done")
                                    }
                                }
                                BluetoothDevice.BOND_NONE -> {
                                    Timber.w("❌ Pairing failed or bond removed")
                                    _pairingState.value = PairingState.PairingFailed("Pairing cancelled or failed")
                                    _isPaired.value = false
                                    isInitializingOrDone.set(false)
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

    private fun setupGlobalConnectionListener() {
        try {
            BonlalaOperateManager.getInstance().setBleConnStatusListener { mac, status ->
                when (status) {
                    0 -> {
                        _connectionState.value = ConnectionState.DISCONNECTED
                        _batteryLevel.value = null
                        _firmwareVersion.value = null
                        _isPaired.value = false
                        _pairingState.value = PairingState.NotPaired
                        isInitializingOrDone.set(false) // CHANGE B: reset on disconnect
                        Timber.i("Global listener: Disconnected from $mac")
                    }
                    1 -> {
                        _connectionState.value = ConnectionState.CONNECTED
                        Timber.i("Global listener: Connected to $mac")
                    }
                }
            }
        } catch (t: Throwable) {
            Timber.e(t, "Failed to setup global connection listener")
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

                    // ─────────────────────────────────────────────────────
                    // CHANGE C: Relaxed scan filter — accept any device with
                    // a valid MAC address.
                    //
                    // BEFORE: We filtered by name (must contain "ring",
                    // "bonlala", or start with "W"). This silently dropped
                    // your ring on phones where the Bluetooth stack hadn't
                    // yet resolved the device name from the advertisement
                    // packet — which is common on first-time encounters on
                    // Android 12+, and varies by OEM/firmware.
                    //
                    // AFTER: We only require a non-null MAC (the only field
                    // guaranteed to always be present in a BLE advertisement).
                    // We still log whether it looks like a ring for debugging.
                    // The UI shows all found devices — the user can see and
                    // pick theirs. Non-ring-looking names get a visual hint
                    // in the UI (see DeviceScanningScreen change).
                    // ─────────────────────────────────────────────────────
                    if (deviceAddress == null) {
                        Timber.d("Skipped device with null MAC")
                        return
                    }

                    val looksLikeRing = !deviceName.isNullOrBlank() && (
                            deviceName.contains("ring", ignoreCase = true) ||
                                    deviceName.contains("bonlala", ignoreCase = true) ||
                                    deviceName.startsWith("W", ignoreCase = false) // case-sensitive: W not w
                            )

                    Timber.i("Scan found: [$deviceAddress] Name: [$deviceName] looksLikeRing=$looksLikeRing")

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

    override fun connect(deviceMac: String) {
        if (_connectionState.value == ConnectionState.CONNECTING ||
            _connectionState.value == ConnectionState.CONNECTED
        ) {
            Timber.i("🛑 Ignoring connect request: Already ${_connectionState.value}")
            return
        }

        Timber.i("🔵 connect() called with MAC: $deviceMac")
        _connectionState.value = ConnectionState.CONNECTING
        _pairingState.value = PairingState.NotPaired
        // CHANGE B: Reset guard at the start of each new connection attempt
        isInitializingOrDone.set(false)
        stopScan()

        try {
            manager.connDevice("", deviceMac, object : ConnStatusListener {
                override fun connStatus(status: Int) {
                    Timber.i("connStatus callback: $status")
                    if (status == 1 && _connectionState.value != ConnectionState.CONNECTED) {
                        _connectionState.value = ConnectionState.CONNECTED
                    }
                }

                override fun setNoticeStatus(noticeStatus: Int) {
                    Timber.i("📢 setNoticeStatus triggered: $noticeStatus")
                    if (_connectionState.value != ConnectionState.CONNECTED) {
                        _connectionState.value = ConnectionState.CONNECTED
                    }
                    // ─────────────────────────────────────────────────────
                    // CHANGE B (continued): Guard here too. setNoticeStatus
                    // and BOND_BONDED broadcast can both fire — only the
                    // first one proceeds.
                    // ─────────────────────────────────────────────────────
                    if (isInitializingOrDone.compareAndSet(false, true)) {
                        initializeDeviceWithPairing()
                    } else {
                        Timber.i("🛑 setNoticeStatus: init already in progress/done, skipping")
                    }
                }
            })
        } catch (t: Throwable) {
            Timber.e(t, "❌ Error connecting to device $deviceMac")
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    override fun disconnect() {
        try {
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
    }

    override fun openRealTimeHeartRate() {
        try {
            manager.openRealTimeHeartRateSwitch { isSuccess ->
                Timber.i(if (isSuccess) "Real-time HR started" else "Failed to start real-time HR")
            }
        } catch (t: Throwable) {
            Timber.e(t, "openRealTimeHeartRate failed")
        }
    }

    override fun closeRealTimeHeartRate() {
        try {
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
                if (battery != null && battery >= 0) {
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

    override suspend fun getFirmwareVersion(): String? = suspendCoroutine { cont ->
        try {
            BonlalaOperateManager.getInstance().getDeviceFirmwareVersion { version ->
                _firmwareVersion.value = version
                cont.resume(version)
            }
        } catch (t: Throwable) {
            Timber.e(t, "getFirmwareVersion failed")
            cont.resume(null)
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
        return retryRecordData(day, attempt = 1)
    }

    private suspend fun retryRecordData(day: Int, attempt: Int): RecordDataResult {
        if (attempt > 5) return RecordDataResult.Error(-99)

        val result = try {
            withTimeout(15000L) {
                suspendCancellableCoroutine<RecordDataResult> { continuation ->
                    BonlalaOperateManager.getInstance().getRecordByData(
                        day,
                        object : OnRecordDataListener {
                            override fun isNoResponseData(dayStr: String, stateCode: Int) {
                                if (continuation.isActive) {
                                    continuation.resume(RecordDataResult.Error(stateCode))
                                }
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
        } catch (e: Exception) {
            RecordDataResult.Error(-1)
        }

        if (result is RecordDataResult.Error && (result.code == 4 || result.code == 5)) {
            delay(1500)
            return retryRecordData(day, attempt + 1)
        }
        return result
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

    private fun initializeDeviceWithPairing() {
        // Note: no isInitializingOrDone check here — callers already do it
        initializationJob?.cancel()
        initializationJob = scope.launch {
            try {
                Timber.i("🚀 Starting Device Initialization with Pairing Check")
                delay(300)

                val deviceInfo = getDeviceInfo().getOrNull()

                if (deviceInfo != null && deviceInfo.isPair) {
                    Timber.i("✅ Device already paired, skipping pairing step")
                    _isPaired.value = true
                    _pairingState.value = PairingState.Paired
                    initializeDevice()
                } else {
                    Timber.i("⚠️ Device not paired — requesting pairing")
                    manager.readDeviceMac { macBytes ->
                        manager.toPairDevice(macBytes)
                        _pairingState.value = PairingState.PairingRequested
                        // isInitializingOrDone stays true here — when BOND_BONDED
                        // fires, it will NOT re-enter initializeDeviceWithPairing.
                        // Instead the bond broadcast directly calls initializeDevice().
                        // We reset the guard so BOND_BONDED can call initializeDevice().
                        isInitializingOrDone.set(false)
                    }
                }
            } catch (t: Throwable) {
                Timber.e(t, "❌ Pairing check failed")
                _pairingState.value = PairingState.PairingFailed(t.message ?: "Unknown error")
                _initializationComplete.emit(Unit)
            }
        }
    }

    private fun initializeDevice() {
        scope.launch {
            try {
                Timber.i("🚀 Starting Device Initialization")
                delay(200)

                val jobs = listOf(
                    launch { syncDeviceTime() },
                    launch { readBatteryLevel() },
                    launch { getFirmwareVersion() }
                )
                jobs.forEach { it.join() }

                delay(200)
                openRealTimeHeartRate()
                delay(100)

                startPeriodicBatteryUpdates()

                Timber.i("✅✅✅ Device fully initialized")
                _initializationComplete.emit(Unit)
            } catch (t: Throwable) {
                Timber.e(t, "❌ Error during device initialization")
                _initializationComplete.emit(Unit) // always emit so ViewModel doesn't hang
            }
        }
    }
}