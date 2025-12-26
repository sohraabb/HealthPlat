package com.bonyad.healthplat.data.repository

import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.HealthPlatApiService
import com.bonyad.healthplat.domain.model.AddCaregiverByUserIdRequest
import com.bonyad.healthplat.domain.model.AddCaregiverRequest
import com.bonyad.healthplat.domain.model.CarePermissions
import com.bonyad.healthplat.domain.model.CaregiverData
import com.bonyad.healthplat.domain.model.CaregiverUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CareRepository @Inject constructor(
    private val apiService: HealthPlatApiService,
    private val userPreferences: UserPreferencesDataStore
) {

    /**
     * Add a caregiver by phone number
     * Returns the caregiver data if successful
     */
    suspend fun addCaregiverByPhone(
        phoneNumber: String,
        permissions: CarePermissions
    ): AuthResult<CaregiverData> {
        return withContext(Dispatchers.IO) {
            try {
                val request = AddCaregiverRequest(
                    phoneNumber = phoneNumber,
                    heartRate = permissions.heartRate,
                    bloodPressure = permissions.bloodPressure,
                    stressLevel = permissions.stressLevel,
                    sleepQuality = permissions.sleepQuality
                )

                val response = apiService.addCaregiver(request)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val data = response.body()?.data
                    if (data != null) {
                        Timber.i("✅ Caregiver added: ${data.phoneNumber}")
                        AuthResult.Success(data)
                    } else {
                        AuthResult.Error("خطا در افزودن تن‌بار")
                    }
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: response.body()?.message
                        ?: "خطا در افزودن تن‌بار"
                    Timber.w("❌ Add caregiver failed: $errorMessage")
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Add caregiver exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Add a caregiver by user ID (from QR code scan)
     * This method automatically accepts the relationship
     */
    suspend fun addCaregiverByUserId(
        userId: String,
        permissions: CarePermissions
    ): AuthResult<CaregiverData> {
        return withContext(Dispatchers.IO) {
            try {
                val request = AddCaregiverByUserIdRequest(
                    userId = userId,
                    heartRate = permissions.heartRate,
                    bloodPressure = permissions.bloodPressure,
                    stressLevel = permissions.stressLevel,
                    sleepQuality = permissions.sleepQuality
                )

                val response = apiService.addCaregiverByUserId(request)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val data = response.body()?.data
                    if (data != null) {
                        Timber.i("✅ Caregiver added by QR code: ${data.caregiverId}")
                        AuthResult.Success(data)
                    } else {
                        AuthResult.Error("خطا در افزودن تن‌بار")
                    }
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: response.body()?.message
                        ?: "خطا در افزودن تن‌بار"
                    Timber.w("❌ Add caregiver by QR failed: $errorMessage")
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Add caregiver by QR exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Accept a caregiver request
     * Called by the caregiver to accept being someone's caregiver
     */
    suspend fun acceptCaregiverRequest(careId: Int): AuthResult<CaregiverData> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.acceptCaregiverRequest(careId)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val data = response.body()?.data
                    if (data != null) {
                        Timber.i("✅ Caregiver request accepted: $careId")
                        AuthResult.Success(data)
                    } else {
                        AuthResult.Error("خطا در پذیرش درخواست")
                    }
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: "خطا در پذیرش درخواست"
                    Timber.w("❌ Accept caregiver failed: $errorMessage")
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Accept caregiver exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Get my caregivers (people taking care of me)
     */
    suspend fun getMyCaregivers(): AuthResult<List<CaregiverUiModel>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMyCaregivers()

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val caregivers = response.body()?.data ?: emptyList()

                    val uiModels = caregivers.map { it.toUiModel() }

                    Timber.i("✅ Got ${caregivers.size} caregivers")
                    AuthResult.Success(uiModels)
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: "خطا در دریافت لیست تن‌بارها"
                    Timber.w("❌ Get caregivers failed: $errorMessage")
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Get caregivers exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Get users I'm taking care of
     */
    suspend fun getMyUsers(): AuthResult<List<CaregiverUiModel>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMyUsers()

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val users = response.body()?.data ?: emptyList()

                    val uiModels = users.map { it.toUiModel() }

                    Timber.i("✅ Got ${users.size} users I'm caring for")
                    AuthResult.Success(uiModels)
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: "خطا در دریافت لیست کاربران"
                    Timber.w("❌ Get my users failed: $errorMessage")
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Get my users exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Update caregiver permissions
     */
    suspend fun updateCaregiverPermissions(
        careId: Int,
        phoneNumber: String,
        permissions: CarePermissions
    ): AuthResult<CaregiverData> {
        return withContext(Dispatchers.IO) {
            try {
                val request = AddCaregiverRequest(
                    phoneNumber = phoneNumber,
                    heartRate = permissions.heartRate,
                    bloodPressure = permissions.bloodPressure,
                    stressLevel = permissions.stressLevel,
                    sleepQuality = permissions.sleepQuality
                )

                val response = apiService.updateCaregiverPermissions(careId, request)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val data = response.body()?.data
                    if (data != null) {
                        Timber.i("✅ Caregiver permissions updated: $careId")
                        AuthResult.Success(data)
                    } else {
                        AuthResult.Error("خطا در به‌روزرسانی دسترسی‌ها")
                    }
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: "خطا در به‌روزرسانی دسترسی‌ها"
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Update permissions exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Delete a caregiver relationship
     */
    suspend fun deleteCaregiver(careId: Int): AuthResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.deleteCaregiver(careId)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    Timber.i("✅ Caregiver deleted: $careId")
                    AuthResult.Success(Unit)
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: "خطا در حذف تن‌بار"
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Delete caregiver exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Generate QR code data for sharing
     */
    suspend fun generateQrCodeData(): String {
        val userId = userPreferences.getUserId().first() ?: ""
        val userName = userPreferences.getUserName().first() ?: "کاربر"

        // Simple format: userId|userName|timestamp
        return "$userId|$userName|${System.currentTimeMillis()}"
    }

    /**
     * Parse QR code data
     * Returns userId if valid, null otherwise
     */
    fun parseQrCodeData(qrData: String): String? {
        return try {
            val parts = qrData.split("|")
            if (parts.size == 3) {
                val userId = parts[0]
                val timestamp = parts[2].toLong()

                // Check if QR code is not older than 5 minutes
                val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)
                if (timestamp > fiveMinutesAgo) {
                    userId
                } else {
                    Timber.w("QR code expired")
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse QR code")
            null
        }
    }

    /**
     * Convert API model to UI model
     */
    private fun CaregiverData.toUiModel() = CaregiverUiModel(
        id = id,
        name = null, // Backend doesn't return name yet
        phoneNumber = phoneNumber,
        userId = userId,
        isPending = !isAccepted,
        permissions = CarePermissions(
            heartRate = heartRate,
            bloodPressure = bloodPressure,
            stressLevel = stressLevel,
            sleepQuality = sleepQuality
        )
    )
}