package com.bonyad.healthplat.domain.model

/**
 * Represents the type of health parameter for alarm
 */
enum class AlarmType(
    val persianName: String,
    val description: String,
    val defaultMin: Int,
    val defaultMax: Int,
    val unit: String
) {
    HEART_RATE(
        persianName = "ضربان قلب",
        description = "حد معمولی ضربان قلب بصورت میانگین برای بزرگسالان ۶۰ تا ۱۰۰ ضربه در دقیقه است.",
        defaultMin = 60,
        defaultMax = 100,
        unit = "bpm"
    ),
//    BLOOD_PRESSURE(
//        persianName = "فشار خون",
//        description = "فشار خون نرمال برای بزرگسالان کمتر از ۱۲۰/۸۰ میلی‌متر جیوه است.",
//        defaultMin = 90,
//        defaultMax = 140,
//        unit = "mmHg"
//    ),
    STRESS(
        persianName = "میزان استرس",
        description = "سطح استرس نرمال بین ۰ تا ۳۰ است. بالای ۶۰ نشان‌دهنده استرس بالاست.",
        defaultMin = 0,
        defaultMax = 60,
        unit = ""
    ),
    BLOOD_OXYGEN(
        persianName = "اکسیژن خون",
        description = "سطح اکسیژن خون نرمال بین ۹۵ تا ۱۰۰ درصد است.",
        defaultMin = 95,
        defaultMax = 100,
        unit = "%"
    ),
//    ARRHYTHMIA(
//        persianName = "آریتمی",
//        description = "آریتمی نشان‌دهنده نامنظمی در ضربان قلب است.",
//        defaultMin = 0,
//        defaultMax = 1,
//        unit = ""
//    )
}

/**
 * Represents a health alarm configuration
 */
data class HealthAlarm(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: AlarmType,
    val minThreshold: Int,
    val maxThreshold: Int,
    val isEnabled: Boolean = true,
    val sendToCaregiver: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * UI State for alarm screen
 */
sealed class AlarmUiState {
    object Loading : AlarmUiState()
    object Empty : AlarmUiState()
    data class Success(val alarms: List<HealthAlarm>) : AlarmUiState()
    data class Error(val message: String) : AlarmUiState()
}

/**
 * Events from the alarm screen
 */
sealed class AlarmEvent {
    object AddAlarm : AlarmEvent()
    data class EditAlarm(val alarm: HealthAlarm) : AlarmEvent()
    data class DeleteAlarm(val alarmId: String) : AlarmEvent()
    data class ToggleAlarm(val alarmId: String, val enabled: Boolean) : AlarmEvent()
}