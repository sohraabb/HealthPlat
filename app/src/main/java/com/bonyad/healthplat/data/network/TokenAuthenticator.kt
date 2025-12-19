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

class TokenAuthenticator(
    private val userPreferences: UserPreferencesDataStore,
    private val apiService: HealthPlatApiService
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // 🔍 DEBUG LOG: Use this to confirm the method is hit
        Timber.e("🚨 Authenticator HIT! Response code: ${response.code} for path: ${response.request.url.encodedPath}")

        // 1. Infinite Loop Prevention
        if (response.request.header("Token-Refresh-Attempted") != null) {
            Timber.e("🛑 Authenticator: Already tried refreshing, giving up.")
            return null
        }

        // 2. Synchronized Refresh Logic
        return runBlocking {
            val accessToken = userPreferences.getAuthToken().first()
            val refreshToken = userPreferences.getRefreshToken().first()

            if (accessToken.isNullOrEmpty() || refreshToken.isNullOrEmpty()) {
                Timber.w("⚠️ Authenticator: No tokens found in storage.")
                return@runBlocking null
            }

            // 🔍 DEBUG LOG
            Timber.d("🔄 Authenticator: Calling Refresh API...")

            // 3. The Refresh Call (Wrapped in Try/Catch is safer)
            val newTokens = try {
                val refreshResponse = apiService.refreshToken(
                    expiredTokenWithBearer = "Bearer $accessToken", // Needs Header
                    accessToken = accessToken, // Needs Query
                    refreshToken = refreshToken // Needs Query
                )

                if (refreshResponse.isSuccessful && refreshResponse.body()?.isSuccess == true) {
                    refreshResponse.body()?.data
                } else {
                    Timber.e("❌ Refresh API Failed: Code=${refreshResponse.code()}, Msg=${refreshResponse.message()}")
                    null
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Refresh API Exception")
                null
            }

            // 4. Handle Result
            if (newTokens != null) {
                val access = newTokens.accessToken
                val refresh = newTokens.refreshToken
                val exp = newTokens.expDate

                if (access.isNullOrEmpty() || refresh.isNullOrEmpty() || exp.isNullOrEmpty()) {
                    Timber.e("❌ Authenticator received invalid token data")
                    return@runBlocking null
                }

                userPreferences.saveTokens(access, refresh, exp)

                response.request.newBuilder()
                    .header("Authorization", "Bearer $access")
                    .header("Token-Refresh-Attempted", "true")
                    .build()
            } else {
                Timber.e("🚫 Authenticator: Failed to refresh")
                null
            }
        }
    }
}