package com.bonyad.healthplat.ui.dashboard.details.stepts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.data.repository.HealthDataRepository
import com.bonyad.healthplat.data.repository.MetricType
import com.bonyad.healthplat.domain.model.MetricData
import com.bonyad.healthplat.domain.model.RecordDataResult
import com.bonyad.healthplat.ui.utils.PersianDateUtils
import com.bonyad.healthplat.ui.utils.rtl
import com.bonyad.healthplat.ui.utils.toFarsiDigits
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import saman.zamani.persiandate.PersianDate
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.GregorianCalendar
import javax.inject.Inject
import com.bonyad.healthplat.ui.components.PersianDate as UiPersianDate

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
        val isSelected: Boolean = false,
        val timePosition: Float = 0f
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

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Dynamic comparison labels
    private val _orangeLabel = MutableStateFlow("امروز")
    val orangeLabel: StateFlow<String> = _orangeLabel.asStateFlow()

    private val _greyLabel = MutableStateFlow("میانگین")
    val greyLabel: StateFlow<String> = _greyLabel.asStateFlow()

    private val _orangeValue = MutableStateFlow(0)
    val orangeValue: StateFlow<Int> = _orangeValue.asStateFlow()

    private val _greyValue = MutableStateFlow(0)
    val greyValue: StateFlow<Int> = _greyValue.asStateFlow()

    // Cached today's minute-level data for comparison reuse
    private var cachedTodayMinutes: List<Int> = emptyList()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Date label for display
    private val _dateLabel = MutableStateFlow("")
    val dateLabel: StateFlow<String> = _dateLabel.asStateFlow()

    init {
        loadStepsFromApi()
    }

    // ============ Sync: Ring → Server → API fetch → Display ============

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val syncDay = ChronoUnit.DAYS.between(_selectedDate.value, LocalDate.now()).toInt().coerceIn(0, 6)
                healthRepository.syncDashboardData(syncDay)
                Timber.i("✅ Ring steps data synced to server for day $syncDay")
            } catch (e: Exception) {
                Timber.e(e, "⚠️ Ring→Server sync failed, loading from API anyway")
            }
            loadStepsFromApi()
        }
    }

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
        loadStepsFromApi()
    }

    fun selectDay(date: LocalDate) {
        _selectedDate.value = date
        if (_selectedTimeRange.value == "روزانه") {
            loadStepsFromApi()
        }
    }

    fun selectDate(date: UiPersianDate) {
        _selectedDate.value = LocalDate.parse(date.toGregorianIsoDate())
        _selectedTimeRange.value = "روزانه"
        loadStepsFromApi()
    }

    // ============ Date Range ============

    private fun getDateRange(): Pair<String, String> {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        return when (_selectedTimeRange.value) {
            "روزانه" -> {
                val dateStr = _selectedDate.value.format(formatter)
                updateDateLabel(_selectedDate.value)
                Pair(dateStr, dateStr)
            }
            "هفتگی" -> {
                val startDate = today.minusDays(6)
                _dateLabel.value = "\u200F۷ روز اخیر"
                Pair(startDate.format(formatter), today.format(formatter))
            }
            "ماهانه" -> {
                val startDate = today.minusDays(27)
                _dateLabel.value = "\u200F۴ هفته اخیر"
                Pair(startDate.format(formatter), today.format(formatter))
            }
            else -> {
                val dateStr = today.format(formatter)
                Pair(dateStr, dateStr)
            }
        }
    }

    // ============ Standardized Date Label ============

    private fun updateDateLabel(date: LocalDate) {
        val today = LocalDate.now()
        val calendar = GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth)
        val pDate = PersianDate(calendar.time)
        val dayOfMonth = pDate.shDay.toString().toFarsiDigits()
        val monthName = pDate.monthName

        _dateLabel.value = when {
            date == today -> "امروز $dayOfMonth $monthName".rtl()
            date == today.minusDays(1) -> "دیروز $dayOfMonth $monthName".rtl()
            else -> "$dayOfMonth $monthName".rtl()
        }
    }

    // ============ PRIMARY: Load from API ============

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
                        loadStepsFromDevice(ChronoUnit.DAYS.between(_selectedDate.value, LocalDate.now()).toInt().coerceIn(0, 6))
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to load steps from API")
                loadStepsFromDevice(ChronoUnit.DAYS.between(_selectedDate.value, LocalDate.now()).toInt().coerceIn(0, 6))
            } finally {
                _isLoading.value = false
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

        val total = allValues.sum()
        _totalSteps.value = total
        _todaySteps.value = total

        _barChartData.value = convertToHourlyBarsPerHour(allValues)

        // Cache for comparison reuse
        cachedTodayMinutes = allValues

        loadDailyComparisonTodayVsYesterday()
    }

    private fun processWeeklySteps(metricsData: List<MetricData>) {
        if (metricsData.isEmpty()) {
            clearData()
            return
        }

        // Build 7-day list: oldest on the left, today on the right (RTL-friendly)
        val today = LocalDate.now()
        val persianLabels = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
        val todayPersianIndex = when (today.dayOfWeek.value) {
            6 -> 0; 7 -> 1; else -> today.dayOfWeek.value + 1
        }
        val dailyTotals = mutableListOf<Int>()
        val barPoints = (6 downTo 0).mapIndexed { listIndex, i ->
            val date = today.minusDays(i.toLong())
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val metric = metricsData.find { it.recordDate.startsWith(dateStr) }
            val dayTotal = metric?.values?.sum() ?: 0
            dailyTotals.add(dayTotal)

            StepBarPoint(
                hourLabel = persianLabels[(todayPersianIndex - i + 7) % 7],
                steps = dayTotal,
                isSelected = i == 0,
                timePosition = listIndex.toFloat() / 6f
            )
        }

        val total = dailyTotals.sum()
        val average = if (dailyTotals.isNotEmpty()) dailyTotals.average().toInt() else 0

        _totalSteps.value = total
        _averageSteps.value = average
        _weeklySteps.value = dailyTotals
        _barChartData.value = barPoints

        loadLastWeekAndCompare(dailyTotals)
    }

    private fun processMonthlySteps(metricsData: List<MetricData>) {
        if (metricsData.isEmpty()) {
            clearData()
            return
        }

        // Build list for last 28 days, chunked into 4 weeks (oldest first)
        val today = LocalDate.now()
        val startDate = today.minusDays(27)
        val dailyTotals = (0 until 28).map { dayOffset ->
            val date = startDate.plusDays(dayOffset.toLong())
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val metric = metricsData.find { it.recordDate.startsWith(dateStr) }
            metric?.values?.sum() ?: 0
        }

        val total = dailyTotals.sum()
        val average = if (dailyTotals.isNotEmpty()) dailyTotals.average().toInt() else 0

        _totalSteps.value = total
        _averageSteps.value = average

        val weeklyGroups = dailyTotals.chunked(7)
        _barChartData.value = weeklyGroups.mapIndexed { index, weekData ->
            StepBarPoint(
                hourLabel = "هفته ${(index + 1).toString().toFarsiDigits()}",
                steps = weekData.sum(),
                isSelected = index == weeklyGroups.lastIndex,
                timePosition = if (weeklyGroups.size > 1) index.toFloat() / (weeklyGroups.size - 1) else 0.5f
            )
        }

        loadLastMonthAndCompare(dailyTotals)
    }

    /** Per-half-hour breakdown: each bar = steps walked during that 30-min slot */
    private fun convertToHourlyBarsPerHour(stepsData: List<Int>): List<StepBarPoint> {
        val bars = mutableListOf<StepBarPoint>()
        val minutesPerSlot = 30
        val totalSlots = 48

        for (slot in 0 until totalSlots) {
            val startIdx = slot * minutesPerSlot
            val endIdx = minOf(startIdx + minutesPerSlot, stepsData.size)

            val slotSteps = if (startIdx < stepsData.size) {
                stepsData.subList(startIdx, endIdx).sum()
            } else {
                0
            }

            val hour = slot / 2
            val minute = (slot % 2) * 30

            bars.add(
                StepBarPoint(
                    hourLabel = String.format("%02d:%02d", hour, minute),
                    steps = slotSteps,
                    isSelected = false,
                    timePosition = slot / 47f
                )
            )
        }

        // Mark the slot with most steps as selected (for tooltip)
        if (bars.any { it.steps > 0 }) {
            val maxIndex = bars.indices.maxByOrNull { bars[it].steps } ?: 0
            return bars.mapIndexed { index, point ->
                point.copy(isSelected = index == maxIndex)
            }
        }

        return bars
    }

    /** Cumulative: each bar = total steps up to that slot (last bar = daily total) */
