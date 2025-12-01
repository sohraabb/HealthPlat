package com.bonyad.healthplat.data.repository

import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.HealthPlatApiService
import com.bonyad.healthplat.domain.model.MetricRequest
import com.bonyad.healthplat.domain.model.RecordDataResult
import com.bonyad.healthplat.domain.model.SyncHealthDataRequest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class HealthDataRepository @Inject constructor(
    private val deviceManager: HealthDeviceManager,
    private val apiService: HealthPlatApiService,
    private val userPreferences: UserPreferencesDataStore
) {

    suspend fun syncDashboardData(): RecordDataResult {
        val today = 0
        val yesterday = 1
        val recordResult = deviceManager.getRecordData(5)

        if (recordResult is RecordDataResult.Success) {
            uploadHealthData(recordResult)
        }

        return recordResult
    }

    private suspend fun uploadHealthData(data: RecordDataResult.Success) = coroutineScope {
        // 1. Get User and Device Info
        val userId = userPreferences.getUserId().first()
        val deviceId = userPreferences.getDeviceId().first() // You need to implement getDeviceId in Prefs

        if (userId == null || deviceId == null) {
            Timber.e("❌ Cannot sync: Missing UserId or DeviceId")
            return@coroutineScope
        }

        // 2. FIX: Get Date from the SDK Bean, not the System Clock
        // The SDK returns "yyyy-MM-dd". We append the time.
        // We try heartRate first, if null, try steps, etc.
        val sdkDateString = data.heartRate?.recordDay
            ?: data.steps?.recordDay
            ?: data.sleep?.recordDay

        // Fallback to system date ONLY if SDK returns absolutely nothing (rare)
        val finalRecordDate = if (!sdkDateString.isNullOrEmpty()) {
            "${sdkDateString}T00:00:00Z"
        } else {
            getCurrentDateISO()
        }

        Timber.i("🔄 Starting parallel upload for date: $finalRecordDate")

        // 3. FIX: Parallel Uploads using 'launch' inside 'coroutineScope'
        // This fires all requests at the same time.

        // Heart Rate
        if (data.heartRate?.heartRateSource?.any { it > 0 } == true) {
            Timber.w("HR data value : {${data.heartRate.heartRateSource}}")

            launch {
                uploadMetric(userId, deviceId, finalRecordDate, data.heartRate.heartRateSource, MetricType.HEART_RATE)
            }
        }

        // Steps
        if (data.steps?.stepSource?.any { it > 0 } == true) {
            Timber.w("Steps data value : {${data.steps.stepSource}}")

            launch {
                uploadMetric(userId, deviceId, finalRecordDate, data.steps.stepSource, MetricType.STEPS)
            }
        }

        // Sleep
        if (data.sleep?.sourceList?.any { it > 0 } == true) {
            Timber.w("Sleep data value : {${data.sleep.sourceList}}")
            launch {
                uploadMetric(userId, deviceId, finalRecordDate, data.sleep.sourceList, MetricType.SLEEP)
            }
        }

        // SpO2
        if (data.spo2?.sourceList?.any { it > 0 } == true) {
            Timber.w("Spo2 data value : {${data.spo2.sourceList}}")

            launch {
                uploadMetric(userId, deviceId, finalRecordDate, data.spo2.sourceList, MetricType.SPO2)
            }
        }

        // Stress
        if (data.stress?.stressSource?.any { it > 0 } == true) {
            Timber.w("Stress data value : {${data.stress.stressSource}}")

            launch {
                uploadMetric(userId, deviceId, finalRecordDate, data.stress.stressSource, MetricType.STRESS)
            }
        }

        // HRV
        if (data.hrv?.hrvSource?.any { it > 0 } == true) {
            Timber.w("Hrv data value : {${data.hrv.hrvSource}}")
            launch {
                uploadMetric(userId, deviceId, finalRecordDate, data.hrv.hrvSource, MetricType.HRV)
            }
        }
    }

    private suspend fun uploadMetric(
        userId: String,
        deviceId: Int,
        recordDate: String,
        values: List<Int>,
        type: MetricType
    ) {
        try {
            val request = MetricRequest(
                userId = userId,
                deviceId = deviceId,
                recordDate = recordDate,
                values = values
            )

            val response = when (type) {
                MetricType.HEART_RATE -> apiService.uploadHeartRate(request)
                MetricType.STEPS -> apiService.uploadSteps(request)
                MetricType.SLEEP -> apiService.uploadSleep(request)
                MetricType.SPO2 -> apiService.uploadSpo2(request)
                MetricType.STRESS -> apiService.uploadStress(request)
                MetricType.HRV -> apiService.uploadHrv(request)
            }

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Timber.i("✅ Uploaded ${type.name}: ${values.size} values")
            } else {
                Timber.w("⚠️ Failed to upload ${type.name}: ${response.body()?.message}")
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error uploading ${type.name}")
        }
    }

    private fun getCurrentDateISO(): String {
        val calendar = java.util.Calendar.getInstance()
        return String.format(
            "%04d-%02d-%02dT00:00:00Z",
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }
}

enum class MetricType {
    HEART_RATE, STEPS, SLEEP, SPO2, STRESS, HRV
}