package com.bonyad.healthplat.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.R
import com.bonyad.healthplat.domain.model.DiseaseData
import kotlinx.coroutines.delay

// Colors - Same as PersonalInfoScreen
private val TealPrimary = Color(0xFF5BA3A3)
private val TextDark = Color(0xFF2C2C2C)
private val TextGray = Color(0xFF666666)
private val PlaceholderColor = Color(0xFFCCCCCC)
private val InputBorder = Color(0xFFE0E0E0)
private val InputBackground = Color.White
private val BackgroundColor = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPersonalInfoScreen(
    viewModel: PersonalInfoViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    // Load data once when entering
    LaunchedEffect(Unit) {
        viewModel.loadExistingData()
    }

    // State Collection
    val name by viewModel.name.collectAsState()
    val lastName by viewModel.lastName.collectAsState()
    val birthDate by viewModel.birthDate.collectAsState()
    val height by viewModel.height.collectAsState()
    val weight by viewModel.weight.collectAsState()
    val gender by viewModel.gender.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val nationalCode by viewModel.nationalCode.collectAsState()
    val email by viewModel.email.collectAsState()
    val selectedDiseasesText by viewModel.selectedDiseasesText.collectAsState()

    val uiState by viewModel.uiState.collectAsState()
    val isFormValid by viewModel.isFormValid.collectAsState()

    // Pickers
    val showDatePicker by viewModel.showDatePicker.collectAsState()
    val showGenderPicker by viewModel.showGenderPicker.collectAsState()
    val showDiseasePicker by viewModel.showDiseasePicker.collectAsState()

    // Phone change
    val showPhoneChangeSheet by viewModel.showPhoneChangeSheet.collectAsState()
    val showPhoneOtpSheet by viewModel.showPhoneOtpSheet.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is PersonalInfoUiState.Success) {
            viewModel.resetState()
            onBack()
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
            containerColor = BackgroundColor,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "اطلاعات کاربری",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        )
                    },
                    navigationIcon = {
                        // Refresh button
                        IconButton(onClick = { }) {
                            Icon(
                                painter = painterResource(id = R.drawable.notification),
                                tint = Color.Black,
                                contentDescription = "بازخوانی",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(id = R.drawable.back_arrow),
                                contentDescription = "Back",
                                tint = TextDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = BackgroundColor
                    )
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundColor)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = { viewModel.savePersonalInfo() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TealPrimary,
                            disabledContainerColor = InputBorder
                        ),
                        shape = RoundedCornerShape(16.dp),
                        enabled = isFormValid && uiState !is PersonalInfoUiState.Loading
                    ) {
                        if (uiState is PersonalInfoUiState.Loading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "ذخیره",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // --- Form Fields ---

                // Full Name (First + Last in one row)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        EditTextField(
                            value = name,
                            onValueChange = { viewModel.updateName(it) },
                            label = "نام",
                            placeholder = "علی"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        EditTextField(
                            value = lastName,
                            onValueChange = { viewModel.updateLastName(it) },
                            label = "نام خانوادگی",
                            placeholder = "محمدی"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Height & Birth Date Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        EditTextField(
                            value = height,
                            onValueChange = { viewModel.updateHeight(it) },
                            label = "قد",
                            keyboardType = KeyboardType.Number,
                            placeholder = "cm"
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        EditClickableField(
                            label = "تاریخ تولد",
                            value = if (birthDate.isNotEmpty()) birthDate else "انتخاب کنید",
                            icon = Icons.Default.DateRange,
                            onClick = { viewModel.onDatePickerClick() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Gender & Weight Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        EditClickableField(
                            label = "جنسیت",
                            value = if (gender.isNotEmpty()) gender else "انتخاب کنید",
                            icon = Icons.Default.ArrowDropDown,
                            onClick = { viewModel.onGenderPickerClick() }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        EditTextField(
                            value = weight,
                            onValueChange = { viewModel.updateWeight(it) },
                            label = "وزن",
                            keyboardType = KeyboardType.Number,
                            placeholder = "kg"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Phone Number & National Code Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        EditClickableField(
                            label = "شماره تماس",
                            value = if (phoneNumber.isNotEmpty()) phoneNumber else "-",
                            onClick = { viewModel.onPhoneChangeClick() }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        EditTextField(
                            value = nationalCode,
                            onValueChange = { viewModel.updateNationalCode(it) },
                            label = "کد ملی",
                            keyboardType = KeyboardType.Number,
                            placeholder = "-"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Disease (Multi-select)
                EditClickableField(
                    label = "بیماری زمینه‌ای",
                    value = selectedDiseasesText,
                    icon = Icons.Filled.ArrowDropDown,
                    onClick = { viewModel.onDiseasePickerClick() }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Email
                EditTextField(
                    value = email,
                    onValueChange = { viewModel.updateEmail(it) },
                    label = "ایمیل",
                    keyboardType = KeyboardType.Email,
                    placeholder = "example@mail.com"
                )

                // Extra Padding at Bottom for Button
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        // --- Bottom Sheets ---

        if (showDatePicker) {
            val dateParts = viewModel.getSelectedDateParts()
            PersianDatePickerBottomSheet(
                initialYear = dateParts.first,
                initialMonth = dateParts.second,
                initialDay = dateParts.third,
                onDismiss = { viewModel.onDatePickerDismiss() },
                onDateSelected = { y, m, d -> viewModel.onDateSelected(y, m, d) }
            )
        }

        if (showGenderPicker) {
            GenderPickerBottomSheet(
                onDismiss = { viewModel.onGenderPickerDismiss() },
                onGenderSelected = { viewModel.onGenderSelected(it) }
            )
        }

        if (showDiseasePicker) {
            MultiDiseasePickerBottomSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.onDiseasePickerDismiss() }
            )
        }

        if (showPhoneChangeSheet) {
            PhoneChangeBottomSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.onPhoneChangeSheetDismiss() }
            )
        }

        if (showPhoneOtpSheet) {
            PhoneOtpBottomSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.onPhoneOtpSheetDismiss() }
            )
        }
    }
}

// --- Components Styled Like PersonalInfoScreen ---

@Composable
fun EditTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                color = TextGray,
                fontWeight = FontWeight.Normal
            ),
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TealPrimary,
                unfocusedBorderColor = InputBorder,
                focusedContainerColor = InputBackground,
                unfocusedContainerColor = InputBackground,
                cursorColor = TealPrimary
            ),
            placeholder = {
                Text(
                    text = placeholder,
                    color = PlaceholderColor,
                    style = LocalTextStyle.current.copy(fontSize = 16.sp)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                textAlign = TextAlign.Start,
                color = TextDark,
                fontSize = 16.sp
            )
        )
    }
}

