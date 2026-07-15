package com.bonyad.healthplat.ui.dashboard.details.stress

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
import timber.log.Timber
import saman.zamani.persiandate.PersianDate
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.GregorianCalendar
import javax.inject.Inject
import kotlin.math.abs
import com.bonyad.healthplat.ui.components.PersianDate as UiPersianDate

@HiltViewModel
class StressDetailViewModel @Inject constructor(
    private val deviceManager: HealthDeviceManager,
    private val healthRepository: HealthDataRepository,
    private val careRepository: CareRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val patientUserId: String? = savedStateHandle.get<String>("patientUserId")
    val isCaregiverMode: Boolean = patientUserId != null

    data class StressStats(
        val rangeMin: Int = 0,
        val rangeMax: Int = 0,
        val currentVal: Int = 0,
        val currentTime: String = "",
        val high: Int = 0,
        val avg: Int = 0,
        val low: Int = 0
    )

    data class StressPoint(val xRatio: Float, val value: Int)

    private val _chartPoints = MutableStateFlow<List<StressPoint>>(emptyList())
    val chartPoints: StateFlow<List<StressPoint>> = _chartPoints.asStateFlow()

    private val _stats = MutableStateFlow(StressStats())
    val stats: StateFlow<StressStats> = _stats.asStateFlow()

    private val _selectedTimeRange = MutableStateFlow("روزانه")
    val selectedTimeRange: StateFlow<String> = _selectedTimeRange.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Date label for display
    private val _dateLabel = MutableStateFlow("")
    val dateLabel: StateFlow<String> = _dateLabel.asStateFlow()

    // Dynamic x-axis labels for weekly/monthly
    private val _xAxisLabels = MutableStateFlow<List<String>>(emptyList())
    val xAxisLabels: StateFlow<List<String>> = _xAxisLabels.asStateFlow()

    init {
        loadStressFromApi()
    }

    // ============ Sync: Ring → Server → API fetch → Display ============

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            if (!isCaregiverMode) {
                try {
                    val syncDay = ChronoUnit.DAYS.between(_selectedDate.value, LocalDate.now()).toInt().coerceIn(0, 6)
                    healthRepository.syncDashboardData(syncDay)
                    Timber.i("✅ Ring stress data synced to server for day $syncDay")
                } catch (e: Exception) {
                    Timber.e(e, "⚠️ Ring→Server sync failed, loading from API anyway")
                }
            }
            loadStressFromApi()
        }
    }

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
        updateXAxisLabels()
        loadStressFromApi()
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
            loadStressFromApi()
        }
    }

    fun selectDate(date: UiPersianDate) {
        _selectedDate.value = LocalDate.parse(date.toGregorianIsoDate())
        _selectedTimeRange.value = "روزانه"
        updateXAxisLabels()
        loadStressFromApi()
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

    private fun loadStressFromApi() {
        viewModelScope.launch {
            _isLoading.value = true

            if (isCaregiverMode) {
                loadCaregiverStress()
            } else {
                loadOwnStress()
            }
        }
    }

    private suspend fun loadCaregiverStress() {
        try {
            val (dateFrom, dateTo) = getDateRange()
            Timber.i("📡 Fetching caregiver stress: $dateFrom to $dateTo for patient $patientUserId")

            when (val result = careRepository.getPatientStress(patientUserId!!, dateFrom, dateTo)) {
                is AuthResult.Success -> {
                    if (result.data.isNotEmpty()) {
                        processStressData(result.data)
                        Timber.i("✅ Caregiver stress loaded: ${result.data.size} records")
                    } else {
                        clearData()
                    }
                }
                is AuthResult.Error -> {
                    Timber.e("❌ Caregiver stress error: ${result.message}")
                    clearData()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to load caregiver stress")
            clearData()
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun loadOwnStress() {
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
                    loadStressFromDevice(ChronoUnit.DAYS.between(_selectedDate.value, LocalDate.now()).toInt().coerceIn(0, 6))
                }
            )

        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to load stress from API")
            loadStressFromDevice(ChronoUnit.DAYS.between(_selectedDate.value, LocalDate.now()).toInt().coerceIn(0, 6))
        } finally {
            _isLoading.value = false
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

        _chartPoints.value = convertToCurvePoints(allValues)
    }

    private fun processWeeklyStress(metricsData: List<MetricData>) {
        if (metricsData.isEmpty()) {
            clearData()
            return
        }

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

        // Last 7 days: oldest on the left, today on the right (RTL-friendly)
        val today = LocalDate.now()
        val persianLabels = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
        val todayPersianIndex = when (today.dayOfWeek.value) {
            6 -> 0; 7 -> 1; else -> today.dayOfWeek.value + 1
        }
        _xAxisLabels.value = (6 downTo 0).map { i -> persianLabels[(todayPersianIndex - i + 7) % 7] }
        var listIndex = 0
        _chartPoints.value = (6 downTo 0).map { i ->
            val date = today.minusDays(i.toLong())
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val metric = metricsData.find { it.recordDate.startsWith(dateStr) }
            val validData = metric?.values?.filter { it > 0 } ?: emptyList()
            val dayAvg = if (validData.isNotEmpty()) validData.average().toInt() else 0
            val xRatio = (listIndex++).toFloat() / 6f
            StressPoint(xRatio, dayAvg)
        }
    }

    private fun processMonthlyStress(metricsData: List<MetricData>) {
        if (metricsData.isEmpty()) {
            clearData()
            return
        }

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

        // Last 28 days chunked into 4 weeks (oldest first)
        val today = LocalDate.now()
        val startDate = today.minusDays(27)
        _xAxisLabels.value = (1..4).map { "هفته ${it.toString().toFarsiDigits()}" }
        val dailyAvgs = (0 until 28).map { dayOffset ->
            val date = startDate.plusDays(dayOffset.toLong())
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val metric = metricsData.find { it.recordDate.startsWith(dateStr) }
            val validData = metric?.values?.filter { it > 0 } ?: emptyList()
            if (validData.isNotEmpty()) validData.average().toInt() else 0
        }

        val weeklyGroups = dailyAvgs.chunked(7)
        _chartPoints.value = weeklyGroups.mapIndexed { index, weekData ->
            val validWeek = weekData.filter { it > 0 }
            val weekAvg = if (validWeek.isNotEmpty()) validWeek.average().toInt() else 0
            val xRatio = if (weeklyGroups.size > 1) index.toFloat() / (weeklyGroups.size - 1).toFloat() else 0f
            StressPoint(xRatio, weekAvg)
        }
    }

    private fun convertToCurvePoints(stressData: List<Int>): List<StressPoint> {
        if (stressData.isEmpty()) return emptyList()

        val points = mutableListOf<StressPoint>()

        val sampleInterval = maxOf(1, stressData.size / 20)

        for (i in stressData.indices step sampleInterval) {
            val value = stressData[i]
            if (value > 0) {
                val xRatio = i.toFloat() / stressData.size
                points.add(StressPoint(xRatio, value))
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