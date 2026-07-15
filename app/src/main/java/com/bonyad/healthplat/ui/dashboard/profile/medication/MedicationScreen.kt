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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import com.bonyad.healthplat.R
import com.bonyad.healthplat.domain.model.*
import com.bonyad.healthplat.ui.utils.PersianDateUtils
import com.bonyad.healthplat.ui.utils.rtl
import com.bonyad.healthplat.ui.utils.toFarsiDigits

// Color constants
private val TealColor = Color(0xFF5BA3A3)
private val BackgroundColor = Color(0xFFF5F5F5)
private val CardColor = Color.White
private val TextPrimaryColor = Color(0xFF6B6B6B)
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
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
                // Top row: Title (right) and Toggle (left)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Medication title and name (RTL start = right)
                    Text(
                        text = "${medication.title} ${medication.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimaryColor
                    )

                    // Toggle switch (RTL end = left)
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
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom row: Dot + dosage (right) and date (left)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time and dosage (RTL start = right)
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
                            text = "${medication.times.firstOrNull()?.to24HourString() ?: "00:00"} ${medication.dosage}".toFarsiDigits(),
                            fontSize = 13.sp,
                            color = TextSecondaryColor
                        )
                    }

                    // Start date (RTL end = left)
                    Text(
                        text = formatPersianDate(medication.startDate),
                        fontSize = 12.sp,
                        color = TextTertiaryColor
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
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = CardColor,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            when (state.currentStep) {
                AddMedicationStep.DETAILS -> DetailsStep(state, viewModel)
                AddMedicationStep.FREQUENCY -> FrequencyStep(state, viewModel)
                AddMedicationStep.TIME -> TimeStep(state, viewModel)
            }
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
            style = MaterialTheme.typography.titleMedium,
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
                placeholder = "مانند : قرص قلب ، قرص کبد ...".rtl(),
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
                placeholder = "مانند: یک قرص یا ۱۰۰ میلی گرم...".rtl(),
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
private fun MedicationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = {
            Text(
                text = label,
                color = Color(0xFF383838),
                fontSize = 12.sp
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                color = Color(0xFF868686),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        textStyle = LocalTextStyle.current.copy(
            textAlign = TextAlign.Start,
            color = Color.Black,
            fontSize = 12.sp
        ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF5BA3A3),
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}

// ==================== Step 2: Frequency + Days ====================

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
            style = MaterialTheme.typography.titleMedium,
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

        // Days selection — shown when any frequency is selected
        AnimatedVisibility(
            visible = state.frequency != null,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))

                // Days selection - Row 1 (شنبه to سه شنبه) — RTL: Saturday on the right
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
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Next button
        Button(
            onClick = { viewModel.onFrequencyNext() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = state.isFrequencyAndDaysValid,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TealColor,
                disabledContainerColor = DisabledColor
            )
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
            width = if (isSelected) 1.dp else 1.dp,
            color = if (isSelected) TealColor else BorderColor
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) TealColor.copy(alpha = 0.1f) else Color.White
        )
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (isSelected) TealColor else Color(0xFF6B6B6B),
            fontWeight = FontWeight.Normal
        )
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
            width = 1.dp,
            color = if (isSelected) TealColor else BorderColor
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) TealColor.copy(alpha = 0.1f) else Color.White
        )
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (isSelected) TealColor else Color(0xFF6B6B6B),
            fontWeight = FontWeight.Normal
        )
    }
}

// ==================== Step 3: Time ====================

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
            style = MaterialTheme.typography.titleMedium,
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
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, TealColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = (painterResource(R.drawable.add)),
                    contentDescription = "اضافه کردن زمان",
                    tint = TealColor
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "با زدن این دکمه میتوانید ساعت دیگری هم اضافه کنید",
                fontSize = 16.sp,
                color = Color(0xFF6B6B6B)
            )



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
private fun NumberPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    format: (Int) -> String = { it.toString() }
) {
    val rangeSize = range.last - range.first + 1
    var dragAccumulator by remember { mutableStateOf(0f) }
    val dragThreshold = 14f // Lower threshold for faster scrolling

    fun wrapValue(v: Int): Int {
        val offset = v - range.first
        return range.first + ((offset % rangeSize) + rangeSize) % rangeSize
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.pointerInput(value, range) {
            detectVerticalDragGestures(
                onDragEnd = { dragAccumulator = 0f },
                onDragCancel = { dragAccumulator = 0f }
            ) { _, dragAmount ->
                dragAccumulator += dragAmount
                while (dragAccumulator > dragThreshold) {
                    dragAccumulator -= dragThreshold
                    onValueChange(wrapValue(value - 1))
                }
                while (dragAccumulator < -dragThreshold) {
                    dragAccumulator += dragThreshold
                    onValueChange(wrapValue(value + 1))
                }
            }
        }
    ) {
        // Show 5 values: value-2, value-1, value, value+1, value+2
        val displayValues = listOf(
            wrapValue(value - 2),
            wrapValue(value - 1),
            value,
            wrapValue(value + 1),
            wrapValue(value + 2)
        )

        displayValues.forEachIndexed { index, displayValue ->
            val isSelected = index == 2
            val alpha = when (index) {
                0, 4 -> 0.3f
                1, 3 -> 0.6f
                else -> 1f
            }

            // Teal separator above the selected value
            if (isSelected) {
                HorizontalDivider(
                    modifier = Modifier.width(50.dp),
                    thickness = 1.dp,
                    color = TealColor
                )
            }

            Text(
                text = format(displayValue),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black.copy(alpha = alpha),
                modifier = Modifier
                    .clickable {
                        when (index) {
                            0 -> onValueChange(wrapValue(value - 2))
                            1 -> onValueChange(wrapValue(value - 1))
                            3 -> onValueChange(wrapValue(value + 1))
                            4 -> onValueChange(wrapValue(value + 2))
                        }
                    }
                    .padding(vertical = 4.dp)
            )

            // Teal separator below the selected value
            if (isSelected) {
                HorizontalDivider(
                    modifier = Modifier.width(50.dp),
                    thickness = 1.dp,
                    color = TealColor
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
    val calendar = java.util.Calendar.getInstance()
    calendar.timeInMillis = timestamp

    val gYear = calendar.get(java.util.Calendar.YEAR)
    val gMonth = calendar.get(java.util.Calendar.MONTH) + 1
    val gDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)

    val (jy, jm, jd) = PersianDateUtils.georgianToJalali(gYear, gMonth, gDay)

    return String.format("%04d/%02d/%02d", jy, jm, jd).toFarsiDigits()
}