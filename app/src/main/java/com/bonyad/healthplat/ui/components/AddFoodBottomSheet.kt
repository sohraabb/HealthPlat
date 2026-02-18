package com.bonyad.healthplat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bonyad.healthplat.domain.model.MealType
import com.bonyad.healthplat.ui.dashboard.calory.TealPrimary
import com.bonyad.healthplat.ui.dashboard.calory.TextDark
import com.bonyad.healthplat.ui.dashboard.calory.TextGray

// ============ Add Food Bottom Sheet ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodBottomSheet(
    mealType: MealType,
    onDismiss: () -> Unit,
    onAddFood: (name: String, caloriesMin: Int, caloriesMax: Int, amount: Int, unit: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current

    var foodName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("۱") }
    var selectedUnit by remember { mutableStateOf("عدد") }
    var showUnitDropdown by remember { mutableStateOf(false) }

    val units = listOf("عدد", "گرم", "میلی‌لیتر", "فنجان", "قاشق", "لیوان")

    // Validation
    val isValid = foodName.isNotBlank() && amount.isNotBlank()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                // Custom drag handle
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
                // Header with close button
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mealType.persianName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = TextDark
                    )

                    // Close button
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

                // Food Name Input
                OutlinedTextField(
                    value = foodName,
                    onValueChange = { foodName = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "تخم مرغ",
                            color = Color(0xFF868686),
                        )
                    },
                    label = {
                        Text(
                            text = "نام غذا را جستجو کنید",
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Amount and Unit Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Unit Dropdown

                    // Amount Input
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { newValue ->
                            // Only allow digits
                            if (newValue.all { it.isDigit() || it in '۰'..'۹' }) {
                                amount = newValue
                            }
                        },
                        modifier = Modifier.weight(0.6f),
                        placeholder = {
                            Text(
                                text = "۱",
                                color = Color(0xFF868686),
                            )
                        },
                        label = {
                            Text(
                                text = "میزان مصرف",
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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        )
                    )

                    Box(modifier = Modifier.weight(0.4f)) {
                        OutlinedTextField(
                            value = selectedUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text(
                                    text = "واحد",
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
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = TextGray,
                                    modifier = Modifier.clickable { showUnitDropdown = true }
                                )
                            },
                            modifier = Modifier.clickable { showUnitDropdown = true }
                        )

                        DropdownMenu(
                            expanded = showUnitDropdown,
                            onDismissRequest = { showUnitDropdown = false }
                        ) {
                            units.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit) },
                                    onClick = {
                                        selectedUnit = unit
                                        showUnitDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (isValid) {
                            val amountInt = convertPersianToEnglishNumber(amount).toIntOrNull() ?: 1
                            // Estimate calories based on common foods (in production, use API)
                            val estimatedCalories =
                                estimateCalories(foodName, amountInt, selectedUnit)
                            onAddFood(
                                foodName,
                                estimatedCalories.first,
                                estimatedCalories.second,
                                amountInt,
                                selectedUnit
                            )
                            onDismiss()
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

private fun convertPersianToEnglishNumber(text: String): String {
    val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
    val englishDigits = "0123456789"

    return text.map { char ->
        val index = persianDigits.indexOf(char)
        if (index != -1) englishDigits[index] else char
    }.joinToString("")
}

private fun estimateCalories(foodName: String, amount: Int, unit: String): Pair<Int, Int> {
    // Simple estimation based on common foods
    // In production, this would come from the API
    val baseCalories = when {
        foodName.contains("تخم") || foodName.contains("egg", ignoreCase = true) -> 78
        foodName.contains("برنج") || foodName.contains("rice", ignoreCase = true) -> 130
        foodName.contains("مرغ") || foodName.contains("chicken", ignoreCase = true) -> 165
        foodName.contains("نان") || foodName.contains("bread", ignoreCase = true) -> 80
        foodName.contains("سیب") || foodName.contains("apple", ignoreCase = true) -> 52
        foodName.contains("موز") || foodName.contains("banana", ignoreCase = true) -> 89
        foodName.contains("شیر") || foodName.contains("milk", ignoreCase = true) -> 42
        foodName.contains("پنیر") || foodName.contains("cheese", ignoreCase = true) -> 110
        foodName.contains("سالاد") || foodName.contains("salad", ignoreCase = true) -> 20
        foodName.contains("آب میوه") || foodName.contains("juice", ignoreCase = true) -> 45
        else -> 100 // Default estimate
    }

    // Adjust based on unit
    val multiplier = when (unit) {
        "گرم" -> 0.01 // Per gram
        "میلی‌لیتر" -> 0.01 // Per ml
        "فنجان", "لیوان" -> 2.0 // Per cup
        "قاشق" -> 0.5 // Per spoon
        else -> 1.0 // Per unit
    }

    val totalCalories = (baseCalories * amount * multiplier).toInt()
    val minCalories = (totalCalories * 0.9).toInt()
    val maxCalories = (totalCalories * 1.1).toInt()

    return Pair(minCalories, maxCalories)
}