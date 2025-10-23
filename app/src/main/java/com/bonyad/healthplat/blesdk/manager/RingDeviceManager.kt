package com.bonyad.healthplat.blesdk.manager

import android.annotation.SuppressLint
import android.content.Context
import com.bonlala.bonlalable.BonlalaOperateManager
import com.bonlala.bonlalable.bean.ScanDeviceInfo
import com.bonlala.bonlalable.listener.ConnStatusListener
import com.bonlala.bonlalable.listener.OnScanListener
import com.bonlala.bonlalable.listener.OnWriteDataStatusListener
import com.bonyad.healthplat.blesdk.model.ConnectionState
import com.bonyad.healthplat.blesdk.model.RealTimeData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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



    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        // Ensure SDK is initialized (Application already called init, this is defensive)
        try {
            manager.initContext(context)
        } catch (t: Throwable) {
            Timber.w(t, "Ring initContext from BonlalaDeviceManager")
        }
    }


    override fun startScan() {
        _connectionState.value = ConnectionState.SCANNING

        try {
            manager.scanBleDevice(object : OnScanListener {
                override fun onSearchStarted() {
                    Timber.i("Scan started")
                }

                @SuppressLint("MissingPermission")
                override fun onDeviceFounded(p0: ScanDeviceInfo) {
                    // You might want to emit device list via another Flow
                    Timber.i("Found device: ${p0.bluetoothDevice?.address ?: p0.bluetoothDevice?.name}")
                }

                override fun onSearchStopped() {
                    Timber.i("Scan stopped")
                    _connectionState.value = ConnectionState.DISCONNECTED
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
        _connectionState.value = ConnectionState.DISCONNECTED

    }

    override fun connect(deviceMac: String) {
        _connectionState.value = ConnectionState.CONNECTING
        // the SDK has connDevice overloads expecting mac/name or BluetoothDevice
        try {
            manager.connDevice("", deviceMac, object : ConnStatusListener {
                override fun connStatus(status: Int) {
                    if (status == 1) {
                        _connectionState.value = ConnectionState.CONNECTED
                        Timber.i("Connected to $deviceMac")
                    } else {
                        _connectionState.value = ConnectionState.DISCONNECTED
                        Timber.i("Connection status $status for $deviceMac")
                    }
                }

                override fun setNoticeStatus(p0: Int) {
                    // called when data channel opened — you can start real-time reading
                    Timber.i("Set notice status: $p0")
                }
            })
        } catch (t: Throwable) {
            Timber.e(t, "Error connecting to device $deviceMac")
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    override fun disconnect() {
        try {
            manager.disConnDevice()
        } catch (t: Throwable) {
            Timber.w(t, "disConnDevice failed")
        }
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override fun openRealTimeHeartRate() {
        try {
            manager.openRealTimeHeartRateSwitch(object : OnWriteDataStatusListener {
                override fun writeStatus(isSuccess: Boolean) {
                    Timber.i("openRealTimeHeartRate writeStatus=$isSuccess")
                }
            })
        } catch (t: Throwable) {
            Timber.w(t, "openRealTimeHeartRate failed")
        }
    }

    override fun closeRealTimeHeartRate() {
        try {
            manager.closeRealTimeHeartRateSwitch(object : OnWriteDataStatusListener {
                override fun writeStatus(isSuccess: Boolean) {
                    Timber.i("closeRealTimeHeartRate writeStatus=$isSuccess")
                }
            })
        } catch (t: Throwable) {
            Timber.w(t, "closeRealTimeHeartRate failed")
        }
    }

    override fun setUserId(userId: Long) {
        try {
            manager.setUserInfo(userId.toInt(), object : OnWriteDataStatusListener {
                override fun writeStatus(isSuccess: Boolean) {
                    Timber.i("setUserId writeStatus=$isSuccess")
                }
            })
        } catch (t: Throwable) {
            Timber.w(t, "setUserId failed")
        }
    }

    override fun syncDeviceTime() {
        try {
            manager.setDeviceTime(object : OnWriteDataStatusListener {
                override fun writeStatus(isSuccess: Boolean) {
                    Timber.i("syncDeviceTime writeStatus=$isSuccess")
                }
            })
        } catch (t: Throwable) {
            Timber.w(t, "syncDeviceTime failed")
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

}