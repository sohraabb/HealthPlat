package com.bonyad.healthplat.ui.dashboard.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.bonyad.healthplat.R
import com.bonyad.healthplat.ui.navigation.NavRoutes
import com.bonyad.healthplat.ui.navigation.ProfileRoutes
import com.bonyad.healthplat.ui.utils.toFarsiDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenDashboard(
    viewModel: ProfileDashboardViewModel = hiltViewModel(),
    onNavigateToProfileRoutes: (String) -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val userName by viewModel.userName.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val currentGoal by viewModel.currentGoal.collectAsState()
    val goalProgress by viewModel.goalProgress.collectAsState()
    val nightModeEnabled by viewModel.nightModeEnabled.collectAsState()
    val showLogoutDialog by viewModel.showLogoutDialog.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is ProfileNavigationEvent.NavigateToLogin -> onNavigateToLogin()
            }
        }
    }

    if (showLogoutDialog) {
        Dialog(onDismissRequest = { viewModel.dismissLogoutDialog() }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "خروج از حساب",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF2C2C2C)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "آیا مطمئن هستید که می‌خواهید از حساب خود خارج شوید؟",
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                    ) {
                        Button(
                            onClick = { viewModel.dismissLogoutDialog() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF0F0F0),
                                contentColor = Color(0xFF6B6B6B)
                            )
                        ) {
                            Text("انصراف", fontSize = 14.sp)
                        }

                        Button(
                            onClick = { viewModel.confirmLogout() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE53935),
                                contentColor = Color.White
                            )
                        ) {
                            Text("بله، خروج", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    TextButton(
                        onClick = { onNavigateToProfileRoutes(ProfileRoutes.EditPersonalInfo.route) }
                    ) {
                        Text(
                            text = "ویرایش اطلاعات",
                            color = Color(0xFF5BA3A3),
                            fontSize = 14.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Notifications */ }) {
                        Icon(
                            painter = painterResource(R.drawable.notification),
                            tint = Color.Black,
                            contentDescription = "اعلان‌ها",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F5F5)
                ),
                windowInsets = WindowInsets(top = 8.dp)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Profile Avatar
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                // TODO: Replace with actual user avatar
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF5BA3A3),
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Name
            Text(
                text = userName ?: "کاربر",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = Color(0xFF2C2C2C)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Phone Number
            Text(
                text = phoneNumber?.toFarsiDigits() ?: "",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp
                ),
                color = Color(0xFF999999)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Goals Card
            GoalsCard(
                currentGoal = currentGoal,
                progress = goalProgress,
                onAdjustGoals = { viewModel.onAdjustGoalsClick() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Medals/Achievements
            MedalsSection()

            Spacer(modifier = Modifier.height(24.dp))

            // Section 1: Main menu items
            MenuSection {
                MenuItemRow(
                    icon = painterResource(R.drawable.notif_setting),
                    title = "تنظیم هشدار شخصی",
                    onClick = { onNavigateToProfileRoutes(ProfileRoutes.AlarmSettings.route) }
                )

                HorizontalDivider(color = Color(0xFFE8E8E8), thickness = 1.dp)

                MenuItemRow(
                    icon = painterResource(R.drawable.pills),
                    title = "ثبت دارو",
                    onClick = { onNavigateToProfileRoutes(ProfileRoutes.Medication.route) }
                )

                HorizontalDivider(color = Color(0xFFE8E8E8), thickness = 1.dp)

                MenuItemRow(
                    icon = painterResource(R.drawable.wallet),
                    title = "شارژ کیف پول",
                    onClick = { onNavigateToProfileRoutes(ProfileRoutes.Wallet.route) }
                )

                HorizontalDivider(color = Color(0xFFE8E8E8), thickness = 1.dp)

                MenuItemRow(
                    icon = painterResource(R.drawable.ring),
                    title = "حلقه",
                    onClick = { onNavigateToProfileRoutes(ProfileRoutes.DeviceSetup.route) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Settings and logout
            MenuSection {
                NightModeToggleRow(
                    enabled = nightModeEnabled,
                    onToggle = { viewModel.onNightModeToggle(it) }
                )

                HorizontalDivider(color = Color(0xFFE8E8E8), thickness = 1.dp)

                MenuItemRow(
                    icon = painterResource(R.drawable.calling),
                    title = "پشتیبانی و پیگیری",
                    onClick = { /* TODO */ }
                )

                HorizontalDivider(color = Color(0xFFE8E8E8), thickness = 1.dp)

                MenuItemRow(
                    icon = painterResource(R.drawable.information),
                    title = "راهنما و قوانین",
                    onClick = { /* TODO */ }
                )

                HorizontalDivider(color = Color(0xFFE8E8E8), thickness = 1.dp)

                MenuItemRow(
                    icon = painterResource(R.drawable.logout),
                    title = "خروج",
                    onClick = { viewModel.showLogoutConfirmation() },
                    iconTint = Color(0xFF6B6B6B)
                )
            }

            // Bottom padding for navigation bar
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun GoalsCard(
    currentGoal: String,
    progress: Float,
    onAdjustGoals: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onAdjustGoals,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "تنظیم اهداف",
                        color = Color(0xFF5BA3A3),
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = "اهداف شما:",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF2C2C2C)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Goal Description
            Text(
                text = "امروز فقط ۵۰۰۰ قدم دیگه بذار تا هدفت کامل بشه!",
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp
                ),
                color = Color(0xFF666666),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Time remaining
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "زمان باقی مانده: ۱۴ ساعت",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp
                        ),
                        color = Color(0xFF999999)
                    )
                    Image(
                        painter = painterResource(R.drawable.circle_clock),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Progress percentage
                Text(
                    text = "٪ ۴۳",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF5BA3A3)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "۵۰۰۰ قدمی",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = Color(0xFF999999)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    // Background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(
                                color = Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    // Progress
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(8.dp)
                            .background(
                                color = Color(0xFF5BA3A3),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }

                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF5BA3A3),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun MedalsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { /* TODO: View all medals */ },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.back_arrow),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "دیدن همه",
                            color = Color(0xFF5BA3A3),
                            fontSize = 14.sp
                        )
                    }
                }

                Text(
                    text = "مدال ها ۱۲ از ۷۰",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF2C2C2C)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Medal Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MedalItem(
                    icon = Icons.Default.Person, // Trophy
                    title = "قهرمان",
                    backgroundColor = Color(0xFFFFF3E0)
                )

                MedalItem(
                    icon = Icons.Default.Person, // Badge
                    title = "خستگی ناپذیر",
                    backgroundColor = Color(0xFFE8F5E9)
                )

                MedalItem(
                    icon = Icons.Default.Favorite, // Health
                    title = "بدن سالم",
                    backgroundColor = Color(0xFFE3F2FD)
                )
            }
        }
    }
}

