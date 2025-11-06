package com.bonyad.healthplat.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInfoScreen(
    viewModel: PersonalInfoViewModel = hiltViewModel(),
    onComplete: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val name by viewModel.name.collectAsState()
    val birthDate by viewModel.birthDate.collectAsState()
    val height by viewModel.height.collectAsState()
    val weight by viewModel.weight.collectAsState()
    val isFormValid by viewModel.isFormValid.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val showDatePicker by viewModel.showDatePicker.collectAsState()


    val snackbarHostState = remember { SnackbarHostState() }

    // Handle back press
    BackHandler(enabled = onBack != null) {
        onBack?.invoke()
    }

    // Navigate on success
    LaunchedEffect(uiState) {
        if (uiState is PersonalInfoUiState.Success) {
            onComplete()
        }
    }

    // Show error snackbar
    LaunchedEffect(uiState) {
        if (uiState is PersonalInfoUiState.Error) {
            snackbarHostState.showSnackbar((uiState as PersonalInfoUiState.Error).message)
            viewModel.resetError()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color(0xFFF5F5F5)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Image with gradient and back button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.onboarding_3),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xFFF5F5F5).copy(alpha = 0.3f),
                                            Color(0xFFF5F5F5).copy(alpha = 0.9f),
                                            Color(0xFFF5F5F5)
                                        )
                                    )
                                )
                        )

                        // Back button
                        if (onBack != null) {
                            IconButton(
                                onClick = { onBack() },
                                modifier = Modifier
                                    .padding(16.dp)
                                    .background(Color.White.copy(alpha = 0.9f), CircleShape)
                                    .size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "بازگشت",
                                    tint = Color(0xFF2C2C2C)
                                )
                            }
                        }
                    }

                    // Form content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "اطلاعات پایه",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center
                            ),
                            color = Color(0xFF2C2C2C),
                            modifier = Modifier.padding(bottom = 32.dp)
                        )

                        // Name field (full width)
                        OutlinedTextField(
                            value = name,
                            onValueChange = { viewModel.updateName(it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("نام") },
                            placeholder = { Text("علی کمالی", color = Color(0xFFCCCCCC)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF5BA3A3),
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Birth Date (full width)
                        OutlinedTextField(
                            value = birthDate,
                            onValueChange = { },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onDatePickerClick() },
                            label = { Text("تاریخ تولد") },
                            placeholder = { Text("۱۳۷۹/۰۹/۰۵", color = Color(0xFFCCCCCC)) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = Color(0xFF5BA3A3)
                                )
                            },
                            readOnly = true,
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = Color(0xFFE0E0E0),
                                disabledContainerColor = Color.White,
                                disabledLabelColor = Color(0xFF666666),
                                disabledTextColor = Color(0xFF2C2C2C),
                                disabledTrailingIconColor = Color(0xFF5BA3A3)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Height and Weight Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Weight (left in RTL)
                            OutlinedTextField(
                                value = weight,
                                onValueChange = { viewModel.updateWeight(it) },
                                modifier = Modifier.weight(1f),
                                label = { Text("وزن") },
                                placeholder = { Text("کیلو گرم", color = Color(0xFFCCCCCC)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF5BA3A3),
                                    unfocusedBorderColor = Color(0xFFE0E0E0),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
                            )

                            // Height (right in RTL)
                            OutlinedTextField(
                                value = height,
                                onValueChange = { viewModel.updateHeight(it) },
                                modifier = Modifier.weight(1f),
                                label = { Text("قد") },
                                placeholder = { Text("سانتی متر", color = Color(0xFFCCCCCC)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF5BA3A3),
                                    unfocusedBorderColor = Color(0xFFE0E0E0),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Submit Button
                        Button(
                            onClick = { viewModel.savePersonalInfo() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFormValid) Color(0xFF5BA3A3) else Color(0xFFE0E0E0),
                                disabledContainerColor = Color(0xFFE0E0E0)
                            ),
                            enabled = isFormValid && uiState !is PersonalInfoUiState.Loading
                        ) {
                            if (uiState is PersonalInfoUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "ذخیره و ادامه",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }

        if (showDatePicker) {
            PersianDatePickerBottomSheet(
                onDismiss = { viewModel.onDatePickerDismiss() },
                onDateSelected = { year, month, day ->
                    viewModel.onDateSelected(year, month, day)
                }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersianDatePickerBottomSheet(
    onDismiss: () -> Unit,
    onDateSelected: (Int, Int, Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    // Set default to a reasonable birth year (e.g., 1379 = ~2000)
    var selectedYear by remember { mutableStateOf(1379) }
    var selectedMonth by remember { mutableStateOf(1) }
    var selectedDay by remember { mutableStateOf(1) }

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
                text = "انتخاب تاریخ تولد",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = Color(0xFF2C2C2C),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Year Picker (1320-1404)
                PersianDateColumn(
                    label = "سال",
                    range = 1320..1404,  // Current Persian year
                    selectedValue = selectedYear,
                    onValueChange = { selectedYear = it },
                    modifier = Modifier.weight(1f)
                )

                // Month Picker
                PersianDateColumn(
                    label = "ماه",
                    range = 1..12,
                    selectedValue = selectedMonth,
                    onValueChange = { selectedMonth = it },
                    modifier = Modifier.weight(1f)
                )

                // Day Picker
                PersianDateColumn(
                    label = "روز",
                    range = 1..31,
                    selectedValue = selectedDay,
                    onValueChange = { selectedDay = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onDateSelected(selectedYear, selectedMonth, selectedDay)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5BA3A3)
                )
            ) {
                Text(
                    text = "تایید",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersianDateColumn(
    label: String,
    range: IntRange,
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            ),
            color = Color(0xFF666666),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                range.forEach { value ->
                    Text(
                        text = convertToPersianNumber(value),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (value == selectedValue) FontWeight.Bold else FontWeight.Normal,
                            fontSize = if (value == selectedValue) 18.sp else 16.sp
                        ),
                        color = if (value == selectedValue) Color(0xFF5BA3A3) else Color(0xFF2C2C2C),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onValueChange(value) }
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

fun convertToPersianNumber(number: Int): String {
    val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
    return number.toString().map { char ->
        if (char.isDigit()) persianDigits[char.toString().toInt()] else char
    }.joinToString("")
}