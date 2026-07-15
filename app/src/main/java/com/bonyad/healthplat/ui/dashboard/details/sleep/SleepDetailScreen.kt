package com.bonyad.healthplat.ui.dashboard.details.sleep

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    val selectedDate by viewModel.selectedDate.collectAsState()
    val analysisState by viewModel.analysisState.collectAsState()

    var showDatePicker by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val todayPersian = androidx.compose.runtime.remember {
        val (jy, jm, jd) = com.bonyad.healthplat.ui.utils.PersianDateUtils.getCurrentJalaliDate()
        com.bonyad.healthplat.ui.components.PersianDate(jy, jm, jd)
    }
    val selectedPersianDate = androidx.compose.runtime.remember(selectedDate) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Time range selector
            TimeRangeSelector(
                selected = selectedRange,
                onSelect = { viewModel.setTimeRange(it) },
                onCalendarClick = { showDatePicker = true }
            )

            // 2. Date strip (daily only)
            if (selectedRange == "روزانه") {
                DateStrip(
                    selectedDate = selectedDate,
                    onDaySelected = { date -> viewModel.selectDay(date) }
                )
            }

            // 3. Sleep time header
            if (analysisState.hasData && analysisState.sessions.isNotEmpty()) {
                // New: show session start/end times
                SleepSessionTimesHeader(
                    sessions = analysisState.sessions,
                    selectedIndex = analysisState.selectedSessionIndex,
                    onSessionSelected = { viewModel.selectSession(it) },
                    dateLabel = dateLabel
                )
            } else {
                // Legacy: show total hours/minutes
                LegacySleepTimeHeader(
                    stats = stats,
                    selectedRange = selectedRange,
                    dateLabel = dateLabel
                )
            }

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
                // 4. Quality donut card
                SleepQualityCard(
                    score = stats.score,
                    percentages = percentages,
                    qualityLabel = viewModel.getSleepQualityLabel()
                )

                // === New sections (only when analysis API data is available) ===
                if (analysisState.hasData) {
                    // 5. Sleep details card
                    SleepDetailsCard(
                        totalDuration = analysisState.totalDuration,
                        netDuration = analysisState.netDuration,
                        efficiency = analysisState.efficiency,
                        restfulnessLabel = analysisState.restfulnessLabel,
                        remDuration = analysisState.remDuration,
                        deepDuration = analysisState.deepDuration,
                        latency = analysisState.latency
                    )

                    // 6. Key metrics grid
                    KeyMetricsGrid(
                        avgHeartRate = analysisState.avgHeartRate,
                        netDuration = analysisState.netDuration,
                        avgSpO2 = analysisState.avgSpO2,
                        efficiency = analysisState.efficiency
                    )

                    // 7. Sleep debt card
                    SleepDebtCard(
                        debtMinutes = analysisState.sleepDebtMinutes
                    )
                }

                // 8. Timeline chart (daily only)
                if (selectedRange == "روزانه") {
                    LegacySleepTimelineChart(
                        data = timelineData,
                        showEmptyState = timelineData.isEmpty(),
                        xLabels = if (analysisState.hasData) analysisState.timelineXLabels else emptyList()
                    )
                }

                // === More new sections ===
                if (analysisState.hasData) {
                    // 9. Breathing regularity
                    BreathingRegularityCard(
                        label = analysisState.breathingLabel,
                        description = analysisState.breathingDescription
                    )

                    // 10. Heart rate during sleep (range bar chart)
                    if (analysisState.hrChartData.isNotEmpty()) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = "ضربان قلب در خواب",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.Black,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            SleepHeartRateRangeChart(
                                data = analysisState.hrChartData,
                                xLabels = analysisState.hrTimestamps
                            )
                        }
                    }

                    // 11. HRV during sleep (area chart)
                    if (analysisState.hrvChartData.isNotEmpty()) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = "HRV در خواب",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.Black,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            SleepHrvAreaChart(
                                data = analysisState.hrvChartData,
                                xLabels = analysisState.hrvTimestamps
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

// ============ Sleep Session Times Header (New) ============

@Composable
fun SleepSessionTimesHeader(
    sessions: List<SleepDetailViewModel.SleepSession>,
    selectedIndex: Int,
    onSessionSelected: (Int) -> Unit,
    dateLabel: String
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "زمان خواب",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B6B6B),
            modifier = Modifier.align(Alignment.End)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Session chips — wraps to next line when > 2 sessions
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sessions.reversed().forEachIndexed { reversedIdx, session ->
                val actualIdx = sessions.size - 1 - reversedIdx
                val isSelected = actualIdx == selectedIndex
                val timeRange = "${session.endTime.toFarsiDigits()} - ${session.startTime.toFarsiDigits()}"
                val label = if (session.type == 1) "خواب" else "چرت"

                Card(
                    onClick = { onSessionSelected(actualIdx) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF5BA3A3) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF5BA3A3) else Color(0xFF6B6B6B)
                    )
                ) {
                    Text(
                        text = "$label  $timeRange",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        ),
                        color = if (isSelected) Color.White else Color(0xFF6B6B6B),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        maxLines = 1
                    )
                }
            }
        }

        // Date label
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

