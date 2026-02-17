package com.bonyad.healthplat.ui.dashboard.details.sleep


import androidx.compose.foundation.BorderStroke
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
    onBack: () -> Unit,
    onInfoClick: () -> Unit = {}
) {
    val timelineData by viewModel.sleepTimeline.collectAsState()
    val stats by viewModel.sleepStats.collectAsState()
    val percentages by viewModel.stagePercentages.collectAsState()
    val selectedRange by viewModel.selectedTimeRange.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val dateLabel by viewModel.dateLabel.collectAsState()

    Scaffold(
        topBar = {
            CustomDetailTopBar(
                title = "پایش خواب",
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
            // 1. Selector & Dates
            TimeRangeSelector(
                selected = selectedRange,
                onSelect = { viewModel.setTimeRange(it) }
            )

            // Date Strip (only for daily view)
            if (selectedRange == "روزانه") {
                val selectedOffset by viewModel.selectedDayOffset.collectAsState()
                DateStrip(
                    selectedOffset = selectedOffset,
                    onDaySelected = { offset ->
                        viewModel.selectDay(offset)
                    }
                )
            }

            // 2. Main Header (Time)
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = when (selectedRange) {
                        "هفتگی" -> "میانگین زمان خواب"
                        "ماهانه" -> "میانگین زمان خواب"
                        else -> "زمان خواب"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B6B6B),
                    modifier = Modifier.align(Alignment.End)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        "دقیقه",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B6B6B),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        stats.minutes.toString().toFarsiDigits(),
                        color = Color.Black,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 20.sp,
                            color = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "ساعت",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B6B6B),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        stats.hours.toString().toFarsiDigits(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 20.sp,
                            color = Color.Black
                        )
                    )
                }
                // Dynamic date label
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            // 3. Timeline Chart (only for daily view) - Always show structure
            if (selectedRange == "روزانه") {
                SleepTimelineChart(
                    data = timelineData,
                    showEmptyState = timelineData.isEmpty() && !isLoading
                )
            }

            // 4. Quality Donut Card
            SleepQualityCard(
                score = stats.score,
                percentages = percentages,
                qualityLabel = viewModel.getSleepQualityLabel(),
                userName = ""
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun SleepTimelineChart(
    data: List<SleepDetailViewModel.SleepSegment>,
    showEmptyState: Boolean = false
) {
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
            HorizontalDivider(
                thickness = 0.5.dp,
                color = Color.LightGray.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                // Always draw the chart canvas with grid
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val rowHeight = h / 3

                    // Grid Lines (Vertical Time) - Always drawn
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    listOf(0.25f, 0.5f, 0.75f).forEach { r ->
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.5f),
                            start = Offset(w * r, 0f),
                            end = Offset(w * r, h),
                            pathEffect = pathEffect
                        )
                    }

                    // Draw Segments - Only if we have data
                    if (data.isNotEmpty()) {
                        data.forEach { segment ->
                            val x = segment.startRatio * w
                            val width = (segment.widthRatio * w).coerceAtLeast(4f)

                            val (color, yTop) = when (segment.stage) {
                                SleepDetailViewModel.SleepStage.LIGHT -> Color(0xFF66BB6A) to 0f
                                SleepDetailViewModel.SleepStage.DEEP -> Color(0xFF42A5F5) to rowHeight
                                SleepDetailViewModel.SleepStage.REM -> Color(0xFFAB47BC) to rowHeight * 2
                                else -> Color.Transparent to 0f
                            }

                            if (color != Color.Transparent) {
                                drawRoundRect(
                                    color = color,
                                    topLeft = Offset(x, yTop + 10f),
                                    size = Size(width, rowHeight - 20f),
                                    cornerRadius = CornerRadius(8f, 8f)
                                )
                            }
                        }
                    }
                }

                // Labels Overlay - Always shown
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    Text("سبک", fontSize = 12.sp, color = Color.Gray)
                    Text("عمیق", fontSize = 12.sp, color = Color.Gray)
                    Text("REM", fontSize = 12.sp, color = Color.Gray)
                }

                // Empty State Overlay - Only shown when there's no data
                if (showEmptyState) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "داده خوابی موجود نیست",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            }

            // X-Axis - Always shown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("۲۱:۰۰", fontSize = 10.sp, color = Color.Gray)
                Text("۰۰:۰۰", fontSize = 10.sp, color = Color.Gray)
                Text("۰۳:۰۰", fontSize = 10.sp, color = Color.Gray)
                Text("۰۶:۰۰", fontSize = 10.sp, color = Color.Gray)
                Text("۰۹:۰۰", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(end = 8.dp)
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
    }
}

