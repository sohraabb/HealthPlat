package com.bonyad.healthplat.ui.dashboard.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bonyad.healthplat.R
import com.bonyad.healthplat.domain.model.NotificationItem

// Colors matching the design
private val BackgroundColor = Color(0xFFF5F5F5)
private val CardContainerColor = Color.White
private val ItemBackgroundColor = Color(0xFFF5F4F2)
private val ItemBorderColor = Color(0xFFB6B6B6)
private val TitleColor = Color(0xFF2C2C2C)
private val DescriptionColor = Color(0xFF9E9E9E)
private val DateColor = Color(0xFF9E9E9E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    notifications: List<NotificationItem>,
    onBackClick: () -> Unit,
    onNotificationClick: (NotificationItem) -> Unit = {}
) {
    var selectedNotification by remember { mutableStateOf<NotificationItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            NotificationTopBar(onBack = onBackClick)
        },
        containerColor = BackgroundColor
    ) { padding ->
        if (notifications.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "اعلانی وجود ندارد",
                    style = MaterialTheme.typography.bodyLarge,
                    color = DescriptionColor
                )
            }
        } else {
            // White card container with shadow
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Color.Black.copy(alpha = 0.1f)
                    ),
                colors = CardDefaults.cardColors(containerColor = CardContainerColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    notifications.forEach { notification ->
                        NotificationItemCard(
                            notification = notification,
                            onClick = {
                                selectedNotification = notification
                                onNotificationClick(notification)
                            }
                        )
                    }
                }
            }
        }
    }

    // Bottom Sheet for notification details
    if (selectedNotification != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedNotification = null },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = null
        ) {
            NotificationDetailBottomSheet(
                notification = selectedNotification!!,
                onClose = { selectedNotification = null }
            )
        }
    }
}

@Composable
private fun NotificationTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Title - Absolutely centered on screen
        Text(
            text = "اعلان ها",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.Black,
            modifier = Modifier.align(Alignment.Center)
        )

        // Back button - Left aligned (for RTL, this appears on the right visually)
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.CenterStart)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.back_arrow),
                contentDescription = "بازگشت",
                tint = ItemBorderColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun NotificationItemCard(
    notification: NotificationItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ItemBackgroundColor)
            .border(
                width = 1.dp,
                color = ItemBorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Date (Left side)
            Text(
                text = notification.date,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp
                ),
                color = DateColor
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title and Description (Right side - RTL aligned)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = TitleColor,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    ),
                    color = DescriptionColor,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NotificationDetailBottomSheet(
    notification: NotificationItem,
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
            // Spacer for balance
            Spacer(modifier = Modifier.size(32.dp))

            // Title
            Text(
                text = notification.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = TitleColor,
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
                    contentDescription = "بستن",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Date
        Text(
            text = notification.date,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp
            ),
            color = DateColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Description text
        Text(
            text = notification.description,
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

@Preview(showBackground = true, locale = "fa")
@Composable
private fun NotificationScreenPreview() {
    val sampleNotifications = listOf(
        NotificationItem(
            id = 1,
            title = "بروزرسانی اپلیکیشن تن‌یار",
            description = "در نسخه جدید شما میتوانید، با فعال کردن قابلیت‌های جدید، تجربه بهتری داشته باشید.",
            date = "۱۴۰۴/۰۶/۲۰"
        ),
        NotificationItem(
            id = 2,
            title = "یادآوری سلامت",
            description = "زمان اندازه‌گیری ضربان قلب فرا رسیده است",
            date = "۱۴۰۴/۰۶/۱۹"
        )
    )

    MaterialTheme {
        NotificationScreen(
            notifications = sampleNotifications,
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, locale = "fa")
@Composable
private fun NotificationScreenEmptyPreview() {
    MaterialTheme {
        NotificationScreen(
            notifications = emptyList(),
            onBackClick = {}
        )
    }
}