package com.bonyad.healthplat.ui.dashboard.details.arrhythmia

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
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.ui.dashboard.details.CustomDetailTopBar
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.DateStrip
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.TimeRangeSelector
import com.bonyad.healthplat.ui.utils.rtl
import com.bonyad.healthplat.ui.utils.toFarsiDigits

@Composable
fun ArrhythmiaDetailScreen(
    viewModel: ArrhythmiaDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onInfoClick: () -> Unit = {}
) {
    val points by viewModel.chartPoints.collectAsState()
    val pointsOthers by viewModel.chartPointsOthers.collectAsState()
    val barChartData by viewModel.barChartData.collectAsState()
    val distribution by viewModel.distribution.collectAsState()
    val selectedRange by viewModel.selectedTimeRange.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val dateLabel by viewModel.dateLabel.collectAsState()
    val predictionLabel by viewModel.predictionLabel.collectAsState()
    val xAxisLabels by viewModel.xAxisLabels.collectAsState()

    Scaffold(
        topBar = {
            CustomDetailTopBar(
                title = "آریتمی",
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
            // 1. Time Range Selector
            TimeRangeSelector(
                selected = selectedRange,
                onSelect = { viewModel.setTimeRange(it) }
            )

            // 2. Date Strip (daily only)
            if (selectedRange == "روزانه") {
                val selectedOffset by viewModel.selectedDayOffset.collectAsState()
                DateStrip(
                    selectedDate = java.time.LocalDate.now().minusDays(selectedOffset.toLong()),
                    onDaySelected = { date ->
                        val offset = java.time.temporal.ChronoUnit.DAYS
                            .between(date, java.time.LocalDate.now()).toInt().coerceIn(0, 6)
                        viewModel.selectDay(offset)
                    }
                )
            }

            // 3. Chart Header + Chart
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // Header (right-aligned for RTL)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "پیشبینی احتمال",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B6B6B),
                        textAlign = TextAlign.End
                    )

                    Text(
                        text = predictionLabel,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        ),
                        textAlign = TextAlign.End
                    )

                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B6B6B),
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Area Chart Card
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
                                .height(250.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFE88B8B))
                        }
                    } else {
                        if (selectedRange == "روزانه") {
                            ArrhythmiaAreaChart(
                                points = points,
                                pointsOthers = pointsOthers,
                                showEmptyState = points.isEmpty(),
                                selectedRange = selectedRange,
                                xAxisLabels = xAxisLabels
                            )
                        } else {
                            ArrhythmiaBarChart(
                                data = barChartData,
                                selectedRange = selectedRange,
                                xAxisLabels = xAxisLabels,
                                showEmptyState = barChartData.isEmpty()
                            )
                        }
                    }
                }
            }

            // 4. Donut Chart Card (daily only)
            if (selectedRange == "روزانه") {
                ArrhythmiaDistributionCard(distribution = distribution)
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Area Chart
// ═══════════════════════════════════════════════════════════════

@Composable
fun ArrhythmiaAreaChart(
    points: List<ArrhythmiaDetailViewModel.ArrhythmiaPoint>,
    pointsOthers: List<ArrhythmiaDetailViewModel.ArrhythmiaPoint> = emptyList(),
    showEmptyState: Boolean = false,
    selectedRange: String = "روزانه",
    xAxisLabels: List<String> = emptyList()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(208.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Y-axis labels
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 8.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("٪۱۰۰", fontSize = 10.sp, color = Color.Gray)
                    Text("٪۸۰", fontSize = 10.sp, color = Color.Gray)
                    Text("٪۶۰", fontSize = 10.sp, color = Color.Gray)
                    Text("٪۴۰", fontSize = 10.sp, color = Color.Gray)
                    Text("٪۲۰", fontSize = 10.sp, color = Color.Gray)
                    Text("۰", fontSize = 10.sp, color = Color.Gray)
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
                    ) {
                        val w = size.width
                        val h = size.height
                        val maxVal = 100f

                        // Horizontal grid lines
                        val pathEffect =
                            PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        listOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f).forEach { r ->
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.5f),
                                start = Offset(0f, h * r),
                                end = Offset(w, h * r),
                                pathEffect = pathEffect
                            )
                        }

                        // Vertical grid lines
                        if (selectedRange == "روزانه") {
                            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { r ->
                                drawLine(
                                    color = Color.LightGray.copy(alpha = 0.5f),
                                    start = Offset(w * r, 0f),
                                    end = Offset(w * r, h),
                                    pathEffect = pathEffect
                                )
                            }
                        } else {
                            listOf(0.33f, 0.66f).forEach { r ->
                                drawLine(
                                    color = Color.LightGray.copy(alpha = 0.5f),
                                    start = Offset(w * r, 0f),
                                    end = Offset(w * r, h),
                                    pathEffect = pathEffect
                                )
                            }
                        }

                        // AFib area curve (red)
                        if (points.any { it.value > 0f }) {
                            val linePath = Path()
                            val fillPath = Path()

                            points.forEachIndexed { i, p ->
                                val x = p.xRatio * w
                                val y = (h - (p.value / maxVal * h)).coerceIn(0f, h)

                                if (i == 0) {
                                    linePath.moveTo(x, y)
                                    fillPath.moveTo(x, h)
                                    fillPath.lineTo(x, y)
                                } else {
                                    linePath.lineTo(x, y)
                                    fillPath.lineTo(x, y)
                                }
                            }

                            val lastX = points.last().xRatio * w
                            fillPath.lineTo(lastX, h)
                            fillPath.close()

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFE88B8B).copy(alpha = 0.6f),
                                        Color(0xFFE88B8B).copy(alpha = 0.05f)
                                    )
                                )
                            )

                            drawPath(
                                path = linePath,
                                color = Color(0xFFE88B8B),
                                style = Stroke(
                                    width = 3.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            )
                        }

                        // Others area curve (green) — drawn on top
                        if (pointsOthers.any { it.value > 0f }) {
                            val linePathOthers = Path()
                            val fillPathOthers = Path()

                            pointsOthers.forEachIndexed { i, p ->
                                val x = p.xRatio * w
                                val y = (h - (p.value / maxVal * h)).coerceIn(0f, h)

                                if (i == 0) {
                                    linePathOthers.moveTo(x, y)
                                    fillPathOthers.moveTo(x, h)
                                    fillPathOthers.lineTo(x, y)
                                } else {
                                    linePathOthers.lineTo(x, y)
                                    fillPathOthers.lineTo(x, y)
                                }
                            }

                            val lastXOthers = pointsOthers.last().xRatio * w
                            fillPathOthers.lineTo(lastXOthers, h)
                            fillPathOthers.close()

                            drawPath(
                                path = fillPathOthers,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF66BB6A).copy(alpha = 0.6f),
                                        Color(0xFF66BB6A).copy(alpha = 0.05f)
                                    )
                                )
                            )

                            drawPath(
                                path = linePathOthers,
                                color = Color(0xFF66BB6A),
                                style = Stroke(
                                    width = 3.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            )
                        }
                    }

                    // X-axis labels (LTR)
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            when (selectedRange) {
                                "هفتگی", "ماهانه" -> {
                                    xAxisLabels.forEach {
                                        Text(it, fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                                else -> {
                                    if (xAxisLabels.isNotEmpty()) {
                                        xAxisLabels.forEach {
                                            Text(it, fontSize = 10.sp, color = Color.Gray)
                                        }
                                    } else {
                                        listOf("۰", "۱۰۰", "۲۰۰", "۳۰۰", "۴۰۰").forEach {
                                            Text(it, fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Empty state
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

        // Legend row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // AFib legend
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFFE88B8B), CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("AFib", fontSize = 11.sp, color = Color(0xFF6B6B6B))

            Spacer(modifier = Modifier.width(16.dp))

            // Others legend
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFF66BB6A), CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("دیگر", fontSize = 11.sp, color = Color(0xFF6B6B6B))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Bar Chart (weekly / monthly)
// ═══════════════════════════════════════════════════════════════

@Composable
fun ArrhythmiaBarChart(
    data: List<ArrhythmiaDetailViewModel.ArrhythmiaBarPoint>,
    selectedRange: String,
    xAxisLabels: List<String> = emptyList(),
    showEmptyState: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(208.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Y-axis labels
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 8.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("٪۱۰۰", fontSize = 10.sp, color = Color.Gray)
                    Text("٪۸۰", fontSize = 10.sp, color = Color.Gray)
                    Text("٪۶۰", fontSize = 10.sp, color = Color.Gray)
                    Text("٪۴۰", fontSize = 10.sp, color = Color.Gray)
                    Text("٪۲۰", fontSize = 10.sp, color = Color.Gray)
                    Text("۰", fontSize = 10.sp, color = Color.Gray)
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
                    ) {
                        val w = size.width
                        val h = size.height

                        // Horizontal grid lines (6 lines: 0%, 20%, 40%, 60%, 80%, 100%)
                        val pathEffect =
                            PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        for (i in 0..5) {
                            val y = h * (i.toFloat() / 5f)
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.5f),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1f,
                                pathEffect = pathEffect
                            )
                        }

                        // Draw stacked bars
                        val barWidth =
                            if (selectedRange == "هفتگی") 24.dp.toPx() else 32.dp.toPx()
                        val chartPaddingX = barWidth / 2 + 4.dp.toPx()
                        val usableWidth = w - chartPaddingX * 2
                        val cornerR = 6.dp.toPx()

                        data.forEach { point ->
                            val normalH = (point.normalPercent / 100f) * h
                            val afibH = (point.afibPercent / 100f) * h
                            val otherH = (point.otherPercent / 100f) * h
                            val totalH = normalH + afibH + otherH
                            if (totalH <= 0f) return@forEach

                            val x = chartPaddingX + point.timeRatio * usableWidth
                            val barLeft = x - barWidth / 2
                            val barTop = h - totalH

                            // Clip to rounded-top rect so segments get rounded top corners
                            val barPath = Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        left = barLeft,
                                        top = barTop,
                                        right = barLeft + barWidth,
                                        bottom = h,
                                        topLeftCornerRadius = CornerRadius(cornerR),
                                        topRightCornerRadius = CornerRadius(cornerR),
                                        bottomLeftCornerRadius = CornerRadius.Zero,
                                        bottomRightCornerRadius = CornerRadius.Zero
                                    )
                                )
                            }

                            clipPath(barPath) {
                                var segY = h

                                // Normal (bottom) — darkest
                                if (normalH > 0f) {
                                    drawRect(
                                        color = normalColor,
                                        topLeft = Offset(barLeft, segY - normalH),
                                        size = Size(barWidth, normalH)
                                    )
                                    segY -= normalH
                                }

                                // AFib (middle)
                                if (afibH > 0f) {
                                    drawRect(
                                        color = afibColor,
                                        topLeft = Offset(barLeft, segY - afibH),
                                        size = Size(barWidth, afibH)
                                    )
                                    segY -= afibH
                                }

                                // Other (top) — lightest
                                if (otherH > 0f) {
                                    drawRect(
                                        color = otherColor,
                                        topLeft = Offset(barLeft, segY - otherH),
                                        size = Size(barWidth, otherH)
                                    )
                                }
                            }
                        }
                    }

                    // X-axis labels (LTR for correct ordering)
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp)
                                .align(Alignment.BottomCenter),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            xAxisLabels.forEach {
                                Text(it, fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }

                    // Empty state
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

        // Legend row (matching the stacked segments)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(normalColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("عادی", fontSize = 11.sp, color = Color(0xFF6B6B6B))

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(afibColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("AFib", fontSize = 11.sp, color = Color(0xFF6B6B6B))

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(otherColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("دیگر", fontSize = 11.sp, color = Color(0xFF6B6B6B))
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Donut Chart Card
// ═══════════════════════════════════════════════════════════════

private val normalColor = Color(0xFF3D5AF1)
private val afibColor = Color(0xFFBBC5FA)
private val otherColor = Color(0xFFECEFFE)

@Composable
fun ArrhythmiaDistributionCard(
    distribution: ArrhythmiaDetailViewModel.ArrhythmiaDistribution
) {
    val normalPct = distribution.normalPercent.toInt()
    val afibPct = distribution.afibPercent.toInt()
    val otherPct = distribution.otherPercent.toInt()
    val total = normalPct + afibPct + otherPct

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEBEBEB)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Donut chart with percentage labels
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(145.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        val strokeWidth = 24.dp.toPx()

                        // Background ring
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
                            val hasNormal = normalPct > 0
                            val hasAfib = afibPct > 0
                            val hasOther = otherPct > 0

                            if (hasNormal) {
                                val isLast = !hasAfib && !hasOther
                                val sweep = if (isLast) 270f - startAngle else 360f * (normalPct / 100f)
                                drawArc(normalColor, startAngle, sweep, false, style = capStyle)
                                startAngle += sweep
                            }

                            if (hasAfib) {
                                val isLast = !hasOther
                                val sweep = if (isLast) 270f - startAngle else 360f * (afibPct / 100f)
                                drawArc(afibColor, startAngle, sweep, false, style = capStyle)
                                startAngle += sweep
                            }

                            if (hasOther) {
                                val sweep = 270f - startAngle
                                drawArc(otherColor, startAngle, sweep, false, style = capStyle)
                            }
                        }
                    }

                    // Center score (final_probability)
                    Text(
                        text = if (distribution.totalScore > 0) distribution.totalScore.toString().toFarsiDigits() else "0",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 24.sp,
                            color = Color.Black
                        )
                    )

                    // Percentage labels on each segment
                    if (total > 0) {
                        val radius = 48.dp
                        var angle = -90f

                        if (normalPct > 0) {
                            val midAngle = angle + (360f * (normalPct / 100f)) / 2f
                            val radians = Math.toRadians(midAngle.toDouble())
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = (radius.value * kotlin.math.cos(radians)).dp,
                                        y = (radius.value * kotlin.math.sin(radians)).dp
                                    )
                                    .background(Color(0x73F3F3F3), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "%${normalPct.toString().toFarsiDigits()}",
                                    fontSize = 14.sp,
                                    color = Color(0xFF6B6B6B)
                                )
                            }
                            angle += 360f * (normalPct / 100f)
                        }

                        if (afibPct > 0) {
                            val midAngle = angle + (360f * (afibPct / 100f)) / 2f
                            val radians = Math.toRadians(midAngle.toDouble())
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = (radius.value * kotlin.math.cos(radians)).dp,
                                        y = (radius.value * kotlin.math.sin(radians)).dp
                                    )
                                    .background(Color(0x73F3F3F3), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "%${afibPct.toString().toFarsiDigits()}",
                                    fontSize = 14.sp,
                                    color = Color(0xFF6B6B6B)
                                )
                            }
                            angle += 360f * (afibPct / 100f)
                        }

                        if (otherPct > 0) {
                            val midAngle = angle + (360f * (otherPct / 100f)) / 2f
                            val radians = Math.toRadians(midAngle.toDouble())
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = (radius.value * kotlin.math.cos(radians)).dp,
                                        y = (radius.value * kotlin.math.sin(radians)).dp
                                    )
                                    .background(Color(0x73F3F3F3), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "%${otherPct.toString().toFarsiDigits()}",
                                    fontSize = 14.sp,
                                    color = Color(0xFF6B6B6B)
                                )
                            }
                        }
                    }
                }

                // Legend
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    ArrhythmiaLegendItem("عادی", normalColor)
                    ArrhythmiaLegendItem("AFib", afibColor)
                    ArrhythmiaLegendItem("دیگر", otherColor)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description text
            Text(
                text = "ضربان های قلب شما دارای الگوی نا منظم (آریتمی) بوده است.".rtl(),
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 22.sp
                ),
                color = Color(0xFF6B6B6B),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ArrhythmiaLegendItem(label: String, color: Color) {
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
