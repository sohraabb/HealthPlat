package com.bonyad.healthplat

import android.app.Application
import com.bonlala.bonlalable.BonlalaOperateManager
import dagger.hilt.android.HiltAndroidApp
import no.nordicsemi.android.dfu.BuildConfig
import timber.log.Timber


@HiltAndroidApp
class HealthPlatApp: Application() {

    override fun onCreate() {
        super.onCreate()

//        if (BuildConfig.DEBUG)
        Timber.plant(Timber.DebugTree())

        // Initialize Ring SDK
        try {
            BonlalaOperateManager.getInstance().initContext(this)
            Timber.i("Ring SDK Initilized")
        } catch (t: Throwable) {
            Timber.e(t, "Failed to init Ring SDK")
        }
    }
}