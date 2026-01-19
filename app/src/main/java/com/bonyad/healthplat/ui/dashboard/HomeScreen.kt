package com.bonyad.healthplat.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bonyad.healthplat.R
import com.bonyad.healthplat.ui.components.ConnectionIndicatorButton
import com.bonyad.healthplat.ui.components.ConnectivityBottomSheet
import com.bonyad.healthplat.ui.components.DeviceConnectionState
import com.bonyad.healthplat.ui.components.InfoBottomSheet
import com.bonyad.healthplat.ui.navigation.HealthDetailRoutes
import com.bonyad.healthplat.ui.navigation.NavRoutes
import com.bonyad.healthplat.ui.utils.toFarsiDigits

// Define custom colors from design
val TealPrimary = Color(0xFF5BA3A3)
val OrangeAccent = Color(0xFFFF9800)
val RedAccent = Color(0xFFE53935)
val BlueAccent = Color(0xFF2196F3)
val TextDark = Color(0xFF2C2C2C)
val TextGray = Color(0xFF999999)

// Data class for health cards
data class HealthCardData(
    val title: String,
    val value: String,
    val unit: String?,
    val statusText: String?,
    val iconRes: Int,
    val iconTint: Color,
    val route: String,
    val chartType: ChartType
)

enum class ChartType {
    HEART_RATE,
    STEPS,
    SPO2,
    SLEEP,
    STRESS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DashboardViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAi: (String) -> Unit
) {
    val healthOverview by viewModel.healthOverview.collectAsState()
    val userName by viewModel.userName.collectAsState()

    val readinessScore by viewModel.readinessScore.collectAsState()
    val insights by viewModel.healthInsights.collectAsState()

    // ==================== CONNECTIVITY STATE ====================
    var showConnectivitySheet by remember { mutableStateOf(false) }
    val connectivitySheetState = rememberModalBottomSheetState()

    // Determine connection state for indicator
    val connectionState = remember(healthOverview.isDeviceConnected, healthOverview.batteryLevel) {
        if (healthOverview.isDeviceConnected) {
            DeviceConnectionState.Connected(batteryLevel = healthOverview.batteryLevel ?: 50)
        } else {
            DeviceConnectionState.Disconnected
        }
    }
    // ============================================================

    // ==================== INFO BOTTOM SHEET STATE ====================
    var showInfoSheet by remember { mutableStateOf(false) }
    val infoSheetState = rememberModalBottomSheetState()
    // =================================================================

    val healthCards = remember(healthOverview) {
        listOf(
            HealthCardData(
                title = "ضربان قلب",
                value = healthOverview.heartRate.toString().toFarsiDigits(),
                unit = "bpm",
                statusText = when {
                    healthOverview.heartRate == 0 -> "در انتظار داده"
                    healthOverview.heartRate < 60 -> "کمتر از حد طبیعی"
                    healthOverview.heartRate > 100 -> "بالاتر از حد طبیعی"
                    else -> "عادی"
                },
                iconRes = R.drawable.heart_rate,
                iconTint = RedAccent,
                route = HealthDetailRoutes.HeartRateDetail.route,
                chartType = ChartType.HEART_RATE
            ),
            HealthCardData(
                title = "تعداد قدم",
                value = healthOverview.steps.toString().toFarsiDigits(),
                unit = "قدم",
                statusText = when {
                    healthOverview.steps == 0 -> "شروع کن!"
                    healthOverview.steps < 5000 -> "بیشتر کن"
                    healthOverview.steps < 10000 -> "خوب پیش میری"
                    else -> "عالی!"
                },
                iconRes = R.drawable.walk,
                iconTint = OrangeAccent,
                route = HealthDetailRoutes.StepsDetail.route,
                chartType = ChartType.STEPS
            ),
            HealthCardData(
                title = "اکسیژن خون",
                value = if (healthOverview.bloodOxygen > 0) {
                    healthOverview.bloodOxygen.toString().toFarsiDigits()
                } else {
                    "0"
                },
                unit = "%",
                statusText = when {
                    healthOverview.bloodOxygen == 0 -> ""
                    healthOverview.bloodOxygen < 90 -> "پایین"
                    healthOverview.bloodOxygen < 95 -> "قابل قبول"
                    else -> "عادی"
                },
                iconRes = R.drawable.hospital,
                iconTint = BlueAccent,
                route = HealthDetailRoutes.SpO2Detail.route,
                chartType = ChartType.SPO2
            ),
            HealthCardData(
                title = "خواب",
                value = if (healthOverview.sleepDurationHours > 0) {
                    String.format("%.1f", healthOverview.sleepDurationHours).toFarsiDigits()
                } else {
                    "0"
                },
                unit = "ساعت",
                statusText = when {
                    healthOverview.sleepDurationHours == 0f -> ""
                    healthOverview.sleepDurationHours < 6f -> "کم خوابیدی"
                    healthOverview.sleepDurationHours < 8f -> "خوب"
                    healthOverview.sleepDurationHours > 9f -> "زیاد خوابیدی"
                    else -> "عالی"
                },
                iconRes = R.drawable.chemistry_flask,
                iconTint = TealPrimary,
                route = HealthDetailRoutes.SleepDetail.route,
                chartType = ChartType.SLEEP
            ),
            HealthCardData(
                title = "استرس",
                value = if (healthOverview.stressLevel > 0) {
                    healthOverview.stressLevel.toString().toFarsiDigits()
                } else {
                    "0"
                },
                unit = null,
                statusText = when {
                    healthOverview.stressLevel == 0 -> ""
                    healthOverview.stressLevel < 30 -> "آرام"
                    healthOverview.stressLevel < 60 -> "متوسط"
                    else -> "بالا"
                },
                iconRes = R.drawable.care,
                iconTint = TealPrimary,
                route = HealthDetailRoutes.StressDetail.route,
                chartType = ChartType.STRESS
            )
        )
    }

    // ==================== CONNECTIVITY BOTTOM SHEET ====================
    if (showConnectivitySheet) {
        ConnectivityBottomSheet(
            connectionState = connectionState,
            onDismiss = { showConnectivitySheet = false },
            onConnectClick = {
                showConnectivitySheet = false
                viewModel.connectDevice()
            },
            sheetState = connectivitySheetState
        )
    }
    // ===================================================================

    // ==================== INFO BOTTOM SHEET ====================
    if (showInfoSheet) {
        InfoBottomSheet(
            onDismiss = { showInfoSheet = false },
            onFaqClick = {
                // TODO: Navigate to FAQ screen
                // onNavigateToDetail("faq")
            },
            onContactClick = {
                // TODO: Navigate to Contact screen or open contact dialog
                // onNavigateToDetail("contact")
            },
            onAppGuideClick = {
                // TODO: Navigate to App Guide screen
                // onNavigateToDetail("app_guide")
            },
            sheetState = infoSheetState
        )
    }
    // ===========================================================

    Scaffold(
        topBar = {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TopAppBar(
                    title = {
                        Text(
                            text = "سلام ${userName ?: "کاربر"}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { /* TODO: Notifications */ }) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = TextGray
                            )
                        }
                    },
                    actions = {
                        // ==================== CONNECTION INDICATOR BUTTON ====================
                        ConnectionIndicatorButton(
                            connectionState = connectionState,
                            onClick = { showConnectivitySheet = true }
                        )
                        // =====================================================================

                        // ==================== INFO/HELP BUTTON ====================
                        IconButton(onClick = { showInfoSheet = true }) {
                            Icon(
                                painter = painterResource(R.drawable.information),
                                contentDescription = "Help",
                                tint = TextGray
                            )
                        }
                        // ==========================================================
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                )
            }
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            NewHealthStatusCard(
                score = readinessScore,
                insights = insights,
                navigateToAi = { onNavigateToAi(NavRoutes.AiScreen.route) }
            )

            // Horizontal scrollable health cards in 2x2 grid
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                // Group cards into pages of 4 (2x2 grid)
                val pages = healthCards.chunked(4)
                items(pages.size) { pageIndex ->
                    val pageCards = pages[pageIndex]
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Top row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pageCards.getOrNull(0)?.let { card ->
                                HealthMetricCard(
                                    data = card,
                                    onClick = { onNavigateToDetail(card.route) }
                                )
                            }
                            pageCards.getOrNull(1)?.let { card ->
                                HealthMetricCard(
                                    data = card,
                                    onClick = { onNavigateToDetail(card.route) }
                                )
                            }
                        }
                        // Bottom row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pageCards.getOrNull(2)?.let { card ->
                                HealthMetricCard(
                                    data = card,
                                    onClick = { onNavigateToDetail(card.route) }
                                )
                            }
                            pageCards.getOrNull(3)?.let { card ->
                                HealthMetricCard(
                                    data = card,
                                    onClick = { onNavigateToDetail(card.route) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp)) // Extra space at bottom
        }
    }
}

