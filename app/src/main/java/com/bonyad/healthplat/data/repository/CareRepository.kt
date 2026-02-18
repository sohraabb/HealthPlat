package com.bonyad.healthplat.data.repository

import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.HealthPlatApiService
import com.bonyad.healthplat.domain.model.AddCaregiverByScanRequest
import com.bonyad.healthplat.domain.model.AddCaregiverRequest
import com.bonyad.healthplat.domain.model.CarePermissions
import com.bonyad.healthplat.domain.model.CaregiverData
import com.bonyad.healthplat.domain.model.CaregiverUiModel
import com.bonyad.healthplat.domain.model.MetricData
import com.bonyad.healthplat.domain.model.UpdateCaregiverPermissionsRequest
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

    // ============ Add Caregiver ============

    /**
     * Add a caregiver by phone number
     * POST /api/Caregiver/AddCareGiverByPhoneNumber
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

                val response = apiService.addCaregiverByPhoneNumber(request)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val data = response.body()?.data
                    if (data != null) {
                        Timber.i("✅ Caregiver added: ${data.caregiverPhoneNumber}")
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
     * POST /api/Caregiver/AddCareGiverByScan
     */
    suspend fun addCaregiverByScan(
        userId: String,
        permissions: CarePermissions
    ): AuthResult<CaregiverData> {
        return withContext(Dispatchers.IO) {
            try {
                val request = AddCaregiverByScanRequest(
                    userId = userId,
                    heartRate = permissions.heartRate,
                    bloodPressure = permissions.bloodPressure,
                    stressLevel = permissions.stressLevel,
                    sleepQuality = permissions.sleepQuality
                )

                val response = apiService.addCaregiverByScan(request)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val data = response.body()?.data
                    if (data != null) {
                        Timber.i("✅ Caregiver added by QR scan: ${data.caregiverId}")
                        AuthResult.Success(data)
                    } else {
                        AuthResult.Error("خطا در افزودن تن‌بار")
                    }
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: response.body()?.message
                        ?: "خطا در افزودن تن‌بار"
                    Timber.w("❌ Add caregiver by scan failed: $errorMessage")
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Add caregiver by scan exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    // ============ Accept / Update / Delete ============

    /**
     * Accept a caregiver request
     * PUT /api/Caregiver/Accept/{CareId}
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
     * Update caregiver permissions (no PhoneNumber in request body)
     * PUT /api/Caregiver/Update/{id}
     */
    suspend fun updateCaregiverPermissions(
        careId: Int,
        permissions: CarePermissions
    ): AuthResult<CaregiverData> {
        return withContext(Dispatchers.IO) {
            try {
                val request = UpdateCaregiverPermissionsRequest(
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
     * DELETE /api/Caregiver/Delete/{CareId}
     */
    suspend fun deleteCaregiver(careId: Int): AuthResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.deleteCaregiver(careId)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    Timber.i("✅ Caregiver deleted: $careId")
                    AuthResult.Success(true)
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

    // ============ Get Lists ============

    /**
     * Get my caregivers (people taking care of me)
     * GET /api/Caregiver/GetMyCaregivers
     */
    suspend fun getMyCaregivers(): AuthResult<List<CaregiverUiModel>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMyCaregivers()

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val caregivers = response.body()?.data ?: emptyList()
                    val uiModels = caregivers.map { it.toCaregiverUiModel() }
                    Timber.i("✅ Got ${caregivers.size} caregivers")
                    AuthResult.Success(uiModels)
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: "خطا در دریافت لیست تن‌باره‌ها"
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
     * Get patients I'm taking care of
     * GET /api/Caregiver/GetMyPatients
     */
    suspend fun getMyPatients(): AuthResult<List<CaregiverUiModel>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMyPatients()

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val patients = response.body()?.data ?: emptyList()
                    val uiModels = patients.map { it.toPatientUiModel() }
                    Timber.i("✅ Got ${patients.size} patients I'm caring for")
                    AuthResult.Success(uiModels)
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: "خطا در دریافت لیست کاربران"
                    Timber.w("❌ Get my patients failed: $errorMessage")
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Get my patients exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    // ============ Caregiver Metrics (NEW) ============

    /**
     * Get patient's heart rate metrics as a caregiver
     */
    suspend fun getPatientHeartRate(
        patientUserId: String,
        dateFrom: String,
        dateTo: String
    ): AuthResult<List<MetricData>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCaregiverHeartRate(patientUserId, dateFrom, dateTo)
                handleMetricResponse(response, "HeartRate")
            } catch (e: Exception) {
                Timber.e(e, "❌ Get patient heart rate exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Get patient's sleep metrics as a caregiver
     */
    suspend fun getPatientSleep(
        patientUserId: String,
        dateFrom: String,
        dateTo: String
    ): AuthResult<List<MetricData>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCaregiverSleep(patientUserId, dateFrom, dateTo)
                handleMetricResponse(response, "Sleep")
            } catch (e: Exception) {
                Timber.e(e, "❌ Get patient sleep exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Get patient's SpO2 metrics as a caregiver
     */
    suspend fun getPatientSpo2(
        patientUserId: String,
        dateFrom: String,
        dateTo: String
    ): AuthResult<List<MetricData>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCaregiverSpo2(patientUserId, dateFrom, dateTo)
                handleMetricResponse(response, "Spo2")
            } catch (e: Exception) {
                Timber.e(e, "❌ Get patient spo2 exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Get patient's stress metrics as a caregiver
     */
    suspend fun getPatientStress(
        patientUserId: String,
        dateFrom: String,
        dateTo: String
    ): AuthResult<List<MetricData>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCaregiverStress(patientUserId, dateFrom, dateTo)
                handleMetricResponse(response, "Stress")
            } catch (e: Exception) {
                Timber.e(e, "❌ Get patient stress exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Helper to handle metric API responses
     */
    private fun handleMetricResponse(
        response: retrofit2.Response<com.bonyad.healthplat.domain.model.ApiResponse<List<MetricData>>>,
        metricName: String
    ): AuthResult<List<MetricData>> {
        return if (response.isSuccessful && response.body()?.isSuccess == true) {
            val data = response.body()?.data ?: emptyList()
            Timber.i("✅ Got ${data.size} $metricName metrics for patient")
            AuthResult.Success(data)
        } else {
            val errorMessage = response.body()?.errors?.firstOrNull()
                ?: "خطا در دریافت اطلاعات $metricName"
            Timber.w("❌ Get patient $metricName failed: $errorMessage")
            AuthResult.Error(errorMessage)
        }
    }

    // ============ QR Code ============

    /**
     * Generate QR code data for sharing
     */
    suspend fun generateQrCodeData(): String {
        val userId = userPreferences.getUserId().first() ?: ""
        val userName = userPreferences.getUserName().first() ?: "کاربر"
        return "$userId|$userName|${System.currentTimeMillis()}"
    }

    /**
     * Parse QR code data — returns userId if valid, null otherwise
     */
    fun parseQrCodeData(qrData: String): String? {
        return try {
            val parts = qrData.split("|")
            if (parts.size == 3) {
                val userId = parts[0]
                val timestamp = parts[2].toLong()
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

    // ============ Model Mapping ============

    /**
     * For MY_CAREGIVERS tab: show caregiver info (name/phone of who cares for me)
     */
    private fun CaregiverData.toCaregiverUiModel() = CaregiverUiModel(
        id = id,
        name = caregiverName,
        phoneNumber = caregiverPhoneNumber ?: "شماره نامشخص",
        userId = caregiverId,
        patientId = patientId,
        isPending = !isAccepted,
        permissions = CarePermissions(
            heartRate = heartRate,
            bloodPressure = bloodPressure,
            stressLevel = stressLevel,
            sleepQuality = sleepQuality
        )
    )

    /**
     * For I_AM_CAREGIVER tab: show patient info (name/phone of who I'm caring for)
     */
    private fun CaregiverData.toPatientUiModel() = CaregiverUiModel(
        id = id,
        name = patientName,
        phoneNumber = patientPhoneNumber ?: "شماره نامشخص",
        userId = caregiverId,
        patientId = patientId,
        isPending = !isAccepted,
        permissions = CarePermissions(
            heartRate = heartRate,
            bloodPressure = bloodPressure,
            stressLevel = stressLevel,
            sleepQuality = sleepQuality
        )
    )
}