@Composable
fun SleepQualityCard(
    score: Int,
    percentages: Triple<Int, Int, Int>, // first=Deep, second=Light, third=REM
    qualityLabel: String,
    userName: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFEBEBEB)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. PIE CHART (Aligned Start/Left)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(135.dp)
                ) {
                    Canvas(modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)) {
                        val strokeWidth = 16.dp.toPx()

                        // Background Track (The gray circle behind the segments)
                        drawArc(
                            color = Color(0xFFF2F2F2),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )

                        val total = percentages.first + percentages.second + percentages.third
                        if (total > 0) {
                            val capStyle = Stroke(width = strokeWidth, cap = StrokeCap.Square)

                            var startAngle = -90f

                            // REM - Purple
                            val sweepREM = 360f * (percentages.third / 100f)
                            if (sweepREM > 0) {
                                drawArc(Color(0xFFAB47BC), startAngle, sweepREM, false, style = capStyle)
                                startAngle += sweepREM
                            }

                            // Light - Green
                            val sweepLight = 360f * (percentages.second / 100f)
                            if (sweepLight > 0) {
                                drawArc(Color(0xFF66BB6A), startAngle, sweepLight, false, style = capStyle)
                                startAngle += sweepLight
                            }

                            // Deep - Yellow/Amber
                            val sweepDeep = 360f * (percentages.first / 100f)
                            if (sweepDeep > 0) {
                                drawArc(Color(0xFFFFCA28), startAngle, sweepDeep, false, style = capStyle)
                            }
                        }
                    }
                    // Score in center
                    Text(
                        text = if (score > 0) score.toString().toFarsiDigits() else "-",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 24.sp,
                            color = Color.Black
                        )
                    )
                }

                // 2. STATUS & LEGEND (Aligned End/Right)
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Title Aligned to End
                    Text(
                        text = "$qualityLabel $userName",
                        fontSize = 20.sp,
                        color = Color(0xFF6B6B6B),
                        textAlign = TextAlign.End
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Legend items
                    LegendItem("REM", Color(0xFFAB47BC))
                    LegendItem("خواب سبک", Color(0xFF66BB6A))
                    LegendItem("خواب عمیق", Color(0xFFFFCA28))
                }
            }

            // 3. HORIZONTAL SEPARATOR
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 20.dp),
                thickness = 1.dp,
                color = Color(0xFFF5F5F5)
            )

            // 4. SUMMARY TEXT (Aligned End/Right)
            val feedbackMessage = when {
                score >= 85 -> "زمان خوابت خیلی خوب بوده و امتیاز خواب دیشب شده ${
                    score.toString().toFarsiDigits()
                }، همینطوری ادامه بده"

                score >= 70 -> "خوابت خوب بوده. سعی کن ساعت خواب منظم‌تری داشته باشی."
                score >= 50 -> "کیفیت خواب متوسط بوده. استراحت بیشتری نیاز داری."
                score > 0 -> "کیفیت خوابت پایین بوده. سعی کن زودتر بخوابی."
                else -> "داده‌ای برای نمایش وجود ندارد."
            }

            Text(
                text = feedbackMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                lineHeight = 22.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}