// =========================== NEW MAIN CARD ===========================

@Composable
fun NewHealthStatusCard(
    score: Int,
    insights: List<String>,
    navigateToAi: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = TealPrimary.copy(alpha = 0.3f),
                spotColor = TealPrimary.copy(alpha = 0.3f)
            )
            .border(1.dp, TealPrimary.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "وضعیت کلی سلامت",
                style = MaterialTheme.typography.titleMedium,
                color = TextDark,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // --- MODIFIED CIRCLE SECTION ---
                HealthScoreCircle(
                    score = score,
                    modifier = Modifier.size(120.dp) // Increased size slightly to fit dots
                )
                // -------------------------------

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    insights.forEach { insight ->
                        InsightItem(text = insight)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { navigateToAi() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                border = BorderStroke(1.dp, TealPrimary)
            ) {
                Text(
                    text = "تحلیل هوش مصنوعی",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * Custom Component to draw the dotted ring and thinner progress bar
 */
@Composable
fun HealthScoreCircle(
    score: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 1. The Dotted Ring (Canvas)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = this.center
            // Radius is half the size, minus a little padding so dots aren't cut off
            val radius = (size.minDimension / 2) - 2.dp.toPx()
            val dotRadius = 1.5.dp.toPx() // Size of the dots
            val step = 10 // Degrees between dots (360 / 10 = 36 dots)

            for (angle in 0 until 360 step step) {
                val rad = Math.toRadians(angle.toDouble())
                val x = center.x + (radius * Math.cos(rad)).toFloat()
                val y = center.y + (radius * Math.sin(rad)).toFloat()

                drawCircle(
                    color = TealPrimary.copy(alpha = 0.2f), // Faint dots
                    radius = dotRadius,
                    center = Offset(x, y)
                )
            }
        }

        // 2. The Progress Bar (with padding to sit inside the dots)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp), // Push the circle inside the dots
            contentAlignment = Alignment.Center
        ) {
            // Background Track
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = TealPrimary.copy(alpha = 0.1f),
                strokeWidth = 5.dp, // Thinner
            )
            // Actual Progress
            CircularProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier.fillMaxSize(),
                color = TealPrimary,
                strokeWidth = 5.dp, // Thinner
                strokeCap = StrokeCap.Round,
            )
        }

        // 3. The Text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score.toString().toFarsiDigits(),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    fontSize = 32.sp
                )
            )
            Text(
                text = "/۱۰۰",
                style = MaterialTheme.typography.bodySmall,
                color = TextGray
            )
        }
    }
}