// ============ Legacy Sleep Time Header ============

@Composable
fun LegacySleepTimeHeader(
    stats: SleepDetailViewModel.SleepStats,
    selectedRange: String,
    dateLabel: String
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = when (selectedRange) {
                "هفتگی", "ماهانه" -> "میانگین زمان خواب"
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
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

// ============ Sleep Timeline Chart ============

@Composable
fun SleepTimelineChart(
    data: List<SleepDetailViewModel.SleepSegment>,
    isLoading: Boolean = false,
    showEmptyState: Boolean = false,
    xLabels: List<String> = emptyList()
) {
    // Stage Y-position mapping (adjusted slightly so the top and bottom don't clip)
    val stageYPosition = { stage: SleepDetailViewModel.SleepStage, h: Float ->
        when (stage) {
            SleepDetailViewModel.SleepStage.AWAKE -> h * 0.05f   // Near top
            SleepDetailViewModel.SleepStage.REM   -> h * 0.35f   // Upper middle
            SleepDetailViewModel.SleepStage.LIGHT -> h * 0.65f   // Lower middle
            SleepDetailViewModel.SleepStage.DEEP  -> h * 0.95f   // Near bottom
        }
    }

    // Color mapping to match your Legend
    val stageColor = { stage: SleepDetailViewModel.SleepStage ->
        when (stage) {
            SleepDetailViewModel.SleepStage.AWAKE -> Color(0xFFA1A1A1) // Gray
            SleepDetailViewModel.SleepStage.REM   -> Color(0xFF96D1F5) // Light Blue
            SleepDetailViewModel.SleepStage.LIGHT -> Color(0xFF51AEE2) // Blue
            SleepDetailViewModel.SleepStage.DEEP  -> Color(0xFF2B6D88) // Dark Blue
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
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

        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                    Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 20.dp)
                        ) {
                            val w = size.width
                            val h = size.height

                            // Horizontal grid lines matching the Y positions roughly
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            listOf(0.35f, 0.65f, 0.95f).forEach { ratio ->
                                drawLine(
                                    color = Color.LightGray.copy(alpha = 0.3f),
                                    start = Offset(0f, h * ratio),
                                    end = Offset(w, h * ratio),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = dashEffect
                                )
                            }

                            if (data.isNotEmpty()) {
                                data.forEach { segment ->
                                    val startX = segment.startRatio * w
                                    val segmentW = (segment.widthRatio * w).coerceAtLeast(4f)
                                    val y = stageYPosition(segment.stage, h)
                                    val color = stageColor(segment.stage)

                                    // Filled area from stage Y down to chart bottom
                                    drawRect(
                                        color = color.copy(alpha = 0.5f),
                                        topLeft = Offset(startX, y),
                                        size = Size(segmentW, h - y)
                                    )

                                    // Top edge line for definition
                                    drawLine(
                                        color = color,
                                        start = Offset(startX, y),
                                        end = Offset(startX + segmentW, y),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                }
                            }
                        }

                        // X-axis labels
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (xLabels.isNotEmpty()) {
                                    xLabels.forEach { label ->
                                        Text(label, fontSize = 12.sp, color = Color.Gray)
                                    }
                                } else {
                                    Text("۲۱:۰۰", fontSize = 12.sp, color = Color.Gray)
                                    Text("۰۰:۰۰", fontSize = 12.sp, color = Color.Gray)
                                    Text("۰۳:۰۰", fontSize = 12.sp, color = Color.Gray)
                                    Text("۰۶:۰۰", fontSize = 12.sp, color = Color.Gray)
                                    Text("۰۹:۰۰", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }

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

            // Legend row
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem("عمیق", Color(0xFF2B6D88))
                LegendItem("سبک", Color(0xFF51AEE2))
                LegendItem("REM", Color(0xFF96D1F5))
                LegendItem("بیدار", Color(0xFFA1A1A1))
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
            modifier = Modifier.padding(end = 6.dp)
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
    }
}

// ============ Sleep Quality Card ============

@Composable
fun SleepQualityCard(
    score: Int,
    percentages: Triple<Int, Int, Int>,
    qualityLabel: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
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
                // Donut chart with labels
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(145.dp)
                ) {
                    val deepPct = percentages.first
                    val lightPct = percentages.second
                    val remPct = percentages.third
                    val total = deepPct + lightPct + remPct

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        val strokeWidth = 24.dp.toPx()

                        drawArc(
                            color = Color(0xFFF2F2F2),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )

                        if (total > 0) {
                            val capStyle = Stroke(width = strokeWidth, cap = StrokeCap.Butt)

                            var startAngle = -90f
                            val hasRem = remPct > 0
                            val hasLight = lightPct > 0
                            val hasDeep = deepPct > 0

                            if (hasRem) {
                                val isLast = !hasLight && !hasDeep
                                val sweep = if (isLast) 270f - startAngle else 360f * (remPct / 100f)
                                drawArc(Color(0xFF95D0F1), startAngle, sweep, false, style = capStyle)
                                startAngle += sweep
                            }

                            if (hasLight) {
                                val isLast = !hasDeep
                                val sweep = if (isLast) 270f - startAngle else 360f * (lightPct / 100f)
                                drawArc(Color(0xFF51B2E7), startAngle, sweep, false, style = capStyle)
                                startAngle += sweep
                            }

                            if (hasDeep) {
                                val sweep = 270f - startAngle
                                drawArc(Color(0xFF2B6C88), startAngle, sweep, false, style = capStyle)
                            }
                        }
                    }

                    // Center score
                    Text(
                        text = if (score > 0) score.toString().toFarsiDigits() else "-",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 24.sp,
                            color = Color.Black
                        )
                    )

                    // Percentage labels on each segment
                    if (total > 0) {
                        val radius = 48.dp
                        var angle = -90f

                        // REM label
                        if (remPct > 0) {
                            val midAngle = angle + (360f * (remPct / 100f)) / 2f
                            val radians = Math.toRadians(midAngle.toDouble())
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = (radius.value * kotlin.math.cos(radians)).dp,
                                        y = (radius.value * kotlin.math.sin(radians)).dp
                                    )
                                    .background(
                                        Color(0x73F3F3F3),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "%${remPct.toString().toFarsiDigits()}",
                                    fontSize = 14.sp,
                                    color = Color(0xFF6B6B6B)
                                )
                            }
                            angle += 360f * (remPct / 100f)
                        }

                        // Light label
                        if (lightPct > 0) {
                            val midAngle = angle + (360f * (lightPct / 100f)) / 2f
                            val radians = Math.toRadians(midAngle.toDouble())
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = (radius.value * kotlin.math.cos(radians)).dp,
                                        y = (radius.value * kotlin.math.sin(radians)).dp
                                    )
                                    .background(
                                        Color(0x73F3F3F3),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "%${lightPct.toString().toFarsiDigits()}",
                                    fontSize = 14.sp,
                                    color = Color(0xFF6B6B6B)
                                )
                            }
                            angle += 360f * (lightPct / 100f)
                        }

                        // Deep label
                        if (deepPct > 0) {
                            val midAngle = angle + (360f * (deepPct / 100f)) / 2f
                            val radians = Math.toRadians(midAngle.toDouble())
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = (radius.value * kotlin.math.cos(radians)).dp,
                                        y = (radius.value * kotlin.math.sin(radians)).dp
                                    )
                                    .background(
                                        Color(0x73F3F3F3),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "%${deepPct.toString().toFarsiDigits()}",
                                    fontSize = 14.sp,
                                    color = Color(0xFF6B6B6B)
                                )
                            }
                        }
                    }
                }

                // Status & Legend
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = qualityLabel,
                        fontSize = 20.sp,
                        color = Color(0xFF6B6B6B),
                        textAlign = TextAlign.End
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LegendItem("REM", Color(0xFF95D0F1))
                    LegendItem("خواب سبک", Color(0xFF51B2E7))
                    LegendItem("خواب عمیق", Color(0xFF2B6C88))
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 20.dp),
                thickness = 1.dp,
                color = Color(0xFFF5F5F5)
            )

            val feedbackMessage = when {
                score >= 85 -> "زمان خوابت خیلی خوب بوده و امتیاز خواب دیشب شده ${
                    score.toString().toFarsiDigits()
                }، همینطوری ادامه بده"
                score >= 70 -> "خوابت خوب بوده. سعی کن ساعت خواب منظم‌تری داشته باشی.".rtl()
                score >= 50 -> "کیفیت خواب متوسط بوده. استراحت بیشتری نیاز داری.".rtl()
                score > 0 -> "کیفیت خوابت پایین بوده. سعی کن زودتر بخوابی.".rtl()
                else -> "داده‌ای برای نمایش وجود ندارد.".rtl()
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

