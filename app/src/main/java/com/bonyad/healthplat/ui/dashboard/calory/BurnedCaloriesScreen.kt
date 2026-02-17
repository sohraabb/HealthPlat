package com.bonyad.healthplat.ui.dashboard.calory

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.R
import com.bonyad.healthplat.ui.utils.PersianDateUtils
import com.bonyad.healthplat.ui.utils.toFarsiDigits

// ============ Data Classes ============

data class ActivityItem(
    val id: String,
    val name: String,
    val caloriesBurned: Int,
    val duration: Int, // in minutes
    val distance: Float? = null // in km, optional
)

// ============ Burned Calories Detail Screen ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BurnedCaloriesScreen(
    viewModel: CaloryViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val burnedCalories by viewModel.burnedCalories.collectAsState()

    // Mock data for activities - in production, get from ViewModel/API
    var activities by remember {
        mutableStateOf(listOf<ActivityItem>())
    }

    var showAddActivitySheet by remember { mutableStateOf(false) }

    val currentDate = remember {
        PersianDateUtils.getFormattedPersianDate()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "کالری سوخته",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = TextDark
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { /* Notifications */ }) {
                            Icon(
                                painter = painterResource(R.drawable.notification),
                                contentDescription = "Notifications",
                                tint = TextGray
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.back_arrow),
                                contentDescription = "Back",
                                tint = TextDark
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = BackgroundColor
                    )
                )
            },
            containerColor = BackgroundColor
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Calorie Gauge Card
                BurnedCalorieGaugeCard(
                    title = "کالری سوخته",
                    date = currentDate,
                    calories = burnedCalories,
                    modifier = Modifier.padding(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Activity Grid (includes activities + Add Activity card)
                ActivityGridWithAddButton(
                    activities = activities,
                    onAddClick = { showAddActivitySheet = true }
                )

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Add Activity Bottom Sheet
    if (showAddActivitySheet) {
        AddActivityBottomSheet(
            onDismiss = { showAddActivitySheet = false },
            onAddActivity = { name, duration, distance ->
                val caloriesBurned = calculateCaloriesBurned(name, duration, distance)
                activities = activities + ActivityItem(
                    id = System.currentTimeMillis().toString(),
                    name = name,
                    caloriesBurned = caloriesBurned,
                    duration = duration,
                    distance = distance
                )
                showAddActivitySheet = false
            }
        )
    }
}

// ============ Burned Calorie Gauge Card ============

@Composable
fun BurnedCalorieGaugeCard(
    title: String,
    date: String,
    calories: Int,
    modifier: Modifier = Modifier
) {
    val maxCalories = 500 // Typical daily burn goal
    val progress = (calories.toFloat() / maxCalories).coerceIn(0f, 1f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF6B6B6B)
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B6B6B)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Gauge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                // Semi-circle gauge with dashed style
                Canvas(modifier = Modifier.size(180.dp)) {
                    val strokeWidth = 12.dp.toPx()
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

                    // Background arc (dashed)
                    drawArc(
                        color = Color(0xFFE8E8E8),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Progress arc
                    drawArc(
                        color = TealPrimary,
                        startAngle = 180f,
                        sweepAngle = 180f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Center content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = 20.dp)
                ) {
                    // Icon
                    Icon(
                        painter = painterResource(id = R.drawable.fire_4), // Lightning icon
                        contentDescription = null,
                        tint = BlueAccent,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calories
                    Text(
                        text = calories.toString().toFarsiDigits(),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp
                        ),
                        color = TextDark
                    )

                    Text(
                        text = "کیلو کالری",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B6B6B)
                    )
                }
            }
        }
    }
}

// ============ Add Activity Card ============

@Composable
fun AddActivityCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "اضافه کردن فعالیت جدید",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFF6B6B6B),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, TealPrimary, RoundedCornerShape(12.dp))
                    .background(Color.White, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.add),
                    contentDescription = "Add Activity",
                    tint = TealPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ============ Activity Grid with Add Button ============

