package com.bonyad.healthplat.ui.dashboard.care

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.data.repository.AuthResult
import com.bonyad.healthplat.data.repository.CareRepository
import com.bonyad.healthplat.domain.model.CarePermissions
import com.bonyad.healthplat.domain.model.CaregiverUiModel
import com.bonyad.healthplat.domain.model.MetricData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

sealed class AddCaregiverMode {
    object PhoneNumber : AddCaregiverMode()
    object QrCode : AddCaregiverMode()
}

sealed class CareUiEvent {
    data class ShowError(val message: String) : CareUiEvent()
    data class ShowSuccess(val message: String) : CareUiEvent()
}

enum class CareTab {
    MY_CAREGIVERS,  // تن‌یار من - People taking care of me
    I_AM_CAREGIVER  // خودم تن‌یارم - People I'm taking care of
}

@HiltViewModel
class CareViewModel @Inject constructor(
    private val careRepository: CareRepository
) : ViewModel() {

    // ============ Tab State ============
    private val _selectedTab = MutableStateFlow(CareTab.MY_CAREGIVERS)
    val selectedTab: StateFlow<CareTab> = _selectedTab.asStateFlow()

    // ============ Data States ============
    private val _myCaregivers = MutableStateFlow<List<CaregiverUiModel>>(emptyList())
    val myCaregivers: StateFlow<List<CaregiverUiModel>> = _myCaregivers.asStateFlow()

    private val _iAmCaregiverFor = MutableStateFlow<List<CaregiverUiModel>>(emptyList())
    val iAmCaregiverFor: StateFlow<List<CaregiverUiModel>> = _iAmCaregiverFor.asStateFlow()

    // ============ Dialog States ============
    private val _showAddCaregiverDialog = MutableStateFlow(false)
    val showAddCaregiverDialog: StateFlow<Boolean> = _showAddCaregiverDialog.asStateFlow()

    private val _showQrCodeDialog = MutableStateFlow(false)
    val showQrCodeDialog: StateFlow<Boolean> = _showQrCodeDialog.asStateFlow()

    private val _showQrScanner = MutableStateFlow(false)
    val showQrScanner: StateFlow<Boolean> = _showQrScanner.asStateFlow()

    // Edit permissions dialog
    private val _showEditPermissionsDialog = MutableStateFlow(false)
    val showEditPermissionsDialog: StateFlow<Boolean> = _showEditPermissionsDialog.asStateFlow()

    private val _editingCaregiver = MutableStateFlow<CaregiverUiModel?>(null)
    val editingCaregiver: StateFlow<CaregiverUiModel?> = _editingCaregiver.asStateFlow()

    // Patient overview
    private val _selectedPatient = MutableStateFlow<CaregiverUiModel?>(null)
    val selectedPatient: StateFlow<CaregiverUiModel?> = _selectedPatient.asStateFlow()

    private val _showPatientOverview = MutableStateFlow(false)
    val showPatientOverview: StateFlow<Boolean> = _showPatientOverview.asStateFlow()

    // Patient overview metrics
    private val _patientHeartRate = MutableStateFlow<List<MetricData>>(emptyList())
    val patientHeartRate: StateFlow<List<MetricData>> = _patientHeartRate.asStateFlow()

    private val _patientSleep = MutableStateFlow<List<MetricData>>(emptyList())
    val patientSleep: StateFlow<List<MetricData>> = _patientSleep.asStateFlow()

    private val _patientSpo2 = MutableStateFlow<List<MetricData>>(emptyList())
    val patientSpo2: StateFlow<List<MetricData>> = _patientSpo2.asStateFlow()

    private val _patientStress = MutableStateFlow<List<MetricData>>(emptyList())
    val patientStress: StateFlow<List<MetricData>> = _patientStress.asStateFlow()

    // ============ QR Code Data ============
    private val _qrCodeData = MutableStateFlow<String?>(null)
    val qrCodeData: StateFlow<String?> = _qrCodeData.asStateFlow()

    private val _pendingQrPermissions = MutableStateFlow<CarePermissions?>(null)
    val pendingQrPermissions: StateFlow<CarePermissions?> = _pendingQrPermissions.asStateFlow()

    // ============ Loading State ============
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ============ UI Events ============
    private val _uiEvents = MutableSharedFlow<CareUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadAllData()
    }

    // ============ Tab Selection ============
    fun onTabSelected(tab: CareTab) {
        _selectedTab.value = tab
        loadDataForCurrentTab()
    }

    // ============ Data Loading ============
    private fun loadAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            loadCaregivers()
            loadMyPatients()
            _isLoading.value = false
        }
    }

    private fun loadDataForCurrentTab() {
        viewModelScope.launch {
            _isLoading.value = true
            when (_selectedTab.value) {
                CareTab.MY_CAREGIVERS -> loadCaregivers()
                CareTab.I_AM_CAREGIVER -> loadMyPatients()
            }
            _isLoading.value = false
        }
    }

    fun loadCareData() {
        loadDataForCurrentTab()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                loadCaregivers()
                loadMyPatients()

            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun loadCaregivers() {
        when (val result = careRepository.getMyCaregivers()) {
            is AuthResult.Success -> {
                _myCaregivers.value = result.data
                Timber.i("✅ Loaded ${result.data.size} caregivers")
            }
            is AuthResult.Error -> {
                Timber.e("❌ Failed to load caregivers: ${result.message}")
                _uiEvents.emit(CareUiEvent.ShowError(result.message))
            }
        }
    }

    private suspend fun loadMyPatients() {
        when (val result = careRepository.getMyPatients()) {
            is AuthResult.Success -> {
                _iAmCaregiverFor.value = result.data
                Timber.i("✅ Loaded ${result.data.size} patients I'm caring for")
            }
            is AuthResult.Error -> {
                Timber.e("❌ Failed to load patients: ${result.message}")
                _uiEvents.emit(CareUiEvent.ShowError(result.message))
            }
        }
    }

    // ============ Add Caregiver Dialog ============
    fun onAddCaregiverClick() {
        _showAddCaregiverDialog.value = true
    }

    fun onDismissAddCaregiverDialog() {
        _showAddCaregiverDialog.value = false
        _pendingQrPermissions.value = null
    }

    // ============ QR Code Display Dialog ============
    fun onShowQrCodeDialog() {
        viewModelScope.launch {
            _isLoading.value = true
            val qrData = careRepository.generateQrCodeData()
            _qrCodeData.value = qrData
            _showQrCodeDialog.value = true
            _isLoading.value = false
        }
    }

    fun onDismissQrCodeDialog() {
        _showQrCodeDialog.value = false
        _qrCodeData.value = null
    }

    // ============ QR Scanner ============
    fun onStartQrScanner(permissions: CarePermissions) {
        _pendingQrPermissions.value = permissions
        _showAddCaregiverDialog.value = false
        _showQrScanner.value = true
    }

    fun onDismissQrScanner() {
        _showQrScanner.value = false
        _pendingQrPermissions.value = null
    }

    // ============ Add Caregiver Actions ============

    fun onAddCaregiverByPhone(phoneNumber: String, permissions: CarePermissions) {
        viewModelScope.launch {
            _isLoading.value = true

            when (val result = careRepository.addCaregiverByPhone(phoneNumber, permissions)) {
                is AuthResult.Success -> {
                    Timber.i("✅ Caregiver added successfully")
                    _uiEvents.emit(CareUiEvent.ShowSuccess("درخواست با موفقیت ارسال شد"))
                    _showAddCaregiverDialog.value = false
                    loadAllData()
                }
                is AuthResult.Error -> {
                    Timber.e("❌ Failed to add caregiver: ${result.message}")
                    _uiEvents.emit(CareUiEvent.ShowError(result.message))
                }
            }

            _isLoading.value = false
        }
    }

    fun onQrCodeScanned(qrData: String, permissions: CarePermissions) {
        viewModelScope.launch {
            _showQrScanner.value = false

            val userId = careRepository.parseQrCodeData(qrData)

            if (userId == null) {
                _uiEvents.emit(CareUiEvent.ShowError("QR کد نامعتبر یا منقضی شده است"))
                _pendingQrPermissions.value = null
                return@launch
            }

            _isLoading.value = true

            when (val result = careRepository.addCaregiverByScan(userId, permissions)) {
                is AuthResult.Success -> {
                    Timber.i("✅ Caregiver added by QR code")
                    _uiEvents.emit(CareUiEvent.ShowSuccess("تن‌یار با موفقیت اضافه شد"))
                    loadAllData()
                }
                is AuthResult.Error -> {
                    Timber.e("❌ Failed to add caregiver by QR: ${result.message}")
                    _uiEvents.emit(CareUiEvent.ShowError(result.message))
                }
            }

            _pendingQrPermissions.value = null
            _isLoading.value = false
        }
    }

    // ============ Caregiver Actions ============

    fun onAcceptCaregiverRequest(careId: Int) {
        viewModelScope.launch {
            _isLoading.value = true

            when (val result = careRepository.acceptCaregiverRequest(careId)) {
                is AuthResult.Success -> {
                    Timber.i("✅ Caregiver request accepted")
                    _uiEvents.emit(CareUiEvent.ShowSuccess("درخواست پذیرفته شد"))
                    loadAllData()
                }
                is AuthResult.Error -> {
                    Timber.e("❌ Failed to accept request: ${result.message}")
                    _uiEvents.emit(CareUiEvent.ShowError(result.message))
                }
            }

            _isLoading.value = false
        }
    }

    // ============ Edit Permissions ============

    fun onEditCaregiver(caregiver: CaregiverUiModel) {
        _editingCaregiver.value = caregiver
        _showEditPermissionsDialog.value = true
    }

    fun onDismissEditPermissionsDialog() {
        _showEditPermissionsDialog.value = false
        _editingCaregiver.value = null
    }

    fun onSavePermissions(careId: Int, permissions: CarePermissions) {
        viewModelScope.launch {
            _isLoading.value = true

            when (val result = careRepository.updateCaregiverPermissions(careId, permissions)) {
                is AuthResult.Success -> {
                    Timber.i("✅ Permissions updated")
                    _uiEvents.emit(CareUiEvent.ShowSuccess("دسترسی‌ها به‌روزرسانی شد"))
                    _showEditPermissionsDialog.value = false
                    _editingCaregiver.value = null
                    loadAllData()
                }
                is AuthResult.Error -> {
                    Timber.e("❌ Failed to update permissions: ${result.message}")
                    _uiEvents.emit(CareUiEvent.ShowError(result.message))
                }
            }

            _isLoading.value = false
        }
    }

    // ============ Delete ============

    fun onDeleteCaregiver(caregiver: CaregiverUiModel) {
        viewModelScope.launch {
            _isLoading.value = true

            when (val result = careRepository.deleteCaregiver(caregiver.id)) {
                is AuthResult.Success -> {
                    Timber.i("✅ Caregiver deleted")
                    _uiEvents.emit(CareUiEvent.ShowSuccess("تن‌یار حذف شد"))
                    loadAllData()
                }
                is AuthResult.Error -> {
                    Timber.e("❌ Failed to delete caregiver: ${result.message}")
                    _uiEvents.emit(CareUiEvent.ShowError(result.message))
                }
            }

            _isLoading.value = false
        }
    }

    fun onRemoveCaregiverRole(person: CaregiverUiModel) {
        viewModelScope.launch {
            _isLoading.value = true

            when (val result = careRepository.deleteCaregiver(person.id)) {
                is AuthResult.Success -> {
                    Timber.i("✅ Caregiver role removed")
                    _uiEvents.emit(CareUiEvent.ShowSuccess("نقش تن‌بار حذف شد"))
                    loadAllData()
                }
                is AuthResult.Error -> {
                    Timber.e("❌ Failed to remove caregiver role: ${result.message}")
                    _uiEvents.emit(CareUiEvent.ShowError(result.message))
                }
            }

            _isLoading.value = false
        }
    }

    // ============ Patient Overview ============

    fun onOpenPatientOverview(patient: CaregiverUiModel) {
        if (patient.isPending) {
            viewModelScope.launch {
                _uiEvents.emit(CareUiEvent.ShowError("ابتدا باید درخواست تایید شود"))
            }
            return
        }

        _selectedPatient.value = patient
        _showPatientOverview.value = true
        loadPatientMetrics(patient)
    }

    fun onDismissPatientOverview() {
        _showPatientOverview.value = false
        _selectedPatient.value = null
        _patientHeartRate.value = emptyList()
        _patientSleep.value = emptyList()
        _patientSpo2.value = emptyList()
        _patientStress.value = emptyList()
    }

    private fun loadPatientMetrics(patient: CaregiverUiModel) {
        val patientUserId = patient.patientId ?: return

        val today = LocalDate.now()
        val dateFrom = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dateTo = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

        viewModelScope.launch {
            _isLoading.value = true

            // Load all permitted metrics in parallel
            if (patient.permissions.heartRate) {
                when (val result = careRepository.getPatientHeartRate(patientUserId, dateFrom, dateTo)) {
                    is AuthResult.Success -> _patientHeartRate.value = result.data
                    is AuthResult.Error -> Timber.w("Could not load heart rate: ${result.message}")
                }
            }

            if (patient.permissions.sleepQuality) {
                when (val result = careRepository.getPatientSleep(patientUserId, dateFrom, dateTo)) {
                    is AuthResult.Success -> _patientSleep.value = result.data
                    is AuthResult.Error -> Timber.w("Could not load sleep: ${result.message}")
                }
            }

            if (patient.permissions.bloodPressure) {
                when (val result = careRepository.getPatientSpo2(patientUserId, dateFrom, dateTo)) {
                    is AuthResult.Success -> _patientSpo2.value = result.data
                    is AuthResult.Error -> Timber.w("Could not load spo2: ${result.message}")
                }
            }

            if (patient.permissions.stressLevel) {
                when (val result = careRepository.getPatientStress(patientUserId, dateFrom, dateTo)) {
                    is AuthResult.Success -> _patientStress.value = result.data
                    is AuthResult.Error -> Timber.w("Could not load stress: ${result.message}")
                }
            }

            _isLoading.value = false
        }
    }
}