// ============ Sleep Details Card (NEW) ============

@Composable
fun SleepDetailsCard(
    totalDuration: Int,
    netDuration: Int,
    efficiency: Int,
    restfulnessLabel: String,
    remDuration: Int,
    deepDuration: Int,
    latency: Int
) {
    val maxDuration = totalDuration.coerceAtLeast(1)

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "جزئیات خواب",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = TextAlign.End
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, Color(0xFFECEAE6))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DetailRow("مجموع خواب", formatDuration(totalDuration), (totalDuration / 480f).coerceIn(0f, 1f))
                DetailRow("خواب خالص", formatDuration(netDuration), (netDuration.toFloat() / maxDuration).coerceIn(0f, 1f))
                DetailRow("کارایی خواب", "${efficiency.toString().toFarsiDigits()}%", (efficiency / 100f).coerceIn(0f, 1f))
                DetailRow("آرامش خواب", restfulnessLabel, restfulnessToProgress(restfulnessLabel))
                DetailRow("خواب REM", formatDuration(remDuration), (remDuration.toFloat() / maxDuration).coerceIn(0f, 1f))
                DetailRow("خواب عمیق", formatDuration(deepDuration), (deepDuration.toFloat() / maxDuration).coerceIn(0f, 1f))
                DetailRow("تاخیر زمانی خواب", formatDuration(latency), (latency / 30f).coerceIn(0f, 1f))
            }
        }
    }
}

