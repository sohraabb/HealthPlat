package com.bonyad.healthplat.ui.dashboard.profile.medication

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.bonyad.healthplat.R
import com.bonyad.healthplat.domain.model.*
import com.bonyad.healthplat.ui.utils.toFarsiDigits

// Color constants
private val TealColor = Color(0xFF5BA3A3)
private val BackgroundColor = Color(0xFFF5F5F5)
private val CardColor = Color.White
private val TextPrimaryColor = Color(0xFF2C2C2C)
private val TextSecondaryColor = Color(0xFF666666)
private val TextTertiaryColor = Color(0xFF999999)
private val BorderColor = Color(0xFFE8E8E8)
private val DisabledColor = Color(0xFFE0E0E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationScreen(
    onBack: () -> Unit,
    viewModel: MedicationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val showAddSheet by viewModel.showAddSheet.collectAsState()
    val addMedicationState by viewModel.addMedicationState.collectAsState()

        Scaffold(
            topBar = {
                MedicationTopBar(onBack = onBack)
            },
            bottomBar = {
                AddMedicationButton(onClick = { viewModel.onAddMedicationClick() })
            },
            containerColor = BackgroundColor
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (uiState) {
                    is MedicationUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = TealColor
                        )
                    }
                    is MedicationUiState.Empty -> {
                        EmptyMedicationState(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is MedicationUiState.Success -> {
                        MedicationList(
                            medications = medications,
                            onToggleEnabled = { viewModel.toggleMedicationEnabled(it) },
                            onEdit = { viewModel.onEditMedicationClick(it) },
                            onDelete = { viewModel.deleteMedication(it) }
                        )
                    }
                    is MedicationUiState.Error -> {
                        ErrorState(
                            message = (uiState as MedicationUiState.Error).message,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        // Add/Edit Medication Bottom Sheet
        if (showAddSheet) {
            AddMedicationBottomSheet(
                state = addMedicationState,
                viewModel = viewModel,
                onDismiss = { viewModel.dismissAddSheet() }
            )
        }
    }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicationTopBar(onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "ثبت دارو",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextPrimaryColor
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.back_arrow),
                    contentDescription = "بازگشت",
                    tint = TextPrimaryColor
                )
            }
        },
        actions = {
            IconButton(onClick = { /* Sync action */ }) {
                Icon(
                    painter = painterResource(R.drawable.notification),
                    tint = Color.Black,
                    contentDescription = "همگام‌سازی",
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = BackgroundColor
        )
    )
}

@Composable
private fun AddMedicationButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealColor)
        ) {
            Text(
                text = "ثبت دارو جدید",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun EmptyMedicationState(modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = ".شما تا کنون هیچ دارو ای ثبت نکردید",
            fontSize = 16.sp,
            color = TextSecondaryColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        )

        Image(
            painter = painterResource(id = R.drawable.empty_medication),
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.Center),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun MedicationList(
    medications: List<Medication>,
    onToggleEnabled: (String) -> Unit,
    onEdit: (Medication) -> Unit,
    onDelete: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(medications, key = { it.id }) { medication ->
            MedicationCard(
                medication = medication,
                onToggleEnabled = { onToggleEnabled(medication.id) },
                onEdit = { onEdit(medication) },
                onDelete = { onDelete(medication.id) }
            )
        }
    }
}

@Composable
private fun MedicationCard(
    medication: Medication,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row: Title and Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Toggle switch
                Switch(
                    checked = medication.isEnabled,
                    onCheckedChange = { onToggleEnabled() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TealColor,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = DisabledColor
                    )
                )

                // Medication title and name
                Text(
                    text = "${medication.title} ${medication.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimaryColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom row: Time and Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Start date
                Text(
                    text = formatPersianDate(medication.startDate),
                    fontSize = 12.sp,
                    color = TextTertiaryColor
                )

                // Time and dosage
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Green dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(TealColor, CircleShape)
                    )

                    Text(
                        text = "${medication.times.firstOrNull()?.to24HourString() ?: "00:00"} دقیقه صبح ${medication.dosage}".toFarsiDigits(),
                        fontSize = 13.sp,
                        color = TextSecondaryColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            fontSize = 16.sp,
            color = Color.Red,
            textAlign = TextAlign.Center
        )
    }
}

