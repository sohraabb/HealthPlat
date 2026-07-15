package com.bonyad.healthplat.ui.dashboard.care

import android.graphics.Bitmap
import android.graphics.Color
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.domain.model.CarePermissions
import com.bonyad.healthplat.domain.model.CaregiverUiModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.bonyad.healthplat.R
import com.bonyad.healthplat.ui.utils.rtl
import com.bonyad.healthplat.ui.components.StandardFloatingActionButton
import com.bonyad.healthplat.ui.dashboard.calory.TealPrimary
import androidx.compose.ui.graphics.Color as ComposeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareScreen(
    viewModel: CareViewModel = hiltViewModel(),
    onNavigateToRoute: (String) -> Unit = {}
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

    // Edit permissions dialog state
    val showEditPermissionsDialog by viewModel.showEditPermissionsDialog.collectAsState()
    val editingCaregiver by viewModel.editingCaregiver.collectAsState()

    // Patient overview state
    val showPatientOverview by viewModel.showPatientOverview.collectAsState()
    val selectedPatient by viewModel.selectedPatient.collectAsState()

    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var dialogErrorMessage by remember { mutableStateOf<String?>(null) }

    // Collect UI events
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is CareUiEvent.ShowError -> {
                    val translatedMessage = translateErrorToFarsi(event.message)
                    if (showAddCaregiverDialog) {
                        dialogErrorMessage = translatedMessage
                    } else {
                        snackbarHostState.showSnackbar(translatedMessage)
                    }
                }
                is CareUiEvent.ShowSuccess -> {
                    dialogErrorMessage = null
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

    // Show Patient Overview as full screen
    if (showPatientOverview && selectedPatient != null) {
        PatientOverviewScreen(
            viewModel = viewModel,
            patient = selectedPatient!!,
            onBack = { viewModel.onDismissPatientOverview() },
            onNavigateToDetail = onNavigateToRoute
        )
        return
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 80.dp)
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "مراقب",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = ComposeColor(0xFF2C2C2C),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Info */ }) {
                        Icon(
                            painter = painterResource(R.drawable.info_circle),
                            contentDescription = "اطلاعات",
                            modifier = Modifier.size(24.dp),
                            tint = ComposeColor(0xFF6B6B6B)
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
                ),
                windowInsets = WindowInsets(top = 8.dp)
            )
        },
        floatingActionButton = {
            if (selectedTab == CareTab.MY_CAREGIVERS) {
                StandardFloatingActionButton(
                    onClick = { viewModel.onAddCaregiverClick() },
                    icon = R.drawable.add,
                    contentDescription = "Add Care",
                    modifier = Modifier.padding(bottom = 80.dp)
                )
            }
        },
        containerColor = ComposeColor(0xFFF5F5F5)
    ) { paddingValues ->

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tab selector as first item
                item {
                    CareTabSelector(
                        selectedTab = selectedTab,
                        onTabSelected = { viewModel.onTabSelected(it) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Content based on tab
                when (selectedTab) {
                    CareTab.MY_CAREGIVERS -> {
                        if (myCaregivers.isEmpty() && !isLoading) {
                            item {
                                EmptyStateMessage(
                                    message = "هنوز کسی به عنوان تن‌بار شما ثبت نشده است.\n.برای افزودن، دکمه + را بزنید"
                                )
                            }
                        } else {
                            items(myCaregivers) { caregiver ->
                                CaregiverCard(
                                    caregiver = caregiver,
                                    showAcceptButton = false,
                                    onAccept = null,
                                    onEdit = { viewModel.onEditCaregiver(caregiver) },
                                    onDelete = { viewModel.onDeleteCaregiver(caregiver) },
                                    onClick = null
                                )
                            }
                        }
                    }

                    CareTab.I_AM_CAREGIVER -> {
                        if (iAmCaregiverFor.isEmpty() && !isLoading) {
                            item {
                                EmptyStateMessage(
                                    message = "شما هنوز تن‌بار کسی نیستید.\nکسانی که شماره شما را ثبت کنند اینجا نمایش داده می‌شوند.".rtl()
                                )
                            }
                        } else {
                            items(iAmCaregiverFor) { person ->
                                CaregiverCard(
                                    caregiver = person,
                                    showAcceptButton = person.isPending,
                                    onAccept = { viewModel.onAcceptCaregiverRequest(person.id) },
                                    onEdit = null,
                                    onDelete = { viewModel.onRemoveCaregiverRole(person) },
                                    onClick = {
                                        if (!person.isPending) {
                                            viewModel.onOpenPatientOverview(person)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Loading overlay stays outside LazyColumn, inside PullToRefreshBox
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
            onDismiss = {
                dialogErrorMessage = null
                viewModel.onDismissAddCaregiverDialog()
            },
            onConfirmPhone = { phoneNumber, permissions ->
                viewModel.onAddCaregiverByPhone(phoneNumber, permissions)
            },
            onScanQr = { permissions ->
                viewModel.onStartQrScanner(permissions)
            },
            errorMessage = dialogErrorMessage,
            onClearError = { dialogErrorMessage = null }
        )
    }

    // QR Code Display Dialog
    if (showQrCodeDialog && qrCodeData != null) {
        QrCodeDialog(
            qrData = qrCodeData!!,
            onDismiss = { viewModel.onDismissQrCodeDialog() }
        )
    }

    // Edit Permissions Dialog
    if (showEditPermissionsDialog && editingCaregiver != null) {
        EditPermissionsDialog(
            caregiver = editingCaregiver!!,
            onDismiss = { viewModel.onDismissEditPermissionsDialog() },
            onSave = { careId, permissions ->
                viewModel.onSavePermissions(careId, permissions)
            }
        )
    }
}

// ============ Edit Permissions Dialog (NEW) ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPermissionsDialog(
    caregiver: CaregiverUiModel,
    onDismiss: () -> Unit,
    onSave: (Int, CarePermissions) -> Unit
) {
    var heartRate by remember { mutableStateOf(caregiver.permissions.heartRate) }
    var spo2 by remember { mutableStateOf(caregiver.permissions.bloodPressure) }
    var stressLevel by remember { mutableStateOf(caregiver.permissions.stressLevel) }
    var sleepQuality by remember { mutableStateOf(caregiver.permissions.sleepQuality) }

    val hasPermissions = heartRate || spo2 || stressLevel || sleepQuality

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
                    text = "ویرایش دسترسی‌ها",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    color = ComposeColor(0xFF2C2C2C),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = caregiver.name ?: caregiver.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ComposeColor(0xFF666666),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                PermissionCheckboxWithIcon(
                    label = "ضربان قلب",
                    checked = heartRate,
                    onCheckedChange = { heartRate = it },
                    icon = painterResource(R.drawable.heart)
                )
                PermissionCheckboxWithIcon(
                    label = "اکسیژن خون",
                    checked = spo2,
                    onCheckedChange = { spo2 = it },
                    icon = painterResource(R.drawable.hospital_spo2)
                )
                PermissionCheckboxWithIcon(
                    label = "میزان استرس",
                    checked = stressLevel,
                    onCheckedChange = { stressLevel = it },
                    icon = painterResource(R.drawable.stress)
                )
                PermissionCheckboxWithIcon(
                    label = "پایش خواب",
                    checked = sleepQuality,
                    onCheckedChange = { sleepQuality = it },
                    icon = painterResource(R.drawable.sleep_icon)
                )

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

                Button(
                    onClick = {
                        onSave(
                            caregiver.id,
                            CarePermissions(
                                heartRate = heartRate,
                                bloodPressure = spo2,
                                stressLevel = stressLevel,
                                sleepQuality = sleepQuality
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ComposeColor(0xFF5BA3A3),
                        disabledContainerColor = ComposeColor(0xFFE0E0E0)
                    ),
                    enabled = hasPermissions
                ) {
                    Text(
                        text = "ذخیره تغییرات",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = ComposeColor.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

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

// ============ Error Translation ============

private fun translateErrorToFarsi(message: String): String {
    return when {
        message.contains("already exists", ignoreCase = true) ||
                message.contains("already submitted", ignoreCase = true) ||
                message.contains("duplicate", ignoreCase = true) ||
                message.contains("already registered", ignoreCase = true) ||
                message.contains("already added", ignoreCase = true) ->
            "این شماره قبلاً ثبت شده است"

        message.contains("not found", ignoreCase = true) ||
                message.contains("user not found", ignoreCase = true) ->
            "کاربری با این شماره یافت نشد"

        message.contains("invalid phone", ignoreCase = true) ||
                message.contains("invalid number", ignoreCase = true) ->
            "شماره همراه نامعتبر است"

        message.contains("network", ignoreCase = true) ||
                message.contains("connection", ignoreCase = true) ||
                message.contains("internet", ignoreCase = true) ->
            "خطا در اتصال به اینترنت"

        message.contains("unauthorized", ignoreCase = true) ||
                message.contains("authentication", ignoreCase = true) ->
            "لطفاً دوباره وارد شوید"

        message.contains("server error", ignoreCase = true) ||
                message.contains("internal error", ignoreCase = true) ->
            "خطای سرور. لطفاً دوباره تلاش کنید"

        message.contains("timeout", ignoreCase = true) ->
            "زمان درخواست به پایان رسید"

        message.contains("permission", ignoreCase = true) ->
            "دسترسی مجاز نیست"

        message.contains("Verification SMS sent", ignoreCase = true) ->
            "پیامک تایید ارسال شد. پس از ثبت‌نام تن‌یار متصل خواهد شد.".rtl()

        else -> message
    }
}

// ============ Tab Selector ============

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
            text = "خودم تن‌یارم",
            isSelected = selectedTab == CareTab.I_AM_CAREGIVER,
            onClick = { onTabSelected(CareTab.I_AM_CAREGIVER) },
            modifier = Modifier.weight(1f)
        )

        TabButton(
            text = "تن‌یار من",
            isSelected = selectedTab == CareTab.MY_CAREGIVERS,
            onClick = { onTabSelected(CareTab.MY_CAREGIVERS) },
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

// ============ Caregiver Card (Updated with onClick) ============

@Composable
fun CaregiverCard(
    caregiver: CaregiverUiModel,
    showAcceptButton: Boolean,
    onAccept: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onDelete: () -> Unit,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            ),
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
                        painter = painterResource(R.drawable.delete),
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
                            painter = painterResource(R.drawable.edit),
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
                            painter = painterResource(R.drawable.check_sqaure),
                            contentDescription = "پذیرش",
                            tint = ComposeColor(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Arrow indicator for clickable patient cards
                if (onClick != null && !caregiver.isPending) {
                    Icon(
                        painter = painterResource(R.drawable.check_sqaure),
                        contentDescription = "مشاهده",
                        tint = ComposeColor(0xFF5BA3A3),
                        modifier = Modifier.size(24.dp)
                    )
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
                    // Name or phone
                    Text(
                        text = (caregiver.name ?: caregiver.phoneNumber).toString(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = ComposeColor(0xFF2C2C2C)
                    )

                    // Show phone below name if name exists
                    if (caregiver.name != null) {
                        Text(
                            text = caregiver.phoneNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = ComposeColor(0xFF999999)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Status indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = getStatusText(caregiver.isPending),
                            style = MaterialTheme.typography.bodySmall,
                            color = getStatusTextColor(caregiver.isPending)
                        )

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

// ============ Add Caregiver Dialog ============

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCaregiverDialog(
    onDismiss: () -> Unit,
    onConfirmPhone: (String, CarePermissions) -> Unit,
    onScanQr: (CarePermissions) -> Unit,
    errorMessage: String? = null,
    onClearError: () -> Unit = {}
) {
    var phoneNumber by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf<AddCaregiverMode>(AddCaregiverMode.PhoneNumber) }

    var heartRate by remember { mutableStateOf(true) }
    var spo2 by remember { mutableStateOf(true) }
    var stressLevel by remember { mutableStateOf(false) }
    var sleepQuality by remember { mutableStateOf(false) }

    val hasPermissions = heartRate || spo2 || stressLevel || sleepQuality
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
                    color = ComposeColor(0xFF2C2C2C),
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (selectedMode == AddCaregiverMode.QrCode)
                                    ComposeColor.White else ComposeColor.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                selectedMode = AddCaregiverMode.QrCode
                                onClearError()
                            },
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

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (selectedMode == AddCaregiverMode.PhoneNumber)
                                    ComposeColor.White else ComposeColor.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                selectedMode = AddCaregiverMode.PhoneNumber
                                onClearError()
                            },
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
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Content based on mode
                when (selectedMode) {
                    is AddCaregiverMode.PhoneNumber -> {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = {
                                    if (it.length <= 11 && it.all(Char::isDigit)) {
                                        phoneNumber = it
                                        onClearError()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text(
                                        text = "شماره همراه",
                                        color = ComposeColor(0xFF666666)
                                    )
                                },
                                placeholder = {
                                    Text(
                                        text = "09123456789",
                                        color = ComposeColor(0xFF999999)
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone
                                ),
                                isError = errorMessage != null,
                                supportingText = errorMessage?.let {
                                    {
                                        Text(
                                            text = it,
                                            color = ComposeColor(0xFFE57373),
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ComposeColor(0xFF5BA3A3),
                                    unfocusedBorderColor = ComposeColor(0xFFE0E0E0),
                                    errorBorderColor = ComposeColor(0xFFE57373),
                                    focusedLabelColor = ComposeColor(0xFF5BA3A3),
                                    unfocusedLabelColor = ComposeColor(0xFF666666),
                                    cursorColor = ComposeColor(0xFF5BA3A3)
                                ),
                                textStyle = LocalTextStyle.current.copy(
                                    color = ComposeColor(0xFF2C2C2C),
                                    textDirection = TextDirection.Ltr
                                )
                            )
                        }
                    }

                    is AddCaregiverMode.QrCode -> {
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
                    color = ComposeColor(0xFF2C2C2C),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )

                Spacer(modifier = Modifier.height(12.dp))

                PermissionCheckboxWithIcon(
                    label = "ضربان قلب",
                    checked = heartRate,
                    onCheckedChange = { heartRate = it },
                    icon = painterResource(R.drawable.heart)
                )
                PermissionCheckboxWithIcon(
                    label = "اکسیژن خون",
                    checked = spo2,
                    onCheckedChange = { spo2 = it },
                    icon = painterResource(R.drawable.hospital_spo2)
                )
                PermissionCheckboxWithIcon(
                    label = "میزان استرس",
                    checked = stressLevel,
                    onCheckedChange = { stressLevel = it },
                    icon = painterResource(R.drawable.stress)
                )
                PermissionCheckboxWithIcon(
                    label = "پایش خواب",
                    checked = sleepQuality,
                    onCheckedChange = { sleepQuality = it },
                    icon = painterResource(R.drawable.sleep_icon)
                )

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

                Button(
                    onClick = {
                        val permissions = CarePermissions(
                            heartRate = heartRate,
                            bloodPressure = spo2,
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

// ============ Checkbox Components ============

@Composable
fun FilledCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (checked) ComposeColor(0xFF5BA3A3) else ComposeColor.Transparent
            )
            .border(
                width = 2.dp,
                color = if (checked) ComposeColor(0xFF5BA3A3) else ComposeColor(0xFFCCCCCC),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        // Filled color only — no check icon
    }
}

@Composable
fun PermissionCheckboxWithIcon(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: Painter
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                color = ComposeColor(0xFFF5F5F5),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledCheckbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (checked) ComposeColor(0xFF2C2C2C) else ComposeColor(0xFF666666),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            painter = icon,
            contentDescription = null,
            tint = if (checked) ComposeColor(0xFF5BA3A3) else ComposeColor(0xFFCCCCCC),
            modifier = Modifier.size(20.dp)
        )
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

// ============ QR Code Dialog ============

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
                    color = ComposeColor(0xFF2C2C2C)
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