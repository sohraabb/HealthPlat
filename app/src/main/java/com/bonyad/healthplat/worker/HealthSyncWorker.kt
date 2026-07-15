package com.bonyad.healthplat.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.blesdk.model.ConnectionState
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.repository.HealthDataRepository
import com.bonyad.healthplat.domain.model.RecordDataResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Background worker that syncs health data from ring device to server.
 *
 * Runs every 15 minutes to:
 * - Check device connection
 * - Get today's health data (with corrected sleep!)
 * - Upload to server
 *
 * Uses Hilt for dependency injection.
 */
@HiltWorker
class HealthSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val deviceManager: HealthDeviceManager,
    private val healthRepository: HealthDataRepository,
    private val userPreferences: UserPreferencesDataStore
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Timber.i("🔄 [BackgroundSync] Worker started")

        return try {
            // Check if device is connected
            val connectionState = deviceManager.connectionState.first()

            if (connectionState != ConnectionState.CONNECTED) {
                Timber.w("⚠️ [BackgroundSync] Device not connected, skipping sync")
                // Return success - this is not a failure, just no device
                return Result.success()
            }

            Timber.i("📱 [BackgroundSync] Device connected, syncing data...")

            // Sync today's data (offset = 0)
            val (result, serverTime) = healthRepository.syncDashboardData(day = 0)

            when (result) {
                is RecordDataResult.Success -> {
                    val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                    userPreferences.saveLastSyncDate(today, serverTime)
                    Timber.i("✅ [BackgroundSync] Sync successful (server time: $serverTime)")
                    Result.success()
                }
                is RecordDataResult.Error -> {
                    Timber.w("⚠️ [BackgroundSync] Sync failed: ${result.message}")
                    Result.retry()
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "❌ [BackgroundSync] Exception during sync")
            // Retry on exception
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "health_sync_work"
    }
}