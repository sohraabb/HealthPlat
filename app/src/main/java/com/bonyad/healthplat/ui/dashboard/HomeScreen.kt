package com.bonyad.healthplat.ui.dashboard

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.bonyad.healthplat.ui.theme.HealthPlatTheme
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bonyad.healthplat.R
import com.bonyad.healthplat.ui.components.ConnectionIndicatorButton
import com.bonyad.healthplat.ui.components.ConnectivityBottomSheet
import com.bonyad.healthplat.ui.components.DeviceConnectionState
import com.bonyad.healthplat.ui.components.InfoBottomSheet
import com.bonyad.healthplat.ui.navigation.HealthDetailRoutes
import com.bonyad.healthplat.ui.navigation.NavRoutes
import com.bonyad.healthplat.ui.utils.PersianDateUtils
import com.bonyad.healthplat.ui.utils.rtl
import com.bonyad.healthplat.ui.utils.toFarsiDigits
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.coerceIn
import kotlin.math.cos
import kotlin.math.sin

// Define custom colors from design
val TealPrimary = Color(0xFF5BA3A3)
val OrangeAccent = Color(0xFFF2994A)
val RedAccent = Color(0xFFE53935)
val BlueAccent = Color(0xFF2196F3)
val TextDark = Color(0xFF000000)
val TextGray = Color(0xFF999999)

// Data class for health cards
data class HealthCardData(
    val title: String,
    val value: String,
    val unit: String?,
    val statusText: String?,
    val iconRes: Int,
    val iconTint: Color,
    val route: String,
    val chartType: ChartType
)

enum class ChartType {
    HEART_RATE,
    STEPS,
    SPO2,
    SLEEP,
    STRESS,
    ARRHYTHMIA
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DashboardViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAi: (String) -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    val context = LocalContext.current
    val healthOverview by viewModel.healthOverview.collectAsState()
    val userName by viewModel.userName.collectAsState()

    val readinessScore by viewModel.readinessScore.collectAsState()
    val insights by viewModel.healthInsights.collectAsState()

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val lastSyncServerTime by viewModel.lastSyncServerTime.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()

    // ==================== CONNECTIVITY STATE ====================
    var showConnectivitySheet by remember { mutableStateOf(false) }
    var showLocationServicesDialog by remember { mutableStateOf(false) }
    val connectivitySheetState = rememberModalBottomSheetState()

