package com.bonyad.healthplat.ui.dashboard.care

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bonyad.healthplat.R
import com.bonyad.healthplat.domain.model.CaregiverUiModel
import com.bonyad.healthplat.domain.model.MetricData
import com.bonyad.healthplat.ui.dashboard.*
import com.bonyad.healthplat.ui.navigation.HealthDetailRoutes
import com.bonyad.healthplat.ui.utils.rtl
import com.bonyad.healthplat.ui.utils.toFarsiDigits

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientOverviewScreen(
    viewModel: CareViewModel,
    patient: CaregiverUiModel,
    onBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit = {}
) {
    val isLoading by viewModel.isLoading.collectAsState()

    val heartRateData by viewModel.patientHeartRate.collectAsState()
    val sleepHours by viewModel.patientSleepHours.collectAsState()
    val spo2Data by viewModel.patientSpo2.collectAsState()
    val stressData by viewModel.patientStress.collectAsState()

    // Build HealthCardData list based on permissions — same structure as HomeScreen
    val permissions = patient.permissions

    val patientId = patient.patientId ?: ""

    val healthCards = remember(heartRateData, spo2Data, sleepHours, stressData, permissions, patientId) {
        buildList {
            if (permissions.heartRate) {
                val hrValue = getLatestMetricValue(heartRateData)
                val hrInt = hrValue.toIntOrNull() ?: 0
                add(
                    HealthCardData(
                        title = "ضربان قلب",
                        value = hrInt.toString().toFarsiDigits(),
                        unit = "bpm",
                        statusText = when {
                            hrInt == 0 -> "در انتظار داده"
                            hrInt < 60 -> "کمتر از حد طبیعی"
                            hrInt > 100 -> "بالاتر از حد طبیعی"
                            else -> "عادی"
                        },
                        iconRes = R.drawable.heart_rate,
                        iconTint = RedAccent,
                        route = HealthDetailRoutes.CaregiverHeartRate.createRoute(patientId),
                        chartType = ChartType.HEART_RATE
                    )
                )
            }

            if (permissions.bloodPressure) {
                val spo2Value = getLatestMetricValue(spo2Data)
                val spo2Int = spo2Value.toIntOrNull() ?: 0
                add(
                    HealthCardData(
                        title = "اکسیژن خون",
                        value = spo2Int.toString().toFarsiDigits(),
                        unit = "%",
                        statusText = when {
                            spo2Int == 0 -> "در انتظار داده"
                            spo2Int < 90 -> "پایین"
                            spo2Int < 95 -> "قابل قبول"
                            else -> "عادی"
                        },
                        iconRes = R.drawable.hospital,
                        iconTint = BlueAccent,
                        route = HealthDetailRoutes.CaregiverSpO2.createRoute(patientId),
                        chartType = ChartType.SPO2
                    )
                )
            }

            if (permissions.sleepQuality) {
                add(
                    HealthCardData(
                        title = "خواب",
                        value = if (sleepHours > 0f) {
                            String.format("%.1f", sleepHours).toFarsiDigits()
                        } else "۰",
                        unit = "ساعت",
                        statusText = when {
                            sleepHours == 0f -> "در انتظار داده"
                            sleepHours < 6f -> "کم خوابیده"
                            sleepHours < 8f -> "خوب"
                            sleepHours > 9f -> "زیاد خوابیده"
                            else -> "عالی"
                        },
                        iconRes = R.drawable.sleep,
                        iconTint = Color(0xFF9747FF),
                        route = HealthDetailRoutes.CaregiverSleep.createRoute(patientId),
                        chartType = ChartType.SLEEP
                    )
                )
            }

            if (permissions.stressLevel) {
                val stressValue = getLatestMetricValue(stressData)
                val stressInt = stressValue.toIntOrNull() ?: 0
                add(
                    HealthCardData(
                        title = "استرس",
                        value = stressInt.toString().toFarsiDigits(),
                        unit = null,
                        statusText = when {
                            stressInt == 0 -> "در انتظار داده"
                            stressInt < 30 -> "آرام"
                            stressInt < 60 -> "متوسط"
                            else -> "بالا"
                        },
                        iconRes = R.drawable.stress_card,
                        iconTint = Color(0xFFF9C640),
                        route = HealthDetailRoutes.CaregiverStress.createRoute(patientId),
                        chartType = ChartType.STRESS
                    )
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "مراقب",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF2C2C2C),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.back_arrow),
                            contentDescription = "بازگشت",
                            tint = Color(0xFF2C2C2C)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Notifications or QR */ }) {
                        Icon(
                            painter = painterResource(R.drawable.notification),
                            contentDescription = "Notification",
                            tint = Color(0xFF6B6B6B)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F5F5),
                    titleContentColor = Color(0xFF2C2C2C),
                    navigationIconContentColor = Color(0xFF2C2C2C),
                    actionIconContentColor = Color(0xFF5BA3A3)
                ),
                windowInsets = WindowInsets(top = 8.dp)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val screenWidth = maxWidth
            val horizontalPadding = 16.dp
            val cardSpacing = 12.dp
            val cardWidth = (screenWidth - (horizontalPadding * 2) - cardSpacing) / 2

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPadding)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Description text matching Figma
                Text(
                    text = "شما پارامتر های سلامتی ${patient.name ?: "کاربر"} را مشاهده میکنید.".rtl(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B6B6B),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 2x2 Metric Cards Grid — same layout logic as HomeScreen
                if (healthCards.isEmpty()) {
                    NoDataMessage()
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(cardSpacing)
                    ) {
                        // Chunk cards into rows of 2
                        healthCards.chunked(2).forEach { rowCards ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(cardSpacing),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowCards.forEach { card ->
                                    HealthMetricCard(
                                        data = card,
                                        onClick = {
                                            if (card.route.isNotEmpty()) {
                                                onNavigateToDetail(card.route)
                                            }
                                        },
                                        cardWidth = cardWidth
                                    )
                                }
                                // If odd number of cards, add spacer to fill the row
                                if (rowCards.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
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

@Composable
private fun NoDataMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "دسترسی به هیچ داده‌ای تعریف نشده است",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

// ============ Metric Value Helpers ============

private fun getLatestMetricValue(data: List<MetricData>): String {
    if (data.isEmpty()) return "0"
    val latestValue = data.lastOrNull()?.values?.lastOrNull { it > 0 } ?: return "0"
    return latestValue.toInt().toString()
}