private fun restfulnessToProgress(label: String): Float = when (label) {
    "عالی" -> 1f
    "خوب" -> 0.75f
    "متوسط" -> 0.5f
    else -> 0.25f
}

@Composable
private fun DetailRow(label: String, value: String, progress: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                fontSize = 12.sp,
                color = Color(0xFF6B6B6B)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color(0xFF6B6B6B)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFFECEAE6), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(Color(0xFF5BA3A3), RoundedCornerShape(12.dp))
            )
        }
    }
}

private fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) {
        "\u200F${h.toString().toFarsiDigits()} ساعت و ${m.toString().toFarsiDigits()} دقیقه"
    } else {
        "\u200F${m.toString().toFarsiDigits()} دقیقه"
    }
}

// ============ Key Metrics Grid (NEW) ============

@Composable
fun KeyMetricsGrid(
    avgHeartRate: Int,
    netDuration: Int,
    avgSpO2: Int,
    efficiency: Int
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "موارد کلیدی",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = TextAlign.End
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KeyMetricItem(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.sleep,
                iconTint = Color(0xFF9747FF),
                value = formatDuration(netDuration),
                label = "خواب خالص"
            )

            KeyMetricItem(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.heart_rate,
                iconTint = Color(0xFFFF6B6B),
                value = "${avgHeartRate.toString().toFarsiDigits()} bpm",
                label = "میانگین ضربان قلب"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KeyMetricItem(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.star_effect,
                iconTint = Color(0xFF58BD83),
                value = "${efficiency.toString().toFarsiDigits()}%",
                label = "کارایی خواب"
            )

            KeyMetricItem(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.hospital,
                iconTint = Color(0xFF56CCF2),
                value = "${avgSpO2.toString().toFarsiDigits()}%",
                label = "اکسیژن خون"
            )
        }
    }
}

