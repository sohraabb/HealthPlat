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

    data class SpO2Point(val timeLabel: String, val timeRatio: Float, val value: Int)

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

    private val _rangeText = MutableStateFlow("")
    val rangeText: StateFlow<String> = _rangeText.asStateFlow()

    init {
        loadSpO2Data(0)
    }

    // ── Public actions ────────────────────────────────────────────────────────

    fun refreshData() {
        viewModelScope.launch {
            // Phase 1 — Device → Server
            _isSyncing.value = true
            try {
                when (val result = healthRepository.syncDashboardData(day = 0)) {
                    is RecordDataResult.Success ->
                        _syncMessage.trySend("داده‌های اکسیژن خون به‌روز شد ✓")
                    is RecordDataResult.Error ->
                        _syncMessage.trySend("دستگاه متصل نیست — نمایش آخرین داده")
                }
            } catch (e: Exception) {
                Timber.w(e, "SpO2 sync failed")
                _syncMessage.trySend("خطا در همگام‌سازی")
            } finally {
                _isSyncing.value = false
            }

            // Phase 2 — Reload (always runs)
            loadSpO2Data(_selectedDayOffset.value)
        }
    }

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
        loadSpO2Data(_selectedDayOffset.value)
    }

    fun selectDay(offset: Int) {
        _selectedDayOffset.value = offset
        updateDateLabel(offset)
        loadSpO2Data(offset)
    }

    // ── Private: data loading ─────────────────────────────────────────────────

    private fun loadSpO2Data(offset: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            when (_selectedTimeRange.value) {
                "هفتگی" -> _dateLabel.value = "هفته گذشته"
                "ماهانه" -> _dateLabel.value = "ماه گذشته"
                else -> updateDateLabel(offset)
            }

            try {
                val result = deviceManager.getRecordData(offset)
                if (result is RecordDataResult.Success &&
                    result.spo2?.sourceList?.any { it > 0 } == true) {
                    Timber.i("📊 SpO2 from RING: ${result.spo2.sourceList.size} values")
                    processSpO2Data(result.spo2.sourceList)
                } else {
                    Timber.w("⚠️ No ring data, trying API fallback...")
                    loadSpO2FromApi(offset)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to load SpO2 from ring")
                loadSpO2FromApi(offset)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadSpO2FromApi(offset: Int) {
        try {
            val today = LocalDate.now()
            val dateStr = today.plusDays(offset.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
            Timber.d("📡 SpO2 API fallback: $dateStr")

            healthRepository.getMetricData(MetricType.SPO2, dateStr, dateStr).fold(
                onSuccess = { metricsData ->
                    if (metricsData.isNotEmpty()) {
                        val values = metricsData.first().values
                        val cleaned = if (values.size > 48 && values.size % 48 == 0) {
                            Timber.w("⚠️ API data duplicated (${values.size}), taking first 48")
                            values.take(48)
                        } else values
                        processSpO2Data(cleaned)
                    } else clearData()
                },
                onFailure = { e -> Timber.e(e, "❌ SpO2 API error"); clearData() }
            )
        } catch (e: Exception) {
            Timber.e(e, "❌ SpO2 API fallback failed")
            clearData()
        }
    }

    private fun processSpO2Data(spo2List: List<Int>) {
        val validData = spo2List.filter { it > 0 }
        if (validData.isEmpty()) { clearData(); return }

        _spo2Data.value = validData
        _currentSpO2.value = validData.lastOrNull() ?: 0
        _minSpO2.value = validData.minOrNull() ?: 0
        _maxSpO2.value = validData.maxOrNull() ?: 0
        _chartData.value = convertToScatterPoints(spo2List)

        val lastValidIndex = spo2List.indexOfLast { it > 0 }
        val lastTime = if (lastValidIndex >= 0) indexToTimeLabel(lastValidIndex, spo2List.size) else ""

        _stats.value = SpO2Stats(
            high = _maxSpO2.value,
            avg = validData.average().toInt(),
            low = _minSpO2.value,
            lastValue = _currentSpO2.value,
            lastTime = lastTime
        )
        _rangeText.value = "${_minSpO2.value} - ${_maxSpO2.value}"
        Timber.i("✅ SpO2: ${validData.size} points, range=${_minSpO2.value}-${_maxSpO2.value}")
    }

    private fun convertToScatterPoints(spo2Data: List<Int>): List<SpO2Point> {
        val total = spo2Data.size
        return spo2Data.mapIndexedNotNull { index, value ->
            if (value <= 0) null
            else SpO2Point(
                timeLabel = indexToTimeLabel(index, total),
                timeRatio = index.toFloat() / total.toFloat(),
                value = value
            )
        }
    }

    private fun indexToTimeLabel(index: Int, totalPoints: Int): String {
        val minutesPerPoint = (24.0 * 60.0) / totalPoints
        val totalMinutes = (index * minutesPerPoint).toInt()
        return String.format("%02d:%02d", (totalMinutes / 60) % 24, totalMinutes % 60)
    }

    private fun updateDateLabel(offset: Int) {
        val today = LocalDate.now()
        val targetDate = today.plusDays(offset.toLong())
        val calendar = GregorianCalendar(targetDate.year, targetDate.monthValue - 1, targetDate.dayOfMonth)
        val pDate = PersianDate(calendar.time)
        val dayOfMonth = pDate.shDay.toString().toFarsiDigits()
        _dateLabel.value = when (offset) {
            0 -> "امروز $dayOfMonth ${pDate.monthName}"
            -1 -> "دیروز $dayOfMonth ${pDate.monthName}"
            else -> "$dayOfMonth ${pDate.monthName}"
        }
    }

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