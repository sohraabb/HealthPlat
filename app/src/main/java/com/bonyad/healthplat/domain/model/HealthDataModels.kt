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

    data class Error(val code: Int) : RecordDataResult() {
        val message: String
            get() = when (code) {
                -1 -> "خطای عمومی"
                -2 -> "دستگاه آماده نیست"
                -3 -> "دستگاه جفت‌سازی نشده است"
                -98 -> "درخواست لغو شد"
                -99 -> "زمان انتظار تمام شد"
                1 -> "پیام نامعتبر"
                2 -> "باتری کم است"
                3 -> "کانال داده باز نیست"
                4 -> "دستگاه در حال درخواست فاصله ارتباطی"
                5 -> "دستگاه مشغول است"
                6 -> "داده‌ای موجود نیست"
                else -> "خطای ناشناخته: $code"
            }
    }
}