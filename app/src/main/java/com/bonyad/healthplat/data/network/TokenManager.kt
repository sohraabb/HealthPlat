package com.bonyad.healthplat.data.network

import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val userPreferences: UserPreferencesDataStore,
    @Named("RefreshApiService") private val refreshApi: HealthPlatApiService
) {
    private val refreshMutex = Mutex()

    /**
     * Check if token is expired or will expire soon (within 2 minutes)
     * Returns TRUE if token needs refresh
     */
    suspend fun isTokenExpiringSoon(): Boolean {
        val expiryMillis = userPreferences.getAccessTokenExpiry().first()
        val now = System.currentTimeMillis()

        Timber.d("🔍 TOKEN CHECK: expiryMillis=$expiryMillis, now=$now")

        if (expiryMillis == null) {
            Timber.w("⚠️ TOKEN CHECK: No expiry found, consider expired")
            return true
        }

        val timeUntilExpiry = expiryMillis - now
        val minutesUntilExpiry = timeUntilExpiry / 60000.0

        // Token needs refresh if:
        // 1. Already expired (timeUntilExpiry < 0)
        // 2. OR will expire in less than 2 minutes
        val bufferMillis = 2 * 60 * 1000
        val needsRefresh = timeUntilExpiry < bufferMillis

        if (timeUntilExpiry < 0) {
            Timber.w("⚠️ TOKEN CHECK: Token ALREADY EXPIRED by %.1f minutes", -minutesUntilExpiry)
        } else {
            Timber.d("🔍 TOKEN CHECK: Token expires in %.2f minutes", minutesUntilExpiry)
        }

        Timber.d("🔍 TOKEN CHECK: needsRefresh=$needsRefresh")

        return needsRefresh
    }

    /**
     * Refresh token if needed. Thread-safe with mutex.
     * @return true if tokens are valid/refreshed successfully
     */
    suspend fun ensureValidToken(): Boolean = refreshMutex.withLock {
        try {
            Timber.d("🔐 REFRESH: Acquired mutex lock")

            val accessToken = userPreferences.getAuthToken().first()
            val refreshToken = userPreferences.getRefreshToken().first()

            Timber.d("🔐 REFRESH: accessToken exists=${!accessToken.isNullOrEmpty()}, refreshToken exists=${!refreshToken.isNullOrEmpty()}")

            if (accessToken.isNullOrEmpty() || refreshToken.isNullOrEmpty()) {
                Timber.e("❌ REFRESH: No tokens available")
                return false
            }

            // Check if token is expiring soon OR already expired
            val isExpiring = isTokenExpiringSoon()

            if (!isExpiring) {
                Timber.i("✅ REFRESH: Token still valid, no refresh needed")
                return true
            }

            Timber.i("🔄 REFRESH: Token expired or expiring soon, calling refresh API...")

            val response = refreshApi.refreshToken(
                expiredTokenWithBearer = "Bearer $accessToken",
                accessToken = accessToken,
                refreshToken = refreshToken
            )

            Timber.d("🔐 REFRESH: API response code=${response.code()}, isSuccessful=${response.isSuccessful}")

            val rawBody = response.body()
            Timber.d("🔐 REFRESH: rawBody is null? ${rawBody == null}")
            if (rawBody != null) {
                Timber.d("🔐 REFRESH: rawBody.isSuccess=${rawBody.isSuccess}")
                Timber.d("🔐 REFRESH: rawBody.message=${rawBody.message}")
                Timber.d("🔐 REFRESH: rawBody.data is null? ${rawBody.data == null}")

                if (rawBody.data != null) {
                    Timber.d("🔐 REFRESH: data.accessToken exists=${!rawBody.data.accessToken.isNullOrEmpty()}")
                    Timber.d("🔐 REFRESH: data.refreshToken exists=${!rawBody.data.refreshToken.isNullOrEmpty()}")
                    Timber.d("🔐 REFRESH: data.expDate=${rawBody.data.expDate}")
                }
            }
            val errorBodyString = response.errorBody()?.string()
            if (errorBodyString != null) {
                Timber.e("🔐 REFRESH: errorBody=$errorBodyString")
            }
            Timber.d("🔐 REFRESH: body=$rawBody")
            Timber.d("🔐 REFRESH: body?.isSuccess=${rawBody?.isSuccess}")
            Timber.d("🔐 REFRESH: body?.data=${rawBody?.data}")


            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val data = response.body()!!.data!!

                Timber.d("🔐 REFRESH: Saving new tokens...")

                userPreferences.saveTokens(
                    data.accessToken,
                    data.refreshToken,
                    data.expDate!!
                )

                logTokenExpiry(data.accessToken)
                Timber.i("✅ REFRESH: Token refreshed successfully")
                return true
            } else {
                val errorBody = response.errorBody()?.string()
                Timber.e("❌ REFRESH: Failed - code=${response.code()}, body=$errorBody")
                return false
            }

        } catch (e: Exception) {
            Timber.e(e, "❌ REFRESH: Exception occurred")
            return false
        }
    }

    /**
     * Check if refresh token is expired (older than 30 days)
     */
    suspend fun isRefreshTokenExpired(): Boolean {
        val expiryMillis = userPreferences.getAccessTokenExpiry().first()
        val now = System.currentTimeMillis()

        Timber.d("🔍 REFRESH TOKEN CHECK: expiryMillis=$expiryMillis, now=$now")

        if (expiryMillis == null) {
            Timber.w("⚠️ REFRESH TOKEN CHECK: No expiry found")
            return true
        }

        // If expiry is more than 30 days old, refresh token is likely expired
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val ageMs = now - expiryMillis
        val ageDays = ageMs / (24 * 60 * 60 * 1000.0)

        val isExpired = ageMs > thirtyDaysMs

        Timber.d("🔍 REFRESH TOKEN CHECK: ageDays=%.1f, isExpired=$isExpired", ageDays)

        return isExpired
    }

    private fun logTokenExpiry(token: String) {
        try {
            val parts = token.split(".")
            if (parts.size < 2) return

            val payload = String(
                android.util.Base64.decode(
                    parts[1],
                    android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
                )
            )
            val json = org.json.JSONObject(payload)
            val exp = json.getLong("exp")
            val now = System.currentTimeMillis() / 1000
            val minutesUntilExpiry = (exp - now) / 60.0

            Timber.d("⏰ New token expires in: %.1f minutes", minutesUntilExpiry)
        } catch (e: Exception) {
            Timber.w("Could not parse token expiration")
        }
    }
}