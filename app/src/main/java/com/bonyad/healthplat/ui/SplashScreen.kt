package com.bonyad.healthplat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.bonyad.healthplat.R
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.ui.navigation.HealthPlatNavGraph
import com.bonyad.healthplat.ui.navigation.NavRoutes
import kotlinx.coroutines.delay

@Composable
fun HealthPlatEntryPoint() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val userPreferences = remember { UserPreferencesDataStore(context) }

    val isOnboardingComplete by userPreferences.isOnboardingComplete().collectAsState(initial = null)
    val userId by userPreferences.getUserId().collectAsState(initial = null)

    // Wait for preferences to load
    if (isOnboardingComplete == null) {
        // You can show a simple centered logo or progress indicator if you like
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Once loaded, decide the start destination dynamically
    val startDestination = when {
        isOnboardingComplete == false -> NavRoutes.Onboarding.route
        userId == null -> NavRoutes.PhoneAuth.route
        else -> NavRoutes.Dashboard.route
    }

    HealthPlatNavGraph(navController = navController, startDestination = startDestination)
}