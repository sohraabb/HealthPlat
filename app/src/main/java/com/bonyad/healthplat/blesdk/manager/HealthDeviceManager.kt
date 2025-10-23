package com.bonyad.healthplat.blesdk.manager

import com.bonyad.healthplat.blesdk.model.ConnectionState
import com.bonyad.healthplat.blesdk.model.RealTimeData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

interface HealthDeviceManager {
    val connectionState: StateFlow<ConnectionState>
    val realTimeData: Flow<RealTimeData>

    fun startScan()
    fun stopScan()

    fun connect(deviceMac: String)
    fun disconnect()

    fun openRealTimeHeartRate()
    fun closeRealTimeHeartRate()

    fun setUserId(userId: Long)
    fun syncDeviceTime()

    suspend fun readCharacteristic(uuid: UUID): Result<ByteArray>
    suspend fun writeCharacteristic(uuid: UUID, data: ByteArray): Result<Unit>
}