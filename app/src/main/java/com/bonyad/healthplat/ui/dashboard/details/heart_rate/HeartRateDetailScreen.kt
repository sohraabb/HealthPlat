package com.bonyad.healthplat.ui.dashboard.details.heart_rate


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.R
import com.bonyad.healthplat.ui.dashboard.details.CustomDetailTopBar
import com.bonyad.healthplat.ui.utils.toFarsiDigits
import androidx.compose.ui.tooling.preview.Preview
import saman.zamani.persiandate.PersianDate
import kotlin.math.abs


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
    val selectedDate by viewModel.selectedDate.collectAsState()
    val isCaregiverMode = viewModel.isCaregiverMode

    var showDatePicker by remember { mutableStateOf(false) }

    val todayPersian = remember {
        val (jy, jm, jd) = com.bonyad.healthplat.ui.utils.PersianDateUtils.getCurrentJalaliDate()
        com.bonyad.healthplat.ui.components.PersianDate(jy, jm, jd)
    }
    val selectedPersianDate = remember(selectedDate) {
        val (jy, jm, jd) = com.bonyad.healthplat.ui.utils.PersianDateUtils.georgianToJalali(
            selectedDate.year, selectedDate.monthValue, selectedDate.dayOfMonth
        )
        com.bonyad.healthplat.ui.components.PersianDate(jy, jm, jd)
    }

    if (showDatePicker) {
        com.bonyad.healthplat.ui.components.PersianDatePickerDialog(
            selectedDate = selectedPersianDate,
            onDateSelected = { date ->
                showDatePicker = false
                viewModel.selectDate(date)
            },
            onDismiss = { showDatePicker = false },
            maxDate = todayPersian
        )
    }

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
                onSelect = { viewModel.setTimeRange(it) },
                onCalendarClick = { showDatePicker = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Date Strip (only for daily view)
            if (selectedRange == "روزانه") {
                DateStrip(
                    selectedDate = selectedDate,
                    onDaySelected = { date -> viewModel.selectDay(date) }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = when (selectedRange) {
                            "هفتگی", "ماهانه" -> "میانگین ضربان قلب شما"
                            else -> "ضربان قلب شما"
                        },
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
                        text = currentHeartRate.toString().toFarsiDigits(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 20.sp,
                            color = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "bpm",
                        fontSize = 14.sp,
                        color = Color(0xFF6B6B6B)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Current Date display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    HeartRateRangeChart(
                        data = chartData,
                        selectedRange = selectedRange
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            StatsRow(
                max = maxRate,
                avg = avgRate,
                min = minRate
            )

            if (!isCaregiverMode) {
                Spacer(modifier = Modifier.height(32.dp))

                HrvSection(currentHrv = currentHrv, hrvChartData = hrvChartData, selectedRange = selectedRange)
            }

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
fun TimeRangeSelector(
    selected: String,
    onSelect: (String) -> Unit,
    onCalendarClick: () -> Unit = {}
) {
    val options = listOf("ماهانه", "هفتگی", "روزانه")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
                .clickable { onCalendarClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.calendar),
                contentDescription = "انتخاب تاریخ",
                tint = Color(0xFF4FA8A6),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
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
                                if (isSelected) Color(0xFF4FA8A6) else Color.Transparent,
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
}

@Composable
fun DateStrip(
    selectedDate: java.time.LocalDate,
    onDaySelected: (java.time.LocalDate) -> Unit
) {
    val today = java.time.LocalDate.now()

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

    // Generate last 7 days oldest-first, reversed so today appears on the right (RTL)
    val days = (-6..0).map { offset -> today.plusDays(offset.toLong()) }.reversed()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { date ->
            val isSelected = date == selectedDate
            val cal = java.util.GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth)
            val pDate = PersianDate(cal.time)
            val dayOfWeek = pDate.dayOfWeek()

            Column(
                modifier = Modifier
                    .weight(1f)
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
                    .clickable { onDaySelected(date) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = pDate.shDay.toString().toFarsiDigits(),
                    color = if (isSelected) Color.White else Color.Gray,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
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
fun HeartRateRangeChart(
    data: List<HeartRateRangePoint>,
    showEmptyState: Boolean = data.isEmpty() || data.all { it.min <= 1 && it.max <= 1 },
    selectedRange: String = "روزانه",
    initialSelectedIndex: Int = -1,
    onBarSelected: (HeartRateRangePoint?) -> Unit = {}
) {
    // Track selected bar index and X position for tooltip
    var selectedBarIndex by remember(selectedRange) { mutableIntStateOf(-1) }
    var selectedBarX by remember(selectedRange) { mutableFloatStateOf(0f) }

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
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Y-Axis Labels (Left side)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 8.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("۲۲۰", fontSize = 12.sp, color = Color.Gray)
                    Text("۱۲۵", fontSize = 12.sp, color = Color.Gray)
                    Text("۳۰", fontSize = 12.sp, color = Color.Gray)
                }

                // Chart Content
                Box(modifier = Modifier.weight(1f)) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 20.dp)
                            .pointerInput(data, selectedRange) {
                                detectTapGestures { tapOffset ->
                                    val width = size.width.toFloat()
                                    val barWidth = when (selectedRange) {
                                        "هفتگی" -> 12.dp.toPx()
                                        "ماهانه" -> 14.dp.toPx()
                                        else -> 3.dp.toPx()
                                    }
                                    // Tap hit zone radius (generous for easy tapping)
                                    val hitRadius =
                                        if (selectedRange == "روزانه") 12.dp.toPx() else 16.dp.toPx()

                                    var closestIndex = -1
                                    var closestDist = Float.MAX_VALUE
                                    var closestX = 0f

                                    data.forEachIndexed { index, point ->
                                        if (point.min <= 1 && point.max <= 1) return@forEachIndexed

                                        val x = when (selectedRange) {
                                            "روزانه" -> {
                                                val parts = point.timeLabel.split(":")
                                                val hour =
                                                    parts.getOrNull(0)?.toIntOrNull() ?: 0
                                                val minute =
                                                    parts.getOrNull(1)?.toIntOrNull() ?: 0
                                                ((hour * 60 + minute) / (24f * 60f)) * width
                                            }

                                            else -> {
                                                val pad = barWidth / 2 + 4.dp.toPx()
                                                val usable = width - pad * 2
                                                if (data.size > 1) {
                                                    pad + (index.toFloat() / (data.size - 1)) * usable
                                                } else {
                                                    width / 2f
                                                }
                                            }
                                        }

                                        val dist = abs(tapOffset.x - x)
                                        if (dist < hitRadius && dist < closestDist) {
                                            closestDist = dist
                                            closestIndex = index
                                            closestX = x
                                        }
                                    }


                                    if (closestIndex == selectedBarIndex) {
                                        // Tap same bar again → deselect
                                        selectedBarIndex = -1
                                        onBarSelected(null)
                                    } else {
                                        selectedBarIndex = closestIndex
                                        selectedBarX = closestX
                                        if (closestIndex >= 0) {
                                            onBarSelected(data[closestIndex])
                                        } else {
                                            onBarSelected(null)
                                        }
                                    }
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height

                        // Fixed Range for stability (matching the 50-150 labels)
                        val yMin = 30f
                        val yMax = 220f
                        val yRange = yMax - yMin

                        // 1. Draw Grid Lines (Dashed)
                        val gridLines = listOf(0f, 0.5f, 1f)
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                        gridLines.forEach { fraction ->
                            val y = height * fraction
                            drawLine(
                                color = Color(0xFFEEEEEE),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = dashEffect
                            )
                        }

                        // Vertical Grid Lines (every 6h = 5 main lines)
                        val verticalDivisions = 4
                        for (i in 0..verticalDivisions) {
                            val x = (width / verticalDivisions) * i
                            drawLine(
                                color = Color(0xFFEEEEEE),
                                start = Offset(x, 0f),
                                end = Offset(x, height),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = dashEffect
                            )
                        }


                        if (data.isEmpty()) return@Canvas

                        // 2. Draw Bars — thinner for daily (48 half-hour slots)
                        val barWidth = when (selectedRange) {
                            "هفتگی" -> 12.dp.toPx()
                            "ماهانه" -> 14.dp.toPx()
                            else -> 3.dp.toPx()
                        }
                        val cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                        val barColor = Color(0xFFFF6B6B)
                        val selectedBarColor = Color(0xFFE53935)

                        data.forEachIndexed { index, point ->
                            // Skip drawing for entries with no data
                            if (point.min <= 1 && point.max <= 1) return@forEachIndexed

                            val isSelected = index == selectedBarIndex

                            // Calculate X position based on time range
                            val x = when (selectedRange) {
                                "روزانه" -> {
                                    val parts = point.timeLabel.split(":")
                                    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
                                    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                    ((hour * 60 + minute) / (24f * 60f)) * width
                                }
                                else -> {
                                    // Weekly/Monthly: position by index in full list
                                    val padding = barWidth / 2 + 4.dp.toPx()
                                    val usable = width - padding * 2
                                    if (data.size > 1) {
                                        padding + (index.toFloat() / (data.size - 1)) * usable
                                    } else {
                                        width / 2f
                                    }
                                }
                            }

                            // Calculate Y positions (clamped to visual range)
                            val safeMin = point.min.toFloat().coerceIn(yMin, yMax)
                            val safeMax = point.max.toFloat().coerceIn(yMin, yMax)

                            val yTop = height - ((safeMax - yMin) / yRange) * height
                            val yBottom = height - ((safeMin - yMin) / yRange) * height
                            val calculatedHeight = yBottom - yTop

                            val currentBarColor = if (isSelected) selectedBarColor else barColor
                            val currentBarWidth = if (isSelected) barWidth * 1.5f else barWidth

                            if (calculatedHeight < barWidth) {
                                // Draw as a Dot if range is very small
                                val dotRadius = if (selectedRange == "روزانه") 2.dp.toPx() else barWidth / 2
                                val currentDotRadius = if (isSelected) dotRadius * 1.5f else dotRadius
                                drawCircle(
                                    color = currentBarColor,
                                    radius = currentDotRadius,
                                    center = Offset(x, yTop + calculatedHeight / 2)
                                )
                            } else {
                                // Draw as a Capsule
                                drawRoundRect(
                                    color = currentBarColor,
                                    topLeft = Offset(x - currentBarWidth / 2, yTop),
                                    size = Size(currentBarWidth, calculatedHeight),
                                    cornerRadius = CornerRadius(currentBarWidth / 2, currentBarWidth / 2)
                                )
                            }
                        }
                    }

                    // X-Axis Labels (Bottom)
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp)
                                .align(Alignment.BottomCenter),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            when (selectedRange) {
                                "هفتگی" -> {
                                    if (data.isNotEmpty()) {
                                        data.forEach { point ->
                                            Text(point.timeLabel, fontSize = 12.sp, color = Color.Gray)
                                        }
                                    } else {
                                        // Fallback: show day labels for last 7 days
                                        val today = java.time.LocalDate.now()
                                        (0..6).forEach { i ->
                                            val date = today.minusDays((6 - i).toLong())
                                            val label = when (date.dayOfWeek.value) {
                                                1 -> "د"; 2 -> "س"; 3 -> "چ"; 4 -> "پ"
                                                5 -> "ج"; 6 -> "ش"; 7 -> "ی"; else -> ""
                                            }
                                            Text(label, fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                }
                                "ماهانه" -> {
                                    if (data.isNotEmpty()) {
                                        data.forEach { point ->
                                            Text(point.timeLabel, fontSize = 12.sp, color = Color.Gray)
                                        }
                                    } else {
                                        val (_, _, days) = com.bonyad.healthplat.ui.utils.PersianDateUtils.getCurrentPersianMonthRange()
                                        val numWeeks = (days + 6) / 7
                                        (1..numWeeks).forEach { Text("هفته $it", fontSize = 12.sp, color = Color.Gray) }
                                    }
                                }
                                else -> {
                                    listOf("00:00", "06:00", "12:00", "18:00", "24:00").forEach { label ->
                                        Text(label, fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }

                    // Tooltip overlay
                    if (selectedBarIndex >= 0 && selectedBarIndex < data.size) {
                        val point = data[selectedBarIndex]
                        val timeRangeText = formatTimeRange(point.timeLabel, selectedRange)
                        val density = LocalDensity.current
                        var tooltipWidthPx by remember { mutableIntStateOf(0) }
                        var parentWidthPx by remember { mutableIntStateOf(0) }

                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .fillMaxWidth()
                                    .onSizeChanged { parentWidthPx = it.width }
                            ) {
                                val offsetXDp = with(density) {
                                    val halfTooltip = tooltipWidthPx / 2f
                                    val maxOffset =
                                        (parentWidthPx - tooltipWidthPx).coerceAtLeast(0).toFloat()
                                    (selectedBarX - halfTooltip).coerceIn(0f, maxOffset).toDp()
                                }

                                Box(
                                    modifier = Modifier
                                        .offset(x = offsetXDp)
                                        .onSizeChanged { tooltipWidthPx = it.width }
                                        .border(
                                            width = 0.5.dp,
                                            color = Color(0xFFEFEFEF),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .background(
                                            color = Color(0xFFEDEDED).copy(alpha = 0.85f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${point.min.toString().toFarsiDigits()} - ${point.max.toString().toFarsiDigits()}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                "bpm",
                                                fontSize = 10.sp,
                                                color = Color(0xFF666666)
                                            )
                                        }
                                        Text(
                                            text = timeRangeText,
                                            fontSize = 10.sp,
                                            color = Color(0xFF666666)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Empty State
            if (showEmptyState) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("داده‌ای موجود نیست", color = Color(0xFF6B6B6B)
                    )
                }
            }
        }
    }
}

/**
 * Formats the time range label for the tooltip based on the selected range mode.
 * Daily: "05:00" → "۵ تا ۵:۳۰" / with AM/PM in Farsi
 * Weekly: day label as-is
 * Monthly: week label as-is
 */
private fun formatTimeRange(timeLabel: String, selectedRange: String): String {
    return when (selectedRange) {
        "روزانه" -> {
            val parts = timeLabel.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            val endMinute = minute + 30
            val endHour = if (endMinute >= 60) hour + 1 else hour
            val endMin = if (endMinute >= 60) endMinute - 60 else endMinute

            val period = when {
                endHour < 12 -> "صبح"
                endHour < 17 -> "بعدازظهر"
                else -> "شب"
            }

            val startDisplay = formatTimeOnly(hour, minute)
            val endDisplay = formatTimeOnly(endHour, endMin)

            "\u202B$startDisplay تا $endDisplay $period\u202C"
        }
        "هفتگی" -> {
            when (timeLabel) {
                "ش" -> "شنبه"
                "ی" -> "یکشنبه"
                "د" -> "دوشنبه"
                "س" -> "سه‌شنبه"
                "چ" -> "چهارشنبه"
                "پ" -> "پنجشنبه"
                "ج" -> "جمعه"
                else -> timeLabel
            }
        }
        else -> timeLabel
    }
}

/**
 * Formats hour:minute to a short time string without period (e.g., "۵" or "۵:۳۰").
 */
private fun formatTimeOnly(hour: Int, minute: Int): String {
    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    return if (minute == 0) {
        displayHour.toString().toFarsiDigits()
    } else {
        "${displayHour.toString().toFarsiDigits()}:${minute.toString().padStart(2, '0').toFarsiDigits()}"
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
fun StatBox(title: String, value: Int, modifier: Modifier, unit: String = "bpm") {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The box with value only
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFF5BA3A3)), // Green border
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
                        fontSize = 20.sp,
                        color = Color(0xFF6B6B6B)
                    )
                    if (unit.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            unit,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
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
    hrvChartData: List<Int> = emptyList(),
    selectedRange: String = "روزانه"
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Title row - HRV on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text("HRV", color = Color.Black, fontSize = 20.sp)
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
                HrvAreaChart(data = hrvChartData, selectedRange = selectedRange)
            }
        }
    }
}

@Composable
fun HrvAreaChart(
    data: List<Int>,
    selectedRange: String = "روزانه"
) {
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

            // Draw vertical grid lines (every 6h = 5 main lines)
            val gridCount = 4
            val gridDashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            for (i in 0..gridCount) {
                val x = (width / gridCount) * i
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = gridDashEffect
                )
            }

            if (data.isEmpty()) return@Canvas

            val yMax = 100f
            val yMin = 0f
            val yRange = yMax - yMin

            // Build list of (x, y) points — only non-zero values, positioned by time
            val drawPoints = mutableListOf<Pair<Float, Float>>()

            when (selectedRange) {
                "روزانه" -> {
                    // data = all 48 slots (with zeros). Position by slot index.
                    val totalSlots = data.size.coerceAtLeast(1)
                    data.forEachIndexed { index, value ->
                        if (value > 0) {
                            val x = if (totalSlots > 1) (index.toFloat() / (totalSlots - 1)) * width else width / 2f
                            val clamped = value.coerceIn(yMin.toInt(), yMax.toInt())
                            val y = height - ((clamped - yMin) / yRange) * height
                            drawPoints.add(x to y)
                        }
                    }
                }
                else -> {
                    // Weekly/Monthly: data = daily or weekly averages, evenly spaced
                    // Use full list size for positioning, skip zero values for drawing
                    if (data.all { it <= 0 }) return@Canvas
                    val spacing = if (data.size > 1) width / (data.size - 1) else 0f
                    data.forEachIndexed { index, value ->
                        if (value > 0) {
                            val x = if (data.size > 1) index * spacing else width / 2f
                            val clamped = value.coerceIn(yMin.toInt(), yMax.toInt())
                            val y = height - ((clamped - yMin) / yRange) * height
                            drawPoints.add(x to y)
                        }
                    }
                }
            }

            if (drawPoints.isEmpty()) return@Canvas

            // Build smooth bezier paths through the draw points
            val path = Path()
            val fillPath = Path()

            drawPoints.forEachIndexed { index, (x, y) ->
                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height) // start fill from bottom-left of first point
                    fillPath.lineTo(x, y)
                } else {
                    val (prevX, prevY) = drawPoints[index - 1]
                    val cx = (prevX + x) / 2f
                    path.cubicTo(cx, prevY, cx, y, x, y)
                    fillPath.cubicTo(cx, prevY, cx, y, x, y)
                }
            }

            // Complete fill path
            val lastX = drawPoints.last().first
            fillPath.lineTo(lastX, height)
            fillPath.close()

            // Draw gradient fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE53935).copy(alpha = 0.4f),
                        Color(0xFFE53935).copy(alpha = 0.05f)
                    )
                )
            )

            // Draw line
            drawPath(
                path = path,
                color = Color(0xFFE53935),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // X-axis labels — time-based per range
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                when (selectedRange) {
                    "هفتگی" -> {
                        val today = java.time.LocalDate.now()
                        val persianLabels = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
                        val todayIdx = when (today.dayOfWeek.value) {
                            6 -> 0; 7 -> 1; else -> today.dayOfWeek.value + 1
                        }
                        (6 downTo 0).forEach { i ->
                            Text(
                                persianLabels[(todayIdx - i + 7) % 7],
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    "ماهانه" -> {
                        listOf("هفته ۱", "هفته ۲", "هفته ۳", "هفته ۴").forEach {
                            Text(it, fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                    else -> {
                        // Daily: time labels matching 48 half-hour slots
                        listOf("00:00", "06:00", "12:00", "18:00", "24:00").forEach {
                            Text(it, fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}


// ============ Previews ============

@Preview(showBackground = true, widthDp = 380, heightDp = 700)
@Composable
private fun HeartRateChartPreview() {
    val sampleData = listOf(
        HeartRateRangePoint("00:00", 0, 0),
        HeartRateRangePoint("00:30", 0, 0),
        HeartRateRangePoint("01:00", 0, 0),
        HeartRateRangePoint("06:00", 68, 75),
        HeartRateRangePoint("06:30", 70, 78),
        HeartRateRangePoint("07:00", 72, 85),
        HeartRateRangePoint("07:30", 75, 90),
        HeartRateRangePoint("08:00", 65, 80),
        HeartRateRangePoint("08:30", 68, 76),
        HeartRateRangePoint("09:00", 70, 82),
        HeartRateRangePoint("09:30", 0, 0),
        HeartRateRangePoint("10:00", 72, 88),
        HeartRateRangePoint("10:30", 74, 92),
        HeartRateRangePoint("11:00", 78, 95),
        HeartRateRangePoint("11:30", 80, 98),
        HeartRateRangePoint("12:00", 0, 0),
        HeartRateRangePoint("12:30", 0, 0),
        HeartRateRangePoint("13:00", 72, 80),
        HeartRateRangePoint("13:30", 70, 78),
        HeartRateRangePoint("14:00", 68, 75),
        HeartRateRangePoint("15:00", 0, 0),
        HeartRateRangePoint("16:00", 0, 0),
        HeartRateRangePoint("17:00", 85, 103, isAlert = false),
        HeartRateRangePoint("17:30", 90, 120, isAlert = false),
        HeartRateRangePoint("18:00", 95, 130, isAlert = true),
        HeartRateRangePoint("18:30", 88, 110),
        HeartRateRangePoint("19:00", 75, 90),
        HeartRateRangePoint("19:30", 70, 82),
        HeartRateRangePoint("20:00", 68, 78),
        HeartRateRangePoint("20:30", 65, 72),
        HeartRateRangePoint("21:00", 62, 70),
        HeartRateRangePoint("22:00", 60, 68),
        HeartRateRangePoint("23:00", 58, 65),
    )

    // Pre-selected bar for static preview
    val preSelectedBar = sampleData[22] // "17:00" → 85-103 bpm
    var selectedBar by remember { mutableStateOf<HeartRateRangePoint?>(preSelectedBar) }

    MaterialTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .background(Color(0xFFF9F9F9))
                    .padding(16.dp)
            ) {
                // Tooltip or default heart rate info
                if (selectedBar != null) {
                    val bar = selectedBar!!
                    val timeRangeText = formatTimeRange(bar.timeLabel, "روزانه")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFF0F0F0),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${bar.min.toString().toFarsiDigits()} - ${bar.max.toString().toFarsiDigits()}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("bpm", fontSize = 12.sp, color = Color(0xFF444444))
                            }
                            Text(timeRangeText, fontSize = 12.sp, color = Color(0xFF666666))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text("ضربان قلب شما", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("۸۵", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("bpm", fontSize = 14.sp, color = Color(0xFF6B6B6B))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text("امروز ۷ اردیبهشت", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(12.dp))

                HeartRateRangeChart(
                    data = sampleData,
                    selectedRange = "روزانه",
                    initialSelectedIndex = 22,
                    onBarSelected = { bar -> selectedBar = bar }
                )

                Spacer(modifier = Modifier.height(24.dp))

                StatsRow(max = 130, avg = 85, min = 58)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 400)
@Composable
private fun HeartRateChartWeeklyPreview() {
    val weeklyData = listOf(
        HeartRateRangePoint("ش", 62, 95),
        HeartRateRangePoint("ی", 58, 88),
        HeartRateRangePoint("د", 65, 110),
        HeartRateRangePoint("س", 70, 105),
        HeartRateRangePoint("چ", 60, 92),
        HeartRateRangePoint("پ", 68, 130, isAlert = true),
        HeartRateRangePoint("ج", 55, 85),
    )

    val preSelectedBar = weeklyData[3] // "س" day
    var selectedBar by remember { mutableStateOf<HeartRateRangePoint?>(preSelectedBar) }

    MaterialTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .background(Color(0xFFF9F9F9))
                    .padding(16.dp)
            ) {
                if (selectedBar != null) {
                    val bar = selectedBar!!
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFF0F0F0),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${bar.min.toString().toFarsiDigits()} - ${bar.max.toString().toFarsiDigits()}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("bpm", fontSize = 12.sp, color = Color(0xFF444444))
                            }
                            Text(bar.timeLabel, fontSize = 12.sp, color = Color(0xFF666666))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                HeartRateRangeChart(
                    data = weeklyData,
                    selectedRange = "هفتگی",
                    initialSelectedIndex = 3,
                    onBarSelected = { bar -> selectedBar = bar }
                )
            }
        }
    }
}