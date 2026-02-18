package com.bonyad.healthplat.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages background health data synchronization using WorkManager.
 *
 * Schedules periodic sync every 15 minutes when:
 * - Device has network connection
 * - Battery is not low (optional constraint)
 */
@Singleton
class HealthSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val workManager = WorkManager.getInstance(context)

    /**
     * Start periodic background sync.
     *
     * @param intervalMinutes Sync interval (minimum 15 minutes per Android restrictions)
     * @param requireCharging Whether to sync only when charging (default: false)
     * @param requireBatteryNotLow Whether to require battery not low (default: true)
     */
    fun startPeriodicSync(
        intervalMinutes: Long = 15,
        requireCharging: Boolean = false,
        requireBatteryNotLow: Boolean = true
    ) {
        // Ensure minimum 15 minutes (WorkManager restriction)
        val actualInterval = intervalMinutes.coerceAtLeast(15)

        if (intervalMinutes < 15) {
            Timber.w("⚠️ Requested interval $intervalMinutes min is too short. Using 15 min (WorkManager minimum)")
        }

        // Configure constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(requireCharging)
            .setRequiresBatteryNotLow(requireBatteryNotLow)
            .build()

        // Create periodic work request
        val syncWorkRequest = PeriodicWorkRequestBuilder<HealthSyncWorker>(
            repeatInterval = actualInterval,
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
            flexTimeInterval = 5,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(TAG_HEALTH_SYNC)
            .build()

        // Schedule work (replace existing if any)
        workManager.enqueueUniquePeriodicWork(
            HealthSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )

        Timber.i("✅ [BackgroundSync] Scheduled periodic sync every $actualInterval minutes")
    }

    /**
     * Stop background sync.
     */
    fun stopPeriodicSync() {
        workManager.cancelUniqueWork(HealthSyncWorker.WORK_NAME)
        Timber.i("🛑 [BackgroundSync] Periodic sync stopped")
    }

    /**
     * Check if background sync is currently scheduled.
     */
    fun isSyncScheduled(): Boolean {
        val workInfos = workManager.getWorkInfosForUniqueWork(HealthSyncWorker.WORK_NAME)
            .get() // Blocking call, use in background thread if needed

        return workInfos.any { !it.state.isFinished }
    }

    /**
     * Trigger immediate sync (useful for testing or manual sync button).
     */
    fun triggerImmediateSync() {
        // OneTime work request for immediate execution
        val immediateWorkRequest = androidx.work.OneTimeWorkRequestBuilder<HealthSyncWorker>()
            .addTag(TAG_HEALTH_SYNC)
            .build()

        workManager.enqueue(immediateWorkRequest)
        Timber.i("🔄 [BackgroundSync] Immediate sync triggered")
    }

    companion object {
        private const val TAG_HEALTH_SYNC = "health_sync"
    }
}