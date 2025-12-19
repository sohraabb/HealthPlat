package com.bonyad.healthplat.ui.dashboard.details.sleep


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
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
import com.bonyad.healthplat.ui.dashboard.TextGray
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.CustomDetailTopBar
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.DateStrip
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.TimeRangeSelector
import com.bonyad.healthplat.ui.utils.toFarsiDigits

@Composable
fun SleepDetailScreen(
    viewModel: SleepDetailViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val timelineData by viewModel.sleepTimeline.collectAsState()
    val stats by viewModel.sleepStats.collectAsState()
    val percentages by viewModel.stagePercentages.collectAsState()
    val selectedRange by viewModel.selectedTimeRange.collectAsState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CustomDetailTopBar(title = "پایش خواب",
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
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. Selector & Dates
                TimeRangeSelector(selected = selectedRange, onSelect = { viewModel.setTimeRange(it) })

                val selectedOffset by viewModel.selectedDayOffset.collectAsState()

                DateStrip(
                    selectedOffset = selectedOffset,
                    onDaySelected = { offset ->
                        viewModel.selectDay(offset)
                    }
                )
                // 2. Main Header (Time)
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("زمان خواب", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.align(Alignment.End))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text("دقیقه", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stats.minutes.toString().toFarsiDigits(), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ساعت", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, modifier = Modifier.padding(bottom = 6.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stats.hours.toString().toFarsiDigits(), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Text("امروز ۲۲ مهر", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.align(Alignment.End))
                }

                // 3. Timeline Chart (The rows of bars)
                SleepTimelineChart(timelineData)

                // 4. Quality Donut Card
                SleepQualityCard(stats.score, percentages)

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun SleepTimelineChart(data: List<SleepDetailViewModel.SleepSegment>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Label Row
            Text("بیدار", fontSize = 12.sp, color = Color.Gray)
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))

            // Chart Area
            Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val rowHeight = h / 3 // Light, Deep, REM rows

                    // Grid Lines (Vertical Time)
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    listOf(0.25f, 0.5f, 0.75f).forEach { r ->
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.5f),
                            start = Offset(w * r, 0f),
                            end = Offset(w * r, h),
                            pathEffect = pathEffect
                        )
                    }

                    // Draw Segments
                    data.forEach { segment ->
                        val x = segment.startRatio * w
                        val width = segment.widthRatio * w

                        val (color, yTop) = when (segment.stage) {
                            SleepDetailViewModel.SleepStage.LIGHT -> Color(0xFF66BB6A) to 0f // Green
                            SleepDetailViewModel.SleepStage.DEEP -> Color(0xFF42A5F5) to rowHeight // Blue
                            SleepDetailViewModel.SleepStage.REM -> Color(0xFFAB47BC) to rowHeight * 2 // Purple
                            else -> Color.Transparent to 0f
                        }

                        if (color != Color.Transparent) {
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(x, yTop + 10f), // +10 padding
                                size = Size(width, rowHeight - 20f),
                                cornerRadius = CornerRadius(8f, 8f)
                            )
                        }
                    }
                }

                // Labels Overlay (Right side labels in layout)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    Text("سبک", fontSize = 12.sp, color = Color.Gray)
                    Text("عمیق", fontSize = 12.sp, color = Color.Gray)
                    Text("REM", fontSize = 12.sp, color = Color.Gray)
                }
            }

            // X-Axis
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("21:00", fontSize = 10.sp, color = Color.Gray)
                Text("00:00", fontSize = 10.sp, color = Color.Gray)
                Text("03:00", fontSize = 10.sp, color = Color.Gray)
                Text("06:00", fontSize = 10.sp, color = Color.Gray)
                Text("09:00", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextGray
        )
    }
}

@Composable
fun SleepQualityCard(score: Int, percentages: Triple<Int, Int, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("کیفیت خواب", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut Chart
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 25f.dp.toPx()
                        val sizeBox = size.minDimension

                        // Deep (Yellow in screenshot example, but using standard match)
                        // Actually screenshot uses: Purple (Large), Green, Yellow?
                        // Let's match screenshot colors:
                        // Purple (53%), Green (18%), Yellow (29%)

                        val sweep1 = 360f * (percentages.third / 100f) // Purple
                        val sweep2 = 360f * (percentages.second / 100f) // Green
                        val sweep3 = 360f * (percentages.first / 100f)  // Yellow

                        var startAngle = -90f

                        // 1. Purple
                        drawArc(Color(0xFFAB47BC), startAngle, sweep1, false, style = Stroke(
                            strokeWidth,
                            cap = StrokeCap.Round
                        )
                        )
                        startAngle += sweep1

                        // 2. Green
                        drawArc(Color(0xFF66BB6A), startAngle, sweep2, false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                        startAngle += sweep2

                        // 3. Yellow
                        drawArc(Color(0xFFFFCA28), startAngle, sweep3, false, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                    }
                    Text(score.toString().toFarsiDigits(), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Legend
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("عالی داریوش", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    LegendItem("REM", Color(0xFFAB47BC))
                    LegendItem("سبک", Color(0xFF66BB6A))
                    LegendItem("عمیق", Color(0xFFFFCA28))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "زمان خوابت خیلی خوب بوده و امتیاز خواب دیشب شده ۹۵، همینطوری ادامه بده",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                lineHeight = 20.sp
            )
        }
    }
}