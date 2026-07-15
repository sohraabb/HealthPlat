package com.bonyad.healthplat.blesdk.manager

import android.bluetooth.BluetoothDevice
import com.bonlala.bonlalable.bean.DeviceInfoBean
import com.bonlala.bonlalable.bean.ScanDeviceInfo
import com.bonyad.healthplat.blesdk.model.ConnectionState
import com.bonyad.healthplat.blesdk.model.PairingState
import com.bonyad.healthplat.blesdk.model.RealTimeData
import com.bonyad.healthplat.domain.model.RecordDataResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

interface HealthDeviceManager {
    val connectionState: StateFlow<ConnectionState>
    val realTimeData: Flow<RealTimeData>
    val scannedDevices: StateFlow<List<ScanDeviceInfo>>
    val batteryLevel: StateFlow<Int?>
    val firmwareVersion: StateFlow<String?>
    val initializationComplete: SharedFlow<Unit>
    val isPaired: StateFlow<Boolean>
    val pairingState: StateFlow<PairingState>

    fun startScan()
    fun stopScan()

    fun connect(device: BluetoothDevice)
    fun reconnect(mac: String)
    fun disconnect()

    fun openRealTimeHeartRate()
    fun closeRealTimeHeartRate()

    fun setUserId(userId: Long)
    fun syncDeviceTime()

    fun readBatteryLevel()

    fun startMeasureSpo2()

    fun cleanup()

    suspend fun getFirmwareVersion(): String?
    suspend fun getDeviceInfo(): Result<DeviceInfoBean>
    suspend fun getRecordData(day: Int): RecordDataResult

    suspend fun readCharacteristic(uuid: UUID): Result<ByteArray>
    suspend fun writeCharacteristic(uuid: UUID, data: ByteArray): Result<Unit>
}