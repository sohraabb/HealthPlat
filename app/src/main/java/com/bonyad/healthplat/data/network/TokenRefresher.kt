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
class TokenRefresher @Inject constructor(
    private val userPreferences: UserPreferencesDataStore,
    private val signalRManager: SignalRManager,
    @Named("RefreshApiService") private val api: HealthPlatApiService
) {

    private val mutex = Mutex()

    suspend fun refreshIfNeeded() = mutex.withLock {
        val expiresAt = userPreferences.getAccessTokenExpiry().first() ?: return
        val now = System.currentTimeMillis()

        // Refresh 1 minute before expiry
        if (now < expiresAt - 60_000) return

        val access = userPreferences.getAuthToken().first() ?: return
        val refresh = userPreferences.getRefreshToken().first() ?: return

        Timber.i("⏳ Proactive token refresh")

        val response = api.refreshToken(
            expiredTokenWithBearer = "Bearer $access",
            accessToken = access,
            refreshToken = refresh
        )

        if (response.isSuccessful && response.body()?.isSuccess == true) {
            val data = response.body()!!.data!!

            val access = data.accessToken
            val refresh = data.refreshToken
            val exp = data.expDate

            if (access.isNullOrEmpty() || refresh.isNullOrEmpty() || exp.isNullOrEmpty()) {
                Timber.e("❌ Token refresh returned invalid data")
                return
            }

            userPreferences.saveTokens(access, refresh, exp)
            signalRManager.reconnectWithFreshToken()
        }
    }
}