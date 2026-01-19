package com.bonyad.healthplat.ui.dashboard.details.stepts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.R
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.CustomDetailTopBar
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.DateStrip
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.TimeRangeSelector
import com.bonyad.healthplat.ui.utils.toFarsiDigits

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
                onSelect = { viewModel.setTimeRange(it) }
            )

            // 2. Date Strip (only for daily view)
            if (selectedRange == "روزانه") {
                val selectedOffset by viewModel.selectedDayOffset.collectAsState()
                DateStrip(
                    selectedOffset = selectedOffset,
                    onDaySelected = { offset ->
                        viewModel.selectDay(offset)
                    }
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
                        color = Color.Gray
                    )
                }

                // Value row (Steps + unit) — EXACTLY like bpm row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قدم",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = totalSteps.toString().toFarsiDigits(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Date row — EXACT SAME LOGIC as HR
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

                Spacer(modifier = Modifier.height(24.dp))

                // Bar Chart
                if (barData.isNotEmpty()) {
                    StepsBarChart(data = barData)
                } else if (!isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("داده‌ای موجود نیست", color = Color.Gray)
                    }
                }
            }

            // 4. Comparison Card (Bottom Section)
            StepsComparisonCard(
                data = comparisonData,
                todaySteps = totalSteps,
                averageSteps = averageSteps
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun StepsBarChart(data: List<StepsDetailViewModel.StepBarPoint>) {
    val maxVal = (data.maxOfOrNull { it.steps } ?: 1).coerceAtLeast(1000).toFloat()
    val selectedPoint = data.find { it.isSelected }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 30.dp, end = 16.dp, bottom = 24.dp, top = 40.dp)
        ) {
            val w = size.width
            val h = size.height

            // 1. Draw Grid Lines (Horizontal)
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            listOf(0f, 0.5f, 1f).forEach { ratio ->
                val y = h * ratio
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    pathEffect = pathEffect
                )
            }

            // 2. Draw Vertical Grid Lines
            listOf(0.25f, 0.5f, 0.75f).forEach { ratio ->
                val x = w * ratio
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    pathEffect = pathEffect
                )
            }

            // 3. Draw Bars
            val barWidth = 12.dp.toPx()
            val spacing = w / (data.size + 1)

            data.forEachIndexed { index, point ->
                val x = spacing * (index + 1)
                val barHeight = (point.steps / maxVal * h).coerceAtLeast(4.dp.toPx())

                drawRoundRect(
                    color = Color(0xFFFF9100),
                    topLeft = Offset(x - barWidth / 2, h - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )

                // 4. Draw Tooltip line if selected
                if (point.isSelected) {
                    drawLine(
                        color = Color.Gray,
                        start = Offset(x, h - barHeight),
                        end = Offset(x, -30f),
                        pathEffect = pathEffect,
                        strokeWidth = 2f
                    )
                }
            }
        }

        // 5. Tooltip Box (Overlay UI) - Dynamic data
        if (selectedPoint != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = (20).dp, y = (-10).dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${selectedPoint.steps.toString().toFarsiDigits()} قدم",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        selectedPoint.hourLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }

        // 6. Labels (Y-Axis)
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                maxVal.toInt().toString().toFarsiDigits(),
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(y = (-20).dp)
            )
            Text(
                (maxVal / 5).toInt().toString().toFarsiDigits(),
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(y = (-50).dp)
            )
        }

        // 7. Labels (X-Axis)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 30.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (data.size <= 7) {
                // Show all labels for weekly view
                data.forEach { point ->
                    Text(point.hourLabel, fontSize = 10.sp, color = Color.Gray)
                }
            } else {
                // Show time labels for daily view
                Text("00:00", fontSize = 10.sp, color = Color.Gray)
                Text("07:59", fontSize = 10.sp, color = Color.Gray)
                Text("15:59", fontSize = 10.sp, color = Color.Gray)
                Text("23:59", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun StepsComparisonCard(
    data: List<StepsDetailViewModel.ComparisonPoint>,
    todaySteps: Int,
    averageSteps: Int
) {
    val comparisonMessage = when {
        todaySteps == 0 -> "داده‌ای برای مقایسه وجود ندارد."
        todaySteps >= averageSteps ->
            "🎉 !شما امروز تعداد قدم بیشتری نسبت به میانگین برداشته‌اید. آفرین"

        else ->
            "شما امروز تعداد قدم کمتری نسبت به هر روز خود در این زمان برداشته اید."
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

                // Average (Grey) — NOW LEFT
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .background(Color.Gray, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("میانگین", color = Color.Gray, fontSize = 12.sp)
                    }
                    Text(
                        "${averageSteps.toString().toFarsiDigits()} قدم",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }

                // Today (Orange) — NOW RIGHT
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .background(Color(0xFFFF9100), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("امروز", color = Color(0xFFFF9100), fontSize = 12.sp)
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
                        .height(100.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val maxSteps = (data.maxOfOrNull { maxOf(it.todaySteps, it.avgSteps) } ?: 1)
                            .coerceAtLeast(100).toFloat()

                        fun getCoord(
                            point: StepsDetailViewModel.ComparisonPoint,
                            isToday: Boolean
                        ): Offset {
                            val x = point.timeRatio * w
                            val steps = if (isToday) point.todaySteps else point.avgSteps
                            val y = h - (steps / maxSteps * h)
                            return Offset(x, y.coerceIn(0f, h))
                        }

                        // 1. Draw Average Line (Grey)
                        val avgPath = Path()
                        data.forEachIndexed { i, p ->
                            val coord = getCoord(p, false)
                            if (i == 0) avgPath.moveTo(
                                coord.x,
                                coord.y
                            ) else avgPath.lineTo(coord.x, coord.y)

                            if (i == data.lastIndex) {
                                drawCircle(Color.Gray, 3.dp.toPx(), coord)
                            }
                        }
                        drawPath(
                            path = avgPath,
                            color = Color.Gray,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // 2. Draw Today Line (Orange)
                        val todayPath = Path()
                        data.forEachIndexed { i, p ->
                            val coord = getCoord(p, true)
                            if (i == 0) todayPath.moveTo(coord.x, coord.y) else todayPath.lineTo(
                                coord.x,
                                coord.y
                            )

                            if (i == data.lastIndex) {
                                drawCircle(Color(0xFFFF9100), 3.dp.toPx(), coord)
                                drawLine(
                                    color = Color.LightGray,
                                    start = coord,
                                    end = Offset(coord.x, h),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                )
                            }
                        }
                        drawPath(
                            path = todayPath,
                            color = Color(0xFFFF9100),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Bottom Time Labels
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .offset(y = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("۱۲ شب", fontSize = 8.sp, color = Color.Gray)
                        Text("۵ غروب", fontSize = 8.sp, color = Color.Gray)
                        Text("۱۲ شب", fontSize = 8.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}