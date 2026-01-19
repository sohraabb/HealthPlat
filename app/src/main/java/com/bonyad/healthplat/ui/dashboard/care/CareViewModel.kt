package com.bonyad.healthplat.ui.dashboard.care

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.data.repository.AuthResult
import com.bonyad.healthplat.data.repository.CareRepository
import com.bonyad.healthplat.domain.model.CarePermissions
import com.bonyad.healthplat.domain.model.CaregiverUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
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

    // ============ QR Code Data ============
    private val _qrCodeData = MutableStateFlow<String?>(null)
    val qrCodeData: StateFlow<String?> = _qrCodeData.asStateFlow()

    // Store permissions temporarily when starting QR scanner
    private val _pendingQrPermissions = MutableStateFlow<CarePermissions?>(null)
    val pendingQrPermissions: StateFlow<CarePermissions?> = _pendingQrPermissions.asStateFlow()

    // ============ Loading State ============
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ============ UI Events ============
    private val _uiEvents = MutableSharedFlow<CareUiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

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

            // Load both lists
            loadCaregivers()
            loadMyUsers()

            _isLoading.value = false
        }
    }

    private fun loadDataForCurrentTab() {
        viewModelScope.launch {
            _isLoading.value = true

            when (_selectedTab.value) {
                CareTab.MY_CAREGIVERS -> loadCaregivers()
                CareTab.I_AM_CAREGIVER -> loadMyUsers()
            }

            _isLoading.value = false
        }
    }

    fun loadCareData() {
        loadDataForCurrentTab()
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

    private suspend fun loadMyUsers() {
        when (val result = careRepository.getMyUsers()) {
            is AuthResult.Success -> {
                _iAmCaregiverFor.value = result.data
                Timber.i("✅ Loaded ${result.data.size} users I'm caring for")
            }
            is AuthResult.Error -> {
                Timber.e("❌ Failed to load users: ${result.message}")
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
        // Store permissions for use after scanning
        _pendingQrPermissions.value = permissions
        // Close the add dialog
        _showAddCaregiverDialog.value = false
        // Show scanner
        _showQrScanner.value = true
    }

    fun onDismissQrScanner() {
        _showQrScanner.value = false
        _pendingQrPermissions.value = null
    }

    // ============ Add Caregiver Actions ============

    /**
     * Add caregiver by phone number
     */
    fun onAddCaregiverByPhone(
        phoneNumber: String,
        permissions: CarePermissions
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            when (val result = careRepository.addCaregiverByPhone(phoneNumber, permissions)) {
                is AuthResult.Success -> {
                    Timber.i("✅ Caregiver added successfully")
                    _uiEvents.emit(CareUiEvent.ShowSuccess("درخواست با موفقیت ارسال شد"))
                    _showAddCaregiverDialog.value = false
                    loadAllData() // Reload to show the new caregiver
                }
                is AuthResult.Error -> {
                    Timber.e("❌ Failed to add caregiver: ${result.message}")
                    _uiEvents.emit(CareUiEvent.ShowError(result.message))
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Add caregiver by scanning QR code
     */
    fun onQrCodeScanned(qrData: String, permissions: CarePermissions) {
        viewModelScope.launch {
            // Close scanner first
            _showQrScanner.value = false

            val userId = careRepository.parseQrCodeData(qrData)

            if (userId == null) {
                _uiEvents.emit(CareUiEvent.ShowError("QR کد نامعتبر یا منقضی شده است"))
                _pendingQrPermissions.value = null
                return@launch
            }

            _isLoading.value = true

            when (val result = careRepository.addCaregiverByUserId(userId, permissions)) {
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

    /**
     * Accept a caregiver request (called by the caregiver)
     */
    fun onAcceptCaregiverRequest(careId: Int) {
        viewModelScope.launch {
            _isLoading.value = true

            when (val result = careRepository.acceptCaregiverRequest(careId)) {
                is AuthResult.Success -> {
                    Timber.i("✅ Caregiver request accepted")
                    _uiEvents.emit(CareUiEvent.ShowSuccess("درخواست پذیرفته شد"))
                    loadAllData() // Reload to update the status
                }
                is AuthResult.Error -> {
                    Timber.e("❌ Failed to accept request: ${result.message}")
                    _uiEvents.emit(CareUiEvent.ShowError(result.message))
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Edit caregiver permissions
     */
    fun onEditCaregiver(caregiver: CaregiverUiModel) {
        // TODO: Show edit dialog with current permissions
        viewModelScope.launch {
            Timber.i("Edit caregiver: ${caregiver.phoneNumber}")
            _uiEvents.emit(CareUiEvent.ShowSuccess("ویرایش به زودی اضافه می‌شود"))
        }
    }

    /**
     * Delete a caregiver from my caregivers list
     */
    fun onDeleteCaregiver(caregiver: CaregiverUiModel) {
        viewModelScope.launch {
            _isLoading.value = true

            when (val result = careRepository.deleteCaregiver(caregiver.id)) {
                is AuthResult.Success -> {
                    Timber.i("✅ Caregiver deleted")
                    _uiEvents.emit(CareUiEvent.ShowSuccess("تن‌یار حذف شد"))
                    loadAllData() // Reload the list
                }
                is AuthResult.Error -> {
                    Timber.e("❌ Failed to delete caregiver: ${result.message}")
                    _uiEvents.emit(CareUiEvent.ShowError(result.message))
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Remove myself as caregiver for someone
     */
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
}