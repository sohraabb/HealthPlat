package com.bonyad.healthplat.di

import android.content.Context
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.blesdk.manager.RingDeviceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BleModule {
    @Provides
    @Singleton
    fun provideHealthDeviceManager(@ApplicationContext context: Context): HealthDeviceManager {
        return RingDeviceManager(context)

    }
}