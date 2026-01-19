package com.bonyad.healthplat.ui.dashboard.details.heart_rate


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.R
import com.bonyad.healthplat.ui.utils.toFarsiDigits
import saman.zamani.persiandate.PersianDate
import kotlin.math.min


@Composable
fun HeartRateDetailScreen(
    viewModel: HeartRateDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onInfoClick: () -> Unit = {}
) {
    val chartData by viewModel.chartData.collectAsState()
    val currentHrv by viewModel.currentHrv.collectAsState()
    val selectedRange by viewModel.selectedTimeRange.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val maxRate by viewModel.maxHeartRate.collectAsState()
    val minRate by viewModel.minHeartRate.collectAsState()
    val avgRate by viewModel.avgHeartRate.collectAsState()

    val currentPersianDate by viewModel.currentPersianDate.collectAsState()
    val currentHeartRate by viewModel.currentHeartRate.collectAsState()

    val hrvChartData by viewModel.hrvChartData.collectAsState()


    Scaffold(
        topBar = {
            CustomDetailTopBar(
                title = "ضربان قلب",
                onBack = onBack,
                onSync = { viewModel.refreshData() },
                onInfo = { onInfoClick() }
            )
        },
        containerColor = Color(0xFFF9F9F9)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Segment Control (Daily/Weekly/Monthly)
            TimeRangeSelector(
                selected = selectedRange,
                onSelect = { viewModel.setTimeRange(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Date Strip (The horizontal list of days)
            val selectedOffset by viewModel.selectedDayOffset.collectAsState()

            DateStrip(
                selectedOffset = selectedOffset,
                onDaySelected = { offset ->
                    viewModel.selectDay(offset)
                }
            )
            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "ضربان قلب شما",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "bpm",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = currentHeartRate.toString().toFarsiDigits(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Current Date display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // در حال ورزش - COMMENTED OUT
                    // Row(verticalAlignment = Alignment.CenterVertically) {
                    //     Icon(
                    //         painter = painterResource(R.drawable.ic_running),
                    //         contentDescription = null,
                    //         tint = Color.Gray,
                    //         modifier = Modifier.size(16.dp)
                    //     )
                    //     Spacer(modifier = Modifier.width(4.dp))
                    //     Text(
                    //         text = "در حال ورزش",
                    //         style = MaterialTheme.typography.bodySmall,
                    //         color = Color.Gray
                    //     )
                    // }
                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = currentPersianDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Loading or Chart
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFE53935))
                    }
                } else {
                    // The Bar Chart - FIXED VERSION
                    HeartRateRangeChart(data = chartData)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            StatsRow(
                max = maxRate,
                avg = avgRate,
                min = minRate
            )

            Spacer(modifier = Modifier.height(32.dp))

            HrvSection(currentHrv = currentHrv, hrvChartData = hrvChartData)

            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartRateDetailScreenLegacy(
    viewModel: HeartRateDetailViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val currentHeartRate by viewModel.currentHeartRate.collectAsState()
    val heartRateData by viewModel.heartRateData.collectAsState()
    val avgHeartRate by viewModel.avgHeartRate.collectAsState()
    val minHeartRate by viewModel.minHeartRate.collectAsState()
    val maxHeartRate by viewModel.maxHeartRate.collectAsState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "ضربان قلب",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.back_arrow),
                                contentDescription = "بازگشت"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                )
            },
            containerColor = Color(0xFFF5F5F5)
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Current Heart Rate Card
                CurrentHeartRateCard(currentHeartRate)

                // Weekly Chart Card
                HeartRateChartCard(heartRateData)

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "پایین ترین",
                        value = minHeartRate.toString().toFarsiDigits(),
                        unit = "bpm",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "میانگین",
                        value = avgHeartRate.toString().toFarsiDigits(),
                        unit = "bpm",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "بالا ترین",
                        value = maxHeartRate.toString().toFarsiDigits(),
                        unit = "bpm",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Recommendations Card
                RecommendationsCard(
                    title = "توضیحات",
                    items = listOf(
                        "ضربان قلب شما در حال ورزش می تواند تا 140 BPM افزایش یابد" to R.drawable.information,
                        "ضربان قلب طبیعی در حال ورزش 120-140 BPM است" to R.drawable.information
                    )
                )
            }
        }
    }
}

@Composable
fun CurrentHeartRateCard(heartRate: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "آخرین اندازه گیری",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                heartRate.toString().toFarsiDigits(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 64.sp
                ),
                color = Color(0xFFE53935)
            )
            Text(
                "bpm",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF999999)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "عادی",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
