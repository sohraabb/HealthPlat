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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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

    data class StepBarPoint(val hourLabel: String, val steps: Int, val isSelected: Boolean = false)

    private val _barChartData = MutableStateFlow<List<StepBarPoint>>(emptyList())
    val barChartData: StateFlow<List<StepBarPoint>> = _barChartData.asStateFlow()

    data class ComparisonPoint(val timeRatio: Float, val todaySteps: Int, val avgSteps: Int)

    private val _comparisonData = MutableStateFlow<List<ComparisonPoint>>(emptyList())
    val comparisonData: StateFlow<List<ComparisonPoint>> = _comparisonData.asStateFlow()

    private val _totalSteps = MutableStateFlow(0)
    val totalSteps: StateFlow<Int> = _totalSteps.asStateFlow()

    private val _selectedTimeRange = MutableStateFlow("روزانه")
    val selectedTimeRange: StateFlow<String> = _selectedTimeRange.asStateFlow()

    private val _selectedDayOffset = MutableStateFlow(0)
    val selectedDayOffset = _selectedDayOffset.asStateFlow()

    // ── Loading: server → UI ──────────────────────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── Syncing: device → server ──────────────────────────────────────────────
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // ── One-time snackbar events ──────────────────────────────────────────────
    private val _syncMessage = Channel<String>(Channel.BUFFERED)
    val syncMessage = _syncMessage.receiveAsFlow()

    private val _dateLabel = MutableStateFlow("")
    val dateLabel: StateFlow<String> = _dateLabel.asStateFlow()

    init {
        loadStepsFromApi()
    }

    // ── Public actions ────────────────────────────────────────────────────────

    fun refreshData() {
        viewModelScope.launch {
            // Phase 1 — Device → Server
            _isSyncing.value = true
            try {
                when (val result = healthRepository.syncDashboardData(day = 0)) {
                    is RecordDataResult.Success ->
                        _syncMessage.trySend("تعداد قدم به‌روز شد ✓")
                    is RecordDataResult.Error ->
                        _syncMessage.trySend("دستگاه متصل نیست — نمایش آخرین داده")
                }
            } catch (e: Exception) {
                Timber.w(e, "Steps sync failed")
                _syncMessage.trySend("خطا در همگام‌سازی")
            } finally {
                _isSyncing.value = false
            }

            // Phase 2 — Reload (always runs)
            loadStepsFromApi()
        }
    }

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
        loadStepsFromApi()
    }

    fun selectDay(offset: Int) {
        _selectedDayOffset.value = offset
        if (_selectedTimeRange.value == "روزانه") loadStepsFromApi()
    }

    // ── Private: data loading ─────────────────────────────────────────────────

    private fun loadStepsFromApi() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val (dateFrom, dateTo) = getDateRange()
                Timber.i("📡 Fetching steps: $dateFrom → $dateTo")

                healthRepository.getMetricData(MetricType.STEPS, dateFrom, dateTo).fold(
                    onSuccess = { metricsData ->
                        if (metricsData.isNotEmpty()) processStepsData(metricsData)
                        else { Timber.w("⚠️ No steps data"); clearData() }
                    },
                    onFailure = { e ->
                        Timber.e(e, "❌ Steps API error")
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

    private fun getDateRange(): Pair<String, String> {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        return when (_selectedTimeRange.value) {
            "روزانه" -> {
                val targetDate = today.plusDays(_selectedDayOffset.value.toLong())
                updateDateLabel(targetDate)
                targetDate.format(formatter).let { Pair(it, it) }
            }
            "هفتگی" -> {
                _dateLabel.value = "هفته گذشته"
                Pair(today.minusDays(6).format(formatter), today.format(formatter))
            }
            "ماهانه" -> {
                _dateLabel.value = "ماه گذشته"
                Pair(today.minusDays(29).format(formatter), today.format(formatter))
            }
            else -> today.format(formatter).let { Pair(it, it) }
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
                "${pDate.shDay.toString().toFarsiDigits()} ${pDate.monthName}"
            }
        }
    }

    // ── Data processing ───────────────────────────────────────────────────────

    private fun processStepsData(metricsData: List<MetricData>) {
        when (_selectedTimeRange.value) {
            "روزانه" -> processDailySteps(metricsData)
            "هفتگی" -> processWeeklySteps(metricsData)
            "ماهانه" -> processMonthlySteps(metricsData)
        }
    }

    private fun processDailySteps(metricsData: List<MetricData>) {
        val allValues = metricsData.flatMap { it.values }
        if (allValues.isEmpty()) { clearData(); return }

        val total = allValues.maxOrNull() ?: 0
        _totalSteps.value = total
        _todaySteps.value = total
        _barChartData.value = convertToHourlyBars(allValues)
        loadComparisonDataFromApi()
    }

    private fun processWeeklySteps(metricsData: List<MetricData>) {
        if (metricsData.isEmpty()) { clearData(); return }
        val dailyTotals = metricsData.map { it.values.maxOrNull() ?: 0 }
        _totalSteps.value = dailyTotals.sum()
        _averageSteps.value = dailyTotals.average().toInt()
        _weeklySteps.value = dailyTotals
        _barChartData.value = metricsData.mapIndexed { index, metric ->
            val dayTotal = metric.values.maxOrNull() ?: 0
            val dayLabel = try {
                getDayOfWeekShort(LocalDate.parse(metric.recordDate.substring(0, 10)))
            } catch (e: Exception) { "Day ${index + 1}" }
            StepBarPoint(dayLabel, dayTotal, isSelected = index == metricsData.lastIndex)
        }
        updateWeeklyComparison(dailyTotals)
    }

    private fun processMonthlySteps(metricsData: List<MetricData>) {
        if (metricsData.isEmpty()) { clearData(); return }
        val dailyTotals = metricsData.map { it.values.maxOrNull() ?: 0 }
        _totalSteps.value = dailyTotals.sum()
        _averageSteps.value = dailyTotals.average().toInt()
        val weeklyGroups = dailyTotals.chunked(7)
        _barChartData.value = weeklyGroups.mapIndexed { index, weekData ->
            StepBarPoint("هفته ${index + 1}", weekData.sum(), isSelected = index == weeklyGroups.lastIndex)
        }
        updateMonthlyComparison(dailyTotals)
    }

    private fun convertToHourlyBars(stepsData: List<Int>): List<StepBarPoint> {
        val bars = mutableListOf<StepBarPoint>()
        var previousTotal = 0
        for (hour in 0..23) {
            val startIdx = hour * 60
            val endIdx = minOf(startIdx + 60, stepsData.size)
            if (startIdx >= stepsData.size) break
            val hourEndValue = stepsData.subList(startIdx, endIdx).maxOrNull() ?: 0
            val hourSteps = (hourEndValue - previousTotal).coerceAtLeast(0)
            previousTotal = hourEndValue
            if (hourSteps > 0) bars.add(StepBarPoint(String.format("%02d:00", hour), hourSteps))
        }
        if (bars.isEmpty()) return bars
        val maxIndex = bars.indices.maxByOrNull { bars[it].steps } ?: 0
        return bars.mapIndexed { index, point -> point.copy(isSelected = index == maxIndex) }
    }

    // ── Comparison data ───────────────────────────────────────────────────────

    private fun loadComparisonDataFromApi() {
        viewModelScope.launch {
            try {
                val today = LocalDate.now()
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                healthRepository.getMetricData(
                    MetricType.STEPS,
                    today.minusDays(7).format(formatter),
                    today.minusDays(1).format(formatter)
                ).fold(
                    onSuccess = { metricsData ->
                        val avgData = metricsData.mapNotNull { it.values.maxOrNull() }
                        val avgTotal = if (avgData.isNotEmpty()) avgData.average().toInt() else 0
                        _averageSteps.value = avgTotal
                        val todayTotal = _todaySteps.value
                        _comparisonData.value = listOf(
                            ComparisonPoint(0f, 0, 0),
                            ComparisonPoint(0.25f, todayTotal / 4, avgTotal / 4),
                            ComparisonPoint(0.5f, todayTotal / 2, avgTotal / 2),
                            ComparisonPoint(0.75f, (todayTotal * 3) / 4, (avgTotal * 3) / 4),
                            ComparisonPoint(1.0f, todayTotal, avgTotal)
                        )
                    },
                    onFailure = { Timber.e(it, "Failed to load comparison data") }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading comparison data")
            }
        }
    }

    private fun updateWeeklyComparison(dailyTotals: List<Int>) {
        val avg = if (dailyTotals.isNotEmpty()) dailyTotals.average().toInt() else 0
        _comparisonData.value = dailyTotals.mapIndexed { index, steps ->
            ComparisonPoint((index + 1).toFloat() / dailyTotals.size, steps, avg)
        }
    }

    private fun updateMonthlyComparison(dailyTotals: List<Int>) {
        val avg = if (dailyTotals.isNotEmpty()) dailyTotals.average().toInt() else 0
        val sampleIndices = listOf(0, 7, 14, 21, dailyTotals.lastIndex).filter { it < dailyTotals.size }
        _comparisonData.value = sampleIndices.map { index ->
            ComparisonPoint(
                (index + 1).toFloat() / dailyTotals.size,
                dailyTotals.take(index + 1).sum(),
                avg * (index + 1)
            )
        }
    }

    private fun getDayOfWeekShort(date: LocalDate) = when (date.dayOfWeek.value) {
        1 -> "د"; 2 -> "س"; 3 -> "چ"; 4 -> "پ"
        5 -> "ج"; 6 -> "ش"; 7 -> "ی"; else -> ""
    }

    // ── Device fallback ───────────────────────────────────────────────────────

    private fun loadStepsFromDevice(offset: Int) {
        viewModelScope.launch {
            try {
                val result = deviceManager.getRecordData(offset)
                if (result is RecordDataResult.Success) {
                    val validData = result.steps?.stepSource?.filter { it >= 0 } ?: return@launch
                    if (validData.isEmpty()) return@launch
                    val total = validData.maxOrNull() ?: 0
                    _totalSteps.value = total
                    _todaySteps.value = total
                    _barChartData.value = convertToHourlyBars(validData)
                    loadComparisonDataFromDevice(offset)
                    Timber.i("✅ Steps from device: $total steps")
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
            Timber.e(e, "Error loading comparison from device")
        }
    }

    // ── Clear ─────────────────────────────────────────────────────────────────

    private fun clearData() {
        _totalSteps.value = 0
        _todaySteps.value = 0
        _averageSteps.value = 0
        _barChartData.value = emptyList()
        _comparisonData.value = emptyList()
        _weeklySteps.value = emptyList()
    }
}