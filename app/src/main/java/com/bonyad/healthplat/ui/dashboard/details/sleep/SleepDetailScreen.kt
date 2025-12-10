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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.R
import com.bonyad.healthplat.ui.utils.toFarsiDigits


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepDetailScreen(
    viewModel: SleepDetailViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val totalMinutes by viewModel.totalSleepMinutes.collectAsState()
    val deepSleepMinutes by viewModel.deepMinutes.collectAsState()
    val lightSleepMinutes by viewModel.lightMinutes.collectAsState()
    val remMinutes by viewModel.remMinutes.collectAsState()
    val awakeMinutes by viewModel.awakeMinutes.collectAsState()
    val sleepQuality by viewModel.sleepQuality.collectAsState()

    val totalSleepHours = totalMinutes / 60f

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("پایش خواب", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painterResource(R.drawable.back_arrow),
                                contentDescription = null
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                )
            },
            containerColor = Color(0xFFF5F5F5)
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // 🟦 Total Sleep Card
                TotalSleepCard(totalSleepHours)

                // 🟩 Sleep Stages Timeline
                SleepTimelineCard(
                    deepSleep = deepSleepMinutes,
                    lightSleep = lightSleepMinutes,
                    rem = remMinutes,
                    awake = awakeMinutes
                )

                // 🟪 Donut Chart
                SleepQualityCard(
                    deepSleep = deepSleepMinutes,
                    lightSleep = lightSleepMinutes,
                    rem = remMinutes
                )

                // 🟧 Summary (Matches screenshot)
                SleepStatsCard(
                    deepSleep = deepSleepMinutes,
                    lightSleep = lightSleepMinutes,
                    rem = remMinutes,
                    awake = awakeMinutes
                )
            }
        }
    }
}

@Composable
fun TotalSleepCard(totalHours: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("زمان خواب", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF666666))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    String.format("%.0f", totalHours).toFarsiDigits(),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp, fontWeight = FontWeight.Bold),
                    color = Color(0xFF2C2C2C)
                )
                Text("ساعت", style = MaterialTheme.typography.titleMedium, color = Color(0xFF999999), modifier = Modifier.padding(bottom = 8.dp))
                Text(
                    String.format("%.0f", (totalHours % 1) * 60).toFarsiDigits(),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF2C2C2C)
                )
                Text("دقیقه", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF999999), modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
fun SleepTimelineCard(
    deepSleep: Int,
    lightSleep: Int,
    rem: Int,
    awake: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text("نمودار زمانی خواب", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(24.dp))

            // Timeline rows (simplified version from image 6)
            SleepStageRow("بیدار", awake, Color(0xFFFFB74D))
            Spacer(modifier = Modifier.height(12.dp))
            SleepStageRow("سبک", lightSleep, Color(0xFF64B5F6))
            Spacer(modifier = Modifier.height(12.dp))
            SleepStageRow("REM", rem, Color(0xFFBA68C8))
            Spacer(modifier = Modifier.height(12.dp))
            SleepStageRow("عمیق", deepSleep, Color(0xFF4DB6AC))
        }
    }
}

@Composable
fun SleepStageRow(label: String, minutes: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Bar visualization (simplified)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.End
            ) {
                repeat(minutes / 30) {
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(40.dp)
                            .padding(horizontal = 2.dp)
                            .background(color, RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(50.dp), textAlign = TextAlign.End)
    }
}

@Composable
fun SleepQualityCard(
    deepSleep: Int,
    lightSleep: Int,
    rem: Int
) {
    val total = deepSleep + lightSleep + rem
    if (total == 0) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("کیفیت خواب", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                // Donut Chart
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sweepAngle1 = 360f * (deepSleep.toFloat() / total)
                    val sweepAngle2 = 360f * (lightSleep.toFloat() / total)
                    val sweepAngle3 = 360f * (rem.toFloat() / total)

                    drawArc(
                        color = Color(0xFF4DB6AC),
                        startAngle = -90f,
                        sweepAngle = sweepAngle1,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 40.dp.toPx())
                    )
                    drawArc(
                        color = Color(0xFF64B5F6),
                        startAngle = -90f + sweepAngle1,
                        sweepAngle = sweepAngle2,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 40.dp.toPx())
                    )
                    drawArc(
                        color = Color(0xFFBA68C8),
                        startAngle = -90f + sweepAngle1 + sweepAngle2,
                        sweepAngle = sweepAngle3,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 40.dp.toPx())
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "۹۵",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF2C2C2C)
                    )
                    Text("عالی دریابوش", style = MaterialTheme.typography.bodySmall, color = Color(0xFF666666))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem("عمیق", "${(deepSleep * 100 / total)}%".toFarsiDigits(), Color(0xFF4DB6AC))
                LegendItem("سبک", "${(lightSleep * 100 / total)}%".toFarsiDigits(), Color(0xFF64B5F6))
                LegendItem("REM", "${(rem * 100 / total)}%".toFarsiDigits(), Color(0xFFBA68C8))
            }
        }
    }
}

@Composable
fun LegendItem(label: String, percentage: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(percentage, style = MaterialTheme.typography.bodySmall, color = Color(0xFF666666))
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF666666))
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
    }
}

@Composable
fun SleepStatsCard(
    deepSleep: Int,
    lightSleep: Int,
    rem: Int,
    awake: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text("خلاصه", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(16.dp))

            SleepStatRow("خواب عمیق", deepSleep)
            SleepStatRow("خواب سبک", lightSleep)
            SleepStatRow("REM", rem)
            SleepStatRow("بیداری", awake)
        }
    }
}

@Composable
fun SleepStatRow(label: String, minutes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "${minutes / 60}h ${minutes % 60}m".toFarsiDigits(),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF666666))
    }
}