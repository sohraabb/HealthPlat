package com.bonyad.healthplat.data.repository

import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.HealthPlatApiService
import com.bonyad.healthplat.domain.model.RegisterDeviceRequest
import com.bonyad.healthplat.domain.model.UserDevice
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

    suspend fun registerDevice(
        deviceMac: String,
        deviceName: String?,
        firmwareVersion: String?,
        batteryLevel: Int?
    ): AuthResult<UserDevice> {
        return withContext(Dispatchers.IO) {
            try {
                // Get userId from preferences
                val userId = userPreferences.getUserId().first()
                if (userId == null) {
                    return@withContext AuthResult.Error("کاربر وارد نشده است")
                }

                val request = RegisterDeviceRequest(
                    userId = userId.toString(),
                    deviceMac = deviceMac,
                    deviceName = deviceName,
                    deviceType = "ring",
                    firmwareVersion = firmwareVersion,
                    batteryLevel = batteryLevel,
                    isActive = true
                )

                val response = apiService.registerDevice(request)

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
                        ?: "خطا در ثبت دستگاه"
                    AuthResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                Timber.e(e, "Device registration failed")
                AuthResult.Error("خطا در ارتباط با سرور")
            }
        }
    }

    suspend fun getUserDevices(): AuthResult<List<UserDevice>> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = userPreferences.getUserId().first()
                if (userId == null) {
                    return@withContext AuthResult.Error("کاربر وارد نشده است")
                }

                val response = apiService.getUserDevices(userId.toString())

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val devices = response.body()?.data ?: emptyList()
                    AuthResult.Success(devices)
                } else {
                    AuthResult.Error("خطا در دریافت لیست دستگاه‌ها")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to get devices")
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