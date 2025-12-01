package com.bonyad.healthplat.domain.model

import com.bonlala.bonlalable.bean.RecordHeartRateBean
import com.bonlala.bonlalable.bean.RecordHrvBean
import com.bonlala.bonlalable.bean.RecordSleepBean
import com.bonlala.bonlalable.bean.RecordSpo2Bean
import com.bonlala.bonlalable.bean.RecordStepBean
import com.bonlala.bonlalable.bean.RecordStressBean
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class MetricData(
    @SerialName("Id") val id: Int,
    @SerialName("UserId") val userId: String,
    @SerialName("DeviceId") val deviceId: Int,
    @SerialName("RecordDate") val recordDate: String,
    @SerialName("Values") val values: List<Int>,
    @SerialName("LastSyncedTime") val lastSyncedTime: String,
    @SerialName("LastSyncedIndex") val lastSyncedIndex: Int
)

@Serializable
data class MetricRequest(
    @SerialName("UserId") val userId: String,
    @SerialName("DeviceId") val deviceId: Int,
    @SerialName("RecordDate") val recordDate: String,
    @SerialName("Values") val values: List<Int>
)

data class SyncHealthDataRequest(
    val date: String,
    val heartRateData: List<Int>? = null,
    val stepData: List<Int>? = null,
    val sleepData: List<Int>? = null,
    val spo2Data: List<Int>? = null,
    val stressData: List<Int>? = null,
    val hrvData: List<Int>? = null
)

data class SyncHealthDataResponse(
    val success: Boolean,
    val message: String
)

data class HealthHistoryResponse(
    val success: Boolean,
    val data: List<HealthDayData>? = null
)

data class HealthDayData(
    val date: String,
    val steps: Int,
    val avgHeartRate: Int,
    val sleepHours: Float,
    val caloriesBurned: Int
)

data class RealtimeHealthDataResponse(
    val success: Boolean,
    val data: RealtimeData? = null
)

data class RealtimeData(
    val heartRate: Int,
    val steps: Int,
    val timestamp: Long
)

sealed class RecordDataResult {
    data class Success(
        val heartRate: RecordHeartRateBean?,
        val steps: RecordStepBean?,
        val sleep: RecordSleepBean?,
        val stress: RecordStressBean?,
        val spo2: RecordSpo2Bean?,
        val hrv: RecordHrvBean?
    ) : RecordDataResult()

    data class Error(val code: Int) : RecordDataResult()
}