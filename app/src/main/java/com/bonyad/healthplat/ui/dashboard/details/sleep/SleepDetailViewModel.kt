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

    // Sleep Timeline Data
    data class SleepSegment(
        val stage: SleepStage,
        val startRatio: Float,
        val widthRatio: Float
    )

    enum class SleepStage { AWAKE, LIGHT, DEEP, REM }

    private val _sleepTimeline = MutableStateFlow<List<SleepSegment>>(emptyList())
    val sleepTimeline: StateFlow<List<SleepSegment>> = _sleepTimeline.asStateFlow()

    // Stats
    data class SleepStats(val hours: Int = 0, val minutes: Int = 0, val score: Int = 0)

    private val _sleepStats = MutableStateFlow(SleepStats())
    val sleepStats: StateFlow<SleepStats> = _sleepStats.asStateFlow()

    // Percentages for Donut (Deep, Light, REM)
    private val _stagePercentages = MutableStateFlow(Triple(0, 0, 0))
    val stagePercentages: StateFlow<Triple<Int, Int, Int>> = _stagePercentages.asStateFlow()

    private val _selectedTimeRange = MutableStateFlow("روزانه")
    val selectedTimeRange: StateFlow<String> = _selectedTimeRange.asStateFlow()

    private val _selectedDayOffset = MutableStateFlow(0)
    val selectedDayOffset = _selectedDayOffset.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Date label
    private val _dateLabel = MutableStateFlow("")
    val dateLabel: StateFlow<String> = _dateLabel.asStateFlow()

    init {
        loadSleepData()
    }

    // ============ Sync: Ring → Server → API fetch → Display ============

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val syncDay = abs(_selectedDayOffset.value)
                healthRepository.syncDashboardData(syncDay)
                Timber.i("✅ Ring sleep data synced to server for day $syncDay")
            } catch (e: Exception) {
                Timber.e(e, "⚠️ Ring→Server sync failed, loading from API anyway")
            }
            loadSleepData()
        }
    }

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
        loadSleepData()
    }

    fun selectDay(offset: Int) {
        _selectedDayOffset.value = offset
        if (_selectedTimeRange.value == "روزانه") {
            loadSleepData()
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

    private fun loadSleepData() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val (dateFrom, dateTo) = getDateRange()
                Timber.i("📡 Fetching sleep from API: $dateFrom to $dateTo")

                healthRepository.getMetricData(
                    metricType = MetricType.SLEEP,
                    dateFrom = dateFrom,
                    dateTo = dateTo
                ).fold(
                    onSuccess = { metricsData ->
                        if (metricsData.isNotEmpty()) {
                            when (_selectedTimeRange.value) {
                                "روزانه" -> {
                                    val values = metricsData.first().values
                                    val cleanedValues = if (values.size > 1440 && values.size % 1440 == 0) {
                                        Timber.w("⚠️ API sleep data duplicated (${values.size}), taking first 1440")
                                        values.take(1440)
                                    } else {
                                        values
                                    }
                                    processSleepData(cleanedValues)
                                }
                                "هفتگی", "ماهانه" -> {
                                    processMultiDaySleep(metricsData)
                                }
                            }
                            Timber.i("✅ Sleep loaded from API: ${metricsData.size} records")
                        } else {
                            Timber.w("⚠️ No sleep data from API, trying ring fallback...")
                            loadSleepFromRing(_selectedDayOffset.value)
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "❌ Sleep API error, trying ring fallback...")
                        loadSleepFromRing(_selectedDayOffset.value)
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to load sleep from API")
                loadSleepFromRing(_selectedDayOffset.value)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ============ FALLBACK: Load from Ring ============

    private suspend fun loadSleepFromRing(offset: Int) {
        try {
            val fullSleepData = sleepDataHelper.getFullSleepData(offset)

            if (fullSleepData.isNotEmpty()) {
                Timber.i("✅ Sleep loaded from ring fallback: ${fullSleepData.size} values")
                processSleepData(fullSleepData)
            } else {
                Timber.w("⚠️ No sleep data from ring either")
                clearData()
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Ring fallback also failed")
            clearData()
        }
    }

    // ============ Process Sleep Data (single day) ============

    private fun processSleepData(sourceList: List<Int>) {
        if (sourceList.isEmpty()) {
            clearData()
            return
        }

        // Sleep values: 0=Activity, 1=Deep, 2=Light, 3=Awake, 4=REM
        val deep = sourceList.count { it == 1 }
        val light = sourceList.count { it == 2 }
        val awake = sourceList.count { it == 3 }
        val rem = sourceList.count { it == 4 }

        val totalSleep = deep + light + rem

        if (totalSleep == 0) {
            Timber.w("⚠️ No actual sleep data (all activity/awake)")
            clearData()
            return
        }

        _deepMinutes.value = deep
        _lightMinutes.value = light
        _awakeMinutes.value = awake
        _remMinutes.value = rem
        _totalSleepMinutes.value = totalSleep

        val hours = totalSleep / 60
        val minutes = totalSleep % 60
        val score = calculateSleepQuality(deep, light, rem, awake)

        _sleepStats.value = SleepStats(hours, minutes, score)
        _sleepQuality.value = score

        if (totalSleep > 0) {
            val deepPct = (deep * 100) / totalSleep
            val lightPct = (light * 100) / totalSleep
            val remPct = (rem * 100) / totalSleep
            _stagePercentages.value = Triple(deepPct, lightPct, remPct)
        } else {
            _stagePercentages.value = Triple(0, 0, 0)
        }

        _sleepTimeline.value = generateTimeline(sourceList)

        Timber.i("✅ Sleep: ${hours}h ${minutes}m, Deep=$deep, Light=$light, REM=$rem, Awake=$awake, Score=$score")
    }

    // ============ Process Multi-Day Sleep (weekly/monthly) ============

    private fun processMultiDaySleep(metricsData: List<MetricData>) {
        var totalDeep = 0
        var totalLight = 0
        var totalRem = 0
        var totalAwake = 0
        var daysWithData = 0

        metricsData.forEach { metric ->
            val values = metric.values
            val deep = values.count { it == 1 }
            val light = values.count { it == 2 }
            val rem = values.count { it == 4 }
            val awake = values.count { it == 3 }
            val daySleep = deep + light + rem

            if (daySleep > 0) {
                totalDeep += deep
                totalLight += light
                totalRem += rem
                totalAwake += awake
                daysWithData++
            }
        }

        if (daysWithData == 0) {
            clearData()
            return
        }

        // Average per day
        val avgDeep = totalDeep / daysWithData
        val avgLight = totalLight / daysWithData
        val avgRem = totalRem / daysWithData
        val avgAwake = totalAwake / daysWithData
        val avgTotalSleep = avgDeep + avgLight + avgRem

        _deepMinutes.value = avgDeep
        _lightMinutes.value = avgLight
        _remMinutes.value = avgRem
        _awakeMinutes.value = avgAwake
        _totalSleepMinutes.value = avgTotalSleep

        val hours = avgTotalSleep / 60
        val minutes = avgTotalSleep % 60
        val score = calculateSleepQuality(avgDeep, avgLight, avgRem, avgAwake)

        _sleepStats.value = SleepStats(hours, minutes, score)
        _sleepQuality.value = score

        if (avgTotalSleep > 0) {
            val deepPct = (avgDeep * 100) / avgTotalSleep
            val lightPct = (avgLight * 100) / avgTotalSleep
            val remPct = (avgRem * 100) / avgTotalSleep
            _stagePercentages.value = Triple(deepPct, lightPct, remPct)
        } else {
            _stagePercentages.value = Triple(0, 0, 0)
        }

        // No timeline for multi-day view
        _sleepTimeline.value = emptyList()

        Timber.i("✅ Sleep avg over $daysWithData days: ${hours}h ${minutes}m, Score=$score")
    }

    // ============ Timeline Generation ============

    private fun generateTimeline(sourceList: List<Int>): List<SleepSegment> {
        if (sourceList.isEmpty()) return emptyList()

        val segments = mutableListOf<SleepSegment>()
        val totalMinutes = sourceList.size

        var currentStage: SleepStage? = null
        var segmentStart = 0

        sourceList.forEachIndexed { index, value ->
            val stage = when (value) {
                0 -> null  // Activity - skip
                1 -> SleepStage.DEEP
                2 -> SleepStage.LIGHT
                3 -> SleepStage.AWAKE
                4 -> SleepStage.REM
                else -> null
            }

            if (stage != currentStage) {
                if (currentStage != null && currentStage != SleepStage.AWAKE) {
                    val startRatio = segmentStart.toFloat() / totalMinutes
                    val widthRatio = (index - segmentStart).toFloat() / totalMinutes

                    if (widthRatio > 0.001f) {
                        segments.add(
                            SleepSegment(
                                stage = currentStage!!,
                                startRatio = startRatio,
                                widthRatio = widthRatio.coerceAtLeast(0.01f)
                            )
                        )
                    }
                }
                currentStage = stage
                segmentStart = index
            }
        }

        // Add final segment
        if (currentStage != null && currentStage != SleepStage.AWAKE) {
            val startRatio = segmentStart.toFloat() / totalMinutes
            val widthRatio = (totalMinutes - segmentStart).toFloat() / totalMinutes

            if (widthRatio > 0.001f) {
                segments.add(
                    SleepSegment(
                        stage = currentStage!!,
                        startRatio = startRatio,
                        widthRatio = widthRatio.coerceAtLeast(0.01f)
                    )
                )
            }
        }

        Timber.d("📊 Generated ${segments.size} sleep segments")
        return segments
    }

    // ============ Quality Calculation ============

    private fun calculateSleepQuality(deep: Int, light: Int, rem: Int, awake: Int): Int {
        val total = deep + light + rem + awake
        if (total == 0) return 0

        val deepWeight = deep * 0.4
        val remWeight = rem * 0.35
        val lightWeight = light * 0.2
        val awakeWeight = awake * -0.1

        val rawScore = deepWeight + remWeight + lightWeight + awakeWeight
        val normalized = (rawScore / total) * 100

        return normalized.coerceIn(0.0, 100.0).toInt()
    }

    // ============ Clear Data ============

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

    fun getSleepQualityLabel(): String {
        return when {
            _sleepStats.value.score >= 85 -> "عالی"
            _sleepStats.value.score >= 70 -> "خوب"
            _sleepStats.value.score >= 50 -> "متوسط"
            _sleepStats.value.score > 0 -> "ضعیف"
            else -> ""
        }
    }
}