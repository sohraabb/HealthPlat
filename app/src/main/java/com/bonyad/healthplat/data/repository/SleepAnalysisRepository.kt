package com.bonyad.healthplat.data.repository

import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.SleepAnalysisApiService
import com.bonyad.healthplat.domain.model.SleepAnalysisData
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepAnalysisRepository @Inject constructor(
    private val sleepAnalysisApi: SleepAnalysisApiService,
    private val userPreferences: UserPreferencesDataStore
) {

    /**
     * Fetches sleep analysis for a given date using the logged-in user's ID.
     * @param date ISO date string (yyyy-MM-dd)
     */
    suspend fun getSleepAnalysis(date: String): Result<SleepAnalysisData> {
        val userId = userPreferences.getUserId().first()
            ?: return Result.failure(Exception("User ID not found"))

        return getSleepAnalysisForUser(userId, date)
    }

    /**
     * Fetches sleep analysis for a given date using an explicit user ID.
     * Used by caregiver mode to fetch a patient's sleep analysis.
     * @param userId the target user's ID
     * @param date ISO date string (yyyy-MM-dd)
     */
    suspend fun getSleepAnalysisForUser(userId: String, date: String): Result<SleepAnalysisData> {
        return try {
            val response = sleepAnalysisApi.getSleepAnalysis(userId, date)
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val data = response.body()?.data
                if (data != null) {
                    Result.success(data)
                } else {
                    Result.failure(Exception("No sleep analysis data"))
                }
            } else {
                val errorMsg = response.body()?.errors?.message
                    ?: "Sleep analysis API error: ${response.code()}"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Timber.e(e, "Sleep analysis API call failed")
            Result.failure(e)
        }
    }
}