// ==================== Add Medication Bottom Sheet ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMedicationBottomSheet(
    state: AddMedicationState,
    viewModel: MedicationViewModel,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        when (state.currentStep) {
            AddMedicationStep.DETAILS -> DetailsStep(state, viewModel)
            AddMedicationStep.FREQUENCY -> FrequencyStep(state, viewModel)
            AddMedicationStep.DAYS -> DaysStep(state, viewModel)
            AddMedicationStep.TIME -> TimeStep(state, viewModel)
        }
    }
}

// ==================== Step 1: Details ====================

@Composable
private fun DetailsStep(
    state: AddMedicationState,
    viewModel: MedicationViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        // Title
        Text(
            text = "با پر کردن فرم زیر دارو خود را ثبت کنید",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimaryColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Row 1: Title and Name
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Drug name (نام دارو)
            MedicationTextField(
                value = state.name,
                onValueChange = { viewModel.updateName(it) },
                label = "نام دارو",
                placeholder = "نام داروی مصرفی را وارد کنید",
                modifier = Modifier.weight(1f)
            )

            // Drug title (عنوان دارو)
            MedicationTextField(
                value = state.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = "عنوان دارو",
                placeholder = "مانند : قرص قلب ، قرص کبد ...",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Row 2: Dosage and Duration
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Dosage (مقدار مصرف)
            MedicationTextField(
                value = state.dosage,
                onValueChange = { viewModel.updateDosage(it) },
                label = "مقدار مصرف",
                placeholder = "مانند: یک قرص یا ۱۰۰ میلی گرم...",
                modifier = Modifier.weight(1f)
            )

            // Duration (مدت زمان مصرف)
            MedicationTextField(
                value = state.duration,
                onValueChange = { viewModel.updateDuration(it) },
                label = "مدت زمان مصرف",
                placeholder = "مدتی که باید دارو را مصرف کنید.",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Next button
        Button(
            onClick = { viewModel.onDetailsNext() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = state.isDetailsValid,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TealColor,
                disabledContainerColor = DisabledColor
            )
        ) {
            Text(
                text = "ثبت",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun MedicationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextTertiaryColor,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = 12.sp,
                    color = TextTertiaryColor,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            textStyle = LocalTextStyle.current.copy(
                textAlign = TextAlign.End,
                fontSize = 14.sp
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TealColor,
                unfocusedBorderColor = BorderColor,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )
    }
}

// ==================== Step 2: Frequency ====================

@Composable
private fun FrequencyStep(
    state: AddMedicationState,
    viewModel: MedicationViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        // Title
        Text(
            text = "در چه دوره هایی باید دارو را مصرف کنید",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimaryColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Frequency options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MedicationFrequency.values().reversed().forEach { frequency ->
                FrequencyButton(
                    text = frequency.persianName,
                    isSelected = state.frequency == frequency,
                    onClick = { viewModel.selectFrequency(frequency) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Next button
        Button(
            onClick = { viewModel.onFrequencyNext() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = state.isFrequencyValid,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TealColor,
                disabledContainerColor = DisabledColor
            )
        ) {
            Text(
                text = "ثبت",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun FrequencyButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) TealColor else BorderColor
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) TealColor.copy(alpha = 0.1f) else Color.White
        )
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (isSelected) TealColor else TextPrimaryColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ==================== Step 3: Days ====================

@Composable
private fun DaysStep(
    state: AddMedicationState,
    viewModel: MedicationViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        // Title
        Text(
            text = "در چه دوره هایی باید دارو را مصرف کنید",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimaryColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Frequency options (shown but disabled)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MedicationFrequency.values().reversed().forEach { frequency ->
                FrequencyButton(
                    text = frequency.persianName,
                    isSelected = state.frequency == frequency,
                    onClick = { viewModel.selectFrequency(frequency) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Days selection - Row 1 (شنبه to سه شنبه)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY,
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY
            ).forEach { day ->
                DayButton(
                    text = day.persianName,
                    isSelected = state.selectedDays.contains(day),
                    onClick = { viewModel.toggleDay(day) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Days selection - Row 2 (چهارشنبه to جمعه)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY
            ).forEach { day ->
                DayButton(
                    text = day.persianName,
                    isSelected = state.selectedDays.contains(day),
                    onClick = { viewModel.toggleDay(day) },
                    modifier = Modifier.weight(1f)
                )
            }
            // Empty space for alignment
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Next button
        Button(
            onClick = { viewModel.onDaysNext() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = state.isDaysValid,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TealColor,
                disabledContainerColor = DisabledColor
            )
        ) {
            Text(
                text = "ثبت",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun DayButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) TealColor else BorderColor
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) TealColor.copy(alpha = 0.1f) else Color.White
        )
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (isSelected) TealColor else TextPrimaryColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ==================== Step 4: Time ====================

@Composable
private fun TimeStep(
    state: AddMedicationState,
    viewModel: MedicationViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        // Title
        Text(
            text = "زمان مصرف دارو را تنظیم کنید.",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimaryColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Time Picker
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Minutes picker
            NumberPicker(
                value = state.currentTimeMinute,
                range = 0..59,
                onValueChange = { viewModel.updateTimeMinute(it) },
                format = { String.format("%02d", it).toFarsiDigits() }
            )

            Spacer(modifier = Modifier.width(24.dp))

            // Hours picker
            NumberPicker(
                value = state.currentTimeHour,
                range = 0..23,
                onValueChange = { viewModel.updateTimeHour(it) },
                format = { String.format("%02d", it).toFarsiDigits() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Add another time button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.addCurrentTime() }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "با زدن این دکمه میتوانید ساعت دیگری هم اضافه کنید",
                fontSize = 12.sp,
                color = TextSecondaryColor
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "اضافه کردن زمان",
                    tint = TextSecondaryColor
                )
            }
        }

        // Show added times
        if (state.times.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(state.times) { time ->
                    TimeChip(
                        time = time,
                        onRemove = { viewModel.removeTime(time) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Save button
        Button(
            onClick = { viewModel.onTimeSave() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TealColor)
        ) {
            Text(
                text = "ثبت",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun NumberPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    format: (Int) -> String = { it.toString() }
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Show 5 values: value-2, value-1, value, value+1, value+2
        val displayValues = listOf(
            (value - 2).let { if (it < range.first) range.last - (range.first - it - 1) else it },
            (value - 1).let { if (it < range.first) range.last else it },
            value,
            (value + 1).let { if (it > range.last) range.first else it },
            (value + 2).let { if (it > range.last) range.first + (it - range.last - 1) else it }
        )

        displayValues.forEachIndexed { index, displayValue ->
            val isSelected = index == 2
            val alpha = when (index) {
                0, 4 -> 0.3f
                1, 3 -> 0.6f
                else -> 1f
            }

            Text(
                text = format(displayValue),
                fontSize = if (isSelected) 24.sp else 18.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = TextPrimaryColor.copy(alpha = alpha),
                modifier = Modifier
                    .clickable {
                        when (index) {
                            0 -> onValueChange(
                                if (value - 2 < range.first) range.last - (range.first - value + 1) else value - 2
                            )
                            1 -> onValueChange(if (value - 1 < range.first) range.last else value - 1)
                            3 -> onValueChange(if (value + 1 > range.last) range.first else value + 1)
                            4 -> onValueChange(
                                if (value + 2 > range.last) range.first + (value + 1 - range.last) else value + 2
                            )
                        }
                    }
                    .padding(vertical = 4.dp)
            )

            if (isSelected) {
                Divider(
                    modifier = Modifier.width(40.dp),
                    color = BorderColor
                )
            }
        }
    }
}

@Composable
private fun TimeChip(
    time: MedicationTime,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = TealColor.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, TealColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "حذف",
                    tint = TealColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = time.to24HourString().toFarsiDigits(),
                fontSize = 14.sp,
                color = TealColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ==================== Utility Functions ====================

private fun formatPersianDate(timestamp: Long): String {
    // Simple Persian date formatting
    val calendar = java.util.Calendar.getInstance()
    calendar.timeInMillis = timestamp

    val year = calendar.get(java.util.Calendar.YEAR)
    val month = calendar.get(java.util.Calendar.MONTH) + 1
    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

    // Convert to Persian calendar (simplified)
    // In production, use a proper Persian calendar library
    val persianYear = year - 621

    return "$persianYear/${"۰$month".takeLast(2)}/${"۰$day".takeLast(2)}".toFarsiDigits()
}