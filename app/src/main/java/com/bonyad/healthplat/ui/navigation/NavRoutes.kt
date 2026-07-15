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
    object RingManagement : NavRoutes("ring_management")
}

sealed class HealthDetailRoutes(val route: String) {
    object HeartRateDetail : HealthDetailRoutes("heart_rate_detail")
    object StepsDetail : HealthDetailRoutes("steps_detail")
    object SleepDetail : HealthDetailRoutes("sleep_detail")
    object SpO2Detail : HealthDetailRoutes("spo2_detail")
    object StressDetail : HealthDetailRoutes("stress_detail")
    object ArrhythmiaDetail : HealthDetailRoutes("arrhythmia_detail")
    object ReadinessDetail : HealthDetailRoutes("readiness_detail")

    object HeartRateInfo : NavRoutes("heart_rate_info")
    object SleepInfo : NavRoutes("sleep_info")
    object SpO2Info : NavRoutes("spo2_info")
    object StepsInfo : NavRoutes("steps_info")
    object StressInfo : NavRoutes("stress_info")
    object ArrhythmiaInfo : NavRoutes("arrhythmia_info")

    // Caregiver detail routes (reuse existing detail screens with patient data)
    object CaregiverHeartRate : HealthDetailRoutes("caregiver_heart_rate/{patientUserId}") {
        fun createRoute(patientUserId: String) = "caregiver_heart_rate/$patientUserId"
    }
    object CaregiverSleep : HealthDetailRoutes("caregiver_sleep/{patientUserId}") {
        fun createRoute(patientUserId: String) = "caregiver_sleep/$patientUserId"
    }
    object CaregiverSpO2 : HealthDetailRoutes("caregiver_spo2/{patientUserId}") {
        fun createRoute(patientUserId: String) = "caregiver_spo2/$patientUserId"
    }
    object CaregiverStress : HealthDetailRoutes("caregiver_stress/{patientUserId}") {
        fun createRoute(patientUserId: String) = "caregiver_stress/$patientUserId"
    }
}

sealed class CaloryRoutes(val route: String) {
    object Main : CaloryRoutes("calory_main")
    object ConsumedDetails : CaloryRoutes("calory_consumed_details")
    object BurnedDetails : CaloryRoutes("calory_burned_details/{date}") {
        fun createRoute(date: String): String = "calory_burned_details/$date"
    }
    object FoodScan : CaloryRoutes("calory_food_scan/{mealType}") {
        fun createRoute(mealType: String): String = "calory_food_scan/$mealType"
    }

    object ScanResult : CaloryRoutes("calory_scan_result/{imageUri}/{mealType}") {
        fun createRoute(imageUri: String, mealType: String): String {
            val encodedUri = URLEncoder.encode(imageUri, StandardCharsets.UTF_8.toString())
            return "calory_scan_result/$encodedUri/$mealType"
        }

        fun parseImageUri(encodedUri: String): String {
            return URLDecoder.decode(encodedUri, StandardCharsets.UTF_8.toString())
        }
    }
}