package com.bonyad.healthplat.ui.dashboard.ai

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.bonyad.healthplat.ui.components.PersianDate
import com.bonyad.healthplat.ui.components.PersianDatePickerDialog
import com.bonyad.healthplat.ui.utils.PersianDateUtils
import com.bonyad.healthplat.ui.utils.toFarsiDigits

// Color palette matching the design
private val DarkBackground = Color(0xFF0B121E)
private val CardBackground = Color(0xFF131B2E)
private val AccentCyan = Color(0xFF4ECDC4)
private val AccentCyanLight = Color(0xFF5DD3CB)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFF8A9199)
private val TextLightGray = Color(0xFFB0B8C1)
private val StatusGood = Color(0xFF4ECDC4)
private val StatusMedium = Color(0xFFE99C2E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAnalysisScreen(
    viewModel: AiAnalysisViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onConsultClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.selectedPersianDate.collectAsState()

    // Date picker dialog visibility
    var showDatePicker by remember { mutableStateOf(false) }

    // Max selectable date = today (can't pick future)
    val todayPersian = remember {
        val (jy, jm, jd) = PersianDateUtils.getCurrentJalaliDate()
        PersianDate(jy, jm, jd)
    }

    // Show date picker dialog
    if (showDatePicker) {
        PersianDatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                showDatePicker = false
                viewModel.fetchAnalysisForDate(date)
            },
            onDismiss = { showDatePicker = false },
            maxDate = todayPersian
        )
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.back_arrow),
                            contentDescription = "بازگشت",
                            tint = TextWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                actions = {
                    // Calendar icon — opposite side of back button
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            painter = painterResource(R.drawable.calendar),
                            contentDescription = "انتخاب تاریخ",
                            tint = TextWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        when (val currentState = state) {
            is AiAnalysisUiState.Loading -> {
                LoadingContent(modifier = Modifier.padding(padding))
            }
            is AiAnalysisUiState.Error -> {
                ErrorContent(
                    message = currentState.message,
                    onRetry = { viewModel.fetchAnalysis() },
                    modifier = Modifier.padding(padding)
                )
            }
            is AiAnalysisUiState.Success -> {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    SuccessContent(
                        data = currentState.data,
                        onConsultClick = onConsultClick,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = AccentCyan,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "در حال دریافت تحلیل...",
                color = TextGray,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.retry_device_connection),
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = message,
                color = TextWhite,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "تلاش مجدد",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SuccessContent(
    data: AiAnalysisData,
    onConsultClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // AI Icon at top
        AiIconHeader()

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = "تحلیل هوش مصنوعی",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = TextWhite
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Readiness Score Label with sparkle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "امتیاز آمادگی",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_sparkle),
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Circular Score
        AnimatedCircularScore(score = data.overallScore)

        Spacer(modifier = Modifier.height(12.dp))

        // Score Badge
        Surface(
            color = AccentCyan.copy(alpha = 0.15f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_sparkle),
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "امتیاز آمادگی",
                    color = AccentCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Summary Card
        SummaryCard(text = data.summaryText)

        Spacer(modifier = Modifier.height(24.dp))

        // Section Title
        Text(
            text = "شاخص\u200Cهای سبک زندگی",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = TextWhite
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Metric Cards
        data.metrics.forEach { metric ->
            AiMetricCard(metric = metric)
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Consultant Button
        Button(
            onClick = onConsultClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentCyan
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_consultant),
                contentDescription = null,
                tint = DarkBackground,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "مشاور",
                color = DarkBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Disclaimer and last analysis
        Text(
            text = data.disclaimerText,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                lineHeight = 18.sp
            ),
            color = TextGray.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = data.lastAnalysisDate.toFarsiDigits(),
            style = MaterialTheme.typography.bodySmall,
            color = TextGray
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AiIconHeader() {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        AccentCyan.copy(alpha = 0.3f),
                        AccentCyan.copy(alpha = 0.1f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_ai_brain),
            contentDescription = null,
            tint = AccentCyan,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun AnimatedCircularScore(score: Int) {
    var animationProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(score) {
        animationProgress = 0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (score > 0) score / 100f else 0f,
        animationSpec = tween(durationMillis = 1500),
        label = "score_animation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(160.dp)
    ) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val strokeWidth = 10.dp.toPx()

            // Background circle
            drawArc(
                color = AccentCyan.copy(alpha = 0.15f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        AccentCyan,
                        AccentCyanLight,
                        AccentCyan
                    )
                ),
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Glow effect dot at the end
            if (animatedProgress > 0) {
                val angle = Math.toRadians((-90 + animatedProgress * 360).toDouble())
                val radius = size.minDimension / 2
                val x = center.x + (radius * kotlin.math.cos(angle)).toFloat()
                val y = center.y + (radius * kotlin.math.sin(angle)).toFloat()

                drawCircle(
                    color = AccentCyan,
                    radius = 6.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score.toString().toFarsiDigits(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp
                ),
                color = TextWhite
            )
            Text(
                text = "از ۱۰۰",
                style = MaterialTheme.typography.bodySmall,
                color = TextGray
            )
        }
    }
}

@Composable
private fun SummaryCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground.copy(alpha = 0.8f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_sparkle),
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "ارزیابی کلی",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextGray
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 26.sp,
                    fontSize = 14.sp
                ),
                color = TextWhite,
                textAlign = TextAlign.Justify
            )
        }
    }
}

@Composable
private fun AiMetricCard(metric: AiMetric) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title
                Text(
                    text = metric.title,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )

                // Status Badge
                Surface(
                    color = metric.statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = metric.status,
                        color = metric.statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AccentCyan.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(metric.iconRes),
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Value text
            Text(
                text = metric.value.toFarsiDigits(),
                color = TextGray,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Description
            Text(
                text = metric.description,
                color = TextLightGray,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Advice Box
            Surface(
                color = Color(0xFF1A2436),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_lightbulb),
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = metric.advice,
                        color = AccentCyan,
                        fontSize = 12.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}