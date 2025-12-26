package com.bonyad.healthplat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.bonyad.healthplat.blesdk.model.PpgRawDataParser
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.ui.MainViewModel
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

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition {
            mainViewModel.isLoading.value
        }

        PpgRawDataParser.testWithExampleData()

        setContent {
            HealthPlatTheme {
                val startDest by mainViewModel.startDestination.collectAsState()

                if (startDest != null) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        HealthPlatNavGraph(startDestination = startDest!!)
                    }
                }
            }
        }
    }

    fun testPpgParser() {
        // Test with known data from image
        val passed = PpgRawDataParser.testWithExampleData()

        if (passed) {
            Timber.d("✓ PPG Parser working correctly!")
            Timber.d("Next: Enable device notifications and call parse() on incoming data")
        } else {
            Timber.e("✗ PPG Parser test failed - check byte positions")
        }
    }
}