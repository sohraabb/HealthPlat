package com.bonyad.healthplat.ui.dashboard.details.sp02

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.ui.components.PersianDate
import com.bonyad.healthplat.ui.dashboard.details.CustomDetailTopBar
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.DateStrip
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.TimeRangeSelector
import com.bonyad.healthplat.ui.utils.PersianDateUtils
import com.bonyad.healthplat.ui.utils.toFarsiDigits

@Composable
fun SpO2DetailScreen(
    viewModel: SpO2DetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onInfoClick: () -> Unit = {}
) {
    val chartData by viewModel.chartData.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val selectedRange by viewModel.selectedTimeRange.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val dateLabel by viewModel.dateLabel.collectAsState()
    val rangeText by viewModel.rangeText.collectAsState()
    val xAxisLabels by viewModel.xAxisLabels.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }

    val todayPersian = remember {
        val (jy, jm, jd) = PersianDateUtils.getCurrentJalaliDate()
        PersianDate(jy, jm, jd)
    }
    val selectedPersianDate = remember(selectedDate) {
        val (jy, jm, jd) = PersianDateUtils.georgianToJalali(
            selectedDate.year, selectedDate.monthValue, selectedDate.dayOfMonth
        )
        PersianDate(jy, jm, jd)
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
                title = "اکسیژن خون",
                onBack = onBack,
                onSync = { viewModel.refreshData() },
                onInfo = { onInfoClick() },
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

            // 3. Chart Section - Always show the chart structure
            SpO2ChartSection(
                data = chartData,
                rangeText = rangeText,
                dateLabel = dateLabel,
                selectedRange = selectedRange,
                isLoading = isLoading,
                showEmptyState = chartData.isEmpty() && !isLoading,
                xAxisLabels = xAxisLabels
            )

            // 4. Latest Measurement Card
            LatestMeasurementCard(
                value = stats.lastValue,
                time = stats.lastTime.toFarsiDigits()
            )

            // 5. Stats Row (High, Avg, Low)
            SpO2StatsRow(stats)

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}


