package com.bonyad.healthplat.ui.dashboard.calory

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.bonyad.healthplat.R
import com.bonyad.healthplat.domain.model.FoodItemUi
import com.bonyad.healthplat.domain.model.MealType
import com.bonyad.healthplat.ui.utils.PersianDateUtils
import com.bonyad.healthplat.ui.utils.toFarsiDigits

// ============ Consumed Calories Detail Screen ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumedCaloriesScreen(
    viewModel: CaloryViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val consumedCalories by viewModel.consumedCalories.collectAsState()
    val calorieGoal by viewModel.calorieGoal.collectAsState()
    val allFoodItems by viewModel.allFoodItems.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Load full meal data (with dishes) on first composition
    LaunchedEffect(Unit) {
        viewModel.loadFullMealsForSelectedDate()
    }

    // Group items by meal type
    val groupedItems = allFoodItems.groupBy { it.mealType }

    // Get current Persian date
    val currentDate = remember { PersianDateUtils.getFormattedPersianDate() }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "کالری دریافتی",
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
            containerColor = BackgroundColor,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Calorie Gauge Card
                CalorieGaugeCard(
                    title = "کالری دریافتی",
                    date = currentDate,
                    calories = consumedCalories,
                    maxCalories = calorieGoal,
                    modifier = Modifier.padding(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Food Items by Meal Type
                MealType.values().forEach { mealType ->
                    val items = groupedItems[mealType] ?: emptyList()
                    if (items.isNotEmpty()) {
                        MealSection(
                            mealType = mealType,
                            items = items,
                            onEditClick = { /* TODO: Edit food item */ },
                            onDeleteClick = { viewModel.deleteFoodItem(it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// ============ Calorie Gauge Card ============

@Composable
fun CalorieGaugeCard(
    title: String,
    date: String,
    calories: Int,
    maxCalories: Int,
    modifier: Modifier = Modifier
) {
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
                    color = TextGray
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Gauge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                // Semi-circle gauge
                Canvas(modifier = Modifier.size(180.dp)) {
                    val strokeWidth = 12.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

                    // Background arc
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
                        painter = painterResource(id = R.drawable.fire_4),
                        contentDescription = null,
                        tint = TealPrimary,
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
                        color = TextGray
                    )
                }
            }
        }
    }
}

// ============ Meal Section ============

@Composable
fun MealSection(
    mealType: MealType,
    items: List<FoodItemUi>,
    onEditClick: (FoodItemUi) -> Unit,
    onDeleteClick: (FoodItemUi) -> Unit
) {
    // Outer white card with shadow — title inside
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(
                color = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Section title inside the card
            Text(
                text = mealType.persianName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextDark,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Food item cards
            items.forEachIndexed { index, item ->
                FoodItemRow(
                    item = item,
                    onEditClick = { onEditClick(item) },
                    onDeleteClick = { onDeleteClick(item) }
                )
                if (index < items.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// ============ Food Item Row ============

@Composable
fun FoodItemRow(
    item: FoodItemUi,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    // Individual bordered card for each food item
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = Color(0xFFD9D9D9),
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = Color.White,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Start side (right in RTL): Food image + info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Food image
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.LightGray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = resolveMealImageUrl(item.imageUrl),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.pills),
                        error = painterResource(R.drawable.pills),
                        fallback = painterResource(R.drawable.pills),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Food details
                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = TextDark
                    )
                    Text(
                        text = "${item.calories} کیلو کالری".toFarsiDigits(),
                        style = MaterialTheme.typography.bodySmall,
                        color = TealPrimary
                    )
                }
            }

            // End side (left in RTL): Action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Edit button
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.edit),
                        contentDescription = "Edit",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = "Delete",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}