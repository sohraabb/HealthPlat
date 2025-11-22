package com.bonyad.healthplat.ui.dashboard.care

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CareViewModel @Inject constructor(
    // TODO: Inject CareRepository when backend is ready
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(CareTab.MY_CAREGIVERS)
    val selectedTab: StateFlow<CareTab> = _selectedTab.asStateFlow()

    private val _myCaregivers = MutableStateFlow<List<Caregiver>>(emptyList())
    val myCaregivers: StateFlow<List<Caregiver>> = _myCaregivers.asStateFlow()

    private val _iAmCaregiverFor = MutableStateFlow<List<Caregiver>>(emptyList())
    val iAmCaregiverFor: StateFlow<List<Caregiver>> = _iAmCaregiverFor.asStateFlow()

    private val _showAddCaregiverDialog = MutableStateFlow(false)
    val showAddCaregiverDialog: StateFlow<Boolean> = _showAddCaregiverDialog.asStateFlow()

    init {
        loadMockData()
    }

    fun onTabSelected(tab: CareTab) {
        _selectedTab.value = tab
    }

    fun onAddCaregiverClick() {
        _showAddCaregiverDialog.value = true
    }

    fun onDismissAddCaregiverDialog() {
        _showAddCaregiverDialog.value = false
    }

    fun onAddCaregiver(phoneNumber: String, selectedDevices: Set<String>) {
        viewModelScope.launch {
            try {
                // TODO: Call API to add caregiver
                Timber.i("Adding caregiver: $phoneNumber with devices: $selectedDevices")

                // Mock: Add to local list
                val newCaregiver = Caregiver(
                    id = System.currentTimeMillis().toString(),
                    name = "سیاوش حسینی", // Mock name
                    phoneNumber = phoneNumber,
                    isPending = true
                )

                _myCaregivers.value = _myCaregivers.value + newCaregiver
                _showAddCaregiverDialog.value = false
            } catch (e: Exception) {
                Timber.e(e, "Failed to add caregiver")
            }
        }
    }

    fun onEditCaregiver(caregiver: Caregiver) {
        Timber.i("Edit caregiver: ${caregiver.name}")
        // TODO: Show edit dialog or navigate to edit screen
    }

    fun onDeleteCaregiver(caregiver: Caregiver) {
        viewModelScope.launch {
            try {
                // TODO: Call API to delete caregiver
                Timber.i("Deleting caregiver: ${caregiver.name}")

                _myCaregivers.value = _myCaregivers.value.filter { it.id != caregiver.id }
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete caregiver")
            }
        }
    }

    fun onRemoveCaregiverRole(person: Caregiver) {
        viewModelScope.launch {
            try {
                // TODO: Call API to remove caregiver role
                Timber.i("Removing caregiver role for: ${person.name}")

                _iAmCaregiverFor.value = _iAmCaregiverFor.value.filter { it.id != person.id }
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove caregiver role")
            }
        }
    }

    private fun loadMockData() {
        // Mock data for testing UI
        _myCaregivers.value = listOf(
            Caregiver(
                id = "1",
                name = "سیاوش حسینی",
                phoneNumber = "09123456789",
                isPending = true
            )
        )

        _iAmCaregiverFor.value = listOf(
            Caregiver(
                id = "2",
                name = "علی کمالی",
                phoneNumber = "09187654321",
                isPending = false
            )
        )
    }
}