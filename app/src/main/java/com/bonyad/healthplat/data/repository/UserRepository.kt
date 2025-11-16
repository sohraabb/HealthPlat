package com.bonyad.healthplat.data.repository

import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.HealthPlatApiService
import com.bonyad.healthplat.domain.model.UpdateUserRequest
import com.bonyad.healthplat.domain.model.UserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val apiService: HealthPlatApiService,
    private val userPreferences: UserPreferencesDataStore
) {

    /**
     * Add user personal information after registration
     * This is called from PersonalInfoScreen
     */
    suspend fun updateUserInfo(
        name: String,
        birthDate: String,  // Format: "2025-11-12T00:00:00Z"
        gender: Int,        // 1 = Male, 2 = Female
        height: Int,
        weight: Int
    ): AuthResult<UserData> {
        return withContext(Dispatchers.IO) {
            try {
                // Get userId from preferences
                val userId = userPreferences.getUserId().first()
                val acceptedTerms = userPreferences.isTermsAccepted().first()
                val enabledMarketing = userPreferences.isMarketingAccepted().first()
                val token = userPreferences.getAuthToken().first() // ✅ Get token for debugging

                Timber.d("=== Update User Info ===")
                Timber.d("UserId: $userId")
                Timber.d("Token: ${token?.take(20)}...")
                Timber.d("Name: $name")
                Timber.d("Gender: $gender")
                Timber.d("Height: $height")
                Timber.d("Weight: $weight")
                Timber.d("Terms: $acceptedTerms")
                Timber.d("Marketing: $enabledMarketing")

                if (userId == null) {
                    return@withContext AuthResult.Error("کاربر وارد نشده است")
                }

                // Convert userId Long to String (reverse the hashCode operation)
                // Note: We stored userId.hashCode().toLong() in preferences
                // For now, we'll use the userId as string directly
                val userIdString = userId.toString()

                val request = UpdateUserRequest(
                    userId = userIdString,
                    name = name,
                    birthDate = birthDate,
                    gender = gender,
                    height = height,
                    weight = weight,
                    acceptedTermsAndPolicy = acceptedTerms,
                    enabledEmailMarketing = enabledMarketing
                )

                val response = apiService.updateUserInfo(request)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val userData = response.body()?.data
                    if (userData != null) {
                        // Save personal info locally as well
                        userPreferences.savePersonalInfo(
                            name = name,
                            birthDate = birthDate,
                            height = height,
                            weight = weight,
                            gender = if (gender == 1) "مرد" else "زن"
                        )

                        Timber.i("User info added successfully")
                        AuthResult.Success(userData)
                    } else {
                        AuthResult.Error("خطا در ذخیره اطلاعات")
                    }
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: response.body()?.message
                        ?: "خطا در ذخیره اطلاعات"
                    Timber.w("Add user info failed: $errorMessage")
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "Add user info exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Get user profile
     */
    suspend fun getUserProfile(): AuthResult<UserData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getUserProfile()

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val userData = response.body()?.data
                    if (userData != null) {
                        AuthResult.Success(userData)
                    } else {
                        AuthResult.Error("خطا در دریافت اطلاعات کاربر")
                    }
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: "خطا در دریافت اطلاعات کاربر"
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "Get user profile exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }
}