@Composable
fun SpO2ChartSection(
    data: List<SpO2DetailViewModel.SpO2Point>,
    rangeText: String,
    dateLabel: String,
    selectedRange: String,
    isLoading: Boolean = false,
    showEmptyState: Boolean = false,
    xAxisLabels: List<String> = emptyList()
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Chart Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = when (selectedRange) {
                    "هفتگی", "ماهانه" -> "میانگین بازه اکسیژن"
                    else -> "بازه اکسیژن"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Value row (range %)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (rangeText.isNotEmpty())
                    rangeText.toFarsiDigits()
                else
                    "-",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Black
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "%",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Date row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chart Card (same structure as Heart Rate)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF4DD0E1))
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Y-Axis Labels
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(end = 8.dp, bottom = 20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("۱۰۰", fontSize = 12.sp, color = Color.Gray)
                            Text("۹۵", fontSize = 12.sp, color = Color.Gray)
                            Text("۹۰", fontSize = 12.sp, color = Color.Gray)
                            Text("۸۵", fontSize = 12.sp, color = Color.Gray)
                        }

                        // Chart Area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 20.dp)
                            ) {
                                val w = size.width
                                val h = size.height

                                val minVal = 85f
                                val maxVal = 100f
                                val range = maxVal - minVal

                                // Horizontal Grid Lines
                                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                                val steps = 3

                                for (i in 0..steps) {
                                    val y = h * (i.toFloat() / steps)
                                    drawLine(
                                        color = Color.LightGray.copy(alpha = 0.5f),
                                        start = Offset(0f, y),
                                        end = Offset(w, y),
                                        strokeWidth = 1.dp.toPx(),
                                        pathEffect = pathEffect
                                    )
                                }

                                // Vertical Grid Lines (every 6h = 5 main lines)
                                val verticalLines = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
                                verticalLines.forEach { ratio ->
                                    val x = w * ratio
                                    drawLine(
                                        color = Color.LightGray.copy(alpha = 0.3f),
                                        start = Offset(x, 0f),
                                        end = Offset(x, h),
                                        strokeWidth = 1.dp.toPx(),
                                        pathEffect = pathEffect
                                    )
                                }


                                // Draw data points
                                if (data.isNotEmpty()) {
                                    val dotColor = Color(0xFF4DD0E1)

                                    if (selectedRange == "روزانه") {
                                        // Daily: scatter dots
                                        data.forEach { point ->
                                            val x = w * point.timeRatio
                                            val y = (h - ((point.value - minVal) / range) * h).coerceIn(0f, h)
                                            drawCircle(
                                                color = dotColor,
                                                radius = 4.dp.toPx(),
                                                center = Offset(x, y)
                                            )
                                        }
                                    } else {
                                        // Weekly/Monthly: range bar chart (min to max capsules)
                                        val barWidth = if (selectedRange == "هفتگی") 12.dp.toPx() else 14.dp.toPx()
                                        val chartPaddingX = barWidth / 2 + 4.dp.toPx()
                                        val usableWidth = w - chartPaddingX * 2

                                        data.forEach { point ->
                                            if (point.min <= 0 && point.max <= 0) return@forEach
                                            val x = chartPaddingX + point.timeRatio * usableWidth

                                            val safeMin = point.min.toFloat().coerceIn(minVal, maxVal)
                                            val safeMax = point.max.toFloat().coerceIn(minVal, maxVal)

                                            val yTop = h - ((safeMax - minVal) / range) * h
                                            val yBottom = h - ((safeMin - minVal) / range) * h
                                            val capsuleHeight = (yBottom - yTop).coerceAtLeast(barWidth)

                                            if (capsuleHeight < barWidth * 2) {
                                                // Draw as dot when range is tiny
                                                drawCircle(
                                                    color = dotColor,
                                                    radius = barWidth / 2,
                                                    center = Offset(x, yTop + capsuleHeight / 2)
                                                )
                                            } else {
                                                drawRoundRect(
                                                    color = dotColor,
                                                    topLeft = Offset(x - barWidth / 2, yTop),
                                                    size = Size(barWidth, capsuleHeight),
                                                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Time Labels at bottom - LTR for correct ordering
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp)
                                        .align(Alignment.BottomCenter),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    when (selectedRange) {
                                        "هفتگی", "ماهانه" -> {
                                            // Dynamic labels from ViewModel
                                            xAxisLabels.forEach {
                                                Text(it, fontSize = 12.sp, color = Color.Gray)
                                            }
                                        }
                                        else -> {
                                            listOf("00:00", "06:00", "12:00", "18:00", "24:00").forEach { label ->
                                                Text(
                                                    text = label.toFarsiDigits(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Gray,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Empty State Overlay
                    if (showEmptyState) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "داده‌ای موجود نیست",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6B6B6B)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LatestMeasurementCard(value: Int, time: String, unit: String = "%") {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFF5BA3A3))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Value
            Row(verticalAlignment = Alignment.Bottom) {

                Text(
                    text = if (value > 0) value.toString().toFarsiDigits() else "-",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF6B6B6B)
                )

                if (unit.isNotEmpty()) {
                    Text(
                        text = " $unit",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B6B6B),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }


            // Label
            Text(
                text = if (time.isNotEmpty()) "آخرین اندازه گیری: $time" else "آخرین اندازه گیری",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B6B6B)
            )


        }
    }
}

@Composable
fun SpO2StatsRow(stats: SpO2DetailViewModel.SpO2Stats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SpO2StatCard("بالا ترین", stats.high, Modifier.weight(1f))
        SpO2StatCard("میانگین", stats.avg, Modifier.weight(1f))
        SpO2StatCard("پایین ترین", stats.low, Modifier.weight(1f))
    }
}

@Composable
fun SpO2StatCard(title: String, value: Int, modifier: Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The box with value only
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFF5BA3A3))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (value > 0) value.toString().toFarsiDigits() else "-",
                        fontSize = 20.sp,
                        color = Color(0xFF6B6B6B)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "%",
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