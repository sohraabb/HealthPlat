package com.bonyad.healthplat.ui.dashboard.calory

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

import com.bonyad.healthplat.domain.model.FoodFactData
import com.bonyad.healthplat.domain.model.FoodScanResult
import com.bonyad.healthplat.domain.model.FoodTotalFacts
import com.bonyad.healthplat.domain.model.HealthQuality
import com.bonyad.healthplat.domain.model.MealType
import com.bonyad.healthplat.domain.model.ScannedDish
import com.bonyad.healthplat.ui.utils.rtl
import com.bonyad.healthplat.ui.utils.toFarsiDigits
import com.bonyad.healthplat.R
import coil3.compose.AsyncImage
import kotlin.math.roundToInt

// Design colors
private val ErrorRed = Color(0xFFE57373)
private val SelectorBg = Color(0xFFF3F4F6)
private val NutrientProteinColor = Color(0xFFE57373) // red
private val NutrientCarbColor = Color(0xFFE57373) // red
private val NutrientFatColor = Color(0xFF64B5F6) // blue
private val NutrientSugarColor = Color(0xFFFFA726) // orange
private val NutrientFiberColor = Color(0xFF81C784) // green

// Max reference for bar scaling (just for visual proportion)
private const val NUTRIENT_BAR_MAX_G = 100.0

