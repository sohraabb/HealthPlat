package com.bonyad.healthplat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bonyad.healthplat.ui.access.TermsAndPrivacyScreen
import com.bonyad.healthplat.ui.login.OtpVerificationScreen
import com.bonyad.healthplat.ui.login.PhoneAuthScreen
import com.bonyad.healthplat.ui.onboarding.OnboardingScreen
import com.bonyad.healthplat.ui.profile.PersonalInfoScreen

@Composable
fun HealthPlatNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = NavRoutes.Onboarding.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 1. Onboarding
        composable(NavRoutes.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(NavRoutes.PhoneAuth.route) {
                        popUpTo(NavRoutes.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // 2. Phone Authentication
        composable(NavRoutes.PhoneAuth.route) {
            PhoneAuthScreen(
                onPhoneSubmitted = { phoneNumber ->
                    navController.navigate(NavRoutes.OtpVerification.route + "/$phoneNumber")
                }
            )
        }

        // 3. OTP Verification
        composable(
            route = NavRoutes.OtpVerification.route + "/{phoneNumber}",
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            OtpVerificationScreen(
                phoneNumber = phoneNumber,
                onVerified = {
                    navController.navigate(NavRoutes.TermsAndPrivacy.route) {
                        popUpTo(NavRoutes.PhoneAuth.route) { inclusive = true }
                    }
                }
            )
        }

        // 4. Device Connection (Bluetooth)
        composable(NavRoutes.DeviceConnection.route) {
            // TODO: Create DeviceConnectionScreen next
//            DeviceConnectionScreen(
//                onDeviceConnected = {
//                    navController.navigate(NavRoutes.TermsAndPrivacy.route)
//                }
//            )
        }

        // 5. Terms and Privacy (Access screens from GitHub)
        composable(NavRoutes.TermsAndPrivacy.route) {
            TermsAndPrivacyScreen(
                onAccept = {
                    navController.navigate(NavRoutes.PersonalInfo.route)
                }
            )
        }


        // 6. Personal Information
        composable(NavRoutes.PersonalInfo.route) {
            // TODO: Create PersonalInfoScreen
            PersonalInfoScreen(
                onComplete = {
                    navController.navigate(NavRoutes.Dashboard.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // 7. Dashboard (Main App)
        composable(NavRoutes.Dashboard.route) {
            // TODO: Create DashboardScreen
//            DashboardScreen()
        }
    }
}