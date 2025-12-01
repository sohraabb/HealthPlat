package com.bonyad.healthplat.blesdk.manager

import android.annotation.SuppressLint
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
import kotlin.coroutines.cancellation.CancellationException
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

    private val _initializationComplete = MutableSharedFlow<Unit>(replay = 0)
    override val initializationComplete: SharedFlow<Unit> = _initializationComplete.asSharedFlow()

    private var connectionReceiver: BroadcastReceiver? = null

    private var batteryUpdateJob: Job? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Ensure SDK is initialized (Application already called init, this is defensive)
        try {
            manager.initContext(context)
        } catch (t: Throwable) {
            Timber.w(t, "Ring initContext from BonlalaDeviceManager")
        }

        setupConnectionBroadcastReceiver()
        setupRealTimeDataListener()
        setupGlobalConnectionListener()
    }

    private fun setupConnectionBroadcastReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(BleConstant.CONNECTED_ACTION)
            addAction(BleConstant.DIS_CONNECTED_ACTION)
        }

        connectionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BleConstant.DIS_CONNECTED_ACTION -> {
                        _connectionState.value = ConnectionState.DISCONNECTED
                        _batteryLevel.value = null
                        Timber.i("Device disconnected (broadcast)")
                    }

                    BleConstant.CONNECTED_ACTION -> {
                        _connectionState.value = ConnectionState.CONNECTED
                        initializeDevice()
                        Timber.i("Device connected (broadcast)")
                    }
                }
            }
        }

        // FIX: Add proper flag based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                connectionReceiver,
                intentFilter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(
                connectionReceiver,
                intentFilter,
                null,
                null
            )
        } else {
            context.registerReceiver(connectionReceiver, intentFilter)
        }
    }

    private fun setupGlobalConnectionListener() {
        try {
            BonlalaOperateManager.getInstance().setBleConnStatusListener { mac, status ->
                when (status) {
                    0 -> {
                        // Disconnected
                        _connectionState.value = ConnectionState.DISCONNECTED
                        _batteryLevel.value = null
                        _firmwareVersion.value = null
                        Timber.i("Global listener: Disconnected from $mac")
                    }

                    1 -> {
                        // Connected
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
            // setRealTimeData or setRealTimeDataListener
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
                Timber.d("Real-time data - HR: $heart, Steps: $countStep")
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
                val deviceName = device.bluetoothDevice?.name
                val deviceAddress = device.bluetoothDevice?.address

                Timber.i("Scan found: [$deviceAddress] Name: [$deviceName]")

                val isValidRing = !deviceName.isNullOrBlank() && (
                        deviceName.contains("ring", ignoreCase = true) ||
                                deviceName.contains("bonlala", ignoreCase = true) ||
                                deviceName.startsWith("W", ignoreCase = true)
                        )

                if (deviceAddress != null && isValidRing) {
                    _scannedDevices.update { currentList ->
                        if (currentList.none { it.bluetoothDevice?.address == deviceAddress }) {
                            currentList + device
                        } else {
                            currentList
                        }
                    }
                    Timber.i("Added ring device: $deviceName")
                } else {
                    Timber.d("Skipped device: $deviceName (doesn't match pattern)")
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
    Timber.i("🔵 connect() called with MAC: $deviceMac")

    // 1. SDK Recommendation: Always stop scan before connecting


    _connectionState.value = ConnectionState.CONNECTING
    stopScan()

    try {
        // 2. Using the method taking String MAC
        manager.connDevice("", deviceMac, object : ConnStatusListener {
            override fun connStatus(status: Int) {
                Timber.i("connStatus callback: $status")
                // NOTE: We do NOT set DISCONNECTED here immediately if status is 0.
                // The SDK often fires '0' during the handshake or channel setup.
                // We rely on the BroadcastReceiver or setNoticeStatus for final confirmation.

                if (status == 1) {
                    // If we get explicit success here, great.
                    if (_connectionState.value != ConnectionState.CONNECTED) {
                        _connectionState.value = ConnectionState.CONNECTED
                        initializeDevice()
                    }
                }
            }

            override fun setNoticeStatus(noticeStatus: Int) {
                Timber.i("📢 setNoticeStatus triggered: $noticeStatus")
                // SDK Sample Logic: This is the TRUE indicator that the data channel is open.
                // Even if noticeStatus is 0, the SDK sample treats this as "Success".

                if (_connectionState.value != ConnectionState.CONNECTED) {
                    Timber.i("✅ Connection fully established via NoticeStatus")
                    _connectionState.value = ConnectionState.CONNECTED

                    // 3. Start your initialization sequence here
                    initializeDevice()
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
}

override fun openRealTimeHeartRate() {
    try {
        manager.openRealTimeHeartRateSwitch { isSuccess ->
            if (isSuccess) {
                Timber.i("Real-time heart rate monitoring started")
            } else {
                Timber.w("Failed to start real-time heart rate monitoring")
            }
        }
    } catch (t: Throwable) {
        Timber.e(t, "openRealTimeHeartRate failed")
    }
}

override fun closeRealTimeHeartRate() {
    try {
        manager.closeRealTimeHeartRateSwitch { isSuccess ->
            if (isSuccess) {
                Timber.i("Real-time heart rate monitoring stopped")
            } else {
                Timber.w("Failed to stop real-time heart rate monitoring")
            }
        }
    } catch (t: Throwable) {
        Timber.e(t, "closeRealTimeHeartRate failed")
    }
}

override fun readBatteryLevel() {
    val currentState = _connectionState.value
    Timber.d("🔋 readBatteryLevel() called, connection state: $currentState")

    if (currentState != ConnectionState.CONNECTED) {
        Timber.w("🔋 Cannot read battery: device not connected (state: $currentState)")
        return
    }

    try {
        BonlalaOperateManager.getInstance().getDeviceBattery { battery ->
            Timber.d("🔋 Battery callback received: $battery")
            if (battery != null && battery >= 0) {
                _batteryLevel.value = battery
                Timber.i("🔋 ✅ Battery level updated: $battery%")
            } else {
                Timber.w("🔋 ⚠️ Invalid battery value: $battery")
            }
        }
    } catch (t: Throwable) {
        Timber.e(t, "🔋 ❌ readBattery failed")
    }
}

/*
override fun readBatteryLevel() {
    try {
        BonlalaOperateManager.getInstance().getDeviceBattery { battery ->
            if (battery > 0) {
                _batteryLevel.value = battery
                Timber.d("Battery level read: $battery%")
            } else {
                Timber.w("Invalid battery value: $battery")
                scope.launch {
                    delay(2000)
                    readBatteryLevel()
                }
            }
        }
    } catch (t: Throwable) {
        Timber.e(t, "readBattery failed")
    }
}

 */

override fun startMeasureSpo2() {
    try {
        manager.startMeasureSpo2()
        Timber.i("SpO2 measurement started")
    } catch (t: Throwable) {
        Timber.e(t, "startMeasureSpo2 failed")
    }
}

override suspend fun getFirmwareVersion(): String? = suspendCoroutine { cont ->
    try {
        BonlalaOperateManager.getInstance().getDeviceFirmwareVersion { version ->
            // Resume with the actual version
            Timber.d("Firmware version: $version")
            _firmwareVersion.value = version
            cont.resume(version)
        }
    } catch (t: Throwable) {
        Timber.e(t, "getFirmwareVersion failed")
        cont.resume(null)
    }
}

override suspend fun getDeviceInfo(): Result<DeviceInfoBean> = suspendCoroutine { cont ->
    try {
        BonlalaOperateManager.getInstance().getDeviceInfoData { deviceInfo ->
            cont.resume(Result.success(deviceInfo))
        }
    } catch (t: Throwable) {
        cont.resume(Result.failure(t))
    }
}

override suspend fun getRecordData(day: Int): RecordDataResult {
    Timber.i("📊 Requesting record data for day: $day")

    // 1. Validate connection state
    if (_connectionState.value != ConnectionState.CONNECTED) {
        Timber.w("❌ Cannot get record data - device not connected")
        return RecordDataResult.Error(-1)
    }

    // 2. Check device is ready (not busy, channel open)
    val deviceInfo = getDeviceInfo().getOrNull()
    val deviceState = deviceInfo?.deviceState

    when (deviceState) {
        null, 0 -> {
            Timber.d("✅ Device ready (state: $deviceState)")
        }

        1 -> {
            Timber.w("⚠️ Device state: Invalid message")
//                return RecordDataResult.Error(-2)
        }

        2 -> {
            Timber.w("⚠️ Device state: Battery is low")
            return RecordDataResult.Error(-2)
        }

        3 -> {
            Timber.w("⚠️ Device state: Data channel is not open")
            return RecordDataResult.Error(-2)
        }

        4 -> {
            Timber.w("⚠️ Device state: Device is requesting a communication interval")
            return RecordDataResult.Error(-2)
        }

        5 -> {
            Timber.w("⚠️ Device state: Device busy")
            return RecordDataResult.Error(-2)
        }

        6 -> {
            Timber.w("⚠️ Device state: No data")
            return RecordDataResult.Error(-2)
        }

        else -> {
            Timber.w("⚠️ Unknown device state: $deviceState")
            return RecordDataResult.Error(-2)
        }
    }

    // 3. Attempt to get record data with timeout
    return try {
        withTimeout(15000L) { // Increased timeout to 15 seconds
            suspendCancellableCoroutine { continuation ->

                BonlalaOperateManager.getInstance().getRecordByData(
                    day,
                    object : OnRecordDataListener {

                        override fun isNoResponseData(dayStr: String, stateCode: Int) {
                            val errorMessage = when (stateCode) {
                                1 -> "Invalid message"
                                2 -> "Battery is low"
                                3 -> "Data channel is not open"
                                4 -> "Device is requesting a communication interval"
                                5 -> "Device busy"
                                6 -> "No data available for this day"
                                else -> "Unknown error code: $stateCode"
                            }

                            Timber.w("⚠️ Record data error for day $dayStr: $errorMessage (code: $stateCode)")

                            if (continuation.isActive) {
                                continuation.resume(RecordDataResult.Error(stateCode))
                            }
                        }

                        override fun backRecordData(
                            recordHeartBean: RecordHeartRateBean?,
                            recordStepBean: RecordStepBean?,
                            recordSleepBean: RecordSleepBean?,
                            recordSleepStepBean: RecordSleepStepBean,
                            recordStressBean: RecordStressBean?,
                            recordSpo2Bean: RecordSpo2Bean?,
                            recordHrvBean: RecordHrvBean?,
                            recordSleepActivityBean: RecordSleepActivityBean,
                        ) {
                            Timber.i("📥 Ring Record Data Received for day $day")
                            Timber.i("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                            // Log Heart Rate
                            if (recordHeartBean != null) {
                                val validHeartData =
                                    recordHeartBean.heartRateSource?.count { it > 0 } ?: 0
                                Timber.i("❤️ Heart Rate:")
                                Timber.i("   Date: ${recordHeartBean.recordDay}")
                                Timber.i("   Valid readings: $validHeartData/1440")
                                Timber.i("   Data: ${recordHeartBean.heartRateSource?.take(10)}... (showing first 10)")
                            } else {
                                Timber.i("❤️ Heart Rate: null")
                            }

                            // Log Steps
                            if (recordStepBean != null) {
                                val totalSteps = recordStepBean.stepSource?.sum() ?: 0
                                val validStepData =
                                    recordStepBean.stepSource?.count { it > 0 } ?: 0
                                Timber.i("🦶 Steps:")
                                Timber.i("   Date: ${recordStepBean.recordDay}")
                                Timber.i("   Total steps: $totalSteps")
                                Timber.i("   Valid readings: $validStepData/${recordStepBean.stepSource?.size}")
                                Timber.i("   Data: ${recordStepBean.stepSource?.take(10)}... (showing first 10)")
                            } else {
                                Timber.i("🦶 Steps: null")
                            }

                            // Log Sleep
                            if (recordSleepBean != null) {
                                val sleepMinutes =
                                    recordSleepBean.sourceList?.count { it in 1..4 } ?: 0
                                val deepSleep =
                                    recordSleepBean.sourceList?.count { it == 1 } ?: 0
                                val lightSleep =
                                    recordSleepBean.sourceList?.count { it == 2 } ?: 0
                                val awakeSleep =
                                    recordSleepBean.sourceList?.count { it == 3 } ?: 0
                                val rem = recordSleepBean.sourceList?.count { it == 4 } ?: 0

                                Timber.i("😴 Sleep:")
                                Timber.i("   Date: ${recordSleepBean.recordDay}")
                                Timber.i("   Total sleep: ${sleepMinutes} minutes (${sleepMinutes / 60f} hours)")
                                Timber.i("   Deep sleep: $deepSleep min")
                                Timber.i("   Light sleep: $lightSleep min")
                                Timber.i("   Awake: $awakeSleep min")
                                Timber.i("   REM: $rem min")
                                Timber.i("   Data: ${recordSleepBean.sourceList?.take(10)}... (showing first 10)")
                            } else {
                                Timber.i("😴 Sleep: null")
                            }

                            // Log Stress
                            if (recordStressBean != null) {
                                val validStress =
                                    recordStressBean.stressSource?.count { it > 0 } ?: 0
                                val avgStress = recordStressBean.stressSource
                                    ?.filter { it > 0 }
                                    ?.average()
                                    ?.toInt() ?: 0

                                Timber.i("😣 Stress:")
                                Timber.i("   Date: ${recordStressBean.recordDay}")
                                Timber.i("   Valid readings: $validStress")
                                Timber.i("   Average stress: $avgStress")
                                Timber.i("   Data: ${recordStressBean.stressSource?.take(10)}... (showing first 10)")
                            } else {
                                Timber.i("😣 Stress: null")
                            }

                            // Log SpO2
                            if (recordSpo2Bean != null) {
                                val validSpo2 = recordSpo2Bean.sourceList?.count { it > 0 } ?: 0
                                val avgSpo2 = recordSpo2Bean.sourceList
                                    ?.filter { it > 0 }
                                    ?.average()
                                    ?.toInt() ?: 0

                                Timber.i("🩸 SpO₂:")
                                Timber.i("   Date: ${recordSpo2Bean.recordDay}")
                                Timber.i("   Valid readings: $validSpo2")
                                Timber.i("   Average SpO₂: $avgSpo2%")
                                Timber.i("   Data: ${recordSpo2Bean.sourceList?.take(10)}... (showing first 10)")
                            } else {
                                Timber.i("🩸 SpO₂: null")
                            }

                            // Log HRV
                            if (recordHrvBean != null) {
                                val validHrv = recordHrvBean.hrvSource?.count { it > 0 } ?: 0
                                val avgHrv = recordHrvBean.hrvSource
                                    ?.filter { it > 0 }
                                    ?.average()
                                    ?.toInt() ?: 0

                                Timber.i("💓 HRV:")
                                Timber.i("   Date: ${recordHrvBean.recordDay}")
                                Timber.i("   Valid readings: $validHrv")
                                Timber.i("   Average HRV: $avgHrv ms")
                                Timber.i("   Data: ${recordHrvBean.hrvSource?.take(10)}... (showing first 10)")
                            } else {
                                Timber.i("💓 HRV: null")
                            }

                            // Log Sleep Step (if available)
                            Timber.i("💤 Sleep Step Bean: ${recordSleepStepBean}")

                            // Log Sleep Activity (if available)
                            Timber.i("💤 Sleep Activity Bean: ${recordSleepActivityBean}")

                            Timber.i("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                            // Resume coroutine with success
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
                                Timber.i("✅ Record data successfully returned for day $day")
                            } else {
                                Timber.w("⚠️ Continuation no longer active, data discarded")
                            }
                        }
                    }
                )
            }
        }
    } catch (e: TimeoutCancellationException) {
        Timber.e("⏱️ Timeout getting record data for day $day after 15 seconds")
        RecordDataResult.Error(-99)
    } catch (e: CancellationException) {
        Timber.w("🚫 Record data request cancelled for day $day")
        RecordDataResult.Error(-98)
    } catch (e: Exception) {
        Timber.e(e, "❌ Unexpected error getting record data for day $day")
        RecordDataResult.Error(-1)
    }
}

override fun setUserId(userId: Long) {
    try {
        manager.setUserInfo(userId.toInt()) { isSuccess ->
            if (isSuccess) {
                Timber.i("User ID set successfully: $userId")
            } else {
                Timber.w("Failed to set user ID: $userId")
            }
        }
    } catch (t: Throwable) {
        Timber.e(t, "setUserId failed")
    }
}

override fun syncDeviceTime() {
    try {
        manager.setDeviceTime { isSuccess ->
            if (isSuccess) {
                Timber.i("Device time synced successfully")
            } else {
                Timber.w("Failed to sync device time")
            }
        }
    } catch (t: Throwable) {
        Timber.e(t, "syncDeviceTime failed")
    }
}

// Generic characteristic read/write (optional; SDK may have helper methods)
override suspend fun readCharacteristic(uuid: UUID): Result<ByteArray> {
    return Result.failure(NotImplementedError("readCharacteristic not implemented"))
}

override suspend fun writeCharacteristic(
    uuid: UUID,
    data: ByteArray
): Result<Unit> {
    return Result.failure(NotImplementedError("writeCharacteristic not implemented"))
}

override fun cleanup() {
    if (!scope.isActive) return
    try {
        batteryUpdateJob?.cancel()
        closeRealTimeHeartRate()
        manager.clearRealTimeDataListener()
        try {
            connectionReceiver?.let { receiver ->
                context.unregisterReceiver(receiver)
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
            delay(60_000) // Update every 60 seconds
            try {
                readBatteryLevel()
            } catch (t: Throwable) {
                Timber.w(t, "Periodic battery update failed")
            }
        }
    }
}

private fun logAny(tag: String, value: Any?) {
    Timber.i("$tag = ${value?.toString() ?: "null"}")
}

private fun initializeDevice() {
    scope.launch {
        Timber.i("🚀 Starting Device Initialization")

        // 1. Setup Listener (Crucial for receiving data!)
        setupRealTimeDataListener()

        delay(1000) // Small settle time

        try {
            // 2. Sync Time (Must be done BEFORE starting sensors)
            Timber.i("⚙️ Syncing device time...")
            syncDeviceTime()
            delay(1500) // Give it time to write to flash

            // 3. Battery
            Timber.i("⚙️ Reading battery level...")
            readBatteryLevel()
            delay(500)

            // 4. NOW start the sensors
            Timber.i("⚙️ Opening real-time heart rate...")
            openRealTimeHeartRate()
            delay(500)

            Timber.i("⚙️ Getting firmware version...")
            getFirmwareVersion()
            delay(500)

            // Start periodic updates
//                startPeriodicBatteryUpdates()

            Timber.i("✅✅✅ Device fully initialized and ready for data sync ✅✅✅")

            // Signal that initialization is complete
            _initializationComplete.emit(Unit)

        } catch (t: Throwable) {
            Timber.e(t, "❌ Error during device initialization")
            _initializationComplete.emit(Unit) // Emit anyway to unblock
        }
    }
}
}