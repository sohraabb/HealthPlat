package com.bonyad.healthplat.data.network

import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.domain.model.RefreshTokenRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class TokenAuthenticator @Inject constructor(
    private val userPreferences: UserPreferencesDataStore,
    private val apiService: HealthPlatApiService
) : Authenticator {

    private val refreshLock = Any()
    private var refreshInProgress = false
    private var lastRefreshTime = 0L
    private val MIN_REFRESH_INTERVAL = 5000L

    override fun authenticate(route: Route?, response: Response): Request? {

        if (response.request.header("Token-Refresh-Attempted") != null) {
            Timber.e("♻️ Already tried refresh, giving up")
            clearSession()
            return null
        }

        val path = response.request.url.encodedPath

        if (path.contains("/Auth/login", true) ||
            path.contains("/Auth/register", true) ||
            path.contains("/Auth/verify", true)) {
            Timber.d("🔵 Auth endpoint, not refreshing")
            return null
        }

        val authHeader = response.request.header("Authorization")
        if (authHeader == null) {
            Timber.w("⚪ No Authorization header, cannot refresh (user needs to login)")
            clearSession()
            return null
        }

        synchronized(refreshLock) {
            val now = System.currentTimeMillis()

            if (refreshInProgress && (now - lastRefreshTime) < MIN_REFRESH_INTERVAL) {
                Timber.w("⏳ Refresh in progress, skipping duplicate attempt")
                return null
            }

            refreshInProgress = true
            lastRefreshTime = now
        }

        return try {
            runBlocking {
                val accessToken = userPreferences.getAuthToken().first()
                val refreshToken = userPreferences.getRefreshToken().first()

                Timber.d("🔄 Attempting token refresh for: $path")
                Timber.d("   Current Access: ${accessToken?.take(30)}...")
                Timber.d("   Current Refresh: ${refreshToken?.take(30)}...")

                if (refreshToken.isNullOrEmpty()) {
                    Timber.e("❌ No refresh token available, clearing session")
                    clearSession()
                    return@runBlocking null
                }

                // ✅ FIX: Use query parameters instead of body
                try {
                    val refreshResponse = apiService.refreshToken(
                        accessToken = accessToken ?: "",
                        refreshToken = refreshToken
                    )

                    when {
                        refreshResponse.isSuccessful &&
                                refreshResponse.body()?.isSuccess == true &&
                                refreshResponse.body()?.data != null -> {

                            val responseData = refreshResponse.body()!!.data!!
                            val newAccessToken = responseData.accessToken
                            val newRefreshToken = responseData.refreshToken

                            Timber.i("✅ Token refresh successful")
                            Timber.d("   New Access: ${newAccessToken.take(30)}...")
                            Timber.d("   New Refresh: ${newRefreshToken.take(30)}...")

                            // Save new tokens
                            userPreferences.saveAuthToken(newAccessToken)
                            userPreferences.saveRefreshToken(newRefreshToken)

                            // Also save expiry if available
                            responseData.expDate?.let { expDate ->
                                Timber.d("   Expires: $expDate")
                                // You can parse and save this if needed
                            }

                            // Retry original request with new token
                            response.request.newBuilder()
                                .header("Authorization", "Bearer $newAccessToken")
                                .header("Token-Refresh-Attempted", "true")
                                .build()
                        }

                        refreshResponse.code() == 401 -> {
                            Timber.e("❌ Refresh token expired (401), clearing session")
                            clearSession()
                            null
                        }

                        else -> {
                            val errorCode = refreshResponse.code()
                            val errorBody = refreshResponse.errorBody()?.string()
                            val errorMessage = refreshResponse.body()?.message ?: "Unknown error"

                            Timber.e("❌ Refresh failed: $errorCode - $errorMessage")
                            Timber.e("   Error body: $errorBody")

                            if (errorCode in 400..499) {
                                clearSession()
                            }
                            null
                        }
                    }
                } catch (e: IOException) {
                    Timber.e(e, "❌ Network error during token refresh")
                    null
                } catch (e: Exception) {
                    Timber.e(e, "❌ Unexpected error during token refresh")
                    clearSession()
                    null
                }
            }
        } finally {
            synchronized(refreshLock) {
                refreshInProgress = false
            }
        }
    }

    private fun clearSession() = runBlocking {
        Timber.w("🗑️ Clearing auth session")
        userPreferences.clearAuthOnly()
    }
}