@Composable
fun InsightItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TextGray,
            textAlign = TextAlign.End
        )
        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = TextGray.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
        )
    }
}

// =========================== UNIFIED METRIC CARD ===========================

@Composable
fun HealthMetricCard(
    data: HealthCardData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .height(210.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    painter = painterResource(data.iconRes),
                    contentDescription = data.title,
                    tint = data.iconTint,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = data.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark,
                    fontWeight = FontWeight.Bold
                )
            }

            if (data.statusText != null) {
                Text(
                    text = data.statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // --- CHART AREA ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                ChartContent(chartType = data.chartType, color = data.iconTint)
            }

            // --- FOOTER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = data.value,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = TextDark
                    )

                    if (data.unit != null) {
                        Text(
                            text = data.unit,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(TealPrimary.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Details",
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(-45f)
                    )
                }
            }
        }
    }
}

@Composable
fun ChartContent(chartType: ChartType, color: Color) {
    when (chartType) {
        ChartType.HEART_RATE -> HeartRateChart(color)
        ChartType.STEPS -> StepsChart(color)
        ChartType.SPO2 -> SpO2Chart(color)
        ChartType.SLEEP -> SleepChart(color)
        ChartType.STRESS -> StressChart(color)
    }
}

@Composable
fun HeartRateChart(color: Color) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween, // Evenly space the 5 bars
        verticalAlignment = Alignment.Bottom
    ) {
        // Bar 1: Split (Light Top / Dark Bottom)
        SplitBar(
            color = color,
            totalHeightFraction = 0.65f,
            topFraction = 0.3f,
            bottomFraction = 0.6f
        )

        // Bar 2: Solid Tall Bar
        SolidBar(
            color = color,
            heightFraction = 0.95f
        )

        // Bar 3: Split (Larger Top / Smaller Bottom)
        SplitBar(
            color = color,
            totalHeightFraction = 0.6f,
            topFraction = 0.5f,
            bottomFraction = 0.4f
        )

        // Bar 4: Split (Small Top / Large Bottom)
        SplitBar(
            color = color,
            totalHeightFraction = 0.8f,
            topFraction = 0.2f,
            bottomFraction = 0.7f
        )

        // Bar 5: Solid Tall Bar
        SolidBar(
            color = color,
            heightFraction = 0.95f
        )
    }
}

