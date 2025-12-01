package com.yourpackage.healthplat.ui.auth

import androidx.compose.ui.autofill.ContentType.Companion.Gender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.data.repository.AuthRepository
import com.bonyad.healthplat.data.repository.AuthResult
import com.bonyad.healthplat.domain.model.AuthState
import com.bonyad.healthplat.domain.model.SendOtpResponse
import com.bonyad.healthplat.domain.model.VerifyOtpResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _otp = MutableStateFlow("")
    val otp: StateFlow<String> = _otp.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _isNewUser = MutableStateFlow<Boolean?>(null)
    val isNewUser: StateFlow<Boolean?> = _isNewUser.asStateFlow()

    private val _resendTimer = MutableStateFlow(0)
    val resendTimer: StateFlow<Int> = _resendTimer.asStateFlow()


    // Debugging reason
    private val _serverOtp = MutableStateFlow("")
    val serverOtp: StateFlow<String> = _serverOtp.asStateFlow()

    private var timerJob: Job? = null

    fun setPhoneNumber(phone: String) {
        _phoneNumber.value = phone
    }

    fun updatePhoneNumber(phone: String) {
        val cleaned = phone.filter { it.isDigit() || isPersianDigit(it) }
        if (cleaned.length <= 11) _phoneNumber.value = cleaned
    }

    fun updateOtp(code: String) {
        val cleaned = code.filter { it.isDigit() || isPersianDigit(it) }
        if (cleaned.length <= 5) _otp.value = cleaned
    }

    fun sendOtp() {
        val phone = convertPersianToEnglish(_phoneNumber.value)

        if (phone.length != 11) {
            _authState.value = AuthState.Error("شماره تلفن باید ۱۱ رقم باشد")
            return
        }

        if (!phone.startsWith("09")) {
            _authState.value = AuthState.Error("شماره تلفن باید با ۰۹ شروع شود")
            return
        }

        // Check with real api
        viewModelScope.launch {
            _authState.value = AuthState.Loading


            when (val result = authRepository.requestPhoneVerification(phone)) {
                is AuthResult.Success -> {
                    val data = result.data
//                    Store userId (null for new users, has value for existing)
                    _isNewUser.value = data.userId == null
                    _userId.value = data.userId

                    _authState.value = AuthState.PhoneSubmitted
                    Timber.i("OTP sent to $phone - userId: ${data.userId ?: "NEW USER"}")

                    data.code.let { code ->
                        _serverOtp.value = code
                    }

                    // In dev/test mode, the code is returned: data.code
                    Timber.d("OTP Code (dev mode): ${data.code}")
                    startResendTimer()
                }

                is AuthResult.Error -> {
                    _authState.value = AuthState.Error(result.message)
                    Timber.w("Failed to send OTP: ${result.message}")
                }
            }


            /*
            try {
                // Mock API call - replace with real API later
                delay(1500)
                val response = mockSendOtp(phone)

                if (response.success) {
                    _authState.value = AuthState.PhoneSubmitted
                    Timber.i("OTP sent successfully to $phone")
                    startResendTimer()
                } else {
                    _authState.value = AuthState.Error(response.message)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to send OTP")
                _authState.value = AuthState.Error("خطا در ارسال کد. لطفا دوباره تلاش کنید")
            }

             */
        }
    }

    fun verifyOtp() {
        val code = convertPersianToEnglish(_otp.value)

        if (code.length != 5) {
            _authState.value = AuthState.Error("کد باید ۵ رقم باشد")
            return
        }

        val phone = convertPersianToEnglish(_phoneNumber.value)
//        val isExistingUser = _userId.value != null


        val userIsNew = _isNewUser.value ?: true

        viewModelScope.launch {
            _authState.value = AuthState.Loading

            if (!userIsNew) {
                // EXISTING USER: Login immediately
                when (val result = authRepository.loginByPhone(phone, code)) {
                    is AuthResult.Success -> {
                        val loginData = result.data
                        _userId.value = loginData.userId
                        _authState.value = AuthState.OtpVerified
                        Timber.i("Existing user logged in: ${loginData.userId}")
                    }

                    is AuthResult.Error -> {
                        _authState.value = AuthState.Error(result.message)
                        Timber.w("Failed to login: ${result.message}")
                    }
                }
            } else {
                // NEW USER: Register and get tokens
                when (val result = authRepository.registerByPhone(phone, code)) {
                    is AuthResult.Success -> {
                        val registerData = result.data
                        _userId.value = registerData.userId
                        _authState.value = AuthState.OtpVerified
                        Timber.i("New user registered: ${registerData.userId}")
                    }

                    is AuthResult.Error -> {
                        _authState.value = AuthState.Error(result.message)
                        Timber.w("Failed to register: ${result.message}")
                    }
                }
            }


            /*
            try {
                // Mock API call - replace with real API later
                delay(1500)
                val phone = convertPersianToEnglish(_phoneNumber.value)
                val response = mockVerifyOtp(phone, code)

                if (response.success) {
                    _userId.value = response.userId.toString()
                    _authState.value = AuthState.OtpVerified
                    Timber.i("OTP verified successfully, userId: ${response.userId}")
                } else {
                    _authState.value = AuthState.Error(response.message)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to verify OTP")
                _authState.value = AuthState.Error("خطا در تایید کد. لطفا دوباره تلاش کنید")
            }

             */
        }


    }

    fun resendOtp() {
        if (_resendTimer.value > 0) return

        viewModelScope.launch {
            _otp.value = ""
            _authState.value = AuthState.Idle
            sendOtp()
        }
    }

    private fun startResendTimer() {
        timerJob?.cancel()
        _resendTimer.value = 60

        timerJob = viewModelScope.launch {
            while (_resendTimer.value > 0) {
                delay(1000)
                _resendTimer.value -= 1
            }
        }
    }

    fun resetError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.Idle
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    // Mock API functions - replace with real API calls later
    private fun mockSendOtp(phoneNumber: String): SendOtpResponse {
        // Simulate success for any valid phone number
        Timber.d("Mock: Sending OTP to $phoneNumber")
        // In production, this would be: authRepository.sendOtp(phoneNumber)
        return SendOtpResponse(success = true, message = "کد با موفقیت ارسال شد")
    }

    private fun mockVerifyOtp(phoneNumber: String, otp: String): VerifyOtpResponse {
        // For testing: accept any 5-digit OTP
        // In production, you might want to accept specific test codes like "12345"
        Timber.d("Mock: Verifying OTP $otp for $phoneNumber")

        val userExists = false


        // Simulate successful verification
        // In production: authRepository.verifyOtp(phoneNumber, otp)

        return VerifyOtpResponse(
            success = true,
            userId = System.currentTimeMillis(),
            token = "mock_token_${System.currentTimeMillis()}",
            message = "کد با موفقیت تایید شد",
            isNewUser = !userExists  // Add this field
        )

        // To simulate error for testing:
        // return VerifyOtpResponse(success = false, message = "کد وارد شده اشتباه است")
    }

    private fun isPersianDigit(char: Char): Boolean {
        return char in '۰'..'۹'
    }

    fun convertPersianToEnglish(text: String): String {
        val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
        val englishDigits = "0123456789"

        return text.map { char ->
            val index = persianDigits.indexOf(char)
            if (index != -1) englishDigits[index] else char
        }.joinToString("")
    }

    fun getFormattedPhoneNumber(): String {
        val phone = _phoneNumber.value
        return if (phone.length == 11)
            "${phone.substring(0, 4)} ${phone.substring(4, 7)} ${phone.substring(7)}"
        else phone
    }

    fun shouldShowDeviceConnection(): Boolean {
        return _isNewUser.value == true
    }

}