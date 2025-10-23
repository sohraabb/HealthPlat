package com.bonyad.healthplat.blesdk.manager

import android.annotation.SuppressLint
import android.content.Context
import com.bonlala.bonlalable.BonlalaOperateManager
import com.bonlala.bonlalable.bean.ScanDeviceInfo
import com.bonlala.bonlalable.listener.ConnStatusListener
import com.bonlala.bonlalable.listener.OnBatteryListener
import com.bonlala.bonlalable.listener.OnRealTimeDataListener
import com.bonlala.bonlalable.listener.OnScanListener
import com.bonlala.bonlalable.listener.OnWriteDataStatusListener
import com.bonlala.bonlalable.listener.WriteCommBackListener
import com.bonyad.healthplat.blesdk.model.ConnectionState
import com.bonyad.healthplat.blesdk.model.RealTimeData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Dispatcher
import timber.log.Timber
import java.sql.Connection
import java.util.UUID

class RingDeviceManager(private val context: Context): HealthDeviceManager {

    private val manager = BonlalaOperateManager.getInstance()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _realTimeData = MutableSharedFlow<RealTimeData>(replay = 1)
    override val realTimeData: Flow<RealTimeData> = _realTimeData.asSharedFlow()

    private val _scannedDevices = MutableStateFlow<List<ScanDeviceInfo>>(emptyList())
    val scannedDevices: StateFlow<List<ScanDeviceInfo>> = _scannedDevices.asStateFlow()

    private val _batteryLevel = MutableStateFlow<Int?>(null)
    override val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    init {
        // Ensure SDK is initialized (Application already called init, this is defensive)
        try {
            manager.initContext(context)
        } catch (t: Throwable) {
            Timber.w(t, "Ring initContext from BonlalaDeviceManager")
        }
        setupRealTimeDataListener()
    }


    private fun setupRealTimeDataListener() {
        try {
            manager.setRealTimeDataListener { heartRate, steps ->
                scope.launch {
                    _realTimeData.emit(
                        RealTimeData(
                            heart = heartRate,
                            step = steps,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                Timber.d("Real-time data - HR: $heartRate, Steps: $steps")
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
                    _scannedDevices.update { currentList ->
                        val existingDevice = currentList.find {
                            it.bluetoothDevice?.address == device.bluetoothDevice?.address
                        }
                        if (existingDevice == null) {
                            currentList + device
                        } else {
                            currentList
                        }
                    }
                    Timber.i("Found device: ${device.bluetoothDevice?.address} - ${device.bluetoothDevice?.name}")
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
            }, 1500,1)
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
        _connectionState.value = ConnectionState.CONNECTING

        try {
            manager.connDevice("", deviceMac, object : ConnStatusListener {
                override fun connStatus(status: Int) {
                    when (status) {
                        1 -> {
                            _connectionState.value = ConnectionState.CONNECTED
                            Timber.i("Connected to $deviceMac")
                            // Initialize device after connection
                            initializeDevice()
                        }
                        0 -> {
                            _connectionState.value = ConnectionState.DISCONNECTED
                            Timber.i("Disconnected from $deviceMac")
                        }
                        else -> {
                            Timber.i("Connection status $status for $deviceMac")
                        }
                    }
                }

                override fun setNoticeStatus(noticeStatus: Int) {
                    // Called when data channel is opened
                    Timber.i("Set notice status: $noticeStatus")
                    if (noticeStatus == 1) {
                        // Data channel ready, can start real-time monitoring
                        Timber.i("Data channel ready")
                    }
                }
            })
        } catch (t: Throwable) {
            Timber.e(t, "Error connecting to device $deviceMac")
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    override fun disconnect() {
        try {
            closeRealTimeHeartRate()
            manager.disConnDevice()
        } catch (t: Throwable) {
            Timber.w(t, "disConnDevice failed")
        }
        _connectionState.value = ConnectionState.DISCONNECTED
        _batteryLevel.value = null
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
        try {
            manager.readBattery(object : WriteCommBackListener {
                override fun backCommData(vararg data: Int) {
                    if (data.isNotEmpty()) {
                        _batteryLevel.value = data[0]
                        Timber.d("Battery level read: ${data[0]}%")
                    }
                }

                override fun backCommStrDate(vararg data: String?) {
                    // Not used for battery reading
                }
            })
        } catch (t: Throwable) {
            Timber.w(t, "readBattery failed")
        }
    }

    override fun startMeasureSpo2() {
        try {
            manager.startMeasureSpo2()
            Timber.i("SpO2 measurement started")
        } catch (t: Throwable) {
            Timber.e(t, "startMeasureSpo2 failed")
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
            closeRealTimeHeartRate()
            manager.clearRealTimeDataListener()
            disconnect()
        } catch (t: Throwable) {
            Timber.w(t, "Cleanup failed")
        } finally {
            scope.cancel()
        }
    }

    private fun initializeDevice() {
        // Sync time and read battery after connection
        scope.launch {
            delay(500) // Small delay to ensure connection is stable
            syncDeviceTime()
            readBatteryLevel()
        }
    }
}