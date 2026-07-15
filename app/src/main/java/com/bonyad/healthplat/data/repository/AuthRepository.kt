package com.bonyad.healthplat.data.repository

import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.HealthPlatApiService
import com.bonyad.healthplat.domain.model.LoginByPhoneRequest
import com.bonyad.healthplat.domain.model.LoginResponse
import com.bonyad.healthplat.domain.model.PhoneVerificationData
import com.bonyad.healthplat.domain.model.RefreshTokenRequest
import com.bonyad.healthplat.domain.model.RefreshTokenResponse
import com.bonyad.healthplat.domain.model.RegisterByPhoneRequest
import com.bonyad.healthplat.domain.model.RequestPhoneVerificationRequest
import com.bonyad.healthplat.domain.model.SendOtpRequest
import com.bonyad.healthplat.domain.model.VerifyOtpRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
     * Step 1: Request phone verification (send OTP)
     * Returns userId if user exists, null if new user
     */
    suspend fun requestPhoneVerification(phoneNumber: String): AuthResult<PhoneVerificationData> {
        return withContext(Dispatchers.IO) {
            try {
                val request = RequestPhoneVerificationRequest(
                    phoneNumber = phoneNumber,
                    codeExpirationMinutes = 2
                )

                val response = apiService.requestPhoneVerification(request)

                if (response.isSuccessful) {
                    val body = response.body()

                    if (body != null && body.isSuccess && body.data != null) {
                        Timber.i("OTP sent to $phoneNumber - userId: ${body.data.userId ?: "NEW USER"}")
                        AuthResult.Success(body.data)
                    } else {
                        val errorMessage = body?.errors?.firstOrNull()
                            ?: body?.message
                            ?: "خطا در ارسال کد"
                        Timber.w("Request OTP failed: $errorMessage")
                        AuthResult.Error(errorMessage)
                    }
                } else {
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
     * Step 2a: Login (for existing users)
     * Called when userId was returned from requestPhoneVerification
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

                        // Save tokens and user info
                        saveAuthData(loginData, phoneNumber)

                        Timber.i("Login successful for user: ${loginData.userId}")
                        AuthResult.Success(loginData)
                    } else {
                        val errorMessage = body?.errors?.firstOrNull()
                            ?: body?.message
                            ?: "کد تایید نامعتبر است"
                        Timber.w("Login failed: $errorMessage")
                        AuthResult.Error(errorMessage)
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        401, 403 -> "کد تایید نامعتبر یا منقضی شده است"
                        404 -> "کاربر یافت نشد"
                        else -> "خطا در ورود"
                    }
                    Timber.e("Login HTTP error: ${response.code()}")
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "Login exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }
    /**
     * Step 2b: Register (for new users)
     * Called when userId was null from requestPhoneVerification
     */
    suspend fun registerByPhone(
        phoneNumber: String,
        verificationCode: String
    ): AuthResult<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = RegisterByPhoneRequest(
                    phoneNumber = phoneNumber,
                    verificationCode = verificationCode
                )

                val response = apiService.registerByPhone(request)

                if (response.isSuccessful) {
                    val body = response.body()

                    if (body != null && body.isSuccess && body.data != null) {
                        val registerData = body.data

                        // Save tokens and user info
                        saveAuthData(registerData, phoneNumber)

                        Timber.i("Registration successful for user: ${registerData.userId}")
                        AuthResult.Success(registerData)
                    } else {
                        val errorMessage = body?.errors?.firstOrNull()
                            ?: body?.message
                            ?: "خطا در ثبت‌نام"
                        Timber.w("Registration failed: $errorMessage")
                        AuthResult.Error(errorMessage)
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        401, 403 -> "کد تایید نامعتبر یا منقضی شده است"
                        else -> "خطا در ثبت‌نام"
                    }
                    Timber.e("Registration HTTP error: ${response.code()}")
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "Registration exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Save authentication data to preferences
     */
    private suspend fun saveAuthData(authData: LoginResponse, phoneNumber: String) {
        Timber.i("💾 AUTH: Saving auth data for userId=${authData.userId}")
        Timber.d("💾 AUTH: accessToken=${authData.accessToken.take(20)}...")
        Timber.d("💾 AUTH: refreshToken=${authData.refreshToken.take(20)}...")
        Timber.d("💾 AUTH: expDate=${authData.expDate}")

        // Save tokens atomically
        userPreferences.saveTokens(
            authData.accessToken,
            authData.refreshToken,
            authData.expDate
        )
        // Save other user data
        userPreferences.saveUserId(authData.userId)
        userPreferences.savePhoneNumber(phoneNumber)

        Timber.i("✅ AUTH: Auth data saved successfully")
    }

    suspend fun refreshTokenInternal(): RefreshTokenResponse? {
        val currentAccess = userPreferences.getAuthToken().first() ?: return null
        val currentRefresh = userPreferences.getRefreshToken().first() ?: return null

        val response = apiService.refreshToken(
            expiredTokenWithBearer = "Bearer $currentAccess",
            accessToken = currentAccess,
            refreshToken = currentRefresh
        )

        return if (response.isSuccessful && response.body()?.isSuccess == true) {
            val newData = response.body()?.data
            if (newData != null) {
                userPreferences.saveTokens(newData.accessToken, newData.refreshToken, newData.expDate!!)
                newData
            } else null
        } else {
            null
        }
    }

    private fun logTokenExpiration(token: String) {
        try {
            val parts = token.split(".")
            val payload = String(
                android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
            )
            val json = org.json.JSONObject(payload)
            val exp = json.getLong("exp")
            val now = System.currentTimeMillis() / 1000
            val minutesUntilExpiry = (exp - now) / 60.0

            Timber.d("⏰ New token expires in: %.1f minutes", minutesUntilExpiry)
        } catch (e: Exception) {
            Timber.w("Could not parse token expiration")
        }
    }

    /**
     * Logout user
     */
    suspend fun logout(): AuthResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Call logout API
                apiService.logout()

                // Clear all account data
                userPreferences.clearForLogout()

                Timber.i("Logout successful")
                AuthResult.Success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Logout exception")
                // Clear all account data anyway
                userPreferences.clearForLogout()
                AuthResult.Success(Unit)
            }
        }
    }

    /**
     * Check if user is logged in
     */
    suspend fun isLoggedIn() =
        userPreferences.getAuthToken().first().isNullOrEmpty().not()
}