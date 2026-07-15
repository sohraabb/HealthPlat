package com.bonyad.healthplat.ui.dashboard.calory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.bonyad.healthplat.R
import com.bonyad.healthplat.domain.model.MealSummaryUi
import com.bonyad.healthplat.domain.model.MealType
import com.bonyad.healthplat.ui.components.AddFoodBottomSheet
import com.bonyad.healthplat.ui.components.StandardFloatingActionButton
import com.bonyad.healthplat.ui.navigation.CaloryRoutes
import com.bonyad.healthplat.ui.utils.toFarsiDigits
import saman.zamani.persiandate.PersianDate
import java.text.SimpleDateFormat
import java.util.Locale

// ============ Design System Colors ============
val TealPrimary = Color(0xFF5BA3A3)
val BackgroundColor = Color(0xFFF8F8F8)
val CardBackground = Color(0xFFFFFFFF)
val TextDark = Color(0xFF2C2C2C)
val TextGray = Color(0xFF6B6B6B)
val TextLightGray = Color(0xFFBDBDBD)
val BorderColor = Color(0xFFEEEEEE)
val GreenAccent = Color(0xFF4CAF50)
val BlueAccent = Color(0xFF2196F3)

// ============ Main Screen ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaloryScreen(
    viewModel: CaloryViewModel = hiltViewModel(),
    // Option 1: Individual callbacks (for use in CaloryRoutes.Main)
    onNavigateToConsumed: () -> Unit = {},
    onNavigateToBurned: () -> Unit = {},
    onNavigateToScan: () -> Unit = {},
    // Option 2: Route-based navigation (for use in Dashboard bottom nav)
    onNavigateToRoute: ((String) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val consumedCalories by viewModel.consumedCalories.collectAsState()
    val burnedCalories by viewModel.burnedCalories.collectAsState()
    val mealSummaries by viewModel.mealSummaries.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val dateItems by viewModel.dateItems.collectAsState()
    val showAddFoodSheet by viewModel.showAddFoodSheet.collectAsState()
    val selectedMealType by viewModel.selectedMealType.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()


    val snackbarHostState = remember { SnackbarHostState() }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToConsumedDetails.collect {
            if (onNavigateToRoute != null) {
                onNavigateToRoute("calory_consumed_details")
            } else {
                onNavigateToConsumed()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToBurnedDetails.collect {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(viewModel.selectedDate.value)
            val route = CaloryRoutes.BurnedDetails.createRoute(dateStr)
            if (onNavigateToRoute != null) {
                onNavigateToRoute(route)
            } else {
                onNavigateToBurned()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToFoodScan.collect { mealType ->
            val route = CaloryRoutes.FoodScan.createRoute(mealType.name)
            if (onNavigateToRoute != null) {
                onNavigateToRoute(route)
            } else {
                onNavigateToScan()
            }
        }
    }

    // Refresh data when returning from another screen (e.g., ScanResult saved a meal)
    val lifecycleOwner = LocalLifecycleOwner.current
    var wasStoppedBefore by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> wasStoppedBefore = true
                Lifecycle.Event.ON_RESUME -> {
                    if (wasStoppedBefore) {
                        viewModel.loadMealsForSelectedDate()
                        viewModel.loadActivitiesForSelectedDate()
                        wasStoppedBefore = false
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Force RTL for this screen
    Scaffold(
        topBar = { CaloryTopBar() },
        containerColor = BackgroundColor,
        snackbarHost = {
            // Position snackbar above bottom navigation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp) // Account for bottom nav height
            ) {
                SnackbarHost(snackbarHostState)
            }
        },
        floatingActionButton = { }
    ) { paddingValues ->

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()  // NO paddingValues here
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)  // paddingValues goes here instead
            ) {
                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. More space between appbar and date section
                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Date Strip Section with white rounded background
                    DateStripSection(
                        currentMonth = currentMonth,
                        dateItems = dateItems,
                        onDateSelected = { viewModel.onDateSelected(it.date) }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 2. Stats Cards
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        // Burned Calories Card
                        CalorieStatsCard(
                            modifier = Modifier.weight(1f),
                            title = "کالری سوخته شده",
                            value = burnedCalories,
                            iconRes = R.drawable.flash,
                            iconTint = BlueAccent,
                            timeRange = "",
                            onClick = { viewModel.onBurnedCaloriesClick() }
                        )

                        // Received Calories Card
                        CalorieStatsCard(
                            modifier = Modifier.weight(1f),
                            title = "کالری دریافتی",
                            value = consumedCalories,
                            iconRes = R.drawable.apple,
                            iconTint = GreenAccent,
                            timeRange = "",
                            onClick = { viewModel.onConsumedCaloriesClick() }
                        )
                    }


                    Spacer(modifier = Modifier.height(24.dp))

                    // 3. Meals List
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MealType.entries.forEach { mealType ->
                            val summary = mealSummaries[mealType] ?: MealSummaryUi(
                                mealType = mealType,
                                items = emptyList(),
                                totalCaloriesMin = 0,
                                totalCaloriesMax = 0
                            )
                            MealSummaryCard(
                                mealSummary = summary,
                                onAddClick = { viewModel.onAddFoodClick(mealType) }
                            )
                        }
                    }

                    // Extra space for scrolling above bottom bar
                    Spacer(modifier = Modifier.height(100.dp))
                }

                // Loading overlay — drawn after content so it appears on top
                AnimatedVisibility(
                    visible = uiState is CaloryUiState.Loading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TealPrimary)
                    }
                }
            }
        }
    }


    // Add Food Bottom Sheet
    if (showAddFoodSheet && selectedMealType != null) {
        AddFoodBottomSheet(
            mealType = selectedMealType!!,
            onDismiss = { viewModel.dismissAddFoodSheet() },
            onScanFood = {
                viewModel.dismissAddFoodSheet()
                viewModel.onScanFoodClick(selectedMealType!!)
            },
            onAddFood = { name, caloriesMin, caloriesMax, amount, unit ->
                viewModel.addFoodItem(name, caloriesMin, caloriesMax, amount, unit)
            }
        )
    }
}

