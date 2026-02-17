package com.bonyad.healthplat.ui.dashboard.details.stepts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.data.repository.HealthDataRepository
import com.bonyad.healthplat.data.repository.MetricType
import com.bonyad.healthplat.domain.model.MetricData
import com.bonyad.healthplat.domain.model.RecordDataResult
import com.bonyad.healthplat.ui.utils.toFarsiDigits
import dagger.hilt.android.lifecycle.HiltViewModel
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

@HiltViewModel
class StepsDetailViewModel @Inject constructor(
    private val deviceManager: HealthDeviceManager,
    private val healthRepository: HealthDataRepository
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
        val timeRatio: Float,
        val todaySteps: Int,
        val avgSteps: Int
    )

    private val _comparisonData = MutableStateFlow<List<ComparisonPoint>>(emptyList())
    val comparisonData: StateFlow<List<ComparisonPoint>> = _comparisonData.asStateFlow()

    private val _totalSteps = MutableStateFlow(0)
    val totalSteps: StateFlow<Int> = _totalSteps.asStateFlow()

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
        loadStepsFromApi()
    }

    fun refreshData() {
        loadStepsFromApi()
    }

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
        loadStepsFromApi()
    }

    fun selectDay(offset: Int) {
        _selectedDayOffset.value = offset
        if (_selectedTimeRange.value == "روزانه") {
            loadStepsFromApi()
        }
    }

    // ============ API Integration ============

    private fun loadStepsFromApi() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val (dateFrom, dateTo) = getDateRange()

                Timber.i("📡 Fetching steps: $dateFrom to $dateTo")

                healthRepository.getMetricData(
                    metricType = MetricType.STEPS,
                    dateFrom = dateFrom,
                    dateTo = dateTo
                ).fold(
                    onSuccess = { metricsData ->
                        if (metricsData.isNotEmpty()) {
                            processStepsData(metricsData)
                            Timber.i("✅ Steps loaded: ${metricsData.size} records")
                        } else {
                            Timber.w("⚠️ No steps data from API")
                            clearData()
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "❌ Steps API error")
                        // Fallback to device
                        loadStepsFromDevice(_selectedDayOffset.value)
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to load steps from API")
                loadStepsFromDevice(_selectedDayOffset.value)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Get date range based on selected time range
     * - روزانه: Single day
     * - هفتگی: Last 7 days
     * - ماهانه: Last 30 days
     */
    private fun getDateRange(): Pair<String, String> {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE // yyyy-MM-dd

        return when (_selectedTimeRange.value) {
            "روزانه" -> {
                val targetDate = today.plusDays(_selectedDayOffset.value.toLong())
                val dateStr = targetDate.format(formatter)
                updateDateLabel(targetDate)
                Pair(dateStr, dateStr)
            }
            "هفتگی" -> {
                // Last 7 days
                val dateFrom = today.minusDays(6).format(formatter)
                val dateTo = today.format(formatter)
                _dateLabel.value = "هفته گذشته"
                Pair(dateFrom, dateTo)
            }
            "ماهانه" -> {
                // Last 30 days
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
            else -> {
                val calendar = GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth)
                val pDate = PersianDate(calendar.time)
                val dayOfMonth = pDate.shDay.toString().toFarsiDigits()
                val monthName = pDate.monthName
                "$dayOfMonth $monthName"
            }
        }
    }

    // ============ Data Processing ============

    private fun processStepsData(metricsData: List<MetricData>) {
        when (_selectedTimeRange.value) {
            "روزانه" -> processDailySteps(metricsData)
            "هفتگی" -> processWeeklySteps(metricsData)
            "ماهانه" -> processMonthlySteps(metricsData)
        }
    }

    private fun processDailySteps(metricsData: List<MetricData>) {
        val allValues = metricsData.flatMap { it.values }

        if (allValues.isEmpty()) {
            clearData()
            return
        }

        // Total is the max value (cumulative steps)
        val total = allValues.maxOrNull() ?: 0
        _totalSteps.value = total
        _todaySteps.value = total

        // Convert to hourly bars
        _barChartData.value = convertToHourlyBars(allValues)

        // Load comparison data
        loadComparisonDataFromApi()
    }

    private fun processWeeklySteps(metricsData: List<MetricData>) {
        if (metricsData.isEmpty()) {
            clearData()
            return
        }

        // Get daily totals
        val dailyTotals = metricsData.map { metric ->
            metric.values.maxOrNull() ?: 0
        }

        val total = dailyTotals.sum()
        val average = if (dailyTotals.isNotEmpty()) dailyTotals.average().toInt() else 0

        _totalSteps.value = total
        _averageSteps.value = average
        _weeklySteps.value = dailyTotals

        // Convert to daily bars for weekly view
        _barChartData.value = metricsData.mapIndexed { index, metric ->
            val dayTotal = metric.values.maxOrNull() ?: 0
            val dayLabel = try {
                val date = LocalDate.parse(metric.recordDate.substring(0, 10))
                getDayOfWeekShort(date)
            } catch (e: Exception) {
                "Day ${index + 1}"
            }

            StepBarPoint(
                hourLabel = dayLabel,
                steps = dayTotal,
                isSelected = index == metricsData.lastIndex
            )
        }

        // Update comparison
        updateWeeklyComparison(dailyTotals)
    }

    private fun processMonthlySteps(metricsData: List<MetricData>) {
        if (metricsData.isEmpty()) {
            clearData()
            return
        }

        val dailyTotals = metricsData.map { metric ->
            metric.values.maxOrNull() ?: 0
        }

        val total = dailyTotals.sum()
        val average = if (dailyTotals.isNotEmpty()) dailyTotals.average().toInt() else 0

        _totalSteps.value = total
        _averageSteps.value = average

        // Group by week for monthly view
        val weeklyGroups = dailyTotals.chunked(7)
        _barChartData.value = weeklyGroups.mapIndexed { index, weekData ->
            StepBarPoint(
                hourLabel = "هفته ${index + 1}",
                steps = weekData.sum(),
                isSelected = index == weeklyGroups.lastIndex
            )
        }

        updateMonthlyComparison(dailyTotals)
    }

    private fun convertToHourlyBars(stepsData: List<Int>): List<StepBarPoint> {
        val bars = mutableListOf<StepBarPoint>()
        var previousTotal = 0

        for (hour in 0..23) {
            val startIdx = hour * 60
            val endIdx = minOf(startIdx + 60, stepsData.size)

            if (startIdx < stepsData.size) {
                // Get cumulative value at end of hour
                val hourEndValue = stepsData.subList(startIdx, endIdx).maxOrNull() ?: 0
                // Steps for this hour = current - previous
                val hourSteps = (hourEndValue - previousTotal).coerceAtLeast(0)
                previousTotal = hourEndValue

                if (hourSteps > 0) {
                    bars.add(
                        StepBarPoint(
                            hourLabel = String.format("%02d:00", hour),
                            steps = hourSteps,
                            isSelected = false
                        )
                    )
                }
            }
        }

        // Mark the highest bar as selected (for tooltip)
        if (bars.isNotEmpty()) {
            val maxIndex = bars.indices.maxByOrNull { bars[it].steps } ?: 0
            return bars.mapIndexed { index, point ->
                point.copy(isSelected = index == maxIndex)
            }
        }

        return bars
    }

    // ============ Comparison Data ============

    private fun loadComparisonDataFromApi() {
        viewModelScope.launch {
            try {
                val today = LocalDate.now()
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE

                // Get last 7 days for average
                val dateFrom = today.minusDays(7).format(formatter)
                val dateTo = today.minusDays(1).format(formatter)

                healthRepository.getMetricData(
                    metricType = MetricType.STEPS,
                    dateFrom = dateFrom,
                    dateTo = dateTo
                ).fold(
                    onSuccess = { metricsData ->
                        val avgData = metricsData.mapNotNull { it.values.maxOrNull() }
                        val avgTotal = if (avgData.isNotEmpty()) avgData.average().toInt() else 0
                        _averageSteps.value = avgTotal

                        // Generate comparison points
                        val todayTotal = _todaySteps.value
                        _comparisonData.value = listOf(
                            ComparisonPoint(0f, 0, 0),
                            ComparisonPoint(0.25f, todayTotal / 4, avgTotal / 4),
                            ComparisonPoint(0.5f, todayTotal / 2, avgTotal / 2),
                            ComparisonPoint(0.75f, (todayTotal * 3) / 4, (avgTotal * 3) / 4),
                            ComparisonPoint(1.0f, todayTotal, avgTotal)
                        )
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to load comparison data")
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading comparison data")
            }
        }
    }

    private fun updateWeeklyComparison(dailyTotals: List<Int>) {
        val total = dailyTotals.sum()
        val avg = if (dailyTotals.isNotEmpty()) dailyTotals.average().toInt() else 0

        _comparisonData.value = dailyTotals.mapIndexed { index, steps ->
            val ratio = (index + 1).toFloat() / dailyTotals.size
            ComparisonPoint(ratio, steps, avg)
        }
    }

    private fun updateMonthlyComparison(dailyTotals: List<Int>) {
        val total = dailyTotals.sum()
        val avg = if (dailyTotals.isNotEmpty()) dailyTotals.average().toInt() else 0

        // Sample points for month
        val sampleIndices = listOf(0, 7, 14, 21, dailyTotals.lastIndex).filter { it < dailyTotals.size }
        _comparisonData.value = sampleIndices.map { index ->
            val ratio = (index + 1).toFloat() / dailyTotals.size
            val cumulative = dailyTotals.take(index + 1).sum()
            ComparisonPoint(ratio, cumulative, avg * (index + 1))
        }
    }

    private fun getDayOfWeekShort(date: LocalDate): String {
        return when (date.dayOfWeek.value) {
            1 -> "د"
            2 -> "س"
            3 -> "چ"
            4 -> "پ"
            5 -> "ج"
            6 -> "ش"
            7 -> "ی"
            else -> ""
        }
    }

    // ============ Device Fallback ============

    private fun loadStepsFromDevice(offset: Int) {
        viewModelScope.launch {
            try {
                val result = deviceManager.getRecordData(offset)

                if (result is RecordDataResult.Success) {
                    result.steps?.stepSource?.let { stepsData ->
                        val validData = stepsData.filter { it >= 0 }

                        if (validData.isEmpty()) {
                            Timber.w("No steps data for day $offset")
                            return@let
                        }

                        val total = validData.maxOrNull() ?: 0
                        _totalSteps.value = total
                        _todaySteps.value = total

                        _barChartData.value = convertToHourlyBars(validData)

                        // Load comparison from device
                        loadComparisonDataFromDevice(offset)

                        Timber.i("✅ Steps loaded from device: $total steps")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading steps from device")
                clearData()
            }
        }
    }

    private suspend fun loadComparisonDataFromDevice(currentDayOffset: Int) {
        try {
            val avgData = mutableListOf<Int>()

            for (day in 1..7) {
                val result = deviceManager.getRecordData(currentDayOffset + day)
                if (result is RecordDataResult.Success) {
                    result.steps?.stepSource?.maxOrNull()?.let { avgData.add(it) }
                }
            }

            val avgTotal = if (avgData.isNotEmpty()) avgData.average().toInt() else 0
            _averageSteps.value = avgTotal

            val todayTotal = _todaySteps.value
            _comparisonData.value = listOf(
                ComparisonPoint(0f, 0, 0),
                ComparisonPoint(0.5f, todayTotal / 2, avgTotal / 2),
                ComparisonPoint(1.0f, todayTotal, avgTotal)
            )

        } catch (e: Exception) {
            Timber.e(e, "Error loading comparison data from device")
        }
    }

    // ============ Clear Data ============

    private fun clearData() {
        _totalSteps.value = 0
        _todaySteps.value = 0
        _averageSteps.value = 0
        _barChartData.value = emptyList()
        _comparisonData.value = emptyList()
        _weeklySteps.value = emptyList()
    }
}