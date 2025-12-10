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
    onBack: () -> Unit
) {
    val barData by viewModel.barChartData.collectAsState()
    val comparisonData by viewModel.comparisonData.collectAsState()
    val totalSteps by viewModel.totalSteps.collectAsState()
    val selectedRange by viewModel.selectedTimeRange.collectAsState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CustomDetailTopBar(
                    title = "تعداد قدم", // "Step Count"
                    onBack = onBack,
                    onSync = {},
                    onInfo = {}
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

                // 2. Date Strip
                val selectedOffset by viewModel.selectedDayOffset.collectAsState()

                DateStrip(
                    selectedOffset = selectedOffset,
                    onDaySelected = { offset ->
                        viewModel.selectDay(offset)
                    }
                )

                // 3. Main Chart Section
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // Header Texts
                    Text(
                        text = "مجموع",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = totalSteps.toString().toFarsiDigits(),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "قدم",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Text(
                            text = "امروز ۲۲ مهر",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // The Bar Chart
                    StepsBarChart(data = barData)
                }

                // 4. Comparison Card (Bottom Section)
                StepsComparisonCard(data = comparisonData)

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun StepsBarChart(data: List<StepsDetailViewModel.StepBarPoint>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 30.dp, end = 16.dp, bottom = 24.dp, top = 40.dp) // Padding for labels
        ) {
            val w = size.width
            val h = size.height
            val maxVal = 5000f // Hardcoded based on image Y-axis

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

            // 2. Draw Vertical Grid Lines (for time periods)
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
            // Note: In a real app, map 'hourLabel' to X position.
            // Here we distribute roughly for the mock.
            val barWidth = 8.dp.toPx()

            data.forEachIndexed { index, point ->
                // Mocking X position based on rough index mapping to 24h
                // In production, parse "17:00" to float 0..1
                val xRatio = when(index) {
                    0 -> 0.28f // 07:00
                    1 -> 0.32f
                    2 -> 0.36f // 09:00
                    3 -> 0.40f
                    4 -> 0.65f // 16:00
                    5 -> 0.70f // 17:00 (Selected)
                    6 -> 0.75f
                    else -> 0.85f
                }

                val x = w * xRatio
                val barHeight = (point.steps / maxVal) * h

                // Draw Bar
                drawRoundRect(
                    color = Color(0xFFFF9100), // Vibrant Orange
                    topLeft = Offset(x - barWidth / 2, h - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // 4. Draw Tooltip (Only if selected) - Matches steps 2.png
                if (point.isSelected) {
                    // Dashed line going up
                    drawLine(
                        color = Color.Gray,
                        start = Offset(x, h - barHeight),
                        end = Offset(x, -30f), // Go up above chart
                        pathEffect = pathEffect,
                        strokeWidth = 2f
                    )
                }
            }
        }

        // 5. Tooltip Box (Overlay UI)
        // Positioned manually to match the mock "selected" item
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = (20).dp, y = (-10).dp) // Adjust to align with the tall bar
                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                .border(1.dp, Color.White, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("۹۶۷ قدم", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("۵ تا ۶ بعد از ظهر", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }

        // 6. Labels (Y-Axis) - Manual positioning
        Box(modifier = Modifier.fillMaxSize()) {
            Text("۵۰۰۰", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterStart).offset(y = (-20).dp))
            Text("۱۰۰۰", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.align(Alignment.BottomStart).offset(y = (-50).dp))
        }

        // 7. Labels (X-Axis)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 30.dp, end = 16.dp), // Match canvas padding
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("00:00", fontSize = 10.sp, color = Color.Gray)
            Text("07:59", fontSize = 10.sp, color = Color.Gray)
            Text("15:59", fontSize = 10.sp, color = Color.Gray)
            Text("23:59", fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun StepsComparisonCard(data: List<StepsDetailViewModel.ComparisonPoint>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Icon
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    painter = painterResource(R.drawable.walk), // Replace with walking icon
                    contentDescription = null,
                    tint = Color(0xFFFF9100),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "شما امروز تعداد قدم کمتری نسبت به هر روز خود در این زمان برداشته اید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray,
                    lineHeight = 20.sp
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFEEEEEE))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Today (Orange)
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(Color(0xFFFF9100), CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("امروز", color = Color(0xFFFF9100), fontSize = 12.sp)
                    }
                    Text("۱۰۵۸ قدم", color = Color(0xFFFF9100), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                // Average (Grey)
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(Color.Gray, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("میانگین", color = Color.Gray, fontSize = 12.sp)
                    }
                    Text("۱۹۴۸ قدم", color = Color.Gray, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Comparison Line Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val maxSteps = 3500f // Y-Axis Max

                    // Helper to map point to coordinates
                    fun getCoord(point: StepsDetailViewModel.ComparisonPoint, isToday: Boolean): Offset {
                        val x = point.timeRatio * w
                        val steps = if (isToday) point.todaySteps else point.avgSteps
                        val y = h - (steps / maxSteps * h)
                        return Offset(x, y)
                    }

                    // 1. Draw Average Line (Grey)
                    val avgPath = Path()
                    data.forEachIndexed { i, p ->
                        val coord = getCoord(p, false)
                        if (i == 0) avgPath.moveTo(coord.x, coord.y) else avgPath.lineTo(coord.x, coord.y)

                        // Draw dot at current time index
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
                        if (i == 0) todayPath.moveTo(coord.x, coord.y) else todayPath.lineTo(coord.x, coord.y)

                        if (i == data.lastIndex) {
                            drawCircle(Color(0xFFFF9100), 3.dp.toPx(), coord)
                            // Dashed vertical line for current time
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
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).offset(y = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("۱۲ شب", fontSize = 8.sp, color = Color.Gray)
                    Text("۵ غروب", fontSize = 8.sp, color = Color.Gray)
                    Text("۱۲ شب", fontSize = 8.sp, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}