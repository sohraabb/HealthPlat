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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

import com.bonyad.healthplat.domain.model.FoodScanResult
import com.bonyad.healthplat.domain.model.HealthQuality
import com.bonyad.healthplat.domain.model.MealType
import com.bonyad.healthplat.domain.model.ScannedDish
import com.bonyad.healthplat.ui.utils.toFarsiDigits
import com.bonyad.healthplat.R

// Design colors
private val ErrorRed = Color(0xFFE57373)

/**
 * Scan Result Screen - Shows AI analysis of scanned food
 * Matches detailed_scan_10.png and detailed_scan_11.png designs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(
    imageUri: Uri,
    viewModel: CaloryViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onSaveComplete: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Scan state from ViewModel
    val scanState by viewModel.foodScanState.collectAsState()

    // Local state
    var isDetailsExpanded by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    var selectedMealType by remember { mutableStateOf(MealType.LUNCH) }
    var showMealTypeDropdown by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Editable dish list
    val editableDishes = remember { mutableStateListOf<ScannedDish>() }

    // Add new item bottom sheet
    var showAddItemSheet by remember { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState()

    // Trigger scan when screen opens
    LaunchedEffect(imageUri) {
        viewModel.scanFood(imageUri)
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
                    else -> "در حال تحلیل..."
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
                    // Health Assessment Card
                    HealthAssessmentCard(
                        healthQuality = state.result.healthQuality,
                        healthScore = state.result.healthScore,
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
                isLoading = isLoading,
                onClick = {
                    isLoading = true
                    // TODO: Save to API
                    viewModel.saveScanResult(
                        imageUri = imageUri,
                        mealType = selectedMealType,
                        dishes = editableDishes.toList()
                    )
                    onSaveComplete()
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
    ) {
        // Food Image
//        AsyncImage(
//            model = ImageRequest.Builder(LocalContext.current)
//                .data(imageUri)
//                .crossfade(true)
//                .build(),
//            contentDescription = mealName,
//            modifier = Modifier.fillMaxSize(),
//            contentScale = ContentScale.Crop
//        )

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
                painter = if (isFavorite) painterResource(R.drawable.star) else painterResource(R.drawable.star),
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
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
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

@Composable
private fun HealthAssessmentCard(
    healthQuality: HealthQuality,
    healthScore: Int,
    modifier: Modifier = Modifier
) {
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
            // Header
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

            // Health quality indicators
            HealthQualityIndicator(
                currentQuality = healthQuality
            )
        }
    }
}

@Composable
private fun HealthQualityIndicator(
    currentQuality: HealthQuality
) {
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

        // Progress bar
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
            )

            // Indicator dot
            Box(
                modifier = Modifier
                    .padding(start = ((progress * 100) - 2).dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, TealPrimary, CircleShape)
                    .align(Alignment.CenterStart)
            )
        }
    }
}

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

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header (always visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = if (isExpanded) painterResource(R.drawable.back_arrow) else painterResource(R.drawable.back_arrow),
                    contentDescription = if (isExpanded) "بستن" else "باز کردن",
                    tint = TextGray
                )

                Text(
                    text = "جزئیات وعده",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = TextDark
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
                    // Time and Meal Type selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Meal Type Selector
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = selectedMealType.persianName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("نوع وعده") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showMealTypeDropdown = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealPrimary,
                                    unfocusedBorderColor = Color(0xFFE0E0E0)
                                ),
                                trailingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.back_arrow),
                                        contentDescription = null,
                                        tint = TextGray
                                    )
                                }
                            )

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
                        OutlinedTextField(
                            value = "امروز، ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(System.currentTimeMillis()).toFarsiDigits()}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("زمان") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            ),
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.back_arrow),
                                    contentDescription = null,
                                    tint = TextGray
                                )
                            }
                        )
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
                        textAlign = TextAlign.End
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

        // Dish name
        Text(
            text = dish.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = TextDark,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

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
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
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

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = TealPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "در حال تحلیل تصویر...",
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
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = TextDark,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Dish Name
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

            // Calories
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

            // Add Button
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
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
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