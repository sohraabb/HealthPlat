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
import javax.inject.Inject

class TokenAuthenticator @Inject constructor(
    private val userPreferences: UserPreferencesDataStore,
    private val apiService: HealthPlatApiService
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {

        // Prevent infinite loop
        if (response.request.header("Token-Refresh-Attempted") != null) {
            clearSession()
            return null
        }

        // Only refresh if request had bearer token
        if (response.request.header("Authorization") == null) {
            return null
        }

        // Don't refresh for login/register endpoints
        val path = response.request.url.encodedPath
        if (path.contains("/Auth/", true)) {
            return null
        }

        return runBlocking {
            val access = userPreferences.getAuthToken().first()
            val refresh = userPreferences.getRefreshToken().first()

            if (access.isNullOrEmpty() || refresh.isNullOrEmpty()) {
                clearSession()
                return@runBlocking null
            }

            val refreshResponse = apiService.refreshToken(
                RefreshTokenRequest(access, refresh)
            )

            if (refreshResponse.isSuccessful &&
                refreshResponse.body()?.data != null
            ) {
                val newAccessToken = refreshResponse.body()!!.data!!.accessToken
                val newRefreshToken = refreshResponse.body()!!.data!!.refreshToken

                userPreferences.saveAuthToken(newAccessToken)
                userPreferences.saveRefreshToken(newRefreshToken)

                return@runBlocking response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .header("Token-Refresh-Attempted", "true")
                    .build()
            }

            clearSession()
            null
        }
    }

    private fun clearSession() = runBlocking {
        Timber.w("🗑️ Clearing session due to refresh failure")
        userPreferences.clearAll()
    }
}