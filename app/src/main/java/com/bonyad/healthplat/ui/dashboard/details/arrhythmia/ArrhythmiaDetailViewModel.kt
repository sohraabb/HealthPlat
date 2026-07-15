package com.bonyad.healthplat.ui.dashboard.details.arrhythmia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.ArrhythmiaApiService
import com.bonyad.healthplat.domain.model.ArrhythmiaPredictionData
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
import java.util.GregorianCalendar
import javax.inject.Inject

@HiltViewModel
class ArrhythmiaDetailViewModel @Inject constructor(
    private val arrhythmiaApiService: ArrhythmiaApiService,
    private val userPreferences: UserPreferencesDataStore
) : ViewModel() {

    data class ArrhythmiaPoint(val xRatio: Float, val value: Float)

    data class ArrhythmiaBarPoint(
        val label: String,
        val timeRatio: Float,
        val normalPercent: Float,
        val afibPercent: Float,
        val otherPercent: Float
    )

    data class ArrhythmiaDistribution(
        val normalPercent: Float = 0f,
        val afibPercent: Float = 0f,
        val otherPercent: Float = 0f,
        val totalScore: Int = 0
    )

    // ── Daily area chart points ──
    private val _chartPoints = MutableStateFlow<List<ArrhythmiaPoint>>(emptyList())
    val chartPoints: StateFlow<List<ArrhythmiaPoint>> = _chartPoints.asStateFlow()

    private val _chartPointsOthers = MutableStateFlow<List<ArrhythmiaPoint>>(emptyList())
    val chartPointsOthers: StateFlow<List<ArrhythmiaPoint>> = _chartPointsOthers.asStateFlow()

    // ── Weekly/monthly bar chart points ──
    private val _barChartData = MutableStateFlow<List<ArrhythmiaBarPoint>>(emptyList())
    val barChartData: StateFlow<List<ArrhythmiaBarPoint>> = _barChartData.asStateFlow()

    private val _distribution = MutableStateFlow(ArrhythmiaDistribution())
    val distribution: StateFlow<ArrhythmiaDistribution> = _distribution.asStateFlow()

    private val _selectedTimeRange = MutableStateFlow("روزانه")
    val selectedTimeRange: StateFlow<String> = _selectedTimeRange.asStateFlow()

    private val _selectedDayOffset = MutableStateFlow(0)
    val selectedDayOffset = _selectedDayOffset.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _dateLabel = MutableStateFlow("")
    val dateLabel: StateFlow<String> = _dateLabel.asStateFlow()

    private val _predictionLabel = MutableStateFlow("فیبریلاسیون دهلیزی")
    val predictionLabel: StateFlow<String> = _predictionLabel.asStateFlow()

    private val _xAxisLabels = MutableStateFlow<List<String>>(emptyList())
    val xAxisLabels: StateFlow<List<String>> = _xAxisLabels.asStateFlow()

    private var cachedDataList: List<ArrhythmiaPredictionData>? = null

    init {
        updateDateLabel(LocalDate.now())
        loadArrhythmiaData()
    }

    fun refreshData() {
        cachedDataList = null
        loadArrhythmiaData()
    }

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
        cachedDataList = null
        updateXAxisLabels()
        loadArrhythmiaData()
    }

    fun selectDay(offset: Int) {
        _selectedDayOffset.value = offset
        cachedDataList = null
        updateDateLabel(LocalDate.now().minusDays(offset.toLong()))
        if (_selectedTimeRange.value == "روزانه") {
            loadArrhythmiaData()
        }
    }

    private fun updateXAxisLabels() {
        val today = LocalDate.now()
        _xAxisLabels.value = when (_selectedTimeRange.value) {
            "هفتگی" -> {
                val persianLabels = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
                val todayIdx = when (today.dayOfWeek.value) {
                    6 -> 0; 7 -> 1; else -> today.dayOfWeek.value + 1
                }
                (6 downTo 0).map { i -> persianLabels[(todayIdx - i + 7) % 7] }
            }
            "ماهانه" -> {
                (1..4).map { "هفته ${it.toString().toFarsiDigits()}" }
            }
            else -> emptyList()
        }
    }

    private fun getDateRange(): Pair<String, String> {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        return when (_selectedTimeRange.value) {
            "روزانه" -> {
                val targetDate = today.minusDays(_selectedDayOffset.value.toLong())
                val dateStr = targetDate.format(formatter)
                updateDateLabel(targetDate)
                Pair(dateStr, dateStr)
            }
            "هفتگی" -> {
                val startDate = today.minusDays(6)
                _dateLabel.value = "هفته گذشته"
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

    /**
     * Loads arrhythmia prediction data from the API.
     * For daily view: processes single-day data into area chart points.
     * For weekly/monthly: processes multi-day data into bar chart points.
     */
    private fun loadArrhythmiaData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val (dateFrom, dateTo) = getDateRange()
                Timber.i("📡 Fetching arrhythmia: $dateFrom to $dateTo")

                val dataList = cachedDataList ?: fetchFromApi(dateFrom, dateTo)
                if (dataList.isNullOrEmpty()) {
                    clearData()
                    return@launch
                }
                cachedDataList = dataList

                when (_selectedTimeRange.value) {
                    "روزانه" -> processData(dataList.first())
                    "هفتگی", "ماهانه" -> processMultiDayData(dataList)
                }

            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to load arrhythmia data")
                clearData()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchFromApi(
        dateFrom: String,
        dateTo: String
    ): List<ArrhythmiaPredictionData>? {
        val userId = userPreferences.getUserId().first()
        if (userId.isNullOrBlank()) {
            Timber.e("❌ No user ID for arrhythmia request")
            return null
        }

        val response = arrhythmiaApiService.predictArrhythmia(userId, dateFrom, dateTo)

        if (!response.isSuccessful) {
            Timber.e("❌ Arrhythmia API error: ${response.code()}")
            return null
        }

        val body = response.body()
        if (body == null || !body.isSuccess) {
            Timber.e("❌ Arrhythmia API response not ok: ${body?.errors?.message}")
            return null
        }

        return body.data
    }

    // ═══════════════════════════════════════════════════════════════
    // Daily: area chart processing (unchanged)
    // ═══════════════════════════════════════════════════════════════

    private fun processData(data: ArrhythmiaPredictionData) {
        val afibProbs = data.probabilitiesAfib
        val predictions = data.predictions
        val timestamps = data.timestamps

        if (afibProbs.isEmpty()) {
            clearData()
            return
        }

        // Build chart points: probability_afib * 100 → percentage (0–100)
        val points = afibProbs.mapIndexed { index, prob ->
            val xRatio = if (afibProbs.size > 1) {
                index.toFloat() / (afibProbs.size - 1).toFloat()
            } else {
                0.5f
            }
            ArrhythmiaPoint(
                xRatio = xRatio,
                value = (prob * 100).toFloat().coerceIn(0f, 100f)
            )
        }
        _chartPoints.value = points

        // Build chart points for "others" probabilities
        val othersProbs = data.probabilitiesOthers
        val othersPoints = othersProbs.mapIndexed { index, prob ->
            val xRatio = if (othersProbs.size > 1) {
                index.toFloat() / (othersProbs.size - 1).toFloat()
            } else {
                0.5f
            }
            ArrhythmiaPoint(
                xRatio = xRatio,
                value = (prob * 100).toFloat().coerceIn(0f, 100f)
            )
        }
        _chartPointsOthers.value = othersPoints

        // Build x-axis time labels from timestamps
        if (timestamps.isNotEmpty()) {
            val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val labelCount = 5.coerceAtMost(timestamps.size)
            val step = (timestamps.size - 1).coerceAtLeast(1).toFloat() / (labelCount - 1).coerceAtLeast(1)
            val labels = (0 until labelCount).map { i ->
                val idx = (i * step).toInt().coerceIn(0, timestamps.lastIndex)
                try {
                    val dt = LocalDateTime.parse(timestamps[idx], timeFormatter)
                    "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
                        .toFarsiDigits()
                } catch (e: Exception) {
                    ""
                }
            }
            _xAxisLabels.value = labels
        }

        // Calculate distribution from predictions (0 = normal, 1 = AFib, 3 = other)
        val total = predictions.size.toFloat()
        if (total > 0) {
            val afibCount = predictions.count { it == 1 }
            val normalCount = predictions.count { it == 0 }
            val otherCount = predictions.count { it != 0 && it != 1 }

            val afibPercent = (afibCount / total) * 100f
            val normalPercent = (normalCount / total) * 100f
            val otherPercent = (otherCount / total) * 100f

            _distribution.value = ArrhythmiaDistribution(
                normalPercent = normalPercent,
                afibPercent = afibPercent,
                otherPercent = otherPercent,
                totalScore = data.finalProbability
            )
        } else {
            _distribution.value = ArrhythmiaDistribution()
        }

        // Use final_prediction for the label
        _predictionLabel.value = when (data.finalPrediction) {
            0 -> "ریتم طبیعی"
            1 -> "فیبریلاسیون دهلیزی"
            3 -> "دیگر آریتمی ها"
            else -> "نامشخص"
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Weekly / Monthly: bar chart processing
    // ═══════════════════════════════════════════════════════════════

    /**
     * Computes per-day (weekly) or per-week (monthly) prediction distributions
     * for the stacked bar chart, plus an aggregate donut distribution.
     */
    private fun processMultiDayData(dataList: List<ArrhythmiaPredictionData>) {
        val today = LocalDate.now()
        val isWeekly = _selectedTimeRange.value == "هفتگی"

        val persianLabels = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
        val todayPersianIndex = when (today.dayOfWeek.value) {
            6 -> 0; 7 -> 1; else -> today.dayOfWeek.value + 1
        }

        val points = mutableListOf<ArrhythmiaBarPoint>()

        if (isWeekly) {
            _xAxisLabels.value = (6 downTo 0).map { i ->
                persianLabels[(todayPersianIndex - i + 7) % 7]
            }

            var listIndex = 0
            for (i in 6 downTo 0) {
                val date = today.minusDays(i.toLong())
                val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val dayData = dataList.find { data ->
                    data.timestamps.any { it.startsWith(dateStr) }
                }
                val label = persianLabels[(todayPersianIndex - i + 7) % 7]
                val ratio = listIndex.toFloat() / 6f

                val (normalPct, afibPct, otherPct) = computeDistribution(
                    dayData?.predictions ?: emptyList()
                )

                points.add(
                    ArrhythmiaBarPoint(
                        label = label,
                        timeRatio = ratio,
                        normalPercent = normalPct,
                        afibPercent = afibPct,
                        otherPercent = otherPct
                    )
                )
                listIndex++
            }
        } else {
            _xAxisLabels.value = (1..4).map { "هفته ${it.toString().toFarsiDigits()}" }

            val startDate = today.minusDays(27)
            for (weekIndex in 0 until 4) {
                val weekPredictions = mutableListOf<Int>()
                for (dayInWeek in 0 until 7) {
                    val dayIndex = weekIndex * 7 + dayInWeek
                    val date = startDate.plusDays(dayIndex.toLong())
                    val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val dayData = dataList.find { data ->
                        data.timestamps.any { it.startsWith(dateStr) }
                    }
                    if (dayData != null) {
                        weekPredictions.addAll(dayData.predictions)
                    }
                }

                val ratio = weekIndex.toFloat() / 3f
                val (normalPct, afibPct, otherPct) = computeDistribution(weekPredictions)

                points.add(
                    ArrhythmiaBarPoint(
                        label = "هفته ${(weekIndex + 1).toString().toFarsiDigits()}",
                        timeRatio = ratio,
                        normalPercent = normalPct,
                        afibPercent = afibPct,
                        otherPercent = otherPct
                    )
                )
            }
        }

        _barChartData.value = points

        // Aggregate distribution for the donut chart
        val allPredictions = dataList.flatMap { it.predictions }
        val (normalPct, afibPct, otherPct) = computeDistribution(allPredictions)
        _distribution.value = ArrhythmiaDistribution(
            normalPercent = normalPct,
            afibPercent = afibPct,
            otherPercent = otherPct,
            totalScore = if (dataList.isNotEmpty()) dataList.map { it.finalProbability }.average().toInt() else 0
        )

        // Most common prediction across all days
        val allFinalPredictions = dataList.map { it.finalPrediction }
        val mostCommon = allFinalPredictions.groupBy { it }
            .maxByOrNull { it.value.size }?.key ?: 0
        _predictionLabel.value = when (mostCommon) {
            0 -> "ریتم طبیعی"
            1 -> "فیبریلاسیون دهلیزی"
            3 -> "دیگر آریتمی ها"
            else -> "نامشخص"
        }
    }

    /** Returns (normalPercent, afibPercent, otherPercent) from a predictions list. */
    private fun computeDistribution(predictions: List<Int>): Triple<Float, Float, Float> {
        if (predictions.isEmpty()) return Triple(0f, 0f, 0f)
        val total = predictions.size.toFloat()
        val normal = (predictions.count { it == 0 } / total) * 100f
        val afib = (predictions.count { it == 1 } / total) * 100f
        val other = (predictions.count { it != 0 && it != 1 } / total) * 100f
        return Triple(normal, afib, other)
    }

    private fun clearData() {
        _chartPoints.value = emptyList()
        _chartPointsOthers.value = emptyList()
        _barChartData.value = emptyList()
        _distribution.value = ArrhythmiaDistribution()
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
