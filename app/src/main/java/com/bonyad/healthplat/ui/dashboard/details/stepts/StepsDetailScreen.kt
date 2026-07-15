package com.bonyad.healthplat.ui.dashboard.details.stepts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.DateStrip
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.TimeRangeSelector
import com.bonyad.healthplat.ui.utils.rtl
import com.bonyad.healthplat.ui.utils.toFarsiDigits
import kotlin.math.abs

@Composable
fun StepsDetailScreen(
    viewModel: StepsDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onInfoClick: () -> Unit = {}
) {
    val barData by viewModel.barChartData.collectAsState()
    val comparisonData by viewModel.comparisonData.collectAsState()
    val totalSteps by viewModel.totalSteps.collectAsState()
    val averageSteps by viewModel.averageSteps.collectAsState()
    val selectedRange by viewModel.selectedTimeRange.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val dateLabel by viewModel.dateLabel.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val orangeLabel by viewModel.orangeLabel.collectAsState()
    val greyLabel by viewModel.greyLabel.collectAsState()
    val orangeValue by viewModel.orangeValue.collectAsState()
    val greyValue by viewModel.greyValue.collectAsState()

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
                title = "تعداد قدم",
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Range Selector
            TimeRangeSelector(
                selected = selectedRange,
                onSelect = { viewModel.setTimeRange(it) },
                onCalendarClick = { showDatePicker = true }
            )

            // 2. Date Strip (only for daily view)
            if (selectedRange == "روزانه") {
                DateStrip(
                    selectedDate = selectedDate,
                    onDaySelected = { date -> viewModel.selectDay(date) }
                )
            }

            // 3. Main Chart Section
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = when (selectedRange) {
                            "هفتگی" -> "مجموع هفتگی"
                            "ماهانه" -> "مجموع ماهانه"
                            else -> "مجموع"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B6B6B)
                    )
                }

                // Value row (Steps + unit)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قدم",
                        fontSize = 14.sp,
                        color = Color(0xFF6B6B6B)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = totalSteps.toString().toFarsiDigits(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 20.sp,
                            color = Color.Black
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Date row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                // Bar Chart
                if (isLoading) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(250.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFFF9100))
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        StepsBarChart(
                            data = barData,
                            showEmptyState = barData.isEmpty() || barData.all { it.steps == 0 },
                            selectedRange = selectedRange
                        )
                    }
                }
            }

            // 4. Comparison Card (Bottom Section)
            StepsComparisonCard(
                data = comparisonData,
                todaySteps = orangeValue,
                averageSteps = greyValue,
                orangeLabel = orangeLabel,
                greyLabel = greyLabel,
                selectedRange = selectedRange
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun StepsBarChart(
    data: List<StepsDetailViewModel.StepBarPoint>,
    isLoading: Boolean = false,
    showEmptyState: Boolean = false,
    selectedRange: String = "روزانه",
    onBarSelected: (StepsDetailViewModel.StepBarPoint?) -> Unit = {}
) {
    val maxVal = (data.maxOfOrNull { it.steps } ?: 500).coerceAtLeast(500).toFloat()
    var selectedBarIndex by remember(selectedRange) { mutableIntStateOf(-1) }
    var selectedBarX by remember(selectedRange) { mutableFloatStateOf(0f) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        // Loading state
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFF9100))
            }
            return@Card
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(16.dp)
        ) {
            // Chart body
            Row(modifier = Modifier.fillMaxSize()) {
                // Y-axis labels
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 8.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        maxVal.toInt().toString().toFarsiDigits(),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        (maxVal / 2).toInt().toString().toFarsiDigits(),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text("۰", fontSize = 12.sp, color = Color.Gray)
                }

                // Chart area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 20.dp)
                            .pointerInput(data, selectedRange) {
                                detectTapGestures { tapOffset ->
                                    val w = size.width.toFloat()
                                    val barWidth = when (selectedRange) {
                                        "هفتگی" -> 12.dp.toPx()
                                        "ماهانه" -> 14.dp.toPx()
                                        else -> 3.dp.toPx()
                                    }
                                    val hitRadius = if (selectedRange == "روزانه") 12.dp.toPx() else 16.dp.toPx()
                                    val chartPaddingX = barWidth / 2 + 4.dp.toPx()
                                    val usableWidth = w - chartPaddingX * 2

                                    var closestIndex = -1
                                    var closestDist = Float.MAX_VALUE
                                    var closestX = 0f

                                    data.forEachIndexed { index, point ->
                                        if (point.steps <= 0) return@forEachIndexed
                                        val x = chartPaddingX + point.timePosition * usableWidth
                                        val dist = abs(tapOffset.x - x)
                                        if (dist < hitRadius && dist < closestDist) {
                                            closestDist = dist
                                            closestIndex = index
                                            closestX = x
                                        }
                                    }

                                    if (closestIndex == selectedBarIndex) {
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
                        val w = size.width
                        val h = size.height

                        // Grid lines — horizontal
                        val pathEffect =
                            PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        listOf(0f, 0.5f, 1f).forEach { ratio ->
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.5f),
                                start = Offset(0f, h * ratio),
                                end = Offset(w, h * ratio),
                                pathEffect = pathEffect
                            )
                        }

                        // Grid lines — vertical
                        if (selectedRange == "روزانه") {
                            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { ratio ->
                                drawLine(
                                    color = Color.LightGray.copy(alpha = 0.5f),
                                    start = Offset(w * ratio, 0f),
                                    end = Offset(w * ratio, h),
                                    pathEffect = pathEffect
                                )
                            }
                        } else {
                            listOf(0.25f, 0.5f, 0.75f).forEach { ratio ->
                                drawLine(
                                    color = Color.LightGray.copy(alpha = 0.5f),
                                    start = Offset(w * ratio, 0f),
                                    end = Offset(w * ratio, h),
                                    pathEffect = pathEffect
                                )
                            }
                        }

                        if (data.isEmpty()) return@Canvas

                        val barWidth = when (selectedRange) {
                            "هفتگی" -> 12.dp.toPx()
                            "ماهانه" -> 14.dp.toPx()
                            else -> 3.dp.toPx()
                        }
                        val barColor = Color(0xFFFF9100)
                        val selectedColor = Color(0xFFE65100)
                        val cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)

                        val chartPaddingX = barWidth / 2 + 4.dp.toPx()
                        val usableWidth = w - chartPaddingX * 2

                        data.forEachIndexed { index, point ->
                            if (point.steps <= 0) return@forEachIndexed

                            val isSelected = index == selectedBarIndex
                            val x = chartPaddingX + point.timePosition * usableWidth
                            val barHeight = (point.steps / maxVal * h).coerceIn(4.dp.toPx(), h)
                            val currentBarWidth = if (isSelected) barWidth * 1.5f else barWidth

                            drawRoundRect(
                                color = if (isSelected) selectedColor else barColor,
                                topLeft = Offset(x - currentBarWidth / 2, h - barHeight),
                                size = Size(currentBarWidth, barHeight),
                                cornerRadius = CornerRadius(currentBarWidth / 2, currentBarWidth / 2)
                            )
                        }
                    }

                    // ─── FIX 2: X-axis labels driven by selectedRange ───
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
                                            Text(point.hourLabel, fontSize = 12.sp, color = Color.Gray)
                                        }
                                    } else {
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
                                            Text(point.hourLabel, fontSize = 12.sp, color = Color.Gray)
                                        }
                                    } else {
                                        val (_, _, days) = com.bonyad.healthplat.ui.utils.PersianDateUtils.getCurrentPersianMonthRange()
                                        val numWeeks = (days + 6) / 7
                                        (1..numWeeks).forEach { Text("هفته $it", fontSize = 12.sp, color = Color.Gray) }
                                    }
                                }
                                else -> {
                                    listOf("00:00", "06:00", "12:00", "18:00", "24:00").forEach {
                                        Text(it, fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }

                    // Tooltip overlay
                    if (selectedBarIndex >= 0 && selectedBarIndex < data.size) {
                        val point = data[selectedBarIndex]
                        val timeRangeText = formatStepsTimeRange(point.hourLabel, selectedRange)
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
                                                text = "قدم",
                                                fontSize = 10.sp,
                                                color = Color(0xFF666666)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = point.steps.toString().toFarsiDigits(),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
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

                    // Empty state overlay
                    if (showEmptyState) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "داده‌ای موجود نیست",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Formats the time range for the steps tooltip.
 * Daily: "05:00" → "۵ تا ۵:۳۰ بعدازظهر"
 * Weekly/Monthly: label as-is
 */
private fun formatStepsTimeRange(hourLabel: String, selectedRange: String): String {
    return when (selectedRange) {
        "روزانه" -> {
            val parts = hourLabel.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: return hourLabel
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            val endMinute = minute + 30
            val endHour = if (endMinute >= 60) hour + 1 else hour
            val endMin = if (endMinute >= 60) endMinute - 60 else endMinute

            val period = when {
                endHour < 12 -> "صبح"
                endHour < 17 -> "بعدازظهر"
                else -> "شب"
            }

            val startDisplay = formatStepsTimeOnly(hour, minute)
            val endDisplay = formatStepsTimeOnly(endHour, endMin)

            "\u202B$startDisplay تا $endDisplay $period\u202C"
        }
        "هفتگی" -> {
            when (hourLabel) {
                "ش" -> "شنبه"
                "ی" -> "یکشنبه"
                "د" -> "دوشنبه"
                "س" -> "سه‌شنبه"
                "چ" -> "چهارشنبه"
                "پ" -> "پنجشنبه"
                "ج" -> "جمعه"
                else -> hourLabel
            }
        }
        else -> hourLabel
    }
}

private fun formatStepsTimeOnly(hour: Int, minute: Int): String {
    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    return if (minute == 0) {
        displayHour.toString().toFarsiDigits()
    } else {
        "${displayHour.toString().toFarsiDigits()}:${minute.toString().padStart(2, '0').toFarsiDigits()}"
    }
}

@Composable
fun StepsComparisonCard(
    data: List<StepsDetailViewModel.ComparisonPoint>,
    todaySteps: Int,
    averageSteps: Int,
    orangeLabel: String = "امروز",
    greyLabel: String = "میانگین",
    selectedRange: String = "روزانه"
) {
    val comparisonMessage = when {
        todaySteps == 0 -> "داده‌ای برای مقایسه وجود ندارد.".rtl()
        todaySteps >= averageSteps ->
            "شما در $orangeLabel تعداد قدم بیشتری نسبت به $greyLabel برداشته‌اید.".rtl()

        else ->
            "شما در $orangeLabel تعداد قدم کمتری نسبت به $greyLabel برداشته‌اید.".rtl()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Header (Icon on left, text aligned end)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {

                Icon(
                    painter = painterResource(R.drawable.walk),
                    contentDescription = null,
                    tint = Color(0xFFFF9100),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.weight(1f))


                Text(
                    text = comparisonMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.End
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color(0xFFEEEEEE)
            )

            // Legend (SWAPPED: Average first, Today second)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                // Grey legend — LEFT
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .background(Color.Gray, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(greyLabel, color = Color.Gray, fontSize = 12.sp)
                    }
                    Text(
                        "${averageSteps.toString().toFarsiDigits()} قدم",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }

                // Orange legend — RIGHT
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .background(Color(0xFFFF9100), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(orangeLabel, color = Color(0xFFFF9100), fontSize = 12.sp)
                    }
                    Text(
                        "${todaySteps.toString().toFarsiDigits()} قدم",
                        color = Color(0xFFFF9100),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Comparison Line Chart
            if (data.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val maxSteps = (data.maxOfOrNull {
                            maxOf(
                                if (it.todaySteps >= 0) it.todaySteps else 0,
                                it.avgSteps
                            )
                        } ?: 1).coerceAtLeast(100).toFloat()

                        fun getCoord(
                            point: StepsDetailViewModel.ComparisonPoint,
                            isToday: Boolean
                        ): Offset {
                            val x = point.timeRatio * w
                            val steps = if (isToday) point.todaySteps else point.avgSteps
                            val y = h - (steps / maxSteps * h)
                            return Offset(x, y.coerceIn(0f, h))
                        }

                        // 1. Draw Average/Grey Line (full range)
                        val avgPath = Path()
                        data.forEachIndexed { i, p ->
                            val coord = getCoord(p, false)
                            if (i == 0) avgPath.moveTo(coord.x, coord.y)
                            else avgPath.lineTo(coord.x, coord.y)

                            if (i == data.lastIndex) {
                                drawCircle(Color.Gray, 3.dp.toPx(), coord)
                            }
                        }
                        drawPath(
                            path = avgPath,
                            color = Color.Gray,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // 2. Draw Today/Orange Line (stops at -1 sentinel)
                        val todayPoints = data.filter { it.todaySteps >= 0 }
                        if (todayPoints.isNotEmpty()) {
                            val todayPath = Path()
                            todayPoints.forEachIndexed { i, p ->
                                val coord = getCoord(p, true)
                                if (i == 0) todayPath.moveTo(coord.x, coord.y)
                                else todayPath.lineTo(coord.x, coord.y)

                                if (i == todayPoints.lastIndex) {
                                    drawCircle(Color(0xFFFF9100), 3.dp.toPx(), coord)
                                    drawLine(
                                        color = Color.LightGray,
                                        start = coord,
                                        end = Offset(coord.x, h),
                                        pathEffect = PathEffect.dashPathEffect(
                                            floatArrayOf(5f, 5f), 0f
                                        )
                                    )
                                }
                            }
                            drawPath(
                                path = todayPath,
                                color = Color(0xFFFF9100),
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Bottom Time Labels — static per selectedRange
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
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
                                    (1..4).forEach { week ->
                                        Text(
                                            "هفته ${week.toString().toFarsiDigits()}",
                                            fontSize = 9.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                else -> {
                                    listOf("00:00", "06:00", "12:00", "18:00", "24:00").forEach {
                                        Text(it, fontSize = 9.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
