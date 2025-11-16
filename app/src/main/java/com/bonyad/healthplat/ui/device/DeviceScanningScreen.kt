package com.bonyad.healthplat.ui.device

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonlala.bonlalable.bean.ScanDeviceInfo
import timber.log.Timber

@Composable
fun DeviceScanningScreen(
    viewModel: DeviceConnectionViewModel = hiltViewModel(),
    onDeviceConnected: () -> Unit,
    onTroubleshoot: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val scanDuration by viewModel.scanDuration.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(enabled = onBack != null) {
        viewModel.stopScan()
        onBack?.invoke()
    }

    LaunchedEffect(scannedDevices.size) {
        Timber.d("UI: Scanned devices count changed: ${scannedDevices.size}")
        scannedDevices.forEach { device ->
            Timber.d("UI: Device - ${device.bluetoothDevice?.address}")
        }
    }

    // Navigate on success
    LaunchedEffect(uiState) {
        if (uiState is DeviceConnectionUiState.Connected) {
            onDeviceConnected()
        }
    }

    // Show errors
    LaunchedEffect(uiState) {
        if (uiState is DeviceConnectionUiState.Error) {
            snackbarHostState.showSnackbar((uiState as DeviceConnectionUiState.Error).message)
            viewModel.resetError()
        }
    }

    // Start scan on mount
    LaunchedEffect(Unit) {
        viewModel.startScan()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Add back button at top
                if (onBack != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.stopScan()
                                onBack()
                            },
                            modifier = Modifier
                                .background(Color.White, CircleShape)
                                .size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "بازگشت",
                                tint = Color(0xFF2C2C2C)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Animated scanning indicator
                if (uiState is DeviceConnectionUiState.Scanning) {
                    ScanningAnimation()
                } else if (uiState is DeviceConnectionUiState.Connecting) {
                    ConnectingAnimation()
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Title
                Text(
                    text = when (uiState) {
                        is DeviceConnectionUiState.Scanning -> "جستجو برای دستگاه شما"
                        is DeviceConnectionUiState.Connecting -> "در حال اتصال..."
                        else -> "جستجو برای دستگاه شما"
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center
                    ),
                    color = Color(0xFF2C2C2C),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Instruction
                if (uiState is DeviceConnectionUiState.Scanning) {
                    Text(
                        text = if (scanDuration > 10) {
                            "جستجو بیش از حد طول کشید؟ دستگاه خود را به شارژر وصل کنید."
                        } else {
                            "لطفا صبر کنید..."
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 26.sp
                        ),
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                }

                // Device list
                if (scannedDevices.isNotEmpty()) {
                    Text(
                        text = "دستگاه‌های یافت شده:",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Color(0xFF2C2C2C),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp, start = 8.dp),
                        textAlign = TextAlign.End
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(scannedDevices) { device ->
                            DeviceListItem(
                                device = device,
                                isConnecting = uiState is DeviceConnectionUiState.Connecting,
                                onClick = { viewModel.connectToDevice(device) }
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Troubleshoot button
                TextButton(
                    onClick = onTroubleshoot,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = "مشکل در اتصال؟",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp
                        ),
                        color = Color(0xFF5BA3A3)
                    )
                }
            }
        }
    }
}

@Composable
fun ScanningAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer circle
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = Color(0xFF5BA3A3).copy(alpha = 0.1f),
                    shape = CircleShape
                )
        )

        // Middle circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = Color(0xFF5BA3A3).copy(alpha = 0.2f),
                    shape = CircleShape
                )
                .rotate(rotation)
        )

        // Inner icon
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = Color(0xFF5BA3A3)
        )
    }
}

@Composable
fun ConnectingAnimation() {
    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(60.dp),
            color = Color(0xFF5BA3A3),
            strokeWidth = 4.dp
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceListItem(
    device: ScanDeviceInfo,
    isConnecting: Boolean,
    onClick: () -> Unit
) {

    val deviceName = remember(device) {
        device.bluetoothDevice?.name?.takeIf { it.isNotBlank() } ?: "دستگاه ناشناس"
    }

    val deviceAddress = remember(device) {
        device.bluetoothDevice?.address ?: "Unknown"
    }


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnecting) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device info
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = device.bluetoothDevice?.name ?: "دستگاه ناشناس",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = Color(0xFF2C2C2C)
                )

                Text(
                    text = device.bluetoothDevice?.address ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = Color(0xFF999999),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Bluetooth icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color(0xFF5BA3A3).copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = Color(0xFF5BA3A3),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}