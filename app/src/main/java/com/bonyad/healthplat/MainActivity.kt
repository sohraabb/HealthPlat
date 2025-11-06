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
import com.bonyad.healthplat.ui.HealthPlatEntryPoint
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
            try {
                val isOnboardingComplete = userPreferences.isOnboardingComplete().first()
                val userId = userPreferences.getUserId().first()

                when {
                    !isOnboardingComplete -> NavRoutes.Onboarding.route
                    userId == null -> NavRoutes.PhoneAuth.route
                    else -> {
                        val deviceMac = userPreferences.getDeviceMac().first()
                        val termsAccepted = userPreferences.isTermsAccepted().first()
                        val userName = userPreferences.getUserName().first()

                        when {
                            deviceMac.isNullOrEmpty() -> NavRoutes.DeviceConnection.route
                            !termsAccepted -> NavRoutes.TermsAndPrivacy.route
                            userName.isNullOrEmpty() -> NavRoutes.PersonalInfo.route
                            else -> NavRoutes.Dashboard.route
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error determining start destination")
                NavRoutes.Onboarding.route
            }
        }
    }
}