@Composable
fun KeyMetricItem(
    modifier: Modifier = Modifier,
    iconRes: Int,
    iconTint: Color,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFECEAE6))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top row: icon (left) + title (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B6B6B),
                    fontSize = 11.sp,
                    textAlign = TextAlign.End
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Bottom: value (right-aligned)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

// ============ Sleep Debt Card (NEW - Placeholder) ============

@Composable
fun SleepDebtCard(debtMinutes: Int) {
    // Map debt minutes to progress across 4 segments:
    // Dot 1 (0%): 0 min, Dot 2 (33%): 7h (420min), Dot 3 (66%): 14h (840min), Dot 4 (100%): 14h+
    val progress = when {
        debtMinutes <= 0 -> 0f
        debtMinutes <= 420 -> (debtMinutes / 420f) * 0.333f
        debtMinutes <= 840 -> 0.333f + ((debtMinutes - 420) / 420f) * 0.333f
        else -> 0.667f + ((debtMinutes - 840).coerceAtMost(420) / 420f) * 0.333f
    }.coerceIn(0f, 1f)

    val debtLabel = if (debtMinutes > 0) {
        val h = debtMinutes / 60
        val m = debtMinutes % 60
        if (h > 0) {
            "\u200F${h.toString().toFarsiDigits()} ساعت و ${m.toString().toFarsiDigits()} دقیقه"
        } else {
            "\u200F${m.toString().toFarsiDigits()} دقیقه"
        }
    } else {
        "بدون بدهی"
    }

    val trackColor = Color(0xFFECEAE6)
    val fillColor = Color(0xFF5BA3A3)

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Title outside card
        Text(
            text = "بدهی خواب",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = TextAlign.End
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, Color(0xFFECEAE6))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Top row: "۷ روز گذشته" on right, value on left (RTL layout)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = debtLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0XFF6B6B6B),
                        fontSize = 14.sp
                    )

                    Text(
                        text = "هفته جاری",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress bar with 4 dots (RTL: fill from right toward left)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                ) {
                    // Track background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .align(Alignment.Center)
                            .background(trackColor, RoundedCornerShape(12.dp))
                    )

                    // Filled portion — CenterEnd in RTL fills from right
                    if (progress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(8.dp)
                                .align(Alignment.CenterEnd)
                                .background(fillColor, RoundedCornerShape(12.dp))
                        )
                    }

                    // 4 dots on the track
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val centerY = size.height / 2f
                        val dotRadius = 5.dp.toPx()
                        val borderWidth = 1.5.dp.toPx()

                        // Dot positions in Canvas coords (LTR: x=0 is left of screen)
                        // In RTL layout with CenterEnd, fill starts from x=w (right) going left
                        // Dots: x=w → 0 debt (هیچ), x=0 → max debt (بالا)
                        val dotCanvasPositions = listOf(1f, 0.667f, 0.333f, 0f)

                        dotCanvasPositions.forEach { canvasPos ->
                            val x = canvasPos * w
                            // Dot is filled if the fill (from right) has reached it
                            val isFilled = x >= w * (1f - progress)

                            drawCircle(
                                color = Color.White,
                                radius = dotRadius + borderWidth,
                                center = Offset(
                                    x.coerceIn(dotRadius + borderWidth, w - dotRadius - borderWidth),
                                    centerY
                                )
                            )
                            drawCircle(
                                color = if (isFilled) fillColor else trackColor,
                                radius = dotRadius,
                                center = Offset(
                                    x.coerceIn(dotRadius + borderWidth, w - dotRadius - borderWidth),
                                    centerY
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Labels: هیچ (right/start in RTL) — بالا (left/end in RTL)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "بالا", fontSize = 12.sp, color = Color.Gray)
                    Text(text = "هیچ", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

// ============ Breathing Regularity Card (NEW) ============

@Composable
fun BreathingRegularityCard(
    label: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFECEAE6))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Top row: icon (left) + title (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.check_badge),
                    contentDescription = null,
                    tint = Color(0xFF58BD83),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "منظم بودن تنفس",
                    fontSize = 14.sp,
                    color = Color(0xFF6B6B6B),
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Label below title (right-aligned)
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF6B6B6B),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                lineHeight = 22.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============ Sleep Heart Rate Range Chart ============

/**
 * Capsule/dot range bar chart for sleep heart rate data.
 * Same visual style as HeartRateRangeChart: fixed Y-axis 50-150, dashed grid, red bars.
 * Groups raw per-minute HR data into equal-sized slots and draws min/max bars.
 */
@Composable
fun SleepHeartRateRangeChart(
    data: List<Int>,
    xLabels: List<String> = emptyList()
) {
    val slotCount = 24
    // Group data into equal-sized slots, compute min/max per slot
    val validData = data.filter { it > 0 }
    val isEmpty = validData.isEmpty()

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
                // Y-Axis Labels
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 8.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("۱۰۰", fontSize = 12.sp, color = Color.Gray)
                    Text("۶۵", fontSize = 12.sp, color = Color.Gray)
                    Text("۳۰", fontSize = 12.sp, color = Color.Gray)
                }

                // Chart Content
                Box(modifier = Modifier.weight(1f)) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 20.dp)
                    ) {
                        val width = size.width
                        val height = size.height

                        val yMin = 30f
                        val yMax = 100f
                        val yRange = yMax - yMin

                        // Dashed horizontal grid lines
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

                        // Dashed vertical grid lines
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

                        // Group into slots
                        val slotSize = (data.size + slotCount - 1) / slotCount
                        val barWidth = 3.dp.toPx()
                        val barColor = Color(0xFFFF6B6B)

                        for (slotIdx in 0 until slotCount) {
                            val start = slotIdx * slotSize
                            if (start >= data.size) continue
                            val end = minOf(start + slotSize, data.size)
                            val slotValues = data.subList(start, end).filter { it > 0 }
                            if (slotValues.isEmpty()) continue

                            val slotMin = slotValues.min()
                            val slotMax = slotValues.max()

                            val padding = barWidth / 2 + 4.dp.toPx()
                            val usable = width - padding * 2
                            val x = if (slotCount > 1) {
                                padding + (slotIdx.toFloat() / (slotCount - 1)) * usable
                            } else {
                                width / 2f
                            }

                            val safeMin = slotMin.toFloat().coerceIn(yMin, yMax)
                            val safeMax = slotMax.toFloat().coerceIn(yMin, yMax)

                            val yTop = height - ((safeMax - yMin) / yRange) * height
                            val yBottom = height - ((safeMin - yMin) / yRange) * height
                            val calculatedHeight = yBottom - yTop

                            if (calculatedHeight < barWidth) {
                                // Draw as dot
                                drawCircle(
                                    color = barColor,
                                    radius = 2.dp.toPx(),
                                    center = Offset(x, yTop + calculatedHeight / 2)
                                )
                            } else {
                                // Draw as capsule
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(x - barWidth / 2, yTop),
                                    size = Size(barWidth, calculatedHeight),
                                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                                )
                            }
                        }
                    }

                    // X-Axis Labels
                    if (xLabels.isNotEmpty()) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                xLabels.forEach { label ->
                                    Text(label, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            // Empty State
            if (isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("داده‌ای موجود نیست", color = Color(0xFF6B6B6B))
                }
            }
        }
    }
}

