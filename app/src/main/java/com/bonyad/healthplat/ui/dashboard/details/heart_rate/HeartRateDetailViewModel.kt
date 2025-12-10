package com.bonyad.healthplat.ui.dashboard.details.heart_rate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.data.repository.HealthDataRepository
import com.bonyad.healthplat.domain.model.RecordDataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


data class HeartRateRangePoint(
    val timeLabel: String,
    val min: Int,
    val max: Int,
    val isAlert: Boolean = false
)


@HiltViewModel
class HeartRateDetailViewModel @Inject constructor(
    private val deviceManager: HealthDeviceManager,
    private val healthRepository: HealthDataRepository
) : ViewModel() {

    private val _currentHeartRate = MutableStateFlow(0)
    val currentHeartRate: StateFlow<Int> = _currentHeartRate.asStateFlow()

    private val _heartRateData = MutableStateFlow<List<Int>>(emptyList())
    val heartRateData: StateFlow<List<Int>> = _heartRateData.asStateFlow()

    private val _avgHeartRate = MutableStateFlow(0)
    val avgHeartRate: StateFlow<Int> = _avgHeartRate.asStateFlow()

    private val _minHeartRate = MutableStateFlow(0)
    val minHeartRate: StateFlow<Int> = _minHeartRate.asStateFlow()

    private val _maxHeartRate = MutableStateFlow(0)
    val maxHeartRate: StateFlow<Int> = _maxHeartRate.asStateFlow()

    // NEW: Chart Data (List of Ranges instead of single Ints)
    private val _chartData = MutableStateFlow<List<HeartRateRangePoint>>(emptyList())
    val chartData: StateFlow<List<HeartRateRangePoint>> = _chartData.asStateFlow()

    // NEW: HRV Data
    private val _currentHrv = MutableStateFlow(0)
    val currentHrv: StateFlow<Int> = _currentHrv.asStateFlow()

    // NEW: Selected Time Range (Daily, Weekly, Monthly)
    private val _selectedTimeRange = MutableStateFlow("روزانه") // Daily
    val selectedTimeRange: StateFlow<String> = _selectedTimeRange.asStateFlow()

    private val _selectedDayOffset = MutableStateFlow(0) // 0 = today
    val selectedDayOffset = _selectedDayOffset.asStateFlow()

    init {
        loadMockData()
//        loadHeartRateData()
//        observeRealTimeData()
    }


    private fun observeRealTimeData() {
        viewModelScope.launch {
            deviceManager.realTimeData.collect { data ->
                data.heart?.let { hr ->
                    if (hr > 0) {
                        _currentHeartRate.value = hr
                    }
                }
            }
        }
    }

    private fun loadHeartRateData() {
        viewModelScope.launch {
            try {
                // Get today's data (day = 0)
                val result = deviceManager.getRecordData(0)

                if (result is RecordDataResult.Success) {
                    result.heartRate?.heartRateSource?.let { hrData ->
                        // Filter out zero values (no measurement)
                        val validData = hrData.filter { it > 0 }

                        if (validData.isNotEmpty()) {
                            _heartRateData.value = validData
                            _avgHeartRate.value = validData.average().toInt()
                            _minHeartRate.value = validData.minOrNull() ?: 0
                            _maxHeartRate.value = validData.maxOrNull() ?: 0

                            // Use last valid reading as current
                            _currentHeartRate.value = validData.lastOrNull() ?: 0

                            Timber.i("Heart rate data loaded: avg=${_avgHeartRate.value}, min=${_minHeartRate.value}, max=${_maxHeartRate.value}")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load heart rate data")
            }
        }
    }

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
        // logic to reload data based on range
    }

    private fun loadMockData() {
        // Mocking the specific bars seen in your screenshot
        val mockPoints = listOf(
            HeartRateRangePoint("00:00", 60, 90),
            HeartRateRangePoint("04:00", 65, 85),
            HeartRateRangePoint("08:00", 70, 75),
            HeartRateRangePoint("12:00", 60, 95),
            HeartRateRangePoint("15:59", 70, 120, isAlert = true), // The tall bar
            HeartRateRangePoint("20:00", 80, 85),
            HeartRateRangePoint("23:59", 60, 70)
        )
        _chartData.value = mockPoints

        // Mock HRV
        _currentHrv.value = 67
    }

    fun loadHeartRateForDay(offset: Int) {
        viewModelScope.launch {
            try {
                val result = deviceManager.getRecordData(offset)

                if (result is RecordDataResult.Success) {
                    result.heartRate?.heartRateSource?.let { hrData ->
                        val validData = hrData.filter { it > 0 }

                        if (validData.isNotEmpty()) {
                            _heartRateData.value = validData
                            _avgHeartRate.value = validData.average().toInt()
                            _minHeartRate.value = validData.minOrNull() ?: 0
                            _maxHeartRate.value = validData.maxOrNull() ?: 0
                            _currentHeartRate.value = validData.lastOrNull() ?: 0
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load heart rate for day offset=$offset")
            }
        }
    }

    fun selectDay(offset: Int) {
        _selectedDayOffset.value = offset
        loadHeartRateForDay(offset)
    }
}