/**
 * Scan Result Screen - Shows AI analysis of scanned food
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(
    imageUri: Uri,
    initialMealType: MealType = MealType.LUNCH,
    viewModel: CaloryViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onSaveComplete: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Scan state from ViewModel
    val scanState by viewModel.foodScanState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val editUnitOptions by viewModel.editUnitOptions.collectAsState()
    val isEditUnitsLoading by viewModel.isEditUnitsLoading.collectAsState()

    // Local state
    var isDetailsExpanded by remember { mutableStateOf(false) }
    var isNutrientExpanded by remember { mutableStateOf(true) }
    var isFavorite by remember { mutableStateOf(false) }
    var selectedMealType by remember { mutableStateOf(initialMealType) }

    // Editable dish list
    val editableDishes = remember { mutableStateListOf<ScannedDish>() }

    // Add new item bottom sheet
    var showAddItemSheet by remember { mutableStateOf(false) }

    // Index of the dish currently being edited (amount/unit), null = no sheet
    var editingDishIndex by remember { mutableStateOf<Int?>(null) }

    // Trigger scan when screen opens
    LaunchedEffect(imageUri) {
        viewModel.scanFood(imageUri)
    }

    // Navigate after save completes successfully
    LaunchedEffect(Unit) {
        viewModel.navigateAfterSave.collect {
            onSaveComplete()
        }
    }

    // Update editable dishes when scan completes
    LaunchedEffect(scanState) {
        if (scanState is FoodScanState.Success) {
            val result = (scanState as FoodScanState.Success).result
            editableDishes.clear()
            editableDishes.addAll(result.dishes)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Food Image Header
            FoodImageHeader(
                imageUri = imageUri,
                mealName = when (val state = scanState) {
                    is FoodScanState.Success -> state.result.mealName
                    else -> "در حال تحلیل...".rtl()
                },
                isFavorite = isFavorite,
                onFavoriteToggle = { isFavorite = !isFavorite },
                onDismiss = onDismiss,
                onEditName = { /* TODO: Show edit dialog */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Content based on scan state
            when (val state = scanState) {
                is FoodScanState.Loading -> {
                    LoadingContent()
                }

                is FoodScanState.Success -> {
                    // Health Assessment Card (RTL)
                    HealthAssessmentCard(
                        healthQuality = state.result.healthQuality,
                        healthScore = state.result.healthQuality.score * 25,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live totals reflect any amount/unit edits the user makes below.
                    // Each macro stays null when no dish reports it, so the UI can hide it.
                    val liveTotals = FoodTotalFacts(
                        cal = editableDishes.sumOf { (it.caloriesMin + it.caloriesMax) / 2.0 },
                        fat = editableDishes.sumNutrientOrNull { it.fat },
                        protein = editableDishes.sumNutrientOrNull { it.protein },
                        carb = editableDishes.sumNutrientOrNull { it.carb },
                        fiber = editableDishes.sumNutrientOrNull { it.fiber },
                        sugar = editableDishes.sumNutrientOrNull { it.sugar }
                    )

                    // Nutrient Separation Card (expandable)
                    NutrientSeparationCard(
                        isExpanded = isNutrientExpanded,
                        onToggleExpand = { isNutrientExpanded = !isNutrientExpanded },
                        totalFacts = liveTotals,
                        dishes = editableDishes,
                        onEditDish = { index ->
                            if (index in editableDishes.indices) {
                                editingDishIndex = index
                                viewModel.loadUnitOptionsFor(editableDishes[index])
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Meal Details Card (expandable)
                    MealDetailsCard(
                        isExpanded = isDetailsExpanded,
                        onToggleExpand = { isDetailsExpanded = !isDetailsExpanded },
                        selectedMealType = selectedMealType,
                        onMealTypeChange = { selectedMealType = it },
                        dishes = editableDishes,
                        onRemoveDish = { index ->
                            if (index in editableDishes.indices) {
                                editableDishes.removeAt(index)
                            }
                        },
                        onAddDish = { showAddItemSheet = true },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Delete All Button
                    if (editableDishes.isNotEmpty()) {
                        DeleteAllButton(
                            onClick = { editableDishes.clear() },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                is FoodScanState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.scanFood(imageUri) }
                    )
                }

                else -> {}
            }

            Spacer(modifier = Modifier.height(100.dp)) // Space for button
        }

        // Save Button (fixed at bottom)
        if (scanState is FoodScanState.Success && editableDishes.isNotEmpty()) {
            SaveButton(
                isLoading = isSaving,
                onClick = {
                    viewModel.saveScanResult(
                        mealType = selectedMealType,
                        dishes = editableDishes.toList()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }

        // Add Item Bottom Sheet
        if (showAddItemSheet) {
            AddDishBottomSheet(
                onDismiss = { showAddItemSheet = false },
                onAddDish = { name, calories ->
                    editableDishes.add(
                        ScannedDish(
                            name = name,
                            caloriesMin = calories,
                            caloriesMax = calories
                        )
                    )
                    showAddItemSheet = false
                }
            )
        }

        // Edit Dish (amount/unit) Bottom Sheet
        editingDishIndex?.let { idx ->
            if (idx in editableDishes.indices) {
                EditDishBottomSheet(
                    dish = editableDishes[idx],
                    unitOptions = editUnitOptions,
                    isLoadingUnits = isEditUnitsLoading,
                    onDismiss = {
                        editingDishIndex = null
                        viewModel.clearUnitOptions()
                    },
                    onSave = { newAmount, selectedFact ->
                        val base = editableDishes[idx]
                        editableDishes[idx] = if (selectedFact != null) {
                            recomputeDishFromFact(base, selectedFact, newAmount)
                        } else {
                            recomputeDishByAmount(base, newAmount)
                        }
                        editingDishIndex = null
                        viewModel.clearUnitOptions()
                    }
                )
            }
        }
    }
}

@Composable
private fun FoodImageHeader(
    imageUri: Uri,
    mealName: String,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onDismiss: () -> Unit,
    onEditName: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color(0xFFE0E0E0))
    ) {
        // Captured/selected food photo (bottom layer)
        AsyncImage(
            model = imageUri,
            contentDescription = "تصویر غذا",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        // Close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .padding(top = 48.dp, start = 16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "بستن",
                tint = TextDark
            )
        }

        // Favorite button
        IconButton(
            onClick = onFavoriteToggle,
            modifier = Modifier
                .padding(top = 48.dp, end = 16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
                .align(Alignment.TopEnd)
        ) {
            Icon(
                painter = painterResource(R.drawable.star),
                contentDescription = if (isFavorite) "حذف از علاقه‌مندی" else "افزودن به علاقه‌مندی",
                tint = if (isFavorite) Color(0xFFFFD700) else TextGray
            )
        }

        // Meal name label
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF666666).copy(alpha = 0.8f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = mealName,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                    color = Color.White
                )
            }
        }

        // Edit button
        IconButton(
            onClick = onEditName,
            modifier = Modifier
                .padding(bottom = 16.dp, end = 16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
                .align(Alignment.BottomEnd)
        ) {
            Icon(
                painter = painterResource(R.drawable.pen),
                contentDescription = "ویرایش",
                tint = TextDark,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ============ Health Assessment Card (RTL) ============

@Composable
private fun HealthAssessmentCard(
    healthQuality: HealthQuality,
    healthScore: Int,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header: title on right, badge on left (RTL)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ارزیابی سلامت",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = TextDark
                    )

                    // Health score badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(TealPrimary.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = healthQuality.persianName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = TealPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${healthScore.toString().toFarsiDigits()}%",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = TealPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Health quality indicators (RTL: fills from right)
                HealthQualityIndicator(currentQuality = healthQuality)
            }
        }
    }
}

@Composable
private fun HealthQualityIndicator(
    currentQuality: HealthQuality
) {
    // RTL: first item = rightmost. Scale: محدود (right) → مغذی (left)
    // Progress fills from right (start in RTL) toward left
    val qualities = listOf(
        HealthQuality.LIMITED to "محدود",
        HealthQuality.MODERATE to "متوسط",
        HealthQuality.GOOD to "خوب",
        HealthQuality.NUTRITIOUS to "مغذی"
    )

    Column {
        // Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            qualities.forEach { (quality, label) ->
                val isSelected = quality == currentQuality
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isSelected) TealPrimary else TextGray
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress bar — fills from right in RTL
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE0E0E0))
        ) {
            val progress = when (currentQuality) {
                HealthQuality.LIMITED -> 0.25f
                HealthQuality.MODERATE -> 0.5f
                HealthQuality.GOOD -> 0.75f
                HealthQuality.NUTRITIOUS -> 1f
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(TealPrimary)
                    .align(Alignment.CenterStart) // RTL context makes this right-aligned
            )
        }
    }
}

