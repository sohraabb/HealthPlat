package com.bonyad.healthplat.ui.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TermsPrivacyViewModel @Inject constructor(
    private val userPreferences: UserPreferencesDataStore
) : ViewModel() {

    private val _termsAccepted = MutableStateFlow(false)
    val termsAccepted: StateFlow<Boolean> = _termsAccepted.asStateFlow()

    private val _marketingAccepted = MutableStateFlow(false)
    val marketingAccepted: StateFlow<Boolean> = _marketingAccepted.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<Unit>()
    val navigationEvent: SharedFlow<Unit> = _navigationEvent.asSharedFlow()

    fun updateTermsAccepted(accepted: Boolean) {
        _termsAccepted.value = accepted
    }

    fun updateMarketingAccepted(accepted: Boolean) {
        _marketingAccepted.value = accepted
    }

    fun acceptTerms() {
        if (_termsAccepted.value) {
            viewModelScope.launch {
                try {
                    userPreferences.setTermsAccepted(true)
                    userPreferences.setMarketingAccepted(_marketingAccepted.value)

                    Timber.i("Terms and privacy accepted")
                    _navigationEvent.emit(Unit)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to save terms acceptance")
                }
            }
        }
    }
}