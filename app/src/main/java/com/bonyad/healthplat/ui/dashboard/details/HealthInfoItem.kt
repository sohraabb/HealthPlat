package com.bonyad.healthplat.ui.dashboard.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bonyad.healthplat.R

/**
 * Data class representing a single info section with optional icon/color
 */
data class HealthInfoItem(
    val title: String,
    val description: String,
    val iconRes: Int? = null,       // Optional icon resource
    val dotColor: Color? = null     // Optional colored dot (for sleep stages etc.)
)

/**
 * Sealed class containing predefined info items for each health metric
 */
sealed class HealthInfoType {
    abstract val items: List<HealthInfoItem>
    abstract val screenTitle: String

    object HeartRate : HealthInfoType() {
        override val screenTitle = "توضیحات"
        override val items = listOf(
            HealthInfoItem(
                title = "ضربان قلب",
                description = "یعنی تعداد دفعاتی که قلب در مدت یک دقیقه می‌تپد. این عدد نشان می‌دهد قلب با چه سرعتی خون را در بدن پمپ می‌کند و معمولاً با واحد bpm (ضربه در دقیقه) بیان می‌شود.",
                iconRes = R.drawable.heart
            ),
            HealthInfoItem(
                title = "HRV",
                description = "تغییرپذیری ضربان قلب یعنی میزان تفاوت در فاصله زمانی بین ضربان‌های متوالی قلب. HRV نشان می‌دهد سیستم عصبی بدن چقدر انعطاف‌پذیر است و معمولاً هرچه بالاتر باشد، نشانه سازگاری و وضعیت بهتر بدن است.",
                iconRes = R.drawable.send_love
            )
        )
    }

    object Sleep : HealthInfoType() {
        override val screenTitle = "توضیحات"
        override val items = listOf(
            HealthInfoItem(
                title = "خواب عمیق",
                description = "مرحله‌ای از خواب که بدن بیشترین استراحت و ترمیم را انجام می‌دهد و بیدار شدن در این مرحله سخت‌تر است.",
                dotColor = Color(0xFFFFC107) // Yellow/Amber
            ),
            HealthInfoItem(
                title = "خواب سبک",
                description = "مرحله‌ای از خواب که بدن آرام می‌شود اما به‌راحتی می‌توان از آن بیدار شد و بخش زیادی از خواب شب را تشکیل می‌دهد.",
                dotColor = Color(0xFF4CAF50) // Green
            ),
            HealthInfoItem(
                title = "خواب REM",
                description = "مرحله‌ای از خواب که مغز فعال‌تر است، بیشتر رویاها در آن رخ می‌دهد و نقش مهمی در حافظه و یادگیری دارد.",
                dotColor = Color(0xFF9C27B0) // Purple
            )
        )
    }

    object SpO2 : HealthInfoType() {
        override val screenTitle = "توضیحات"
        override val items = listOf(
            HealthInfoItem(
                title = "SpO₂",
                description = "یعنی درصد اشباع اکسیژن خون و نشان می‌دهد چه مقدار از هموگلوبین خون با اکسیژن پر شده است. این شاخص بیان می‌کند بدن تا چه حد اکسیژن کافی دریافت می‌کند و معمولاً به صورت درصد (%) نمایش داده می‌شود که مقدار آن هرچه بالاتر باشد بهتر است.",
                iconRes = R.drawable.hospital_spo2
            )
        )
    }

    object Steps : HealthInfoType() {
        override val screenTitle = "توضیحات"
        override val items = listOf(
            HealthInfoItem(
                title = "تعداد قدم",
                description = "شمارش قدم یکی از ساده‌ترین روش‌ها برای پایش فعالیت روزانه است. هدف پیشنهادی ۱۰,۰۰۰ قدم در روز است که معادل حدود ۸ کیلومتر پیاده‌روی می‌باشد. تحقیقات نشان می‌دهد حتی ۷,۰۰۰ قدم در روز می‌تواند فواید سلامتی قابل توجهی داشته باشد.",
                dotColor = Color(0xFF4CAF50)
            ),
            HealthInfoItem(
                title = "کالری",
                description = "میزان کالری سوزانده شده بستگی به وزن، سرعت و مدت زمان پیاده‌روی دارد. به طور متوسط، هر ۱۰۰۰ قدم حدود ۴۰-۵۰ کالری می‌سوزاند. پیاده‌روی با سرعت بالاتر یا در سربالایی کالری بیشتری می‌سوزاند.",
                dotColor = Color(0xFFFFC107)
            )
        )
    }

