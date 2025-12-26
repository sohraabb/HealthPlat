package com.bonyad.healthplat.ui.dashboard.details.stress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.domain.model.RecordDataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
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
        loadStressForDay(0)
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

    fun loadStressForDay(offset: Int) {
        viewModelScope.launch {
            try {
                val result = deviceManager.getRecordData(offset)

                if (result is RecordDataResult.Success) {
                    result.stress?.stressSource?.let { stressData ->
                        val validData = stressData.filter { it > 0 }

                        if (validData.isEmpty()) {
                            Timber.w("No stress data for day $offset")
                            return@let
                        }

                        // Calculate stats
                        val high = validData.maxOrNull() ?: 0
                        val low = validData.minOrNull() ?: 0
                        val avg = validData.average().toInt()
                        val current = validData.lastOrNull() ?: 0

                        _stats.value = StressStats(
                            rangeMin = low,
                            rangeMax = high,
                            currentVal = current,
                            currentTime = "23:56",
                            high = high,
                            avg = avg,
                            low = low
                        )

                        // Convert to curve points
                        _chartPoints.value = convertToCurvePoints(validData)

                        Timber.i("✅ Stress loaded: avg=$avg, range=$low-$high")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading stress for day $offset")
            }
        }
    }

    // ADD helper:
    private fun convertToCurvePoints(stressData: List<Int>): List<StressPoint> {
        val points = mutableListOf<StressPoint>()

        // Sample every N minutes to get smooth curve
        val sampleInterval = maxOf(1, stressData.size / 20) // ~20 points

        for (i in stressData.indices step sampleInterval) {
            val value = stressData[i]
            if (value > 0) {
                val xRatio = i.toFloat() / stressData.size
                points.add(StressPoint(xRatio, value))
            }
        }

        // Ensure last point is included
        if (points.lastOrNull()?.xRatio != 1.0f) {
            val lastValue = stressData.lastOrNull { it > 0 } ?: 0
            if (lastValue > 0) {
                points.add(StressPoint(1.0f, lastValue))
            }
        }

        return points
    }

    // UPDATE selectDay:
    fun selectDay(offset: Int) {
        _selectedDayOffset.value = offset
        loadStressForDay(offset)
    }
}