@Composable
fun EditClickableField(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                color = TextGray,
                fontWeight = FontWeight.Normal
            ),
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                enabled = false,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = InputBorder,
                    disabledContainerColor = InputBackground,
                    disabledTextColor = TextDark,
                    disabledTrailingIconColor = TealPrimary
                ),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Start,
                    color = TextDark,
                    fontSize = 16.sp
                ),
                trailingIcon = if (icon != null) {
                    { Icon(imageVector = icon, contentDescription = null, tint = TealPrimary) }
                } else null
            )
            // Overlay to capture clicks over the disabled TextField
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(onClick = onClick)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiDiseasePickerBottomSheet(
    viewModel: PersonalInfoViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val availableDiseases by viewModel.availableDiseases.collectAsState()
    val selectedDiseaseIds by viewModel.selectedDiseaseIds.collectAsState()

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
                text = "انتخاب بیماری زمینه‌ای",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = TextDark,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "می‌توانید چند مورد انتخاب کنید",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 14.sp,
                    color = TextGray
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // "No disease" option
            DiseaseCheckItem(
                name = "ندارم",
                isSelected = selectedDiseaseIds.isEmpty(),
                onClick = { viewModel.clearDiseaseSelection() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Disease list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                items(availableDiseases) { disease ->
                    DiseaseCheckItem(
                        name = disease.name,
                        isSelected = selectedDiseaseIds.contains(disease.id),
                        onClick = { viewModel.toggleDiseaseSelection(disease.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
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
fun DiseaseCheckItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            ),
            color = if (isSelected) TealPrimary else TextDark
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = TealPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneChangeBottomSheet(
    viewModel: PersonalInfoViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val newPhoneNumber by viewModel.newPhoneNumber.collectAsState()
    val phoneChangeState by viewModel.phoneChangeState.collectAsState()

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
                text = "تغییر شماره تماس",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = TextDark,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = newPhoneNumber,
                onValueChange = { viewModel.updateNewPhoneNumber(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("شماره همراه جدید") },
                placeholder = { Text("۰۹۱۲۳۴۵۶۷۸۹", color = PlaceholderColor) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = InputBorder,
                    focusedContainerColor = InputBackground,
                    unfocusedContainerColor = InputBackground
                ),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Start,
                    fontSize = 16.sp
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.requestPhoneChangeOtp() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (newPhoneNumber.length == 11) TealPrimary else InputBorder
                ),
                enabled = newPhoneNumber.length == 11 && phoneChangeState !is PersonalInfoUiState.Loading
            ) {
                if (phoneChangeState is PersonalInfoUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "ارسال کد تایید",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneOtpBottomSheet(
    viewModel: PersonalInfoViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val phoneOtp by viewModel.phoneOtp.collectAsState()
    val phoneChangeState by viewModel.phoneChangeState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-submit when 5 digits entered
    LaunchedEffect(phoneOtp) {
        if (phoneOtp.length == 5) {
            delay(200)
            viewModel.verifyPhoneChangeOtp()
        }
    }

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
                text = "ورود با شماره جدید",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = TextDark,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "کد پیامک شده",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                color = TextGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.End
            )

            // OTP Input boxes
            OtpInputRow(
                otp = phoneOtp,
                onOtpChange = { viewModel.updatePhoneOtp(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "کد به شماره ${viewModel.getFormattedNewPhoneNumber()} ارسال شد. ",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = TextGray
                )
                Text(
                    text = "تغییر شماره همراه",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = TealPrimary,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.clickable {
                        onDismiss()
                        viewModel.onPhoneChangeClick()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.verifyPhoneChangeOtp() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (phoneOtp.length == 5) TealPrimary else InputBorder
                ),
                enabled = phoneOtp.length == 5 && phoneChangeState !is PersonalInfoUiState.Loading
            ) {
                if (phoneChangeState is PersonalInfoUiState.Loading) {
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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun OtpInputRow(
    otp: String,
    onOtpChange: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                },
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            repeat(5) { index ->
                OtpDigitBox(
                    digit = otp.getOrNull(index)?.toString() ?: "",
                    isFilled = index < otp.length
                )
            }
        }

        BasicTextField(
            value = otp,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() } && newValue.length <= 5) {
                    onOtpChange(newValue)
                }
            },
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            cursorBrush = SolidColor(Color.Transparent)
        )
    }
}

@Composable
fun OtpDigitBox(
    digit: String,
    isFilled: Boolean
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(
                color = Color.White,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = if (isFilled) TealPrimary else InputBorder,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (digit.isEmpty()) {
            Text(
                text = "-",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 20.sp
                ),
                color = PlaceholderColor
            )
        } else {
            Text(
                text = digit,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = TextDark
            )
        }
    }
}