//    private fun convertToCumulativeBarsPerHalfHour(stepsData: List<Int>): List<StepBarPoint> {
//        val bars = mutableListOf<StepBarPoint>()
//        var runningTotal = 0
//        val minutesPerSlot = 30
//        val totalSlots = 48
//
//        for (slot in 0 until totalSlots) {
//            val startIdx = slot * minutesPerSlot
//            val endIdx = minOf(startIdx + minutesPerSlot, stepsData.size)
//
//            if (startIdx < stepsData.size) {
//                runningTotal += stepsData.subList(startIdx, endIdx).sum()
//            }
//
//            val hour = slot / 2
//            val minute = (slot % 2) * 30
//
//            bars.add(
//                StepBarPoint(
//                    hourLabel = String.format("%02d:%02d", hour, minute),
//                    steps = runningTotal,
//                    isSelected = false,
//                    timePosition = slot / 47f
//                )
//            )
//        }
//
//        // Mark the last non-zero bar as selected (for tooltip)
//        if (bars.any { it.steps > 0 }) {
//            val lastActiveIndex = bars.indexOfLast { it.steps > 0 }
//            return bars.mapIndexed { index, point ->
//                point.copy(isSelected = index == lastActiveIndex)
//            }
//        }
//
//        return bars
//    }

    // ============ Comparison Data ============

    // --- Daily: Today vs Yesterday ---

    private fun loadDailyComparisonTodayVsYesterday() {
        viewModelScope.launch {
            _orangeLabel.value = "امروز"
            _greyLabel.value = "دیروز"
            try {
                val yesterday = _selectedDate.value.minusDays(1)
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                val dateStr = yesterday.format(formatter)

                healthRepository.getMetricData(
                    metricType = MetricType.STEPS,
                    dateFrom = dateStr,
                    dateTo = dateStr
                ).fold(
                    onSuccess = { metricsData ->
                        val yesterdayMinutes = metricsData.flatMap { it.values }
                        buildDailyComparisonPoints(cachedTodayMinutes, yesterdayMinutes)
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to load yesterday comparison data")
                        buildDailyComparisonPoints(cachedTodayMinutes, emptyList())
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading daily comparison data")
                buildDailyComparisonPoints(cachedTodayMinutes, emptyList())
            }
        }
    }

    /**
     * Builds hourly cumulative comparison points from minute-level data.
     * Yesterday always shows full 24 hours.
     * Today stops at the current hour (todaySteps = -1 beyond current hour).
     */
    private fun buildDailyComparisonPoints(todayMinutes: List<Int>, yesterdayMinutes: List<Int>) {
        val isToday = _selectedDate.value == LocalDate.now()
        val todayLastHour = if (isToday) LocalTime.now().hour else 23
        val points = mutableListOf<ComparisonPoint>()

        var todayCumulative = 0
        var yesterdayCumulative = 0

        for (hour in 0..23) {
            val startIdx = hour * 60
            val endIdx = minOf(startIdx + 60, 1440)

            // Yesterday: always full 24 hours
            if (startIdx < yesterdayMinutes.size) {
                yesterdayCumulative += yesterdayMinutes.subList(startIdx, minOf(endIdx, yesterdayMinutes.size)).sum()
            }

            // Today: only up to current hour, -1 beyond
            val todayValue = if (hour <= todayLastHour) {
                if (startIdx < todayMinutes.size) {
                    todayCumulative += todayMinutes.subList(startIdx, minOf(endIdx, todayMinutes.size)).sum()
                }
                todayCumulative
            } else {
                -1
            }

            points.add(
                ComparisonPoint(
                    timeRatio = hour / 23f,
                    todaySteps = todayValue,
                    avgSteps = yesterdayCumulative
                )
            )
        }

        _comparisonData.value = points
        _orangeValue.value = todayCumulative
        _greyValue.value = yesterdayCumulative
        _averageSteps.value = yesterdayCumulative
    }

    // --- Weekly: This week vs Last week ---

    /** Boring fallback: this week's daily totals vs flat average line */
    private fun updateWeeklyComparison(dailyTotals: List<Int>) {
        val avg = if (dailyTotals.isNotEmpty()) dailyTotals.average().toInt() else 0
        _orangeLabel.value = "این هفته"
        _greyLabel.value = "میانگین"

        _comparisonData.value = dailyTotals.mapIndexed { index, steps ->
            val ratio = (index + 1).toFloat() / dailyTotals.size
            ComparisonPoint(ratio, steps, avg)
        }
        _orangeValue.value = dailyTotals.sum()
        _greyValue.value = avg
    }

    private fun updateWeeklyComparisonVsLastWeek(thisWeek: List<Int>, lastWeek: List<Int>) {
        _orangeLabel.value = "این هفته"
        _greyLabel.value = "هفته گذشته"

        val points = (0 until 7).map { i ->
            ComparisonPoint(
                timeRatio = if (6 > 0) i / 6f else 0f,
                todaySteps = thisWeek.getOrElse(i) { 0 },
                avgSteps = lastWeek.getOrElse(i) { 0 }
            )
        }
        _comparisonData.value = points

        val thisTotal = thisWeek.sum()
        val lastTotal = lastWeek.sum()
        _orangeValue.value = thisTotal
        _greyValue.value = lastTotal
        _averageSteps.value = if (lastWeek.isNotEmpty()) lastWeek.average().toInt() else 0
    }

    private fun loadLastWeekAndCompare(thisWeekTotals: List<Int>) {
        viewModelScope.launch {
            try {
                val today = LocalDate.now()
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                // Previous 7 days = today-13 .. today-7
                val dateFrom = today.minusDays(13).format(formatter)
                val dateTo = today.minusDays(7).format(formatter)

                healthRepository.getMetricData(
                    metricType = MetricType.STEPS,
                    dateFrom = dateFrom,
                    dateTo = dateTo
                ).fold(
                    onSuccess = { metricsData ->
                        // Match the same order: position i = 7 days before this week's position i
                        val lastWeekTotals = (0..6).map { i ->
                            val date = today.minusDays((i + 7).toLong())
                            val dateStr = date.format(formatter)
                            val metric = metricsData.find { it.recordDate.startsWith(dateStr) }
                            metric?.values?.sum() ?: 0
                        }
                        updateWeeklyComparisonVsLastWeek(thisWeekTotals, lastWeekTotals)
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to load last week comparison, using fallback")
                        updateWeeklyComparison(thisWeekTotals)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading last week comparison, using fallback")
                updateWeeklyComparison(thisWeekTotals)
            }
        }
    }

    // --- Monthly: This 4 weeks vs Previous 4 weeks ---

    private fun loadLastMonthAndCompare(thisMonthDailyTotals: List<Int>) {
        viewModelScope.launch {
            try {
                val today = LocalDate.now()
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                // Previous 4 weeks = today-55 .. today-28
                val dateFrom = today.minusDays(55).format(formatter)
                val dateTo = today.minusDays(28).format(formatter)

                healthRepository.getMetricData(
                    metricType = MetricType.STEPS,
                    dateFrom = dateFrom,
                    dateTo = dateTo
                ).fold(
                    onSuccess = { metricsData ->
                        val lastMonthStart = today.minusDays(55)
                        val lastMonthDailyTotals = (0 until 28).map { dayOffset ->
                            val date = lastMonthStart.plusDays(dayOffset.toLong())
                            val dateStr = date.format(formatter)
                            val metric = metricsData.find { it.recordDate.startsWith(dateStr) }
                            metric?.values?.sum() ?: 0
                        }
                        updateMonthlyComparisonVsLastMonth(thisMonthDailyTotals, lastMonthDailyTotals)
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to load last month comparison, using fallback")
                        updateMonthlyComparisonFallback(thisMonthDailyTotals)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading last month comparison, using fallback")
                updateMonthlyComparisonFallback(thisMonthDailyTotals)
            }
        }
    }

    private fun updateMonthlyComparisonVsLastMonth(thisMonthDaily: List<Int>, lastMonthDaily: List<Int>) {
        _orangeLabel.value = "این ماه"
        _greyLabel.value = "ماه گذشته"

        val thisWeeks = thisMonthDaily.chunked(7).map { it.sum() }
        val lastWeeks = lastMonthDaily.chunked(7).map { it.sum() }

        _comparisonData.value = (0 until 4).map { i ->
            ComparisonPoint(
                timeRatio = if (3 > 0) i / 3f else 0f,
                todaySteps = thisWeeks.getOrElse(i) { 0 },
                avgSteps = lastWeeks.getOrElse(i) { 0 }
            )
        }

        _orangeValue.value = thisWeeks.sum()
        _greyValue.value = lastWeeks.sum()
        _averageSteps.value = if (lastWeeks.isNotEmpty()) lastWeeks.average().toInt() else 0
    }

    /** Fallback: this month's weekly totals vs average */
    private fun updateMonthlyComparisonFallback(dailyTotals: List<Int>) {
        val weeklyTotals = dailyTotals.chunked(7).map { it.sum() }
        val avg = if (weeklyTotals.isNotEmpty()) weeklyTotals.average().toInt() else 0
        _orangeLabel.value = "این ماه"
        _greyLabel.value = "میانگین"

        _comparisonData.value = weeklyTotals.mapIndexed { index, total ->
            ComparisonPoint(
                timeRatio = if (weeklyTotals.size > 1) index / (weeklyTotals.size - 1f) else 0f,
                todaySteps = total,
                avgSteps = avg
            )
        }
        _orangeValue.value = weeklyTotals.sum()
        _greyValue.value = avg
        _averageSteps.value = avg
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

                        val total = validData.sum()
                        _totalSteps.value = total
                        _todaySteps.value = total
                        cachedTodayMinutes = validData

                        _barChartData.value = convertToHourlyBarsPerHour(validData)

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
        _orangeLabel.value = "امروز"
        _greyLabel.value = "دیروز"
        try {
            val yesterdayResult = deviceManager.getRecordData(currentDayOffset + 1)
            val yesterdayMinutes = if (yesterdayResult is RecordDataResult.Success) {
                yesterdayResult.steps?.stepSource?.filter { it >= 0 } ?: emptyList()
            } else {
                emptyList()
            }
            buildDailyComparisonPoints(cachedTodayMinutes, yesterdayMinutes)
        } catch (e: Exception) {
            Timber.e(e, "Error loading comparison data from device")
            buildDailyComparisonPoints(cachedTodayMinutes, emptyList())
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
        _orangeLabel.value = "امروز"
        _greyLabel.value = "میانگین"
        _orangeValue.value = 0
        _greyValue.value = 0
        cachedTodayMinutes = emptyList()
    }
}