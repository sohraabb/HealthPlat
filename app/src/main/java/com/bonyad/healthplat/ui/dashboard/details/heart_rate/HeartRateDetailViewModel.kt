package com.bonyad.healthplat.ui.dashboard.details.heart_rate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.HealthPlatApiService
import com.bonyad.healthplat.data.repository.HealthDataRepository
import com.bonyad.healthplat.data.repository.MetricType
import com.bonyad.healthplat.domain.model.MetricData
import com.bonyad.healthplat.domain.model.RecordDataResult
import com.bonyad.healthplat.ui.utils.toFarsiDigits
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import saman.zamani.persiandate.PersianDate
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.GregorianCalendar
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
    private val healthRepository: HealthDataRepository,
    private val userPreferences: UserPreferencesDataStore
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

    // Chart Data (List of Ranges)
    private val _chartData = MutableStateFlow<List<HeartRateRangePoint>>(emptyList())
    val chartData: StateFlow<List<HeartRateRangePoint>> = _chartData.asStateFlow()

    // HRV Data
    private val _currentHrv = MutableStateFlow(0)
    val currentHrv: StateFlow<Int> = _currentHrv.asStateFlow()

    private val _avgHrv = MutableStateFlow(0)
    val avgHrv: StateFlow<Int> = _avgHrv.asStateFlow()

    private val _minHrv = MutableStateFlow(0)
    val minHrv: StateFlow<Int> = _minHrv.asStateFlow()

    private val _maxHrv = MutableStateFlow(0)
    val maxHrv: StateFlow<Int> = _maxHrv.asStateFlow()

    // Selected Time Range (Daily, Weekly, Monthly)
    private val _selectedTimeRange = MutableStateFlow("روزانه") // Daily
    val selectedTimeRange: StateFlow<String> = _selectedTimeRange.asStateFlow()

    private val _selectedDayOffset = MutableStateFlow(0) // 0 = today
    val selectedDayOffset = _selectedDayOffset.asStateFlow()

    private val _currentPersianDate = MutableStateFlow("")
    val currentPersianDate: StateFlow<String> = _currentPersianDate.asStateFlow()

    private val _hrvChartData = MutableStateFlow<List<Int>>(emptyList())
    val hrvChartData: StateFlow<List<Int>> = _hrvChartData.asStateFlow()

    // Add this function
    private fun updateCurrentPersianDate(offset: Int = 0) {
        val today = LocalDate.now()
        val targetDate = today.plusDays(offset.toLong())
        val calendar = GregorianCalendar(targetDate.year, targetDate.monthValue - 1, targetDate.dayOfMonth)
        val pDate = PersianDate(calendar.time)
        val dayOfMonth = pDate.shDay.toString().toFarsiDigits()
        val monthName = pDate.monthName

        _currentPersianDate.value = when (offset) {
            0 -> "امروز $dayOfMonth $monthName"
            -1 -> "دیروز $dayOfMonth $monthName"
            else -> "$monthName $dayOfMonth"
        }
    }

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        updateCurrentPersianDate()
        loadDataFromApi()
        observeRealTimeData()
    }

    fun refreshData() {
        loadDataFromApi()
    }

    /**
     * Load both Heart Rate and HRV data from API
     */
    private fun loadDataFromApi() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val (dateFrom, dateTo) = getDateRange()

                Timber.i("📡 Fetching HR & HRV: $dateFrom to $dateTo")

                // Fetch both in parallel
                val heartRateDeferred = async {
                    healthRepository.getMetricData(MetricType.HEART_RATE, dateFrom, dateTo)
                }
                val hrvDeferred = async {
                    healthRepository.getMetricData(MetricType.HRV, dateFrom, dateTo)
                }

                // Process Heart Rate
                heartRateDeferred.await().fold(
                    onSuccess = { metricsData ->
                        if (metricsData.isNotEmpty()) {
                            processHeartRateData(metricsData)
                            Timber.i("✅ Heart rate loaded: ${metricsData.size} records")
                        } else {
                            Timber.w("⚠️ No heart rate data from API")
                            clearHeartRateData()
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "❌ Heart rate API error")
                        loadHeartRateFromDevice(_selectedDayOffset.value)
                    }
                )

                // Process HRV
                hrvDeferred.await().fold(
                    onSuccess = { metricsData ->
                        if (metricsData.isNotEmpty()) {
                            processHrvData(metricsData)
                            Timber.i("✅ HRV loaded: ${metricsData.size} records")
                        } else {
                            Timber.w("⚠️ No HRV data from API")
                            clearHrvData()
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "❌ HRV API error")
                        loadHrvFromDevice(_selectedDayOffset.value)
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to load data from API")
                loadHeartRateFromDevice(_selectedDayOffset.value)
                loadHrvFromDevice(_selectedDayOffset.value)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Get date range based on selected time range
     */
    private fun getDateRange(): Pair<String, String> {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE // yyyy-MM-dd

        return when (_selectedTimeRange.value) {
            "روزانه" -> {
                val targetDate = today.plusDays(_selectedDayOffset.value.toLong())
                val dateStr = targetDate.format(formatter)
                updateCurrentPersianDate(_selectedDayOffset.value)
                Pair(dateStr, dateStr)
            }
            "هفتگی" -> {
                val dateFrom = today.minusDays(6).format(formatter)
                val dateTo = today.format(formatter)
                _currentPersianDate.value = "هفته گذشته"
                Pair(dateFrom, dateTo)
            }
            "ماهانه" -> {
                val dateFrom = today.minusDays(29).format(formatter)
                val dateTo = today.format(formatter)
                _currentPersianDate.value = "ماه گذشته"
                Pair(dateFrom, dateTo)
            }
            else -> {
                val dateStr = today.format(formatter)
                Pair(dateStr, dateStr)
            }
        }
    }

    // ============ Heart Rate Processing ============

    private fun processHeartRateData(metricsData: List<MetricData>) {
        when (_selectedTimeRange.value) {
            "روزانه" -> processDailyHeartRate(metricsData)
            "هفتگی" -> processWeeklyHeartRate(metricsData)
            "ماهانه" -> processMonthlyHeartRate(metricsData)
        }
    }

    private fun processDailyHeartRate(metricsData: List<MetricData>) {
        val allValues = metricsData.flatMap { it.values }

        if (allValues.isEmpty()) {
            clearHeartRateData()
            return
        }

        val validData = allValues.filter { it > 1 }

        if (validData.isEmpty()) {
            clearHeartRateData()
            return
        }

        _avgHeartRate.value = validData.average().toInt()
        _minHeartRate.value = validData.minOrNull() ?: 0
        _maxHeartRate.value = validData.maxOrNull() ?: 0
        _currentHeartRate.value = validData.lastOrNull() ?: 0
        _heartRateData.value = validData

        _chartData.value = buildHourlyChartData(allValues)
    }

    private fun processWeeklyHeartRate(metricsData: List<MetricData>) {
        val allValidValues = metricsData.flatMap { it.values }.filter { it > 1 }

        if (allValidValues.isEmpty()) {
            clearHeartRateData()
            return
        }

        _avgHeartRate.value = allValidValues.average().toInt()
        _minHeartRate.value = allValidValues.minOrNull() ?: 0
        _maxHeartRate.value = allValidValues.maxOrNull() ?: 0
        _currentHeartRate.value = allValidValues.lastOrNull() ?: 0
        _heartRateData.value = allValidValues

        val chartPoints = metricsData.mapNotNull { metric ->
            val dayValues = metric.values.filter { it > 1 }
            if (dayValues.isEmpty()) return@mapNotNull null

            val dateLabel = try {
                val date = LocalDate.parse(metric.recordDate.substring(0, 10))
                "${date.dayOfMonth}"
            } catch (e: Exception) {
                metric.recordDate.substring(8, 10)
            }

            HeartRateRangePoint(
                timeLabel = dateLabel,
                min = dayValues.minOrNull() ?: 0,
                max = dayValues.maxOrNull() ?: 0,
                isAlert = (dayValues.maxOrNull() ?: 0) > 120 || (dayValues.minOrNull() ?: 0) < 50
            )
        }

        _chartData.value = chartPoints
    }

    private fun processMonthlyHeartRate(metricsData: List<MetricData>) {
        val allValidValues = metricsData.flatMap { it.values }.filter { it > 1 }

        if (allValidValues.isEmpty()) {
            clearHeartRateData()
            return
        }

        _avgHeartRate.value = allValidValues.average().toInt()
        _minHeartRate.value = allValidValues.minOrNull() ?: 0
        _maxHeartRate.value = allValidValues.maxOrNull() ?: 0
        _currentHeartRate.value = allValidValues.lastOrNull() ?: 0
        _heartRateData.value = allValidValues

        val sortedMetrics = metricsData.sortedBy { it.recordDate }
        val weeklyGroups = sortedMetrics.chunked(7)

        val chartPoints = weeklyGroups.mapIndexed { weekIndex, weekMetrics ->
            val weekValues = weekMetrics.flatMap { it.values }.filter { it > 1 }
            if (weekValues.isEmpty()) return@mapIndexed null

            HeartRateRangePoint(
                timeLabel = "هفته ${weekIndex + 1}",
                min = weekValues.minOrNull() ?: 0,
                max = weekValues.maxOrNull() ?: 0,
                isAlert = (weekValues.maxOrNull() ?: 0) > 120 || (weekValues.minOrNull() ?: 0) < 50
            )
        }.filterNotNull()

        _chartData.value = chartPoints
    }

    private fun buildHourlyChartData(hrData: List<Int>): List<HeartRateRangePoint> {
        val result = mutableListOf<HeartRateRangePoint>()
        val minutesPerHour = 60

        for (hour in 0..23) {
            val startIdx = hour * minutesPerHour
            val endIdx = minOf(startIdx + minutesPerHour, hrData.size)

            if (startIdx >= hrData.size) break

            val hourData = hrData.subList(startIdx, endIdx).filter { it > 1 }
            if (hourData.isEmpty()) continue

            val min = hourData.minOrNull() ?: 0
            val max = hourData.maxOrNull() ?: 0
            val isAlert = max > 120 || min < 50

            result.add(
                HeartRateRangePoint(
                    timeLabel = String.format("%02d:00", hour),
                    min = min,
                    max = max,
                    isAlert = isAlert
                )
            )
        }

        return result
    }

    // ============ HRV Processing ============

    private fun processHrvData(metricsData: List<MetricData>) {
        val allValues = metricsData.flatMap { it.values }

        if (allValues.isEmpty()) {
            clearHrvData()
            return
        }

        val validData = allValues.filter { it > 0 }

        if (validData.isEmpty()) {
            clearHrvData()
            return
        }

        _avgHrv.value = validData.average().toInt()
        _minHrv.value = validData.minOrNull() ?: 0
        _maxHrv.value = validData.maxOrNull() ?: 0
        _currentHrv.value = validData.lastOrNull() ?: 0
        _hrvChartData.value = validData // Store for chart

        Timber.d("📊 HRV Stats - Avg: ${_avgHrv.value}, Min: ${_minHrv.value}, Max: ${_maxHrv.value}, Current: ${_currentHrv.value}")
    }

    // ============ Device Fallback ============

    private fun loadHeartRateFromDevice(offset: Int) {
        viewModelScope.launch {
            try {
                val result = deviceManager.getRecordData(offset)

                if (result is RecordDataResult.Success) {
                    result.heartRate?.heartRateSource?.let { hrData ->
                        val validData = hrData.filter { it > 1 }

                        if (validData.isNotEmpty()) {
                            _heartRateData.value = validData
                            _avgHeartRate.value = validData.average().toInt()
                            _minHeartRate.value = validData.minOrNull() ?: 0
                            _maxHeartRate.value = validData.maxOrNull() ?: 0
                            _currentHeartRate.value = validData.lastOrNull() ?: 0
                            _chartData.value = buildHourlyChartData(hrData)

                            Timber.i("✅ HR loaded from device for day $offset")
                        } else {
                            clearHeartRateData()
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading HR from device")
                clearHeartRateData()
            }
        }
    }

    private fun loadHrvFromDevice(offset: Int) {
        viewModelScope.launch {
            try {
                val result = deviceManager.getRecordData(offset)

                if (result is RecordDataResult.Success) {
                    result.hrv?.hrvSource?.let { hrvData ->
                        val validData = hrvData.filter { it > 0 }

                        if (validData.isNotEmpty()) {
                            _avgHrv.value = validData.average().toInt()
                            _minHrv.value = validData.minOrNull() ?: 0
                            _maxHrv.value = validData.maxOrNull() ?: 0
                            _currentHrv.value = validData.lastOrNull() ?: 0

                            Timber.i("✅ HRV loaded from device for day $offset")
                        } else {
                            clearHrvData()
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading HRV from device")
                clearHrvData()
            }
        }
    }

    // ============ UI Actions ============

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
        loadDataFromApi()
    }

    fun selectDay(offset: Int) {
        _selectedDayOffset.value = offset
        updateCurrentPersianDate(offset)
        if (_selectedTimeRange.value == "روزانه") {
            loadDataFromApi()
        }
    }

    // ============ Clear Data ============

    private fun clearHeartRateData() {
        _heartRateData.value = emptyList()
        _chartData.value = emptyList()
        _avgHeartRate.value = 0
        _minHeartRate.value = 0
        _maxHeartRate.value = 0
        _currentHeartRate.value = 0
    }

    private fun clearHrvData() {
        _currentHrv.value = 0
        _avgHrv.value = 0
        _minHrv.value = 0
        _maxHrv.value = 0
        _hrvChartData.value = emptyList()
    }

    // Real-time data observation (optional)
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
}