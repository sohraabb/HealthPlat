package com.bonyad.healthplat.ui.dashboard.care

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bonyad.healthplat.domain.model.CarePermissions
import com.bonyad.healthplat.domain.model.CaregiverUiModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.bonyad.healthplat.R
import androidx.compose.ui.graphics.Color as ComposeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareScreen(
    viewModel: CareViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val myCaregivers by viewModel.myCaregivers.collectAsState()
    val iAmCaregiverFor by viewModel.iAmCaregiverFor.collectAsState()
    val showAddCaregiverDialog by viewModel.showAddCaregiverDialog.collectAsState()
    val showQrCodeDialog by viewModel.showQrCodeDialog.collectAsState()
    val showQrScanner by viewModel.showQrScanner.collectAsState()
    val qrCodeData by viewModel.qrCodeData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pendingPermissions by viewModel.pendingQrPermissions.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Collect UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is CareUiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is CareUiEvent.ShowSuccess -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    // Show QR Scanner as full screen
    if (showQrScanner) {
        QRScannerScreen(
            onQrCodeScanned = { qrData ->
                pendingPermissions?.let { permissions ->
                    viewModel.onQrCodeScanned(qrData, permissions)
                }
            },
            onClose = { viewModel.onDismissQrScanner() }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "مراقب",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = ComposeColor(0xFF2C2C2C), // Explicit dark color
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Info */ }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "اطلاعات",
                            tint = ComposeColor(0xFF666666)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onShowQrCodeDialog() }) {
                        Icon(
                            painter = painterResource(R.drawable.qr_code),
                            contentDescription = "QR Code",
                            tint = ComposeColor(0xFF5BA3A3)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ComposeColor(0xFFF5F5F5),
                    titleContentColor = ComposeColor(0xFF2C2C2C),
                    navigationIconContentColor = ComposeColor(0xFF666666),
                    actionIconContentColor = ComposeColor(0xFF5BA3A3)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAddCaregiverClick() },
                containerColor = ComposeColor.White,
                contentColor = ComposeColor(0xFF5BA3A3),
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "افزودن"
                )
            }
        },
        containerColor = ComposeColor(0xFFF5F5F5)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
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
                        if (myCaregivers.isEmpty() && !isLoading) {
                            EmptyStateMessage(
                                message = "هنوز کسی به عنوان تن‌بار شما ثبت نشده است.\nبرای افزودن، دکمه + را بزنید."
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB
                            ) {
                                items(myCaregivers) { caregiver ->
                                    CaregiverCard(
                                        caregiver = caregiver,
                                        showAcceptButton = false,
                                        onAccept = null,
                                        onEdit = { viewModel.onEditCaregiver(caregiver) },
                                        onDelete = { viewModel.onDeleteCaregiver(caregiver) }
                                    )
                                }
                            }
                        }
                    }

                    CareTab.I_AM_CAREGIVER -> {
                        if (iAmCaregiverFor.isEmpty() && !isLoading) {
                            EmptyStateMessage(
                                message = "شما هنوز تن‌بار کسی نیستید.\nکسانی که شماره شما را ثبت کنند اینجا نمایش داده می‌شوند."
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB
                            ) {
                                items(iAmCaregiverFor) { person ->
                                    CaregiverCard(
                                        caregiver = person,
                                        showAcceptButton = person.isPending,
                                        onAccept = { viewModel.onAcceptCaregiverRequest(person.id) },
                                        onEdit = null,
                                        onDelete = { viewModel.onRemoveCaregiverRole(person) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Loading overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ComposeColor.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ComposeColor(0xFF5BA3A3))
                }
            }
        }
    }

    // Add Caregiver Dialog
    if (showAddCaregiverDialog) {
        AddCaregiverDialog(
            onDismiss = { viewModel.onDismissAddCaregiverDialog() },
            onConfirmPhone = { phoneNumber, permissions ->
                viewModel.onAddCaregiverByPhone(phoneNumber, permissions)
            },
            onScanQr = { permissions ->
                viewModel.onStartQrScanner(permissions)
            }
        )
    }

    // QR Code Display Dialog
    if (showQrCodeDialog && qrCodeData != null) {
        QrCodeDialog(
            qrData = qrCodeData!!,
            onDismiss = { viewModel.onDismissQrCodeDialog() }
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
            .background(ComposeColor.White, RoundedCornerShape(24.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TabButton(
            text = "تن‌یار من",
            isSelected = selectedTab == CareTab.MY_CAREGIVERS,
            onClick = { onTabSelected(CareTab.MY_CAREGIVERS) },
            modifier = Modifier.weight(1f)
        )

        TabButton(
            text = "خودم تن‌یارم",
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
                color = if (isSelected) ComposeColor(0xFF5BA3A3) else ComposeColor.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isSelected) ComposeColor.White else ComposeColor(0xFF666666)
        )
    }
}

@Composable
fun CaregiverCard(
    caregiver: CaregiverUiModel,
    showAcceptButton: Boolean,
    onAccept: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ComposeColor.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Actions (left side in RTL)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = ComposeColor(0xFFE57373),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Edit button (only for MY_CAREGIVERS tab)
                if (onEdit != null) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "ویرایش",
                            tint = ComposeColor(0xFF999999),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Accept button (only for pending requests in I_AM_CAREGIVER tab)
                if (showAcceptButton && onAccept != null) {
                    IconButton(
                        onClick = onAccept,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "پذیرش",
                            tint = ComposeColor(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // User info (right side in RTL)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    // Name
                    Text(
                        text = caregiver.name ?: caregiver.phoneNumber,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = ComposeColor(0xFF2C2C2C)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Status indicator with colored dot
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = getStatusText(caregiver.isPending),
                            style = MaterialTheme.typography.bodySmall,
                            color = getStatusTextColor(caregiver.isPending)
                        )

                        // Status dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(getStatusDotColor(caregiver.isPending))
                        )
                    }
                }

                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ComposeColor(0xFFE8F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = ComposeColor(0xFF5BA3A3),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

// Status helper functions
private fun getStatusText(isPending: Boolean): String {
    return if (isPending) "در انتظار تایید" else "تایید شده"
}

private fun getStatusTextColor(isPending: Boolean): ComposeColor {
    return if (isPending) ComposeColor(0xFFFFA726) else ComposeColor(0xFF66BB6A)
}

private fun getStatusDotColor(isPending: Boolean): ComposeColor {
    return if (isPending) ComposeColor(0xFFFFA726) else ComposeColor(0xFF66BB6A)
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = ComposeColor(0xFFCCCCCC),
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = ComposeColor(0xFF999999),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCaregiverDialog(
    onDismiss: () -> Unit,
    onConfirmPhone: (String, CarePermissions) -> Unit,
    onScanQr: (CarePermissions) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf<AddCaregiverMode>(AddCaregiverMode.PhoneNumber) }

    var heartRate by remember { mutableStateOf(true) }
    var bloodPressure by remember { mutableStateOf(true) }
    var stressLevel by remember { mutableStateOf(false) }
    var sleepQuality by remember { mutableStateOf(false) }

    val hasPermissions = heartRate || bloodPressure || stressLevel || sleepQuality
    val canSubmitPhone = hasPermissions && phoneNumber.length == 11
    val canSubmitQr = hasPermissions

    AlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ComposeColor.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "افزودن تن‌یار",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    color = ComposeColor(0xFF2C2C2C), // Explicit dark color
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Mode Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(ComposeColor(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    // Phone Number Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (selectedMode == AddCaregiverMode.PhoneNumber)
                                    ComposeColor.White else ComposeColor.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedMode = AddCaregiverMode.PhoneNumber },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "شماره همراه",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (selectedMode == AddCaregiverMode.PhoneNumber)
                                    FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (selectedMode == AddCaregiverMode.PhoneNumber)
                                ComposeColor(0xFF5BA3A3) else ComposeColor(0xFF666666)
                        )
                    }

                    // QR Code Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (selectedMode == AddCaregiverMode.QrCode)
                                    ComposeColor.White else ComposeColor.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedMode = AddCaregiverMode.QrCode },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.qr_code),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedMode == AddCaregiverMode.QrCode)
                                    ComposeColor(0xFF5BA3A3) else ComposeColor(0xFF666666)
                            )
                            Text(
                                text = "QR کد",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (selectedMode == AddCaregiverMode.QrCode)
                                        FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (selectedMode == AddCaregiverMode.QrCode)
                                    ComposeColor(0xFF5BA3A3) else ComposeColor(0xFF666666)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Content based on selected mode
                when (selectedMode) {
                    is AddCaregiverMode.PhoneNumber -> {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = {
                                if (it.length <= 11 && it.all { char -> char.isDigit() }) {
                                    phoneNumber = it
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    "شماره همراه",
                                    color = ComposeColor(0xFF666666)
                                )
                            },
                            placeholder = {
                                Text(
                                    "۰۹۱۲۳۴۵۶۷۸۹",
                                    textAlign = TextAlign.End,
                                    color = ComposeColor(0xFF999999),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ComposeColor(0xFF5BA3A3),
                                unfocusedBorderColor = ComposeColor(0xFFE0E0E0),
                                focusedLabelColor = ComposeColor(0xFF5BA3A3),
                                unfocusedLabelColor = ComposeColor(0xFF666666),
                                focusedTextColor = ComposeColor(0xFF2C2C2C),
                                unfocusedTextColor = ComposeColor(0xFF2C2C2C),
                                cursorColor = ComposeColor(0xFF5BA3A3)
                            ),
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.End,
                                color = ComposeColor(0xFF2C2C2C)
                            )
                        )
                    }

                    is AddCaregiverMode.QrCode -> {
                        // QR Code mode info
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ComposeColor(0xFFF5F5F5), RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.qr_code),
                                    contentDescription = null,
                                    tint = ComposeColor(0xFF5BA3A3),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "QR کد تن‌یار خود را اسکن کنید",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ComposeColor(0xFF666666),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Permissions Section
                Text(
                    text = "دسترسی‌های تن‌یار:",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = ComposeColor(0xFF2C2C2C), // Explicit dark color
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Permission Checkboxes
                PermissionCheckbox("ضربان قلب", heartRate) { heartRate = it }
                PermissionCheckbox("فشار خون", bloodPressure) { bloodPressure = it }
                PermissionCheckbox("میزان استرس", stressLevel) { stressLevel = it }
                PermissionCheckbox("پایش خواب", sleepQuality) { sleepQuality = it }

                if (!hasPermissions) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "حداقل یک دسترسی را انتخاب کنید",
                        style = MaterialTheme.typography.bodySmall,
                        color = ComposeColor(0xFFE57373),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Button
                Button(
                    onClick = {
                        val permissions = CarePermissions(
                            heartRate = heartRate,
                            bloodPressure = bloodPressure,
                            stressLevel = stressLevel,
                            sleepQuality = sleepQuality
                        )

                        when (selectedMode) {
                            is AddCaregiverMode.PhoneNumber -> onConfirmPhone(phoneNumber, permissions)
                            is AddCaregiverMode.QrCode -> onScanQr(permissions)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ComposeColor(0xFF5BA3A3),
                        disabledContainerColor = ComposeColor(0xFFE0E0E0)
                    ),
                    enabled = when (selectedMode) {
                        is AddCaregiverMode.PhoneNumber -> canSubmitPhone
                        is AddCaregiverMode.QrCode -> canSubmitQr
                    }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedMode == AddCaregiverMode.QrCode) {
                            Icon(
                                painter = painterResource(R.drawable.qr_code),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = if (selectedMode == AddCaregiverMode.QrCode) "شروع اسکن" else "ثبت تن‌یار",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = ComposeColor.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cancel Button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "انصراف",
                        color = ComposeColor(0xFF999999)
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = ComposeColor(0xFF5BA3A3),
                uncheckedColor = ComposeColor(0xFFCCCCCC)
            )
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (checked) ComposeColor(0xFF2C2C2C) else ComposeColor(0xFF666666),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeDialog(
    qrData: String,
    onDismiss: () -> Unit
) {
    val qrBitmap = remember(qrData) { generateQrCode(qrData) }

    AlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ComposeColor.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "QR کد شما",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = ComposeColor(0xFF2C2C2C) // Explicit dark color
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "دیگران می‌توانند این کد را اسکن کنند\nتا شما را به عنوان تن‌یار اضافه کنند",
                    style = MaterialTheme.typography.bodySmall,
                    color = ComposeColor(0xFF666666),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // QR Code Container
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .background(ComposeColor.White, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        CircularProgressIndicator(
                            color = ComposeColor(0xFF5BA3A3)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ComposeColor(0xFF5BA3A3)
                    )
                ) {
                    Text(
                        text = "بستن",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

private fun generateQrCode(data: String, size: Int = 512): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        bitmap
    } catch (e: Exception) {
        null
    }
}