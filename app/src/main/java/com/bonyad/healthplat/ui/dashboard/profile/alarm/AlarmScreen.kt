package com.bonyad.healthplat.ui.dashboard.profile.alarm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
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
import com.bonyad.healthplat.domain.model.AlarmType
import com.bonyad.healthplat.domain.model.AlarmUiState
import com.bonyad.healthplat.domain.model.HealthAlarm
import com.bonyad.healthplat.ui.utils.toFarsiDigits

// Color constants
private val TealColor = Color(0xFF5BA3A3)
private val BackgroundColor = Color(0xFFF5F5F5)
private val CardColor = Color.White
private val TextPrimaryColor = Color(0xFF2C2C2C)
private val TextSecondaryColor = Color(0xFF666666)
private val TextTertiaryColor = Color(0xFF999999)
private val BorderColor = Color(0xFFE8E8E8)
private val DeleteColor = Color(0xFFE57373)
private val GreenColor = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    onBack: () -> Unit,
    viewModel: AlarmViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showParameterSheet by viewModel.showParameterSheet.collectAsState()
    val showThresholdSheet by viewModel.showThresholdSheet.collectAsState()

        Scaffold(
            topBar = {
                AlarmTopBar(onBack = onBack)
            },
            bottomBar = {
                AddAlarmButton(onClick = { viewModel.onAddAlarmClick() })
            },
            containerColor = BackgroundColor
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (uiState) {
                    is AlarmUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = TealColor
                        )
                    }
                    is AlarmUiState.Empty -> {
                        EmptyAlarmState()
                    }
                    is AlarmUiState.Success -> {
                        AlarmList(
                            alarms = (uiState as AlarmUiState.Success).alarms,
                            onEditClick = { viewModel.onEditAlarmClick(it) },
                            onDeleteClick = { viewModel.deleteAlarm(it.id) }
                        )
                    }
                    is AlarmUiState.Error -> {
                        ErrorState(
                            message = (uiState as AlarmUiState.Error).message,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        // Parameter Selection Bottom Sheet
        if (showParameterSheet) {
            ParameterSelectionBottomSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.dismissParameterSheet() }
            )
        }

        // Threshold Settings Bottom Sheet
        if (showThresholdSheet) {
            ThresholdSettingsBottomSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.dismissThresholdSheet() }
            )
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmTopBar(onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "تنظیم هشدار شخصی",
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
private fun AddAlarmButton(onClick: () -> Unit) {
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
                text = "هشدار جدید",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun EmptyAlarmState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Text at top
        Text(
            text = ".شما تا کنون هیچ هشدار شخصی ای ثبت نکردید",
            fontSize = 16.sp,
            color = TextSecondaryColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        )

        // Image in center
        Image(
            painter = painterResource(id = R.drawable.empty_alarm),
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.Center),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun AlarmList(
    alarms: List<HealthAlarm>,
    onEditClick: (HealthAlarm) -> Unit,
    onDeleteClick: (HealthAlarm) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(alarms, key = { it.id }) { alarm ->
            AlarmCard(
                alarm = alarm,
                onEditClick = { onEditClick(alarm) },
                onDeleteClick = { onDeleteClick(alarm) }
            )
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: HealthAlarm,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Action buttons (left side in RTL)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = DeleteColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Edit button
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "ویرایش",
                        tint = TextTertiaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Alarm info (right side in RTL)
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "هشدار ${alarm.type.persianName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimaryColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (alarm.sendToCaregiver) "فعال برای مراقب" else "فقط برای خودم",
                        fontSize = 12.sp,
                        color = TextTertiaryColor
                    )

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (alarm.isEnabled) GreenColor else TextTertiaryColor,
                                CircleShape
                            )
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
            color = DeleteColor,
            textAlign = TextAlign.Center
        )
    }
}

