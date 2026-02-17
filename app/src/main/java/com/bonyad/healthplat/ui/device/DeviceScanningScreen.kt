package com.bonyad.healthplat.ui.device

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bonlala.bonlalable.bean.ScanDeviceInfo
import com.bonyad.healthplat.R
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
            // Back button
            if (onBack != null) {
                IconButton(
                    onClick = { onBack() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(start = 16.dp, top = 8.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.back_arrow),
                        contentDescription = "بازگشت",
                        tint = Color(0xFF2C2C2C)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(80.dp))

                // Title
                Text(
                    text = "جستجو برای حلقه شما",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = Color(0xFF2C2C2C)
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Compact iOS-style spinner
                when (uiState) {
                    is DeviceConnectionUiState.Scanning,
                    is DeviceConnectionUiState.Idle -> {
                        CompactSpinner()
                    }
                    is DeviceConnectionUiState.Connecting,
                    is DeviceConnectionUiState.WaitingForPairing,
                    is DeviceConnectionUiState.Initializing -> {
                        // Teal colored spinner when connecting
                        CompactSpinner(color = Color(0xFF5BA3A3))
                    }
                    is DeviceConnectionUiState.Connected -> {
                        // Checkmark or success indicator
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF4CAF50).copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✓",
                                fontSize = 20.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    else -> CompactSpinner()
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Status message
                val statusText = when (uiState) {
                    is DeviceConnectionUiState.Connecting -> "در حال برقراری ارتباط..."
                    is DeviceConnectionUiState.WaitingForPairing -> "درخواست جفت‌سازی را تایید کنید"
                    is DeviceConnectionUiState.Initializing -> "در حال آماده‌سازی دستگاه..."
                    is DeviceConnectionUiState.Connected -> "اتصال برقرار شد"
                    else -> {
                        if (scanDuration > 15) {
                            "جستجو بیش از حد طول کشید؟"
                        } else null
                    }
                }

                statusText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        ),
                        color = Color(0xFF666666)
                    )
                }

                // Secondary hint when scanning takes long
                if (uiState is DeviceConnectionUiState.Scanning && scanDuration > 15) {
                    Text(
                        text = "حلقه خود را به شارژر وصل کنید.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        ),
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Pairing hint
                if (uiState is DeviceConnectionUiState.WaitingForPairing) {
                    Text(
                        text = "یک پیام در بخش اعلان‌ها ظاهر می‌شود",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        ),
                        color = Color(0xFF999999),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Device list
                if (scannedDevices.isNotEmpty()) {
                    Text(
                        text = "دستگاه‌های یافت شده:",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        ),
                        color = Color(0xFF666666),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        textAlign = TextAlign.End
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(scannedDevices) { device ->
                            DeviceListItem(
                                device = device,
                                isConnecting = uiState is DeviceConnectionUiState.Connecting ||
                                        uiState is DeviceConnectionUiState.WaitingForPairing ||
                                        uiState is DeviceConnectionUiState.Initializing,
                                onClick = { viewModel.connectToDevice(device) }
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Troubleshoot button
                if (uiState !is DeviceConnectionUiState.Connecting &&
                    uiState !is DeviceConnectionUiState.WaitingForPairing &&
                    uiState !is DeviceConnectionUiState.Initializing
                ) {
                    TextButton(
                        onClick = onTroubleshoot,
                        modifier = Modifier.padding(vertical = 16.dp)
                    ) {
                        Text(
                            text = "مشکل در اتصال ؟",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = Color(0xFF5BA3A3)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(56.dp))
                }
            }
        }
    }
}

@Composable
fun CompactSpinner(
    size: Dp = 40.dp,
    color: Color = Color(0xFF2C2C2C),
    lineCount: Int = 8
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation },
        contentAlignment = Alignment.Center
    ) {
        for (i in 0 until lineCount) {
            val angle = i * (360f / lineCount)
            // Opacity decreases as we go around (creates the spinning trail effect)
            val alpha = 1f - (i * (0.85f / lineCount))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = angle }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp) // Small gap from edge
                        .width(4.dp)   // Fixed line width
                        .height(10.dp) // Fixed line height
                        .background(
                            color = color.copy(alpha = alpha.coerceIn(0.15f, 1f)),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceListItem(
    device: ScanDeviceInfo,
    isConnecting: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnecting) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnecting) Color(0xFFF0F0F0) else Color.White
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
                    color = if (isConnecting) Color(0xFF999999) else Color(0xFF2C2C2C)
                )

                Text(
                    text = device.bluetoothDevice?.address ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color(0xFF999999),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Ring icon
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
                    painter = painterResource(id = R.drawable.ring_img),
                    contentDescription = null,
                    tint = Color(0xFF5BA3A3),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}