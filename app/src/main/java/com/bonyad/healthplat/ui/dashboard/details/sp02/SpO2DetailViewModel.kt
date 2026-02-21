package com.bonyad.healthplat.ui.dashboard.details.sp02

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
import kotlin.math.abs

@HiltViewModel
class SpO2DetailViewModel @Inject constructor(
    private val deviceManager: HealthDeviceManager,
    private val healthRepository: HealthDataRepository
) : ViewModel() {

    private val _currentSpO2 = MutableStateFlow(0)
    val currentSpO2: StateFlow<Int> = _currentSpO2.asStateFlow()

    private val _spo2Data = MutableStateFlow<List<Int>>(emptyList())
    val spo2Data: StateFlow<List<Int>> = _spo2Data.asStateFlow()

    private val _minSpO2 = MutableStateFlow(0)
    val minSpO2: StateFlow<Int> = _minSpO2.asStateFlow()

    private val _maxSpO2 = MutableStateFlow(0)
    val maxSpO2: StateFlow<Int> = _maxSpO2.asStateFlow()

    private val _selectedDayOffset = MutableStateFlow(0)
    val selectedDayOffset = _selectedDayOffset.asStateFlow()

    data class SpO2Point(
        val timeLabel: String,
        val timeRatio: Float,
        val value: Int
    )

    private val _chartData = MutableStateFlow<List<SpO2Point>>(emptyList())
    val chartData: StateFlow<List<SpO2Point>> = _chartData.asStateFlow()

    data class SpO2Stats(
        val high: Int = 0,
        val avg: Int = 0,
        val low: Int = 0,
        val lastValue: Int = 0,
        val lastTime: String = ""
    )

    private val _stats = MutableStateFlow(SpO2Stats())
    val stats: StateFlow<SpO2Stats> = _stats.asStateFlow()

    private val _selectedTimeRange = MutableStateFlow("روزانه")
    val selectedTimeRange: StateFlow<String> = _selectedTimeRange.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _dateLabel = MutableStateFlow("")
    val dateLabel: StateFlow<String> = _dateLabel.asStateFlow()

    private val _rangeText = MutableStateFlow("")
    val rangeText: StateFlow<String> = _rangeText.asStateFlow()

    init {
        loadSpO2Data()
    }

    // ============ Sync: Ring → Server → API fetch → Display ============

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val syncDay = abs(_selectedDayOffset.value)
                healthRepository.syncDashboardData(syncDay)
                Timber.i("✅ Ring SpO2 data synced to server for day $syncDay")
            } catch (e: Exception) {
                Timber.e(e, "⚠️ Ring→Server sync failed, loading from API anyway")
            }
            loadSpO2Data()
        }
    }

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
        loadSpO2Data()
    }

    fun selectDay(offset: Int) {
        _selectedDayOffset.value = offset
        if (_selectedTimeRange.value == "روزانه") {
            loadSpO2Data()
        }
    }

    // ============ Date Range (consistent with all other VMs) ============

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

    // ============ Standardized Date Label ============

    private fun updateDateLabel(date: LocalDate) {
        val today = LocalDate.now()
        val calendar = GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth)
        val pDate = PersianDate(calendar.time)
        val dayOfMonth = pDate.shDay.toString().toFarsiDigits()
        val monthName = pDate.monthName

        _dateLabel.value = when {
            date == today -> "امروز $dayOfMonth $monthName"
            date == today.minusDays(1) -> "دیروز $dayOfMonth $monthName"
            else -> "$dayOfMonth $monthName"
        }
    }

    // ============ PRIMARY: Load from API ============

    private fun loadSpO2Data() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val (dateFrom, dateTo) = getDateRange()
                Timber.i("📡 Fetching SpO2 from API: $dateFrom to $dateTo")

                healthRepository.getMetricData(
                    metricType = MetricType.SPO2,
                    dateFrom = dateFrom,
                    dateTo = dateTo
                ).fold(
                    onSuccess = { metricsData ->
                        if (metricsData.isNotEmpty()) {
                            when (_selectedTimeRange.value) {
                                "روزانه" -> {
                                    val values = metricsData.first().values
                                    val cleanedValues = if (values.size > 48 && values.size % 48 == 0) {
                                        Timber.w("⚠️ API data duplicated (${values.size}), taking first 48")
                                        values.take(48)
                                    } else {
                                        values
                                    }
                                    processSpO2Data(cleanedValues)
                                }
                                "هفتگی", "ماهانه" -> {
                                    processMultiDaySpO2(metricsData)
                                }
                            }
                            Timber.i("✅ SpO2 loaded from API: ${metricsData.size} records")
                        } else {
                            Timber.w("⚠️ No SpO2 data from API, trying ring fallback...")
                            loadSpO2FromRing(_selectedDayOffset.value)
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "❌ SpO2 API error, trying ring fallback...")
                        loadSpO2FromRing(_selectedDayOffset.value)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to load SpO2 from API")
                loadSpO2FromRing(_selectedDayOffset.value)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ============ FALLBACK: Load from Ring ============

    private suspend fun loadSpO2FromRing(offset: Int) {
        try {
            val result = deviceManager.getRecordData(offset)

            if (result is RecordDataResult.Success &&
                result.spo2?.sourceList?.any { it > 0 } == true
            ) {
                val spo2List = result.spo2.sourceList
                Timber.i("✅ SpO2 loaded from ring fallback: ${spo2List.size} values")
                processSpO2Data(spo2List)
            } else {
                Timber.w("⚠️ No SpO2 data from ring either")
                clearData()
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Ring fallback also failed")
            clearData()
        }
    }

    // ============ Process Single Day Data ============

    private fun processSpO2Data(spo2List: List<Int>) {
        val validData = spo2List.filter { it > 0 }

        if (validData.isEmpty()) {
            Timber.w("⚠️ No valid SpO2 data")
            clearData()
            return
        }

        _spo2Data.value = validData
        _currentSpO2.value = validData.lastOrNull() ?: 0
        _minSpO2.value = validData.minOrNull() ?: 0
        _maxSpO2.value = validData.maxOrNull() ?: 0

        _chartData.value = convertToScatterPoints(spo2List)

        val avg = validData.average().toInt()

        val lastValidIndex = spo2List.indexOfLast { it > 0 }
        val lastTime = if (lastValidIndex >= 0) {
            indexToTimeLabel(lastValidIndex, spo2List.size)
        } else ""

        _stats.value = SpO2Stats(
            high = _maxSpO2.value,
            avg = avg,
            low = _minSpO2.value,
            lastValue = _currentSpO2.value,
            lastTime = lastTime
        )

        _rangeText.value = "${_minSpO2.value} - ${_maxSpO2.value}"

        Timber.i("✅ SpO2: ${validData.size} points, avg=$avg, range=${_minSpO2.value}-${_maxSpO2.value}")
    }

    // ============ Process Multi-Day Data (weekly/monthly) ============

    private fun processMultiDaySpO2(metricsData: List<MetricData>) {
        val allValid = metricsData.flatMap { it.values }.filter { it > 0 }

        if (allValid.isEmpty()) {
            clearData()
            return
        }

        _spo2Data.value = allValid
        _currentSpO2.value = allValid.lastOrNull() ?: 0
        _minSpO2.value = allValid.minOrNull() ?: 0
        _maxSpO2.value = allValid.maxOrNull() ?: 0

        val avg = allValid.average().toInt()

        // Build scatter points distributed across days
        val points = mutableListOf<SpO2Point>()
        metricsData.forEachIndexed { dayIndex, metric ->
            val dayValues = metric.values
            val totalPointsInDay = dayValues.size

            dayValues.forEachIndexed { pointIndex, value ->
                if (value > 0) {
                    // Distribute points across the full chart width
                    val dayStart = dayIndex.toFloat() / metricsData.size
                    val dayWidth = 1f / metricsData.size
                    val withinDayRatio = pointIndex.toFloat() / totalPointsInDay.coerceAtLeast(1)
                    val ratio = dayStart + (withinDayRatio * dayWidth)

                    points.add(SpO2Point("", ratio.coerceIn(0f, 1f), value))
                }
            }
        }
        _chartData.value = points

        // Find last measurement time from the last day with data
        val lastDayWithData = metricsData.lastOrNull { it.values.any { v -> v > 0 } }
        val lastTime = if (lastDayWithData != null) {
            val lastValidIndex = lastDayWithData.values.indexOfLast { it > 0 }
            if (lastValidIndex >= 0) {
                indexToTimeLabel(lastValidIndex, lastDayWithData.values.size)
            } else ""
        } else ""

        _stats.value = SpO2Stats(
            high = _maxSpO2.value,
            avg = avg,
            low = _minSpO2.value,
            lastValue = _currentSpO2.value,
            lastTime = lastTime
        )

        _rangeText.value = "${_minSpO2.value} - ${_maxSpO2.value}"

        Timber.i("✅ SpO2 multi-day: ${allValid.size} valid points, avg=$avg")
    }

    // ============ Helpers ============

    private fun convertToScatterPoints(spo2Data: List<Int>): List<SpO2Point> {
        val points = mutableListOf<SpO2Point>()
        val totalPoints = spo2Data.size

        val minutesPerPoint = (24.0 * 60.0) / totalPoints

        Timber.d("📊 Converting: $totalPoints points, ${minutesPerPoint}min/point")

        spo2Data.forEachIndexed { index, value ->
            if (value > 0) {
                val timeRatio = index.toFloat() / totalPoints.toFloat()
                val timeLabel = indexToTimeLabel(index, totalPoints)

                points.add(SpO2Point(timeLabel, timeRatio, value))
            }
        }

        Timber.d("📊 Created ${points.size} scatter points")
        return points
    }

    private fun indexToTimeLabel(index: Int, totalPoints: Int): String {
        val minutesPerPoint = (24.0 * 60.0) / totalPoints
        val totalMinutes = (index * minutesPerPoint).toInt()
        val hour = (totalMinutes / 60) % 24
        val minute = totalMinutes % 60
        return String.format("%02d:%02d", hour, minute)
    }

    // ============ Clear Data ============

    private fun clearData() {
        _currentSpO2.value = 0
        _spo2Data.value = emptyList()
        _minSpO2.value = 0
        _maxSpO2.value = 0
        _chartData.value = emptyList()
        _stats.value = SpO2Stats()
        _rangeText.value = ""
    }
}