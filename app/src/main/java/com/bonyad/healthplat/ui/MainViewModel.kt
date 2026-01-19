package com.bonyad.healthplat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.TokenManager
import com.bonyad.healthplat.ui.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferences: UserPreferencesDataStore,
    private val tokenManager: TokenManager

) : ViewModel() {

    // Start with a "Loading" state (null)
    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination = _startDestination.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    init {
        resolveStartDestination()
    }

    private fun resolveStartDestination() {
        viewModelScope.launch {
            Timber.d("🚀 APP LAUNCH: Starting navigation resolution...")

            // Step 1: Load all preferences
            val userId = userPreferences.getUserId().first()
            val token = userPreferences.getAuthToken().first()
            val refreshToken = userPreferences.getRefreshToken().first()
            val isOnboardingComplete = userPreferences.isOnboardingComplete().first()
            val termsAccepted = userPreferences.isTermsAccepted().first()
            val phoneNumber = userPreferences.getPhoneNumber().first()
            val deviceMac = userPreferences.getDeviceMac().first()

            Timber.d("🚀 APP LAUNCH: userId=$userId, hasToken=${!token.isNullOrEmpty()}, onboarding=$isOnboardingComplete")

            // Step 2: Determine navigation path
            val destination = when {
                // CASE 1: Fresh install (no userId from server)
                userId.isNullOrEmpty() -> {
                    if (!isOnboardingComplete) {
                        Timber.d("📍 → Onboarding (fresh install)")
                        NavRoutes.Onboarding.route
                    } else {
                        Timber.d("📍 → Login (no userId)")
                        NavRoutes.PhoneAuth.route
                    }
                }

                // CASE 2: User exists but logged out (has userId, no tokens)
                token.isNullOrEmpty() || refreshToken.isNullOrEmpty() -> {
                    Timber.d("📍 → Login (logged out)")
                    NavRoutes.PhoneAuth.route
                }

                // CASE 3: User logged in - check token validity
                else -> {
                    Timber.i("🔐 APP LAUNCH: User logged in, checking token...")

                    // Check if refresh token is ancient (> 30 days)
                    if (tokenManager.isRefreshTokenExpired()) {
                        Timber.w("⚠️ Refresh token expired (>30 days) - require re-login")
                        userPreferences.clearAuthOnly() // ✅ Only clear tokens
                        _startDestination.value = NavRoutes.PhoneAuth.route
                        _isLoading.value = false
                        return@launch
                    }

                    // Try to refresh if access token is expiring
                    val tokenValid = tokenManager.ensureValidToken()

                    if (!tokenValid) {
                        Timber.e("❌ Token refresh failed - require re-login")
                        userPreferences.clearAuthOnly() // ✅ Only clear tokens
                        _startDestination.value = NavRoutes.PhoneAuth.route
                        _isLoading.value = false
                        return@launch
                    }

                    Timber.i("✅ Token valid, determining completion status...")

                    // Check completion steps
                    when {
                        !termsAccepted -> {
                            Timber.d("📍 → Terms (not accepted)")
                            NavRoutes.TermsAndPrivacy.route
                        }

                        phoneNumber.isNullOrEmpty() -> {
                            Timber.d("📍 → PersonalInfo (missing)")
                            NavRoutes.PersonalInfo.route
                        }

//                        deviceMac.isNullOrEmpty() -> {
//                            Timber.d("📍 → DeviceConnection (missing)")
//                            NavRoutes.DeviceConnection.route
//                        }

                        else -> {
                            Timber.d("📍 → Dashboard (complete)")
                            NavRoutes.Dashboard.route
                        }
                    }
                }
            }

            Timber.i("🎯 APP LAUNCH: Final destination = $destination")
            _startDestination.value = destination
            _isLoading.value = false
        }
    }
}