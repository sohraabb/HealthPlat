package com.bonyad.healthplat.ui.dashboard.profile.alarm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.domain.model.AlarmType
import com.bonyad.healthplat.domain.model.AlarmUiState
import com.bonyad.healthplat.domain.model.HealthAlarm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    // TODO: Inject AlarmRepository when available
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlarmUiState>(AlarmUiState.Loading)
    val uiState: StateFlow<AlarmUiState> = _uiState.asStateFlow()

    private val _alarms = MutableStateFlow<List<HealthAlarm>>(emptyList())
    val alarms: StateFlow<List<HealthAlarm>> = _alarms.asStateFlow()

    // Bottom sheet states
    private val _showParameterSheet = MutableStateFlow(false)
    val showParameterSheet: StateFlow<Boolean> = _showParameterSheet.asStateFlow()

    private val _showThresholdSheet = MutableStateFlow(false)
    val showThresholdSheet: StateFlow<Boolean> = _showThresholdSheet.asStateFlow()

    // Selected parameters for new alarm
    private val _selectedTypes = MutableStateFlow<Set<AlarmType>>(emptySet())
    val selectedTypes: StateFlow<Set<AlarmType>> = _selectedTypes.asStateFlow()

    // Current alarm being edited/created
    private val _currentAlarmType = MutableStateFlow<AlarmType?>(null)
    val currentAlarmType: StateFlow<AlarmType?> = _currentAlarmType.asStateFlow()

    private val _minThreshold = MutableStateFlow("")
    val minThreshold: StateFlow<String> = _minThreshold.asStateFlow()

    private val _maxThreshold = MutableStateFlow("")
    val maxThreshold: StateFlow<String> = _maxThreshold.asStateFlow()

    private val _sendToCaregiver = MutableStateFlow(false)
    val sendToCaregiver: StateFlow<Boolean> = _sendToCaregiver.asStateFlow()

    // Edit mode
    private val _editingAlarmId = MutableStateFlow<String?>(null)
    val editingAlarmId: StateFlow<String?> = _editingAlarmId.asStateFlow()

    init {
        loadAlarms()
    }

    private fun loadAlarms() {
        viewModelScope.launch {
            _uiState.value = AlarmUiState.Loading
            try {
                // TODO: Load from repository
                // For now, using mock data or empty list
                val savedAlarms = emptyList<HealthAlarm>()
                _alarms.value = savedAlarms
                _uiState.value = if (savedAlarms.isEmpty()) {
                    AlarmUiState.Empty
                } else {
                    AlarmUiState.Success(savedAlarms)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load alarms")
                _uiState.value = AlarmUiState.Error("خطا در بارگذاری هشدارها")
            }
        }
    }

    // ==================== Bottom Sheet Actions ====================

    fun onAddAlarmClick() {
        _editingAlarmId.value = null
        _selectedTypes.value = emptySet()
        _showParameterSheet.value = true
    }

    fun onEditAlarmClick(alarm: HealthAlarm) {
        _editingAlarmId.value = alarm.id
        _currentAlarmType.value = alarm.type
        _minThreshold.value = alarm.minThreshold.toString()
        _maxThreshold.value = alarm.maxThreshold.toString()
        _sendToCaregiver.value = alarm.sendToCaregiver
        _showThresholdSheet.value = true
    }

    fun dismissParameterSheet() {
        _showParameterSheet.value = false
        _selectedTypes.value = emptySet()
    }

    fun dismissThresholdSheet() {
        _showThresholdSheet.value = false
        _currentAlarmType.value = null
        _minThreshold.value = ""
        _maxThreshold.value = ""
        _sendToCaregiver.value = false
        _editingAlarmId.value = null
    }

    // ==================== Parameter Selection ====================

    fun toggleAlarmType(type: AlarmType) {
        _selectedTypes.update { current ->
            if (current.contains(type)) {
                current - type
            } else {
                current + type
            }
        }
    }

    fun onParameterSelectionConfirm() {
        val selected = _selectedTypes.value
        if (selected.isEmpty()) return

        _showParameterSheet.value = false

        // Start with the first selected type
        val firstType = selected.first()
        _currentAlarmType.value = firstType
        _minThreshold.value = firstType.defaultMin.toString()
        _maxThreshold.value = firstType.defaultMax.toString()
        _sendToCaregiver.value = false
        _showThresholdSheet.value = true
    }

    // ==================== Threshold Settings ====================

    fun updateMinThreshold(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _minThreshold.value = value
        }
    }

    fun updateMaxThreshold(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            _maxThreshold.value = value
        }
    }

    fun toggleSendToCaregiver() {
        _sendToCaregiver.value = !_sendToCaregiver.value
    }

    fun onThresholdConfirm() {
        val type = _currentAlarmType.value ?: return
        val min = _minThreshold.value.toIntOrNull() ?: return
        val max = _maxThreshold.value.toIntOrNull() ?: return

        viewModelScope.launch {
            val editId = _editingAlarmId.value

            if (editId != null) {
                // Update existing alarm
                _alarms.update { current ->
                    current.map { alarm ->
                        if (alarm.id == editId) {
                            alarm.copy(
                                minThreshold = min,
                                maxThreshold = max,
                                sendToCaregiver = _sendToCaregiver.value
                            )
                        } else {
                            alarm
                        }
                    }
                }
                Timber.i("Alarm updated: $editId")
            } else {
                // Create new alarm
                val newAlarm = HealthAlarm(
                    type = type,
                    minThreshold = min,
                    maxThreshold = max,
                    sendToCaregiver = _sendToCaregiver.value
                )
                _alarms.update { current -> current + newAlarm }
                Timber.i("New alarm created: ${newAlarm.id}")
            }

            // Update UI state
            val updatedAlarms = _alarms.value
            _uiState.value = if (updatedAlarms.isEmpty()) {
                AlarmUiState.Empty
            } else {
                AlarmUiState.Success(updatedAlarms)
            }

            // Check if there are more types to configure
            val remainingTypes = _selectedTypes.value - type
            if (remainingTypes.isNotEmpty() && editId == null) {
                // Configure next type
                val nextType = remainingTypes.first()
                _selectedTypes.value = remainingTypes
                _currentAlarmType.value = nextType
                _minThreshold.value = nextType.defaultMin.toString()
                _maxThreshold.value = nextType.defaultMax.toString()
                _sendToCaregiver.value = false
            } else {
                // All done
                dismissThresholdSheet()
            }
        }
    }

    // ==================== Alarm Management ====================

    fun deleteAlarm(alarmId: String) {
        viewModelScope.launch {
            _alarms.update { current ->
                current.filter { it.id != alarmId }
            }

            val updatedAlarms = _alarms.value
            _uiState.value = if (updatedAlarms.isEmpty()) {
                AlarmUiState.Empty
            } else {
                AlarmUiState.Success(updatedAlarms)
            }

            Timber.i("Alarm deleted: $alarmId")
            // TODO: Delete from repository
        }
    }

    fun toggleAlarmEnabled(alarmId: String) {
        viewModelScope.launch {
            _alarms.update { current ->
                current.map { alarm ->
                    if (alarm.id == alarmId) {
                        alarm.copy(isEnabled = !alarm.isEnabled)
                    } else {
                        alarm
                    }
                }
            }

            _uiState.value = AlarmUiState.Success(_alarms.value)
            // TODO: Update in repository
        }
    }
}