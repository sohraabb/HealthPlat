package com.bonyad.healthplat.ui.dashboard.profile.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.domain.model.AddMedicationState
import com.bonyad.healthplat.domain.model.AddMedicationStep
import com.bonyad.healthplat.domain.model.DayOfWeek
import com.bonyad.healthplat.domain.model.Medication
import com.bonyad.healthplat.domain.model.MedicationFrequency
import com.bonyad.healthplat.domain.model.MedicationTime
import com.bonyad.healthplat.domain.model.MedicationUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MedicationViewModel @Inject constructor(
    // TODO: Inject MedicationRepository when available
) : ViewModel() {

    private val _uiState = MutableStateFlow<MedicationUiState>(MedicationUiState.Loading)
    val uiState: StateFlow<MedicationUiState> = _uiState.asStateFlow()

    private val _medications = MutableStateFlow<List<Medication>>(emptyList())
    val medications: StateFlow<List<Medication>> = _medications.asStateFlow()

    // Add medication bottom sheet states
    private val _showAddSheet = MutableStateFlow(false)
    val showAddSheet: StateFlow<Boolean> = _showAddSheet.asStateFlow()

    private val _addMedicationState = MutableStateFlow(AddMedicationState())
    val addMedicationState: StateFlow<AddMedicationState> = _addMedicationState.asStateFlow()

    // Edit mode
    private val _editingMedicationId = MutableStateFlow<String?>(null)
    val editingMedicationId: StateFlow<String?> = _editingMedicationId.asStateFlow()

    init {
        loadMedications()
    }

    private fun loadMedications() {
        viewModelScope.launch {
            _uiState.value = MedicationUiState.Loading
            try {
                delay(500) // Simulate network delay

                // TODO: Load from repository
                val savedMedications = emptyList<Medication>()
                _medications.value = savedMedications
                _uiState.value = if (savedMedications.isEmpty()) {
                    MedicationUiState.Empty
                } else {
                    MedicationUiState.Success(savedMedications)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load medications")
                _uiState.value = MedicationUiState.Error("خطا در بارگذاری داروها")
            }
        }
    }

    // ==================== Bottom Sheet Actions ====================

    fun onAddMedicationClick() {
        _editingMedicationId.value = null
        _addMedicationState.value = AddMedicationState()
        _showAddSheet.value = true
    }

    fun onEditMedicationClick(medication: Medication) {
        _editingMedicationId.value = medication.id
        _addMedicationState.value = AddMedicationState(
            currentStep = AddMedicationStep.DETAILS,
            title = medication.title,
            name = medication.name,
            duration = medication.duration,
            dosage = medication.dosage,
            frequency = medication.frequency,
            selectedDays = medication.selectedDays,
            times = medication.times
        )
        _showAddSheet.value = true
    }

    fun dismissAddSheet() {
        _showAddSheet.value = false
        _addMedicationState.value = AddMedicationState()
        _editingMedicationId.value = null
    }

    // ==================== Step 1: Details ====================

    fun updateTitle(value: String) {
        _addMedicationState.update { it.copy(title = value) }
    }

    fun updateName(value: String) {
        _addMedicationState.update { it.copy(name = value) }
    }

    fun updateDuration(value: String) {
        _addMedicationState.update { it.copy(duration = value) }
    }

    fun updateDosage(value: String) {
        _addMedicationState.update { it.copy(dosage = value) }
    }

    fun onDetailsNext() {
        if (_addMedicationState.value.isDetailsValid) {
            _addMedicationState.update { it.copy(currentStep = AddMedicationStep.FREQUENCY) }
        }
    }

    // ==================== Step 2: Frequency ====================

    fun selectFrequency(frequency: MedicationFrequency) {
        _addMedicationState.update {
            it.copy(frequency = frequency)
        }
    }

    fun onFrequencyNext() {
        val state = _addMedicationState.value
        if (state.isFrequencyAndDaysValid) {
            _addMedicationState.update { it.copy(currentStep = AddMedicationStep.TIME) }
        }
    }

    fun toggleDay(day: DayOfWeek) {
        _addMedicationState.update { state ->
            val newDays = if (state.selectedDays.contains(day)) {
                state.selectedDays - day
            } else {
                state.selectedDays + day
            }
            state.copy(selectedDays = newDays)
        }
    }

    // ==================== Step 3: Time ====================

    fun updateTimeHour(hour: Int) {
        _addMedicationState.update { it.copy(currentTimeHour = hour) }
    }

    fun updateTimeMinute(minute: Int) {
        _addMedicationState.update { it.copy(currentTimeMinute = minute) }
    }

    fun addCurrentTime() {
        _addMedicationState.update { state ->
            val newTime = MedicationTime(state.currentTimeHour, state.currentTimeMinute)
            // Avoid duplicates
            if (state.times.any { it.hour == newTime.hour && it.minute == newTime.minute }) {
                state
            } else {
                state.copy(times = state.times + newTime)
            }
        }
    }

    fun removeTime(time: MedicationTime) {
        _addMedicationState.update { state ->
            state.copy(times = state.times.filter { it != time })
        }
    }

    fun onTimeSave() {
        // Add current time if not already added
        val state = _addMedicationState.value
        if (state.times.isEmpty()) {
            addCurrentTime()
        }

        saveMedication()
    }

    // ==================== Navigation ====================

    fun goBack() {
        val currentStep = _addMedicationState.value.currentStep
        val previousStep = when (currentStep) {
            AddMedicationStep.DETAILS -> {
                dismissAddSheet()
                return
            }
            AddMedicationStep.FREQUENCY -> AddMedicationStep.DETAILS
            AddMedicationStep.TIME -> AddMedicationStep.FREQUENCY
        }
        _addMedicationState.update { it.copy(currentStep = previousStep) }
    }

    // ==================== Save Medication ====================

    private fun saveMedication() {
        viewModelScope.launch {
            val state = _addMedicationState.value
            val editId = _editingMedicationId.value

            // Make sure we have at least one time
            val times = if (state.times.isEmpty()) {
                listOf(MedicationTime(state.currentTimeHour, state.currentTimeMinute))
            } else {
                state.times
            }

            if (editId != null) {
                // Update existing medication
                _medications.update { current ->
                    current.map { medication ->
                        if (medication.id == editId) {
                            medication.copy(
                                title = state.title,
                                name = state.name,
                                duration = state.duration,
                                dosage = state.dosage,
                                frequency = state.frequency!!,
                                selectedDays = state.selectedDays,
                                times = times
                            )
                        } else {
                            medication
                        }
                    }
                }
                Timber.i("Medication updated: $editId")
            } else {
                // Create new medication
                val newMedication = Medication(
                    title = state.title,
                    name = state.name,
                    duration = state.duration,
                    dosage = state.dosage,
                    frequency = state.frequency!!,
                    selectedDays = state.selectedDays,
                    times = times
                )
                _medications.update { current -> current + newMedication }
                Timber.i("New medication created: ${newMedication.id}")
            }

            // Update UI state
            val updatedMedications = _medications.value
            _uiState.value = if (updatedMedications.isEmpty()) {
                MedicationUiState.Empty
            } else {
                MedicationUiState.Success(updatedMedications)
            }

            dismissAddSheet()
        }
    }

    // ==================== Medication Management ====================

    fun deleteMedication(medicationId: String) {
        viewModelScope.launch {
            _medications.update { current ->
                current.filter { it.id != medicationId }
            }

            val updatedMedications = _medications.value
            _uiState.value = if (updatedMedications.isEmpty()) {
                MedicationUiState.Empty
            } else {
                MedicationUiState.Success(updatedMedications)
            }

            Timber.i("Medication deleted: $medicationId")
        }
    }

    fun toggleMedicationEnabled(medicationId: String) {
        viewModelScope.launch {
            _medications.update { current ->
                current.map { medication ->
                    if (medication.id == medicationId) {
                        medication.copy(isEnabled = !medication.isEnabled)
                    } else {
                        medication
                    }
                }
            }

            _uiState.value = MedicationUiState.Success(_medications.value)
        }
    }

    fun refreshMedications() {
        loadMedications()
    }
}