    // Determine connection state for indicator
    val connectionState = remember(healthOverview.isDeviceConnected, healthOverview.batteryLevel, isConnecting) {
        when {
            healthOverview.isDeviceConnected -> DeviceConnectionState.Connected(batteryLevel = healthOverview.batteryLevel ?: 50)
            isConnecting -> DeviceConnectionState.Connecting
            else -> DeviceConnectionState.Disconnected
        }
    }
    // ============================================================

    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Bluetooth was enabled, now connect
            viewModel.onBluetoothEnabled()
        }
    }

    // Observe bluetooth enable requests from ViewModel
    LaunchedEffect(Unit) {
        viewModel.requestBluetoothEnable.collect {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher.launch(enableBtIntent)
        }
    }

    // Observe location services enable requests — show dialog before opening settings
    // Required for BLE scanning on Samsung + OEM devices
    LaunchedEffect(Unit) {
        viewModel.requestLocationServicesEnable.collect {
            showLocationServicesDialog = true
        }
    }

    if (showLocationServicesDialog) {
        AlertDialog(
            onDismissRequest = { showLocationServicesDialog = false },
            title = { Text("موقعیت‌مکانی غیرفعال است") },
            text = { Text("برای اتصال به حلقه، لطفا سوئیچ موقعیت‌مکانی گوشی خود را فعال کنید.".rtl()) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocationServicesDialog = false
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                ) {
                    Text("باز کردن تنظیمات", color = Color(0xFF5BA3A3))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationServicesDialog = false }) {
                    Text("بعدا", color = Color(0xFF666666))
                }
            }
        )
    }

    // ==================== INFO BOTTOM SHEET STATE ====================
    var showInfoSheet by remember { mutableStateOf(false) }
    val infoSheetState = rememberModalBottomSheetState()
    // =================================================================

    val healthCards = remember(healthOverview) {
        listOf(
            HealthCardData(
                title = "ضربان قلب",
                value = healthOverview.heartRate.toString().toFarsiDigits(),
                unit = "bpm",
                statusText = when {
                    healthOverview.heartRate == 0 -> "در انتظار داده"
                    healthOverview.heartRate < 60 -> "کمتر از حد طبیعی"
                    healthOverview.heartRate > 100 -> "بالاتر از حد طبیعی"
                    else -> "عادی"
                },
                iconRes = R.drawable.heart_rate,
                iconTint = RedAccent,
                route = HealthDetailRoutes.HeartRateDetail.route,
                chartType = ChartType.HEART_RATE
            ),
            HealthCardData(
                title = "تعداد قدم",
                value = healthOverview.steps.toString().toFarsiDigits(),
                unit = "قدم",
                statusText = when {
                    healthOverview.steps == 0 -> "شروع کن"
                    healthOverview.steps < 5000 -> "بیشتر کن"
                    healthOverview.steps < 10000 -> "خوب پیش میری"
                    else -> "عالی"
                },
                iconRes = R.drawable.walk,
                iconTint = OrangeAccent,
                route = HealthDetailRoutes.StepsDetail.route,
                chartType = ChartType.STEPS
            ),
            HealthCardData(
                title = "اکسیژن خون",
                value = if (healthOverview.bloodOxygen > 0) {
                    healthOverview.bloodOxygen.toString().toFarsiDigits()
                } else {
                    "0"
                },
                unit = "%",
                statusText = when {
                    healthOverview.bloodOxygen == 0 -> ""
                    healthOverview.bloodOxygen < 90 -> "پایین"
                    healthOverview.bloodOxygen < 95 -> "قابل قبول"
                    else -> "عادی"
                },
                iconRes = R.drawable.hospital,
                iconTint = BlueAccent,
                route = HealthDetailRoutes.SpO2Detail.route,
                chartType = ChartType.SPO2
            ),
            HealthCardData(
                title = "خواب",
                value = if (healthOverview.sleepDurationHours > 0) {
                    String.format("%.1f", healthOverview.sleepDurationHours).toFarsiDigits()
                } else {
                    "0"
                },
                unit = "ساعت",
                statusText = when {
                    healthOverview.sleepDurationHours == 0f -> ""
                    healthOverview.sleepDurationHours < 6f -> "کم خوابیدی"
                    healthOverview.sleepDurationHours < 8f -> "خوب"
                    healthOverview.sleepDurationHours > 9f -> "زیاد خوابیدی"
                    else -> "عالی"
                },
                iconRes = R.drawable.sleep,
                iconTint = Color(0xFF9747FF),
                route = HealthDetailRoutes.SleepDetail.route,
                chartType = ChartType.SLEEP
            ),
            HealthCardData(
                title = "استرس",
                value = if (healthOverview.stressLevel > 0) {
                    healthOverview.stressLevel.toString().toFarsiDigits()
                } else {
                    "0"
                },
                unit = null,
                statusText = when {
                    healthOverview.stressLevel == 0 -> ""
                    healthOverview.stressLevel < 30 -> "آرام"
                    healthOverview.stressLevel < 60 -> "متوسط"
                    else -> "بالا"
                },
                iconRes = R.drawable.stress_card,
                iconTint = Color(0xFFF9C640),
                route = HealthDetailRoutes.StressDetail.route,
                chartType = ChartType.STRESS
            ),
            HealthCardData(
                title = "آریتمی قلب",
                value = if (healthOverview.arrhythmiaAfibPercent >= 0f) {
                    String.format("%.0f", healthOverview.arrhythmiaAfibPercent).toFarsiDigits()
                } else {
                    "-"
                },
                unit = "%",
                statusText = when {
                    healthOverview.arrhythmiaAfibPercent < 0f -> ""
                    healthOverview.arrhythmiaAfibPercent == 0f -> "عادی"
                    healthOverview.arrhythmiaAfibPercent < 30f -> "خفیف"
                    healthOverview.arrhythmiaAfibPercent < 60f -> "متوسط"
                    else -> "بالا"
                },
                iconRes = R.drawable.arrhythmia,
                iconTint = Color(0xFFBD2727),
                route = HealthDetailRoutes.ArrhythmiaDetail.route,
                chartType = ChartType.ARRHYTHMIA
            )
        )
    }

    val pages = remember(healthCards) { healthCards.chunked(4) }

    // ==================== CONNECTIVITY BOTTOM SHEET ====================
    if (showConnectivitySheet) {
        ConnectivityBottomSheet(
            connectionState = connectionState,
            onDismiss = { showConnectivitySheet = false },
            onConnectClick = {
                showConnectivitySheet = false
                viewModel.connectDevice()
            },
            onDisconnectClick = {
                showConnectivitySheet = false
                viewModel.disconnectDevice()
            },
            sheetState = connectivitySheetState
        )
    }
    // ===================================================================

    // ==================== INFO BOTTOM SHEET ====================
    if (showInfoSheet) {
        InfoBottomSheet(
            onDismiss = { showInfoSheet = false },
            onFaqClick = {
                // TODO: Navigate to FAQ screen
                // onNavigateToDetail("faq")
            },
            onContactClick = {
                // TODO: Navigate to Contact screen or open contact dialog
                // onNavigateToDetail("contact")
            },
            onAppGuideClick = {
                // TODO: Navigate to App Guide screen
                // onNavigateToDetail("app_guide")
            },
            sheetState = infoSheetState
        )
    }
    // ===========================================================

    Scaffold(
        topBar = {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TopAppBar(
                    title = {
                        Text(
                            text = "سلام ${userName ?: "کاربر"}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.Black
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick =  onNavigateToNotifications ,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painterResource(R.drawable.notification),
                                contentDescription = "Notifications",
                                tint = Color.Black
                            )
                        }
                    },
                    actions = {
                        ConnectionIndicatorButton(
                            connectionState = connectionState,
                            onClick = { showConnectivitySheet = true },
                            modifier = Modifier.size(40.dp)
                        )

                        IconButton(
                            onClick = { showInfoSheet = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.information),
                                contentDescription = "Help",
                                tint = TextGray
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFF5F5F5)
                    ),
                    // Reduce vertical padding
                    windowInsets = WindowInsets(top = 8.dp)
                )
            }
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        HomeContent(
            modifier = Modifier.padding(paddingValues),
            readinessScore = readinessScore,
            insights = insights,
            pages = pages,
            syncStatus = syncStatus,
            lastSyncServerTime = lastSyncServerTime,
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshData() },
            onNavigateToDetail = onNavigateToDetail,
            onNavigateToAi = { onNavigateToAi(NavRoutes.AiScreen.route) }
        )
    }
}

