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

    suspend fun syncDashboardData(day: Int): RecordDataResult {
        val recordResult = deviceManager.getRecordData(day)

        if (recordResult is RecordDataResult.Success) {
            uploadHealthData(recordResult)
        }

        return recordResult
    }

    private suspend fun uploadHealthData(data: RecordDataResult.Success) = coroutineScope {
        val userId = userPreferences.getUserId().first()
        val deviceId = userPreferences.getDeviceId().first()

        if (userId == null || deviceId == null) {
            Timber.e("❌ Cannot sync: Missing UserId or DeviceId")
            return@coroutineScope
        }

        // Get date from SDK beans [cite: 211, 217, 224]
        val sdkDateString = data.heartRate?.recordDay
            ?: data.steps?.recordDay
            ?: data.sleep?.recordDay
            ?: getCurrentDateString() // Fallback

        // Format to ISO 8601 as required by typical backends
        val finalRecordDate = "${sdkDateString}T00:00:00Z"

        Timber.i("🔄 Preparing upload for $finalRecordDate")

        // ---------------------------------------------------------
        // Parallel Uploads
        // ---------------------------------------------------------

        // 1. Heart Rate [cite: 212]
        // Checks if there is at least one non-zero reading
        if (data.heartRate?.heartRateSource?.any { it > 0 } == true) {
            launch {
                uploadMetric(
                    userId, deviceId, finalRecordDate,
                    data.heartRate.heartRateSource, MetricType.HEART_RATE
                )
            }
        }

        // 2. Steps [cite: 218]
        if (data.steps?.stepSource?.any { it > 0 } == true) {
            launch {
                uploadMetric(
                    userId, deviceId, finalRecordDate,
                    data.steps.stepSource, MetricType.STEPS
                )
            }
        }

        // 3. Sleep [cite: 234]
        // Filter > 0 keeps Deep(1), Light(2), Awake(3), REM(4)
        if (data.sleep?.sourceList?.any { it > 0 } == true) {
            launch {
                uploadMetric(
                    userId, deviceId, finalRecordDate,
                    data.sleep.sourceList, MetricType.SLEEP
                )
            }
        }

        // 4. SpO2 [cite: 260]
        if (data.spo2?.sourceList?.any { it > 0 } == true) {
            launch {
                uploadMetric(
                    userId, deviceId, finalRecordDate,
                    data.spo2.sourceList, MetricType.SPO2
                )
            }
        }

        // 5. Stress [cite: 252]
        if (data.stress?.stressSource?.any { it > 0 } == true) {
            launch {
                uploadMetric(
                    userId, deviceId, finalRecordDate,
                    data.stress.stressSource, MetricType.STRESS
                )
            }
        }

        // 6. HRV [cite: 266]
        if (data.hrv?.hrvSource?.any { it > 0 } == true) {
            launch {
                uploadMetric(
                    userId, deviceId, finalRecordDate,
                    data.hrv.hrvSource, MetricType.HRV
                )
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

    private fun getCurrentDateString(): String {
        val calendar = java.util.Calendar.getInstance()
        return String.format(
            "%04d-%02d-%02d",
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }
}

enum class MetricType {
    HEART_RATE, STEPS, SLEEP, SPO2, STRESS, HRV
}