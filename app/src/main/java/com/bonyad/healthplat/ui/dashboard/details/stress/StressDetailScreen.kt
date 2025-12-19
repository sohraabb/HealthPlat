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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.CustomDetailTopBar
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.DateStrip
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.StatBox
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.TimeRangeSelector
import com.bonyad.healthplat.ui.dashboard.details.sp02.LatestMeasurementCard
import com.bonyad.healthplat.ui.utils.toFarsiDigits

@Composable
fun StressDetailScreen(
    viewModel: StressDetailViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val points by viewModel.chartPoints.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val selectedRange by viewModel.selectedTimeRange.collectAsState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CustomDetailTopBar(title = "میزان استرس",
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
                // 1. Top Section
                TimeRangeSelector(selected = selectedRange, onSelect = { viewModel.setTimeRange(it) })
                val selectedOffset by viewModel.selectedDayOffset.collectAsState()

                DateStrip(
                    selectedOffset = selectedOffset,
                    onDaySelected = { offset ->
                        viewModel.selectDay(offset)
                    }
                )

                // 2. Chart Header
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("بازه استرس", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.align(
                        Alignment.End))
                    Text("${stats.rangeMin} - ${stats.rangeMax}", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.End))
                    Text("امروز ۲۲ مهر", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.align(Alignment.End))
                }

                // 3. Stress Chart (Bezier Curve)
                StressChart(points)

                // 4. Last Measurement
                LatestMeasurementCard(value = stats.currentVal, time = stats.currentTime)

                // 5. Stats Row
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatBox("بالا ترین", Integer.valueOf(stats.high.toString().toFarsiDigits()), Modifier.weight(1f))
                    StatBox("میانگین", Integer.valueOf(stats.avg.toString().toFarsiDigits()), Modifier.weight(1f))
                    StatBox("پایین ترین", Integer.valueOf(stats.low.toString().toFarsiDigits()), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun StressChart(points: List<StressDetailViewModel.StressPoint>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .background(Color.White)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp)) {
            val w = size.width
            val h = size.height
            val maxVal = 100f

            // Grid Lines
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            listOf(0f, 0.5f, 1f).forEach { r ->
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(0f, h * r),
                    end = Offset(w, h * r),
                    pathEffect = pathEffect
                )
            }
            listOf(0.33f, 0.66f).forEach { r ->
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(w * r, 0f),
                    end = Offset(w * r, h),
                    pathEffect = pathEffect
                )
            }

            // Draw Curve
            if (points.isNotEmpty()) {
                val path = Path()
                points.forEachIndexed { i, p ->
                    val x = p.xRatio * w
                    val y = h - (p.value / maxVal * h)

                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        // Smooth cubic bezier
                        val prev = points[i - 1]
                        val prevX = prev.xRatio * w
                        val prevY = h - (prev.value / maxVal * h)

                        // Control points for smoothness
                        val cx1 = (prevX + x) / 2
                        path.cubicTo(cx1, prevY, cx1, y, x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = Color(0xFFFFD54F), // Yellow/Orange
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Dots at start and end
                val start = points.first()
                val end = points.last()
                drawCircle(Color(0xFFFFD54F), 6.dp.toPx(), Offset(start.xRatio * w, h - (start.value / maxVal * h)))
                drawCircle(Color(0xFFFFD54F), 6.dp.toPx(), Offset(end.xRatio * w, h - (end.value / maxVal * h)))
            }
        }

        // Axis Labels
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
            Column(modifier = Modifier.fillMaxHeight().padding(vertical = 24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text("۱۰۰", fontSize = 10.sp, color = Color.Gray)
                Text("۵۰", fontSize = 10.sp, color = Color.Gray)
                Text("۰", fontSize = 10.sp, color = Color.Gray)
            }
            Row(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("00:00", fontSize = 10.sp, color = Color.Gray)
                Text("07:59", fontSize = 10.sp, color = Color.Gray)
                Text("15:59", fontSize = 10.sp, color = Color.Gray)
                Text("23:59", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}