// Stateless body of the Home screen — extracted so it can be previewed across
// devices with sample data (see HomeScreenPreview). HomeScreen feeds it real
// state; previews feed it fakes. Both share this single layout source of truth.
@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    modifier: Modifier = Modifier,
    readinessScore: Int,
    insights: List<Pair<String, Int>>,
    pages: List<List<HealthCardData>>,
    syncStatus: SyncStatus,
    lastSyncServerTime: String?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAi: () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val screenWidth = maxWidth
        val viewportHeight = maxHeight
        val horizontalPadding = 16.dp
        val cardSpacing = 10.dp

        val lazyRowState = rememberLazyListState()
        val currentPage by remember { derivedStateOf { lazyRowState.firstVisibleItemIndex } }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            // The inner Column below fits the screen exactly via weight(1f), so it
            // never scrolls vertically — which starves PullToRefreshBox of the
            // nested-scroll events it needs to detect the pull gesture. Wrapping it
            // in a verticalScroll column (pinned to the viewport height, so scroll
            // range stays 0 and the layout looks identical) restores nested-scroll
            // dispatch, so the swipe-down is seen and the spinner/onRefresh fire.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(viewportHeight)
                    .padding(horizontal = horizontalPadding)
                    .padding(top = 8.dp)
            ) {
                // Status card — natural height, no measurement needed
                NewHealthStatusCard(
                    score = readinessScore,
                    insights = insights,
                    navigateToAi = onNavigateToAi,
                    onScoreClick = { onNavigateToDetail(HealthDetailRoutes.ReadinessDetail.route) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Card grid — weight(1f) tells the Column:
                // "give this child every dp that isn't claimed by the other children."
                // The inner BoxWithConstraints then sees that exact height and splits
                // it into two equal card rows. Change any spacer anywhere and this
                // automatically adjusts — no manual counting required.
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val cardHeight = ((maxHeight - cardSpacing) / 2).coerceAtLeast(120.dp)

                    LazyRow(
                        state = lazyRowState,
                        flingBehavior = rememberSnapFlingBehavior(lazyRowState),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(pages.size) { pageIndex ->
                            val pageCards = pages[pageIndex]
                            Column(
                                modifier = Modifier.width(screenWidth - (horizontalPadding * 2))
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(cardSpacing),
                                    modifier = Modifier.fillMaxWidth().height(cardHeight)
                                ) {
                                    pageCards.getOrNull(0)?.let { card ->
                                        HealthMetricCardAdaptive(
                                            data = card,
                                            onClick = { onNavigateToDetail(card.route) },
                                            modifier = Modifier.weight(1f).fillMaxHeight()
                                        )
                                    }
                                    pageCards.getOrNull(1)?.let { card ->
                                        HealthMetricCardAdaptive(
                                            data = card,
                                            onClick = { onNavigateToDetail(card.route) },
                                            modifier = Modifier.weight(1f).fillMaxHeight()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(cardSpacing))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(cardSpacing),
                                    modifier = Modifier.fillMaxWidth().height(cardHeight)
                                ) {
                                    pageCards.getOrNull(2)?.let { card ->
                                        HealthMetricCardAdaptive(
                                            data = card,
                                            onClick = { onNavigateToDetail(card.route) },
                                            modifier = Modifier.weight(1f).fillMaxHeight()
                                        )
                                    }
                                    pageCards.getOrNull(3)?.let { card ->
                                        HealthMetricCardAdaptive(
                                            data = card,
                                            onClick = { onNavigateToDetail(card.route) },
                                            modifier = Modifier.weight(1f).fillMaxHeight()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Page indicator — pill for active page, dot for inactive
                if (pages.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        pages.indices.forEach { index ->
                            val isActive = index == currentPage
                            Box(
                                modifier = Modifier
                                    .width(if (isActive) 16.dp else 6.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        if (isActive) Color(0xFF5BA3A3)
                                        else Color(0xFF5BA3A3).copy(alpha = 0.3f)
                                    )
                            )
                            if (index < pages.size - 1) {
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Sync status row
                SyncStatusRow(syncStatus = syncStatus, lastSyncServerTime = lastSyncServerTime)

                // Reserve space for the floating nav bar overlay. Trimmed from 96.dp
                // → 80.dp: the floating bar (DashboardScreen) carries its own 12.dp
                // top padding, so 80.dp still clears it while handing ~16.dp back to
                // the grid. Eyeball the bottom row on Pixel + S23 after changing this.
                Spacer(modifier = Modifier.navigationBarsPadding().height(80.dp))
            }
            }
        }
    }
}

// =========================== SYNC STATUS ===========================

@Composable
fun SyncStatusRow(syncStatus: SyncStatus, lastSyncServerTime: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (syncStatus) {
            SyncStatus.Syncing -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = TealPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "در حال همگام‌سازی...".rtl(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    maxLines = 1
                )
            }
            SyncStatus.Success -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                LastSyncText(lastSyncServerTime)
            }
            SyncStatus.Failed -> {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = RedAccent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "خطا در همگام‌سازی",
                    style = MaterialTheme.typography.bodySmall,
                    color = RedAccent,
                    maxLines = 1
                )
            }
            SyncStatus.Idle -> {
                LastSyncText(lastSyncServerTime)
            }
        }
    }
}

@Composable
private fun LastSyncText(serverTime: String?) {
    if (serverTime.isNullOrEmpty()) return

    val formatted = remember(serverTime) {
        try {
            // Server returns local time with Z suffix (e.g. "2026-02-23T22:00:00Z")
            // so we parse without timezone conversion — treat as-is.
            val stripped = serverTime.replace("T", " ").replace("Z", "").split("\\.")[0]
            val parts = stripped.split(" ")
            if (parts.size >= 2) {
                val dateParts = parts[0].split("-")
                val timeParts = parts[1].split(":")
                val (_, jm, jd) = PersianDateUtils.georgianToJalali(
                    dateParts[0].toInt(), dateParts[1].toInt(), dateParts[2].toInt()
                )
                val monthName = PersianDateUtils.getMonthName(jm)
                val time = "${timeParts[0]}:${timeParts[1]}".toFarsiDigits()
                "$jd $monthName — $time".toFarsiDigits()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    if (formatted != null) {
        Text(
            text = "آخرین همگام‌سازی: $formatted",
            style = MaterialTheme.typography.bodySmall,
            color = TextGray,
            maxLines = 1
        )
    }
}

// =========================== NEW MAIN CARD ===========================

@Composable
fun NewHealthStatusCard(
    score: Int,
    insights: List<Pair<String, Int>>,
    navigateToAi: () -> Unit,
    onScoreClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = TealPrimary.copy(alpha = 0.3f),
                spotColor = TealPrimary.copy(alpha = 0.3f)
            )
            .border(1.dp, TealPrimary.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp), // Reduced from 16.dp
            horizontalAlignment = Alignment.End // Changed to End for RTL alignment
        ) {
            // Title aligned to end
            Text(
                text = "وضعیت کلی سلامت",
                style = MaterialTheme.typography.titleMedium,
                color = TextDark,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End // Align text to end
            )

            Spacer(modifier = Modifier.height(8.dp)) // Reduced from 16.dp

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle section
                HealthScoreCircle(
                    score = score,
                    modifier = Modifier
                        .size(124.dp)
                        .clickable { onScoreClick() }
                )

                Spacer(modifier = Modifier.width(8.dp)) // Reduced from 16.dp

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp), // Reduced from 12.dp
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    insights.forEach { (text, flagScore) ->
                        InsightItem(text = text, flagScore = flagScore)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp)) // Reduced from 20.dp

            Button(
                onClick = { navigateToAi() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp), // Reduced from 50.dp
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                border = BorderStroke(1.dp, TealPrimary)
            ) {
                Text(
                    text = "تحلیل هوش مصنوعی",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * Custom Component to draw the dotted ring and thinner progress bar
 */
@Composable
fun HealthScoreCircle(
    score: Int,
    modifier: Modifier = Modifier
) {
    // 1. Determine base color based on score thresholds
    val baseColor = when {
        score < 25 -> Color(0xFFEB5757) // Red
        score in 25..50 -> Color(0xFFF2C94C) // Yellow
        else -> Color(0xFF6FCF97) // Green
    }

    Box(
        modifier = modifier.size(124.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = center.x
            val cy = center.y

            // === 1. Dotted Ring (outermost) ===
            val dotRadius = 1.5.dp.toPx()
            val dotRingRadius = (size.minDimension / 2) - dotRadius
            val stepDeg = 10

            for (angle in 0 until 360 step stepDeg) {
                val rad = Math.toRadians(angle.toDouble())
                val x = cx + dotRingRadius * cos(rad).toFloat()
                val y = cy + dotRingRadius * sin(rad).toFloat()

                drawCircle(
                    color = baseColor.copy(alpha = 0.25f), // Match dot color to current state
                    radius = dotRadius,
                    center = Offset(x, y)
                )
            }

            // === 2. Progress Arc ===
            val strokeWidthPx = 4.dp.toPx() // Roughly matches the 1.5px border from Figma
            val arcPadding = 10.dp.toPx()
            val arcRadius = (size.minDimension / 2) - arcPadding

            // Background track (very faint ring underneath the progress)
            drawCircle(
                color = baseColor.copy(alpha = 0.08f),
                radius = arcRadius,
                center = center,
                style = Stroke(width = strokeWidthPx)
            )

            // Setup the Gradient Brush matching the Figma linear-gradient
            val arcBrush = if (score >= 100) {
                SolidColor(baseColor.copy(alpha = 0.87f))
            } else {
                Brush.verticalGradient(
                    0.0f to baseColor.copy(alpha = 0.87f),
                    0.3462f to baseColor.copy(alpha = 0.87f),
                    1.0f to Color.White.copy(alpha = 0.87f),
                    startY = cy - arcRadius, // Top of the arc bounds
                    endY = cy + arcRadius    // Bottom of the arc bounds
                )
            }

            // Calculate symmetrical start and sweep angles
            val sweepAngle = (score / 100f) * 360f
            // By subtracting half the sweep from the top (-90 degrees), it fills evenly down both sides
            val startAngle = -90f - (sweepAngle / 2f)

            // Draw a single, clean arc
            drawArc(
                brush = arcBrush,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(cx - arcRadius, cy - arcRadius),
                size = Size(arcRadius * 2, arcRadius * 2),
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        // === 3. Inner White Circle with Shadow ===
        Box(
            modifier = Modifier
                .size(88.dp)
                .shadow(
                    elevation = 6.dp, // Softened elevation slightly to match design
                    shape = CircleShape,
                    ambientColor = baseColor.copy(alpha = 0.15f), // Colored shadow looks more premium
                    spotColor = baseColor.copy(alpha = 0.15f)
                )
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = score.toString(), // Add your .toFarsiDigits() here
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2D2D), // Assuming TextDark
                        fontSize = 32.sp
                    )
                )
                Text(
                    text = "/۱۰۰",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E) // Assuming TextGray
                )
            }
        }
    }
}

@Composable
fun InsightItem(text: String, flagScore: Int) {
    val (iconRes, iconTint) = when (flagScore) {
        3    -> Pair(R.drawable.star,           Color(0xFF4ECDC4)) // عالی — teal
        2    -> Pair(R.drawable.check_sqaure,   Color(0xFF4CAF50)) // خوب — green
        1    -> Pair(R.drawable.info_circle,    TextGray)          // متوسط — gray
        else -> Pair(R.drawable.error_triangle, Color(0xFFE57373)) // نیازمند توجه — red
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text.rtl(),
            style = MaterialTheme.typography.bodySmall,
            color = TextGray,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(14.dp)
        )
    }
}

// =========================== UNIFIED METRIC CARD ===========================

@Composable
fun HealthMetricCard(
    data: HealthCardData,
    onClick: () -> Unit,
    cardWidth: Dp = 180.dp
) {
    Card(
        modifier = Modifier
            .width(cardWidth)
            .height(170.dp)
            .padding(1.dp)
            .clickable { onClick() },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    painter = painterResource(data.iconRes),
                    contentDescription = data.title,
                    tint = data.iconTint,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = data.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextDark,
                )
            }

            if (data.statusText != null) {
                Text(
                    text = data.statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    textAlign = TextAlign.End,
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
                ChartContent(chartType = data.chartType, color = data.iconTint)
            }

            // --- FOOTER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = data.value,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 22.sp
                        ),
                        color = TextDark
                    )

                    if (data.unit != null) {
                        Text(
                            text = data.unit,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(TealPrimary.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.custom_arrow),
                        contentDescription = "Details",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

// Flexible-height variant used by HomeScreen's responsive grid.
// Accepts a Modifier instead of fixed width/height — the Row's height(cardHeight)
// drives the size, and the chart area uses weight(1f) to fill remaining space.
@Composable
private fun HealthMetricCardAdaptive(
    data: HealthCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(1.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    painter = painterResource(data.iconRes),
                    contentDescription = data.title,
                    tint = data.iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextDark
                )
            }

            if (data.statusText != null) {
                Text(
                    text = data.statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                ChartContent(chartType = data.chartType, color = data.iconTint)
            }

            // Keep the chart bars off the footer/arrow — without this the
            // bottom-aligned bars run straight into the teal arrow button.
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = data.value,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp),
                        color = TextDark
                    )
                    if (data.unit != null) {
                        Text(
                            text = data.unit,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(TealPrimary.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.custom_arrow),
                        contentDescription = "Details",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ChartContent(chartType: ChartType, color: Color) {
    when (chartType) {
        ChartType.HEART_RATE -> HeartRateChart(color)
        ChartType.STEPS -> StepsChart(color)
        ChartType.SPO2 -> SpO2Chart(color)
        ChartType.SLEEP -> SleepChart(color)
        ChartType.STRESS -> StressChart(color)
        ChartType.ARRHYTHMIA -> ArrhythmiaChart(color)
    }
}

@Composable
fun HeartRateChart(color: Color) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween, // Evenly space the 5 bars
        verticalAlignment = Alignment.Bottom
    ) {
        // Bar 1: Split (Light Top / Dark Bottom)
        SplitBar(
            color = color,
            totalHeightFraction = 0.65f,
            topFraction = 0.3f,
            bottomFraction = 0.6f
        )

        // Bar 2: Solid Tall Bar
        SolidBar(
            color = color,
            heightFraction = 0.95f
        )

        // Bar 3: Split (Larger Top / Smaller Bottom)
        SplitBar(
            color = color,
            totalHeightFraction = 0.6f,
            topFraction = 0.5f,
            bottomFraction = 0.4f
        )

        // Bar 4: Split (Small Top / Large Bottom)
        SplitBar(
            color = color,
            totalHeightFraction = 0.8f,
            topFraction = 0.2f,
            bottomFraction = 0.7f
        )

        // Bar 5: Solid Tall Bar
        SolidBar(
            color = color,
            heightFraction = 0.95f
        )
    }
}

@Composable
fun StepsChart(color: Color) {
    val stepsGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFF2994A), // #F2994A
            Color(0xFFA86F3C)  // #A86F3C
        )
    )

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
                    .background(stepsGradient)
            )
        }
    }
}

