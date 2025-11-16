package com.bonyad.healthplat.data.repository

import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.HealthPlatApiService
import com.bonyad.healthplat.domain.model.AddUserDeviceRequest
import com.bonyad.healthplat.domain.model.UserDeviceData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val apiService: HealthPlatApiService,
    private val userPreferences: UserPreferencesDataStore
) {

    /**
     * Register a new device for the user
     * Called from DeviceConnectionScreen after BLE connection
     */
    suspend fun addUserDevice(
        deviceMac: String,
        deviceName: String?,
        firmwareVersion: String?
    ): AuthResult<UserDeviceData> {
        return withContext(Dispatchers.IO) {
            try {
                // Get userId from preferences
                val userId = userPreferences.getUserId().first()
                if (userId == null) {
                    return@withContext AuthResult.Error("کاربر وارد نشده است")
                }

                val userIdString = userId.toString()

                val request = AddUserDeviceRequest(
                    userId = userIdString,
                    deviceMac = deviceMac,
                    deviceName = deviceName,
                    deviceType = "ring",
                    firmwareVersion = firmwareVersion,
                    isActive = true
                )

                val response = apiService.addUserDevice(request)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val device = response.body()?.data
                    if (device != null) {
                        // Save device info locally
                        userPreferences.saveDeviceInfo(deviceMac, deviceName)

                        Timber.i("Device registered: $deviceMac")
                        AuthResult.Success(device)
                    } else {
                        AuthResult.Error("خطا در ثبت دستگاه")
                    }
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: response.body()?.message
                        ?: "خطا در ثبت دستگاه"
                    Timber.w("Add device failed: $errorMessage")
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "Add device exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    /**
     * Get all devices for current user
     */
    suspend fun getUserDevices(): AuthResult<List<UserDeviceData>> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = userPreferences.getUserId().first()
                if (userId == null) {
                    return@withContext AuthResult.Error("کاربر وارد نشده است")
                }

                val userIdString = userId.toString()
                val response = apiService.getUserDevices(userIdString)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val devices = response.body()?.data ?: emptyList()
                    AuthResult.Success(devices)
                } else {
                    val errorMessage = response.body()?.errors?.firstOrNull()
                        ?: "خطا در دریافت لیست دستگاه‌ها"
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "Get devices exception")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }


    suspend fun deactivateDevice(deviceId: Int): AuthResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.deactivateDevice(deviceId)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    AuthResult.Success(Unit)
                } else {
                    AuthResult.Error("خطا در غیرفعال‌سازی دستگاه")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to deactivate device")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }
}