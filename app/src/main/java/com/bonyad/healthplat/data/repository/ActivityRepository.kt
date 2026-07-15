package com.bonyad.healthplat.data.repository

import com.bonyad.healthplat.data.network.HealthPlatApiService
import com.bonyad.healthplat.domain.model.ActivityFact
import com.bonyad.healthplat.domain.model.CreateActivityRequest
import com.bonyad.healthplat.domain.model.UpdateActivityRequest
import com.bonyad.healthplat.domain.model.UserActivity
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepository @Inject constructor(
    private val api: HealthPlatApiService
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Search activity facts by name with server-side pagination.
     * Returns up to [pageSize] results per call. Increment [page] for infinite scroll.
     *
     * Note: The same activity name can appear multiple times in the list with
     * different age ranges — the server returns the most relevant match for the
     * user's age. fact.cal is the activity's MET value; burned calories use the
     * MET formula: MET × 3.5 × weightKg × durationMin / 200
     */
    suspend fun searchActivityFacts(
        query: String,
        page: Int = 1,
        pageSize: Int = 20
    ): AuthResult<List<ActivityFact>> {
        return try {
            val response = api.searchActivityFacts(query, page, pageSize)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.isSuccess == true) {
                    Timber.d("✅ Searched '$query' page $page → ${body.data?.size} results")
                    AuthResult.Success(body.data ?: emptyList())
                } else {
                    val error = body?.errors?.firstOrNull() ?: "خطا در جستجو"
                    Timber.w("⚠️ searchActivityFacts error: $error")
                    AuthResult.Error(error)
                }
            } else {
                Timber.e("❌ searchActivityFacts HTTP ${response.code()}")
                AuthResult.Error("خطا در جستجو")
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ searchActivityFacts exception")
            AuthResult.Error("خطا در اتصال به سرور")
        }
    }

    /**
     * Get all logged activities for a specific date.
     * Passes the same date string for both dateFrom and dateTo.
     *
     * Note: Confirm with backend whether single-date queries work with
     * identical dateFrom/dateTo or if T00:00:00/T23:59:59 suffixes are needed.
     */
    suspend fun getActivitiesForDate(date: Date): AuthResult<List<UserActivity>> {
        val dateStr = dateFormat.format(date)
        return try {
            val response = api.getActivityByDate(dateStr, dateStr)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.isSuccess == true) {
                    Timber.d("✅ Got ${body.data?.size} activities for $dateStr")
                    AuthResult.Success(body.data ?: emptyList())
                } else {
                    val error = body?.errors?.firstOrNull() ?: "خطا در دریافت فعالیت‌ها"
                    Timber.w("⚠️ getActivitiesForDate error: $error")
                    AuthResult.Error(error)
                }
            } else {
                Timber.e("❌ getActivitiesForDate HTTP ${response.code()}")
                AuthResult.Error("خطا در دریافت فعالیت‌ها")
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ getActivitiesForDate exception")
            AuthResult.Error("خطا در اتصال به سرور")
        }
    }

    /**
     * Log a new activity for today.
     * [activityCal] must be pre-calculated via the MET formula:
     * fact.cal (MET) × 3.5 × weightKg × durationMin / 200
     */
    suspend fun createActivity(
        fact: ActivityFact,
        durationHours: Double,
        activityCal: Double
    ): AuthResult<UserActivity> {
        return try {
            val request = CreateActivityRequest(
                activityFactId = fact.id,
                activityName = fact.name,
                activityOrgName = fact.orgName,
                duration = durationHours,
                activityCal = activityCal
            )
            val response = api.createActivity(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.isSuccess == true && body.data != null) {
                    Timber.i("✅ Activity logged: ${fact.name} for ${durationHours}h (${activityCal} kcal)")
                    AuthResult.Success(body.data)
                } else {
                    val error = body?.errors?.firstOrNull() ?: "خطا در ثبت فعالیت"
                    Timber.w("⚠️ createActivity error: $error")
                    AuthResult.Error(error)
                }
            } else {
                Timber.e("❌ createActivity HTTP ${response.code()}")
                AuthResult.Error("خطا در ثبت فعالیت")
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ createActivity exception")
            AuthResult.Error("خطا در اتصال به سرور")
        }
    }

    /**
     * Update an existing logged activity.
     */
    suspend fun updateActivity(request: UpdateActivityRequest): AuthResult<Unit> {
        return try {
            val response = api.updateActivity(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.isSuccess == true) {
                    Timber.i("✅ Activity updated: id=${request.id}")
                    AuthResult.Success(Unit)
                } else {
                    val error = body?.errors?.firstOrNull() ?: "خطا در ویرایش فعالیت"
                    Timber.w("⚠️ updateActivity error: $error")
                    AuthResult.Error(error)
                }
            } else {
                Timber.e("❌ updateActivity HTTP ${response.code()}")
                AuthResult.Error("خطا در ویرایش فعالیت")
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ updateActivity exception")
            AuthResult.Error("خطا در اتصال به سرور")
        }
    }

    /**
     * Delete a logged activity by ID.
     */
    suspend fun deleteActivity(id: Int): AuthResult<Unit> {
        return try {
            val response = api.deleteActivity(id)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.isSuccess == true) {
                    Timber.i("✅ Activity deleted: id=$id")
                    AuthResult.Success(Unit)
                } else {
                    val error = body?.errors?.firstOrNull() ?: "خطا در حذف فعالیت"
                    Timber.w("⚠️ deleteActivity error: $error")
                    AuthResult.Error(error)
                }
            } else {
                Timber.e("❌ deleteActivity HTTP ${response.code()}")
                AuthResult.Error("خطا در حذف فعالیت")
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ deleteActivity exception")
            AuthResult.Error("خطا در اتصال به سرور")
        }
    }
}
