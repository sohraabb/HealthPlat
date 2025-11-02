package com.bonyad.healthplat.ui.device

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel

enum class ConnectionStep {
    START,
    SCANNING,
    TROUBLESHOOT
}

@Composable
fun DeviceConnectionScreen(
    viewModel: DeviceConnectionViewModel = hiltViewModel(),
    onDeviceConnected: () -> Unit,
    onSkip: (() -> Unit)? = null
) {
    var currentStep by remember { mutableStateOf(ConnectionStep.START) }

    when (currentStep) {
        ConnectionStep.START -> {
            DeviceConnectionStartScreen(
                viewModel = viewModel,
                onStartScan = { currentStep = ConnectionStep.SCANNING },
                onSkip = onSkip
            )
        }

        ConnectionStep.SCANNING -> {
            DeviceScanningScreen(
                viewModel = viewModel,
                onDeviceConnected = onDeviceConnected,
                onTroubleshoot = { currentStep = ConnectionStep.TROUBLESHOOT }
            )
        }

        ConnectionStep.TROUBLESHOOT -> {
            TroubleshootScreen(
                onBack = { currentStep = ConnectionStep.START },
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