// ============ Top Bar ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaloryTopBar() {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "کالری",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = TextDark
            )
        },
        navigationIcon = {
            IconButton(onClick = { /* Info */ }) {
                Icon(
                    painter = painterResource(R.drawable.info_circle),
                    contentDescription = "اطلاعات",
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF6B6B6B)
                )
            }
        },
        actions = {
            IconButton(onClick = { /* Notifications */ }) {
                Icon(
                    painter = painterResource(R.drawable.notification),
                    contentDescription = "Notifications",
                    tint = Color.Black
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = BackgroundColor
        ),
        windowInsets = WindowInsets(top = 8.dp)
    )
}

// ============ Date Strip Section ============

@Composable
fun DateStripSection(
    currentMonth: String,
    dateItems: List<DateItem>,
    onDateSelected: (DateItem) -> Unit
) {
    // White rounded background container for the entire date section
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(
                color = CardBackground,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(vertical = 16.dp)
    ) {
        // Header with Month Name and Calendar Icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Navigation arrows
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.circle_left),
                    contentDescription = "Previous",
                    tint = Color(0xFF6B6B6B),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { /* Navigate to previous week */ }
                )
                Icon(
                    painter = painterResource(R.drawable.circle_right),
                    contentDescription = "Next",
                    tint = Color(0xFF6B6B6B),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { /* Navigate to next week */ }
                )
            }

            // Month and Calendar
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentMonth,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextGray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.calendar),
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Day List - Using the working date logic
        DateStrip(
            selectedOffset = dateItems.indexOfFirst { it.isSelected }.let {
                if (it >= 0) -(dateItems.size - 1 - it) else 0
            },
            onDaySelected = { offset ->
                // Find the date item matching this offset and call onDateSelected
                val index = dateItems.size - 1 + offset
                if (index in dateItems.indices) {
                    onDateSelected(dateItems[index])
                }
            }
        )
    }
}

