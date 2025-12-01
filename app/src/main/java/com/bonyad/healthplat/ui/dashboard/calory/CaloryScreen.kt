package com.bonyad.healthplat.ui.dashboard.calory

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.R
import com.bonyad.healthplat.ui.utils.toFarsiDigits
import java.util.UUID

data class FoodItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val calories: Int,
    val mealType: MealType,
    val imageRes: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MealType(val persianName: String) {
    BREAKFAST("صبحانه"),
    LUNCH("ناهار"),
    DINNER("شام"),
    SNACK("میان‌وعده")
}

// --- Colors from Design ---
val TealPrimary = Color(0xFF5BA3A3)
val BackgroundColor = Color(0xFFF8F8F8)
val TextDark = Color(0xFF2C2C2C)
val TextGray = Color(0xFF9E9E9E)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaloryScreen(
    viewModel: CaloryViewModel = hiltViewModel()
) {
    val consumedCalories by viewModel.consumedCalories.collectAsState()
    val burnedCalories by viewModel.burnedCalories.collectAsState()
    val foodItems by viewModel.foodItems.collectAsState()

    // Force RTL for this screen
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = { CaloryTopBar() },
            containerColor = BackgroundColor
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Date Strip Section
                DateStripSection()

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Stats Cards (Burned / Received)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Received (Green Icon)
                    StatsCard(
                        modifier = Modifier.weight(1f),
                        title = "کالری دریافتی",
                        value = consumedCalories,
                        iconRes = R.drawable.pills, // Replace with your apple drawable
                        iconTint = Color(0xFF4CAF50), // Green
                        timeRange = "۱۰:۲۵ تا ۱۷:۲۹"
                    )

                    // Burned (Blue/Lightning Icon)
                    StatsCard(
                        modifier = Modifier.weight(1f),
                        title = "کالری سوخته شده",
                        value = burnedCalories,
                        iconRes = R.drawable.pills, // Replace with your lightning drawable
                        iconTint = Color(0xFF2196F3), // Blue
                        timeRange = "۰۰:۰۱ تا ۱۷:۲۹"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Meals List
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MealType.values().forEach { mealType ->
                        val mealsInThisType = foodItems.filter { it.mealType == mealType }
                        MealSummaryCard(
                            mealType = mealType,
                            items = mealsInThisType,
                            onAddClick = { viewModel.onAddFoodClick(mealType) }
                        )
                    }
                }

                // Extra space for scrolling above bottom bar
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Your existing Bottom Sheet logic here
    val showAddFoodSheet by viewModel.showAddFoodSheet.collectAsState()
    val selectedMealType by viewModel.selectedMealType.collectAsState()

    if (showAddFoodSheet && selectedMealType != null) {
        AddFoodBottomSheet(
            mealType = selectedMealType!!,
            onDismiss = { viewModel.dismissAddFoodSheet() },
            onAddFood = { name, calories ->
                viewModel.addFoodItem(name, calories, selectedMealType!!)
            }
        )
    }
}

// --- 1. Top Bar ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaloryTopBar() {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "کالری",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextDark
            )
        },
        navigationIcon = {
            IconButton(onClick = { /* Info */ }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = Color.Gray
                )
            }
        },
        actions = {
            IconButton(onClick = { /* Notifications */ }) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.Gray
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = BackgroundColor
        )
    )
}

// --- 2. Date Strip ---
@Composable
fun DateStripSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header with Month Name and Calendar Icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.End, // Aligns "Mehr Mah" to right (start in RTL)
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "مهر ماه",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.Gray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Day List
        // Mock data: (DayNum, DayName, isSelected)
        val days = listOf(
            Triple("۱۶", "ج", false),
            Triple("۱۷", "ش", false),
            Triple("۱۸", "ی", false),
            Triple("۱۹", "د", false),
            Triple("۲۰", "س", false),
            Triple("۲۱", "چ", false),
            Triple("۲۲", "پ", true)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(days.size) { index ->
                val (num, name, selected) = days[index]
                DateItem(num, name, selected)
            }
        }
    }
}

