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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bonyad.healthplat.domain.model.CarePermissions
import com.bonyad.healthplat.domain.model.CaregiverUiModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
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
    val qrCodeData by viewModel.qrCodeData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    IconButton(onClick = { viewModel.onShowQrCodeDialog() }) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "QR Code"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ComposeColor.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAddCaregiverClick() },
                containerColor = ComposeColor.White,
                contentColor = ComposeColor(0xFF5BA3A3),
                shape = CircleShape
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
                // TODO: Launch QR scanner
                // For now, show a toast
                scope.launch {
                    snackbarHostState.showSnackbar("قابلیت اسکن QR به زودی...")
                }
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
            .background(ComposeColor.White, RoundedCornerShape(12.dp))
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
                color = if (isSelected) ComposeColor(0xFF5BA3A3) else ComposeColor.Transparent,
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ComposeColor.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            tint = ComposeColor(0xFFE53935),
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
                                tint = ComposeColor(0xFF5BA3A3),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (showAcceptButton && onAccept != null) {
                        IconButton(
                            onClick = onAccept,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "پذیرش",
                                tint = ComposeColor(0xFF4CAF50),
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
                            text = caregiver.name ?: caregiver.phoneNumber,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = ComposeColor(0xFF2C2C2C)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (caregiver.isPending) "در انتظار تایید" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = ComposeColor(0xFF999999)
                            )

                            if (caregiver.isPending) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = ComposeColor(0xFF999999),
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
                            .background(ComposeColor(0xFF5BA3A3).copy(alpha = 0.1f)),
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

            // Show permissions
            if (caregiver.permissions.heartRate || caregiver.permissions.bloodPressure ||
                caregiver.permissions.stressLevel || caregiver.permissions.sleepQuality) {

                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "دسترسی‌ها:",
                    style = MaterialTheme.typography.bodySmall,
                    color = ComposeColor(0xFF666666),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (caregiver.permissions.heartRate) {
                        PermissionChip("ضربان قلب")
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (caregiver.permissions.bloodPressure) {
                        PermissionChip("فشار خون")
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (caregiver.permissions.stressLevel) {
                        PermissionChip("میزان استرس")
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (caregiver.permissions.sleepQuality) {
                        PermissionChip("پایش خواب")
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionChip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = ComposeColor(0xFF5BA3A3).copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = ComposeColor(0xFF5BA3A3),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
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
            color = ComposeColor(0xFF999999),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
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
    var selectedMode by remember { mutableStateOf(AddCaregiverMode.PhoneNumber) }

    var heartRate by remember { mutableStateOf(false) }
    var bloodPressure by remember { mutableStateOf(false) }
    var stressLevel by remember { mutableStateOf(false) }
    var sleepQuality by remember { mutableStateOf(false) }

    val hasPermissions = heartRate || bloodPressure || stressLevel || sleepQuality
    val canSubmit = hasPermissions && (selectedMode == AddCaregiverMode.QrCode || phoneNumber.length == 11)

    AlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ComposeColor.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "شماره تن‌بار خود را وارد کنید",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(ComposeColor(0xFFF5F5F5), RoundedCornerShape(8.dp))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (selectedMode == AddCaregiverMode.PhoneNumber)
                                    ComposeColor.White else ComposeColor.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { selectedMode = AddCaregiverMode.PhoneNumber },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "شماره همراه",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (selectedMode == AddCaregiverMode.QrCode)
                                    ComposeColor.White else ComposeColor.Transparent,
                                RoundedCornerShape(6.dp)
                            )
//                                selectedMode = AddCaregiverMode.QrCode
                            .clickable {  },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "QR کد",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Phone Number Input (only show for PhoneNumber mode)
                if (selectedMode == AddCaregiverMode.PhoneNumber) {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = {
                            if (it.length <= 11 && it.all { char -> char.isDigit() }) {
                                phoneNumber = it
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("شماره همراه") },
                        placeholder = { Text("09123456789") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = "دسترسی های لازم را برای تن‌بار خود انتخاب کنید",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Permissions
                PermissionCheckbox("ضربان قلب", heartRate) { heartRate = it }
                PermissionCheckbox("فشار خون", bloodPressure) { bloodPressure = it }
                PermissionCheckbox("میزان استرس", stressLevel) { stressLevel = it }
                PermissionCheckbox("پایش خواب", sleepQuality) { sleepQuality = it }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val permissions = CarePermissions(
                            heartRate = heartRate,
                            bloodPressure = bloodPressure,
                            stressLevel = stressLevel,
                            sleepQuality = sleepQuality
                        )

                        when (selectedMode) {
                            AddCaregiverMode.PhoneNumber -> onConfirmPhone(phoneNumber, permissions)
                            AddCaregiverMode.QrCode -> onScanQr(permissions)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canSubmit) ComposeColor(0xFF5BA3A3) else ComposeColor(0xFFE0E0E0)
                    ),
                    enabled = canSubmit
                ) {
                    Text(
                        text = if (selectedMode == AddCaregiverMode.QrCode) "اسکن QR" else "ثبت",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = ComposeColor.White
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
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = ComposeColor(0xFF5BA3A3)
            )
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = ComposeColor(0xFF2C2C2C),
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
            shape = RoundedCornerShape(16.dp),
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
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "دیگران می‌توانند این کد را اسکن کنند تا به عنوان تن‌بار شما اضافه شوند",
                    style = MaterialTheme.typography.bodySmall,
                    color = ComposeColor(0xFF666666),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(250.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ComposeColor(0xFF5BA3A3)
                    )
                ) {
                    Text("بستن")
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