@Composable
fun ActivityGridWithAddButton(
    activities: List<ActivityItem>,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Combine activities with add button as the last item
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Create rows of 2 items (activities + add button)
        val itemsWithAdd = activities + null // null represents the Add button

        itemsWithAdd.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowItems.forEach { item ->
                    if (item != null) {
                        // Activity card
                        ActivityCard(
                            activity = item,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // Add Activity card
                        AddActivityCard(
                            onClick = onAddClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                // Fill empty space if only one item in row
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ActivityCard(
    activity: ActivityItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with activity name and icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activity.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF6B6B6B)
                )
                Icon(
                    painter = painterResource(id = R.drawable.walk), // Activity icon
                    contentDescription = null,
                    tint = GreenAccent,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Duration - Number black, unit gray, aligned to end (right in RTL)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = activity.duration.toString().toFarsiDigits(),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "دقیقه",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B6B6B)
                )

            }

            // Details - with more spacing
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Calories burned
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Label on left (start in RTL = right side visually)
                    Text(
                        text = "کالری از بین رفته:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B6B6B)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Icon(
                            painter = painterResource(id = R.drawable.fire_4),
                            contentDescription = null,
                            tint = Color(0xFFFF5722),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "kc",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B6B6B)
                        )
                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = activity.caloriesBurned.toString().toFarsiDigits(),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = Color.Black
                        )


                    }
                }

                // Distance (if available)
                activity.distance?.let { dist ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Label on left (start in RTL = right side visually)
                        Text(
                            text = "مسافت طی شده:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B6B6B)
                        )
                        // Value on right (end in RTL = left side visually)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "km",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B6B6B)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = dist.toString().toFarsiDigits(),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============ Add Activity Bottom Sheet ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivityBottomSheet(
    onDismiss: () -> Unit,
    onAddActivity: (name: String, duration: Int, distance: Float?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current

    var activityName by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }

    val activityTypes = listOf("دویدن", "پیاده‌روی", "دوچرخه‌سواری", "شنا", "یوگا", "سایر")

    val isValid = activityName.isNotBlank() && duration.isNotBlank()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.LightGray)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "فعالیت جدید",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = TextDark
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .border(1.dp, Color.LightGray, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Activity Name Input
                OutlinedTextField(
                    value = activityName,
                    onValueChange = { activityName = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "دویدن",
                            color = Color(0xFF868686),
                        )
                    },
                    label = {
                        Text(
                            text = "نام فعالیت جستجو کنید",
                            color = Color(0xFF383838)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF5BA3A3),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Start,
                        color = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Duration and Distance Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Duration Input
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { if (it.all { c -> c.isDigit() || c in '۰'..'۹' }) duration = it },
                        modifier = Modifier.weight(0.5f),
                        placeholder = {
                            Text(
                                text = "۱۲۰",
                                color = Color(0xFF868686),
                            )
                        },
                        label = {
                            Text(
                                text = "دقیقه",
                                color = Color(0xFF383838)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF5BA3A3),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Start,
                            color = Color.Black
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // Distance Input
                    OutlinedTextField(
                        value = distance,
                        onValueChange = { distance = it },
                        modifier = Modifier.weight(0.5f),
                        placeholder = {
                            Text(
                                text = "۵ کیلومتر",
                                color = Color(0xFF868686),
                            )
                        },
                        label = {
                            Text(
                                text = "میزان",
                                color = Color(0xFF383838)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF5BA3A3),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Start,
                            color = Color.Black
                        )
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (isValid) {
                            val durationInt = duration.toIntOrNull() ?: 0
                            val distanceFloat = distance.toFloatOrNull()
                            onAddActivity(activityName, durationInt, distanceFloat)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TealPrimary,
                        disabledContainerColor = Color(0xFFE0E0E0)
                    ),
                    enabled = isValid
                ) {
                    Text(
                        text = "ثبت",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ============ Helper Functions ============

private fun calculateCaloriesBurned(activityName: String, duration: Int, distance: Float?): Int {
    // MET values for different activities
    val metValue = when {
        activityName.contains("دویدن") || activityName.contains("run", ignoreCase = true) -> 9.8
        activityName.contains("پیاده") || activityName.contains("walk", ignoreCase = true) -> 3.5
        activityName.contains("دوچرخه") || activityName.contains("cycle", ignoreCase = true) -> 7.5
        activityName.contains("شنا") || activityName.contains("swim", ignoreCase = true) -> 8.0
        activityName.contains("یوگا") || activityName.contains("yoga", ignoreCase = true) -> 3.0
        else -> 5.0 // Default moderate activity
    }

    // Calories burned = MET × weight (kg) × time (hours)
    // Assuming average weight of 70kg
    val weightKg = 70
    val timeHours = duration / 60.0

    return (metValue * weightKg * timeHours).toInt()
}