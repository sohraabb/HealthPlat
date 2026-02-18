package com.bonyad.healthplat.ui.dashboard.details.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.repository.HealthDataRepository
import com.bonyad.healthplat.data.repository.MetricType
import com.bonyad.healthplat.domain.model.MetricData
import com.bonyad.healthplat.domain.model.RecordDataResult
import com.bonyad.healthplat.ui.utils.SleepDataHelper
import com.bonyad.healthplat.ui.utils.toFarsiDigits
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import saman.zamani.persiandate.PersianDate
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.GregorianCalendar
import javax.inject.Inject

@HiltViewModel
class SleepDetailViewModel @Inject constructor(
    private val deviceManager: HealthDeviceManager,
    private val healthRepository: HealthDataRepository,
    private val userPreferences: UserPreferencesDataStore,
    private val sleepDataHelper: SleepDataHelper
) : ViewModel() {

    private val _deepMinutes = MutableStateFlow(0)
    val deepMinutes: StateFlow<Int> = _deepMinutes.asStateFlow()

    private val _lightMinutes = MutableStateFlow(0)
    val lightMinutes: StateFlow<Int> = _lightMinutes.asStateFlow()

    private val _remMinutes = MutableStateFlow(0)
    val remMinutes: StateFlow<Int> = _remMinutes.asStateFlow()

    private val _awakeMinutes = MutableStateFlow(0)
    val awakeMinutes: StateFlow<Int> = _awakeMinutes.asStateFlow()

    private val _totalSleepMinutes = MutableStateFlow(0)
    val totalSleepMinutes: StateFlow<Int> = _totalSleepMinutes.asStateFlow()

    private val _sleepQuality = MutableStateFlow(0)
    val sleepQuality: StateFlow<Int> = _sleepQuality.asStateFlow()

    data class SleepSegment(
        val stage: SleepStage,
        val startRatio: Float,
        val widthRatio: Float
    )

    enum class SleepStage { AWAKE, LIGHT, DEEP, REM }

    private val _sleepTimeline = MutableStateFlow<List<SleepSegment>>(emptyList())
    val sleepTimeline: StateFlow<List<SleepSegment>> = _sleepTimeline.asStateFlow()

    data class SleepStats(val hours: Int = 0, val minutes: Int = 0, val score: Int = 0)

    private val _sleepStats = MutableStateFlow(SleepStats())
    val sleepStats: StateFlow<SleepStats> = _sleepStats.asStateFlow()

    private val _stagePercentages = MutableStateFlow(Triple(0, 0, 0))
    val stagePercentages: StateFlow<Triple<Int, Int, Int>> = _stagePercentages.asStateFlow()

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
        loadSleepData(0)
    }

    // ── Public actions ────────────────────────────────────────────────────────

    /**
     * Phase 1: push latest ring data → server  (isSyncing = true)
     * Phase 2: reload sleep data from ring/API   (isLoading = true)
     */
    fun refreshData() {
        viewModelScope.launch {
            // Phase 1 — Device → Server
            _isSyncing.value = true
            try {
                when (val result = healthRepository.syncDashboardData(day = 0)) {
                    is RecordDataResult.Success ->
                        _syncMessage.trySend("داده‌های خواب به‌روز شد ✓")
                    is RecordDataResult.Error ->
                        _syncMessage.trySend("دستگاه متصل نیست — نمایش آخرین داده")
                }
            } catch (e: Exception) {
                Timber.w(e, "Sleep sync to server failed")
                _syncMessage.trySend("خطا در همگام‌سازی")
            } finally {
                _isSyncing.value = false
            }

            // Phase 2 — Reload (always runs)
            loadSleepData(_selectedDayOffset.value)
        }
    }

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
        loadSleepData(_selectedDayOffset.value)
    }

    fun selectDay(offset: Int) {
        _selectedDayOffset.value = offset
        loadSleepData(offset)
    }

    // ── Private: data loading ─────────────────────────────────────────────────

    private fun loadSleepData(offset: Int) {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        viewModelScope.launch {
            _isLoading.value = true
            val targetDate = today.plusDays(_selectedDayOffset.value.toLong())
            updateDateLabel(targetDate)

            try {
                val fullSleepData = sleepDataHelper.getFullSleepData(offset)
                if (fullSleepData.isNotEmpty()) {
                    Timber.i("📊 Sleep from RING: ${fullSleepData.size} values")
                    processSleepData(fullSleepData)
                } else {
                    Timber.w("⚠️ No ring data, trying API fallback...")
                    loadSleepFromApi(offset)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to load sleep from ring")
                loadSleepFromApi(offset)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadSleepFromApi(offset: Int) {
        try {
            val today = LocalDate.now()
            val targetDate = today.plusDays(offset.toLong())
            val dateStr = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            Timber.d("📡 Sleep API fallback: $dateStr")

            healthRepository.getMetricData(
                metricType = MetricType.SLEEP,
                dateFrom = dateStr,
                dateTo = dateStr
            ).fold(
                onSuccess = { metricsData ->
                    if (metricsData.isNotEmpty()) {
                        val values = metricsData.first().values
                        val cleanedValues = if (values.size > 1440 && values.size % 1440 == 0) {
                            Timber.w("⚠️ API sleep data duplicated (${values.size}), taking first 1440")
                            values.take(1440)
                        } else values
                        processSleepData(cleanedValues)
                    } else clearData()
                },
                onFailure = { e ->
                    Timber.e(e, "❌ Sleep API error")
                    clearData()
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "❌ Sleep API fallback failed")
            clearData()
        }
    }

    // ── Sleep data processing ─────────────────────────────────────────────────

    private fun processSleepData(sourceList: List<Int>) {
        if (sourceList.isEmpty()) { clearData(); return }

        val deep = sourceList.count { it == 1 }
        val light = sourceList.count { it == 2 }
        val awake = sourceList.count { it == 3 }
        val rem = sourceList.count { it == 4 }
        val totalSleep = deep + light + rem

        if (totalSleep == 0) { Timber.w("⚠️ No actual sleep data"); clearData(); return }

        _deepMinutes.value = deep
        _lightMinutes.value = light
        _awakeMinutes.value = awake
        _remMinutes.value = rem
        _totalSleepMinutes.value = totalSleep

        val score = calculateSleepQuality(deep, light, rem, awake)
        _sleepStats.value = SleepStats(totalSleep / 60, totalSleep % 60, score)
        _sleepQuality.value = score

        if (totalSleep > 0) {
            _stagePercentages.value = Triple(
                (deep * 100) / totalSleep,
                (light * 100) / totalSleep,
                (rem * 100) / totalSleep
            )
        }

        _sleepTimeline.value = generateTimeline(sourceList)
        Timber.i("✅ Sleep: ${totalSleep / 60}h ${totalSleep % 60}m — Deep=$deep Light=$light REM=$rem Awake=$awake Score=$score")
    }

    private fun generateTimeline(sourceList: List<Int>): List<SleepSegment> {
        if (sourceList.isEmpty()) return emptyList()
        val segments = mutableListOf<SleepSegment>()
        val total = sourceList.size
        var currentStage: SleepStage? = null
        var segmentStart = 0

        sourceList.forEachIndexed { index, value ->
            val stage = when (value) {
                1 -> SleepStage.DEEP
                2 -> SleepStage.LIGHT
                3 -> SleepStage.AWAKE
                4 -> SleepStage.REM
                else -> null
            }
            if (stage != currentStage) {
                if (currentStage != null && currentStage != SleepStage.AWAKE) {
                    val widthRatio = (index - segmentStart).toFloat() / total
                    if (widthRatio > 0.001f) {
                        segments.add(SleepSegment(currentStage!!, segmentStart.toFloat() / total, widthRatio.coerceAtLeast(0.01f)))
                    }
                }
                currentStage = stage
                segmentStart = index
            }
        }
        if (currentStage != null && currentStage != SleepStage.AWAKE) {
            val widthRatio = (total - segmentStart).toFloat() / total
            if (widthRatio > 0.001f) {
                segments.add(SleepSegment(currentStage!!, segmentStart.toFloat() / total, widthRatio.coerceAtLeast(0.01f)))
            }
        }
        Timber.d("📊 Generated ${segments.size} sleep segments")
        return segments
    }

    private fun calculateSleepQuality(deep: Int, light: Int, rem: Int, awake: Int): Int {
        val total = deep + light + rem + awake
        if (total == 0) return 0
        val raw = deep * 0.4 + rem * 0.35 + light * 0.2 + awake * -0.1
        return ((raw / total) * 100).coerceIn(0.0, 100.0).toInt()
    }

    private fun updateDateLabel(date: LocalDate) {
        val today = LocalDate.now()
        _dateLabel.value = when {
            date == today -> "امشب"
            date == today.minusDays(1) -> "دیشب"
            else -> {
                val calendar = GregorianCalendar(date.year, date.monthValue - 1, date.dayOfMonth)
                val pDate = PersianDate(calendar.time)
                "${pDate.monthName} ${pDate.shDay.toString().toFarsiDigits()}"
            }
        }
    }

    private fun clearData() {
        _deepMinutes.value = 0
        _lightMinutes.value = 0
        _remMinutes.value = 0
        _awakeMinutes.value = 0
        _totalSleepMinutes.value = 0
        _sleepQuality.value = 0
        _sleepStats.value = SleepStats()
        _stagePercentages.value = Triple(0, 0, 0)
        _sleepTimeline.value = emptyList()
    }

    fun getSleepQualityLabel(): String = when {
        _sleepStats.value.score >= 85 -> "عالی"
        _sleepStats.value.score >= 70 -> "خوب"
        _sleepStats.value.score >= 50 -> "متوسط"
        _sleepStats.value.score > 0 -> "ضعیف"
        else -> ""
    }
}