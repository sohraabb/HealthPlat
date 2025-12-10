package com.bonyad.healthplat.ui.dashboard.care

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

enum class CareTab {
    MY_CAREGIVERS, // تن‌بار من
    I_AM_CAREGIVER  // خودم تن‌بارم
}

data class Caregiver(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val avatarUrl: String? = null,
    val isPending: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareScreen(
    viewModel: CareViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val myCaregivers by viewModel.myCaregivers.collectAsState()
    val iAmCaregiverFor by viewModel.iAmCaregiverFor.collectAsState()
    val showAddCaregiverDialog by viewModel.showAddCaregiverDialog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "مراقبت",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Info */ }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "اطلاعات"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Notifications */ }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "اعلان‌ها"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAddCaregiverClick() },
                containerColor = Color.White,
                contentColor = Color(0xFF5BA3A3),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "افزودن"
                )
            }
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Tab Selector
            CareTabSelector(
                selectedTab = selectedTab,
                onTabSelected = { viewModel.onTabSelected(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Content based on selected tab
            when (selectedTab) {
                CareTab.MY_CAREGIVERS -> {
                    if (myCaregivers.isEmpty()) {
                        EmptyStateMessage(
                            message = "یک نفر رو به عنوان مراقب اضافه کنید تا در صورت اضطراری به او اطلاع داده شود."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(myCaregivers) { caregiver ->
                                CaregiverCard(
                                    caregiver = caregiver,
                                    onEdit = { viewModel.onEditCaregiver(caregiver) },
                                    onDelete = { viewModel.onDeleteCaregiver(caregiver) }
                                )
                            }
                        }
                    }
                }

                CareTab.I_AM_CAREGIVER -> {
                    if (iAmCaregiverFor.isEmpty()) {
                        EmptyStateMessage(
                            message = "کسانی که میخواهند شما تن‌بارشان باشید."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(iAmCaregiverFor) { person ->
                                CaregiverCard(
                                    caregiver = person,
                                    onEdit = null, // Can't edit when you're the caregiver
                                    onDelete = { viewModel.onRemoveCaregiverRole(person) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Caregiver Dialog
    if (showAddCaregiverDialog) {
        AddCaregiverDialog(
            onDismiss = { viewModel.onDismissAddCaregiverDialog() },
            onConfirm = { phoneNumber, devices ->
                viewModel.onAddCaregiver(phoneNumber, devices)
            }
        )
    }
}

@Composable
fun CareTabSelector(
    selectedTab: CareTab,
    onTabSelected: (CareTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TabButton(
            text = "تن‌بار من",
            isSelected = selectedTab == CareTab.MY_CAREGIVERS,
            onClick = { onTabSelected(CareTab.MY_CAREGIVERS) },
            modifier = Modifier.weight(1f)
        )

        TabButton(
            text = "خودم تن‌بارم",
            isSelected = selectedTab == CareTab.I_AM_CAREGIVER,
            onClick = { onTabSelected(CareTab.I_AM_CAREGIVER) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(
                color = if (isSelected) Color(0xFF5BA3A3) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isSelected) Color.White else Color(0xFF666666)
        )
    }
}

@Composable
fun CaregiverCard(
    caregiver: Caregiver,
    onEdit: (() -> Unit)?,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Actions (left side in RTL)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (onEdit != null) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "ویرایش",
                            tint = Color(0xFF5BA3A3),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (!caregiver.isPending) {
                    IconButton(
                        onClick = { /* TODO: Mark as done */ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "تایید",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Info (right side in RTL)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = caregiver.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF2C2C2C)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (caregiver.isPending) "در انتظار تایید" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF999999)
                        )

                        if (caregiver.isPending) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = Color(0xFF999999),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF5BA3A3).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF5BA3A3),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

        }

    }

    Spacer(Modifier.height(16.dp))

}

@Composable
fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF999999),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCaregiverDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Set<String>) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var selectedDevices by remember { mutableStateOf(setOf<String>()) }

    val devices = listOf(
        "ضربان قلب" to "heart_rate",
        "فشار خون" to "blood_pressure",
        "میزان استرس" to "stress",
        "پایش خواب" to "sleep"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(phoneNumber, selectedDevices)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        if (selectedDevices.isNotEmpty() || phoneNumber.length == 11)
                            Color(0xFF5BA3A3)
                        else Color(0xFFE0E0E0)
                ),
                enabled = selectedDevices.isNotEmpty() || phoneNumber.length == 11
            ) {
                Text(
                    text = "ثبت",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
        },
        title = {
            Text(
                text = "دسترسی های لازم را برای تن‌بار خود انتخاب کنید",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                devices.forEach { (label, key) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                selectedDevices =
                                    if (selectedDevices.contains(key))
                                        selectedDevices - key
                                    else
                                        selectedDevices + key
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedDevices.contains(key),
                            onCheckedChange = { checked ->
                                selectedDevices = if (checked)
                                    selectedDevices + key else selectedDevices - key
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF5BA3A3)
                            )
                        )

                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2C2C2C),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    )
}