package com.bonyad.healthplat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.ui.access.TermsAndPrivacyScreen
import com.bonyad.healthplat.ui.dashboard.DashboardScreen
import com.bonyad.healthplat.ui.device.DeviceConnectionScreen
import com.bonyad.healthplat.ui.login.OtpVerificationScreen
import com.bonyad.healthplat.ui.login.PhoneAuthScreen
import com.bonyad.healthplat.ui.onboarding.OnboardingScreen
import com.bonyad.healthplat.ui.profile.PersonalInfoScreen
import com.yourpackage.healthplat.ui.auth.AuthViewModel
import timber.log.Timber

@Composable
fun HealthPlatNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String
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
        composable(NavRoutes.PhoneAuth.route) { backStackEntry ->
            // ✅ Use the current backStackEntry
            val viewModel: AuthViewModel = hiltViewModel(backStackEntry)

            PhoneAuthScreen(
                viewModel = viewModel,
                onPhoneSubmitted = { phoneNumber ->
                    navController.navigate(NavRoutes.OtpVerification.route + "/$phoneNumber")
                }
            )
        }

        // 3. OTP Verification - SHARED VIEWMODEL
        composable(
            route = NavRoutes.OtpVerification.route + "/{phoneNumber}",
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(NavRoutes.PhoneAuth.route)
            }
            val viewModel: AuthViewModel = hiltViewModel(parentEntry)

            OtpVerificationScreen(
                phoneNumber = phoneNumber,
                viewModel = viewModel,
                onVerified = {
                    // ✅ CORRECT: Check isNewUser flag that was set during requestPhoneVerification
                    if (viewModel.shouldShowDeviceConnection()) {
                        // New user -> Device Connection
                        navController.navigate(NavRoutes.DeviceConnection.route) {
                            popUpTo(NavRoutes.PhoneAuth.route) { inclusive = true }
                        }
                    } else {
                        // Existing user -> Dashboard
                        navController.navigate(NavRoutes.Dashboard.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        // 4. Device Connection (Bluetooth)
        composable(NavRoutes.DeviceConnection.route) {
            DeviceConnectionScreen(
                onDeviceConnected = {
                    navController.navigate(NavRoutes.TermsAndPrivacy.route)
                },
                navController = navController,
                onSkip = {
                    navController.navigate(NavRoutes.TermsAndPrivacy.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        // 5. Terms and Privacy
        composable(NavRoutes.TermsAndPrivacy.route) {
            TermsAndPrivacyScreen(
                onAccept = {
                    navController.navigate(NavRoutes.PersonalInfo.route)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // 6. Personal Information
        composable(NavRoutes.PersonalInfo.route) {
            PersonalInfoScreen(
                onComplete = {
                    navController.navigate(NavRoutes.Dashboard.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // 7. Dashboard (Main App)
        composable(NavRoutes.Dashboard.route) {
            // TODO: Create DashboardScreen
            DashboardScreen()
        }
    }
}