// Working DateStrip implementation
@Composable
fun DateStrip(
    selectedOffset: Int,  // 0 = today, -1 = yesterday, -2 = day before, etc.
    onDaySelected: (Int) -> Unit
) {
    val today = PersianDate()

    // Persian weekday letters: Saturday=ش, Sunday=ی, Monday=د, etc.
    val weekDayNames = mapOf(
        0 to "ش",  // Saturday
        1 to "ی",  // Sunday
        2 to "د",  // Monday
        3 to "س",  // Tuesday
        4 to "چ",  // Wednesday
        5 to "پ",  // Thursday
        6 to "ج"   // Friday
    )

    // Generate last 7 days: -6 (oldest) to 0 (today)
    // Reversed so today appears on the right (RTL)
    val days = (-6..0).map { offset ->
        val date = PersianDate(today.time).apply {
            if (offset < 0) subDays(-offset)
        }
        Pair(offset, date)
    }.reversed() // Reversed: today first (right side in RTL Row)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { (offset, date) ->
            val isSelected = offset == selectedOffset
            val dayOfWeek = date.dayOfWeek()

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(70.dp)
                    .background(
                        if (isSelected) TealPrimary else Color.White,
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        1.dp,
                        if (isSelected) Color.Transparent else BorderColor,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable {
                        onDaySelected(offset)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = date.shDay.toString().toFarsiDigits(),
                    color = if (isSelected) Color.White else TextGray,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = weekDayNames[dayOfWeek] ?: "",
                    color = if (isSelected) Color.White else TextGray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun DateItemCard(
    item: DateItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(50.dp)
            .height(70.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                color = if (item.isSelected) TealPrimary else CardBackground
            )
            .border(
                width = if (item.isSelected) 0.dp else 1.dp,
                color = if (item.isSelected) Color.Transparent else BorderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = item.dayNumber,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (item.isSelected) Color.White else TextDark
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.dayName,
            style = MaterialTheme.typography.bodySmall,
            color = if (item.isSelected) Color.White.copy(alpha = 0.8f) else TextGray
        )
    }
}

// ============ Stats Card ============

@Composable
fun CalorieStatsCard(
    modifier: Modifier = Modifier,
    title: String,
    value: Int,
    iconRes: Int,
    iconTint: Color,
    timeRange: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(150.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Icon on START (left in RTL), Title on END (right in RTL)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon without circle background (on START)
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )

                // Title on END
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
            }

            // Middle: Value with kcal
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = value.toString().toFarsiDigits(),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            // Bottom Row: Time range and arrow button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.circle_clock),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = TextGray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeRange,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                }

                // Bigger Up Arrow Button (more height)
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TealPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_up),
                        contentDescription = "Details",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ============ Meal Summary Card ============

@Composable
fun MealSummaryCard(
    mealSummary: MealSummaryUi,
    onAddClick: () -> Unit
) {
    val calRange = if (mealSummary.totalCaloriesMax > 0) {
        "${mealSummary.totalCaloriesMin} _ ${mealSummary.totalCaloriesMax}".toFarsiDigits()
    } else {
        "۰"
    }

    // Get meal icon based on type
    val iconRes = when (mealSummary.mealType) {
        MealType.BREAKFAST -> R.drawable.breakfast
        MealType.LUNCH -> R.drawable.lunch
        MealType.DINNER -> R.drawable.dinner
        MealType.SNACK -> R.drawable.brunch
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(85.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // LEFT SIDE (in RTL this appears on right): Add Button + Food Images
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((-12).dp)
            ) {
                // Add Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.dp, TealPrimary, CircleShape)
                        .background(Color.White)
                        .clickable { onAddClick() }
                        .zIndex(10f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add),
                        contentDescription = "Add",
                        tint = TealPrimary
                    )
                }

                // Food Images (overlapping)
                mealSummary.items.take(3).forEachIndexed { index, item ->
                    FoodImageCircle(
                        imageUrl = item.imageUrl,
                        modifier = Modifier.zIndex((5 - index).toFloat())
                    )
                }
            }

            // RIGHT SIDE (in RTL): Title on top, Icon + Calories below
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Title on top-end
                Text(
                    text = mealSummary.mealType.persianName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Icon and calory value next to each other
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$calRange کیلو کالری",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FoodImageCircle(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .border(2.dp, Color.White, CircleShape)
            .background(Color.LightGray.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        // Load the real meal image; fall back to the placeholder when missing or on error.
        AsyncImage(
            model = resolveMealImageUrl(imageUrl),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.pills),
            error = painterResource(R.drawable.pills),
            fallback = painterResource(R.drawable.pills),
            modifier = Modifier.fillMaxSize()
        )
    }
}

/** Host the meal images are served from — mirrors NetworkModule's MAIN_BASE_URL origin. */
private const val IMAGE_HOST = "http://192.168.18.165:7005"

/**
 * Resolve a meal image reference into a loadable URL. The server may return an absolute URL,
 * a root-relative path ("/Images/..."), or a bare path; handle all three.
 */
fun resolveMealImageUrl(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return when {
        raw.startsWith("http://", true) || raw.startsWith("https://", true) -> raw
        raw.startsWith("/") -> IMAGE_HOST + raw
        else -> "$IMAGE_HOST/$raw"
    }
}

// ============ Floating Scan Button ============

@Composable
fun FloatingScanButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, TealPrimary, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.camera_scan),
            contentDescription = "Scan Food",
            tint = TealPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}