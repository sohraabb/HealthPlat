package com.bonyad.healthplat.ui.dashboard.details.sp02

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.data.repository.AuthResult
import com.bonyad.healthplat.data.repository.CareRepository
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
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.GregorianCalendar
import javax.inject.Inject
import kotlin.math.abs
import com.bonyad.healthplat.ui.components.PersianDate as UiPersianDate

@HiltViewModel
class SpO2DetailViewModel @Inject constructor(
    private val deviceManager: HealthDeviceManager,
    private val healthRepository: HealthDataRepository,
    private val careRepository: CareRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val patientUserId: String? = savedStateHandle.get<String>("patientUserId")
    val isCaregiverMode: Boolean = patientUserId != null

    private val _currentSpO2 = MutableStateFlow(0)
    val currentSpO2: StateFlow<Int> = _currentSpO2.asStateFlow()

    private val _spo2Data = MutableStateFlow<List<Int>>(emptyList())
    val spo2Data: StateFlow<List<Int>> = _spo2Data.asStateFlow()

    private val _minSpO2 = MutableStateFlow(0)
    val minSpO2: StateFlow<Int> = _minSpO2.asStateFlow()

    private val _maxSpO2 = MutableStateFlow(0)
    val maxSpO2: StateFlow<Int> = _maxSpO2.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    data class SpO2Point(
        val timeLabel: String,
        val timeRatio: Float,
        val value: Int,
        val min: Int = 0,
        val max: Int = 0
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

    // Dynamic x-axis labels for weekly/monthly
    private val _xAxisLabels = MutableStateFlow<List<String>>(emptyList())
    val xAxisLabels: StateFlow<List<String>> = _xAxisLabels.asStateFlow()

    init {
        loadSpO2Data()
    }

    // ============ Sync: Ring → Server → API fetch → Display ============

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            if (!isCaregiverMode) {
                try {
                    val syncDay = ChronoUnit.DAYS.between(_selectedDate.value, LocalDate.now()).toInt().coerceIn(0, 6)
                    healthRepository.syncDashboardData(syncDay)
                    Timber.i("✅ Ring SpO2 data synced to server for day $syncDay")
                } catch (e: Exception) {
                    Timber.e(e, "⚠️ Ring→Server sync failed, loading from API anyway")
                }
            }
            loadSpO2Data()
        }
    }

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
        updateXAxisLabels()
        loadSpO2Data()
    }

    private fun updateXAxisLabels() {
        val today = LocalDate.now()
        _xAxisLabels.value = when (_selectedTimeRange.value) {
            "هفتگی" -> {
                val persianLabels = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
                val todayIdx = when (today.dayOfWeek.value) {
                    6 -> 0; 7 -> 1; else -> today.dayOfWeek.value + 1
                }
                (0..6).map { i -> persianLabels[(todayIdx - i + 7) % 7] }
            }
            "ماهانه" -> {
                (1..4).map { "هفته ${it.toString().toFarsiDigits()}" }
            }
            else -> emptyList()
        }
    }

    fun selectDay(date: LocalDate) {
        _selectedDate.value = date
        if (_selectedTimeRange.value == "روزانه") {
            loadSpO2Data()
        }
    }

    fun selectDate(date: UiPersianDate) {
        _selectedDate.value = LocalDate.parse(date.toGregorianIsoDate())
        _selectedTimeRange.value = "روزانه"
        updateXAxisLabels()
        loadSpO2Data()
    }

    // ============ Date Range (consistent with all other VMs) ============

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

    private fun loadSpO2Data() {
        viewModelScope.launch {
            _isLoading.value = true

            if (isCaregiverMode) {
                loadCaregiverSpO2()
            } else {
                loadOwnSpO2()
            }
        }
    }

    private suspend fun loadCaregiverSpO2() {
        try {
            val (dateFrom, dateTo) = getDateRange()
            Timber.i("📡 Fetching caregiver SpO2: $dateFrom to $dateTo for patient $patientUserId")

            when (val result = careRepository.getPatientSpo2(patientUserId!!, dateFrom, dateTo)) {
                is AuthResult.Success -> {
                    val metricsData = result.data
                    if (metricsData.isNotEmpty()) {
                        when (_selectedTimeRange.value) {
                            "روزانه" -> {
                                val values = metricsData.first().values
                                val cleanedValues = if (values.size > 48 && values.size % 48 == 0) {
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
                        Timber.i("✅ Caregiver SpO2 loaded: ${metricsData.size} records")
                    } else {
                        clearData()
                    }
                }
                is AuthResult.Error -> {
                    Timber.e("❌ Caregiver SpO2 error: ${result.message}")
                    clearData()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to load caregiver SpO2")
            clearData()
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun loadOwnSpO2() {
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
                        Timber.w("⚠️ No SpO2 data from API")
                        clearData()
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "❌ SpO2 API error, trying ring fallback...")
                    loadSpO2FromRing(ChronoUnit.DAYS.between(_selectedDate.value, LocalDate.now()).toInt().coerceIn(0, 6))
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to load SpO2 from API")
            loadSpO2FromRing(ChronoUnit.DAYS.between(_selectedDate.value, LocalDate.now()).toInt().coerceIn(0, 6))
        } finally {
            _isLoading.value = false
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

        val isWeekly = _selectedTimeRange.value == "هفتگی"
        val today = LocalDate.now()

        // Update labels
        val persianLabels = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
        val todayPersianIndex = when (today.dayOfWeek.value) {
            6 -> 0; 7 -> 1; else -> today.dayOfWeek.value + 1
        }

        if (isWeekly) {
            _xAxisLabels.value = (6 downTo 0).map { i -> persianLabels[(todayPersianIndex - i + 7) % 7] }
        } else {
            _xAxisLabels.value = (1..4).map { "هفته ${it.toString().toFarsiDigits()}" }
        }

        // Build one bar per bucket (day for weekly, week for monthly)
        val points = mutableListOf<SpO2Point>()

        if (isWeekly) {
            // 7 bars: oldest on the left, today on the right (RTL-friendly)
            var listIndex = 0
            for (i in 6 downTo 0) {
                val date = today.minusDays(i.toLong())
                val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val metric = metricsData.find { it.recordDate.startsWith(dateStr) }
                val dayValid = metric?.values?.filter { it > 0 } ?: emptyList()
                val dayAvg = if (dayValid.isNotEmpty()) dayValid.average().toInt() else 0
                val dayMin = dayValid.minOrNull() ?: 0
                val dayMax = dayValid.maxOrNull() ?: 0
                val ratio = listIndex.toFloat() / 6f
                points.add(SpO2Point(persianLabels[(todayPersianIndex - i + 7) % 7], ratio, dayAvg, min = dayMin, max = dayMax))
                listIndex++
            }
        } else {
            // Last 28 days, one bar per week chunk (oldest first)
            val startDate = today.minusDays(27)
            val numWeeks = 4
            for (weekIndex in 0 until numWeeks) {
                val weekValues = mutableListOf<Int>()
                for (dayInWeek in 0 until 7) {
                    val dayIndex = weekIndex * 7 + dayInWeek
                    val date = startDate.plusDays(dayIndex.toLong())
                    val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val metric = metricsData.find { it.recordDate.startsWith(dateStr) }
                    metric?.values?.filter { it > 0 }?.let { weekValues.addAll(it) }
                }
                val weekAvg = if (weekValues.isNotEmpty()) weekValues.average().toInt() else 0
                val weekMin = if (weekValues.isNotEmpty()) weekValues.min() else 0
                val weekMax = if (weekValues.isNotEmpty()) weekValues.max() else 0
                val ratio = if (numWeeks > 1) weekIndex.toFloat() / (numWeeks - 1).toFloat() else 0.5f
                points.add(SpO2Point("هفته ${(weekIndex + 1).toString().toFarsiDigits()}", ratio, weekAvg, min = weekMin, max = weekMax))
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
}