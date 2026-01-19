package com.bonyad.healthplat.ui.dashboard.details.stress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.data.repository.HealthDataRepository
import com.bonyad.healthplat.data.repository.MetricType
import com.bonyad.healthplat.domain.model.MetricData
import com.bonyad.healthplat.domain.model.RecordDataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class StressDetailViewModel @Inject constructor(
    private val deviceManager: HealthDeviceManager,
    private val healthRepository: HealthDataRepository
) : ViewModel() {

    data class StressStats(
        val rangeMin: Int = 0,
        val rangeMax: Int = 0,
        val currentVal: Int = 0,
        val currentTime: String = "",
        val high: Int = 0,
        val avg: Int = 0,
        val low: Int = 0
    )

    // Points for the curve
    data class StressPoint(val xRatio: Float, val value: Int)

    private val _chartPoints = MutableStateFlow<List<StressPoint>>(emptyList())
    val chartPoints: StateFlow<List<StressPoint>> = _chartPoints.asStateFlow()

    private val _stats = MutableStateFlow(StressStats())
    val stats: StateFlow<StressStats> = _stats.asStateFlow()

    private val _selectedTimeRange = MutableStateFlow("روزانه")
    val selectedTimeRange: StateFlow<String> = _selectedTimeRange.asStateFlow()

    private val _selectedDayOffset = MutableStateFlow(0)
    val selectedDayOffset = _selectedDayOffset.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Date label for display
    private val _dateLabel = MutableStateFlow("")
    val dateLabel: StateFlow<String> = _dateLabel.asStateFlow()

    init {
        loadStressFromApi()
    }

    fun refreshData() {
        loadStressFromApi()
    }

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
        loadStressFromApi()
    }

    fun selectDay(offset: Int) {
        _selectedDayOffset.value = offset
        if (_selectedTimeRange.value == "روزانه") {
            loadStressFromApi()
        }
    }

    // ============ API Integration ============

    private fun loadStressFromApi() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val (dateFrom, dateTo) = getDateRange()

                Timber.i("📡 Fetching stress: $dateFrom to $dateTo")

                healthRepository.getMetricData(
                    metricType = MetricType.STRESS,
                    dateFrom = dateFrom,
                    dateTo = dateTo
                ).fold(
                    onSuccess = { metricsData ->
                        if (metricsData.isNotEmpty()) {
                            processStressData(metricsData)
                            Timber.i("✅ Stress loaded: ${metricsData.size} records")
                        } else {
                            Timber.w("⚠️ No stress data from API")
                            clearData()
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "❌ Stress API error")
                        // Fallback to device
                        loadStressFromDevice(_selectedDayOffset.value)
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to load stress from API")
                loadStressFromDevice(_selectedDayOffset.value)
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
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        return when (_selectedTimeRange.value) {
            "روزانه" -> {
                val targetDate = today.plusDays(_selectedDayOffset.value.toLong())
                val dateStr = targetDate.format(formatter)
                updateDateLabel(targetDate)
                Pair(dateStr, dateStr)
            }
            "هفتگی" -> {
                val dateFrom = today.minusDays(6).format(formatter)
                val dateTo = today.format(formatter)
                _dateLabel.value = "هفته گذشته"
                Pair(dateFrom, dateTo)
            }
            "ماهانه" -> {
                val dateFrom = today.minusDays(29).format(formatter)
                val dateTo = today.format(formatter)
                _dateLabel.value = "ماه گذشته"
                Pair(dateFrom, dateTo)
            }
            else -> {
                val dateStr = today.format(formatter)
                Pair(dateStr, dateStr)
            }
        }
    }

    private fun updateDateLabel(date: LocalDate) {
        val today = LocalDate.now()
        _dateLabel.value = when {
            date == today -> "امروز"
            date == today.minusDays(1) -> "دیروز"
            else -> "${date.dayOfMonth} ${getPersianMonth(date.monthValue)}"
        }
    }

    private fun getPersianMonth(month: Int): String {
        return when (month) {
            1 -> "ژانویه"
            2 -> "فوریه"
            3 -> "مارس"
            4 -> "آوریل"
            5 -> "می"
            6 -> "ژوئن"
            7 -> "جولای"
            8 -> "آگوست"
            9 -> "سپتامبر"
            10 -> "اکتبر"
            11 -> "نوامبر"
            12 -> "دسامبر"
            else -> ""
        }
    }

    // ============ Data Processing ============

    private fun processStressData(metricsData: List<MetricData>) {
        when (_selectedTimeRange.value) {
            "روزانه" -> processDailyStress(metricsData)
            "هفتگی" -> processWeeklyStress(metricsData)
            "ماهانه" -> processMonthlyStress(metricsData)
        }
    }

    private fun processDailyStress(metricsData: List<MetricData>) {
        val allValues = metricsData.flatMap { it.values }
        val validData = allValues.filter { it > 0 }

        if (validData.isEmpty()) {
            clearData()
            return
        }

        // Calculate stats
        val high = validData.maxOrNull() ?: 0
        val low = validData.minOrNull() ?: 0
        val avg = validData.average().toInt()
        val current = validData.lastOrNull() ?: 0

        // Get current time for last measurement
        val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        _stats.value = StressStats(
            rangeMin = low,
            rangeMax = high,
            currentVal = current,
            currentTime = currentTime,
            high = high,
            avg = avg,
            low = low
        )

        // Convert to curve points
        _chartPoints.value = convertToCurvePoints(allValues)
    }

    private fun processWeeklyStress(metricsData: List<MetricData>) {
        if (metricsData.isEmpty()) {
            clearData()
            return
        }

        // Aggregate all values from the week
        val allValues = mutableListOf<Int>()
        var totalHigh = 0
        var totalLow = Int.MAX_VALUE
        var totalSum = 0
        var totalCount = 0

        metricsData.forEach { metric ->
            val validData = metric.values.filter { it > 0 }
            if (validData.isNotEmpty()) {
                allValues.addAll(validData)
                totalHigh = maxOf(totalHigh, validData.maxOrNull() ?: 0)
                totalLow = minOf(totalLow, validData.minOrNull() ?: Int.MAX_VALUE)
                totalSum += validData.sum()
                totalCount += validData.size
            }
        }

        if (totalCount == 0) {
            clearData()
            return
        }

        val avg = totalSum / totalCount
        if (totalLow == Int.MAX_VALUE) totalLow = 0

        _stats.value = StressStats(
            rangeMin = totalLow,
            rangeMax = totalHigh,
            currentVal = allValues.lastOrNull() ?: 0,
            currentTime = "",
            high = totalHigh,
            avg = avg,
            low = totalLow
        )

        // Generate weekly curve - average per day
        _chartPoints.value = metricsData.mapIndexed { index, metric ->
            val validData = metric.values.filter { it > 0 }
            val dayAvg = if (validData.isNotEmpty()) validData.average().toInt() else 0
            val xRatio = (index + 1).toFloat() / metricsData.size
            StressPoint(xRatio, dayAvg)
        }.filter { it.value > 0 }
    }

    private fun processMonthlyStress(metricsData: List<MetricData>) {
        // Same logic as weekly
        processWeeklyStress(metricsData)
    }

    private fun convertToCurvePoints(stressData: List<Int>): List<StressPoint> {
        if (stressData.isEmpty()) return emptyList()

        val points = mutableListOf<StressPoint>()

        // Sample every N minutes to get smooth curve (~20 points)
        val sampleInterval = maxOf(1, stressData.size / 20)

        for (i in stressData.indices step sampleInterval) {
            val value = stressData[i]
            if (value > 0) {
                val xRatio = i.toFloat() / stressData.size
                points.add(StressPoint(xRatio, value))
            }
        }

        // Ensure last point is included
        if (points.isNotEmpty() && points.lastOrNull()?.xRatio != 1.0f) {
            val lastValue = stressData.lastOrNull { it > 0 } ?: 0
            if (lastValue > 0) {
                points.add(StressPoint(1.0f, lastValue))
            }
        }

        return points
    }

    // ============ Device Fallback ============

    private fun loadStressFromDevice(offset: Int) {
        viewModelScope.launch {
            try {
                val result = deviceManager.getRecordData(offset)

                if (result is RecordDataResult.Success) {
                    result.stress?.stressSource?.let { stressData ->
                        val validData = stressData.filter { it > 0 }

                        if (validData.isEmpty()) {
                            Timber.w("No stress data for day $offset")
                            clearData()
                            return@let
                        }

                        val high = validData.maxOrNull() ?: 0
                        val low = validData.minOrNull() ?: 0
                        val avg = validData.average().toInt()
                        val current = validData.lastOrNull() ?: 0

                        val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

                        _stats.value = StressStats(
                            rangeMin = low,
                            rangeMax = high,
                            currentVal = current,
                            currentTime = currentTime,
                            high = high,
                            avg = avg,
                            low = low
                        )

                        _chartPoints.value = convertToCurvePoints(stressData)

                        Timber.i("✅ Stress loaded from device: avg=$avg, range=$low-$high")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading stress from device")
                clearData()
            }
        }
    }

    // ============ Clear Data ============

    private fun clearData() {
        _stats.value = StressStats()
        _chartPoints.value = emptyList()
    }

    // ============ Helper ============

    fun getStressLevelLabel(): String {
        val avg = _stats.value.avg
        return when {
            avg == 0 -> ""
            avg <= 25 -> "آرام"
            avg <= 50 -> "عادی"
            avg <= 75 -> "متوسط"
            else -> "بالا"
        }
    }
}