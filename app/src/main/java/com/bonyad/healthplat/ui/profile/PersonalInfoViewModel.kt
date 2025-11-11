package com.bonyad.healthplat.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

sealed class PersonalInfoUiState {
    object Idle : PersonalInfoUiState()
    object Loading : PersonalInfoUiState()
    object Success : PersonalInfoUiState()
    data class Error(val message: String) : PersonalInfoUiState()
}

@HiltViewModel
class PersonalInfoViewModel @Inject constructor(
    private val userPreferences: UserPreferencesDataStore
    // TODO: Inject UserRepository when backend is ready
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _birthDate = MutableStateFlow("")
    val birthDate: StateFlow<String> = _birthDate.asStateFlow()

    private val _height = MutableStateFlow("")
    val height: StateFlow<String> = _height.asStateFlow()

    private val _weight = MutableStateFlow("")
    val weight: StateFlow<String> = _weight.asStateFlow()

    private val _gender = MutableStateFlow("")
    val gender: StateFlow<String> = _gender.asStateFlow()

    private val _uiState = MutableStateFlow<PersonalInfoUiState>(PersonalInfoUiState.Idle)
    val uiState: StateFlow<PersonalInfoUiState> = _uiState.asStateFlow()

    private val _showDatePicker = MutableStateFlow(false)
    val showDatePicker: StateFlow<Boolean> = _showDatePicker.asStateFlow()

    private val _showGenderPicker = MutableStateFlow(false)
    val showGenderPicker: StateFlow<Boolean> = _showGenderPicker.asStateFlow()

    // Form validation
    val isFormValid: StateFlow<Boolean> = combine(
        _name,
        _birthDate,
        _height,
        _weight,
        _gender
    ) { name, birthDate, height, weight, gender ->
        name.isNotBlank() &&
                birthDate.isNotBlank() &&
                height.isNotBlank() && height.toIntOrNull() != null &&
                weight.isNotBlank() && weight.toIntOrNull() != null &&
                gender.isNotBlank()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun updateName(value: String) {
        // Allow Persian and English letters, spaces
        if (value.all { it.isLetter() || it.isWhitespace() || isPersianCharacter(it) }) {
            _name.value = value
        }
    }

    fun updateBirthDate(value: String) {
        _birthDate.value = value
    }

    fun updateHeight(value: String) {
        // Only allow Persian and English digits
        val cleaned = value.filter { it.isDigit() || isPersianDigit(it) }
        if (cleaned.length <= 3) { // Max 3 digits for height
            _height.value = cleaned
        }
    }

    fun updateWeight(value: String) {
        // Only allow Persian and English digits
        val cleaned = value.filter { it.isDigit() || isPersianDigit(it) }
        if (cleaned.length <= 3) { // Max 3 digits for weight
            _weight.value = cleaned
        }
    }

    fun onDatePickerClick() {
        _showDatePicker.value = true
    }

    fun onDatePickerDismiss() {
        _showDatePicker.value = false
    }

    fun onDateSelected(year: Int, month: Int, day: Int) {
        _birthDate.value = "${convertToPersianNumber(year)}/${convertToPersianNumber(month).padStart(2, '۰')}/${convertToPersianNumber(day).padStart(2, '۰')}"
    }

    fun onGenderPickerClick() {
        _showGenderPicker.value = true
    }

    fun onGenderPickerDismiss() {
        _showGenderPicker.value = false
    }

    fun onGenderSelected(selectedGender: String) {
        _gender.value = selectedGender
    }

    fun savePersonalInfo() {
        if (!isFormValid.value) return

        viewModelScope.launch {
            _uiState.value = PersonalInfoUiState.Loading

            try {
                // Convert Persian digits to English
                val heightInt = convertPersianToEnglish(_height.value).toInt()
                val weightInt = convertPersianToEnglish(_weight.value).toInt()

                // Mock API call - replace with real API later
                delay(1500)

                // Save to local preferences
                userPreferences.savePersonalInfo(
                    name = _name.value,
                    birthDate = _birthDate.value,
                    height = heightInt,
                    weight = weightInt,
                    gender = _gender.value
                )

                Timber.i("Personal info saved: ${_name.value}, $heightInt cm, $weightInt kg")
                _uiState.value = PersonalInfoUiState.Success

            } catch (e: Exception) {
                Timber.e(e, "Failed to save personal info")
                _uiState.value = PersonalInfoUiState.Error("خطا در ذخیره اطلاعات. لطفا دوباره تلاش کنید")
            }
        }
    }

    fun resetError() {
        if (_uiState.value is PersonalInfoUiState.Error) {
            _uiState.value = PersonalInfoUiState.Idle
        }
    }

    private fun isPersianDigit(char: Char): Boolean {
        return char in '۰'..'۹'
    }

    private fun isPersianCharacter(char: Char): Boolean {
        return char in '\u0600'..'\u06FF' || char in '\uFB8A'..'\uFDFD'
    }

    private fun convertPersianToEnglish(text: String): String {
        val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
        val englishDigits = "0123456789"

        return text.map { char ->
            val index = persianDigits.indexOf(char)
            if (index != -1) englishDigits[index] else char
        }.joinToString("")
    }
}