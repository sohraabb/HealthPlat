package com.bonyad.healthplat.ui.dashboard.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProfileDashboardViewModel @Inject constructor(
    private val userPreferences: UserPreferencesDataStore
) : ViewModel() {

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
                userPreferences.clearAuthOnly()
                Timber.i("User logged out successfully")
                // TODO: Navigate to login screen
            } catch (e: Exception) {
                Timber.e(e, "Failed to logout")
            }
        }
    }
}
