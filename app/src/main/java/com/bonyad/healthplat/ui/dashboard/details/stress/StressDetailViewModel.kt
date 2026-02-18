package com.bonyad.healthplat.ui.dashboard.details.stress

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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.GregorianCalendar
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

    data class StressPoint(val xRatio: Float, val value: Int)

    private val _chartPoints = MutableStateFlow<List<StressPoint>>(emptyList())
    val chartPoints: StateFlow<List<StressPoint>> = _chartPoints.asStateFlow()

    private val _stats = MutableStateFlow(StressStats())
    val stats: StateFlow<StressStats> = _stats.asStateFlow()

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
        loadStressFromApi()
    }

    // ── Public actions ────────────────────────────────────────────────────────

    fun refreshData() {
        viewModelScope.launch {
            // Phase 1 — Device → Server
            _isSyncing.value = true
            try {
                when (val result = healthRepository.syncDashboardData(day = 0)) {
                    is RecordDataResult.Success ->
                        _syncMessage.trySend("داده‌های استرس به‌روز شد ✓")
                    is RecordDataResult.Error ->
                        _syncMessage.trySend("دستگاه متصل نیست — نمایش آخرین داده")
                }
            } catch (e: Exception) {
                Timber.w(e, "Stress sync failed")
                _syncMessage.trySend("خطا در همگام‌سازی")
            } finally {
                _isSyncing.value = false
            }

            // Phase 2 — Reload (always runs)
            loadStressFromApi()
        }
    }

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
        loadStressFromApi()
    }

    fun selectDay(offset: Int) {
        _selectedDayOffset.value = offset
        if (_selectedTimeRange.value == "روزانه") loadStressFromApi()
    }

    // ── Private: data loading ─────────────────────────────────────────────────

    private fun loadStressFromApi() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val (dateFrom, dateTo) = getDateRange()
                Timber.i("📡 Fetching stress: $dateFrom → $dateTo")

                healthRepository.getMetricData(MetricType.STRESS, dateFrom, dateTo).fold(
                    onSuccess = { metricsData ->
                        if (metricsData.isNotEmpty()) processStressData(metricsData)
                        else { Timber.w("⚠️ No stress data"); clearData() }
                    },
                    onFailure = { e ->
                        Timber.e(e, "❌ Stress API error")
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
                "${pDate.monthName} ${pDate.shDay.toString().toFarsiDigits()}"
            }
        }
    }

    // ── Data processing ───────────────────────────────────────────────────────

    private fun processStressData(metricsData: List<MetricData>) {
        when (_selectedTimeRange.value) {
            "روزانه" -> processDailyStress(metricsData)
            "هفتگی", "ماهانه" -> processAggregatedStress(metricsData)
        }
    }

    private fun processDailyStress(metricsData: List<MetricData>) {
        val allValues = metricsData.flatMap { it.values }
        val validData = allValues.filter { it > 0 }
        if (validData.isEmpty()) { clearData(); return }

        _stats.value = StressStats(
            rangeMin = validData.minOrNull() ?: 0,
            rangeMax = validData.maxOrNull() ?: 0,
            currentVal = validData.lastOrNull() ?: 0,
            currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
            high = validData.maxOrNull() ?: 0,
            avg = validData.average().toInt(),
            low = validData.minOrNull() ?: 0
        )
        _chartPoints.value = convertToCurvePoints(allValues)
    }

    private fun processAggregatedStress(metricsData: List<MetricData>) {
        if (metricsData.isEmpty()) { clearData(); return }
        val allValues = mutableListOf<Int>()
        var high = 0; var low = Int.MAX_VALUE; var sum = 0; var count = 0

        metricsData.forEach { metric ->
            val validData = metric.values.filter { it > 0 }
            if (validData.isNotEmpty()) {
                allValues.addAll(validData)
                high = maxOf(high, validData.maxOrNull() ?: 0)
                low = minOf(low, validData.minOrNull() ?: Int.MAX_VALUE)
                sum += validData.sum()
                count += validData.size
            }
        }

        if (count == 0) { clearData(); return }
        if (low == Int.MAX_VALUE) low = 0

        _stats.value = StressStats(
            rangeMin = low, rangeMax = high,
            currentVal = allValues.lastOrNull() ?: 0,
            high = high, avg = sum / count, low = low
        )
        _chartPoints.value = metricsData.mapIndexed { index, metric ->
            val dayAvg = metric.values.filter { it > 0 }.average().toInt().takeIf { it > 0 } ?: return@mapIndexed null
            StressPoint((index + 1).toFloat() / metricsData.size, dayAvg)
        }.filterNotNull()
    }

    private fun convertToCurvePoints(stressData: List<Int>): List<StressPoint> {
        if (stressData.isEmpty()) return emptyList()
        val sampleInterval = maxOf(1, stressData.size / 20)
        val points = mutableListOf<StressPoint>()
        for (i in stressData.indices step sampleInterval) {
            val value = stressData[i]
            if (value > 0) points.add(StressPoint(i.toFloat() / stressData.size, value))
        }
        val lastValue = stressData.lastOrNull { it > 0 } ?: 0
        if (lastValue > 0 && points.lastOrNull()?.xRatio != 1.0f) {
            points.add(StressPoint(1.0f, lastValue))
        }
        return points
    }

    // ── Device fallback ───────────────────────────────────────────────────────

    private fun loadStressFromDevice(offset: Int) {
        viewModelScope.launch {
            try {
                val result = deviceManager.getRecordData(offset)
                if (result is RecordDataResult.Success) {
                    val stressData = result.stress?.stressSource ?: return@launch
                    val validData = stressData.filter { it > 0 }
                    if (validData.isEmpty()) { clearData(); return@launch }

                    _stats.value = StressStats(
                        rangeMin = validData.minOrNull() ?: 0,
                        rangeMax = validData.maxOrNull() ?: 0,
                        currentVal = validData.lastOrNull() ?: 0,
                        currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                        high = validData.maxOrNull() ?: 0,
                        avg = validData.average().toInt(),
                        low = validData.minOrNull() ?: 0
                    )
                    _chartPoints.value = convertToCurvePoints(stressData)
                    Timber.i("✅ Stress from device")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading stress from device")
                clearData()
            }
        }
    }

    // ── Clear ─────────────────────────────────────────────────────────────────

    private fun clearData() {
        _stats.value = StressStats()
        _chartPoints.value = emptyList()
    }

    fun getStressLevelLabel(): String = when {
        _stats.value.avg == 0 -> ""
        _stats.value.avg <= 25 -> "آرام"
        _stats.value.avg <= 50 -> "عادی"
        _stats.value.avg <= 75 -> "متوسط"
        else -> "بالا"
    }
}