@Composable
fun StepsChart(color: Color) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        val heights = listOf(0.4f, 0.6f, 0.3f, 0.8f, 0.5f, 0.7f, 0.4f)
        heights.forEach { relativeHeight ->
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight(relativeHeight)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color.copy(alpha = 0.7f))
            )
        }
    }
}

@Composable
fun SpO2Chart(color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        val w = size.width
        val h = size.height

        // Step line path
        val linePath = Path().apply {
            moveTo(0f, h * 0.6f)
            lineTo(w * 0.25f, h * 0.6f)
            lineTo(w * 0.25f, h * 0.3f)
            lineTo(w * 0.5f, h * 0.3f)
            lineTo(w * 0.5f, h * 0.5f)
            lineTo(w * 0.75f, h * 0.5f)
            lineTo(w * 0.75f, h * 0.4f)
            lineTo(w, h * 0.4f)
        }

        // Filled area path (same as line but closed at bottom)
        val fillPath = Path().apply {
            moveTo(0f, h * 0.6f)
            lineTo(w * 0.25f, h * 0.6f)
            lineTo(w * 0.25f, h * 0.3f)
            lineTo(w * 0.5f, h * 0.3f)
            lineTo(w * 0.5f, h * 0.5f)
            lineTo(w * 0.75f, h * 0.5f)
            lineTo(w * 0.75f, h * 0.4f)
            lineTo(w, h * 0.4f)
            // Close the path at the bottom
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        // Draw gradient fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.3f),
                    color.copy(alpha = 0.05f)
                ),
                startY = 0f,
                endY = h
            )
        )

        // Draw the step line on top
        drawPath(
            path = linePath,
            color = color,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Square)
        )
    }
}

@Composable
fun SleepChart(color: Color) {
    // Colors based on the purple palette in the image
    val awakeColor = Color(0xFFE1D5FF)
    val remColor = Color(0xFFC4B5FD)
    val deepColor = Color(0xFF8B5CF6) // Darkest purple
    val lightColor = Color(0xFFDDD6FE)

    val segments = listOf(
        Pair("بیدار", awakeColor),
        Pair("REM", remColor),
        Pair("عمیق", deepColor),
        Pair("سبک", lightColor)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp) // Taller bar like the image
                .clip(RoundedCornerShape(4.dp))
        ) {
            segments.forEach { (label, color) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = label,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontSize = 8.sp
        )
    }
}