@Composable
fun SpO2Chart(color: Color) {
    val spo2Blue = Color(0xFF56CCF2) // Figma border color

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp) // less vertical padding = taller chart
    ) {
        val w = size.width
        val h = size.height

        // Step line path
        val linePath = Path().apply {
            moveTo(0f, h * 0.5f)          // ① START here (50% down)
            lineTo(w * 0.20f, h * 0.5f)   // ② → flat line to 20% width (same Y)
            lineTo(w * 0.20f, h * 0.8f)   // ③ ↓ drop down to 70% height
            lineTo(w * 0.40f, h * 0.8f)   // ④ → flat line to 40% width
            lineTo(w * 0.40f, h * 0.2f)   // ⑤ ↑ big jump up to 20% height
            lineTo(w * 0.70f, h * 0.2f)   // ⑥ → long flat line to 70% width
            lineTo(w * 0.70f, h * 0.8f)   // ⑦ ↓ drop back to 50%
            lineTo(w, h * 0.8f)           // ⑧ → flat line to end
        }

        // Filled area path
        val fillPath = Path().apply {
            moveTo(0f, h * 0.5f)          // ① START here (50% down)
            lineTo(w * 0.20f, h * 0.5f)   // ② → flat line to 20% width (same Y)
            lineTo(w * 0.20f, h * 0.8f)   // ③ ↓ drop down to 70% height
            lineTo(w * 0.40f, h * 0.8f)   // ④ → flat line to 40% width
            lineTo(w * 0.40f, h * 0.2f)   // ⑤ ↑ big jump up to 20% height
            lineTo(w * 0.70f, h * 0.2f)   // ⑥ → long flat line to 70% width
            lineTo(w * 0.70f, h * 0.8f)   // ⑦ ↓ drop back to 50%
            lineTo(w, h * 0.8f)           // ⑧ → flat line to end
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        // Gradient fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    spo2Blue.copy(alpha = 0.3f),
                    spo2Blue.copy(alpha = 0.05f)
                ),
                startY = 0f,
                endY = h
            )
        )

        // Step line on top
        drawPath(
            path = linePath,
            color = spo2Blue,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Square)
        )
    }
}

