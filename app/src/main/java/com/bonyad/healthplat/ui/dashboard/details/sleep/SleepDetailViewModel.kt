package com.bonyad.healthplat.ui.dashboard.details.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.repository.HealthDataRepository
import com.bonyad.healthplat.data.repository.MetricType
import com.bonyad.healthplat.domain.model.MetricData
import com.bonyad.healthplat.domain.model.RecordDataResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class SleepDetailViewModel @Inject constructor(
    private val deviceManager: HealthDeviceManager,
    private val healthRepository: HealthDataRepository,
    private val userPreferences: UserPreferencesDataStore
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
        loadSleepData(0)
    }

    fun refreshData() {
        loadSleepData(_selectedDayOffset.value)
    }

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
        loadSleepData(_selectedDayOffset.value)
    }

    fun selectDay(offset: Int) {
        _selectedDayOffset.value = offset
        loadSleepData(offset)
    }

    // ============ PRIMARY: Load from Ring ============

    private fun loadSleepData(offset: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            updateDateLabel(offset)

            try {
                // PRIMARY: Try ring first
                val result = deviceManager.getRecordData(offset)

                if (result is RecordDataResult.Success &&
                    result.sleep?.sourceList?.any { it > 0 } == true) {

                    val sleepList = result.sleep.sourceList
                    Timber.i("📊 Sleep from RING: ${sleepList.size} values")
                    processSleepData(sleepList)

                } else {
                    // FALLBACK: Try API
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

    // ============ FALLBACK: Load from API ============

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

                        // Backend bug: data might be duplicated
                        // Sleep should be 1440 values (1 per minute for 24 hours)
                        // If we have more, take only first 1440
                        val cleanedValues = if (values.size > 1440 && values.size % 1440 == 0) {
                            Timber.w("⚠️ API sleep data duplicated (${values.size}), taking first 1440")
                            values.take(1440)
                        } else {
                            values
                        }

                        processSleepData(cleanedValues)
                    } else {
                        clearData()
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "❌ Sleep API error")
                    clearData()
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "❌ Sleep API fallback failed")
            clearData()
        }
    }

    // ============ Process Sleep Data (same for ring & API) ============

    private fun processSleepData(sourceList: List<Int>) {
        if (sourceList.isEmpty()) {
            clearData()
            return
        }

        // Count minutes per stage
        // Sleep values: 0=Activity, 1=Deep, 2=Light, 3=Awake, 4=REM
        val deep = sourceList.count { it == 1 }
        val light = sourceList.count { it == 2 }
        val awake = sourceList.count { it == 3 }
        val rem = sourceList.count { it == 4 }

        val totalSleep = deep + light + rem  // Don't count awake in total sleep

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

        // Update stats
        val hours = totalSleep / 60
        val minutes = totalSleep % 60
        val score = calculateSleepQuality(deep, light, rem, awake)

        _sleepStats.value = SleepStats(hours, minutes, score)
        _sleepQuality.value = score

        // Calculate percentages for donut
        if (totalSleep > 0) {
            val deepPct = (deep * 100) / totalSleep
            val lightPct = (light * 100) / totalSleep
            val remPct = (rem * 100) / totalSleep
            _stagePercentages.value = Triple(deepPct, lightPct, remPct)
        } else {
            _stagePercentages.value = Triple(0, 0, 0)
        }

        // Generate timeline segments
        _sleepTimeline.value = generateTimeline(sourceList)

        Timber.i("✅ Sleep: ${hours}h ${minutes}m, Deep=$deep, Light=$light, REM=$rem, Awake=$awake, Score=$score")
    }

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

            // Detect stage change
            if (stage != currentStage) {
                // Save previous segment (skip AWAKE for cleaner chart)
                if (currentStage != null && currentStage != SleepStage.AWAKE) {
                    val startRatio = segmentStart.toFloat() / totalMinutes
                    val widthRatio = (index - segmentStart).toFloat() / totalMinutes

                    if (widthRatio > 0.001f) {  // Only add if visible
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

    private fun calculateSleepQuality(deep: Int, light: Int, rem: Int, awake: Int): Int {
        val total = deep + light + rem + awake
        if (total == 0) return 0

        // Weight: Deep sleep most important, then REM, then light
        // Awake time penalizes score
        val deepWeight = deep * 0.4
        val remWeight = rem * 0.35
        val lightWeight = light * 0.2
        val awakeWeight = awake * -0.1

        val rawScore = deepWeight + remWeight + lightWeight + awakeWeight
        val normalized = (rawScore / total) * 100

        return normalized.coerceIn(0.0, 100.0).toInt()
    }

    private fun updateDateLabel(offset: Int) {
        val today = LocalDate.now()
        val targetDate = today.plusDays(offset.toLong())

        _dateLabel.value = when (offset) {
            0 -> "امشب"
            -1 -> "دیشب"
            else -> "${targetDate.dayOfMonth} ${getPersianMonth(targetDate.monthValue)}"
        }
    }

    private fun getPersianMonth(month: Int): String {
        return when (month) {
            1 -> "ژانویه"; 2 -> "فوریه"; 3 -> "مارس"; 4 -> "آوریل"
            5 -> "مه"; 6 -> "ژوئن"; 7 -> "جولای"; 8 -> "اوت"
            9 -> "سپتامبر"; 10 -> "اکتبر"; 11 -> "نوامبر"; 12 -> "دسامبر"
            else -> ""
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