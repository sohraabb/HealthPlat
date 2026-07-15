package com.bonyad.healthplat.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bonyad.healthplat.ui.access.TermsAndPrivacyScreen
import com.bonyad.healthplat.ui.dashboard.DashboardScreen
import com.bonyad.healthplat.ui.dashboard.ai.AiAnalysisScreen
import com.bonyad.healthplat.ui.dashboard.calory.BurnedCaloriesScreen
import com.bonyad.healthplat.ui.dashboard.calory.CaloryScreen
import com.bonyad.healthplat.ui.dashboard.calory.CaloryViewModel
import com.bonyad.healthplat.ui.dashboard.calory.ConsumedCaloriesScreen
import com.bonyad.healthplat.ui.dashboard.calory.FoodScanScreen
import com.bonyad.healthplat.ui.dashboard.calory.ScanResultScreen
import com.bonyad.healthplat.ui.dashboard.details.HealthInfoScreen
import com.bonyad.healthplat.ui.dashboard.details.HealthInfoType
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.HeartRateDetailScreen
import com.bonyad.healthplat.ui.dashboard.details.sleep.SleepDetailScreen
import com.bonyad.healthplat.ui.dashboard.details.sp02.SpO2DetailScreen
import com.bonyad.healthplat.ui.dashboard.details.stepts.StepsDetailScreen
import com.bonyad.healthplat.ui.dashboard.details.arrhythmia.ArrhythmiaDetailScreen
import com.bonyad.healthplat.ui.dashboard.details.readiness.ReadinessScreen
import com.bonyad.healthplat.ui.dashboard.details.stress.StressDetailScreen
import com.bonyad.healthplat.ui.dashboard.notification.NotificationScreen
import com.bonyad.healthplat.ui.dashboard.notification.NotificationViewModel
import com.bonyad.healthplat.ui.dashboard.profile.alarm.AlarmScreen
import com.bonyad.healthplat.ui.dashboard.profile.medication.MedicationScreen
import com.bonyad.healthplat.ui.dashboard.profile.personal_info.EditPersonalInfoScreen
import com.bonyad.healthplat.ui.dashboard.profile.ring.RingScreen
import com.bonyad.healthplat.ui.dashboard.profile.wallet.WalletScreen
import com.bonyad.healthplat.ui.device.DeviceConnectionScreen
import com.bonyad.healthplat.ui.login.AuthViewModel
import com.bonyad.healthplat.ui.login.OtpVerificationScreen
import com.bonyad.healthplat.ui.login.PhoneAuthScreen
import com.bonyad.healthplat.ui.onboarding.OnboardingScreen
import com.bonyad.healthplat.ui.profile.PersonalInfoScreen

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
            DashboardScreen(
                onNavigateToRoot = { route ->
                    navController.navigate(route)
                },
                onLogout = {
//                    healthSyncScheduler.stopPeriodicSync()

                    navController.navigate(NavRoutes.PhoneAuth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // 8. AI
        composable(NavRoutes.AiScreen.route) {
            AiAnalysisScreen(
                onBack = { navController.popBackStack() }
            )
        }


        // NavRoutes From Profile

        // 9. Alarm Settings - NEW
        composable(ProfileRoutes.AlarmSettings.route) {
            AlarmScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 10. Edit Personal Info - NEW
        composable(ProfileRoutes.EditPersonalInfo.route) {
            EditPersonalInfoScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(ProfileRoutes.Wallet.route) {
            WalletScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(ProfileRoutes.Medication.route) {
            MedicationScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 11. Device Setup from Profile — back/skip returns to dashboard
        composable(ProfileRoutes.DeviceSetup.route) {
            DeviceConnectionScreen(
                onDeviceConnected = {
                    navController.popBackStack()
                },
                navController = navController,
                onSkip = {
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // 12. Ring Management from Profile
        composable(ProfileRoutes.RingManagement.route) {
            RingScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Health Details Routes :
        composable(HealthDetailRoutes.HeartRateDetail.route) {
            HeartRateDetailScreen(
                onBack = { navController.popBackStack() },
                onInfoClick = {
                    navController.navigate(HealthDetailRoutes.HeartRateInfo.route)
                }
            )
        }

        composable(HealthDetailRoutes.SleepDetail.route) {
            SleepDetailScreen(
                onBack = { navController.popBackStack() },
                onInfoClick = {
                    navController.navigate(HealthDetailRoutes.SleepInfo.route)
                }
            )
        }

        composable(HealthDetailRoutes.StepsDetail.route) {
            StepsDetailScreen(
                onBack = { navController.popBackStack() },
                onInfoClick = {
                    navController.navigate(HealthDetailRoutes.StepsInfo.route)
                }
            )
        }

        composable(HealthDetailRoutes.SpO2Detail.route) {
            SpO2DetailScreen(
                onBack = { navController.popBackStack() },
                onInfoClick = {
                    navController.navigate(HealthDetailRoutes.SpO2Info.route)
                }
            )
        }

        composable(HealthDetailRoutes.StressDetail.route) {
            StressDetailScreen(
                onBack = { navController.popBackStack() },
                onInfoClick = {
                    navController.navigate(HealthDetailRoutes.StressInfo.route)
                }
            )
        }

        composable(HealthDetailRoutes.ArrhythmiaDetail.route) {
            ArrhythmiaDetailScreen(
                onBack = { navController.popBackStack() },
                onInfoClick = {
                    navController.navigate(HealthDetailRoutes.ArrhythmiaInfo.route)
                }
            )
        }

        composable(HealthDetailRoutes.ReadinessDetail.route) {
            ReadinessScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ═══════════════════════════════════════════════════════════════
        // CAREGIVER DETAIL ROUTES
        // Reuse existing detail screens — the ViewModel reads patientUserId
        // from SavedStateHandle and switches to caregiver API mode.
        // ═══════════════════════════════════════════════════════════════

        composable(
            route = HealthDetailRoutes.CaregiverHeartRate.route,
            arguments = listOf(navArgument("patientUserId") { type = NavType.StringType })
        ) {
            HeartRateDetailScreen(
                onBack = { navController.popBackStack() },
                onInfoClick = {
                    navController.navigate(HealthDetailRoutes.HeartRateInfo.route)
                }
            )
        }

        composable(
            route = HealthDetailRoutes.CaregiverSleep.route,
            arguments = listOf(navArgument("patientUserId") { type = NavType.StringType })
        ) {
            SleepDetailScreen(
                onBack = { navController.popBackStack() },
                onInfoClick = {
                    navController.navigate(HealthDetailRoutes.SleepInfo.route)
                }
            )
        }

        composable(
            route = HealthDetailRoutes.CaregiverSpO2.route,
            arguments = listOf(navArgument("patientUserId") { type = NavType.StringType })
        ) {
            SpO2DetailScreen(
                onBack = { navController.popBackStack() },
                onInfoClick = {
                    navController.navigate(HealthDetailRoutes.SpO2Info.route)
                }
            )
        }

        composable(
            route = HealthDetailRoutes.CaregiverStress.route,
            arguments = listOf(navArgument("patientUserId") { type = NavType.StringType })
        ) {
            StressDetailScreen(
                onBack = { navController.popBackStack() },
                onInfoClick = {
                    navController.navigate(HealthDetailRoutes.StressInfo.route)
                }
            )
        }

        // Heart Rate Detail Screen
        composable(HealthDetailRoutes.HeartRateInfo.route) {
            HealthInfoScreen(
                infoType = HealthInfoType.HeartRate,
                onBack = { navController.popBackStack() }
            )
        }

// Sleep Info Screen
        composable(HealthDetailRoutes.SleepInfo.route) {
            HealthInfoScreen(
                infoType = HealthInfoType.Sleep,
                onBack = { navController.popBackStack() }
            )
        }

// SpO2 Info Screen
        composable(HealthDetailRoutes.SpO2Info.route) {
            HealthInfoScreen(
                infoType = HealthInfoType.SpO2,
                onBack = { navController.popBackStack() }
            )
        }

// Steps Info Screen
        composable(HealthDetailRoutes.StepsInfo.route) {
            HealthInfoScreen(
                infoType = HealthInfoType.Steps,
                onBack = { navController.popBackStack() }
            )
        }

// Stress Info Screen
        composable(HealthDetailRoutes.StressInfo.route) {
            HealthInfoScreen(
                infoType = HealthInfoType.Stress,
                onBack = { navController.popBackStack() }
            )
        }

// Arrhythmia Info Screen
        composable(HealthDetailRoutes.ArrhythmiaInfo.route) {
            HealthInfoScreen(
                infoType = HealthInfoType.Arrhythmia,
                onBack = { navController.popBackStack() }
            )
        }

        // Notification
        composable(NavRoutes.Notifications.route) {
            val viewModel: NotificationViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            NotificationScreen(
                notifications = uiState.notifications,
                onBackClick = { navController.popBackStack() },
                onNotificationClick = { notification ->
                    viewModel.markAsRead(notification.id)
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════
        // CALORY FEATURE ROUTES
        // ═══════════════════════════════════════════════════════════════
        //
        // ⚠️ FIX: Removed shared ViewModel pattern via getBackStackEntry
        // because CaloryRoutes.Main is NOT on the back stack when navigating
        // from Dashboard's nested NavHost (which uses route "calory", not "calory_main").
        //
        // Each screen now gets its own ViewModel instance from Hilt.
        // Since CaloryViewModel uses Repository pattern, data will be consistent.
        // ═══════════════════════════════════════════════════════════════

        // 13. Calory Main Screen (only used if navigating directly to this route)
        composable(CaloryRoutes.Main.route) {
            CaloryScreen(
                onNavigateToRoute = { route ->
                    navController.navigate(route)
                }
            )
        }

        // 14. Consumed Calories Details
        // ✅ FIX: Use default hiltViewModel() - no shared parent entry
        composable(CaloryRoutes.ConsumedDetails.route) {
            ConsumedCaloriesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 15. Burned Calories Details
        // ✅ FIX: Use default hiltViewModel() - no shared parent entry
        composable(
            route = CaloryRoutes.BurnedDetails.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) {
            BurnedCaloriesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 16. Food Scan Camera
        composable(
            route = CaloryRoutes.FoodScan.route,
            arguments = listOf(navArgument("mealType") { type = NavType.StringType })
        ) { backStackEntry ->
            val mealType = backStackEntry.arguments?.getString("mealType") ?: "LUNCH"
            FoodScanScreen(
                onNavigateBack = { navController.popBackStack() },
                onFoodScanned = { imageUri ->
                    navController.navigate(
                        CaloryRoutes.ScanResult.createRoute(imageUri.toString(), mealType)
                    )
                }
            )
        }

        // 17. Scan Result Screen
        composable(
            route = CaloryRoutes.ScanResult.route,
            arguments = listOf(
                navArgument("imageUri") { type = NavType.StringType },
                navArgument("mealType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: ""
            val imageUri = Uri.parse(CaloryRoutes.ScanResult.parseImageUri(encodedUri))
            val mealTypeName = backStackEntry.arguments?.getString("mealType") ?: "LUNCH"
            val mealType = try {
                com.bonyad.healthplat.domain.model.MealType.valueOf(mealTypeName)
            } catch (_: IllegalArgumentException) {
                com.bonyad.healthplat.domain.model.MealType.LUNCH
            }

            ScanResultScreen(
                imageUri = imageUri,
                initialMealType = mealType,
                onDismiss = {
                    navController.popBackStack(NavRoutes.Dashboard.route, inclusive = false)
                },
                onSaveComplete = {
                    navController.popBackStack(NavRoutes.Dashboard.route, inclusive = false)
                }
            )
        }
    }
}