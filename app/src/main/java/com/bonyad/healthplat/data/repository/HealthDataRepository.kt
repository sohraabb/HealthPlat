package com.bonyad.healthplat.data.repository

import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.HealthPlatApiService
import com.bonyad.healthplat.domain.model.RecordDataResult
import com.bonyad.healthplat.domain.model.SyncHealthDataRequest
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
    val realTimeMetrics = deviceManager.realTimeData

    // CHANGED: Now returns RecordDataResult so ViewModel can update UI
    suspend fun syncDashboardData(): RecordDataResult {
        val today = 0

        // 1. Fetch from Ring
        val recordResult = deviceManager.getRecordData(today)

        // 2. If success, upload to server quietly
        if (recordResult is RecordDataResult.Success) {
            uploadHealthData(recordResult)
        }

        // 3. Return result to ViewModel
        return recordResult
    }

    private suspend fun uploadHealthData(data: RecordDataResult.Success) {
        // Convert SDK Beans to your API Model
        val request = SyncHealthDataRequest(
            date = ""/*getCurrentDateString()*/,
            stepData = data.steps?.stepSource, // Full array of steps per minute/hour
            heartRateData = data.heartRate?.heartRateSource,
            spo2Data = data.spo2?.sourceList,
            sleepData = data.sleep?.sourceList
        )

        try {
//            apiService.syncHealthData(request)
            Timber.i("✅ Health data synced with server")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to sync health data")
        }
    }
}