// ============ Sleep HRV Area Chart ============

/**
 * Smooth cubic bezier area chart with red gradient fill for sleep HRV data.
 * Same visual style as HrvAreaChart: fixed Y-axis 0-100, dashed vertical grid.
 */
@Composable
fun SleepHrvAreaChart(
    data: List<Int>,
    xLabels: List<String> = emptyList()
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
        ) {
            // Y-axis labels
            Column(
                modifier = Modifier
                    .width(28.dp)
                    .fillMaxHeight()
                    .padding(end = 4.dp, bottom = 20.dp),
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

                // Dashed vertical grid lines
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

                // Deduplicate consecutive identical values — the sleep API repeats
                // each 30-min HRV reading per minute, causing a staircase appearance.
                // Keep only the transition points (where value changes) with their
                // original positions so the cubicTo curves produce smooth lines.
                val deduped = mutableListOf<Pair<Int, Int>>() // (index, value)
                var prevValue = Int.MIN_VALUE
                data.forEachIndexed { index, value ->
                    if (value > 0 && value != prevValue) {
                        deduped.add(index to value)
                        prevValue = value
                    }
                }

                val drawPoints = mutableListOf<Pair<Float, Float>>()
                val totalSlots = data.size.coerceAtLeast(1)

                deduped.forEach { (index, value) ->
                    val x = if (totalSlots > 1) (index.toFloat() / (totalSlots - 1)) * width else width / 2f
                    val clamped = value.coerceIn(yMin.toInt(), yMax.toInt())
                    val y = height - ((clamped - yMin) / yRange) * height
                    drawPoints.add(x to y)
                }

                if (drawPoints.isEmpty()) return@Canvas

                // Build smooth bezier paths
                val path = Path()
                val fillPath = Path()

                drawPoints.forEachIndexed { index, (x, y) ->
                    if (index == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, height)
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
                    style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // X-axis labels
            if (xLabels.isNotEmpty()) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(start = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        xLabels.forEach { label ->
                            Text(label, fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// ============ Legacy Sleep Timeline Chart (backup — not wired up) ============

@Composable
fun LegacySleepTimelineChart(
    data: List<SleepDetailViewModel.SleepSegment>,
    showEmptyState: Boolean = false,
    xLabels: List<String> = emptyList()
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 20.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val rowHeight = h / 4

                        // Horizontal dashed grid lines between rows
                        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        listOf(1f, 2f, 3f).forEach { i ->
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.4f),
                                start = Offset(0f, rowHeight * i),
                                end = Offset(w, rowHeight * i),
                                pathEffect = pathEffect
                            )
                        }

                        if (data.isNotEmpty()) {
                            fun stageInfo(stage: SleepDetailViewModel.SleepStage): Pair<Color, Float> =
                                when (stage) {
                                    SleepDetailViewModel.SleepStage.AWAKE -> Color(0xFFA1A1A1) to (rowHeight * 0)
                                    SleepDetailViewModel.SleepStage.LIGHT -> Color(0xFF51AEE2) to (rowHeight * 1)
                                    SleepDetailViewModel.SleepStage.DEEP -> Color(0xFF2B6D88) to (rowHeight * 2)
                                    SleepDetailViewModel.SleepStage.REM -> Color(0xFF96D1F5) to (rowHeight * 3)
                                }

                            // 1. Draw Connecting Lines from center of each block to center of the next
                            for (i in 0 until data.size - 1) {
                                val current = data[i]
                                val next = data[i + 1]

                                val currentStartX = current.startRatio * w
                                val currentWidth = (current.widthRatio * w).coerceAtLeast(12f)
                                val currentEndX = currentStartX + currentWidth

                                val nextStartX = next.startRatio * w

                                val (currentColor, currentYTop) = stageInfo(current.stage)
                                val (_, nextYTop) = stageInfo(next.stage)

                                // Connect from vertical center of current block to vertical center of next
                                val currentCenterY = currentYTop + rowHeight / 2
                                val nextCenterY = nextYTop + rowHeight / 2

                                val startOffset = Offset(currentEndX, currentCenterY)
                                val endOffset = Offset(nextStartX, nextCenterY)

                                drawLine(
                                    color = currentColor,
                                    start = startOffset,
                                    end = endOffset,
                                    strokeWidth = 1.dp.toPx(),
                                    cap = StrokeCap.Round
                                )

                            }

                            // 2. Draw the blocks over the lines
                            for (i in data.indices) {
                                val segment = data[i]
                                val x = segment.startRatio * w
                                val segWidth = (segment.widthRatio * w).coerceAtLeast(12f)
                                val (color, rowTop) = stageInfo(segment.stage)

                                drawRoundRect(
                                    color = color,
                                    topLeft = Offset(x, rowTop),
                                    size = Size(segWidth, rowHeight),
                                    cornerRadius = CornerRadius(8f, 8f)
                                )
                            }
                        }
                    }

                    // X-axis labels (forced LTR so time flows chronologically)
                    if (xLabels.isNotEmpty()) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                xLabels.forEach { label ->
                                    Text(label, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }

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
            }

            // Legend row
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem("عمیق", Color(0xFF2B6D88))
                LegendItem("سبک", Color(0xFF51AEE2))
                LegendItem("REM", Color(0xFF96D1F5))
                LegendItem("بیدار", Color(0xFFA1A1A1))
            }
        }
    }
}

