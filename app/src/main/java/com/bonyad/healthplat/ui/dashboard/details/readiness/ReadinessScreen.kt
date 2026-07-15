package com.bonyad.healthplat.ui.dashboard.details.readiness

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.R
import com.bonyad.healthplat.ui.components.PersianDate
import com.bonyad.healthplat.ui.components.PersianDatePickerDialog
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.DateStrip
import com.bonyad.healthplat.ui.dashboard.details.heart_rate.TimeRangeSelector
import com.bonyad.healthplat.ui.utils.PersianDateUtils
import com.bonyad.healthplat.ui.utils.rtl
import com.bonyad.healthplat.ui.utils.toFarsiDigits
import kotlin.math.cos
import kotlin.math.sin

private val metricDescriptions = mapOf(
    "تغییرپذیری ضربان" to "این شاخص نشان\u200Cدهنده نوسانات زمانیِ بین هر تپش قلب است. یک معیار بسیار مهم برای سنجش میزان ریکاوری بدن، سطح استرس فیزیکی و تعادل سیستم عصبی خودمختار است.\n\nبر حسب میلی\u200Cثانیه (ms) اندازه\u200Cگیری می\u200Cشود. مقدار بالاتر معمولاً نشان\u200Cدهنده ریکاوری بهتر، استرس کمتر و آمادگی جسمانی بالاتر است. این عدد برای هر فرد کاملاً منحصربه\u200Cفرد است و باید با میانگین روزهای گذشته\u200Cی خود شخص مقایسه شود، نه با دیگران.",

    "اکسیژن خون" to "نشان\u200Cدهنده میزان اکسیژنی است که توسط گلبول\u200Cهای قرمز در جریان خون شما حمل می\u200Cشود. این شاخص کارایی سیستم تنفسی و گردش خون را نشان می\u200Cدهد.\n\nعالی: سطح اکسیژن بین ۹۵٪ تا ۱۰۰٪ (شرایط کاملاً طبیعی و سالم)\nخوب: سطح اکسیژن بین ۹۰٪ تا ۹۴٪\nمتوسط: سطحی مرزی، ممکن است به دلیل خستگی یا ارتفاع بالا باشد\nنیازمند توجه: زیر ۹۰٪، ممکن است نشان\u200Cدهنده مشکلات تنفسی باشد",

    "فعالیت" to "نشان\u200Cدهنده میزان تحرک فیزیکی روزانه شماست که نقش کلیدی در سلامت قلب و عروق، کنترل وزن و روحیه دارد.\n\nمعمولاً بر اساس تعداد قدم، کالری سوزانده شده یا دقایق فعالیت اندازه\u200Cگیری می\u200Cشود. از سبک زندگی کم\u200Cتحرک (زیر ۵٬۰۰۰ قدم در روز) تا بسیار فعال (بالای ۱۰٬۰۰۰ قدم) متغیر است.",

    "اضطراب" to "این شاخص سطح فشار فیزیکی یا روانی روی بدن را (معمولاً با تحلیل داده\u200Cهای ضربان قلب و HRV) تخمین می\u200Cزند.\n\nبه صورت درصد بیان می\u200Cشود. درصدهای پایین (۰ تا ۲۵٪) نشان\u200Cدهنده آرامش و استراحت بدن است، در حالی که درصدهای بالا نشان\u200Cدهنده استرس روانی، خستگی فیزیکی، بیماری، یا حتی هضم یک وعده غذایی سنگین است.",

    "خواب" to "ارزیابی کلی از کیفیت استراحت شبانه شما که فاکتورهایی مثل مدت زمان خواب، مراحل خواب (عمیق، سبک، REM) و میزان بیداری\u200Cها را در نظر می\u200Cگیرد.\n\nعالی: مدت زمان کافی، بدون بیداری\u200Cهای مکرر و خواب عمیق به میزان لازم\nخوب: خوابی نسبتاً راحت که انرژی لازم برای روز بعد را تامین می\u200Cکند\nمتوسط: کیفیت پایین\u200Cتر، ممکن است کمی کوتاه بوده یا بیداری\u200Cهای مقطعی داشته باشد\nنیازمند توجه: خواب بسیار بی\u200Cکیفیت که مانع از ریکاوری بدن و مغز می\u200Cشود",

    "ضربان قلب استراحت" to "تعداد تپش\u200Cهای قلب در هر دقیقه زمانی که بدن در حالت استراحت کامل (معمولاً بلافاصله بعد از بیداری در صبح) قرار دارد. این معیار یکی از بهترین نشانگرهای سلامت قلب و عروق است.\n\nبرای افراد بالغ سالم معمولاً بین ۶۰ تا ۱۰۰ ضربان در دقیقه است. در ورزشکاران این عدد می\u200Cتواند پایین\u200Cتر (۴۰ تا ۶۰) باشد. اعداد پایین\u200Cتر نشان\u200Cدهنده قلبی قوی\u200Cتر و کارآمدتر است."
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadinessScreen(
    viewModel: ReadinessViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var selectedAspect by remember { mutableStateOf<AspectItem?>(null) }

    val todayPersian = remember {
        val (jy, jm, jd) = PersianDateUtils.getCurrentJalaliDate()
        PersianDate(jy, jm, jd)
    }
    val selectedPersianDate = remember(selectedDate) {
        val (jy, jm, jd) = PersianDateUtils.georgianToJalali(
            selectedDate.year, selectedDate.monthValue, selectedDate.dayOfMonth
        )
        PersianDate(jy, jm, jd)
    }

    if (showDatePicker) {
        PersianDatePickerDialog(
            selectedDate = selectedPersianDate,
            onDateSelected = { date ->
                showDatePicker = false
                viewModel.selectDate(date)
            },
            onDismiss = { showDatePicker = false },
            maxDate = todayPersian
        )
    }

    // Metric description bottom sheet
    if (selectedAspect != null) {
        MetricDescriptionBottomSheet(
            title = selectedAspect!!.label,
            description = metricDescriptions[selectedAspect!!.label] ?: "",
            onDismiss = { selectedAspect = null }
        )
    }

    Scaffold(containerColor = Color(0xFFF5F5F5)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Top bar: back button + calendar icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.back_arrow),
                        contentDescription = "Back",
                        tint = Color(0xFF6B6B6B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.calendar),
                        contentDescription = "Calendar",
                        tint = Color(0xFF6B6B6B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            DateStrip(
                selectedDate = selectedDate,
                onDaySelected = { date -> viewModel.selectDay(date) }
            )
            

            Spacer(modifier = Modifier.height(24.dp))

            // Content
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF5BA3A3),
                            trackColor = Color(0xFF5BA3A3).copy(alpha = 0.15f)
                        )
                    }
                }

                uiState.hasError -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "خطا در دریافت اطلاعات",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF999999),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    // Score circle (smaller: 150dp)
                    ReadinessScoreCircle(
                        score = uiState.overallScore,
                        modifier = Modifier
                            .size(150.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Description text below score
                    Text(
                        text = "امتیاز آمادگی شما بر اساس عوامل کلیدی سلامتی محاسبه شده است",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9E9E9E),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Section title
                    Text(
                        text = "عوامل تاثیرگذار",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = Color(0xFF2C3E50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        textAlign = TextAlign.End
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Aspect list
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            uiState.aspects.forEach { aspect ->
                                AspectScoreRow(
                                    aspect = aspect,
                                    onClick = { selectedAspect = aspect }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun ReadinessScoreCircle(
    score: Int,
    modifier: Modifier = Modifier
) {
    val baseColor = when {
        score < 25 -> Color(0xFFEB5757)
        score in 25..50 -> Color(0xFFF2C94C)
        else -> Color(0xFF6FCF97)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = center.x
            val cy = center.y

            // Dotted ring (outermost)
            val dotRadius = 1.5f.dp.toPx()
            val dotRingRadius = (size.minDimension / 2) - dotRadius
            val stepDeg = 8

            for (angle in 0 until 360 step stepDeg) {
                val rad = Math.toRadians(angle.toDouble())
                val x = cx + dotRingRadius * cos(rad).toFloat()
                val y = cy + dotRingRadius * sin(rad).toFloat()

                drawCircle(
                    color = baseColor.copy(alpha = 0.25f),
                    radius = dotRadius,
                    center = Offset(x, y)
                )
            }

            // Progress arc
            val strokeWidthPx = 4.dp.toPx()
            val arcPadding = 12.dp.toPx()
            val arcRadius = (size.minDimension / 2) - arcPadding

            // Background track
            drawCircle(
                color = baseColor.copy(alpha = 0.08f),
                radius = arcRadius,
                center = center,
                style = Stroke(width = strokeWidthPx)
            )

            // Gradient arc
            val arcBrush = if (score >= 100) {
                SolidColor(baseColor.copy(alpha = 0.87f))
            } else {
                Brush.verticalGradient(
                    0.0f to baseColor.copy(alpha = 0.87f),
                    0.35f to baseColor.copy(alpha = 0.87f),
                    1.0f to Color.White.copy(alpha = 0.87f),
                    startY = cy - arcRadius,
                    endY = cy + arcRadius
                )
            }

            val sweepAngle = (score / 100f) * 360f
            val startAngle = -90f - (sweepAngle / 2f)

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

        // Inner white circle with score
        Box(
            modifier = Modifier
                .size(114.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = CircleShape,
                    ambientColor = baseColor.copy(alpha = 0.15f),
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
                    text = score.toString().toFarsiDigits(),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D2D2D),
                        fontSize = 36.sp
                    )
                )
                Text(
                    text = "/۱۰۰",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9E9E)
                )
            }
        }
    }
}

@Composable
private fun AspectScoreRow(aspect: AspectItem, onClick: () -> Unit) {
    val barColor = when {
        aspect.score < 25 -> Color(0xFFEB5757)
        aspect.score in 25..50 -> Color(0xFFF2C94C)
        else -> Color(0xFF5BA3A3)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = aspect.value.rtl(),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5BA3A3),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Icon + title on the right
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = aspect.label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = Color(0xFF2C3E50)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Icon(
                    painter = painterResource(id = aspect.iconRes),
                    contentDescription = aspect.label,
                    tint = Color(0xFF2C3E50),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress bar (RTL: fills from right to left)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFF0F0F0)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (aspect.score.coerceIn(0, 100) / 100f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetricDescriptionBottomSheet(
    title: String,
    description: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header: title + close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = Color(0xFF5BA3A3),
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close_square),
                            contentDescription = "بستن",
                            tint = Color(0xFF9E9E9E),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description text
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 26.sp
                    ),
                    color = Color(0xFF555555),
                    textAlign = TextAlign.Justify
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