@Composable
fun StressChart(color: Color, stressLevel: Int = 90) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 8.dp)
    ) {
        val width = size.width
        val height = size.height

        // The image shows a filled semi-circle/arc
        val arcPath = Path().apply {
            moveTo(0f, height)
            // Create the "hump" shape
            cubicTo(
                width * 0.1f, height * 0.1f,
                width * 0.9f, height * 0.1f,
                width, height
            )
            close()
        }

        drawPath(
            path = arcPath,
            color = Color(0xFFDBAB33) // The golden/yellow color from the screenshot
        )
    }
}

// --- Helper Composables for the specific Figma style ---

@Composable
private fun SolidBar(color: Color, heightFraction: Float) {
    Box(
        modifier = Modifier
            .width(8.dp) // Match thickness from design
            .fillMaxHeight(heightFraction)
            .clip(RoundedCornerShape(50)) // Fully rounded "Pill" shape
            .background(color)
    )
}

@Composable
private fun SplitBar(
    color: Color,
    totalHeightFraction: Float,
    topFraction: Float,
    bottomFraction: Float
) {
    Column(
        modifier = Modifier
            .width(8.dp)
            .fillMaxHeight(totalHeightFraction),
        verticalArrangement = Arrangement.SpaceBetween // Pushes top up and bottom down
    ) {
        // Top Segment (Lighter/Faded)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(topFraction)
                .clip(RoundedCornerShape(50))
                .background(color.copy(alpha = 0.3f)) // Lighter shade for top part
        )

        Spacer(modifier = Modifier.height(3.dp)) // The gap between segments

        // Bottom Segment (Solid)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(bottomFraction)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}



// --------------------------------LEGACY------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenLegacy(
    viewModel: DashboardViewModel,
    onNavigateToDetail: (String) -> Unit
) {
    val healthOverview by viewModel.healthOverview.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val readinessScore by viewModel.readinessScore.collectAsState()
    val insights by viewModel.healthInsights.collectAsState()

    Scaffold(
        topBar = {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TopAppBar(
                    title = {
                        Text(
                            text = "سلام ${userName ?: "کاربر"}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { /* TODO: Notifications */ }) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = TextGray
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* TODO: Help */ }) {
                            Icon(
                                painter = painterResource(R.drawable.information),
                                contentDescription = "Help",
                                tint = TextGray
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                )
            }
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            NewHealthStatusCard(
                score = readinessScore, // Mock score
                insights = insights,
            ) { onNavigateToDetail("") }
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(Modifier.weight(1f)) {
                        HeartRateCard(
                            heartRate = healthOverview.heartRate,
                            onClick = {
                                onNavigateToDetail(HealthDetailRoutes.HeartRateDetail.route)
                            }
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        StepsCard(
                            steps = healthOverview.steps,
                            onClick = { onNavigateToDetail(HealthDetailRoutes.StepsDetail.route) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(Modifier.weight(1f)) {
                        BloodPressureCard(
                            healthOverview.sleepDurationHours,
                            onClick = { onNavigateToDetail(HealthDetailRoutes.SleepDetail.route) }
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        SpO2Card(
                            spo2 = healthOverview.bloodOxygen,
                            onClick = { onNavigateToDetail(HealthDetailRoutes.SpO2Detail.route) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp)) // Extra space at bottom
        }
    }
}

// =========================== NEW MAIN CARD ===========================

@Composable
fun NewHealthStatusCardLegacy(
    score: Int,
    insights: List<String>,
    viewModel: DashboardViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = TealPrimary.copy(alpha = 0.3f),
                spotColor = TealPrimary.copy(alpha = 0.3f)
            )
            .border(1.dp, TealPrimary.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "وضعیت کلی سلامت",
                style = MaterialTheme.typography.titleMedium,
                color = TextDark,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .border(4.dp, TealPrimary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = score.toString().toFarsiDigits(),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        )
                        Text(
                            text = "/۱۰۰",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))


                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    insights.forEach { insight ->
                        InsightItem(text = insight)
                    }
                }

            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { viewModel.syncDeviceHistory() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary)
            ) {
                Text(
                    text = "تحلیل هوش مصنوعی",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun InsightItemLegacy(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TextGray,
            textAlign = TextAlign.End
        )
        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = TextGray.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
        )

    }
}

// =========================== METRIC CARDS BASE ===========================

@Composable
fun BaseMetricCard(
    title: String,
    value: String,
    unit: String?,
    statusText: String?,
    iconPainter: Painter,
    iconTint: Color,
    onClick: () -> Unit = {},
    chartContent: @Composable BoxScope.() -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth() // Fills the weight in the row
            .height(210.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- HEADER ---
            // In RTL: Start is Right, End is Left.
            // We want Text on Right (Start), Icon on Left (End).
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Icon(
                    painter = iconPainter,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark,
                    fontWeight = FontWeight.Bold
                )

            }

            if (statusText != null) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    textAlign = TextAlign.End, // Right in RTL
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // --- CHART AREA ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                chartContent()
            }

            // --- FOOTER ---
            // We want Button on Right (Start), Value on Left (End)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = TextDark
                    )

                    if (unit != null) {
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(TealPrimary.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Details",
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(-45f) // Rotate to point diagonal up
                    )
                }
            }
        }
    }
}


