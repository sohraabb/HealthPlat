package com.bonyad.healthplat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
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
    private val userPreferences: UserPreferencesDataStore
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
            val isOnboardingComplete = userPreferences.isOnboardingComplete().first()
            val token = userPreferences.getAuthToken().first()
            val refreshToken = userPreferences.getRefreshToken().first()
            val userId = userPreferences.getUserId().first()
            val deviceMac = userPreferences.getDeviceMac().first()
            val termsAccepted = userPreferences.isTermsAccepted().first()
            val userName = userPreferences.getUserName().first()

            // 🔍 Debug Log to catch "Zombie" states
            Timber.d("🔍 Startup Check: Token=${token != null}, User=$userId")

            val destination = when {
                userId != null && !refreshToken.isNullOrEmpty() -> {
                    if (userName.isNullOrEmpty()) NavRoutes.PersonalInfo.route
                    else if (deviceMac.isNullOrEmpty()) NavRoutes.DeviceConnection.route
                    else NavRoutes.Dashboard.route
                }

                !isOnboardingComplete -> NavRoutes.Onboarding.route
                !termsAccepted -> NavRoutes.TermsAndPrivacy.route
                else -> NavRoutes.PhoneAuth.route
            }

            _startDestination.value = destination
            _isLoading.value = false
        }
    }
}