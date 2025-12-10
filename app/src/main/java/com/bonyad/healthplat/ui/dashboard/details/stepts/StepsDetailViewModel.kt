package com.bonyad.healthplat.ui.dashboard.details.stepts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.domain.model.RecordDataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StepsDetailViewModel @Inject constructor(
    private val deviceManager: HealthDeviceManager
) : ViewModel() {

    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    private val _weeklySteps = MutableStateFlow<List<Int>>(emptyList())
    val weeklySteps: StateFlow<List<Int>> = _weeklySteps.asStateFlow()

    private val _averageSteps = MutableStateFlow(0)
    val averageSteps: StateFlow<Int> = _averageSteps.asStateFlow()

    data class StepBarPoint(
        val hourLabel: String,
        val steps: Int,
        val isSelected: Boolean = false
    )

    private val _barChartData = MutableStateFlow<List<StepBarPoint>>(emptyList())
    val barChartData: StateFlow<List<StepBarPoint>> = _barChartData.asStateFlow()


    data class ComparisonPoint(
        val timeRatio: Float, // 0.0 to 1.0 (x-axis position)
        val todaySteps: Int,
        val avgSteps: Int
    )

    private val _comparisonData = MutableStateFlow<List<ComparisonPoint>>(emptyList())
    val comparisonData: StateFlow<List<ComparisonPoint>> = _comparisonData.asStateFlow()

    // 3. UI State
    private val _totalSteps = MutableStateFlow(5450)
    val totalSteps: StateFlow<Int> = _totalSteps.asStateFlow()

    private val _selectedTimeRange = MutableStateFlow("روزانه")
    val selectedTimeRange: StateFlow<String> = _selectedTimeRange.asStateFlow()

    private val _selectedDayOffset = MutableStateFlow(0) // 0 = today
    val selectedDayOffset = _selectedDayOffset.asStateFlow()

    init {
        loadMockData()
//        observeRealTimeData()
//        loadWeeklyData()
    }

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
    }

    private fun loadMockData() {
        // Mocking the Orange Bar Chart
        _barChartData.value = listOf(
            StepBarPoint("07:00", 200),
            StepBarPoint("08:00", 300),
            StepBarPoint("09:00", 1200), // Peak 1
            StepBarPoint("10:00", 400),
            StepBarPoint("16:00", 1000), // Peak 2
            StepBarPoint("17:00", 4200, isSelected = true), // The big spike with tooltip
            StepBarPoint("18:00", 500),
            StepBarPoint("20:00", 300)
        )

        // Mocking the Comparison Lines
        _comparisonData.value = listOf(
            ComparisonPoint(0f, 0, 0),
            ComparisonPoint(0.2f, 100, 200),
            ComparisonPoint(0.5f, 500, 1200),
            ComparisonPoint(0.7f, 800, 2500),
            ComparisonPoint(1.0f, 1058, 3000)
        )
    }

    fun selectDay(offset: Int) {
        _selectedDayOffset.value = offset
//        loadHeartRateForDay(offset)
    }




    private fun observeRealTimeData() {
        viewModelScope.launch {
            deviceManager.realTimeData.collect { data ->
                data.step?.let { _todaySteps.value = it }
            }
        }
    }

    private fun loadWeeklyData() {
        viewModelScope.launch {
            val weekData = mutableListOf<Int>()
            // Load last 7 days (day 0 to 6)
            for (day in 0..6) {
                val result = deviceManager.getRecordData(day)
                if (result is RecordDataResult.Success) {
                    val totalSteps = result.steps?.stepSource?.sum() ?: 0
                    weekData.add(totalSteps)
                }
            }
            _weeklySteps.value = weekData
            _averageSteps.value = weekData.average().toInt()
        }
    }
}