@Composable
fun SleepChart(color: Color) {
    // Colors based on the purple palette in the image
    val awakeColor = Color(0xFFE1D5FF)
    val remColor = Color(0xFFC4B5FD)
    val deepColor = Color(0xFF8B5CF6) // Darkest purple
    val lightColor = Color(0xFFDDD6FE)

    val segments = listOf(
        Pair("بیدار", awakeColor),
        Pair("REM", remColor),
        Pair("عمیق", deepColor),
        Pair("سبک", lightColor)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp) // Taller bar like the image
                .clip(RoundedCornerShape(4.dp))
        ) {
            segments.forEach { (label, color) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = label,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StressChart(color: Color, stressLevel: Int = 90) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 8.dp)
    ) {
        val width = size.width
        val height = size.height

        // The image shows a filled semi-circle/arc
        val arcPath = Path().apply {
            moveTo(0f, height)
            // Create the "hump" shape
            cubicTo(
                width * 0.1f, height * 0.1f,
                width * 0.9f, height * 0.1f,
                width, height
            )
            close()
        }

        drawPath(
            path = arcPath,
            color = Color(0xFFDBAB33) // The golden/yellow color from the screenshot
        )
    }
}

@Composable
fun ArrhythmiaChart(color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        val w = size.width
        val h = size.height
        val mid = h * 0.5f

        // ECG-style heartbeat waveform
        val ecgPath = Path().apply {
            moveTo(0f, mid)
            // Flat lead-in
            lineTo(w * 0.15f, mid)
            // Small bump
            lineTo(w * 0.20f, mid - h * 0.1f)
            lineTo(w * 0.25f, mid)
            // Flat
            lineTo(w * 0.30f, mid)
            // Sharp peak up (R-wave)
            lineTo(w * 0.38f, h * 0.05f)
            // Sharp dip down (S-wave)
            lineTo(w * 0.46f, h * 0.85f)
            // Return to baseline
            lineTo(w * 0.54f, mid)
            // Flat
            lineTo(w * 0.60f, mid)
            // Small bump (T-wave)
            lineTo(w * 0.65f, mid - h * 0.12f)
            lineTo(w * 0.72f, mid)
            // Flat lead-out
            lineTo(w, mid)
        }

        drawPath(
            path = ecgPath,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// --- Helper Composables for the specific Figma style ---

@Composable
private fun SolidBar(color: Color, heightFraction: Float) {
    Box(
        modifier = Modifier
            .width(8.dp) // Match thickness from design
            .fillMaxHeight(heightFraction)
            .clip(RoundedCornerShape(50)) // Fully rounded "Pill" shape
            .background(color)
    )
}

@Composable
private fun SplitBar(
    color: Color,
    totalHeightFraction: Float,
    topFraction: Float,
    bottomFraction: Float
) {
    Column(
        modifier = Modifier
            .width(8.dp)
            .fillMaxHeight(totalHeightFraction),
        verticalArrangement = Arrangement.SpaceBetween // Pushes top up and bottom down
    ) {
        // Top Segment (Lighter/Faded)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(topFraction)
                .clip(RoundedCornerShape(50))
                .background(color.copy(alpha = 0.3f)) // Lighter shade for top part
        )

        Spacer(modifier = Modifier.height(3.dp)) // The gap between segments

        // Bottom Segment (Solid)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(bottomFraction)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

// =========================== PREVIEWS ===========================

// Sample data mirroring DashboardViewModel.healthOverview so previews look like
// a populated screen. Used only by @Preview functions.
private fun sampleHealthCards(): List<HealthCardData> = listOf(
    HealthCardData(
        "ضربان قلب", "۷۲".toFarsiDigits(), "bpm", "عادی",
        R.drawable.heart_rate, RedAccent, HealthDetailRoutes.HeartRateDetail.route, ChartType.HEART_RATE
    ),
    HealthCardData(
        "تعداد قدم", "۶۵۴۳".toFarsiDigits(), "قدم", "خوب پیش میری",
        R.drawable.walk, OrangeAccent, HealthDetailRoutes.StepsDetail.route, ChartType.STEPS
    ),
    HealthCardData(
        "اکسیژن خون", "۹۸".toFarsiDigits(), "%", "عادی",
        R.drawable.hospital, BlueAccent, HealthDetailRoutes.SpO2Detail.route, ChartType.SPO2
    ),
    HealthCardData(
        "خواب", "۷.۵".toFarsiDigits(), "ساعت", "خوب",
        R.drawable.sleep, Color(0xFF9747FF), HealthDetailRoutes.SleepDetail.route, ChartType.SLEEP
    ),
    HealthCardData(
        "استرس", "۳۴".toFarsiDigits(), null, "متوسط",
        R.drawable.stress_card, Color(0xFFF9C640), HealthDetailRoutes.StressDetail.route, ChartType.STRESS
    ),
    HealthCardData(
        "آریتمی قلب", "۰".toFarsiDigits(), "%", "عادی",
        R.drawable.arrhythmia, Color(0xFFBD2727), HealthDetailRoutes.ArrhythmiaDetail.route, ChartType.ARRHYTHMIA
    )
)

// Mimics the floating nav bar that DashboardScreen overlays on top of HomeScreen,
// so previews show whether the bottom card row clears it. Not used at runtime.
@Composable
private fun PreviewFauxNavBar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("پروفایل", "کالری", "مراقبت", "خانه").forEach { label ->
                    Text(label, style = MaterialTheme.typography.labelSmall, color = TextGray)
                }
            }
        }
    }
}