@Composable
fun DateItem(dayNum: String, dayName: String, isSelected: Boolean) {
    Column(
        modifier = Modifier
            .width(50.dp)
            .height(70.dp)
            .background(
                color = if (isSelected) TealPrimary else Color.White,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = if (isSelected) Color.Transparent else Color(0xFFEEEEEE),
                shape = RoundedCornerShape(16.dp)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayNum,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isSelected) Color.White else TextDark
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dayName,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextGray
        )
    }
}

// --- 3. Stats Cards ---
@Composable
fun StatsCard(
    modifier: Modifier = Modifier,
    title: String,
    value: Int,
    iconRes: Int,
    iconTint: Color,
    timeRange: String
) {
    Card(
        modifier = modifier.height(150.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon (Apple or Lightning)
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(4.dp))
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.pills), // Needs a clock icon
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = TextGray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeRange.toFarsiDigits(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                }

                // Up Arrow Circle
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(TealPrimary.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// --- 4. Meal Summary Row (The Design Specifics) ---
@Composable
fun MealSummaryCard(
    mealType: MealType,
    items: List<FoodItem>,
    onAddClick: () -> Unit
) {
    val totalCalories = items.sumOf { it.calories }
    // Mocking a range for visual similarity to design "450 - 580"
    val calRange = if(totalCalories > 0) "${totalCalories - 100} _ $totalCalories" else "۰"

    val iconRes = when(mealType) {
        MealType.BREAKFAST -> R.drawable.pills // Replace with egg/toast icon
        MealType.LUNCH -> R.drawable.pills // Replace with chicken icon
        MealType.DINNER -> R.drawable.pills // Replace with soup icon
        MealType.SNACK -> R.drawable.pills // Replace with grape icon
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(85.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // LEFT SIDE (In RTL, this is the right side visually): Icon + Text
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Meal Icon Container
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, Color(0xFFEEEEEE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = Color.Unspecified, // Use original colors of drawable
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = mealType.persianName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$calRange کیلو کالری".toFarsiDigits(),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
            }

            // RIGHT SIDE (In RTL, this is the left side visually): Overlapping Images + Add Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((-12).dp) // Negative spacing for overlap
            ) {
                // Add Button (First item in the visual stack on the left)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.dp, TealPrimary, CircleShape)
                        .background(Color.White)
                        .clickable { onAddClick() }
                        .zIndex(10f), // Ensure it's on top
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = TealPrimary
                    )
                }

                // Food Images (Iterate backwards or handle z-index)
                items.take(3).forEachIndexed { index, item ->
                    // In a real app, use Coil/Glide for URLs.
                    // Using a placeholder Box for now based on design.
                    Image(
                        painter = painterResource(id = R.drawable.pills), // Your food image
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                            .zIndex((5 - index).toFloat()) // Stack order
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodBottomSheet(
    mealType: MealType,
    onDismiss: () -> Unit,
    onAddFood: (String, Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var foodName by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "افزودن به ${mealType.persianName}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = Color(0xFF2C2C2C),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Food name input
            OutlinedTextField(
                value = foodName,
                onValueChange = { foodName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("نام غذا") },
                placeholder = { Text("مثال: سیب") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF5BA3A3),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Calories input
            OutlinedTextField(
                value = calories,
                onValueChange = { if (it.all { char -> char.isDigit() }) calories = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("کالری") },
                placeholder = { Text("مثال: ۱۹۹") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF5BA3A3),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Add button
            Button(
                onClick = {
                    if (foodName.isNotBlank() && calories.isNotBlank()) {
                        onAddFood(foodName, calories.toInt())
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5BA3A3),
                    disabledContainerColor = Color(0xFFE0E0E0)
                ),
                enabled = foodName.isNotBlank() && calories.isNotBlank()
            ) {
                Text(
                    text = "افزودن",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}