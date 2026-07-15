package com.bonyad.healthplat.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response payload from the sleep analysis API:
 * GET /sleep-analysis/{user_id}/{date}
 *
 * All list fields are indexed by sleep session.
 * Session type: 0 = Nap, 1 = Main Sleep.
 */
@Serializable
data class SleepAnalysisData(
    @SerialName("total_durations")
    val totalDurations: List<Int>,

    @SerialName("net_durations")
    val netDurations: List<Int>,

    @SerialName("per_stage_durations")
    val perStageDurations: List<Map<String, Int>>,

    @SerialName("types")
    val types: List<Int>,

    @SerialName("timings")
    val timings: List<Int?>,

    @SerialName("efficiencies")
    val efficiencies: List<Int>,

    @SerialName("restfulnesses")
    val restfulnesses: List<Int>,

    @SerialName("latencies")
    val latencies: List<Int>,

    @SerialName("debts")
    val debt: List<Int>,

    @SerialName("stages")
    val stages: List<List<Int>>,

    @SerialName("timestamps")
    val timestamps: List<List<String>>,

    @SerialName("hr")
    val hr: List<List<Int>>,

    @SerialName("hrv")
    val hrv: List<List<Int>>,

    @SerialName("spo2")
    val spo2: List<List<Int>>,

    @SerialName("breathing_irregularity")
    val breathingIrregularity: List<Int>
)
