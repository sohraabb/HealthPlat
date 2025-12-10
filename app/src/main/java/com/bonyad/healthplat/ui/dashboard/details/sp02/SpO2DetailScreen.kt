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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.CustomDetailTopBar
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.DateStrip
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.TimeRangeSelector
import com.bonyad.healthplat.ui.utils.toFarsiDigits

@Composable
fun SpO2DetailScreen(
    viewModel: SpO2DetailViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val chartData by viewModel.chartData.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val selectedRange by viewModel.selectedTimeRange.collectAsState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CustomDetailTopBar(
                    title = "اکسیژن خون", // Blood Oxygen
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
                // 3. Chart Section
                SpO2ChartSection(chartData)

                // 4. Latest Measurement Card
                LatestMeasurementCard(
                    value = stats.lastValue,
                    time = stats.lastTime
                )

                // 5. Stats Row (High, Avg, Low)
                SpO2StatsRow(stats)

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun SpO2ChartSection(data: List<SpO2DetailViewModel.SpO2Point>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Chart Header
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "بازه اکسیژن", // Oxygen Range
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Range Value
            Text(
                text = "۹۲ - ۹۸ ٪",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "امروز ۲۲ مهر",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scatter Chart Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(Color.White)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 30.dp, end = 16.dp, top = 20.dp, bottom = 30.dp)
            ) {
                val w = size.width
                val h = size.height

                // Y-Axis Range (85% to 100% based on screenshot)
                val minVal = 85f
                val maxVal = 100f
                val range = maxVal - minVal

                // 1. Horizontal Grid Lines (85, 90, 95, 100)
                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                val steps = 3 // 0(100), 1(95), 2(90), 3(85)

                for (i in 0..steps) {
                    val y = h * (i.toFloat() / steps)
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        pathEffect = pathEffect
                    )
                }

                // 2. Vertical Grid Lines (Time)
                listOf(0.33f, 0.66f).forEach { ratio ->
                    val x = w * ratio
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        pathEffect = pathEffect
                    )
                }

                // 3. Draw Scatter Points (Dots)
                val dotColor = Color(0xFF4DD0E1) // Cyan/Light Blue

                data.forEach { point ->
                    val x = w * point.timeRatio
                    // Invert Y because canvas 0 is top
                    val y = h - ((point.value - minVal) / range) * h

                    drawCircle(
                        color = dotColor,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }

            // Y-Axis Labels
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 20.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("۱۰۰", fontSize = 10.sp, color = Color.Gray)
                Text("۹۵", fontSize = 10.sp, color = Color.Gray)
                Text("۹۰", fontSize = 10.sp, color = Color.Gray)
                Text("۸۵", fontSize = 10.sp, color = Color.Gray)
            }

            // X-Axis Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 30.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("23:59", fontSize = 10.sp, color = Color.Gray)
                Text("15:59", fontSize = 10.sp, color = Color.Gray)
                Text("07:59", fontSize = 10.sp, color = Color.Gray)
                Text("00:00", fontSize = 10.sp, color = Color.Gray)

            }
        }
    }
}

@Composable
fun LatestMeasurementCard(value: Int, time: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFB2EBF2)) // Cyan border
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {


            // Label (Left/End in RTL)
            Text(
                text = "آخرین اندازه گیری: ${time.toFarsiDigits()}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            // Value (Right/Start in RTL)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "٪",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = value.toString().toFarsiDigits(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.Gray
                )
            }
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
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFB2EBF2)) // Cyan border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.Bottom) {

                Text(
                    text = " ٪",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )


                Text(
                    text = value.toString().toFarsiDigits(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.Gray
                )

            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}