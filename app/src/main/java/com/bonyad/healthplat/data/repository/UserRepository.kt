package com.bonyad.healthplat.data.repository

import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.HealthPlatApiService
import com.bonyad.healthplat.domain.model.UpdateUserRequest
import com.bonyad.healthplat.domain.model.UserData
import com.bonyad.healthplat.domain.model.UserOverviewData
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
     * Get user overview and sync to local preferences
     * This should be called after login/registration or when preferences are empty
     */
    suspend fun getUserOverview(): AuthResult<UserOverviewData> {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("📥 Fetching user overview from API...")

                val response = apiService.getUserOverview()

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val overview = response.body()?.data

                    if (overview != null) {
                        Timber.i("✅ User overview fetched successfully")

                        // Sync to preferences
                        syncOverviewToPreferences(overview)

                        AuthResult.Success(overview)
                    } else {
                        Timber.w("⚠️ User overview data is null")
                        AuthResult.Error("خطا در دریافت اطلاعات کاربر")
                    }
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: response.body()?.message
                        ?: "خطا در دریافت اطلاعات کاربر"
                    Timber.w("❌ Get user overview failed: $errorMessage")
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Get user overview exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Sync user overview data to local preferences
     */
    private suspend fun syncOverviewToPreferences(overview: UserOverviewData) {
        try {
            Timber.d("💾 Syncing user overview to preferences...")

            // Save user ID
            userPreferences.saveUserId(overview.id)

            // Save phone number
            userPreferences.savePhoneNumber(overview.phoneNumber)

            // Save personal info if available
            if (!overview.name.isNullOrEmpty() &&
                !overview.birthDate.isNullOrEmpty() &&
                overview.height != null &&
                overview.weight != null &&
                overview.gender != null) {

                userPreferences.savePersonalInfo(
                    name = overview.name,
                    birthDate = overview.birthDate,
                    height = overview.height,
                    weight = overview.weight,
                    gender = if (overview.gender == 1) "مرد" else "زن"
                )

                Timber.d("💾 Saved personal info: ${overview.name}")
            }

            // Save terms acceptance status
            userPreferences.setTermsAccepted(overview.acceptedTermsAndPolicy)
            userPreferences.setMarketingAccepted(overview.enabledEmailMarketing)

            // Save device info if available
            val activeDevice = overview.userDevices?.firstOrNull { it.isActive }
            if (activeDevice != null) {
                userPreferences.saveDeviceInfo(
                    mac = activeDevice.deviceMac,
                    name = activeDevice.deviceName
                )
                userPreferences.saveDeviceId(activeDevice.id)

                Timber.d("💾 Saved device: ${activeDevice.deviceName} (${activeDevice.deviceMac})")
            }

            Timber.i("✅ User overview synced to preferences")

        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to sync overview to preferences")
        }
    }

    /**
     * Ensure user data is available (fetch from API if preferences are empty)
     */
    suspend fun ensureUserDataAvailable(): AuthResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if we have essential data in preferences
                val userName = userPreferences.getUserName().first()
                val deviceMac = userPreferences.getDeviceMac().first()

                if (userName.isNullOrEmpty() || deviceMac.isNullOrEmpty()) {
                    Timber.d("⚠️ Missing user data in preferences, fetching from API...")

                    when (val result = getUserOverview()) {
                        is AuthResult.Success -> {
                            Timber.i("✅ User data fetched and synced")
                            AuthResult.Success(Unit)
                        }
                        is AuthResult.Error -> {
                            Timber.e("❌ Failed to fetch user data: ${result.message}")
                            AuthResult.Error(result.message)
                        }
                    }
                } else {
                    Timber.d("✅ User data already available in preferences")
                    AuthResult.Success(Unit)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Error ensuring user data")
                AuthResult.Error("خطا در بارگذاری اطلاعات کاربر")
            }
        }
    }

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