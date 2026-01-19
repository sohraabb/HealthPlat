package com.bonyad.healthplat.data.repository

import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.HealthPlatApiService
import com.bonyad.healthplat.domain.model.MetricData
import com.bonyad.healthplat.domain.model.MetricRequest
import com.bonyad.healthplat.domain.model.RecordDataResult
import com.bonyad.healthplat.domain.model.SleepMetricRequest
import com.bonyad.healthplat.domain.model.SleepSummary
import com.bonyad.healthplat.domain.model.SyncHealthDataRequest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class HealthDataRepository @Inject constructor(
    private val deviceManager: HealthDeviceManager,
    private val apiService: HealthPlatApiService,
    private val userPreferences: UserPreferencesDataStore
) {

    companion object {
        private const val MAX_HISTORY_DAYS = 7 // Ring stores up to 7 days
    }

    /**
     * Syncs all missing days since last successful sync.
     *
     * Logic:
     * - First-time user (null lastSyncDate) → sync day 0 only
     * - Gap > 7 days → sync day 0 only (ring doesn't have older data)
     * - Gap 1-7 days → sync from oldest to today
     * - Same day (gap = 0) → sync day 0 (refresh today's data)
     *
     * @return RecordDataResult of day 0 (today) for UI display
     */
    suspend fun syncAllMissingDays(): RecordDataResult {
        val today = LocalDate.now()
        val todayString = today.format(DateTimeFormatter.ISO_DATE)

        val lastSyncDate = userPreferences.getLastSyncDate().first()

        Timber.i("📅 Sync check - Today: $todayString, Last sync: ${lastSyncDate ?: "never"}")

        // Calculate days to sync
        val daysToSync = calculateDaysToSync(lastSyncDate, today)

        Timber.i("📅 Days to sync: $daysToSync")

        var todayResult: RecordDataResult = RecordDataResult.Error(-1)

        // Sync from oldest to newest (so if interrupted, older data is saved)
        for (day in daysToSync.sortedDescending()) {
            Timber.i("🔄 Syncing day $day (${today.minusDays(day.toLong())})")

            val result = syncDashboardData(day)

            if (day == 0) {
                todayResult = result
            }

            when (result) {
                is RecordDataResult.Success -> {
                    Timber.i("✅ Day $day synced successfully")
                }
                is RecordDataResult.Error -> {
                    Timber.w("⚠️ Day $day sync failed: ${result.message} (code: ${result.code})")
                    // Continue with other days, don't stop on failure
                }
            }
        }

        // Update last sync date only if today (day 0) synced successfully
        if (todayResult is RecordDataResult.Success) {
            userPreferences.saveLastSyncDate(todayString)
            Timber.i("💾 Last sync date updated to: $todayString")
        }

        return todayResult
    }

    /**
     * Calculate which days need to be synced
     */
    private fun calculateDaysToSync(lastSyncDate: String?, today: LocalDate): List<Int> {
        // First-time user
        if (lastSyncDate == null) {
            Timber.i("📅 First-time user - syncing today only")
            return listOf(0)
        }

        return try {
            val lastSync = LocalDate.parse(lastSyncDate, DateTimeFormatter.ISO_DATE)
            val daysMissing = ChronoUnit.DAYS.between(lastSync, today).toInt()

            when {
                // User gone too long - ring doesn't have data that old
                daysMissing > MAX_HISTORY_DAYS -> {
                    Timber.i("📅 Gap too large ($daysMissing days) - syncing today only")
                    listOf(0)
                }

                // Same day - just refresh today
                daysMissing <= 0 -> {
                    Timber.i("📅 Same day resync")
                    listOf(0)
                }

                // Normal catch-up: sync all missing days
                else -> {
                    Timber.i("📅 Catching up $daysMissing days")
                    // daysMissing = 1 means yesterday + today = [1, 0]
                    // daysMissing = 3 means [3, 2, 1, 0]
                    (daysMissing downTo 0).toList()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error parsing last sync date: $lastSyncDate")
            listOf(0) // Fallback to today only
        }
    }

    suspend fun syncDashboardData(day: Int): RecordDataResult {
        val recordResult = deviceManager.getRecordData(day)

        if (recordResult is RecordDataResult.Success) {
            uploadHealthData(recordResult)
        }

        return recordResult
    }

    suspend fun getMetricData(
        metricType: MetricType,
        dateFrom: String,
        dateTo: String
    ): Result<List<MetricData>> {
        return try {
            val response = when (metricType) {
                MetricType.HEART_RATE -> apiService.getHeartRateMetrics(dateFrom, dateTo)
                MetricType.STEPS -> apiService.getStepsMetrics(dateFrom, dateTo)
                MetricType.SLEEP -> apiService.getSleepMetrics(dateFrom, dateTo)
                MetricType.SPO2 -> apiService.getSpo2Metrics(dateFrom, dateTo)
                MetricType.STRESS -> apiService.getStressMetrics(dateFrom, dateTo)
                MetricType.HRV -> apiService.getHrvMetrics(dateFrom, dateTo)
            }

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception(response.body()?.message ?: "API error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadHealthData(data: RecordDataResult.Success) = coroutineScope {
        val userId = userPreferences.getUserId().first()
        val deviceId = userPreferences.getDeviceId().first()

        if (userId == null || deviceId == null) {
            Timber.e("❌ Cannot sync: Missing UserId or DeviceId")
            return@coroutineScope
        }

        // Get date from SDK beans
        val sdkDateString = data.heartRate?.recordDay
            ?: data.steps?.recordDay
            ?: data.sleep?.recordDay
            ?: getCurrentDateString() // Fallback

        val time = LocalTime.now(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("HH:mm:ss"))

        val finalRecordDate = "${sdkDateString}T${time}Z"

        Timber.i("🔄 Preparing upload for $finalRecordDate")

        // ---------------------------------------------------------
        // Parallel Uploads
        // ---------------------------------------------------------

        // 1. Heart Rate
        if (data.heartRate?.heartRateSource?.any { it > 0 } == true) {
            launch {
                uploadMetric(
                    userId, deviceId, finalRecordDate,
                    data.heartRate.heartRateSource, MetricType.HEART_RATE
                )
            }
        }

        // 2. Steps
        if (data.steps?.stepSource?.any { it > 0 } == true) {
            launch {
                uploadMetric(
                    userId, deviceId, finalRecordDate,
                    data.steps.stepSource, MetricType.STEPS
                )
            }
        }

        // 3. Sleep
        if (data.sleep?.sourceList?.any { it > 0 } == true) {
            launch {
                uploadMetric(
                    userId, deviceId, finalRecordDate,
                    data.sleep.sourceList, MetricType.SLEEP
                )
            }
        }

        // 4. SpO2
        if (data.spo2?.sourceList?.any { it > 0 } == true) {
            launch {
                uploadMetric(
                    userId, deviceId, finalRecordDate,
                    data.spo2.sourceList, MetricType.SPO2
                )
            }
        }

        // 5. Stress
        if (data.stress?.stressSource?.any { it > 0 } == true) {
            launch {
                uploadMetric(
                    userId, deviceId, finalRecordDate,
                    data.stress.stressSource, MetricType.STRESS
                )
            }
        }

        // 6. HRV
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
            val response = when (type) {
                MetricType.SLEEP -> {
                    val summary = calculateSleepSummary(values)

                    val sleepRequest = SleepMetricRequest(
                        userId = userId,
                        deviceId = deviceId,
                        recordDate = recordDate,
                        values = values,
                        sleepSummary = summary
                    )
                    apiService.uploadSleep(sleepRequest)
                }
                else -> {
                    val request = MetricRequest(
                        userId = userId,
                        deviceId = deviceId,
                        recordDate = recordDate,
                        values = values
                    )
                    when (type) {
                        MetricType.HEART_RATE -> apiService.uploadHeartRate(request)
                        MetricType.STEPS -> apiService.uploadSteps(request)
                        MetricType.SPO2 -> apiService.uploadSpo2(request)
                        MetricType.STRESS -> apiService.uploadStress(request)
                        MetricType.HRV -> apiService.uploadHrv(request)
                        else -> throw IllegalArgumentException("Unknown metric type")
                    }
                }
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

private fun calculateSleepSummary(values: List<Int>): SleepSummary {
    val deep = values.count { it == 1 }
    val light = values.count { it == 2 }
    val awake = values.count { it == 3 }
    val rem = values.count { it == 4 }

    val total = deep + light + rem

    val sleepQuality = if (total > 0) {
        val restorativeRatio = (deep + rem).toFloat() / total.toFloat()
        (restorativeRatio * 200).coerceAtMost(100f).toInt()
    } else {
        0
    }

    return SleepSummary(
        deepSleepMinutes = deep,
        lightSleepMinutes = light,
        remSleepMinutes = rem,
        awakeMinutes = awake,
        totalSleepMinutes = total,
        sleepQuality = sleepQuality
    )
}

enum class MetricType {
    HEART_RATE, STEPS, SLEEP, SPO2, STRESS, HRV
}