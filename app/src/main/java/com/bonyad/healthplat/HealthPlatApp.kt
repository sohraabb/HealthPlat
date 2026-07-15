package com.bonyad.healthplat

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.bonlala.bonlalable.BonlalaOperateManager
import com.bonyad.healthplat.logging.FileLoggingTree
import com.bonyad.healthplat.logging.LogFiles
import com.bonyad.healthplat.worker.HealthSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import no.nordicsemi.android.dfu.BuildConfig
import timber.log.Timber
import javax.inject.Inject


@HiltAndroidApp
class HealthPlatApp: Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    @Inject
    lateinit var healthSyncScheduler: HealthSyncScheduler

    override fun onCreate() {
        super.onCreate()

//        if (BuildConfig.DEBUG)
        Timber.plant(Timber.DebugTree())
        // Persist logs to a file (esp. for the BLE pairing flow).
        Timber.plant(FileLoggingTree(this))
        // Mirror the previous session's log into Downloads on launch — safety net in
        // case the app was force-closed before the connection screen could export it.
        LogFiles.exportToDownloads(this)

        // Initialize Ring SDK
        try {
            BonlalaOperateManager.getInstance().initContext(this)
            Timber.i("Ring SDK Initilized")
        } catch (t: Throwable) {
            Timber.e(t, "Failed to init Ring SDK")
        }

        try {
            healthSyncScheduler.startPeriodicSync(
                intervalMinutes = 15,
                requireCharging = false,        // Sync even when not charging
                requireBatteryNotLow = true     // Only sync if battery not low
            )
            Timber.i("✅ Background sync scheduled")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to schedule background sync")
        }

    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR)
            .build()
}