// ==================== Parameter Selection Bottom Sheet ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParameterSelectionBottomSheet(
    viewModel: AlarmViewModel,
    onDismiss: () -> Unit
) {
    val selectedTypes by viewModel.selectedTypes.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "برای کدام پارامتر شخصی سازی هشدار نیاز دارید؟",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimaryColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Parameter options
            AlarmType.entries.forEach { type ->
                ParameterOption(
                    type = type,
                    isSelected = selectedTypes.contains(type),
                    onClick = { viewModel.toggleAlarmType(type) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submit button
            Button(
                onClick = { viewModel.onParameterSelectionConfirm() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = selectedTypes.isNotEmpty(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealColor,
                    disabledContainerColor = TealColor.copy(alpha = 0.5f)
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
}

@Composable
private fun ParameterOption(
    type: AlarmType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = if (isSelected) TealColor else BorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox
        CustomCheckbox(
            checked = isSelected,
            onCheckedChange = { onClick() }
        )

        // Type info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = type.persianName,
                fontSize = 16.sp,
                color = TextPrimaryColor
            )

            Icon(
                painter = painterResource(getIconForAlarmType(type)),
                contentDescription = null,
                tint = TealColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CustomCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(
                width = 2.dp,
                color = if (checked) TealColor else BorderColor,
                shape = RoundedCornerShape(6.dp)
            )
            .background(
                if (checked) TealColor else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ==================== Threshold Settings Bottom Sheet ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThresholdSettingsBottomSheet(
    viewModel: AlarmViewModel,
    onDismiss: () -> Unit
) {
    val currentType by viewModel.currentAlarmType.collectAsState()
    val minThreshold by viewModel.minThreshold.collectAsState()
    val maxThreshold by viewModel.maxThreshold.collectAsState()
    val sendToCaregiver by viewModel.sendToCaregiver.collectAsState()

    val type = currentType ?: return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "لطفا حد هشدار را تعیین کنید.",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimaryColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = type.description,
                fontSize = 14.sp,
                color = TextSecondaryColor,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Threshold inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Max threshold (right side in RTL - "کمتر از")
                ThresholdInput(
                    label = "کمتر از",
                    value = minThreshold,
                    onValueChange = { viewModel.updateMinThreshold(it) },
                    modifier = Modifier.weight(1f)
                )

                // Min threshold (left side in RTL - "بیشتر از")
                ThresholdInput(
                    label = "بیشتر از",
                    value = maxThreshold,
                    onValueChange = { viewModel.updateMaxThreshold(it) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Send to caregiver option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleSendToCaregiver() }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                CustomCheckbox(
                    checked = sendToCaregiver,
                    onCheckedChange = { viewModel.toggleSendToCaregiver() }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "ارسال هشدار برای مراقب ( با فعال کردن این گزینه، زمانی که ${type.persianName} شما به مرز تعیین شده برسد هشدار برای مراقب شما هم ارسال میشود )",
                    fontSize = 13.sp,
                    color = TextSecondaryColor,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit button
            Button(
                onClick = { viewModel.onThresholdConfirm() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = minThreshold.isNotEmpty() && maxThreshold.isNotEmpty(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealColor,
                    disabledContainerColor = TealColor.copy(alpha = 0.5f)
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
}

@Composable
private fun ThresholdInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextSecondaryColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = value.toFarsiDigits(),
            onValueChange = { newValue ->
                // Convert Persian digits to English for storage
                val englishValue = newValue
                    .replace('۰', '0').replace('۱', '1').replace('۲', '2')
                    .replace('۳', '3').replace('۴', '4').replace('۵', '5')
                    .replace('۶', '6').replace('۷', '7').replace('۸', '8')
                    .replace('۹', '9')
                    .filter { it.isDigit() }
                onValueChange(englishValue)
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryColor
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

// ==================== Helper Functions ====================

private fun getIconForAlarmType(type: AlarmType): Int {
    return when (type) {
        AlarmType.HEART_RATE -> R.drawable.heart
//        AlarmType.BLOOD_PRESSURE -> R.drawable.blood_pressure
        AlarmType.STRESS -> R.drawable.stress
        AlarmType.BLOOD_OXYGEN -> R.drawable.hospital_spo2
//        AlarmType.ARRHYTHMIA -> R.drawable.heart
    }
}