package com.bonyad.healthplat.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthReportResponse(
    @SerialName("overall_summary")
    val overallSummary: String,

    @SerialName("sleep")
    val sleep: ReportAspect,

    @SerialName("activity")
    val activity: ReportAspect,

    @SerialName("heart")
    val heart: ReportAspect,

    @SerialName("stress")
    val stress: ReportAspect,

    @SerialName("abs_readiness_score")
    val absReadinessScore: Int, // Note: JSON key has "_score" here

    @SerialName("rel_readiness_score")
    val relReadinessScore: String? = null
)

/**
 * Reusable class for Sleep, Activity, Heart, and Stress details
 */
@Serializable
data class ReportAspect(
    @SerialName("notable_findings")
    val notableFindings: List<String>,

    @SerialName("lifestyle_suggestions")
    val lifestyleSuggestions: List<String>,

    @SerialName("score")
    val score: String
)

// ==========================================
// 2. Readiness Response (Updated)
// ==========================================

@Serializable
data class ReadinessResponse(
    @SerialName("abs_readiness")
    val absReadiness: Int, // Note: JSON key is just "abs_readiness" here

    @SerialName("rel_readiness_score")
    val relReadinessScore: String? = null,

    @SerialName("per_aspect_notes")
    val perAspectNotes: Map<String, String>
)

