package com.bonyad.healthplat.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object ApiErrorType {
    const val NO_METRICS_FOUND = "NoMetricsFound"
}

@Serializable
data class AiApiResponse<T>(
    @SerialName("IsSuccess")
    val isSuccess: Boolean,

    @SerialName("Data")
    val data: T? = null,

    @SerialName("Errors")
    val errors: AiApiError? = null,

    @SerialName("Meta")
    val meta: Meta? = null
)

@Serializable
data class AiApiError(
    @SerialName("Code")
    val code: String,

    @SerialName("Message")
    val message: String,

    @SerialName("Type")
    val type: String? = null,

    @SerialName("Details")
    val details: String? = null
)

@Serializable
data class Meta(
    @SerialName("Path")
    val path: String? = null,
    @SerialName("image_name")
    val imageName: String? = null
)


@Serializable
data class ReadinessDto(
    @SerialName("abs_readiness_score")
    val absReadinessScore: Int,

    @SerialName("rel_readiness_score")
    val relReadinessScore: String? = null,

    @SerialName("per_aspect_scores")
    val perAspectScores: Map<String, Int> = emptyMap(),

    @SerialName("per_aspect_values")
    val perAspectValues: Map<String, String> = emptyMap(),

    @SerialName("per_aspect_notes")
    val perAspectNotes: Map<String, String> = emptyMap(),

    @SerialName("per_aspect_flags")
    val perAspectFlags: Map<String, Int> = emptyMap()
)


@Serializable
data class HealthReportResponse(
    @SerialName("overall_summary")
    val overallSummary: String,

    val sleep: ReportAspect,
    val activity: ReportAspect,
    val heart: ReportAspect,
    val stress: ReportAspect,

    @SerialName("references")
    val references: List<String> = emptyList(),

    @SerialName("abs_readiness_score")
    val absReadinessScore: Int,

    @SerialName("rel_readiness_score")
    val relReadinessScore: String? = null
)

@Serializable
data class ReportAspect(
    @SerialName("notable_findings")
    val notableFindings: List<String> = emptyList(),

    @SerialName("lifestyle_suggestions")
    val lifestyleSuggestions: List<String> = emptyList(),

    val score: Int
)

@Serializable
data class ArrhythmiaPredictionData(
    @SerialName("final_prediction")
    val finalPrediction: Int = 0,

    @SerialName("final_probability")
    val finalProbability: Int = 0,

    @SerialName("predictions")
    val predictions: List<Int> = emptyList(),

    @SerialName("probabilities_afib")
    val probabilitiesAfib: List<Double> = emptyList(),

    @SerialName("probabilities_others")
    val probabilitiesOthers: List<Double> = emptyList(),

    @SerialName("timestameps")
    val timestamps: List<String> = emptyList()
)