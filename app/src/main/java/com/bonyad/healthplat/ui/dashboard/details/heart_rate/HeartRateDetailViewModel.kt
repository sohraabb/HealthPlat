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

    private val _chartData = MutableStateFlow<List<HeartRateRangePoint>>(emptyList())
    val chartData: StateFlow<List<HeartRateRangePoint>> = _chartData.asStateFlow()

    private val _currentHrv = MutableStateFlow(0)
    val currentHrv: StateFlow<Int> = _currentHrv.asStateFlow()

    private val _avgHrv = MutableStateFlow(0)
    val avgHrv: StateFlow<Int> = _avgHrv.asStateFlow()

    private val _minHrv = MutableStateFlow(0)
    val minHrv: StateFlow<Int> = _minHrv.asStateFlow()

    private val _maxHrv = MutableStateFlow(0)
    val maxHrv: StateFlow<Int> = _maxHrv.asStateFlow()

    private val _selectedTimeRange = MutableStateFlow("روزانه")
    val selectedTimeRange: StateFlow<String> = _selectedTimeRange.asStateFlow()

    private val _selectedDayOffset = MutableStateFlow(0)
    val selectedDayOffset = _selectedDayOffset.asStateFlow()

    private val _currentPersianDate = MutableStateFlow("")
    val currentPersianDate: StateFlow<String> = _currentPersianDate.asStateFlow()

    private val _hrvChartData = MutableStateFlow<List<Int>>(emptyList())
    val hrvChartData: StateFlow<List<Int>> = _hrvChartData.asStateFlow()

    // ── Loading: server → UI (chart skeleton shown) ──────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── Syncing: device → server (sync button animates) ──────────────────────
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // ── One-time snackbar messages (Channel guarantees each event consumed once) ──
    private val _syncMessage = Channel<String>(Channel.BUFFERED)
    val syncMessage = _syncMessage.receiveAsFlow()

    init {
        updateCurrentPersianDate()
        loadDataFromApi()
        observeRealTimeData()
    }

    // ── Public actions ────────────────────────────────────────────────────────

    /**
     * Called by the sync button.
     * Phase 1: push latest ring data → server  (isSyncing = true)
     * Phase 2: fetch updated data from server  (isLoading = true)
     */
    fun refreshData() {
        viewModelScope.launch {
            // Phase 1 — Device → Server
            _isSyncing.value = true
            try {
                when (val result = healthRepository.syncDashboardData(day = 0)) {
                    is RecordDataResult.Success ->
                        _syncMessage.trySend("داده‌ها به‌روز شد ✓")
                    is RecordDataResult.Error ->
                        _syncMessage.trySend("دستگاه متصل نیست — نمایش آخرین داده")
                }
            } catch (e: Exception) {
                Timber.w(e, "Sync to server failed")
                _syncMessage.trySend("خطا در همگام‌سازی")
            } finally {
                _isSyncing.value = false
            }

            // Phase 2 — Server → UI (always runs, even if sync failed)
            loadDataFromApi()
        }
    }

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

    // ── Private: data loading ─────────────────────────────────────────────────

    private fun loadDataFromApi() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val (dateFrom, dateTo) = getDateRange()
                Timber.i("📡 Fetching HR & HRV: $dateFrom → $dateTo")

                val heartRateResult = async {
                    healthRepository.getMetricData(MetricType.HEART_RATE, dateFrom, dateTo)
                }
                val hrvResult = async {
                    healthRepository.getMetricData(MetricType.HRV, dateFrom, dateTo)
                }

                heartRateResult.await().fold(
                    onSuccess = { data ->
                        if (data.isNotEmpty()) processHeartRateData(data)
                        else { Timber.w("⚠️ No HR data"); clearHeartRateData() }
                    },
                    onFailure = { e ->
                        Timber.e(e, "❌ HR API error")
                        loadHeartRateFromDevice(_selectedDayOffset.value)
                    }
                )

                hrvResult.await().fold(
                    onSuccess = { data ->
                        if (data.isNotEmpty()) processHrvData(data)
                        else { Timber.w("⚠️ No HRV data"); clearHrvData() }
                    },
                    onFailure = { e ->
                        Timber.e(e, "❌ HRV API error")
                        loadHrvFromDevice(_selectedDayOffset.value)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed loading from API")
                loadHeartRateFromDevice(_selectedDayOffset.value)
                loadHrvFromDevice(_selectedDayOffset.value)
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
                val dateStr = targetDate.format(formatter)
                updateCurrentPersianDate(_selectedDayOffset.value)
                Pair(dateStr, dateStr)
            }
            "هفتگی" -> {
                _currentPersianDate.value = "هفته گذشته"
                Pair(today.minusDays(6).format(formatter), today.format(formatter))
            }
            "ماهانه" -> {
                _currentPersianDate.value = "ماه گذشته"
                Pair(today.minusDays(29).format(formatter), today.format(formatter))
            }
            else -> today.format(formatter).let { Pair(it, it) }
        }
    }

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

    // ── Heart Rate Processing ─────────────────────────────────────────────────

    private fun processHeartRateData(metricsData: List<MetricData>) {
        when (_selectedTimeRange.value) {
            "روزانه" -> processDailyHeartRate(metricsData)
            "هفتگی" -> processWeeklyHeartRate(metricsData)
            "ماهانه" -> processMonthlyHeartRate(metricsData)
        }
    }

    private fun processDailyHeartRate(metricsData: List<MetricData>) {
        val allValues = metricsData.flatMap { it.values }
        val validData = allValues.filter { it > 1 }
        if (validData.isEmpty()) { clearHeartRateData(); return }

        _avgHeartRate.value = validData.average().toInt()
        _minHeartRate.value = validData.minOrNull() ?: 0
        _maxHeartRate.value = validData.maxOrNull() ?: 0
        _currentHeartRate.value = validData.lastOrNull() ?: 0
        _heartRateData.value = validData
        _chartData.value = buildHourlyChartData(allValues)
    }

    private fun processWeeklyHeartRate(metricsData: List<MetricData>) {
        val allValidValues = metricsData.flatMap { it.values }.filter { it > 1 }
        if (allValidValues.isEmpty()) { clearHeartRateData(); return }

        _avgHeartRate.value = allValidValues.average().toInt()
        _minHeartRate.value = allValidValues.minOrNull() ?: 0
        _maxHeartRate.value = allValidValues.maxOrNull() ?: 0
        _currentHeartRate.value = allValidValues.lastOrNull() ?: 0
        _heartRateData.value = allValidValues

        _chartData.value = metricsData.mapNotNull { metric ->
            val dayValues = metric.values.filter { it > 1 }
            if (dayValues.isEmpty()) return@mapNotNull null
            val dateLabel = try {
                LocalDate.parse(metric.recordDate.substring(0, 10)).dayOfMonth.toString()
            } catch (e: Exception) { metric.recordDate.substring(8, 10) }
            HeartRateRangePoint(
                timeLabel = dateLabel,
                min = dayValues.minOrNull() ?: 0,
                max = dayValues.maxOrNull() ?: 0,
                isAlert = (dayValues.maxOrNull() ?: 0) > 120 || (dayValues.minOrNull() ?: 0) < 50
            )
        }
    }

    private fun processMonthlyHeartRate(metricsData: List<MetricData>) {
        val allValidValues = metricsData.flatMap { it.values }.filter { it > 1 }
        if (allValidValues.isEmpty()) { clearHeartRateData(); return }

        _avgHeartRate.value = allValidValues.average().toInt()
        _minHeartRate.value = allValidValues.minOrNull() ?: 0
        _maxHeartRate.value = allValidValues.maxOrNull() ?: 0
        _currentHeartRate.value = allValidValues.lastOrNull() ?: 0
        _heartRateData.value = allValidValues

        _chartData.value = metricsData.sortedBy { it.recordDate }
            .chunked(7)
            .mapIndexed { weekIndex, weekMetrics ->
                val weekValues = weekMetrics.flatMap { it.values }.filter { it > 1 }
                if (weekValues.isEmpty()) return@mapIndexed null
                HeartRateRangePoint(
                    timeLabel = "هفته ${weekIndex + 1}",
                    min = weekValues.minOrNull() ?: 0,
                    max = weekValues.maxOrNull() ?: 0,
                    isAlert = (weekValues.maxOrNull() ?: 0) > 120 || (weekValues.minOrNull() ?: 0) < 50
                )
            }.filterNotNull()
    }

    private fun buildHourlyChartData(hrData: List<Int>): List<HeartRateRangePoint> {
        val result = mutableListOf<HeartRateRangePoint>()
        for (hour in 0..23) {
            val startIdx = hour * 60
            val endIdx = minOf(startIdx + 60, hrData.size)
            if (startIdx >= hrData.size) break
            val hourData = hrData.subList(startIdx, endIdx).filter { it > 1 }
            if (hourData.isEmpty()) continue
            result.add(HeartRateRangePoint(
                timeLabel = String.format("%02d:00", hour),
                min = hourData.minOrNull() ?: 0,
                max = hourData.maxOrNull() ?: 0,
                isAlert = (hourData.maxOrNull() ?: 0) > 120 || (hourData.minOrNull() ?: 0) < 50
            ))
        }
        return result
    }

    // ── HRV Processing ────────────────────────────────────────────────────────

    private fun processHrvData(metricsData: List<MetricData>) {
        val validData = metricsData.flatMap { it.values }.filter { it > 0 }
        if (validData.isEmpty()) { clearHrvData(); return }

        _avgHrv.value = validData.average().toInt()
        _minHrv.value = validData.minOrNull() ?: 0
        _maxHrv.value = validData.maxOrNull() ?: 0
        _currentHrv.value = validData.lastOrNull() ?: 0
        _hrvChartData.value = validData
    }

    // ── Device Fallbacks ──────────────────────────────────────────────────────

    private fun loadHeartRateFromDevice(offset: Int) {
        viewModelScope.launch {
            try {
                val result = deviceManager.getRecordData(offset)
                if (result is RecordDataResult.Success) {
                    val validData = result.heartRate?.heartRateSource?.filter { it > 1 } ?: return@launch
                    if (validData.isNotEmpty()) {
                        _heartRateData.value = validData
                        _avgHeartRate.value = validData.average().toInt()
                        _minHeartRate.value = validData.minOrNull() ?: 0
                        _maxHeartRate.value = validData.maxOrNull() ?: 0
                        _currentHeartRate.value = validData.lastOrNull() ?: 0
                        _chartData.value = buildHourlyChartData(result.heartRate.heartRateSource)
                    } else clearHeartRateData()
                }
            } catch (e: Exception) {
                Timber.e(e, "HR device fallback failed")
                clearHeartRateData()
            }
        }
    }

    private fun loadHrvFromDevice(offset: Int) {
        viewModelScope.launch {
            try {
                val result = deviceManager.getRecordData(offset)
                if (result is RecordDataResult.Success) {
                    val validData = result.hrv?.hrvSource?.filter { it > 0 } ?: return@launch
                    if (validData.isNotEmpty()) {
                        _avgHrv.value = validData.average().toInt()
                        _minHrv.value = validData.minOrNull() ?: 0
                        _maxHrv.value = validData.maxOrNull() ?: 0
                        _currentHrv.value = validData.lastOrNull() ?: 0
                    } else clearHrvData()
                }
            } catch (e: Exception) {
                Timber.e(e, "HRV device fallback failed")
                clearHrvData()
            }
        }
    }

    // ── Real-time ─────────────────────────────────────────────────────────────

    private fun observeRealTimeData() {
        viewModelScope.launch {
            deviceManager.realTimeData.collect { data ->
                data.heart?.let { hr -> if (hr > 0) _currentHeartRate.value = hr }
            }
        }
    }

    // ── Clear ─────────────────────────────────────────────────────────────────

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
}