@Composable
fun MedalItem(
    icon: ImageVector,
    title: String,
    backgroundColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(backgroundColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when (backgroundColor) {
                    Color(0xFFFFF3E0) -> Color(0xFFFF9800)
                    Color(0xFFE8F5E9) -> Color(0xFF4CAF50)
                    else -> Color(0xFF2196F3)
                },
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp
            ),
            color = Color(0xFF666666),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ProfileMenuItem(
    icon: Painter,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.back_arrow),
                contentDescription = null,
                tint = Color(0xFF6B6B6B),
                modifier = Modifier.size(20.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp
                    ),
                    color = Color(0xFF6B6B6B)
                )

                Image(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun NightModeToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF5BA3A3),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE0E0E0)
                )
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "حالت شب",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp
                    ),
                    color = Color(0xFF6B6B6B)
                )

                Image(
                    painter = painterResource(R.drawable.dark_mode),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun MenuSection(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
fun MenuItemRow(
    icon: Painter,
    title: String,
    onClick: () -> Unit,
    iconTint: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.back_arrow),
            contentDescription = null,
            tint = Color(0xFF6B6B6B),
            modifier = Modifier.size(20.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp
                ),
                color = Color(0xFF6B6B6B)
            )

            Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = if (iconTint != Color.Unspecified) {
                    androidx.compose.ui.graphics.ColorFilter.tint(iconTint)
                } else null
            )
        }
    }
}

@Composable
fun NightModeToggleRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF5BA3A3),
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFBBBBBB),
                uncheckedBorderColor = Color.Transparent
            ),
            modifier = Modifier.scale(0.9f)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "حالت شب",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp
                ),
                color = Color(0xFF6B6B6B)
            )

            Image(
                painter = painterResource(R.drawable.dark_mode),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}