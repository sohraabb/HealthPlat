package com.bonyad.healthplat.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.repository.AuthResult
import com.bonyad.healthplat.data.repository.UserRepository
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
    private val userRepository: UserRepository,
    private val userPreferences: UserPreferencesDataStore

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

    private val _nationalCode = MutableStateFlow("")
    val nationalCode = _nationalCode.asStateFlow()

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _disease = MutableStateFlow("ندارم")
    val disease = _disease.asStateFlow()


    val isEditFormValid: StateFlow<Boolean> = combine(
        _name, _birthDate, _height, _weight, _gender
    ) { name, birth, h, w, g ->
        name.isNotBlank() && birth.isNotBlank() &&
                h.isNotBlank() && w.isNotBlank() && g.isNotBlank()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun updateEmail(value: String) { _email.value = value }
    fun updateNationalCode(value: String) { _nationalCode.value = value }


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
        _birthDate.value = "${convertToPersianNumber(year)}/${
            convertToPersianNumber(month).padStart(
                2,
                '۰'
            )
        }/${convertToPersianNumber(day).padStart(2, '۰')}"
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
                // Convert Persian to English
                val heightInt = convertPersianToEnglish(_height.value).toInt()
                val weightInt = convertPersianToEnglish(_weight.value).toInt()

                // Convert Persian date to ISO format
                val birthDateIso = convertPersianDateToIso(_birthDate.value)

                // Convert gender to API format
                val genderInt = if (_gender.value == "مرد") 1 else 2

                // Call real API
                when (val result = userRepository.updateUserInfo(
                    name = _name.value,
                    birthDate = birthDateIso,
                    gender = genderInt,
                    height = heightInt,
                    weight = weightInt

                )) {
                    is AuthResult.Success -> {
                        Timber.i("Personal info saved successfully")
                        _uiState.value = PersonalInfoUiState.Success
                    }

                    is AuthResult.Error -> {
                        Timber.e("Failed to save personal info: ${result.message}")
                        _uiState.value = PersonalInfoUiState.Error(result.message)
                    }
                }

            } catch (e: Exception) {
                Timber.e(e, "Failed to save personal info")
                _uiState.value = PersonalInfoUiState.Error("خطا در ذخیره اطلاعات")
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

    /**
     * Convert Persian date (1379/09/05) to ISO format (2000-11-25T00:00:00Z)
     */
    private fun convertPersianDateToIso(persianDate: String): String {
        try {
            // Parse Persian date
            val dateEnglish = convertPersianToEnglish(persianDate)
            val parts = dateEnglish.split("/")

            if (parts.size != 3) {
                throw IllegalArgumentException("Invalid date format")
            }

            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()

            // Convert Jalali to Gregorian
            val (gYear, gMonth, gDay) = jalaliToGregorian(year, month, day)

            // Format as ISO
            return String.format("%04d-%02d-%02dT00:00:00Z", gYear, gMonth, gDay)
        } catch (e: Exception) {
            Timber.e(e, "Failed to convert date")
            return "2000-01-01T00:00:00Z" // Fallback
        }
    }

    /**
     * Convert Jalali (Persian) calendar to Gregorian
     */
    private fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        var gy: Int
        var gm: Int
        var gd: Int

        val jy2 = jy + 1595
        var days = 365 * jy2 + (jy2 / 33 * 8 + (jy2 % 33 + 3) / 4)

        if (jm < 7) {
            days += (jm - 1) * 31
        } else {
            days += (jm - 7) * 30 + 186
        }
        days += jd

        gy = 400 * (days / 146097)
        days %= 146097

        var leap = true
        if (days >= 36525) {
            days--
            gy += 100 * (days / 36524)
            days %= 36524
            if (days >= 365) {
                days++
            } else {
                leap = false
            }
        }

        gy += 4 * (days / 1461)
        days %= 1461

        if (days >= 366) {
            leap = false
            days--
            gy += days / 365
            days %= 365
        }

        val sal_a = intArrayOf(
            0, 31, if (leap && gy >= 0 || !leap && gy < 0) 29 else 28,
            31, 30, 31, 30, 31, 31, 30, 31, 30, 31
        )

        gm = 0
        while (gm < 13 && days >= sal_a[gm]) {
            days -= sal_a[gm]
            gm++
        }
        gd = days + 1

        return Triple(gy, gm, gd)
    }
}