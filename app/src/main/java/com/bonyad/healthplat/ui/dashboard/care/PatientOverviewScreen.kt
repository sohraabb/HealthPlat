package com.bonyad.healthplat.ui.dashboard.care

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bonyad.healthplat.R
import com.bonyad.healthplat.domain.model.CaregiverUiModel
import com.bonyad.healthplat.domain.model.MetricData
import com.bonyad.healthplat.ui.utils.toFarsiDigits

// Colors matching HomeScreen
private val TealPrimary = Color(0xFF5BA3A3)
private val OrangeAccent = Color(0xFFFF9800)
private val RedAccent = Color(0xFFE53935)
private val BlueAccent = Color(0xFF2196F3)
private val TextDark = Color(0xFF2C2C2C)
private val TextGray = Color(0xFF999999)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientOverviewScreen(
    viewModel: CareViewModel,
    patient: CaregiverUiModel,
    onBack: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val heartRateData by viewModel.patientHeartRate.collectAsState()
    val sleepData by viewModel.patientSleep.collectAsState()
    val spo2Data by viewModel.patientSpo2.collectAsState()
    val stressData by viewModel.patientStress.collectAsState()

    val patientName = patient.name ?: patient.phoneNumber

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = patientName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextDark,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = Color(0xFF6B6B6B)
                        )
                    }
                },
                actions = {
                    // Placeholder for symmetry
                    Spacer(modifier = Modifier.size(48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Patient info header
                PatientInfoHeader(patient = patient)

                Spacer(modifier = Modifier.height(20.dp))

                // Metric cards in 2-column grid
                val permissions = patient.permissions

                if (!permissions.heartRate && !permissions.bloodPressure &&
                    !permissions.stressLevel && !permissions.sleepQuality
                ) {
                    // No permissions
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "دسترسی به هیچ داده‌ای تعریف نشده است",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Health metric cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (permissions.heartRate) {
                            PatientMetricCard(
                                title = "ضربان قلب",
                                value = getLatestMetricValue(heartRateData),
                                unit = "bpm",
                                statusText = getHeartRateStatus(heartRateData),
                                iconRes = R.drawable.heart_rate,
                                iconTint = RedAccent,
                                chartColor = RedAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (permissions.bloodPressure) {
                            PatientMetricCard(
                                title = "اکسیژن خون",
                                value = getLatestMetricValue(spo2Data),
                                unit = "%",
                                statusText = getSpo2Status(spo2Data),
                                iconRes = R.drawable.hospital,
                                iconTint = BlueAccent,
                                chartColor = BlueAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (permissions.heartRate || permissions.bloodPressure) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (permissions.sleepQuality) {
                            PatientMetricCard(
                                title = "پایش خواب",
                                value = getLatestSleepValue(sleepData),
                                unit = "ساعت",
                                statusText = getSleepStatus(sleepData),
                                iconRes = R.drawable.chemistry_flask,
                                iconTint = TealPrimary,
                                chartColor = TealPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (permissions.stressLevel) {
                            PatientMetricCard(
                                title = "استرس",
                                value = getLatestMetricValue(stressData),
                                unit = null,
                                statusText = getStressStatus(stressData),
                                iconRes = R.drawable.care,
                                iconTint = OrangeAccent,
                                chartColor = OrangeAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp)) // Bottom padding
            }

            // Loading overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TealPrimary)
                }
            }
        }
    }
}

@Composable
fun PatientInfoHeader(patient: CaregiverUiModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = patient.name ?: "کاربر",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextDark
                )

                if (patient.name != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = patient.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Permission badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (patient.permissions.heartRate) {
                        PermissionBadge("ضربان قلب", RedAccent)
                    }
                    if (patient.permissions.bloodPressure) {
                        PermissionBadge("SpO2", BlueAccent)
                    }
                    if (patient.permissions.stressLevel) {
                        PermissionBadge("استرس", OrangeAccent)
                    }
                    if (patient.permissions.sleepQuality) {
                        PermissionBadge("خواب", TealPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun PermissionBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PatientMetricCard(
    title: String,
    value: String,
    unit: String?,
    statusText: String?,
    iconRes: Int,
    iconTint: Color,
    chartColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header row: icon + title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTint.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (statusText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mini chart placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                contentAlignment = Alignment.Center
            ) {
                MiniChart(chartColor = chartColor)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = value.toFarsiDigits(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = TextDark
                )

                if (unit != null) {
                    Spacer(modifier = Modifier.width(4.dp))
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

@Composable
fun MiniChart(chartColor: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp)
    ) {
        val path = Path()
        val width = size.width
        val height = size.height
        path.moveTo(0f, height * 0.6f)
        path.lineTo(width * 0.15f, height * 0.5f)
        path.lineTo(width * 0.3f, height * 0.7f)
        path.lineTo(width * 0.45f, height * 0.3f)
        path.lineTo(width * 0.6f, height * 0.6f)
        path.lineTo(width * 0.75f, height * 0.4f)
        path.lineTo(width * 0.9f, height * 0.55f)
        path.lineTo(width, height * 0.45f)

        drawPath(
            path = path,
            color = chartColor,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

// ============ Metric Value Helpers ============

private fun getLatestMetricValue(data: List<MetricData>): String {
    if (data.isEmpty()) return "—"

    val latestMetric = data.lastOrNull() ?: return "—"
    val latestValue = latestMetric.values.lastOrNull() ?: return "—"

    return latestValue.toString()
}

private fun getLatestSleepValue(data: List<MetricData>): String {
    if (data.isEmpty()) return "—"

    val value = data.lastOrNull()
        ?.values
        ?.lastOrNull()
        ?: return "—"

    return String.format("%.1f", value.toFloat())
}

private fun getHeartRateStatus(data: List<MetricData>): String {
    if (data.isEmpty()) return "در انتظار داده"

    val value = data.lastOrNull()
        ?.values
        ?.lastOrNull()
        ?: return "در انتظار داده"

    return when {
        value < 60 -> "کمتر از حد طبیعی"
        value > 100 -> "بالاتر از حد طبیعی"
        else -> "عادی"
    }
}

private fun getSpo2Status(data: List<MetricData>): String {
    if (data.isEmpty()) return "در انتظار داده"

    val value = data.lastOrNull()
        ?.values
        ?.lastOrNull()
        ?: return "در انتظار داده"

    return when {
        value < 90 -> "پایین"
        value < 95 -> "قابل قبول"
        else -> "عادی"
    }
}

private fun getSleepStatus(data: List<MetricData>): String? {
    if (data.isEmpty()) return "در انتظار داده"
    val value = data.lastOrNull()
        ?.values
        ?.lastOrNull()
        ?: return "در انتظار داده"

    return when {
        value < 6f -> "کم خوابیده"
        value < 8f -> "خوب"
        value > 9f -> "زیاد خوابیده"
        else -> "عالی"
    }
}

private fun getStressStatus(data: List<MetricData>): String {
    if (data.isEmpty()) return "در انتظار داده"

    val value = data.lastOrNull()
        ?.values
        ?.lastOrNull()
        ?: return "در انتظار داده"

    return when {
        value < 30 -> "آرام"
        value < 60 -> "متوسط"
        else -> "بالا"
    }
}