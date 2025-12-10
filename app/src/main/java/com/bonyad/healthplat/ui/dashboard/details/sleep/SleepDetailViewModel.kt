package com.bonyad.healthplat.ui.dashboard.details.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.data.repository.HealthDataRepository
import com.bonyad.healthplat.domain.model.RecordDataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SleepDetailViewModel @Inject constructor(
    private val deviceManager: HealthDeviceManager
) : ViewModel() {

    private val _deepMinutes = MutableStateFlow(0)
    val deepMinutes: StateFlow<Int> = _deepMinutes.asStateFlow()

    private val _lightMinutes = MutableStateFlow(0)
    val lightMinutes: StateFlow<Int> = _lightMinutes.asStateFlow()

    private val _remMinutes = MutableStateFlow(0)
    val remMinutes: StateFlow<Int> = _remMinutes.asStateFlow()

    private val _awakeMinutes = MutableStateFlow(0)
    val awakeMinutes: StateFlow<Int> = _awakeMinutes.asStateFlow()

    private val _totalSleepMinutes = MutableStateFlow(0)
    val totalSleepMinutes: StateFlow<Int> = _totalSleepMinutes.asStateFlow()

    private val _sleepQuality = MutableStateFlow(0)
    val sleepQuality: StateFlow<Int> = _sleepQuality.asStateFlow()

    init {
        loadSleepData()
    }

    private fun calculateSleepQuality(
        deep: Int,
        light: Int,
        rem: Int,
        awake: Int
    ): Int {
        val total = deep + light + rem + awake
        if (total == 0) return 0

        val deepWeight = deep * 0.5
        val remWeight = rem * 0.3
        val lightWeight = light * 0.1

        val rawScore = deepWeight + remWeight + lightWeight
        val normalized = (rawScore / total) * 100

        return normalized.coerceIn(0.0, 100.0).toInt()
    }

    private fun loadSleepData() {
        viewModelScope.launch {
            try {
                val result = deviceManager.getRecordData(0)

                if (result is RecordDataResult.Success) {
                    val sleepBean = result.sleep

                    if (sleepBean?.sourceList != null) {

                        val deep = sleepBean.sourceList.count { it == 1 }
                        val light = sleepBean.sourceList.count { it == 2 }
                        val awake = sleepBean.sourceList.count { it == 3 }
                        val rem = sleepBean.sourceList.count { it == 4 }

                        _deepMinutes.value = deep
                        _lightMinutes.value = light
                        _awakeMinutes.value = awake
                        _remMinutes.value = rem

                        val total = deep + light + rem + awake
                        _totalSleepMinutes.value = total

                        // Basic quality formula (you can adjust it)
                        val sleepScore = calculateSleepQuality(
                            deep = deep,
                            light = light,
                            rem = rem,
                            awake = awake
                        )
                        _sleepQuality.value = sleepScore

                        Timber.i(
                            "Sleep Loaded: deep=$deep, light=$light, rem=$rem, awake=$awake, total=$total, score=$sleepScore"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load sleep data")
            }
        }
    }

}