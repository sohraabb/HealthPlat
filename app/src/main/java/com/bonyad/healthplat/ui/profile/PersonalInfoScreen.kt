package com.bonyad.healthplat.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.R
import kotlinx.coroutines.launch

// Colors
private val TealPrimary = Color(0xFF5BA3A3)
private val TextDark = Color(0xFF2C2C2C)
private val TextGray = Color(0xFF666666)
private val PlaceholderColor = Color(0xFFCCCCCC)
private val InputBorder = Color(0xFFE0E0E0)
private val BackgroundColor = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInfoScreen(
    viewModel: PersonalInfoViewModel = hiltViewModel(),
    onComplete: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val name by viewModel.name.collectAsState()
    val lastName by viewModel.lastName.collectAsState()
    val birthDate by viewModel.birthDate.collectAsState()
    val height by viewModel.height.collectAsState()
    val weight by viewModel.weight.collectAsState()
    val gender by viewModel.gender.collectAsState()
    val isFormValid by viewModel.isFormValid.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val showDatePicker by viewModel.showDatePicker.collectAsState()
    val showGenderPicker by viewModel.showGenderPicker.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Focus requesters for keyboard navigation
    val lastNameFocusRequester = remember { FocusRequester() }
    val heightFocusRequester = remember { FocusRequester() }
    val weightFocusRequester = remember { FocusRequester() }



    BackHandler(enabled = onBack != null) {
        onBack?.invoke()
    }

    LaunchedEffect(uiState) {
        if (uiState is PersonalInfoUiState.Success) {
            onComplete()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is PersonalInfoUiState.Error) {
            snackbarHostState.showSnackbar((uiState as PersonalInfoUiState.Error).message)
            viewModel.resetError()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = BackgroundColor
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
                            painter = painterResource(id = R.drawable.personal_ring_img),
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
                                            BackgroundColor.copy(alpha = 0.3f),
                                            BackgroundColor.copy(alpha = 0.9f),
                                            BackgroundColor
                                        )
                                    )
                                )
                        )

                        // Back button - TOP LEFT
                        if (onBack != null) {
                            IconButton(
                                onClick = { onBack() },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .statusBarsPadding()
                                    .padding(end = 24.dp, top = 16.dp)
                                    .size(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.back_arrow),
                                    contentDescription = "بازگشت",
                                    tint = TextDark
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "اطلاعات پایه",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    textAlign = TextAlign.Center
                                ),
                                color = TextDark,
                                modifier = Modifier.padding(bottom = 32.dp)
                            )

                            // Name Row (First Name and Last Name)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // First Name
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { viewModel.updateName(it) },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("نام", color = Color.Black) },
                                    placeholder = { Text("علی", color = PlaceholderColor) },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(
                                        onNext = { lastNameFocusRequester.requestFocus() }
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TealPrimary,
                                        unfocusedBorderColor = InputBorder,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    textStyle = LocalTextStyle.current.copy(
                                        textAlign = TextAlign.Start,
                                        color = Color.Black
                                    )
                                )

                                // Last Name
                                OutlinedTextField(
                                    value = lastName,
                                    onValueChange = { viewModel.updateLastName(it) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(lastNameFocusRequester),
                                    label = { Text("نام خانوادگی", color = Color.Black) },
                                    placeholder = { Text("محمدی", color = PlaceholderColor) },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(
                                        onNext = { heightFocusRequester.requestFocus() }
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TealPrimary,
                                        unfocusedBorderColor = InputBorder,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    textStyle = LocalTextStyle.current.copy(
                                        textAlign = TextAlign.Start,
                                        color = Color.Black
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Birth Date and Height Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Height
                                OutlinedTextField(
                                    value = height,
                                    onValueChange = { viewModel.updateHeight(it) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(heightFocusRequester),
                                    label = { Text("قد", color = Color.Black) },
                                    placeholder = { Text("سانتی‌متر", color = PlaceholderColor) },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { weightFocusRequester.requestFocus() }
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TealPrimary,
                                        unfocusedBorderColor = InputBorder,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    textStyle = LocalTextStyle.current.copy(
                                        textAlign = TextAlign.Start,
                                        color = Color.Black
                                    )
                                )

                                // Birth Date
                                OutlinedTextField(
                                    value = birthDate,
                                    onValueChange = { },
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.onDatePickerClick() },
                                    label = { Text("تاریخ تولد") },
                                    placeholder = { Text("۱۳۷۹/۰۹/۰۵", color = PlaceholderColor) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = TealPrimary
                                        )
                                    },
                                    readOnly = true,
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledBorderColor = InputBorder,
                                        disabledContainerColor = Color.White,
                                        disabledLabelColor = TextGray,
                                        disabledTextColor = TextDark,
                                        disabledTrailingIconColor = TealPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    textStyle = LocalTextStyle.current.copy(
                                        textAlign = TextAlign.Start,
                                        color = Color.Black
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Weight and Gender Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Gender selector
                                OutlinedTextField(
                                    value = gender,
                                    onValueChange = { },
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.onGenderPickerClick() },
                                    label = { Text("جنسیت") },
                                    placeholder = {
                                        Text(
                                            "انتخاب کنید",
                                            color = PlaceholderColor
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = TealPrimary
                                        )
                                    },
                                    readOnly = true,
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledBorderColor = InputBorder,
                                        disabledContainerColor = Color.White,
                                        disabledLabelColor = TextGray,
                                        disabledTextColor = TextDark,
                                        disabledTrailingIconColor = TealPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    textStyle = LocalTextStyle.current.copy(
                                        textAlign = TextAlign.Start,
                                        color = Color.Black
                                    )
                                )

                                // Weight
                                OutlinedTextField(
                                    value = weight,
                                    onValueChange = { viewModel.updateWeight(it) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(weightFocusRequester),
                                    label = { Text("وزن", color = Color.Black) },
                                    placeholder = { Text("کیلوگرم", color = PlaceholderColor) },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (isFormValid) {
                                                viewModel.savePersonalInfo()
                                            }
                                        }
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TealPrimary,
                                        unfocusedBorderColor = InputBorder,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    textStyle = LocalTextStyle.current.copy(
                                        textAlign = TextAlign.Start,
                                        color = Color.Black
                                    )
                                )
                            }
                        }

                        // Submit Button - At bottom
                        Column {
                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = { viewModel.savePersonalInfo() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFormValid) TealPrimary else InputBorder,
                                    disabledContainerColor = InputBorder
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
        }

        if (showDatePicker) {
            val dateParts = viewModel.getSelectedDateParts()
            PersianDatePickerBottomSheet(
                initialYear = dateParts.first,
                initialMonth = dateParts.second,
                initialDay = dateParts.third,
                onDismiss = { viewModel.onDatePickerDismiss() },
                onDateSelected = { year, month, day ->
                    viewModel.onDateSelected(year, month, day)
                }
            )
        }

        if (showGenderPicker) {
            GenderPickerBottomSheet(
                onDismiss = { viewModel.onGenderPickerDismiss() },
                onGenderSelected = { selectedGender ->
                    viewModel.onGenderSelected(selectedGender)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenderPickerBottomSheet(
    onDismiss: () -> Unit,
    onGenderSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

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
                text = "انتخاب جنسیت",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = TextDark,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Male option
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onGenderSelected("مرد")
                        onDismiss()
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = "مرد",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = TextDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Female option
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onGenderSelected("زن")
                        onDismiss()
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = "زن",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = TextDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersianDatePickerBottomSheet(
    initialYear: Int = 1370,
    initialMonth: Int = 1,
    initialDay: Int = 1,
    onDismiss: () -> Unit,
    onDateSelected: (Int, Int, Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var selectedYear by remember { mutableStateOf(initialYear) }
    var selectedMonth by remember { mutableStateOf(initialMonth) }
    var selectedDay by remember { mutableStateOf(initialDay) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.LightGray, RoundedCornerShape(2.dp))
            )
        }
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
                color = TextDark,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Year Picker
                ImprovedDateColumn(
                    label = "سال",
                    range = (1320..1404).toList(),
                    selectedValue = selectedYear,
                    onValueChange = { selectedYear = it },
                    modifier = Modifier.weight(1f)
                )

                // Month Picker
                ImprovedDateColumn(
                    label = "ماه",
                    range = (1..12).toList(),
                    selectedValue = selectedMonth,
                    onValueChange = { selectedMonth = it },
                    modifier = Modifier.weight(1f)
                )

                // Day Picker
                ImprovedDateColumn(
                    label = "روز",
                    range = (1..31).toList(),
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
                    containerColor = TealPrimary
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

@Composable
fun ImprovedDateColumn(
    label: String,
    range: List<Int>,
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Find the index of the selected value
    val selectedIndex = range.indexOf(selectedValue).coerceAtLeast(0)

    // Scroll to selected item on first composition
    LaunchedEffect(selectedValue) {
        listState.scrollToItem(maxOf(0, selectedIndex))
    }

    // Update selection when scrolling stops
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex
            if (centerIndex in range.indices) {
                onValueChange(range[centerIndex])
            }
        }
    }

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
            color = TextGray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(BackgroundColor, RoundedCornerShape(12.dp))
        ) {
            // Selection indicator in the center
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.Center)
                    .padding(horizontal = 8.dp)
                    .background(TealPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 60.dp) // Add padding for center alignment
            ) {
                items(range) { value ->
                    val isSelected = value == selectedValue
                    Text(
                        text = convertToPersianNumber(value),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = if (isSelected) 18.sp else 16.sp
                        ),
                        color = if (isSelected) TealPrimary else TextDark.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onValueChange(value)
                                scope.launch {
                                    val index = range.indexOf(value)
                                    listState.animateScrollToItem(maxOf(0, index - 2))
                                }
                            }
                            .wrapContentHeight(Alignment.CenterVertically),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Fade edges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                BackgroundColor,
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                BackgroundColor
                            )
                        )
                    )
            )
        }
    }
}

fun convertToPersianNumber(number: Int): String {
    val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
    return number.toString().map { char ->
        if (char.isDigit()) persianDigits[char.toString().toInt()] else char
    }.joinToString("")
}