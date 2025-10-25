package com.bonyad.healthplat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bonyad.healthplat.ui.login.OtpVerificationScreen
import com.bonyad.healthplat.ui.login.PhoneAuthScreen
import com.bonyad.healthplat.ui.onboarding.OnboardingScreen

@Composable
fun HealthPlatNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = NavRoutes.Onboarding.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(NavRoutes.TermsAndPrivacy.route) {
                        popUpTo(NavRoutes.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.TermsAndPrivacy.route) {
            // TODO: Create this screen next
//            TermsAndPrivacyScreen(
//                onAccept = {
//                    navController.navigate(NavRoutes.DeviceConnection.route)
//                }
//            )
        }

        composable(NavRoutes.DeviceConnection.route) {
            // TODO: Create this screen
//            DeviceConnectionScreen(
//                onDeviceConnected = {
//                    navController.navigate(Screen.PhoneAuth.route)
//                }
//            )
        }

        composable(NavRoutes.PhoneAuth.route) {
            // TODO: Create this screen
            PhoneAuthScreen(
                onPhoneSubmitted = { phoneNumber ->
                    navController.navigate(NavRoutes.OtpVerification.route + "/$phoneNumber")
                }
            )
        }

        composable(
            route = NavRoutes.OtpVerification.route + "/{phoneNumber}",
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            // TODO: Create this screen
            OtpVerificationScreen(
                phoneNumber = phoneNumber,
                onVerified = {
                    navController.navigate(NavRoutes.PersonalInfo.route)
                }
            )
        }

        composable(NavRoutes.PersonalInfo.route) {
            // TODO: Create this screen
//            PersonalInfoScreen(
//                onComplete = {
//                    navController.navigate(Screen.Dashboard.route) {
//                        popUpTo(0) { inclusive = true }
//                    }
//                }
//            )
        }

        composable(NavRoutes.Dashboard.route) {
            // TODO: Create this screen
//            DashboardScreen()
        }
    }
}