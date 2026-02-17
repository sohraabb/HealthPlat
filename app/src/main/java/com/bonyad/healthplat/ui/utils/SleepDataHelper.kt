package com.bonyad.healthplat.ui.utils

import com.bonlala.bonlalable.bean.RecordSleepBean
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.domain.model.RecordDataResult
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepDataHelper @Inject constructor(
    private val deviceManager: HealthDeviceManager
) {

    /**
     * Get complete sleep data for a day, including pre-midnight portion.
     *
     * @param offset Day offset (0 = today, -1 = yesterday)
     * @return Complete sleep data list, or empty if no data
     */
    suspend fun getFullSleepData(offset: Int): List<Int> {
        try {
            // Get today's data
            val todayResult = deviceManager.getRecordData(offset)

            if (todayResult !is RecordDataResult.Success) {
                return emptyList()
            }

            val todaySleep = todayResult.sleep?.sourceList ?: emptyList()

            if (todaySleep.isEmpty()) {
                Timber.d("No sleep data for offset $offset")
                return emptyList()
            }

            // Check if today starts with sleep (first 30 minutes)
            // Values: 1=Deep, 2=Light, 3=Awake, 4=REM, 0=Activity
            val startsWithSleep = todaySleep.take(30).any { it != 0 }

            if (!startsWithSleep) {
                // Sleep within single day - no combination needed
                Timber.d("✅ Sleep contained in single day (offset=$offset)")
                return todaySleep
            }

            Timber.d("🌙 Sleep starts at midnight, checking previous day...")

            // Get yesterday's data
            val yesterdayResult = deviceManager.getRecordData(offset - 1)

            if (yesterdayResult !is RecordDataResult.Success) {
                Timber.w("⚠️ No previous day data, using current day only")
                return todaySleep
            }

            val yesterdaySleep = yesterdayResult.sleep?.sourceList ?: emptyList()

            if (yesterdaySleep.isEmpty()) {
                Timber.d("Previous day empty, using current day only")
                return todaySleep
            }

            // Find LAST continuous sleep block from yesterday
            // Walk backwards from 23:59 (index 1439)
            // This ignores earlier naps, only gets night sleep
            var lastSleepBlockStart = yesterdaySleep.size

            for (i in yesterdaySleep.indices.reversed()) {
                if (yesterdaySleep[i] != 0) {
                    lastSleepBlockStart = i
                } else {
                    // Hit activity - sleep block ended
                    break
                }
            }

            // Combine if valid block found
            if (lastSleepBlockStart < yesterdaySleep.size) {
                val preMidnight = yesterdaySleep.subList(lastSleepBlockStart, yesterdaySleep.size)

                val startHour = lastSleepBlockStart / 60
                val startMin = lastSleepBlockStart % 60

                Timber.i("🌙 Combined sleep (offset=$offset):")
                Timber.i("   📍 Started: ${String.format("%02d:%02d", startHour, startMin)} previous day")
                Timber.i("   📊 Pre-midnight: ${preMidnight.size} min")
                Timber.i("   📊 Post-midnight: ${todaySleep.size} min")
                Timber.i("   📊 Total: ${preMidnight.size + todaySleep.size} min")

                return preMidnight + todaySleep
            } else {
                Timber.d("No valid sleep block at end of previous day")
                return todaySleep
            }

        } catch (e: Exception) {
            Timber.e(e, "❌ Error getting full sleep data")
            return emptyList()
        }
    }

    /**
     * Get RecordDataResult with corrected sleep data.
     * Use when you need all metrics + corrected sleep.
     */
    suspend fun getRecordDataWithFullSleep(offset: Int): RecordDataResult {
        val originalResult = deviceManager.getRecordData(offset)

        if (originalResult !is RecordDataResult.Success) {
            return originalResult
        }

        // Get complete sleep
        val fullSleepData = getFullSleepData(offset)

        // If unchanged, return original
        if (fullSleepData == originalResult.sleep?.sourceList) {
            return originalResult
        }

        // Create new RecordSleepBean with corrected data
        val correctedSleep = RecordSleepBean().apply {
            sourceList = fullSleepData
            recordDay = originalResult.sleep?.recordDay
        }

        // Use copy() since Success is a data class
        return originalResult.copy(sleep = correctedSleep)
    }
}