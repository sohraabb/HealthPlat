package com.bonyad.healthplat.ui.dashboard.profile.ring

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.R
import com.bonyad.healthplat.blesdk.model.ConnectionState
import com.bonyad.healthplat.ui.utils.rtl
import com.bonyad.healthplat.ui.utils.toFarsiDigits
import kotlinx.coroutines.flow.collectLatest

// Colors used in the Ring screen
private val TealColor = Color(0xFF5BA3A3)
private val RedColor = Color(0xFFE53935)
private val GreenColor = Color(0xFF4CAF50)
private val DarkCardColor = Color(0xFF2C2C2C)
private val LightGrayBg = Color(0xFFF5F5F5)
private val MediumGray = Color(0xFF999999)
private val DarkText = Color(0xFF2C2C2C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingScreen(
    viewModel: RingViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDisconnectSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val isConnected = uiState.connectionState == ConnectionState.CONNECTED

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = DarkText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ring Image
            Image(
                painter = painterResource(R.drawable.ring_img),
                contentDescription = "حلقه",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(horizontal = 40.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Status Chip
            if (isConnected) {
                ConnectedStatusChip(batteryLevel = uiState.batteryLevel)
            } else {
                DisconnectedStatusChip()
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content area with padding
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // Firmware Update Banner (connected only)
                if (isConnected) {
                    FirmwareUpdateBanner(
                        onInstallClick = {
                            Toast.makeText(context, "به زودی...".rtl(), Toast.LENGTH_SHORT).show()
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Info Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BatteryInfoCard(
                        batteryLevel = if (isConnected) uiState.batteryLevel else null,
                        isConnected = isConnected,
                        modifier = Modifier.weight(1f)
                    )
                    SignalInfoCard(
                        isConnected = isConnected,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                if (isConnected) {
                    ConnectedActionButtons(
                        isSyncing = uiState.isSyncing,
                        onDisconnectClick = { showDisconnectSheet = true },
                        onSyncClick = { viewModel.syncData() }
                    )
                } else {
                    ReconnectButton(
                        isReconnecting = uiState.isReconnecting,
                        onClick = { viewModel.reconnect() }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Device Details Card
                DeviceDetailsCard(
                    firmwareVersion = uiState.firmwareVersion,
                    lastSyncTime = uiState.lastSyncTime,
                    deviceMac = uiState.deviceMac,
                    isConnected = isConnected
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Footer text
                Text(
                    text = "برای بهترین عملکرد، باتری حلقه را بالای ۳۰٪ نگه دارید.".toFarsiDigits(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MediumGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Disconnect Confirmation Bottom Sheet
    if (showDisconnectSheet) {
        DisconnectBottomSheet(
            onDismiss = { showDisconnectSheet = false },
            onConfirm = {
                showDisconnectSheet = false
                viewModel.disconnect()
            }
        )
    }
}

// ─── Status Chips ────────────────────────────────────────────────

@Composable
private fun ConnectedStatusChip(batteryLevel: Int?) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "متصل",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = DarkText
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(GreenColor)
            )
            if (batteryLevel != null) {
                Text(
                    text = "${batteryLevel}%".toFarsiDigits(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkText
                )
            }
        }
    }
}

@Composable
private fun DisconnectedStatusChip() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "قطع ارتباط",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = DarkText
            )
            // Bluetooth off represented as X icon in red
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(RedColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    fontSize = 10.sp,
                    color = RedColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─── Firmware Update Banner ──────────────────────────────────────

@Composable
private fun FirmwareUpdateBanner(onInstallClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkCardColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Install button (left in RTL)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                modifier = Modifier.clickable(onClick = onInstallClick)
            ) {
                Text(
                    text = "نصب",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = DarkText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Text content
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "بروزرسانی جدید",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "نسخه ۳.۰.۱ • رفع باگ و بهبود عملکرد".toFarsiDigits(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFAAAAAA)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Update icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(TealColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = TealColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ─── Info Cards ──────────────────────────────────────────────────

@Composable
private fun BatteryInfoCard(
    batteryLevel: Int?,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isConnected) TealColor else Color(0xFFBDBDBD)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = LightGrayBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quality label (left in RTL)
                if (isConnected && batteryLevel != null) {
                    Text(
                        text = if (batteryLevel > 50) "عالی" else if (batteryLevel > 20) "متوسط" else "ضعیف",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Lightning bolt icon
                Text(
                    text = "⚡",
                    fontSize = 20.sp,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Value
            Text(
                text = if (isConnected && batteryLevel != null) "${batteryLevel}%".toFarsiDigits() else "-",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = DarkText,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Label
            Text(
                text = "باتری باقی‌مانده",
                style = MaterialTheme.typography.bodySmall,
                color = MediumGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun SignalInfoCard(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isConnected) Color(0xFF3F51B5) else Color(0xFFBDBDBD)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = LightGrayBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quality label
                Text(
                    text = if (isConnected) "عالی" else "blue",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor
                )

                // Signal bars icon using unicode
                Text(
                    text = "📶",
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Value
            Text(
                text = if (isConnected) "-۴۲ dBm".toFarsiDigits() else "-",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = DarkText,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Label
            Text(
                text = "کیفیت سیگنال",
                style = MaterialTheme.typography.bodySmall,
                color = MediumGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }
    }
}

// ─── Action Buttons ──────────────────────────────────────────────

@Composable
private fun ConnectedActionButtons(
    isSyncing: Boolean,
    onDisconnectClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Disconnect button
        OutlinedButton(
            onClick = onDisconnectClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedColor),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Text(text = "✕", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "قطع اتصال",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
        }

        // Sync button
        OutlinedButton(
            onClick = onSyncClick,
            enabled = !isSyncing,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkText),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = DarkText
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "همگام‌سازی",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}

@Composable
private fun ReconnectButton(
    isReconnecting: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isReconnecting,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DarkCardColor),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        if (isReconnecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = "᎒", fontSize = 18.sp) // Bluetooth symbol placeholder
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isReconnecting) "در حال اتصال...".rtl() else "اتصال مجدد",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
        )
    }
}

// ─── Device Details Card ─────────────────────────────────────────

@Composable
private fun DeviceDetailsCard(
    firmwareVersion: String?,
    lastSyncTime: String?,
    deviceMac: String?,
    isConnected: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = LightGrayBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "جزئیات دستگاه",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = DarkText
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = DarkText,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Firmware Version
            DeviceDetailRow(
                label = "نسخه فریمور",
                value = firmwareVersion ?: "نامشخص",
                icon = Icons.Default.Star
            )

            HorizontalDivider(
                color = Color(0xFFE0E0E0),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Last Sync
            DeviceDetailRow(
                label = "آخرین بروزرسانی",
                value = if (isConnected && lastSyncTime != null) lastSyncTime else "نامشخص",
                icon = Icons.Default.Refresh
            )

            HorizontalDivider(
                color = Color(0xFFE0E0E0),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Serial Number (MAC)
            DeviceDetailRow(
                label = "شماره سریال",
                value = formatMacAsSerial(deviceMac),
                icon = Icons.Default.Warning
            )
        }
    }
}

@Composable
private fun DeviceDetailRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Value (left in RTL)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MediumGray,
            modifier = Modifier.weight(1f)
        )

        // Label (right in RTL)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = DarkText
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MediumGray,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun formatMacAsSerial(mac: String?): String {
    if (mac.isNullOrBlank()) return "نامشخص"
    // Format: AA:BB:CC:DD:EE:FF → AABB-CCDD-EEFF
    val cleaned = mac.replace(":", "")
    return if (cleaned.length >= 12) {
        "${cleaned.substring(0, 4)}-${cleaned.substring(4, 8)}-${cleaned.substring(8, 12)}".uppercase()
    } else {
        mac
    }
}

// ─── Disconnect Bottom Sheet ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisconnectBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        color = Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close button (top-right)
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "بستن",
                        tint = MediumGray
                    )
                }
            }

            // Bluetooth off icon in red circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(RedColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    fontSize = 24.sp,
                    color = RedColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "قطع ارتباط با حلقه؟",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = DarkText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "دریافت اطلاعات لحظه‌ای متوقف\nخواهد شد.",
                style = MaterialTheme.typography.bodyMedium,
                color = MediumGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Confirm button (red)
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedColor),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Text(
                    text = "بله، قطع کن",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cancel button (outlined)
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Text(
                    text = "انصراف",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = DarkText
                )
            }
        }
    }
}
