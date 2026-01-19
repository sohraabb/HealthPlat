package com.bonyad.healthplat.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.repository.AuthResult
import com.bonyad.healthplat.data.repository.UserRepository
import com.bonyad.healthplat.domain.model.DiseaseData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

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

    // ============ Required Fields ============
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _lastName = MutableStateFlow("")
    val lastName: StateFlow<String> = _lastName.asStateFlow()

    private val _birthDate = MutableStateFlow("")
    val birthDate: StateFlow<String> = _birthDate.asStateFlow()

    private val _height = MutableStateFlow("")
    val height: StateFlow<String> = _height.asStateFlow()

    private val _weight = MutableStateFlow("")
    val weight: StateFlow<String> = _weight.asStateFlow()

    private val _gender = MutableStateFlow("")
    val gender: StateFlow<String> = _gender.asStateFlow()

    // ============ Optional Fields (for Edit screen) ============
    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _nationalCode = MutableStateFlow("")
    val nationalCode: StateFlow<String> = _nationalCode.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    // ============ Disease Multi-Selection ============
    private val _availableDiseases = MutableStateFlow<List<DiseaseData>>(emptyList())
    val availableDiseases: StateFlow<List<DiseaseData>> = _availableDiseases.asStateFlow()

    private val _selectedDiseaseIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedDiseaseIds: StateFlow<Set<Int>> = _selectedDiseaseIds.asStateFlow()

    // For display in the field
    val selectedDiseasesText: StateFlow<String> = combine(
        _availableDiseases,
        _selectedDiseaseIds
    ) { diseases, selectedIds ->
        if (selectedIds.isEmpty()) {
            "ندارم"
        } else {
            diseases
                .filter { it.id in selectedIds }
                .joinToString("، ") { it.name }
                .ifEmpty { "ندارم" }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "ندارم"
    )

    // ============ Phone Change OTP ============
    private val _newPhoneNumber = MutableStateFlow("")
    val newPhoneNumber: StateFlow<String> = _newPhoneNumber.asStateFlow()

    private val _phoneOtp = MutableStateFlow("")
    val phoneOtp: StateFlow<String> = _phoneOtp.asStateFlow()

    private val _showPhoneChangeSheet = MutableStateFlow(false)
    val showPhoneChangeSheet: StateFlow<Boolean> = _showPhoneChangeSheet.asStateFlow()

    private val _showPhoneOtpSheet = MutableStateFlow(false)
    val showPhoneOtpSheet: StateFlow<Boolean> = _showPhoneOtpSheet.asStateFlow()

    private val _phoneChangeState = MutableStateFlow<PersonalInfoUiState>(PersonalInfoUiState.Idle)
    val phoneChangeState: StateFlow<PersonalInfoUiState> = _phoneChangeState.asStateFlow()

    // ============ UI State ============
    private val _uiState = MutableStateFlow<PersonalInfoUiState>(PersonalInfoUiState.Idle)
    val uiState: StateFlow<PersonalInfoUiState> = _uiState.asStateFlow()

    // ============ Picker States ============
    private val _showDatePicker = MutableStateFlow(false)
    val showDatePicker: StateFlow<Boolean> = _showDatePicker.asStateFlow()

    private val _showGenderPicker = MutableStateFlow(false)
    val showGenderPicker: StateFlow<Boolean> = _showGenderPicker.asStateFlow()

    private val _showDiseasePicker = MutableStateFlow(false)
    val showDiseasePicker: StateFlow<Boolean> = _showDiseasePicker.asStateFlow()

    // ============ Track if data was loaded ============
    private val _isDataLoaded = MutableStateFlow(false)
    val isDataLoaded: StateFlow<Boolean> = _isDataLoaded.asStateFlow()

    // ============ Form Validation ============
    val isFormValid: StateFlow<Boolean> = combine(
        _name,
        _lastName,
        _birthDate,
        _height,
        _weight,
        _gender
    ) { values ->
        val name = values[0] as String
        val lastName = values[1] as String
        val birthDate = values[2] as String
        val height = values[3] as String
        val weight = values[4] as String
        val gender = values[5] as String

        name.isNotBlank() &&
                lastName.isNotBlank() &&
                birthDate.isNotBlank() &&
                height.isNotBlank() && convertPersianToEnglish(height).toIntOrNull() != null &&
                weight.isNotBlank() && convertPersianToEnglish(weight).toIntOrNull() != null &&
                gender.isNotBlank()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    // ============ Load Existing Data (for Edit screen) ============

    /**
     * Load existing user data from local preferences
     * Call this when entering Edit screen
     */
    fun loadExistingData() {
        if (_isDataLoaded.value) return // Prevent reloading

        viewModelScope.launch {
            try {
                Timber.d("Loading existing user data from preferences...")

                userPreferences.getUserName().first()?.let { savedName ->
                    _name.value = savedName
                }

                userPreferences.getUserLastName().first()?.let { savedLastName ->
                    _lastName.value = savedLastName
                }

                userPreferences.getUserBirthDate().first()?.let { savedBirthDate ->
                    _birthDate.value = convertIsoToPersianDate(savedBirthDate)
                }

                userPreferences.getUserHeight().first()?.let { savedHeight ->
                    _height.value = convertToPersianNumber(savedHeight)
                }

                userPreferences.getUserWeight().first()?.let { savedWeight ->
                    _weight.value = convertToPersianNumber(savedWeight)
                }

                userPreferences.getUserGender().first()?.let { savedGender ->
                    _gender.value = savedGender
                    Timber.d("Loaded gender from preferences: $savedGender")
                }

                userPreferences.getPhoneNumber().first()?.let { savedPhone ->
                    _phoneNumber.value = savedPhone
                }

                userPreferences.getUserNationalCode().first()?.let { savedNationalCode ->
                    _nationalCode.value = savedNationalCode
                }

                userPreferences.getUserEmail().first()?.let { savedEmail ->
                    _email.value = savedEmail
                }

                // Load disease IDs
                val savedDiseaseIds = userPreferences.getUserDiseaseIds().first()
                _selectedDiseaseIds.value = savedDiseaseIds.toSet()
                Timber.d("Loaded disease IDs: $savedDiseaseIds")

                // Load available diseases from API
                loadDiseases()

                _isDataLoaded.value = true
                Timber.d("Existing user data loaded successfully")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load existing user data")
            }
        }
    }

    /**
     * Load available diseases from API
     */
    fun loadDiseases() {
        viewModelScope.launch {
            when (val result = userRepository.getDiseases()) {
                is AuthResult.Success -> {
                    _availableDiseases.value = result.data
                    Timber.i("Loaded ${result.data.size} diseases")
                }
                is AuthResult.Error -> {
                    Timber.e("Failed to load diseases: ${result.message}")
                }
            }
        }
    }

    /**
     * Refresh data from API (for Edit screen refresh button)
     */
    fun refreshDataFromApi() {
        viewModelScope.launch {
            _uiState.value = PersonalInfoUiState.Loading

            when (val result = userRepository.getUserOverview()) {
                is AuthResult.Success -> {
                    val overview = result.data

                    overview.name?.let { _name.value = it }
                    overview.lastName?.let { _lastName.value = it }
                    overview.birthDate?.let { _birthDate.value = convertIsoToPersianDate(it) }
                    overview.height?.let { _height.value = convertToPersianNumber(it) }
                    overview.weight?.let { _weight.value = convertToPersianNumber(it) }
                    overview.gender?.let { _gender.value = if (it == 1) "مرد" else "زن" }
                    _phoneNumber.value = overview.phoneNumber
                    overview.nationalCode?.let { _nationalCode.value = it }
                    overview.email?.let { _email.value = it }
                    overview.diseaseIds?.let { _selectedDiseaseIds.value = it.toSet() }

                    _uiState.value = PersonalInfoUiState.Idle
                    Timber.i("User data refreshed from API")
                }

                is AuthResult.Error -> {
                    _uiState.value = PersonalInfoUiState.Error(result.message)
                    Timber.e("Failed to refresh: ${result.message}")
                }
            }
        }
    }

    // ============ Update Functions ============

    fun updateName(value: String) {
        if (value.all { it.isLetter() || it.isWhitespace() || isPersianCharacter(it) }) {
            _name.value = value
        }
    }

    fun updateLastName(value: String) {
        if (value.all { it.isLetter() || it.isWhitespace() || isPersianCharacter(it) }) {
            _lastName.value = value
        }
    }

    fun updateHeight(value: String) {
        val cleaned = value.filter { it.isDigit() || isPersianDigit(it) }
        if (cleaned.length <= 3) {
            _height.value = cleaned
        }
    }

    fun updateWeight(value: String) {
        val cleaned = value.filter { it.isDigit() || isPersianDigit(it) }
        if (cleaned.length <= 3) {
            _weight.value = cleaned
        }
    }

    fun updateNationalCode(value: String) {
        val cleaned = value.filter { it.isDigit() || isPersianDigit(it) }
        if (cleaned.length <= 10) {
            _nationalCode.value = cleaned
        }
    }

    fun updateEmail(value: String) {
        _email.value = value
    }

    // ============ Date Picker ============

    fun onDatePickerClick() {
        _showDatePicker.value = true
    }

    fun onDatePickerDismiss() {
        _showDatePicker.value = false
    }

    fun onDateSelected(year: Int, month: Int, day: Int) {
        _birthDate.value = "${convertToPersianNumber(year)}/${
            convertToPersianNumber(month).padStart(2, '۰')
        }/${convertToPersianNumber(day).padStart(2, '۰')}"
    }

    /**
     * Get the currently selected date parts for initializing the date picker
     */
    fun getSelectedDateParts(): Triple<Int, Int, Int> {
        return try {
            val dateEnglish = convertPersianToEnglish(_birthDate.value)
            val parts = dateEnglish.split("/")
            if (parts.size == 3) {
                Triple(
                    parts[0].toInt(),
                    parts[1].toInt(),
                    parts[2].toInt()
                )
            } else {
                Triple(1370, 1, 1)
            }
        } catch (e: Exception) {
            Triple(1370, 1, 1)
        }
    }

    // ============ Gender Picker ============

    fun onGenderPickerClick() {
        _showGenderPicker.value = true
    }

    fun onGenderPickerDismiss() {
        _showGenderPicker.value = false
    }

    fun onGenderSelected(selectedGender: String) {
        _gender.value = selectedGender
        Timber.d("Gender selected: $selectedGender")
    }

    // ============ Disease Picker (Multi-Select) ============

    fun onDiseasePickerClick() {
        _showDiseasePicker.value = true
    }

    fun onDiseasePickerDismiss() {
        _showDiseasePicker.value = false
    }

    fun toggleDiseaseSelection(diseaseId: Int) {
        val current = _selectedDiseaseIds.value.toMutableSet()
        if (current.contains(diseaseId)) {
            current.remove(diseaseId)
        } else {
            current.add(diseaseId)
        }
        _selectedDiseaseIds.value = current
        Timber.d("Disease selection toggled: $diseaseId, current: $_selectedDiseaseIds")
    }

    fun clearDiseaseSelection() {
        _selectedDiseaseIds.value = emptySet()
    }

    // ============ Phone Change ============

    fun onPhoneChangeClick() {
        _showPhoneChangeSheet.value = true
    }

    fun onPhoneChangeSheetDismiss() {
        _showPhoneChangeSheet.value = false
        _newPhoneNumber.value = ""
    }

    fun updateNewPhoneNumber(value: String) {
        val cleaned = value.filter { it.isDigit() || isPersianDigit(it) }
        if (cleaned.length <= 11) {
            _newPhoneNumber.value = cleaned
        }
    }

    fun requestPhoneChangeOtp() {
        val phone = convertPersianToEnglish(_newPhoneNumber.value)
        if (phone.length != 11) {
            _phoneChangeState.value = PersonalInfoUiState.Error("شماره تلفن باید ۱۱ رقم باشد")
            return
        }

        if (!phone.startsWith("09")) {
            _phoneChangeState.value = PersonalInfoUiState.Error("شماره تلفن باید با ۰۹ شروع شود")
            return
        }

        viewModelScope.launch {
            _phoneChangeState.value = PersonalInfoUiState.Loading
            // TODO: Call API to request phone change OTP
            // For now, simulate success
            kotlinx.coroutines.delay(1000)
            _showPhoneChangeSheet.value = false
            _showPhoneOtpSheet.value = true
            _phoneChangeState.value = PersonalInfoUiState.Idle
        }
    }

    fun onPhoneOtpSheetDismiss() {
        _showPhoneOtpSheet.value = false
        _phoneOtp.value = ""
    }

    fun updatePhoneOtp(value: String) {
        val cleaned = value.filter { it.isDigit() || isPersianDigit(it) }
        if (cleaned.length <= 5) {
            _phoneOtp.value = cleaned
        }
    }

    fun verifyPhoneChangeOtp() {
        val otp = convertPersianToEnglish(_phoneOtp.value)
        if (otp.length != 5) {
            _phoneChangeState.value = PersonalInfoUiState.Error("کد باید ۵ رقم باشد")
            return
        }

        viewModelScope.launch {
            _phoneChangeState.value = PersonalInfoUiState.Loading
            // TODO: Call API to verify OTP and change phone
            // For now, simulate success
            kotlinx.coroutines.delay(1000)
            _phoneNumber.value = _newPhoneNumber.value
            _showPhoneOtpSheet.value = false
            _newPhoneNumber.value = ""
            _phoneOtp.value = ""
            _phoneChangeState.value = PersonalInfoUiState.Idle
        }
    }

    fun getFormattedNewPhoneNumber(): String {
        val phone = convertPersianToEnglish(_newPhoneNumber.value)
        return if (phone.length == 11) {
            "${phone.substring(0, 4)} ${phone.substring(4, 7)} ${phone.substring(7)}"
        } else {
            phone
        }
    }

    // ============ Save ============

    fun savePersonalInfo() {
        if (!isFormValid.value) return

        viewModelScope.launch {
            _uiState.value = PersonalInfoUiState.Loading

            try {
                val heightInt = convertPersianToEnglish(_height.value).toInt()
                val weightInt = convertPersianToEnglish(_weight.value).toInt()
                val birthDateIso = convertPersianDateToIso(_birthDate.value)
                val genderInt = if (_gender.value == "مرد") 1 else 2

                // Log for debugging
                Timber.d("Saving: Name=${_name.value}, LastName=${_lastName.value}")
                Timber.d("Saving birthDate: Persian=${_birthDate.value} -> ISO=$birthDateIso")
                Timber.d("Saving diseases: ${_selectedDiseaseIds.value}")

                when (val result = userRepository.updateUserInfo(
                    name = _name.value,
                    lastName = _lastName.value,
                    birthDate = birthDateIso,
                    gender = genderInt,
                    height = heightInt,
                    weight = weightInt,
                    nationalCode = _nationalCode.value.ifEmpty { null },
                    email = _email.value.ifEmpty { null },
                    diseaseIds = _selectedDiseaseIds.value.toList().ifEmpty { null }
                )) {
                    is AuthResult.Success -> {
                        Timber.i("Personal info saved successfully")
                        _uiState.value = PersonalInfoUiState.Success
                    }

                    is AuthResult.Error -> {
                        Timber.e("Failed to save: ${result.message}")
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
        if (_phoneChangeState.value is PersonalInfoUiState.Error) {
            _phoneChangeState.value = PersonalInfoUiState.Idle
        }
    }

    /**
     * Reset state when navigating away (for reuse)
     */
    fun resetState() {
        _uiState.value = PersonalInfoUiState.Idle
    }

    // ============ Helper Functions ============

    private fun isPersianDigit(char: Char): Boolean = char in '۰'..'۹'

    private fun isPersianCharacter(char: Char): Boolean =
        char in '\u0600'..'\u06FF' || char in '\uFB8A'..'\uFDFD'

    private fun convertPersianToEnglish(text: String): String {
        val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
        val englishDigits = "0123456789"
        return text.map { char ->
            val index = persianDigits.indexOf(char)
            if (index != -1) englishDigits[index] else char
        }.joinToString("")
    }

    private fun convertToPersianNumber(number: Int): String {
        val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
        return number.toString().map { char ->
            if (char.isDigit()) persianDigits[char.toString().toInt()] else char
        }.joinToString("")
    }

    private fun convertIsoToPersianDate(isoDate: String): String {
        try {
            val datePart = isoDate.substringBefore("T")
            val parts = datePart.split("-")

            if (parts.size != 3) return ""

            val gYear = parts[0].toInt()
            val gMonth = parts[1].toInt()
            val gDay = parts[2].toInt()

            val (jYear, jMonth, jDay) = gregorianToJalali(gYear, gMonth, gDay)

            return "${convertToPersianNumber(jYear)}/${
                convertToPersianNumber(jMonth).padStart(2, '۰')
            }/${convertToPersianNumber(jDay).padStart(2, '۰')}"
        } catch (e: Exception) {
            Timber.e(e, "Failed to convert ISO date to Persian")
            return ""
        }
    }

    private fun convertPersianDateToIso(persianDate: String): String {
        try {
            val dateEnglish = convertPersianToEnglish(persianDate)
            val parts = dateEnglish.split("/")

            if (parts.size != 3) throw IllegalArgumentException("Invalid date format")

            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()

            val (gYear, gMonth, gDay) = jalaliToGregorian(year, month, day)

            // Log for debugging
            Timber.d("Date conversion: Jalali($year/$month/$day) -> Gregorian($gYear/$gMonth/$gDay)")

            return String.format("%04d-%02d-%02dT00:00:00Z", gYear, gMonth, gDay)
        } catch (e: Exception) {
            Timber.e(e, "Failed to convert date")
            return "2000-01-01T00:00:00Z"
        }
    }

    /**
     * Converts Gregorian date to Jalali (Persian) date
     */
    private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val g_d_m = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) -
                ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + g_d_m[gm - 1]

        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461

        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }

        val jm = if (days < 186) 1 + (days / 31) else 7 + ((days - 186) / 30)
        val jd = 1 + if (days < 186) days % 31 else (days - 186) % 30

        return Triple(jy, jm, jd)
    }

    /**
     * Converts Jalali (Persian) date to Gregorian date
     */
    private fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        // Calculate Jalali day number from epoch
        val jy1 = jy - 979
        val jm1 = jm - 1

        // Days from Jalali epoch
        var jDayNo = 365 * jy1 + (jy1 / 33) * 8 + ((jy1 % 33 + 3) / 4)

        // Add days for complete months
        // First 6 months have 31 days, next 5 months have 30 days
        for (i in 0 until jm1) {
            jDayNo += if (i < 6) 31 else 30
        }
        jDayNo += jd - 1

        // Convert to Gregorian day number
        // 79 is the difference between Jalali and Gregorian epochs (in days from year 1600)
        val gDayNo = jDayNo + 79

        // Calculate Gregorian year
        var gy = 1600 + 400 * (gDayNo / 146097)
        var d = gDayNo % 146097

        var leap = true
        if (d >= 36525) {
            d--
            gy += 100 * (d / 36524)
            d %= 36524
            if (d >= 365) {
                d++
            } else {
                leap = false
            }
        }

        gy += 4 * (d / 1461)
        d %= 1461

        if (d >= 366) {
            leap = false
            d--
            gy += d / 365
            d %= 365
        }

        // Calculate month and day
        val gDm = intArrayOf(
            31,
            if (leap) 29 else 28,
            31, 30, 31, 30, 31, 31, 30, 31, 30, 31
        )

        var gm = 0
        while (gm < 12 && d >= gDm[gm]) {
            d -= gDm[gm]
            gm++
        }

        return Triple(gy, gm + 1, d + 1)
    }
}