package com.bonyad.healthplat

import android.app.Application
import no.nordicsemi.android.dfu.BuildConfig

class HealthPlatApp: Application {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG)
            Timber.plant(Timber.DebugTree)
    }
}