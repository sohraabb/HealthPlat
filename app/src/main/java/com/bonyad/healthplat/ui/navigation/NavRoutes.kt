package com.bonyad.healthplat.ui.navigation

sealed class NavRoutes(val route: String) {
    object Onboarding : NavRoutes("onboarding")
    object TermsAndPrivacy : NavRoutes("terms_and_privacy")
    object DeviceConnection : NavRoutes("device_connection")
    object PhoneAuth : NavRoutes("phone_auth")
    object OtpVerification : NavRoutes("otp_verification")
    object PersonalInfo : NavRoutes("personal_info")
    object Dashboard : NavRoutes("dashboard")
}

