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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.R
import com.bonyad.healthplat.ui.utils.toFarsiDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenDashboard(
    viewModel: ProfileDashboardViewModel = hiltViewModel()
) {
    val userName by viewModel.userName.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val currentGoal by viewModel.currentGoal.collectAsState()
    val goalProgress by viewModel.goalProgress.collectAsState()
    val nightModeEnabled by viewModel.nightModeEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    TextButton(
                        onClick = { /* TODO: Edit profile */ }
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
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "اعلان‌ها",
                            tint = Color(0xFF2C2C2C)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F5F5)
                )
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
                text = userName ?: "داریوش فقیهی",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = Color(0xFF2C2C2C)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Phone Number
            Text(
                text = phoneNumber?.toFarsiDigits() ?: "۰۹۱۸۹۵۶۵۰۸",
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

            // Menu Items
            ProfileMenuItem(
                icon = painterResource(R.drawable.notif_setting),
                title = "تنظیم هشدار شخصی",
                onClick = { /* TODO */ }
            )

            ProfileMenuItem(
                icon = painterResource(R.drawable.pills),
                title = "ثبت دارو",
                onClick = { /* TODO */ }
            )

            ProfileMenuItem(
                icon = painterResource(R.drawable.ring),
                title = "حلقه",
                onClick = { /* TODO */ }
            )

            ProfileMenuItem(
                icon = painterResource(R.drawable.wallet),
                title = "شارژ کیف پول",
                onClick = { /* TODO */ }
            )

            // Night Mode Toggle
            NightModeToggle(
                enabled = nightModeEnabled,
                onToggle = { viewModel.onNightModeToggle(it) }
            )

            ProfileMenuItem(
                icon = painterResource(R.drawable.calling),
                title = "پشتیبانی و پیگیری",
                onClick = { /* TODO */ }
            )

            ProfileMenuItem(
                icon = painterResource(R.drawable.information),
                title = "راهنما و قوانین",
                onClick = { /* TODO */ }
            )

            Spacer(modifier = Modifier.height(32.dp))
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
            Image(
                painter = painterResource(R.drawable.back_arrow),
                contentDescription = null,
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
                    color = Color(0xFF2C2C2C)
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
                    color = Color(0xFF2C2C2C)
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