package com.bonyad.healthplat.ui.dashboard.details.stress

import androidx.lifecycle.ViewModel
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class StressDetailViewModel @Inject constructor(
    private val deviceManager: HealthDeviceManager
) : ViewModel() {

    data class StressStats(
        val rangeMin: Int, val rangeMax: Int,
        val currentVal: Int, val currentTime: String,
        val high: Int, val avg: Int, val low: Int
    )

    // Points for the curve
    data class StressPoint(val xRatio: Float, val value: Int)

    private val _chartPoints = MutableStateFlow<List<StressPoint>>(emptyList())
    val chartPoints: StateFlow<List<StressPoint>> = _chartPoints.asStateFlow()

    private val _stats = MutableStateFlow(
        StressStats(50, 85, 28, "۲۳:۵۶", 90, 66, 28)
    )
    val stats: StateFlow<StressStats> = _stats.asStateFlow()

    private val _selectedTimeRange = MutableStateFlow("روزانه")
    val selectedTimeRange: StateFlow<String> = _selectedTimeRange.asStateFlow()

    private val _selectedDayOffset = MutableStateFlow(0)
    val selectedDayOffset = _selectedDayOffset.asStateFlow()

    init {
        loadMockData()
    }

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
    }

    private fun loadMockData() {
        // Curve points matching the screenshot (Low -> Peak -> Low)
        _chartPoints.value = listOf(
            StressPoint(0.05f, 15),
            StressPoint(0.25f, 20),
            StressPoint(0.35f, 40),
            StressPoint(0.45f, 60), // Peak
            StressPoint(0.55f, 50),
            StressPoint(0.70f, 25),
            StressPoint(0.90f, 18),
            StressPoint(0.95f, 18)  // End dot
        )
    }

    fun selectDay(offset: Int) {
        _selectedDayOffset.value = offset
    }
}