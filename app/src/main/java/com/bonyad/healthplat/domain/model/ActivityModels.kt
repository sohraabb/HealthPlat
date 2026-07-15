package com.bonyad.healthplat.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Activity Fact (compendium template) ────────────────────────────────────
// Cal = the activity's MET value (metabolic equivalent). Burned calories are
// derived via the MET formula: kcal = MET × 3.5 × weightKg × durationMin / 200

@Serializable
data class ActivityFact(
    @SerialName("Id") val id: Int,
    @SerialName("Name") val name: String,
    @SerialName("OrgName") val orgName: String,
    @SerialName("Category") val category: String,
    @SerialName("Duration") val duration: Double,
    @SerialName("DurationUnit") val durationUnit: String,
    @SerialName("Weight") val weight: Double,
    @SerialName("WeightUnit") val weightUnit: String,
    @SerialName("AgeMin") val ageMin: Int? = null,
    @SerialName("AgeMax") val ageMax: Int? = null,
    @SerialName("Gender") val gender: String? = null,
    @SerialName("Cal") val cal: Double,
    @SerialName("Source") val source: String? = null
)

// ─── User-logged Activity ────────────────────────────────────────────────────

@Serializable
data class UserActivity(
    @SerialName("Id") val id: Int,
    @SerialName("UserId") val userId: String,
    @SerialName("ActivityFactId") val activityFactId: Int,
    @SerialName("ActivityName") val activityName: String,
    @SerialName("ActivityOrgName") val activityOrgName: String,
    @SerialName("Duration") val duration: Double,   // hours
    @SerialName("ActivityCal") val activityCal: Double
)

// ─── Request bodies ──────────────────────────────────────────────────────────

@Serializable
data class CreateActivityRequest(
    @SerialName("ActivityFactId") val activityFactId: Int,
    @SerialName("ActivityName") val activityName: String,
    @SerialName("ActivityOrgName") val activityOrgName: String,
    @SerialName("Duration") val duration: Double,   // hours
    @SerialName("ActivityCal") val activityCal: Double
)

@Serializable
data class UpdateActivityRequest(
    @SerialName("Id") val id: Int,
    @SerialName("ActivityFactId") val activityFactId: Int,
    @SerialName("ActivityName") val activityName: String,
    @SerialName("ActivityOrgName") val activityOrgName: String,
    @SerialName("Duration") val duration: Double,   // hours
    @SerialName("ActivityCal") val activityCal: Double
)
