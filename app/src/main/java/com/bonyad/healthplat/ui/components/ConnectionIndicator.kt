package com.bonyad.healthplat.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bonyad.healthplat.ui.utils.toFarsiDigits
import com.bonyad.healthplat.R

// ======================= COLOR DEFINITIONS =======================
private val TealPrimary = Color(0xFF5BA3A3)
private val TealLight = Color(0xFFE8F4F4)
private val RedAccent = Color(0xFFE53935)
private val RedLight = Color(0xFFFFEBEE)
private val GrayLight = Color(0xFFE0E0E0)
private val GrayMedium = Color(0xFF9E9E9E)
private val TextDark = Color(0xFF2C2C2C)
private val TextGray = Color(0xFF666666)

// ======================= CONNECTION STATE =======================
sealed class DeviceConnectionState {
    object Disconnected : DeviceConnectionState()
    data class Connected(val batteryLevel: Int) : DeviceConnectionState() {
        val isLowBattery: Boolean get() = batteryLevel <= 20
    }
}

// ======================= MAIN INDICATOR BUTTON =======================
@Composable
fun ConnectionIndicatorButton(
    connectionState: DeviceConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when (connectionState) {
            is DeviceConnectionState.Disconnected -> {
                IconButton(onClick = onClick, modifier = modifier) {
                    Icon(
                        painter = painterResource(id = R.drawable.disconnect),
                        contentDescription = "دستگاه متصل نیست",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            is DeviceConnectionState.Connected -> {
                if (connectionState.isLowBattery) {
                    LowBatteryIndicator(batteryLevel = connectionState.batteryLevel)
                } else {
                    ConnectedIndicator()
                }
            }
        }
    }
}

// ======================= INDICATOR VARIANTS =======================

@Composable
private fun ConnectedIndicator() {
    Box(
        modifier = Modifier.size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 3.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2

            // Outer arc (partial circle - ~270 degrees)
            drawArc(
                color = TealPrimary,
                startAngle = -90f,
                sweepAngle = 300f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.width - strokeWidth, size.height - strokeWidth)
            )

            // Inner filled circle
            drawCircle(
                color = TealPrimary.copy(alpha = 0.3f),
                radius = radius * 0.5f,
                center = center
            )

            // Center dot
            drawCircle(
                color = TealPrimary,
                radius = radius * 0.25f,
                center = center
            )
        }
    }
}

@Composable
private fun DisconnectedIndicator() {
    Box(
        modifier = Modifier.size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2

            // Outer circle
            drawCircle(
                color = GrayMedium,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // X mark
            val xSize = radius * 0.5f
            val xStroke = 2.dp.toPx()

            // Line 1 of X
            drawLine(
                color = RedAccent,
                start = Offset(center.x - xSize, center.y - xSize),
                end = Offset(center.x + xSize, center.y + xSize),
                strokeWidth = xStroke,
                cap = StrokeCap.Round
            )

            // Line 2 of X
            drawLine(
                color = RedAccent,
                start = Offset(center.x + xSize, center.y - xSize),
                end = Offset(center.x - xSize, center.y + xSize),
                strokeWidth = xStroke,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun LowBatteryIndicator(batteryLevel: Int) {
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        // Base circle indicator (similar to connected but lighter)
        Canvas(modifier = Modifier.size(28.dp)) {
            val strokeWidth = 2.dp.toPx()

            // Outer arc
            drawArc(
                color = GrayMedium,
                startAngle = -90f,
                sweepAngle = 300f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.width - strokeWidth, size.height - strokeWidth)
            )

            // Red arc showing low battery
            drawArc(
                color = RedAccent,
                startAngle = -90f,
                sweepAngle = (batteryLevel / 100f) * 300f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.width - strokeWidth, size.height - strokeWidth)
            )

            // Center dot
            drawCircle(
                color = GrayMedium.copy(alpha = 0.3f),
                radius = size.minDimension * 0.2f,
                center = center
            )
        }

        // Battery badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 4.dp, y = 4.dp)
                .background(RedAccent, RoundedCornerShape(6.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${batteryLevel}%".toFarsiDigits(),
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ======================= BOTTOM SHEETS =======================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectivityBottomSheet(
    connectionState: DeviceConnectionState,
    onDismiss: () -> Unit,
    onConnectClick: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(GrayLight, RoundedCornerShape(2.dp))
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
                // Title
                Text(
                    text = "اتصال حلقه",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    ),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                when (connectionState) {
                    is DeviceConnectionState.Disconnected -> {
                        DisconnectedBottomSheetContent(onConnectClick = onConnectClick)
                    }
                    is DeviceConnectionState.Connected -> {
                        ConnectedBottomSheetContent(batteryLevel = connectionState.batteryLevel)
                    }
                }
            }
        }
    }


@Composable
private fun DisconnectedBottomSheetContent(onConnectClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Connection Status Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Badge
            Box(
                modifier = Modifier
                    .background(RedAccent, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "متصل نیست",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Label
            Text(
                text = "وضعیت اتصال",
                color = TextGray,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Connect Button
        OutlinedButton(
            onClick = onConnectClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, TealPrimary),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TealPrimary
            )
        ) {
            Text(
                text = "اتصال حلقه",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ConnectedBottomSheetContent(batteryLevel: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Connection Status Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(TealPrimary, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "متصل",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = "وضعیت اتصال",
                color = TextGray,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Battery Level Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top // Changed to Top to align with the multiline label on the right
        ) {
            // Battery Info (Now just the percentage)
            Text(
                text = "${batteryLevel}%".toFarsiDigits(),
                color = TextDark,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Right Side: Label + Estimated Days
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "میزان باتری",
                    color = TextGray,
                    fontSize = 14.sp
                )

                // Estimated days remaining (rough calculation: 1% ≈ 0.2 days for a ring)
                val estimatedDays = (batteryLevel * 0.2f).toInt().coerceAtLeast(1)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    Text(
                        text = "روز",
                        color = TextGray,
                        fontSize = 12.sp
                    )

                    Text(
                        text = estimatedDays.toString().toFarsiDigits(),
                        color = TextGray,
                        fontSize = 12.sp
                    )

                    Icon(
                        painter = painterResource(id = R.drawable.time_mini),
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Battery Progress Bar
        BatteryProgressBar(
            progress = batteryLevel / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
        )
    }
}

@Composable
private fun BatteryProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500),
        label = "battery_progress"
    )

    val progressColor by animateColorAsState(
        targetValue = when {
            progress <= 0.2f -> RedAccent
            progress <= 0.4f -> Color(0xFFFF9800) // Orange
            else -> TealPrimary
        },
        animationSpec = tween(durationMillis = 300),
        label = "battery_color"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(GrayLight)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .background(progressColor, RoundedCornerShape(4.dp))
        )
    }
}

// ======================= PREVIEW HELPERS =======================

@Composable
fun ConnectionIndicatorPreview() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        ConnectionIndicatorButton(
            connectionState = DeviceConnectionState.Connected(batteryLevel = 75),
            onClick = {}
        )
        ConnectionIndicatorButton(
            connectionState = DeviceConnectionState.Disconnected,
            onClick = {}
        )
        ConnectionIndicatorButton(
            connectionState = DeviceConnectionState.Connected(batteryLevel = 15),
            onClick = {}
        )
    }
}