@Composable
fun ConnectedSleepTimelineChart(
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
            // Optional Header
            Text("مراحل خواب", fontSize = 14.sp, color = Color.DarkGray)

            HorizontalDivider(
                thickness = 0.5.dp,
                color = Color.LightGray.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 20.dp)
                        ) {
                            val w = size.width
                            val h = size.height

                            // Divide canvas into 4 rows for the 4 stages
                            val rowHeight = h / 4

                            // Background dashed grid lines
                            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            listOf(1f, 2f, 3f).forEach { i ->
                                drawLine(
                                    color = Color.LightGray.copy(alpha = 0.4f),
                                    start = Offset(0f, rowHeight * i),
                                    end = Offset(w, rowHeight * i),
                                    pathEffect = pathEffect
                                )
                            }

                            if (data.isNotEmpty()) {
                                var prevX: Float? = null
                                var prevY: Float? = null
                                var prevColor: Color? = null

                                // Make the line thick enough to look like a solid block
                                val strokeWidthPx = 4.dp.toPx()

                                data.forEach { segment ->
                                    val startX = segment.startRatio * w
                                    val segWidth = (segment.widthRatio * w).coerceAtLeast(2f)
                                    val endX = startX + segWidth

                                    // Map each stage to its color and exact vertical center
                                    val (stageColor, yCenter) = when (segment.stage) {
                                        SleepDetailViewModel.SleepStage.AWAKE -> Color(0xFFA1A1A1) to (rowHeight * 0.5f)
                                        SleepDetailViewModel.SleepStage.REM -> Color(0xFF96D1F5) to (rowHeight * 1.5f)
                                        SleepDetailViewModel.SleepStage.LIGHT -> Color(0xFF51AEE2) to (rowHeight * 2.5f)
                                        SleepDetailViewModel.SleepStage.DEEP -> Color(0xFF2B6D88) to (rowHeight * 3.5f)
                                    }

                                    // 1. Draw the Vertical Connecting Line First (if not the first segment)
                                    if (prevX != null && prevY != null && prevColor != null) {
                                        drawLine(
                                            brush = Brush.verticalGradient(
                                                // Ensure the gradient direction matches the drop/rise of the line
                                                colors = if (prevY!! < yCenter) listOf(prevColor!!, stageColor) else listOf(stageColor, prevColor!!),
                                                startY = minOf(prevY!!, yCenter),
                                                endY = maxOf(prevY!!, yCenter)
                                            ),
                                            start = Offset(startX, prevY!!),
                                            end = Offset(startX, yCenter),
                                            strokeWidth = strokeWidthPx,
                                            cap = StrokeCap.Round
                                        )
                                    }

                                    // 2. Draw the Horizontal Line for the current stage
                                    drawLine(
                                        color = stageColor,
                                        start = Offset(startX, yCenter),
                                        end = Offset(endX, yCenter),
                                        strokeWidth = strokeWidthPx,
                                        cap = StrokeCap.Round
                                    )

                                    // 3. Draw a semi-transparent fill underneath to give it depth (Optional)
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(stageColor.copy(alpha = 0.25f), Color.Transparent),
                                            startY = yCenter,
                                            endY = h
                                        ),
                                        topLeft = Offset(startX, yCenter),
                                        size = Size(segWidth, h - yCenter)
                                    )

                                    prevX = endX
                                    prevY = yCenter
                                    prevColor = stageColor
                                }
                            }
                        }

                        // X-axis labels (forced LTR so time flows chronologically)
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("۲۱:۰۰", fontSize = 12.sp, color = Color.Gray)
                                Text("۰۰:۰۰", fontSize = 12.sp, color = Color.Gray)
                                Text("۰۳:۰۰", fontSize = 12.sp, color = Color.Gray)
                                Text("۰۶:۰۰", fontSize = 12.sp, color = Color.Gray)
                                Text("۰۹:۰۰", fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        if (showEmptyState) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White.copy(alpha = 0.8f)),
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
            }

            // Legend row
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem("عمیق", Color(0xFF2B6D88))
                LegendItem("سبک", Color(0xFF51AEE2))
                LegendItem("REM", Color(0xFF96D1F5))
                LegendItem("بیدار", Color(0xFFA1A1A1))
            }
        }
    }
}
