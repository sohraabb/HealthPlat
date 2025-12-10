package com.bonyad.healthplat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.ui.navigation.HealthPlatNavGraph
import com.bonyad.healthplat.ui.navigation.NavRoutes
import com.bonyad.healthplat.ui.theme.HealthPlatTheme
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferencesDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen BEFORE super.onCreate()
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Determine start destination
        val startDestination = determineStartDestination()

        // Keep splash screen visible while loading
        var keepSplashScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }

        enableEdgeToEdge()
        setContent {
            LaunchedEffect(Unit) {
                keepSplashScreen = false
            }
            HealthPlatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HealthPlatNavGraph(startDestination = startDestination)
                }
            }
        }
    }

    private fun determineStartDestination(): String {
        return runBlocking {
            val isOnboardingComplete = userPreferences.isOnboardingComplete().first()
            val token = userPreferences.getAuthToken().first()
            val refreshToken = userPreferences.getRefreshToken().first()
            val userId = userPreferences.getUserId().first()

            // 1) Already logged in → go to Dashboard
            if (userId != null && token != null && refreshToken != null)
                return@runBlocking NavRoutes.Dashboard.route

            // 2) New user → start onboarding
            if (!isOnboardingComplete)
                return@runBlocking NavRoutes.Onboarding.route

            // 3) No user ID → authentication
            if (userId == null)
                return@runBlocking NavRoutes.PhoneAuth.route

            // 4) Additional steps:
            val deviceMac = userPreferences.getDeviceMac().first()
            val termsAccepted = userPreferences.isTermsAccepted().first()
            val userName = userPreferences.getUserName().first()

            return@runBlocking when {
                deviceMac.isNullOrEmpty() -> NavRoutes.DeviceConnection.route
                !termsAccepted -> NavRoutes.TermsAndPrivacy.route
                userName.isNullOrEmpty() -> NavRoutes.PersonalInfo.route
                else -> NavRoutes.Dashboard.route
            }
        }
    }
}