    object Stress : HealthInfoType() {
        override val screenTitle = "توضیحات"
        override val items = listOf(
            HealthInfoItem(
                title = "سطح استرس",
                description = "سطح استرس بر اساس تغییرات HRV و الگوهای ضربان قلب محاسبه می‌شود. امتیاز ۰-۲۵ آرامش، ۲۶-۵۰ طبیعی، ۵۱-۷۵ متوسط و ۷۶-۱۰۰ بالا را نشان می‌دهد. استرس کوتاه‌مدت طبیعی است اما استرس مزمن می‌تواند مضر باشد.",
                dotColor = Color(0xFF4CAF50)
            )
        )
    }
}

/**
 * Main info screen with list of items - matching Figma design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthInfoScreen(
    infoType: HealthInfoType,
    onBack: () -> Unit
) {
    var selectedItem by remember { mutableStateOf<HealthInfoItem?>(null) }

    // Bottom sheet state
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            HealthInfoTopBar(
                onBack = onBack
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            infoType.items.forEach { item ->
                InfoItemCard(
                    item = item,
                    onClick = { selectedItem = item }
                )
            }
        }
    }

    // Bottom Sheet for details
    if (selectedItem != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedItem = null },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = null
        ) {
            InfoDetailBottomSheet(
                item = selectedItem!!,
                onClose = { selectedItem = null }
            )
        }
    }
}

@Composable
private fun HealthInfoTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Title - Absolutely centered on screen
        Text(
            text = "توضیحات",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.Black,
            modifier = Modifier.align(Alignment.Center)
        )

        // Close button - Right aligned (same position as CustomDetailTopBar)
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.CenterEnd)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.close_square),
                contentDescription = "Close",
                tint = Color.Gray
            )
        }
    }
}

/**
 * Individual info item card - matches Figma design
 */
@Composable
fun InfoItemCard(
    item: HealthInfoItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Icon or Colored Dot
            when {
                item.iconRes != null -> {
                    Icon(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(24.dp)
                    )
                }
                item.dotColor != null -> {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(item.dotColor)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right side: Title and Description (RTL aligned)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = Color(0xFF2C2C2C),
                    textAlign = TextAlign.End
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = Color(0xFF9E9E9E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

/**
 * Bottom sheet showing full item details - matches Figma design
 */
@Composable
fun InfoDetailBottomSheet(
    item: HealthInfoItem,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Header row with title and close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Spacer for balance (since close is on right)
            Spacer(modifier = Modifier.size(32.dp))

            // Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = Color(0xFF2C2C2C),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            // Close button
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.close_square),
                    contentDescription = "Close",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Description text
        Text(
            text = item.description,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                lineHeight = 26.sp
            ),
            color = Color(0xFF666666),
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Standalone bottom sheet composable that can be called from any detail screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthInfoBottomSheet(
    infoType: HealthInfoType,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        var selectedItem by remember { mutableStateOf<HealthInfoItem?>(null) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = null
        ) {
            if (selectedItem == null) {
                // List view
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 32.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.size(32.dp))

                        Text(
                            text = infoType.screenTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            ),
                            color = Color(0xFF2C2C2C),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.close_square),
                                contentDescription = "Close",
                                tint = Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Items list
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        infoType.items.forEach { item ->
                            InfoItemCard(
                                item = item,
                                onClick = { selectedItem = item }
                            )
                        }
                    }
                }
            } else {
                // Detail view
                InfoDetailBottomSheet(
                    item = selectedItem!!,
                    onClose = { selectedItem = null }
                )
            }
        }
    }
}

/**
 * Simple single-item info bottom sheet (for screens with only one info item)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleInfoBottomSheet(
    title: String,
    description: String,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.size(32.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = Color(0xFF2C2C2C),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close_square),
                            contentDescription = "Close",
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 26.sp
                    ),
                    color = Color(0xFF666666),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}