// ============ Nutrient Separation Card ============

@Composable
private fun NutrientSeparationCard(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    totalFacts: FoodTotalFacts,
    dishes: List<ScannedDish>,
    onEditDish: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExpand() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تفکیک مواد مغذی",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = TextDark
                    )

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "بستن" else "باز کردن",
                        tint = TextGray
                    )
                }

                // Expandable content
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                    ) {
                        // Total nutrients summary
                        Text(
                            text = "مجموع مواد مغذی",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextDark,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Total calories & protein summary boxes. Calories always show;
                        // total protein is hidden when no dish reported protein (null).
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SummaryStatBox(
                                title = "کالری کل",
                                value = "${totalFacts.cal.toInt().toString().toFarsiDigits()} کالری",
                                modifier = if (totalFacts.protein != null) Modifier.weight(1f) else Modifier.fillMaxWidth()
                            )
                            totalFacts.protein?.let { proteinTotal ->
                                SummaryStatBox(
                                    title = "پروتئین کل",
                                    value = "${formatNutrientValue(proteinTotal)} گرم",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Nutrient bars — only those the server reported (null = hidden,
                        // zero is a real value and is shown).
                        val bars = listOfNotNull(
                            totalFacts.protein?.let { Triple("پروتئین", it, NutrientProteinColor) },
                            totalFacts.carb?.let { Triple("کربوهیدرات", it, NutrientCarbColor) },
                            totalFacts.fat?.let { Triple("چربی", it, NutrientFatColor) },
                            totalFacts.sugar?.let { Triple("قند", it, NutrientSugarColor) },
                            totalFacts.fiber?.let { Triple("فیبر", it, NutrientFiberColor) }
                        )
                        val maxVal = (bars.map { it.second } + NUTRIENT_BAR_MAX_G).maxOrNull() ?: NUTRIENT_BAR_MAX_G
                        bars.forEachIndexed { index, (label, value, color) ->
                            NutrientBarRow(
                                label = label,
                                value = value,
                                maxValue = maxVal,
                                color = color
                            )
                            if (index < bars.lastIndex) {
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }

                        // Dish ingredients section
                        if (dishes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "مواد تشکیل دهنده",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = TextDark
                                )

                                // Count badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(TealPrimary.copy(alpha = 0.1f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${dishes.size.toString().toFarsiDigits()} مورد",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = TealPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Per-dish cards
                            dishes.forEachIndexed { index, dish ->
                                DishNutrientCard(
                                    dish = dish,
                                    onEdit = { onEditDish(index) }
                                )
                                if (index < dishes.lastIndex) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NutrientBarRow(
    label: String,
    value: Double,
    maxValue: Double,
    color: Color
) {
    val progress = if (maxValue > 0) (value / maxValue).toFloat().coerceIn(0f, 1f) else 0f

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Label on right (RTL start)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = TextDark
            )

            // Value on left (RTL end)
            Text(
                text = "${formatNutrientValue(value)} گرم",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = TextDark
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFFE0E0E0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
                    .align(Alignment.CenterStart)
            )
        }
    }
}

@Composable
private fun DishNutrientCard(
    dish: ScannedDish,
    onEdit: () -> Unit
) {
    val avgCal = (dish.caloriesMin + dish.caloriesMax) / 2

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Dish header: icon + name + portion + calories
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Name and portion
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dish.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextDark
                    )
                    Text(
                        text = "${dish.amount.toString().toFarsiDigits()} ${dish.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }

                // Calories + edit pen (pen sits to the left of the calories in RTL)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = avgCal.toString().toFarsiDigits(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextDark
                        )
                        Text(
                            text = "کالری",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(SelectorBg)
                            .clickable { onEdit() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.pen),
                            contentDescription = "ویرایش مقدار",
                            tint = TextGray,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Nutrient rows for this dish — hide nutrients with no data (null), keep zeros
            val nutrientRows = listOfNotNull(
                dish.protein?.let { Triple("پروتئین", it, NutrientProteinColor) },
                dish.carb?.let { Triple("کربوهیدرات", it, NutrientCarbColor) },
                dish.fat?.let { Triple("چربی", it, NutrientFatColor) }
            )
            nutrientRows.forEachIndexed { index, (label, value, color) ->
                DishNutrientRow(label, value, color)
                if (index < nutrientRows.lastIndex) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Sugar and fiber chips — only those the server reported
            val chips = listOfNotNull(
                dish.sugar?.let { Triple("قند", it, NutrientSugarColor) },
                dish.fiber?.let { Triple("فیبر", it, NutrientFiberColor) }
            )
            if (chips.isNotEmpty()) {
                if (nutrientRows.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    chips.forEach { (label, value, color) ->
                        NutrientChip(
                            label = label,
                            value = value,
                            color = color,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DishNutrientRow(
    label: String,
    value: Double,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextGray
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Small colored dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${formatNutrientValue(value)}g",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = TextDark
            )
        }
    }
}

@Composable
private fun NutrientChip(
    label: String,
    value: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextGray
        )
        Text(
            text = "${formatNutrientValue(value)}g",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = TextDark
        )
    }
}

private fun formatNutrientValue(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toInt().toString().toFarsiDigits()
    } else {
        String.format("%.1f", value).toFarsiDigits()
    }
}

/**
 * Sums a nullable nutrient across dishes while preserving the "no data" distinction:
 * returns null when every dish reports null (so the UI can hide the nutrient), otherwise
 * the sum of the present values (a dish with 0.0 still counts as real data).
 */
private fun List<ScannedDish>.sumNutrientOrNull(selector: (ScannedDish) -> Double?): Double? {
    val present = mapNotNull(selector)
    return if (present.isEmpty()) null else present.sum()
}

@Composable
private fun SummaryStatBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SelectorBg)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TealPrimary
            )
        }
    }
}