@Preview(name = "Pixel 4", device = "id:pixel_4", showSystemUi = true)
@Preview(name = "Galaxy S23", device = "spec:width=360dp,height=780dp,dpi=420", showSystemUi = true)
@Preview(name = "Galaxy S22 Ultra", device = "spec:width=480dp,height=1010dp,dpi=500", showSystemUi = true)
@Preview(name = "Small phone", device = "spec:width=320dp,height=533dp,dpi=320", showSystemUi = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenPreview() {
    val pages = remember { sampleHealthCards().chunked(4) }
    HealthPlatTheme(dynamicColor = false) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
                Scaffold(
                    containerColor = Color(0xFFF5F5F5),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "سلام مرتضی",
                                    style = MaterialTheme.typography.titleLarge.copy(color = Color.Black)
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF5F5F5)),
                            windowInsets = WindowInsets(top = 8.dp)
                        )
                    }
                ) { paddingValues ->
                    HomeContent(
                        modifier = Modifier.padding(paddingValues),
                        readinessScore = 72,
                        insights = listOf(
                            "ضربان قلب در محدوده طبیعی" to 2,
                            "کیفیت خواب مناسب" to 3,
                            "سطح استرس متوسط" to 1
                        ),
                        pages = pages,
                        syncStatus = SyncStatus.Success,
                        lastSyncServerTime = "2026-06-01T14:30:00Z",
                        isRefreshing = false,
                        onRefresh = {},
                        onNavigateToDetail = {},
                        onNavigateToAi = {}
                    )
                }
                // Overlay the floating nav so the bottom-row clearance is visible.
                PreviewFauxNavBar(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}