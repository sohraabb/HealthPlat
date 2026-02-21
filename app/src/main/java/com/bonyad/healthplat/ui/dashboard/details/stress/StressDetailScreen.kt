package com.bonyad.healthplat.ui.dashboard.details.stress

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.StatBox
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.TimeRangeSelector
import com.bonyad.healthplat.ui.dashboard.details.sp02.LatestMeasurementCard
import com.bonyad.healthplat.ui.utils.toFarsiDigits

@Composable
fun StressDetailScreen(
    viewModel: StressDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onInfoClick: () -> Unit = {}
) {
    val points by viewModel.chartPoints.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val selectedRange by viewModel.selectedTimeRange.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val dateLabel by viewModel.dateLabel.collectAsState()

    Scaffold(
        topBar = {
            CustomDetailTopBar(
                title = "میزان استرس",
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
            // 1. Top Section
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

            // 2+3. Chart Header + Chart in ONE Column so spacedBy(24) on the outer Column
            //       doesn't inflate the gap between date text and the chart card.
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // Header (right-aligned)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = when (selectedRange) {
                            "هفتگی", "ماهانه" -> "میانگین بازه استرس"
                            else -> "بازه استرس"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B6B6B),
                        textAlign = TextAlign.End
                    )

                    val rangeText = if (stats.rangeMin > 0 || stats.rangeMax > 0) {
                        "${stats.rangeMax.toString().toFarsiDigits()} - ${
                            stats.rangeMin.toString().toFarsiDigits()
                        }"
                    } else {
                        "-"
                    }

                    Text(
                        text = rangeText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 20.sp,
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
                            CircularProgressIndicator(color = Color(0xFFFFD54F))
                        }
                    } else {
                        StressChart(
                            points = points,
                            showEmptyState = points.isEmpty()
                        )
                    }
                }
            }


            // 4. Last Measurement
            LatestMeasurementCard(
                value = stats.currentVal,
                time = stats.currentTime.toFarsiDigits(),
                unit = ""
            )

            // 5. Stats Row
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBox("بالا ترین", stats.high, Modifier.weight(1f), unit = "")
                StatBox("میانگین", stats.avg, Modifier.weight(1f), unit = "")
                StatBox("پایین ترین", stats.low, Modifier.weight(1f), unit = "")
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun StressChart(
    points: List<StressDetailViewModel.StressPoint>,
    isLoading: Boolean = false,
    showEmptyState: Boolean = false
) {
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
                CircularProgressIndicator(color = Color(0xFFFFD54F))
            }
            return@Card
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(16.dp)
        ) {
            // Same Row { Y-axis Column | Chart Box } pattern as HeartRate / SpO2
            Row(modifier = Modifier.fillMaxSize()) {

                // Y-axis labels — 3 evenly spaced
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 8.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("۱۰۰", fontSize = 10.sp, color = Color.Gray)
                    Text("۵۰",  fontSize = 10.sp, color = Color.Gray)
                    Text("۰",   fontSize = 10.sp, color = Color.Gray)
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
                        val maxVal = 100f

                        // Horizontal grid lines
                        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        listOf(0f, 0.5f, 1f).forEach { r ->
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.5f),
                                start = Offset(0f, h * r),
                                end = Offset(w, h * r),
                                pathEffect = pathEffect
                            )
                        }

                        // Vertical grid lines
                        listOf(0.33f, 0.66f).forEach { r ->
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.5f),
                                start = Offset(w * r, 0f),
                                end = Offset(w * r, h),
                                pathEffect = pathEffect
                            )
                        }

                        // Curve — only drawn when we have data
                        if (points.isNotEmpty()) {
                            val path = Path()
                            points.forEachIndexed { i, p ->
                                val x = p.xRatio * w
                                val y = (h - (p.value / maxVal * h)).coerceIn(0f, h)

                                if (i == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    val prev = points[i - 1]
                                    val prevX = prev.xRatio * w
                                    val prevY = (h - (prev.value / maxVal * h)).coerceIn(0f, h)
                                    val cx = (prevX + x) / 2f
                                    path.cubicTo(cx, prevY, cx, y, x, y)
                                }
                            }

                            drawPath(
                                path = path,
                                color = Color(0xFFFFD54F),
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Dots at start and end
                            val start = points.first()
                            val end = points.last()
                            drawCircle(
                                Color(0xFFFFD54F), 6.dp.toPx(),
                                Offset(start.xRatio * w, (h - (start.value / maxVal * h)).coerceIn(0f, h))
                            )
                            drawCircle(
                                Color(0xFFFFD54F), 6.dp.toPx(),
                                Offset(end.xRatio * w, (h - (end.value / maxVal * h)).coerceIn(0f, h))
                            )
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
                            Text("00:00", fontSize = 10.sp, color = Color.Gray)
                            Text("07:59", fontSize = 10.sp, color = Color.Gray)
                            Text("15:59", fontSize = 10.sp, color = Color.Gray)
                            Text("23:59", fontSize = 10.sp, color = Color.Gray)
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