package com.bonyad.healthplat.domain.model

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