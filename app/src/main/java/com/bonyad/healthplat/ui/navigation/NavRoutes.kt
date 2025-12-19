package com.bonyad.healthplat.ui.navigation

sealed class NavRoutes(val route: String) {
    object Onboarding : NavRoutes("onboarding")
    object TermsAndPrivacy : NavRoutes("terms_and_privacy")
    object DeviceConnection : NavRoutes("device_connection")
    object PhoneAuth : NavRoutes("phone_auth")
    object OtpVerification : NavRoutes("otp_verification")
    object PersonalInfo : NavRoutes("personal_info")
    object Dashboard : NavRoutes("dashboard")
    object AiScreen : NavRoutes("ai_screen")
}

sealed class HealthDetailRoutes(val route: String) {
    object HeartRateDetail : HealthDetailRoutes("heart_rate_detail")
    object StepsDetail : HealthDetailRoutes("steps_detail")
    object SleepDetail : HealthDetailRoutes("sleep_detail")
    object SpO2Detail : HealthDetailRoutes("spo2_detail")
    object StressDetail : HealthDetailRoutes("stress_detail")
}

