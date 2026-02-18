package com.bonyad.healthplat.ui.dashboard.details.sleep


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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.ui.dashboard.details.CustomDetailTopBar
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
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF9747FF))
                    }
                } else {
                    SleepTimelineChart(
                        data = timelineData,
                        showEmptyState = timelineData.isEmpty()
                    )
                }
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
    isLoading: Boolean = false,
    showEmptyState: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        // Loading state
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF4FA8A6))
            }
            return@Card
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .padding(16.dp)
        ) {
            // Same Row { Y-axis labels | Chart } pattern as HeartRate
            Row(modifier = Modifier.fillMaxSize()) {

                // Y-axis — 4 stage names, top to bottom: بیدار / سبک / عمیق / REM
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 8.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("بیدار", fontSize = 10.sp, color = Color(0xFF6B6B6B))
                    Text("سبک",  fontSize = 10.sp, color = Color(0xFF6B6B6B))
                    Text("عمیق", fontSize = 10.sp, color = Color(0xFF6B6B6B))
                    Text("REM",  fontSize = 10.sp, color = Color(0xFF6B6B6B))
                }

                // Chart area
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 20.dp) // space for X-axis labels
                    ) {
                        val w = size.width
                        val h = size.height
                        val rowHeight = h / 4f  // 4 stages now

                        // Vertical grid lines
                        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        listOf(0.25f, 0.5f, 0.75f).forEach { r ->
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.5f),
                                start = Offset(w * r, 0f),
                                end = Offset(w * r, h),
                                pathEffect = pathEffect
                            )
                        }

                        // Horizontal dividers between 4 rows
                        listOf(1f, 2f, 3f).forEach { row ->
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                start = Offset(0f, rowHeight * row),
                                end = Offset(w, rowHeight * row),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Sleep segments — AWAKE=0, LIGHT=1, DEEP=2, REM=3
                        if (data.isNotEmpty()) {
                            data.forEach { segment ->
                                val x = segment.startRatio * w
                                val segWidth = (segment.widthRatio * w).coerceAtLeast(4f)

                                val (color, rowIndex) = when (segment.stage) {
                                    SleepDetailViewModel.SleepStage.AWAKE -> Color.Transparent to 0  // no bars for awake
                                    SleepDetailViewModel.SleepStage.LIGHT -> Color(0xFF66BB6A) to 1
                                    SleepDetailViewModel.SleepStage.DEEP  -> Color(0xFF4FC3F7) to 2
                                    SleepDetailViewModel.SleepStage.REM   -> Color(0xFFAB47BC) to 3
                                    else -> Color.Transparent to 0
                                }

                                if (color != Color.Transparent) {
                                    val yTop = rowIndex * rowHeight
                                    drawRoundRect(
                                        color = color,
                                        topLeft = Offset(x, yTop + 4f),
                                        size = Size(segWidth, rowHeight - 8f),
                                        cornerRadius = CornerRadius(4f, 4f)
                                    )
                                }
                            }
                        }
                    }

                    // X-axis labels — LTR
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("۲۱:۰۰", fontSize = 10.sp, color = Color.Gray)
                            Text("۰۰:۰۰", fontSize = 10.sp, color = Color.Gray)
                            Text("۰۳:۰۰", fontSize = 10.sp, color = Color.Gray)
                            Text("۰۶:۰۰", fontSize = 10.sp, color = Color.Gray)
                            Text("۰۹:۰۰", fontSize = 10.sp, color = Color.Gray)
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
                                "داده خوابی موجود نیست",
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
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
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
                                drawArc(
                                    Color(0xFFAB47BC),
                                    startAngle,
                                    sweepREM,
                                    false,
                                    style = capStyle
                                )
                                startAngle += sweepREM
                            }

                            // Light - Green
                            val sweepLight = 360f * (percentages.second / 100f)
                            if (sweepLight > 0) {
                                drawArc(
                                    Color(0xFF66BB6A),
                                    startAngle,
                                    sweepLight,
                                    false,
                                    style = capStyle
                                )
                                startAngle += sweepLight
                            }

                            // Deep - Yellow/Amber
                            val sweepDeep = 360f * (percentages.first / 100f)
                            if (sweepDeep > 0) {
                                drawArc(
                                    Color(0xFFFFCA28),
                                    startAngle,
                                    sweepDeep,
                                    false,
                                    style = capStyle
                                )
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