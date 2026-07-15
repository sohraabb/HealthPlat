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
    private val tokenManager: TokenManager,
    private val userPreferences: UserPreferencesDataStore
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath
        val responseCode = response.code

        Timber.w("🚨 AUTHENTICATOR: 401 for path=$path, code=$responseCode")

        // Prevent infinite loop
        if (response.request.header("Token-Refresh-Attempted") != null) {
            Timber.e("🛑 AUTHENTICATOR: Already attempted refresh for this request, giving up")
            return null
        }

        Timber.i("🔄 AUTHENTICATOR: Attempting token refresh...")

        // Synchronously refresh token
        val success = runBlocking {
            tokenManager.ensureValidToken()
        }

        Timber.d("🔐 AUTHENTICATOR: Refresh result=$success")

        if (!success) {
            Timber.e("❌ AUTHENTICATOR: Token refresh failed, request will fail")
            return null
        }

        // Get new token and retry request
        val newToken = runBlocking {
            userPreferences.getAuthToken().first()
        }

        Timber.d("🔐 AUTHENTICATOR: New token exists=${!newToken.isNullOrEmpty()}")

        if (newToken.isNullOrEmpty()) {
            Timber.e("❌ AUTHENTICATOR: No token available after refresh")
            return null
        }

        Timber.i("✅ AUTHENTICATOR: Retrying request with new token for path=$path")

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .header("Token-Refresh-Attempted", "true")
            .build()
    }
}