// ============ Meal Details Card ============

@Composable
private fun MealDetailsCard(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    selectedMealType: MealType,
    onMealTypeChange: (MealType) -> Unit,
    dishes: List<ScannedDish>,
    onRemoveDish: (Int) -> Unit,
    onAddDish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMealTypeDropdown by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header (always visible)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExpand() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "جزئیات وعده",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = TextDark
                    )

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "بستن" else "باز کردن",
                        tint = TextGray
                    )
                }

                // Expandable content
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                    ) {
                        // Time and Meal Type selectors — styled as filled chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Meal Type Selector
                            Box(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SelectorBg)
                                        .clickable { showMealTypeDropdown = true }
                                        .padding(horizontal = 14.dp, vertical = 14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedMealType.persianName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextDark
                                        )
                                        Icon(
                                            painter = painterResource(R.drawable.back_arrow),
                                            contentDescription = null,
                                            tint = TextGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showMealTypeDropdown,
                                    onDismissRequest = { showMealTypeDropdown = false }
                                ) {
                                    MealType.values().forEach { mealType ->
                                        DropdownMenuItem(
                                            text = { Text(mealType.persianName) },
                                            onClick = {
                                                onMealTypeChange(mealType)
                                                showMealTypeDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Time display
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SelectorBg)
                                    .padding(horizontal = 14.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "امروز، ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(System.currentTimeMillis()).toFarsiDigits()}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextDark
                                    )
                                    Icon(
                                        painter = painterResource(R.drawable.back_arrow),
                                        contentDescription = null,
                                        tint = TextGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Food Items Label
                        Text(
                            text = "اقلام غذایی",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = TextDark,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start // RTL start = right
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Food Items List
                        dishes.forEachIndexed { index, dish ->
                            DishItemRow(
                                dish = dish,
                                onRemove = { onRemoveDish(index) }
                            )
                            if (index < dishes.lastIndex) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Add Item Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF0F0F0))
                                .clickable { onAddDish() }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "افزودن قلم غذایی",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = TextGray
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "افزودن",
                                    tint = TextGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DishItemRow(
    dish: ScannedDish,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8F8F8))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dish name (RTL start = right)
        Text(
            text = dish.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = TextDark,
            modifier = Modifier.weight(1f)
        )

        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "حذف",
                tint = TextGray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ============ Bottom section composables ============

@Composable
private fun DeleteAllButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "حذف",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = ErrorRed
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.delete),
                contentDescription = "حذف همه",
                tint = ErrorRed,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SaveButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TealPrimary,
            disabledContainerColor = TealPrimary.copy(alpha = 0.6f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "تایید",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = Color.White
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = TealPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "در حال تحلیل تصویر...".rtl(),
                style = MaterialTheme.typography.bodyLarge,
                color = TextGray
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = ErrorRed,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
        ) {
            Text("تلاش مجدد")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDishBottomSheet(
    onDismiss: () -> Unit,
    onAddDish: (name: String, calories: Int) -> Unit
) {
    var dishName by remember { mutableStateOf("") }
    var caloriesText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "افزودن قلم غذایی",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextDark,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = dishName,
                onValueChange = { dishName = it },
                label = { Text("نام غذا") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = caloriesText,
                onValueChange = { caloriesText = it.filter { c -> c.isDigit() } },
                label = { Text("کالری (اختیاری)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (dishName.isNotBlank()) {
                        onAddDish(dishName, caloriesText.toIntOrNull() ?: 0)
                    }
                },
                enabled = dishName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealPrimary,
                    disabledContainerColor = Color(0xFFE0E0E0)
                )
            ) {
                Text(
                    text = "افزودن",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Bottom sheet to edit a scanned dish's amount and unit.
 *
 * The unit dropdown is populated from the food-facts DB ([unitOptions]); picking a unit and
 * entering an amount recomputes calories/macros exactly from that unit's per-amount values.
 * If no DB rows are available (search failed/offline), only the amount can be edited and the
 * nutrition is scaled linearly from the dish's existing values.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDishBottomSheet(
    dish: ScannedDish,
    unitOptions: List<FoodFactData>,
    isLoadingUnits: Boolean,
    onDismiss: () -> Unit,
    onSave: (amount: Double, selectedFact: FoodFactData?) -> Unit
) {
    var amountText by remember(dish) { mutableStateOf(dish.amount.toString()) }
    var selectedFact by remember(dish, unitOptions) {
        mutableStateOf(
            unitOptions.firstOrNull { it.id == dish.foodFactId }
                ?: unitOptions.firstOrNull { it.unit == dish.unit }
        )
    }
    var unitDropdownOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardBackground
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header: title (right) + close (left)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ویرایش مقدار",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextDark
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SelectorBg)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = TextGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = dish.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Amount field
                Text(
                    text = "مقدار",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        amountText = input.filter { it.isDigit() || it == '.' }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Unit dropdown
                Text(
                    text = "واحد",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                            .clickable(enabled = unitOptions.isNotEmpty() && !isLoadingUnits) {
                                unitDropdownOpen = true
                            }
                            .padding(horizontal = 14.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedFact?.unit ?: dish.unit,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextDark
                        )
                        if (isLoadingUnits) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = TealPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = TextGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = unitDropdownOpen,
                        onDismissRequest = { unitDropdownOpen = false }
                    ) {
                        unitOptions.forEach { fact ->
                            DropdownMenuItem(
                                text = {
                                    Text("${fact.unit} (${fact.amount.toString().toFarsiDigits()})")
                                },
                                onClick = {
                                    selectedFact = fact
                                    unitDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons (RTL: ذخیره on right, انصراف on left)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: dish.amount.toDouble()
                            onSave(amount.coerceAtLeast(0.0), selectedFact)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Text(
                            text = "ذخیره",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "انصراف",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = TextGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Recompute a dish from an exact food-fact row (the chosen unit), scaled to [amount].
 * The fact's nutrition is stated per [FoodFactData.amount], so scale = amount / fact.amount.
 */
private fun recomputeDishFromFact(
    base: ScannedDish,
    fact: FoodFactData,
    amount: Double
): ScannedDish {
    val scale = if (fact.amount > 0) amount / fact.amount else amount
    val cal = fact.cal * scale
    return base.copy(
        amount = amount.roundToInt().coerceAtLeast(1),
        unit = fact.unit,
        portion = amount,
        caloriesMin = (cal * 0.9).roundToInt(),
        caloriesMax = (cal * 1.1).roundToInt(),
        fat = fact.fat?.times(scale),
        protein = fact.protein?.times(scale),
        carb = fact.carb?.times(scale),
        fiber = fact.fiber?.times(scale),
        sugar = fact.sugar?.times(scale),
        foodFactId = fact.id
    )
}

/**
 * Fallback recompute when no DB unit row is available (search failed/offline):
 * scale the dish's existing nutrition linearly by the new [amount], keeping the same unit.
 */
private fun recomputeDishByAmount(
    base: ScannedDish,
    amount: Double
): ScannedDish {
    val basis = if (base.portion > 0) base.portion else base.amount.toDouble().coerceAtLeast(1.0)
    val scale = amount / basis
    val avgCal = (base.caloriesMin + base.caloriesMax) / 2.0
    val cal = avgCal * scale
    return base.copy(
        amount = amount.roundToInt().coerceAtLeast(1),
        portion = amount,
        caloriesMin = (cal * 0.9).roundToInt(),
        caloriesMax = (cal * 1.1).roundToInt(),
        fat = base.fat?.times(scale),
        protein = base.protein?.times(scale),
        carb = base.carb?.times(scale),
        fiber = base.fiber?.times(scale),
        sugar = base.sugar?.times(scale)
    )
}

/**
 * Food Scan State sealed class for ViewModel
 */
sealed class FoodScanState {
    object Idle : FoodScanState()
    object Loading : FoodScanState()
    data class Success(val result: FoodScanResult) : FoodScanState()
    data class Error(val message: String) : FoodScanState()
}
