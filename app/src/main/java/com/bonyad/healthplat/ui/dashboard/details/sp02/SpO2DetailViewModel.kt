package com.bonyad.healthplat.ui.dashboard.details.sp02

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
class SpO2DetailViewModel @Inject constructor(
    private val deviceManager: HealthDeviceManager
) : ViewModel() {

    private val _currentSpO2 = MutableStateFlow(0)
    val currentSpO2: StateFlow<Int> = _currentSpO2.asStateFlow()

    private val _spo2Data = MutableStateFlow<List<Int>>(emptyList())
    val spo2Data: StateFlow<List<Int>> = _spo2Data.asStateFlow()

    private val _minSpO2 = MutableStateFlow(0)
    val minSpO2: StateFlow<Int> = _minSpO2.asStateFlow()

    private val _maxSpO2 = MutableStateFlow(0)
    val maxSpO2: StateFlow<Int> = _maxSpO2.asStateFlow()

    private val _selectedDayOffset = MutableStateFlow(0) // 0 = today
    val selectedDayOffset = _selectedDayOffset.asStateFlow()

    // 1. Chart Data: Individual measurements
    data class SpO2Point(
        val timeLabel: String, // e.g., "15:59"
        val timeRatio: Float,  // 0.0 to 1.0 for X-axis positioning
        val value: Int         // 0-100%
    )

    private val _chartData = MutableStateFlow<List<SpO2Point>>(emptyList())
    val chartData: StateFlow<List<SpO2Point>> = _chartData.asStateFlow()

    // 2. Stats (High, Avg, Low, Last)
    data class SpO2Stats(
        val high: Int,
        val avg: Int,
        val low: Int,
        val lastValue: Int,
        val lastTime: String
    )

    private val _stats = MutableStateFlow(SpO2Stats(99, 97, 93, 97, "۲۳:۵۶"))
    val stats: StateFlow<SpO2Stats> = _stats.asStateFlow()

    // 3. UI State
    private val _selectedTimeRange = MutableStateFlow("روزانه")
    val selectedTimeRange: StateFlow<String> = _selectedTimeRange.asStateFlow()

    init {
        loadSpO2ForDay(0)
//        loadMockData()
//        loadSpO2Data()
    }

    private fun loadSpO2Data() {
        viewModelScope.launch {
            val result = deviceManager.getRecordData(0)
            if (result is RecordDataResult.Success) {
                result.spo2?.sourceList?.let { data ->
                    val validData = data.filter { it > 0 }
                    _spo2Data.value = validData
                    _currentSpO2.value = validData.lastOrNull() ?: 0
                    _minSpO2.value = validData.minOrNull() ?: 0
                    _maxSpO2.value = validData.maxOrNull() ?: 0
                }
            }
        }
    }

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
    }

    private fun loadMockData() {
        // Mocking the Scatter Plot to match the screenshot
        _chartData.value = listOf(
            SpO2Point("01:00", 0.05f, 95),
            SpO2Point("02:00", 0.10f, 98),
            SpO2Point("02:30", 0.12f, 99), // The high point on left
            SpO2Point("03:00", 0.15f, 98),

            SpO2Point("07:00", 0.30f, 96), // Mid-left
            SpO2Point("10:00", 0.45f, 100), // The peak
            SpO2Point("14:00", 0.58f, 94),

            SpO2Point("16:00", 0.65f, 94), // Cluster
            SpO2Point("16:30", 0.68f, 94),
            SpO2Point("17:00", 0.71f, 94),
            SpO2Point("17:30", 0.74f, 94),

            SpO2Point("20:00", 0.85f, 96),
            SpO2Point("22:00", 0.92f, 98),
            SpO2Point("23:00", 0.96f, 97)
        )
    }

    // ADD new function:
    fun loadSpO2ForDay(offset: Int) {
        viewModelScope.launch {
            try {
                val result = deviceManager.getRecordData(offset)

                if (result is RecordDataResult.Success) {
                    result.spo2?.sourceList?.let { spo2Data ->
                        val validData = spo2Data.filter { it > 0 }

                        if (validData.isEmpty()) {
                            Timber.w("No SpO2 data for day $offset")
                            return@let
                        }

                        _spo2Data.value = validData
                        _currentSpO2.value = validData.lastOrNull() ?: 0
                        _minSpO2.value = validData.minOrNull() ?: 0
                        _maxSpO2.value = validData.maxOrNull() ?: 0

                        // Convert to scatter points
                        _chartData.value = convertToScatterPoints(validData)

                        // Update stats
                        val avg = validData.average().toInt()
                        val lastTime = "23:56" // You can calculate from timestamp if needed

                        _stats.value = SpO2Stats(
                            high = _maxSpO2.value,
                            avg = avg,
                            low = _minSpO2.value,
                            lastValue = _currentSpO2.value,
                            lastTime = lastTime
                        )

                        Timber.i("✅ SpO2 loaded: avg=$avg, min=${_minSpO2.value}, max=${_maxSpO2.value}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading SpO2 for day $offset")
            }
        }
    }

    // ADD helper:
    private fun convertToScatterPoints(spo2Data: List<Int>): List<SpO2Point> {
        val points = mutableListOf<SpO2Point>()

        // SDK records SpO2 every 30 minutes (48 readings per day)
        // Map to 0.0-1.0 ratio for 24 hours

        spo2Data.forEachIndexed { index, value ->
            if (value > 0) {
                val timeRatio = index.toFloat() / spo2Data.size

                // Calculate hour:minute for label
                val totalMinutes = (timeRatio * 24 * 60).toInt()
                val hour = totalMinutes / 60
                val minute = totalMinutes % 60
                val timeLabel = String.format("%02d:%02d", hour, minute)

                points.add(
                    SpO2Point(
                        timeLabel = timeLabel,
                        timeRatio = timeRatio,
                        value = value
                    )
                )
            }
        }

        return points
    }

    // UPDATE selectDay:
    fun selectDay(offset: Int) {
        _selectedDayOffset.value = offset
        loadSpO2ForDay(offset)
    }
}