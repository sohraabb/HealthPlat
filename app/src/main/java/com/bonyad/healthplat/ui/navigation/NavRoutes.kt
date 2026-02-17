package com.bonyad.healthplat.ui.navigation

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class NavRoutes(val route: String) {
    object Onboarding : NavRoutes("onboarding")
    object TermsAndPrivacy : NavRoutes("terms_and_privacy")
    object DeviceConnection : NavRoutes("device_connection")
    object PhoneAuth : NavRoutes("phone_auth")
    object OtpVerification : NavRoutes("otp_verification")
    object PersonalInfo : NavRoutes("personal_info")
    object Dashboard : NavRoutes("dashboard")
    object AiScreen : NavRoutes("ai_screen")
    object Notifications : NavRoutes("notifications")}

sealed class ProfileRoutes(val route: String) {
    object EditPersonalInfo : NavRoutes("edit_personal_info")
    object AlarmSettings : NavRoutes("alarm_settings")
    object Wallet : NavRoutes("wallet")
    object Medication : NavRoutes("medication")
    object DeviceSetup : NavRoutes("profile_device_setup")  // ← NEW
}

sealed class HealthDetailRoutes(val route: String) {
    object HeartRateDetail : HealthDetailRoutes("heart_rate_detail")
    object StepsDetail : HealthDetailRoutes("steps_detail")
    object SleepDetail : HealthDetailRoutes("sleep_detail")
    object SpO2Detail : HealthDetailRoutes("spo2_detail")
    object StressDetail : HealthDetailRoutes("stress_detail")

    object HeartRateInfo : NavRoutes("heart_rate_info")
    object SleepInfo : NavRoutes("sleep_info")
    object SpO2Info : NavRoutes("spo2_info")
    object StepsInfo : NavRoutes("steps_info")
    object StressInfo : NavRoutes("stress_info")
}

sealed class CaloryRoutes(val route: String) {
    object Main : CaloryRoutes("calory_main")
    object ConsumedDetails : CaloryRoutes("calory_consumed_details")
    object BurnedDetails : CaloryRoutes("calory_burned_details")
    object FoodScan : CaloryRoutes("calory_food_scan")

    object ScanResult : CaloryRoutes("calory_scan_result/{imageUri}") {
        fun createRoute(imageUri: String): String {
            val encodedUri = URLEncoder.encode(imageUri, StandardCharsets.UTF_8.toString())
            return "calory_scan_result/$encodedUri"
        }

        fun parseImageUri(encodedUri: String): String {
            return URLDecoder.decode(encodedUri, StandardCharsets.UTF_8.toString())
        }
    }
}