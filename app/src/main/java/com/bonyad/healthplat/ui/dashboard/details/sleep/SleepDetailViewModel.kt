package com.bonyad.healthplat.ui.dashboard.details.sleep

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.repository.AuthResult
import com.bonyad.healthplat.data.repository.CareRepository
import com.bonyad.healthplat.data.repository.HealthDataRepository
import com.bonyad.healthplat.data.repository.MetricType
import com.bonyad.healthplat.data.repository.SleepAnalysisRepository
import com.bonyad.healthplat.domain.model.MetricData
import com.bonyad.healthplat.domain.model.RecordDataResult
import com.bonyad.healthplat.domain.model.SleepAnalysisData
import com.bonyad.healthplat.ui.utils.PersianDateUtils
import com.bonyad.healthplat.ui.utils.SleepDataHelper
import com.bonyad.healthplat.ui.utils.rtl
import com.bonyad.healthplat.ui.utils.toFarsiDigits
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import saman.zamani.persiandate.PersianDate
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.GregorianCalendar
import javax.inject.Inject
import com.bonyad.healthplat.ui.components.PersianDate as UiPersianDate

@HiltViewModel
class SleepDetailViewModel @Inject constructor(
    private val deviceManager: HealthDeviceManager,
    private val healthRepository: HealthDataRepository,
    private val userPreferences: UserPreferencesDataStore,
    private val sleepDataHelper: SleepDataHelper,
    private val sleepAnalysisRepository: SleepAnalysisRepository,
    private val careRepository: CareRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val patientUserId: String? = savedStateHandle.get<String>("patientUserId")
    val isCaregiverMode: Boolean = patientUserId != null

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

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Date label
    private val _dateLabel = MutableStateFlow("")
    val dateLabel: StateFlow<String> = _dateLabel.asStateFlow()

    // ============ New Sleep Analysis State ============

    /**
     * Represents a single sleep session (nap or main sleep) with its time range.
     */
    data class SleepSession(
        val type: Int,          // 0 = Nap, 1 = Sleep
        val startTime: String,  // "HH:mm"
        val endTime: String     // "HH:mm"
    )

    /**
     * Consolidated UI state for the new sleep analysis data.
     * Only populated when the new analysis API returns data.
     */
    data class SleepAnalysisUiState(
        val hasData: Boolean = false,
        val userName: String = "",

        // Session info
        val sessions: List<SleepSession> = emptyList(),
        val selectedSessionIndex: Int = 0,

        // Sleep details card
        val totalDuration: Int = 0,       // minutes
        val netDuration: Int = 0,         // minutes
        val efficiency: Int = 0,          // %
        val restfulnessLabel: String = "",
        val remDuration: Int = 0,         // minutes
        val deepDuration: Int = 0,        // minutes
        val latency: Int = 0,            // minutes

        // Key metrics
        val avgHeartRate: Int = 0,
        val avgSpO2: Int = 0,
        val avgHrv: Int = 0,

        // Breathing
        val breathingLabel: String = "",
        val breathingDescription: String = "",

        // Chart data (selected session)
        val hrChartData: List<Int> = emptyList(),
        val hrvChartData: List<Int> = emptyList(),
        val hrTimestamps: List<String> = emptyList(),
        val hrvTimestamps: List<String> = emptyList(),

        // Timeline X-axis labels
        val timelineXLabels: List<String> = emptyList(),

        // Sleep debt (minutes)
        val sleepDebtMinutes: Int = 0
    )

    private val _analysisState = MutableStateFlow(SleepAnalysisUiState())
    val analysisState: StateFlow<SleepAnalysisUiState> = _analysisState.asStateFlow()

    // Cache raw analysis data so we can re-process when session changes
    private var cachedAnalysisData: SleepAnalysisData? = null
    private var cachedUserName: String = ""

    init {
        loadSleepData()
    }

    /**
     * Switch to a different sleep session (nap or main sleep).
     * Re-processes all UI state from the cached API data for the selected session index.
     */
    fun selectSession(index: Int) {
        val data = cachedAnalysisData ?: return
        if (index < 0 || index >= data.types.size) return
        viewModelScope.launch {
            processSessionAtIndex(data, index, cachedUserName)
        }
    }

    // ============ Sync: Ring → Server → API fetch → Display ============

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            if (!isCaregiverMode) {
                try {
                    val syncDay = ChronoUnit.DAYS.between(_selectedDate.value, LocalDate.now()).toInt().coerceIn(0, 6)
                    healthRepository.syncDashboardData(syncDay)
                    Timber.i("Ring sleep data synced to server for day $syncDay")
                } catch (e: Exception) {
                    Timber.e(e, "Ring->Server sync failed, loading from API anyway")
                }
            }
            loadSleepData()
        }
    }

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
        loadSleepData()
    }

    fun selectDay(date: LocalDate) {
        _selectedDate.value = date
        if (_selectedTimeRange.value == "روزانه") {
            loadSleepData()
        }
    }

    fun selectDate(date: UiPersianDate) {
        _selectedDate.value = LocalDate.parse(date.toGregorianIsoDate())
        _selectedTimeRange.value = "روزانه"
        loadSleepData()
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

    /**
     * Chart window: 21:00→09:00 = 720 minutes.
     * Yesterday's indices 1260..1439 (21:00-23:59) = first 180 min of window.
     * Today's indices 0..539 (00:00-08:59) = remaining 540 min of window.
     */
    companion object {
        private const val WINDOW_START_MINUTE = 21 * 60
        private const val WINDOW_TOTAL_MINUTES = 720
        private const val MINUTES_PER_DAY = 1440
        private const val POST_MIDNIGHT_END = 9 * 60

        private val TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        private val TIME_ONLY_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

        fun ratingLabel(value: Int): String = when (value) {
            0 -> "نیازمند توجه"
            1 -> "متوسط"
            2 -> "خوب"
            3 -> "عالی"
            else -> ""
        }
    }

    private fun loadSleepData() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                if (isCaregiverMode) {
                    _analysisState.value = SleepAnalysisUiState()
                    loadCaregiverSleep()
                } else {
                    when (_selectedTimeRange.value) {
                        "روزانه" -> loadDailySleep()
                        "هفتگی", "ماهانه" -> {
                            _analysisState.value = SleepAnalysisUiState()
                            loadMultiDaySleep()
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load sleep data")
                if (!isCaregiverMode) {
                    loadSleepFromRing(ChronoUnit.DAYS.between(_selectedDate.value, LocalDate.now()).toInt().coerceIn(0, 6))
                } else {
                    clearData()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadCaregiverSleep() {
        when (_selectedTimeRange.value) {
            "روزانه" -> loadCaregiverDailySleep()
            "هفتگی", "ماهانه" -> loadCaregiverMultiDaySleep()
        }
    }

    /**
     * Daily caregiver sleep: use the sleep analysis API with the patient's ID,
     * falling back to the legacy caregiver API if the analysis API fails.
     */
    private suspend fun loadCaregiverDailySleep() {
        val date = _selectedDate.value
        updateDateLabel(date)
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

        Timber.i("📡 Fetching caregiver sleep analysis for patient $patientUserId on $dateStr")

        sleepAnalysisRepository.getSleepAnalysisForUser(patientUserId!!, dateStr).fold(
            onSuccess = { data ->
                processSleepAnalysisData(data)
                Timber.i("✅ Caregiver sleep loaded from analysis API")
            },
            onFailure = { error ->
                Timber.w(error, "Sleep analysis API failed for patient, falling back to legacy caregiver API")
                _analysisState.value = SleepAnalysisUiState()
                loadCaregiverDailySleepLegacy()
            }
        )
    }

    /**
     * Fallback: use /api/Caregiver/Sleep for daily sleep when analysis API is unavailable.
     */
    private suspend fun loadCaregiverDailySleepLegacy() {
        val today = _selectedDate.value
        val yesterday = today.minusDays(1)
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val dateFrom = yesterday.format(formatter)
        val dateTo = today.format(formatter)

        when (val result = careRepository.getPatientSleep(patientUserId!!, dateFrom, dateTo)) {
            is AuthResult.Success -> {
                val metricsData = result.data
                if (metricsData.isNotEmpty()) {
                    val todayStr = today.format(formatter)
                    val yesterdayStr = yesterday.format(formatter)

                    val todayValues = metricsData
                        .find { it.recordDate.startsWith(todayStr) }
                        ?.values?.let { cleanDayValues(it) } ?: emptyList()
                    val yesterdayValues = metricsData
                        .find { it.recordDate.startsWith(yesterdayStr) }
                        ?.values?.let { cleanDayValues(it) } ?: emptyList()

                    val fullData = buildFullSleepData(yesterdayValues, todayValues)
                    val chartWindow = buildSleepWindow(yesterdayValues, todayValues)
                    processSleepData(fullData, chartWindow)
                    Timber.i("✅ Caregiver sleep loaded from legacy API")
                } else {
                    clearData()
                }
            }
            is AuthResult.Error -> {
                Timber.e("❌ Caregiver legacy sleep error: ${result.message}")
                clearData()
            }
        }
    }

    /**
     * Weekly/Monthly caregiver sleep: use /api/Caregiver/Sleep for multi-day ranges.
     */
    private suspend fun loadCaregiverMultiDaySleep() {
        _analysisState.value = SleepAnalysisUiState()
        val (dateFrom, dateTo) = getDateRange()

        Timber.i("📡 Fetching caregiver sleep: $dateFrom to $dateTo for patient $patientUserId")

        when (val result = careRepository.getPatientSleep(patientUserId!!, dateFrom, dateTo)) {
            is AuthResult.Success -> {
                if (result.data.isNotEmpty()) {
                    processMultiDaySleep(result.data)
                    Timber.i("✅ Caregiver multi-day sleep loaded: ${result.data.size} records")
                } else {
                    clearData()
                }
            }
            is AuthResult.Error -> {
                Timber.e("❌ Caregiver multi-day sleep error: ${result.message}")
                clearData()
            }
        }
    }

    /**
     * Daily sleep: try the new analysis API first, fall back to legacy.
     */
    private suspend fun loadDailySleep() {
        val date = _selectedDate.value
        updateDateLabel(date)
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

        Timber.i("Fetching sleep analysis for $dateStr")

        sleepAnalysisRepository.getSleepAnalysis(dateStr).fold(
            onSuccess = { data ->
                processSleepAnalysisData(data)
                Timber.i("Sleep loaded from analysis API")
            },
            onFailure = { error ->
                Timber.w(error, "Sleep analysis API failed, falling back to legacy")
                _analysisState.value = SleepAnalysisUiState()
                loadDailySleepLegacy()
            }
        )
    }

    // ============ Process New Sleep Analysis Data ============

    private suspend fun processSleepAnalysisData(data: SleepAnalysisData) {
        if (data.types.isEmpty()) {
            clearData()
            return
        }

        // Cache for session switching
        cachedAnalysisData = data
        cachedUserName = try {
            userPreferences.getUserName().first() ?: ""
        } catch (e: Exception) { "" }

        // Default to main sleep session (type == 1), fallback to last session
        val defaultIdx = data.types.indexOf(1).takeIf { it >= 0 } ?: (data.types.size - 1)
        processSessionAtIndex(data, defaultIdx, cachedUserName)
    }

    /**
     * Extracts and updates all UI state for the given session index.
     * Called on initial load (main sleep) and when user switches sessions.
     */
    private fun processSessionAtIndex(data: SleepAnalysisData, sessionIdx: Int, userName: String) {
        // Build session list with start/end times
        val sessions = data.timestamps.mapIndexed { idx, timestamps ->
            val startTime = try {
                LocalDateTime.parse(timestamps.first(), TIMESTAMP_FORMATTER)
                    .format(TIME_ONLY_FORMATTER)
            } catch (e: Exception) { "" }

            val endTime = try {
                LocalDateTime.parse(timestamps.last(), TIMESTAMP_FORMATTER)
                    .format(TIME_ONLY_FORMATTER)
            } catch (e: Exception) { "" }

            SleepSession(
                type = data.types.getOrElse(idx) { 0 },
                startTime = startTime,
                endTime = endTime
            )
        }

        // Stage durations from selected session
        val stageDurations = data.perStageDurations.getOrNull(sessionIdx) ?: emptyMap()
        val deep = stageDurations["1"] ?: 0
        val light = stageDurations["2"] ?: 0
        val awake = stageDurations["3"] ?: 0
        val rem = stageDurations["4"] ?: 0
        val totalSleep = deep + light + rem

        // Update existing state flows for backward compatibility
        _deepMinutes.value = deep
        _lightMinutes.value = light
        _remMinutes.value = rem
        _awakeMinutes.value = awake
        _totalSleepMinutes.value = totalSleep

        val hours = totalSleep / 60
        val minutes = totalSleep % 60

        // Efficiency & restfulness for selected session
        val efficiency = data.efficiencies.getOrNull(sessionIdx) ?: 0
        val restfulness = data.restfulnesses.getOrNull(sessionIdx) ?: 0

        // Score: derive from timings or calculate locally
        val timing = data.timings.getOrNull(sessionIdx)
        val score = if (timing != null) {
            when (timing) {
                3 -> 95
                2 -> 80
                1 -> 65
                else -> 40
            }
        } else {
            calculateSleepQuality(deep, light, rem, awake)
        }

        _sleepStats.value = SleepStats(hours, minutes, score)
        _sleepQuality.value = score

        // Stage percentages
        if (totalSleep > 0) {
            _stagePercentages.value = Triple(
                (deep * 100) / totalSleep,
                (light * 100) / totalSleep,
                (rem * 100) / totalSleep
            )
        } else {
            _stagePercentages.value = Triple(0, 0, 0)
        }

        // Timeline from selected session stages
        val stagesList = data.stages.getOrNull(sessionIdx) ?: emptyList()
        _sleepTimeline.value = generateTimeline(stagesList)

        // X-axis labels from timestamps
        val sessionTimestamps = data.timestamps.getOrNull(sessionIdx) ?: emptyList()
        val xLabels = generateXLabelsFromTimestamps(sessionTimestamps)

        // HR/HRV/SpO2 data from selected session
        val sessionHr = data.hr.getOrNull(sessionIdx) ?: emptyList()
        val sessionHrv = data.hrv.getOrNull(sessionIdx) ?: emptyList()
        val sessionSpo2 = data.spo2.getOrNull(sessionIdx) ?: emptyList()

        val avgHr = sessionHr.filter { it > 0 }.let { if (it.isNotEmpty()) it.average().toInt() else 0 }
        val avgHrv = sessionHrv.filter { it > 0 }.let { if (it.isNotEmpty()) it.average().toInt() else 0 }
        val avgSpo2 = sessionSpo2.filter { it > 0 }.let { if (it.isNotEmpty()) it.average().toInt() else 0 }

        // Breathing
        val breathingIrregular = data.breathingIrregularity.getOrNull(sessionIdx) ?: 0
        val breathingLabel = if (breathingIrregular == 0) "مطلوب" else "نیازمند توجه"
        val breathingDesc = if (breathingIrregular == 0) {
            "هیچ تغییر قابل توجهی در سطح اکسیژن خون شما مشاهده نشد. این می‌تواند نشان دهد که شما هیچ اختلال تنفسی در خواب خود تجربه نکرده‌اید.".rtl()
        } else {
            "تغییرات قابل توجهی در سطح اکسیژن خون شما در طول خواب مشاهده شده است. توصیه می‌شود با پزشک خود مشورت کنید.".rtl()
        }

        // HR/HRV timestamps for chart x-axis
        val hrTimestamps = generateChartTimestamps(sessionTimestamps, sessionHr.size)
        val hrvTimestamps = generateChartTimestamps(sessionTimestamps, sessionHrv.size)

        // Update analysis state
        _analysisState.value = SleepAnalysisUiState(
            hasData = true,
            userName = userName,
            sessions = sessions,
            selectedSessionIndex = sessionIdx,
            totalDuration = data.totalDurations.getOrNull(sessionIdx) ?: 0,
            netDuration = data.netDurations.getOrNull(sessionIdx) ?: 0,
            efficiency = efficiency,
            restfulnessLabel = ratingLabel(restfulness),
            remDuration = rem,
            deepDuration = deep,
            latency = data.latencies.getOrNull(sessionIdx) ?: 0,
            avgHeartRate = avgHr,
            avgSpO2 = avgSpo2,
            avgHrv = avgHrv,
            breathingLabel = breathingLabel,
            breathingDescription = breathingDesc,
            hrChartData = sessionHr,
            hrvChartData = sessionHrv,
            hrTimestamps = hrTimestamps,
            hrvTimestamps = hrvTimestamps,
            timelineXLabels = xLabels,
            // Sleep debt from API
            sleepDebtMinutes = data.debt.getOrNull(sessionIdx) ?: 0
        )

        Timber.i("Sleep session[$sessionIdx]: ${hours}h ${minutes}m, Score=$score, Sessions=${sessions.size}, AvgHR=$avgHr, AvgHRV=$avgHrv")
    }

    /**
     * Generate 5 evenly-spaced time labels from the session timestamps for X-axis display.
     */
    private fun generateXLabelsFromTimestamps(timestamps: List<String>): List<String> {
        if (timestamps.isEmpty()) return emptyList()
        val count = 5
        val step = (timestamps.size - 1).coerceAtLeast(1) / (count - 1)
        return (0 until count).map { i ->
            val idx = (i * step).coerceAtMost(timestamps.size - 1)
            try {
                val dt = LocalDateTime.parse(timestamps[idx], TIMESTAMP_FORMATTER)
                dt.format(TIME_ONLY_FORMATTER).toFarsiDigits()
            } catch (e: Exception) { "" }
        }
    }

    /**
     * Generate evenly-spaced time labels for HR/HRV charts.
     */
    private fun generateChartTimestamps(timestamps: List<String>, dataSize: Int): List<String> {
        if (timestamps.isEmpty() || dataSize == 0) return emptyList()
        val count = 5
        val step = (timestamps.size - 1).coerceAtLeast(1) / (count - 1)
        return (0 until count).map { i ->
            val idx = (i * step).coerceAtMost(timestamps.size - 1)
            try {
                val dt = LocalDateTime.parse(timestamps[idx], TIMESTAMP_FORMATTER)
                dt.format(TIME_ONLY_FORMATTER).toFarsiDigits()
            } catch (e: Exception) { "" }
        }
    }

    // ============ LEGACY: Load daily from old API ============

    private suspend fun loadDailySleepLegacy() {
        val today = _selectedDate.value
        val yesterday = today.minusDays(1)
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        updateDateLabel(today)

        val dateFrom = yesterday.format(formatter)
        val dateTo = today.format(formatter)

        Timber.i("Fetching sleep from legacy API: $dateFrom to $dateTo")

        healthRepository.getMetricData(
            metricType = MetricType.SLEEP,
            dateFrom = dateFrom,
            dateTo = dateTo
        ).fold(
            onSuccess = { metricsData ->
                if (metricsData.isEmpty()) {
                    Timber.w("No sleep data from legacy API")
                    clearData()
                    return
                }

                val todayStr = today.format(formatter)
                val yesterdayStr = yesterday.format(formatter)

                val todayValues = metricsData
                    .find { it.recordDate.startsWith(todayStr) }
                    ?.values?.let { cleanDayValues(it) } ?: emptyList()
                val yesterdayValues = metricsData
                    .find { it.recordDate.startsWith(yesterdayStr) }
                    ?.values?.let { cleanDayValues(it) } ?: emptyList()

                val fullData = buildFullSleepData(yesterdayValues, todayValues)
                val chartWindow = buildSleepWindow(yesterdayValues, todayValues)
                processSleepData(fullData, chartWindow)

                Timber.i("Sleep loaded from legacy API (combined window)")
            },
            onFailure = { error ->
                Timber.e(error, "Legacy sleep API error, trying ring fallback...")
                loadSleepFromRing(ChronoUnit.DAYS.between(_selectedDate.value, LocalDate.now()).toInt().coerceIn(0, 6))
            }
        )
    }

    private suspend fun loadMultiDaySleep() {
        val (dateFrom, dateTo) = getDateRange()
        Timber.i("Fetching sleep from API: $dateFrom to $dateTo")

        healthRepository.getMetricData(
            metricType = MetricType.SLEEP,
            dateFrom = dateFrom,
            dateTo = dateTo
        ).fold(
            onSuccess = { metricsData ->
                if (metricsData.isNotEmpty()) {
                    processMultiDaySleep(metricsData)
                    Timber.i("Sleep loaded from API: ${metricsData.size} records")
                } else {
                    Timber.w("No sleep data from API")
                    clearData()
                }
            },
            onFailure = { error ->
                Timber.e(error, "Sleep API error")
                clearData()
            }
        )
    }

    private fun cleanDayValues(values: List<Int>): List<Int> {
        return if (values.size > MINUTES_PER_DAY && values.size % MINUTES_PER_DAY == 0) {
            Timber.w("API sleep data duplicated (${values.size}), taking first $MINUTES_PER_DAY")
            values.take(MINUTES_PER_DAY)
        } else {
            values
        }
    }

    private fun buildFullSleepData(yesterdayValues: List<Int>, todayValues: List<Int>): List<Int> {
        val result = mutableListOf<Int>()
        if (yesterdayValues.size >= MINUTES_PER_DAY) {
            result.addAll(yesterdayValues.subList(WINDOW_START_MINUTE, MINUTES_PER_DAY))
        }
        result.addAll(todayValues)
        return result
    }

    private fun buildSleepWindow(yesterdayValues: List<Int>, todayValues: List<Int>): List<Int> {
        val window = MutableList(WINDOW_TOTAL_MINUTES) { 0 }
        if (yesterdayValues.size >= MINUTES_PER_DAY) {
            for (i in WINDOW_START_MINUTE until MINUTES_PER_DAY) {
                window[i - WINDOW_START_MINUTE] = yesterdayValues[i]
            }
        }
        val preMidnightSize = MINUTES_PER_DAY - WINDOW_START_MINUTE
        if (todayValues.isNotEmpty()) {
            val postMidnightCount = minOf(POST_MIDNIGHT_END, todayValues.size)
            for (i in 0 until postMidnightCount) {
                window[preMidnightSize + i] = todayValues[i]
            }
        }
        return window
    }

    // ============ FALLBACK: Load from Ring ============

    private suspend fun loadSleepFromRing(offset: Int) {
        try {
            val todayResult = deviceManager.getRecordData(offset)
            val yesterdayResult = try {
                deviceManager.getRecordData(offset + 1)
            } catch (e: Exception) { null }

            val todayValues = (todayResult as? RecordDataResult.Success)?.sleep?.sourceList ?: emptyList()
            val yesterdayValues = (yesterdayResult as? RecordDataResult.Success)?.sleep?.sourceList ?: emptyList()

            val fullData = buildFullSleepData(yesterdayValues, todayValues)
            val chartWindow = buildSleepWindow(yesterdayValues, todayValues)
            val hasSleep = fullData.any { it == 1 || it == 2 || it == 4 }

            if (hasSleep) {
                Timber.i("Sleep loaded from ring fallback (combined window)")
                processSleepData(fullData, chartWindow)
            } else {
                Timber.w("No sleep data from ring either")
                clearData()
            }
        } catch (e: Exception) {
            Timber.e(e, "Ring fallback also failed")
            clearData()
        }
    }

    // ============ Process Sleep Data (legacy single day) ============

    private fun processSleepData(fullData: List<Int>, chartData: List<Int> = fullData) {
        if (fullData.isEmpty()) {
            clearData()
            return
        }

        val deep = fullData.count { it == 1 }
        val light = fullData.count { it == 2 }
        val awake = fullData.count { it == 3 }
        val rem = fullData.count { it == 4 }
        val totalSleep = deep + light + rem

        if (totalSleep == 0) {
            Timber.w("No actual sleep data (all activity/awake)")
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
            _stagePercentages.value = Triple(
                (deep * 100) / totalSleep,
                (light * 100) / totalSleep,
                (rem * 100) / totalSleep
            )
        } else {
            _stagePercentages.value = Triple(0, 0, 0)
        }

        _sleepTimeline.value = generateTimeline(chartData)

        Timber.i("Sleep: ${hours}h ${minutes}m, Deep=$deep, Light=$light, REM=$rem, Awake=$awake, Score=$score")
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
            _stagePercentages.value = Triple(
                (avgDeep * 100) / avgTotalSleep,
                (avgLight * 100) / avgTotalSleep,
                (avgRem * 100) / avgTotalSleep
            )
        } else {
            _stagePercentages.value = Triple(0, 0, 0)
        }

        _sleepTimeline.value = emptyList()

        Timber.i("Sleep avg over $daysWithData days: ${hours}h ${minutes}m, Score=$score")
    }

    // ============ Timeline Generation ============

    private fun generateTimeline(sourceList: List<Int>): List<SleepSegment> {
        if (sourceList.isEmpty()) return emptyList()

        val firstSleepIndex = sourceList.indexOfFirst { it == 1 || it == 2 || it == 4 }
        val lastSleepIndex = sourceList.indexOfLast { it == 1 || it == 2 || it == 4 }

        if (firstSleepIndex == -1) return emptyList()

        val segments = mutableListOf<SleepSegment>()
        val totalMinutes = sourceList.size

        var currentStage: SleepStage? = null
        var segmentStart = 0

        sourceList.forEachIndexed { index, value ->
            val isWithinSleepSession = index in firstSleepIndex..lastSleepIndex

            val stage = when {
                value == 1 -> SleepStage.DEEP
                value == 2 -> SleepStage.LIGHT
                value == 4 -> SleepStage.REM
                value == 3 && isWithinSleepSession -> SleepStage.AWAKE
                else -> null
            }

            if (stage != currentStage) {
                if (currentStage != null) {
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

        if (currentStage != null) {
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

        Timber.d("Generated ${segments.size} sleep segments")
        return segments
    }

    // ============ Quality Calculation ============

    private fun calculateSleepQuality(deep: Int, light: Int, rem: Int, awake: Int): Int {
        val totalSleep = deep + light + rem
        if (totalSleep == 0) return 0

        val totalInBed = totalSleep + awake

        val restorativeRatio = (deep + rem).toFloat() / totalSleep
        val restorativeScore = when {
            restorativeRatio >= 0.50f -> 40f
            restorativeRatio >= 0.40f -> 35f
            restorativeRatio >= 0.30f -> 28f
            restorativeRatio >= 0.20f -> 20f
            else -> 10f
        }

        val efficiency = totalSleep.toFloat() / totalInBed
        val efficiencyScore = (efficiency * 35f).coerceAtMost(35f)

        val sleepHours = totalSleep / 60f
        val durationScore = when {
            sleepHours in 7f..9f -> 25f
            sleepHours in 6f..7f || sleepHours in 9f..10f -> 20f
            sleepHours in 5f..6f || sleepHours in 10f..11f -> 14f
            sleepHours >= 4f -> 8f
            else -> 4f
        }

        val score = restorativeScore + efficiencyScore + durationScore
        return score.coerceIn(0f, 100f).toInt()
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
        _analysisState.value = SleepAnalysisUiState()
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
