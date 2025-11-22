package com.bonyad.healthplat.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bonyad.healthplat.R
import com.bonyad.healthplat.ui.utils.toFarsiDigits

// Define custom colors from design
val TealPrimary = Color(0xFF5BA3A3)
val OrangeAccent = Color(0xFFFF9800)
val RedAccent = Color(0xFFE53935)
val BlueAccent = Color(0xFF2196F3)
val TextDark = Color(0xFF2C2C2C)
val TextGray = Color(0xFF999999)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DashboardViewModel
) {
    val healthOverview by viewModel.healthOverview.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val insights = viewModel.healthInsights

    Scaffold(
        topBar = {
            // Force RTL layout for the TopBar area
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TopAppBar(
                    title = {
                        Text(
                            text = "سلام ${userName ?: "کاربر"}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { /* TODO: Notifications */ }) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = TextGray
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* TODO: Help */ }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Help",
                                tint = TextGray
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                )
            }
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        // Force RTL for the main content
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. New Main Health Status Card
                NewHealthStatusCard(
                    score = 87, // Mock score
                    insights = insights
                )

                // 2. 2x2 Grid of Cards (Manually constructed with Rows)
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(Modifier.weight(1f)) {
                            HeartRateCard(heartRate = healthOverview.heartRate)
                        }
                        Box(Modifier.weight(1f)) {
                            StepsCard(steps = healthOverview.steps)
                        }
                    }

                    // Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(Modifier.weight(1f)) {
                            BloodPressureCard()
                        }
                        Box(Modifier.weight(1f)) {
                            SpO2Card(spo2 = healthOverview.bloodOxygen)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp)) // Extra space at bottom
            }
        }
    }
}

// =========================== NEW MAIN CARD ===========================

@Composable
fun NewHealthStatusCard(
    score: Int,
    insights: List<String>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = TealPrimary.copy(alpha = 0.3f),
                spotColor = TealPrimary.copy(alpha = 0.3f)
            )
            .border(1.dp, TealPrimary.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "وضعیت کلی سلامت",
                style = MaterialTheme.typography.titleMedium,
                color = TextDark,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main Content Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Text Descriptions (On the Right in RTL layout)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    insights.forEach { insight ->
                        InsightItem(text = insight)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 2. Score Circle (On the Left in RTL layout)
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .border(4.dp, TealPrimary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = score.toString().toFarsiDigits(),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        )
                        Text(
                            text = "/۱۰۰",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { /* TODO: AI Analysis */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary)
            ) {
                Text(
                    text = "تحلیل هوش مصنوعی",
                    color = TealPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun InsightItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TextGray,
            textAlign = TextAlign.End
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = TextGray.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
        )
    }
}

// =========================== METRIC CARDS BASE ===========================

@Composable
fun BaseMetricCard(
    title: String,
    value: String,
    unit: String?,
    statusText: String?,
    iconPainter: Painter,
    iconTint: Color,
    chartContent: @Composable BoxScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth() // Fills the weight in the row
            .height(210.dp)
            .clickable { /* TODO: Navigate */ },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- HEADER ---
            // In RTL: Start is Right, End is Left.
            // We want Text on Right (Start), Icon on Left (End).
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark,
                    fontWeight = FontWeight.Bold
                )

                Icon(
                    painter = iconPainter,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (statusText != null) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    textAlign = TextAlign.Start, // Right in RTL
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // --- CHART AREA ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                chartContent()
            }

            // --- FOOTER ---
            // We want Button on Right (Start), Value on Left (End)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Button (Start / Right)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(TealPrimary.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Details",
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(-45f) // Rotate to point diagonal up
                    )
                }

                // 2. Value + Unit (End / Left)
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = TextDark
                    )

                    if (unit != null) {
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun HeartRateCard(heartRate: Int) {
    BaseMetricCard(
        title = "ضربان قلب",
        value = heartRate.toString().toFarsiDigits(),
        unit = "bpm",
        statusText = "عادی",
        iconPainter = painterResource(R.drawable.heart_rate),
        iconTint = RedAccent
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)) {
            val path = Path()
            val width = size.width
            val height = size.height
            path.moveTo(0f, height * 0.7f)
            path.lineTo(width * 0.2f, height * 0.6f)
            path.lineTo(width * 0.4f, height * 0.8f)
            path.lineTo(width * 0.5f, height * 0.2f)
            path.lineTo(width * 0.6f, height * 0.7f)
            path.lineTo(width * 0.8f, height * 0.5f)
            path.lineTo(width, height * 0.65f)

            drawPath(
                path = path,
                color = RedAccent,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
fun StepsCard(steps: Int) {
    BaseMetricCard(
        title = "تعداد قدم",
        value = steps.toString().toFarsiDigits(),
        unit = "steps",
        statusText = "بیشتر کن",
        iconPainter = painterResource(R.drawable.walk),
        iconTint = OrangeAccent
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            val heights = listOf(0.4f, 0.6f, 0.3f, 0.8f, 0.5f, 0.7f, 0.4f)
            heights.forEach { relativeHeight ->
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight(relativeHeight)
                        .clip(RoundedCornerShape(4.dp))
                        .background(OrangeAccent.copy(alpha = 0.7f))
                )
            }
        }
    }
}

@Composable
fun BloodPressureCard() {
    val systolic = 120
    BaseMetricCard(
        title = "فشار خون",
        value = "$systolic".toFarsiDigits(),
        unit = "mmHg",
        statusText = "عادی",
        iconPainter = painterResource(R.drawable.chemistry_flask),
        iconTint = TealPrimary
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, TealPrimary.copy(alpha=0.3f), RoundedCornerShape(6.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(TealPrimary)
                )
            }
        }
    }
}

@Composable
fun SpO2Card(spo2: Int) {
    val displayValue = if (spo2 > 0) spo2.toString() else "--"
    BaseMetricCard(
        title = "اکسیژن خون",
        value = displayValue.toFarsiDigits(),
        unit = "%",
        statusText = "عادی",
        iconPainter = painterResource(R.drawable.hospital),
        iconTint = BlueAccent
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(vertical = 12.dp)) {
            val path = Path()
            val w = size.width
            val h = size.height
            path.moveTo(0f, h * 0.5f)
            path.lineTo(w * 0.2f, h * 0.5f)
            path.lineTo(w * 0.2f, h * 0.3f)
            path.lineTo(w * 0.5f, h * 0.3f)
            path.lineTo(w * 0.5f, h * 0.6f)
            path.lineTo(w * 0.8f, h * 0.6f)
            path.lineTo(w * 0.8f, h * 0.4f)
            path.lineTo(w, h * 0.4f)

            drawPath(
                path = path,
                color = BlueAccent,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Square)
            )
        }
    }
}