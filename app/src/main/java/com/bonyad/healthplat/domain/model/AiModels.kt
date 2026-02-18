package com.bonyad.healthplat.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object ApiErrorType {
    const val NO_METRICS_FOUND = "NoMetricsFound"
}

@Serializable
data class AiApiResponse<T>(
    @SerialName("ok")
    val ok: Boolean,

    @SerialName("data")
    val data: T? = null,

    @SerialName("error")
    val error: AiApiError? = null,

    @SerialName("meta")
    val meta: Meta? = null
)

@Serializable
data class AiApiError(
    @SerialName("code")
    val code: String,

    @SerialName("message")
    val message: String,

    @SerialName("type")
    val type: String,

    @SerialName("details")
    val details: String? = null
)

@Serializable
data class Meta(
    @SerialName("path")
    val path: String? = null
)


@Serializable
data class ReadinessDto(
    @SerialName("abs_readiness_score")
    val absReadinessScore: Int,

    @SerialName("rel_readiness_score")
    val relReadinessScore: String? = null,

    @SerialName("per_aspect_scores")
    val perAspectScores: Map<String, Int> = emptyMap(),

    @SerialName("per_aspect_notes")
    val perAspectNotes: Map<String, String> = emptyMap()
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

    val score: String
)