fun HeartRateChartCard(data: List<Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "نمودار هفتگی",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    "امروز: ۲۲ مهر",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF999999)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (data.isEmpty()) return@Canvas

                val path = Path()
                val width = size.width
                val height = size.height
                val maxValue = data.maxOrNull() ?: 100
                val minValue = data.minOrNull() ?: 60
                val range = maxValue - minValue

                data.forEachIndexed { index, value ->
                    val x = (index.toFloat() / (data.size - 1)) * width
                    val y =
                        height - ((value - minValue).toFloat() / range) * height * 0.8f - height * 0.1f

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = Color(0xFFE53935),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach { day ->
                    Text(
                        day,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF999999)
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF999999)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF2C2C2C)
                )
                Text(
                    unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF999999),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun RecommendationsCard(
    title: String,
    items: List<Pair<String, Int>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            items.forEach { (text, icon) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = Color(0xFF5BA3A3),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomDetailTopBar(
    title: String,
    onBack: () -> Unit,
    onSync: () -> Unit,
    onInfo: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Title - Absolutely centered on screen
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.Black,
            modifier = Modifier.align(Alignment.Center)
        )

        // Left icons
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onInfo,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    painterResource(R.drawable.info_circle),
                    contentDescription = "Info",
                    tint = Color.Gray
                )
            }

            IconButton(
                onClick = onSync,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    painterResource(R.drawable.refresh_square),
                    contentDescription = "Sync",
                    tint = Color.Gray
                )
            }
        }

        // Right icon
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.CenterEnd)
        ) {
            Icon(
                painterResource(R.drawable.close_square),
                contentDescription = "Close",
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun TimeRangeSelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    val options = listOf("ماهانه", "هفتگی", "روزانه")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(48.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            options.forEach { option ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .background(
                            if (isSelected) Color(0xFF4FA8A6) else Color.Transparent, // Teal color
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelect(option) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        color = if (isSelected) Color.White else Color.Gray,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun DateStrip(
    selectedOffset: Int,  // 0 = today, -1 = yesterday, -2 = day before, etc.
    onDaySelected: (Int) -> Unit
) {
    val today = PersianDate()

    // Persian weekday letters: Saturday=ش, Sunday=ی, Monday=د, etc.
    val weekDayNames = mapOf(
        0 to "ش",  // Saturday
        1 to "ی",  // Sunday
        2 to "د",  // Monday
        3 to "س",  // Tuesday
        4 to "چ",  // Wednesday
        5 to "پ",  // Thursday
        6 to "ج"   // Friday
    )

    // Generate last 7 days: today (offset=0) to 6 days ago (offset=-6)
    // But display order: oldest first (left) to newest/today (right)
    val days = (-6..0).map { offset ->
        val date = PersianDate(today.time).apply {
            if (offset < 0) subDays(-offset)
        }
        Pair(offset, date)
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        reverseLayout = true  // RTL: today on right
    ) {
        items(days) { (offset, date) ->
            val isSelected = offset == selectedOffset
            val dayOfWeek = date.dayOfWeek()  // 0=Saturday, 1=Sunday, etc.

            Column(
                modifier = Modifier
                    .width(50.dp)
                    .height(70.dp)
                    .background(
                        if (isSelected) Color(0xFF4FA8A6) else Color.White,
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        1.dp,
                        if (isSelected) Color.Transparent else Color(0xFFEEEEEE),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        onDaySelected(offset)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = date.shDay.toString().toFarsiDigits(),
                    color = if (isSelected) Color.White else Color.Gray,
                    fontSize = 18.sp
                )
                Text(
                    text = weekDayNames[dayOfWeek] ?: "",
                    color = if (isSelected) Color.White else Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * FIXED Heart Rate Range Chart
 * - Bars stay within canvas bounds
 * - Thicker bars (10dp instead of 6dp)
 * - Proper clipping
 */
@Composable
fun HeartRateRangeChart(data: List<HeartRateRangePoint>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(top = 16.dp, bottom = 24.dp)
        ) {
            // Filter valid data (exclude 0, 1 values)
            val validData = data.filter { it.min > 1 && it.max > 1 }

            Row(modifier = Modifier.fillMaxSize()) {
                // Y-Axis Labels
                Column(
                    modifier = Modifier
                        .width(32.dp)
                        .fillMaxHeight()
                        .padding(end = 4.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("۱۵۰", fontSize = 10.sp, color = Color.Gray)
                    Text("۱۰۰", fontSize = 10.sp, color = Color.Gray)
                    Text("۵۰", fontSize = 10.sp, color = Color.Gray)
                }

                // Chart Area
                Box(modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp, bottom = 16.dp, end = 8.dp)
                    ) {
                        val width = size.width
                        val height = size.height

                        val yMin = 50f
                        val yMax = 150f
                        val yRange = yMax - yMin

                        // Draw horizontal grid lines
                        val gridLines = listOf(50f, 100f, 150f)
                        gridLines.forEach { value ->
                            val y = height - ((value - yMin) / yRange) * height
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.5f),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            )
                        }

                        // Draw vertical grid lines (every 6 hours)
                        val verticalLines = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
                        verticalLines.forEach { fraction ->
                            val x = width * fraction
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                start = Offset(x, 0f),
                                end = Offset(x, height),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            )
                        }

                        if (validData.isEmpty()) return@Canvas

                        val barWidth = 3.dp.toPx()

                        // Map data points to their hour positions (0-23)
                        validData.forEach { point ->
                            // Extract hour from timeLabel (e.g., "08:00" -> 8)
                            val hour = point.timeLabel.split(":").firstOrNull()?.toIntOrNull() ?: 0

                            // X position based on hour (00:00 to 23:59)
                            val x = (hour / 24f) * width

                            // Clamp values to visible range
                            val clampedMin = point.min.coerceIn(yMin.toInt(), yMax.toInt())
                            val clampedMax = point.max.coerceIn(yMin.toInt(), yMax.toInt())

                            val topY = height - ((clampedMax - yMin) / yRange) * height
                            val bottomY = height - ((clampedMin - yMin) / yRange) * height
                            val barHeight = (bottomY - topY).coerceAtLeast(4.dp.toPx())

                            // Draw the vertical bar
                            drawRoundRect(
                                color = Color(0xFFFF8A80),
                                topLeft = Offset(x - barWidth / 2, topY),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                            )

                            // Draw dots
                            drawCircle(
                                color = Color(0xFFE53935),
                                radius = 3.dp.toPx(),
                                center = Offset(x, topY)
                            )
                            drawCircle(
                                color = Color(0xFFE53935),
                                radius = 3.dp.toPx(),
                                center = Offset(x, bottomY)
                            )
                        }
                    }

                    // Time Labels at bottom - LTR for correct ordering
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(end = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("۰۰:۰۰", "۰۷:۵۹", "۱۵:۵۹", "۲۳:۵۹").forEach { label ->
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsRow(min: Int, avg: Int, max: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatBox("بالا ترین", max, Modifier.weight(1f))
        StatBox("میانگین", avg, Modifier.weight(1f))
        StatBox("پایین ترین", min, Modifier.weight(1f))
    }
}

@Composable
fun StatBox(title: String, value: Int, modifier: Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The box with value only
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFF4CAF50)), // Green border
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        value.toString().toFarsiDigits(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "bpm",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Label below the box
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun HrvSection(
    currentHrv: Int,
    hrvChartData: List<Int> = emptyList()
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Title row - HRV on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text("HRV", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }

        // Value and status row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator on left
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("عادی", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF4CAF50), CircleShape)
                )
            }

            // Value on right (below HRV title)
            Text(
                text = "ms ${currentHrv.toString().toFarsiDigits()}",
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // HRV Area Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(16.dp)
            ) {
                HrvAreaChart(data = hrvChartData)
            }
        }
    }
}

@Composable
fun HrvAreaChart(data: List<Int>) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Y-axis labels
        Column(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight()
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("۱۰۰", fontSize = 9.sp, color = Color.Gray)
            Text("۵۰", fontSize = 9.sp, color = Color.Gray)
            Text("۰", fontSize = 9.sp, color = Color.Gray)
        }

        // Chart
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 32.dp, bottom = 20.dp)
        ) {
            val width = size.width
            val height = size.height

            // Draw vertical grid lines
            val gridCount = 4
            for (i in 0..gridCount) {
                val x = (width / gridCount) * i
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
            }

            if (data.isEmpty()) return@Canvas

            val validData = data.filter { it > 0 }
            if (validData.isEmpty()) return@Canvas

            val yMax = 100f
            val yMin = 0f
            val yRange = yMax - yMin

            val path = Path()
            val fillPath = Path()

            // Find first valid point
            val firstValue = validData.first().coerceIn(yMin.toInt(), yMax.toInt())
            val firstY = height - ((firstValue - yMin) / yRange) * height

            path.moveTo(0f, firstY)
            fillPath.moveTo(0f, height)
            fillPath.lineTo(0f, firstY)

            val pointSpacing = width / (validData.size - 1).coerceAtLeast(1)

            validData.forEachIndexed { index, value ->
                val clampedValue = value.coerceIn(yMin.toInt(), yMax.toInt())
                val x = index * pointSpacing
                val y = height - ((clampedValue - yMin) / yRange) * height

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }

            // Complete fill path
            fillPath.lineTo(width, height)
            fillPath.close()

            // Draw gradient fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE53935).copy(alpha = 0.6f),
                        Color(0xFFE53935).copy(alpha = 0.1f)
                    )
                )
            )

            // Draw line
            drawPath(
                path = path,
                color = Color(0xFFE53935),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // X-axis labels
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("۰", "۱۰", "۲۰", "۳۰", "۴۰").forEach { label ->
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}