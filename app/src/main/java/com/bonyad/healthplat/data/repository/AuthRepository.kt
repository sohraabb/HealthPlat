package com.bonyad.healthplat.data.repository

import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.HealthPlatApiService
import com.bonyad.healthplat.domain.model.LoginByPhoneRequest
import com.bonyad.healthplat.domain.model.LoginResponse
import com.bonyad.healthplat.domain.model.RequestPhoneVerificationRequest
import com.bonyad.healthplat.domain.model.SendOtpRequest
import com.bonyad.healthplat.domain.model.VerifyOtpRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton


sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String) : AuthResult<Nothing>()
}

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: HealthPlatApiService,
    private val userPreferences: UserPreferencesDataStore
) {

    /**
     * Request OTP for phone number
     * Sends verification code via SMS
     */
    suspend fun requestPhoneVerification(phoneNumber: String): AuthResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                val request = RequestPhoneVerificationRequest(
                    phoneNumber = phoneNumber
                )

                val response = apiService.requestPhoneVerification(request)

                if (response.isSuccessful) {
                    val body = response.body()

                    if (body != null && body.isSuccess) {
                        // Success - OTP sent
                        Timber.i("OTP sent successfully to $phoneNumber")
                        AuthResult.Success(body.message ?: "کد با موفقیت ارسال شد")
                    } else {
                        // API returned error
                        val errorMessage = body?.errors?.firstOrNull()
                            ?: body?.message
                            ?: "خطا در ارسال کد"
                        Timber.w("Request OTP failed: $errorMessage")
                        AuthResult.Error(errorMessage)
                    }
                } else {
                    // HTTP error
                    val errorMessage = "خطای سرور: ${response.code()}"
                    Timber.e("Request OTP HTTP error: ${response.code()}")
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "Request OTP exception")
                AuthResult.Error("خطا در ارتباط با سرور. لطفا اتصال اینترنت خود را بررسی کنید")
            }
        }
    }

    /**
     * Verify OTP and login
     * Returns access token and user info
     */
    suspend fun loginByPhone(
        phoneNumber: String,
        verificationCode: String
    ): AuthResult<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = LoginByPhoneRequest(
                    phoneNumber = phoneNumber,
                    verificationCode = verificationCode
                )

                val response = apiService.loginByPhone(request)

                if (response.isSuccessful) {
                    val body = response.body()

                    if (body != null && body.isSuccess && body.data != null) {
                        val loginData = body.data

                        // Save tokens and user info to preferences
                        userPreferences.saveAuthToken(loginData.accessToken)
                        userPreferences.saveRefreshToken(loginData.refreshToken)
                        // Convert GUID string to Long for storage (hash it)
                        userPreferences.saveUserId(loginData.userId.hashCode().toLong())
                        userPreferences.savePhoneNumber(phoneNumber)

                        Timber.i("Login successful for user: ${loginData.userId}")
                        AuthResult.Success(loginData)
                    } else {
                        // API returned error
                        val errorMessage = body?.errors?.firstOrNull()
                            ?: body?.message
                            ?: "کد تایید نامعتبر است"
                        Timber.w("Login failed: $errorMessage")
                        AuthResult.Error(errorMessage)
                    }
                } else {
                    // HTTP error
                    val errorMessage = when (response.code()) {
                        401, 403 -> "کد تایید نامعتبر یا منقضی شده است"
                        404 -> "سرویس یافت نشد"
                        500 -> "خطای سرور"
                        else -> "خطا در ورود: ${response.code()}"
                    }
                    Timber.e("Login HTTP error: ${response.code()}")
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "Login exception")
                AuthResult.Error("خطا در ارتباط با سرور. لطفا اتصال اینترنت خود را بررسی کنید")
            }
        }
    }

    /**
     * Logout user
     * Clears tokens and calls logout API
     */
    suspend fun logout(): AuthResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Call logout API
                val response = apiService.logout()

                // Clear local data regardless of API response
                userPreferences.clearAll()

                if (response.isSuccessful) {
                    Timber.i("Logout successful")
                    AuthResult.Success(Unit)
                } else {
                    Timber.w("Logout API failed but local data cleared")
                    AuthResult.Success(Unit) // Still success since local data is cleared
                }
            } catch (e: Exception) {
                Timber.e(e, "Logout exception")
                // Clear local data anyway
                userPreferences.clearAll()
                AuthResult.Success(Unit)
            }
        }
    }

    /**
     * Check if user is logged in
     */
    suspend fun isLoggedIn(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val token = userPreferences.getAuthToken()
                var hasToken = false
                token.collect { t ->
                    hasToken = !t.isNullOrEmpty()
                }
                hasToken
            } catch (e: Exception) {
                Timber.e(e, "Error checking login status")
                false
            }
        }
    }
}