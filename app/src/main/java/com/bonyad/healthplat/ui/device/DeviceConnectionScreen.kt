package com.bonyad.healthplat.ui.device

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

enum class ConnectionStep {
    START,
    SCANNING,
    TROUBLESHOOT
}

@Composable
fun DeviceConnectionScreen(
    viewModel: DeviceConnectionViewModel = hiltViewModel(),
    onDeviceConnected: () -> Unit,
    navController: NavController,
    onSkip: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {

    val navController = rememberNavController()
    var currentStep by remember { mutableStateOf(ConnectionStep.START) }

    BackHandler {
        when (currentStep) {
            ConnectionStep.START -> onBack?.invoke()           // go back to previous nav screen
            ConnectionStep.SCANNING -> currentStep = ConnectionStep.START
            ConnectionStep.TROUBLESHOOT -> currentStep = ConnectionStep.SCANNING
        }
    }

    when (currentStep) {
        ConnectionStep.START -> {
            DeviceConnectionStartScreen(
                viewModel = viewModel,
                onStartScan = { currentStep = ConnectionStep.SCANNING },
                onSkip = onSkip,
                onBack = { navController.popBackStack() } // also handle toolbar back
            )
        }

        ConnectionStep.SCANNING -> {
            DeviceScanningScreen(
                viewModel = viewModel,
                onDeviceConnected = onDeviceConnected,
                onTroubleshoot = { currentStep = ConnectionStep.TROUBLESHOOT },
                onBack = { currentStep = ConnectionStep.START }
            )
        }

        ConnectionStep.TROUBLESHOOT -> {
            TroubleshootScreen(
                onBack = { currentStep = ConnectionStep.SCANNING },
                onRetry = { currentStep = ConnectionStep.SCANNING }
            )
        }
    }
}

@Composable
fun TroubleshootScreen(
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    // Simple troubleshooting screen
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onBack,
        title = {
            androidx.compose.material3.Text(
                text = "راهنمای عیب‌یابی",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge
            )
        },
        text = {
            androidx.compose.material3.Text(
                text = """
                    • مطمئن شوید بلوتوث فعال است
                    • دستگاه را به شارژر وصل کنید
                    • فاصله دستگاه با گوشی کم باشد
                    • دستگاه را خاموش و روشن کنید
                    • مجددا تلاش کنید
                """.trimIndent()
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onRetry) {
                androidx.compose.material3.Text("تلاش مجدد")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onBack) {
                androidx.compose.material3.Text("بازگشت")
            }
        }
    )
}