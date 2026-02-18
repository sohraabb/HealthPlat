package com.bonyad.healthplat.ui.dashboard.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.repository.AuthRepository
import com.bonyad.healthplat.worker.HealthSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


sealed class ProfileNavigationEvent {
    object NavigateToLogin : ProfileNavigationEvent()
}

@HiltViewModel
class ProfileDashboardViewModel @Inject constructor(
    private val userPreferences: UserPreferencesDataStore,
    private val authRepository: AuthRepository,
    private val healthSyncScheduler: HealthSyncScheduler
) : ViewModel() {

    private val _navigationEvent = Channel<ProfileNavigationEvent>()
    val navigationEvent = _navigationEvent.receiveAsFlow()

    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName.asStateFlow()

    private val _phoneNumber = MutableStateFlow<String?>(null)
    val phoneNumber: StateFlow<String?> = _phoneNumber.asStateFlow()

    private val _currentGoal = MutableStateFlow("5000 قدم")
    val currentGoal: StateFlow<String> = _currentGoal.asStateFlow()

    private val _goalProgress = MutableStateFlow(0.43f) // 43%
    val goalProgress: StateFlow<Float> = _goalProgress.asStateFlow()

    private val _nightModeEnabled = MutableStateFlow(false)
    val nightModeEnabled: StateFlow<Boolean> = _nightModeEnabled.asStateFlow()

    private val _showLogoutDialog = MutableStateFlow(false)
    val showLogoutDialog: StateFlow<Boolean> = _showLogoutDialog.asStateFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            userPreferences.getUserName().collect { name ->
                _userName.value = name
            }
        }

        viewModelScope.launch {
            userPreferences.getPhoneNumber().collect { phone ->
                _phoneNumber.value = phone
            }
        }
    }

    fun onAdjustGoalsClick() {
        Timber.i("Adjust goals clicked")
        // TODO: Navigate to goals adjustment screen
    }

    fun onNightModeToggle(enabled: Boolean) {
        _nightModeEnabled.value = enabled
        Timber.i("Night mode toggled: $enabled")
        // TODO: Save to preferences and apply theme
    }

    fun logout() {
        viewModelScope.launch {
            try {
                Timber.i("🚪 Logout initiated...")

                healthSyncScheduler.stopPeriodicSync()
                Timber.i("✅ Background sync stopped")

                authRepository.logout()
                userPreferences.clearAuthOnly()
                Timber.i("✅ Auth data cleared")

                _navigationEvent.send(ProfileNavigationEvent.NavigateToLogin)
                Timber.i("✅ Logout complete")

            } catch (e: Exception) {
                Timber.e(e, "❌ Logout failed, but clearing data anyway...")
                // Fallback
                healthSyncScheduler.stopPeriodicSync()
                userPreferences.clearAuthOnly()
                _navigationEvent.send(ProfileNavigationEvent.NavigateToLogin)
            }
        }
    }

    fun showLogoutConfirmation() {
        _showLogoutDialog.value = true
    }

    fun dismissLogoutDialog() {
        _showLogoutDialog.value = false
    }

    fun confirmLogout() {
        _showLogoutDialog.value = false
        logout()
    }
}
