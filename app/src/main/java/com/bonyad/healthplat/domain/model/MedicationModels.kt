package com.bonyad.healthplat.domain.model

import java.util.UUID

/**
 * Represents a medication/pill entry
 */
data class Medication(
    val id: String = UUID.randomUUID().toString(),
    val title: String,           // عنوان دارو (e.g., قرص قند)
    val name: String,            // نام دارو (e.g., گلوریپا)
    val duration: String,        // مدت زمان مصرف (e.g., ۳ ماه)
    val dosage: String,          // مقدار مصرف (e.g., یک قرص)
    val frequency: MedicationFrequency,
    val selectedDays: Set<DayOfWeek> = emptySet(),  // For daily frequency
    val times: List<MedicationTime> = emptyList(),
    val isEnabled: Boolean = true,
    val startDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Medication intake time
 */
data class MedicationTime(
    val hour: Int,
    val minute: Int
) {
    fun toDisplayString(): String {
        val period = if (hour < 12) "صبح" else if (hour < 17) "ظهر" else "شب"
        val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
        return String.format("%02d:%02d %s", displayHour, minute, period)
    }

    fun to24HourString(): String {
        return String.format("%02d:%02d", hour, minute)
    }
}

/**
 * Frequency of medication intake
 */
enum class MedicationFrequency(val persianName: String) {
    DAILY("روزانه"),
    WEEKLY("هفتگی"),
    MONTHLY("ماهانه")
}

/**
 * Days of the week in Persian calendar
 */
enum class DayOfWeek(val persianName: String, val shortName: String) {
    SATURDAY("شنبه", "ش"),
    SUNDAY("یکشنبه", "ی"),
    MONDAY("دوشنبه", "د"),
    TUESDAY("سه شنبه", "س"),
    WEDNESDAY("چهارشنبه", "چ"),
    THURSDAY("پنجشنبه", "پ"),
    FRIDAY("جمعه", "ج")
}

/**
 * UI State for medication screen
 */
sealed class MedicationUiState {
    object Loading : MedicationUiState()
    object Empty : MedicationUiState()
    data class Success(val medications: List<Medication>) : MedicationUiState()
    data class Error(val message: String) : MedicationUiState()
}

/**
 * Steps in the add medication flow
 */
enum class AddMedicationStep {
    DETAILS,      // Step 1: Title, name, duration, dosage
    FREQUENCY,    // Step 2: Daily, weekly, monthly
    DAYS,         // Step 3: Select days (for daily)
    TIME          // Step 4: Select time(s)
}

/**
 * State for adding a new medication
 */
data class AddMedicationState(
    val currentStep: AddMedicationStep = AddMedicationStep.DETAILS,
    val title: String = "",
    val name: String = "",
    val duration: String = "",
    val dosage: String = "",
    val frequency: MedicationFrequency? = null,
    val selectedDays: Set<DayOfWeek> = emptySet(),
    val times: List<MedicationTime> = emptyList(),
    val currentTimeHour: Int = 10,
    val currentTimeMinute: Int = 0
) {
    val isDetailsValid: Boolean
        get() = title.isNotBlank() && name.isNotBlank() && duration.isNotBlank() && dosage.isNotBlank()

    val isFrequencyValid: Boolean
        get() = frequency != null

    val isDaysValid: Boolean
        get() = frequency != MedicationFrequency.DAILY || selectedDays.isNotEmpty()

    val isTimeValid: Boolean
        get() = times.isNotEmpty()
}