@Composable
fun HeartRateCard(heartRate: Int, onClick: () -> Unit) {
    BaseMetricCard(
        title = "ضربان قلب",
        value = heartRate.toString().toFarsiDigits(),
        unit = "bpm",
        statusText = "عادی",
        iconPainter = painterResource(R.drawable.heart_rate),
        iconTint = RedAccent,
        onClick = onClick
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp)
        ) {
            val path = Path()
            val width = size.width
            val height = size.height
            path.moveTo(0f, height * 0.7f)
            path.lineTo(width * 0.2f, height * 0.6f)
            path.lineTo(width * 0.4f, height * 0.8f)
            path.lineTo(width * 0.5f, height * 0.2f)
            path.lineTo(width * 0.6f, height * 0.7f)
            path.lineTo(width * 0.8f, height * 0.5f)
            path.lineTo(width, height * 0.65f)

            drawPath(
                path = path,
                color = RedAccent,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
fun StepsCard(steps: Int, onClick: () -> Unit) {
    BaseMetricCard(
        title = "تعداد قدم",
        value = steps.toString().toFarsiDigits(),
        unit = "steps",
        statusText = "بیشتر کن",
        iconPainter = painterResource(R.drawable.walk),
        iconTint = OrangeAccent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            val heights = listOf(0.4f, 0.6f, 0.3f, 0.8f, 0.5f, 0.7f, 0.4f)
            heights.forEach { relativeHeight ->
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight(relativeHeight)
                        .clip(RoundedCornerShape(4.dp))
                        .background(OrangeAccent.copy(alpha = 0.7f))
                )
            }
        }
    }
}

@Composable
fun BloodPressureCard(sleep: Float, onClick: () -> Unit) {
//    val systolic = 120
    BaseMetricCard(
        title = "پایش خواب",
        value = "$sleep".toFarsiDigits(),
        unit = "mmHg",
        statusText = "عادی",
        iconPainter = painterResource(R.drawable.chemistry_flask),
        iconTint = TealPrimary,
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, TealPrimary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(TealPrimary)
                )
            }
        }
    }
}

@Composable
fun SpO2Card(spo2: Int, onClick: () -> Unit) {
    val displayValue = if (spo2 > 0) spo2.toString() else "--"
    BaseMetricCard(
        title = "اکسیژن خون",
        value = displayValue.toFarsiDigits(),
        unit = "%",
        statusText = "عادی",
        iconPainter = painterResource(R.drawable.hospital),
        iconTint = BlueAccent,
        onClick = onClick
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp)
        ) {
            val path = Path()
            val w = size.width
            val h = size.height
            path.moveTo(0f, h * 0.5f)
            path.lineTo(w * 0.2f, h * 0.5f)
            path.lineTo(w * 0.2f, h * 0.3f)
            path.lineTo(w * 0.5f, h * 0.3f)
            path.lineTo(w * 0.5f, h * 0.6f)
            path.lineTo(w * 0.8f, h * 0.6f)
            path.lineTo(w * 0.8f, h * 0.4f)
            path.lineTo(w, h * 0.4f)

            drawPath(
                path = path,
                color = BlueAccent,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Square)
            )
        }
    }
}