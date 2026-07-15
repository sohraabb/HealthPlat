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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonlala.bonlalable.bean.ScanDeviceInfo
import com.bonyad.healthplat.R
import com.bonyad.healthplat.logging.LogFiles
import com.bonyad.healthplat.ui.utils.rtl

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

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Silently mirror the log file to the public Downloads folder when the user
    // leaves this screen, so it can be retrieved/sent later. No visible UI.
    DisposableEffect(Unit) {
        onDispose { LogFiles.exportToDownloads(context) }
    }

    BackHandler(enabled = onBack != null) {
        viewModel.stopScan()
        onBack?.invoke()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHANGE D (UI side): Navigate on ReadyToNavigate, not Connected.
    //
    // BEFORE: We navigated immediately on Connected — zero visual feedback.
    // AFTER:  Connected shows a success indicator. ReadyToNavigate triggers
    //         the actual navigation after the 1.2s delay in the ViewModel.
    // ─────────────────────────────────────────────────────────────────────────
    LaunchedEffect(uiState) {
        when (uiState) {
            is DeviceConnectionUiState.ReadyToNavigate -> onDeviceConnected()
            is DeviceConnectionUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as DeviceConnectionUiState.Error).message)
                viewModel.resetError()
            }
            else -> Unit
        }
    }

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

                Text(
                    text = "جستجو برای حلقه شما",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = Color(0xFF2C2C2C)
                )

                Spacer(modifier = Modifier.height(40.dp))

                // ─────────────────────────────────────────────────────────
                // CHANGE D (UI): Add Connected state visual indicator
                // ─────────────────────────────────────────────────────────
                when (uiState) {
                    is DeviceConnectionUiState.Scanning,
                    is DeviceConnectionUiState.Idle -> CompactSpinner()

                    is DeviceConnectionUiState.Connecting,
                    is DeviceConnectionUiState.WaitingForPairing,
                    is DeviceConnectionUiState.Initializing -> CompactSpinner(color = Color(0xFF5BA3A3))

                    is DeviceConnectionUiState.Connected,
                    is DeviceConnectionUiState.ReadyToNavigate -> {
                        // Success checkmark with green circle
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFF4CAF50).copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✓",
                                fontSize = 28.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    else -> CompactSpinner()
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ─────────────────────────────────────────────────────────
                // CHANGE D (UI): Status text now covers the Connected state
                // ─────────────────────────────────────────────────────────
                val statusText: String? = when (uiState) {
                    is DeviceConnectionUiState.Connecting -> "در حال برقراری ارتباط...".rtl()
                    is DeviceConnectionUiState.WaitingForPairing -> "درخواست جفت‌سازی را تایید کنید"
                    is DeviceConnectionUiState.Initializing -> "در حال آماده‌سازی دستگاه...".rtl()
                    is DeviceConnectionUiState.Connected,
                    is DeviceConnectionUiState.ReadyToNavigate -> "دستگاه متصل شد"
                    is DeviceConnectionUiState.Scanning -> {
                        if (scanDuration > 15) "؟جستجو بیش از حد طول کشید" else null
                    }
                    else -> null
                }

                statusText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        ),
                        // Green text for connected confirmation
                        color = if (uiState is DeviceConnectionUiState.Connected ||
                            uiState is DeviceConnectionUiState.ReadyToNavigate
                        ) Color(0xFF4CAF50) else Color(0xFF666666)
                    )
                }

                if (uiState is DeviceConnectionUiState.Scanning && scanDuration > 15) {
                    Text(
                        text = ".حلقه خود را به شارژر وصل کنید",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        ),
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

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

                // Device list — hide during connecting/initializing/connected
                val showDeviceList = uiState is DeviceConnectionUiState.Scanning ||
                        uiState is DeviceConnectionUiState.Idle

                if (scannedDevices.isNotEmpty() && showDeviceList) {
                    Text(
                        text = ":دستگاه‌های یافت شده",
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

                val showTroubleshoot = uiState !is DeviceConnectionUiState.Connecting &&
                        uiState !is DeviceConnectionUiState.WaitingForPairing &&
                        uiState !is DeviceConnectionUiState.Initializing &&
                        uiState !is DeviceConnectionUiState.Connected &&
                        uiState !is DeviceConnectionUiState.ReadyToNavigate

                if (showTroubleshoot) {
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
            val alpha = 1f - (i * (0.85f / lineCount))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = angle }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                        .width(4.dp)
                        .height(10.dp)
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
    val deviceName = device.bluetoothDevice?.name
    val deviceAddress = device.bluetoothDevice?.address

    // ─────────────────────────────────────────────────────────────────────────
    // CHANGE C (UI side): Since we now show ALL devices in scan results,
    // we visually distinguish likely-ring devices from unknowns.
    // The user can still tap any device — we just give them a hint.
    // ─────────────────────────────────────────────────────────────────────────
    val looksLikeRing = !deviceName.isNullOrBlank() && (
            deviceName.contains("ring", ignoreCase = true) ||
                    deviceName.contains("bonlala", ignoreCase = true) ||
                    deviceName.startsWith("W", ignoreCase = false)
            )

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
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = deviceName ?: "دستگاه ناشناس",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = if (isConnecting) Color(0xFF999999) else Color(0xFF2C2C2C)
                )

                Text(
                    text = deviceAddress ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = Color(0xFF999999),
                    modifier = Modifier.padding(top = 2.dp)
                )

                // Hint label for non-ring-looking devices
                if (!looksLikeRing) {
                    Text(
                        text = "ممکن است حلقه نباشد",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = Color(0xFFBBBBBB),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (looksLikeRing) Color(0xFF5BA3A3).copy(alpha = 0.1f)
                        else Color(0xFFBBBBBB).copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ring_img),
                    contentDescription = null,
                    tint = if (looksLikeRing) Color(0xFF5BA3A3) else Color(0xFFBBBBBB),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}