package com.bonyad.healthplat.ui.dashboard.details.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.data.repository.HealthDataRepository
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
import javax.inject.Inject

@HiltViewModel
class SleepDetailViewModel @Inject constructor(
    private val deviceManager: HealthDeviceManager
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


    // 1. Sleep Timeline Data
    // We need segments to draw the blocks on the time axis
    data class SleepSegment(
        val stage: SleepStage,
        val startRatio: Float, // 0.0 (21:00) to 1.0 (09:00)
        val widthRatio: Float  // Width of the block
    )

    enum class SleepStage { AWAKE, LIGHT, DEEP, REM }

    private val _sleepTimeline = MutableStateFlow<List<SleepSegment>>(emptyList())
    val sleepTimeline: StateFlow<List<SleepSegment>> = _sleepTimeline.asStateFlow()

    // 2. Stats
    private val _sleepStats = MutableStateFlow(SleepStats(8, 40, 95)) // 8h 40m, Score 95
    val sleepStats: StateFlow<SleepStats> = _sleepStats.asStateFlow()

    // 3. Percentages for Donut
    private val _stagePercentages = MutableStateFlow(Triple(29, 18, 53)) // Deep, Light, REM (approx from image)
    val stagePercentages: StateFlow<Triple<Int, Int, Int>> = _stagePercentages.asStateFlow()

    data class SleepStats(val hours: Int, val minutes: Int, val score: Int)

    private val _selectedTimeRange = MutableStateFlow("روزانه")
    val selectedTimeRange: StateFlow<String> = _selectedTimeRange.asStateFlow()

    private val _selectedDayOffset = MutableStateFlow(0) // 0 = today
    val selectedDayOffset = _selectedDayOffset.asStateFlow()

    init {
        loadSleepForDay(_selectedDayOffset.value)
//        loadMockData()
//        loadSleepData()
    }

    private fun loadMockData() {
        // Mocking segments to look like the screenshot
        // Chart spans 12 hours: 21:00 to 09:00
        val segments = listOf(
            // Light Sleep (Green)
            SleepSegment(SleepStage.LIGHT, 0.26f, 0.02f),
            SleepSegment(SleepStage.LIGHT, 0.45f, 0.02f),
            SleepSegment(SleepStage.LIGHT, 0.60f, 0.05f),
            SleepSegment(SleepStage.LIGHT, 0.70f, 0.02f), // Tall bar on right

            // Deep Sleep (Blue)
            SleepSegment(SleepStage.DEEP, 0.30f, 0.05f),
            SleepSegment(SleepStage.DEEP, 0.40f, 0.06f),
            SleepSegment(SleepStage.DEEP, 0.48f, 0.05f),
            SleepSegment(SleepStage.DEEP, 0.55f, 0.04f),
            SleepSegment(SleepStage.DEEP, 0.66f, 0.04f),

            // REM (Purple)
            SleepSegment(SleepStage.REM, 0.28f, 0.015f),
            SleepSegment(SleepStage.REM, 0.36f, 0.03f),
            SleepSegment(SleepStage.REM, 0.52f, 0.015f),
            SleepSegment(SleepStage.REM, 0.58f, 0.03f),
        )
        _sleepTimeline.value = segments
    }

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
    }

    fun loadSleepForDay(offset: Int) {
        viewModelScope.launch {
            try {
                val result = deviceManager.getRecordData(offset)

                if (result is RecordDataResult.Success) {
                    result.sleep?.let { sleepBean ->
                        val sourceList = sleepBean.sourceList ?: return@let

                        if (sourceList.isEmpty()) {
                            Timber.w("No sleep data for day $offset")
                            return@let
                        }

                        // Calculate minutes per stage
                        val deep = sourceList.count { it == 1 }
                        val light = sourceList.count { it == 2 }
                        val awake = sourceList.count { it == 3 }
                        val rem = sourceList.count { it == 4 }

                        val total = deep + light + rem + awake

                        _deepMinutes.value = deep
                        _lightMinutes.value = light
                        _awakeMinutes.value = awake
                        _remMinutes.value = rem
                        _totalSleepMinutes.value = total

                        // Update stats
                        val hours = total / 60
                        val minutes = total % 60
                        val score = calculateSleepQuality(deep, light, rem, awake)

                        _sleepStats.value = SleepStats(hours, minutes, score)

                        // Calculate percentages for donut
                        if (total > 0) {
                            val deepPct = (deep * 100) / total
                            val lightPct = (light * 100) / total
                            val remPct = (rem * 100) / total
                            _stagePercentages.value = Triple(deepPct, lightPct, remPct)
                        }

                        // Generate timeline
                        _sleepTimeline.value = generateTimeline(sourceList)

                        Timber.i("✅ Sleep loaded: ${hours}h ${minutes}m, score=$score")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading sleep for day $offset")
            }
        }
    }

    private fun generateTimeline(sourceList: List<Int>): List<SleepSegment> {
        val segments = mutableListOf<SleepSegment>()

        // Assume sleep starts at 21:00 (9 PM) and spans 12 hours
        val totalMinutes = sourceList.size

        var currentStage: SleepStage? = null
        var segmentStart = 0

        sourceList.forEachIndexed { index, value ->
            val stage = when (value) {
                0 -> null // Activity, skip
                1 -> SleepStage.DEEP
                2 -> SleepStage.LIGHT
                3 -> SleepStage.AWAKE
                4 -> SleepStage.REM
                else -> null
            }

            // Detect stage change
            if (stage != currentStage) {
                // Save previous segment
                if (currentStage != null) {
                    val startRatio = segmentStart.toFloat() / totalMinutes
                    val widthRatio = (index - segmentStart).toFloat() / totalMinutes

                    segments.add(
                        SleepSegment(
                            stage = currentStage!!,
                            startRatio = startRatio,
                            widthRatio = widthRatio
                        )
                    )
                }
                currentStage = stage
                segmentStart = index
            }
        }

        // Add final segment
        if (currentStage != null) {
            val startRatio = segmentStart.toFloat() / totalMinutes
            val widthRatio = (totalMinutes - segmentStart).toFloat() / totalMinutes

            segments.add(
                SleepSegment(
                    stage = currentStage!!,
                    startRatio = startRatio,
                    widthRatio = widthRatio
                )
            )
        }

        return segments
    }

    fun selectDay(offset: Int) {
        _selectedDayOffset.value = offset
        loadSleepForDay(offset)
    }

    private fun calculateSleepQuality(
        deep: Int,
        light: Int,
        rem: Int,
        awake: Int
    ): Int {
        val total = deep + light + rem + awake
        if (total == 0) return 0

        val deepWeight = deep * 0.5
        val remWeight = rem * 0.3
        val lightWeight = light * 0.1

        val rawScore = deepWeight + remWeight + lightWeight
        val normalized = (rawScore / total) * 100

        return normalized.coerceIn(0.0, 100.0).toInt()
    }

    private fun loadSleepData() {
        viewModelScope.launch {
            try {
                val result = deviceManager.getRecordData(0)

                if (result is RecordDataResult.Success) {
                    val sleepBean = result.sleep

                    if (sleepBean?.sourceList != null) {

                        val deep = sleepBean.sourceList.count { it == 1 }
                        val light = sleepBean.sourceList.count { it == 2 }
                        val awake = sleepBean.sourceList.count { it == 3 }
                        val rem = sleepBean.sourceList.count { it == 4 }

                        _deepMinutes.value = deep
                        _lightMinutes.value = light
                        _awakeMinutes.value = awake
                        _remMinutes.value = rem

                        val total = deep + light + rem + awake
                        _totalSleepMinutes.value = total

                        // Basic quality formula (you can adjust it)
                        val sleepScore = calculateSleepQuality(
                            deep = deep,
                            light = light,
                            rem = rem,
                            awake = awake
                        )
                        _sleepQuality.value = sleepScore

                        Timber.i(
                            "Sleep Loaded: deep=$deep, light=$light, rem=$rem, awake=$awake, total=$total, score=$sleepScore"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load sleep data")
            }
        }
    }

}