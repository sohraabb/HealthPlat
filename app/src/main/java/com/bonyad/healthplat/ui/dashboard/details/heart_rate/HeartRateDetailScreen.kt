package com.bonyad.healthplat.ui.dashboard.details.heart_rate


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.R
import com.bonyad.healthplat.ui.utils.toFarsiDigits
import saman.zamani.persiandate.PersianDate


@Composable
fun HeartRateDetailScreen(
    viewModel: HeartRateDetailViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val chartData by viewModel.chartData.collectAsState()
    val currentHrv by viewModel.currentHrv.collectAsState()
    val selectedRange by viewModel.selectedTimeRange.collectAsState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

        Scaffold(
            topBar = {
                CustomDetailTopBar(
                    title = "ضربان قلب",
                    onBack = onBack,
                    onSync = { /* TODO: Sync logic */ },
                    onInfo = { /* TODO: Info logic */ }
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
                    Text(
                        text = "در حال ورزش",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.End) // "Today 22 Mehr" logic
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // The Bar Chart
                    HeartRateRangeChart(data = chartData)
                }

                Spacer(modifier = Modifier.height(32.dp))

                StatsRow(
                    max = 140,
                    avg = 85,
                    min = 70
                )

                Spacer(modifier = Modifier.height(32.dp))

                HrvSection(currentHrv = currentHrv)

                Spacer(modifier = Modifier.height(50.dp))
            }
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
//            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        // Right Side (Start in RTL) -> The Close Button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(24.dp)
        ) {
            Icon(
                painterResource(R.drawable.close_square),
                contentDescription = "Close",
                tint = Color.Gray
            )
        }

        // Center Title
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.Black
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onInfo,
                modifier = Modifier
                    .size(24.dp)
            ) {
                Icon(
                    painterResource(R.drawable.info_circle),
                    contentDescription = "Info",
                    tint = Color.Gray
                )
            }


            IconButton(
                onClick = onSync,
                modifier = Modifier
                    .size(24.dp)
            ) {
                Icon(
                    painterResource(R.drawable.refresh_square),
                    contentDescription = "Sync",
                    tint = Color.Gray
                )
            }

        }




    }
}

@Composable
fun TimeRangeSelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    val options = listOf("روزانه", "هفتگی", "ماهانه")

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
    selectedOffset: Int,
    onDaySelected: (Int) -> Unit
) {
    val today = PersianDate()

    val weekDayLetters = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    val diffToSaturday = today.dayOfWeek()

    val startOfWeek = PersianDate(today.time)
    if (diffToSaturday > 0) {
        startOfWeek.subDays(diffToSaturday)
    }

    val weekDates = (0..6).map { offset ->
        PersianDate(startOfWeek.time).apply {
            if (offset > 0) addDays(offset)
        }
    }

    val todayIndex = weekDates.indexOfFirst { d ->
        d.shYear == today.shYear &&
                d.shMonth == today.shMonth &&
                d.shDay == today.shDay
    }.coerceAtLeast(0)

    val selectedIndex = todayIndex + selectedOffset

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(weekDates) { index, date ->

            val isSelected = index == selectedIndex

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
                        onDaySelected(index - todayIndex)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = date.shDay.toString(),
                    color = if (isSelected) Color.White else Color.Gray,
                    fontSize = 18.sp
                )
                Text(
                    text = weekDayLetters[index],
                    color = if (isSelected) Color.White else Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun HeartRateRangeChart(data: List<HeartRateRangePoint>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Color.White) // Background grid container
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp, bottom = 30.dp, start = 16.dp, end = 16.dp)
        ) {
            if (data.isEmpty()) return@Canvas

            val width = size.width
            val height = size.height

            // Hardcoded Y-axis range (50 to 150 bpm) for stability
            val yMin = 50f
            val yMax = 150f
            val yRange = yMax - yMin

            // Draw Grid Lines (Horizontal)
            val gridLines = 3
            for (i in 0..gridLines) {
                val y = height * (i.toFloat() / gridLines)
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            // Draw Bars
            val barWidth = 12.dp.toPx()
            val spacePerItem = width / data.size

            data.forEachIndexed { index, point ->
                val x = (index * spacePerItem) + (spacePerItem / 2)

                // Calculate height of bar based on min/max
                val topY = height - ((point.max - yMin) / yRange) * height
                val bottomY = height - ((point.min - yMin) / yRange) * height

                // Draw the vertical capsule (Range)
                drawRoundRect(
                    color = if (point.isAlert) Color(0xFFFF6B6B) else Color(0xFFFF8A80),
                    topLeft = Offset(x - barWidth / 2, topY),
                    size = Size(barWidth, bottomY - topY),
                    cornerRadius = CornerRadius(10f, 10f)
                )

                // Optional: Draw dots at top/bottom like screenshot
                drawCircle(
                    color = Color(0xFFE53935),
                    radius = 4.dp.toPx(),
                    center = Offset(x, topY)
                )
                drawCircle(
                    color = Color(0xFFE53935),
                    radius = 4.dp.toPx(),
                    center = Offset(x, bottomY)
                )
            }
        }

        // Time Labels at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach {
                Text(
                    text = it.timeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
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
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()      // ⭐ IMPORTANT: allow vertical centering
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center  // ⭐ CENTER VERTICALLY
        ) {
            Text(
                "$value bpm",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun HrvSection(currentHrv: Int) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("HRV", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("$currentHrv ms", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
        }

        Text("عادی", color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(16.dp))

        // Simple Gradient Placeholder for HRV Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Color.White)
                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
        ) {
            // You can implement a similar Path drawing logic here as the Heart Rate one,
            // but fill the path with a Brush.verticalGradient
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val path = Path()
                path.moveTo(0f, size.height)
                path.lineTo(size.width * 0.3f, size.height * 0.8f)
                path.lineTo(size.width * 0.7f, size.height * 0.85f)
                path.lineTo(size.width, size.height * 0.9f)
                path.lineTo(size.width, size.height)
                path.close()

                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFF8A80), Color(0xFFD32F2F))
                    )
                )
            }
        }
    }
}