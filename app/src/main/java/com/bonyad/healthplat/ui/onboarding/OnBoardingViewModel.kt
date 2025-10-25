package com.bonyad.healthplat.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.domain.model.OnBoardingData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel() {

    private val _onboardingComplete = MutableSharedFlow<Unit>()
    val onboardingComplete: SharedFlow<Unit> = _onboardingComplete.asSharedFlow()

    val pages = OnBoardingData.data

    fun completeOnboarding() {
        viewModelScope.launch {
            // TODO: Save to preferences that onboarding is complete
            _onboardingComplete.emit(Unit)
        }
    }
}