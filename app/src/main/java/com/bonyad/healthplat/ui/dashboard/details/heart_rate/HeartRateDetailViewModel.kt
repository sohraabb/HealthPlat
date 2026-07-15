package com.bonyad.healthplat.ui.dashboard.details.heart_rate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.blesdk.manager.HealthDeviceManager
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
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
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import saman.zamani.persiandate.PersianDate
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.GregorianCalendar
import javax.inject.Inject
import kotlin.math.abs
import com.bonyad.healthplat.ui.components.PersianDate as UiPersianDate


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
    private val userPreferences: UserPreferencesDataStore,
    private val careRepository: CareRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val patientUserId: String? = savedStateHandle.get<String>("patientUserId")
    val isCaregiverMode: Boolean = patientUserId != null

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

    // Chart Data (List of Ranges)
    private val _chartData = MutableStateFlow<List<HeartRateRangePoint>>(emptyList())
    val chartData: StateFlow<List<HeartRateRangePoint>> = _chartData.asStateFlow()

    // HRV Data
    private val _currentHrv = MutableStateFlow(0)
    val currentHrv: StateFlow<Int> = _currentHrv.asStateFlow()

    private val _avgHrv = MutableStateFlow(0)
    val avgHrv: StateFlow<Int> = _avgHrv.asStateFlow()

    private val _minHrv = MutableStateFlow(0)
    val minHrv: StateFlow<Int> = _minHrv.asStateFlow()

    private val _maxHrv = MutableStateFlow(0)
    val maxHrv: StateFlow<Int> = _maxHrv.asStateFlow()

    // Selected Time Range (Daily, Weekly, Monthly)
    private val _selectedTimeRange = MutableStateFlow("روزانه") // Daily
    val selectedTimeRange: StateFlow<String> = _selectedTimeRange.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _currentPersianDate = MutableStateFlow("")
    val currentPersianDate: StateFlow<String> = _currentPersianDate.asStateFlow()

    private val _hrvChartData = MutableStateFlow<List<Int>>(emptyList())
    val hrvChartData: StateFlow<List<Int>> = _hrvChartData.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        updateDateLabel(LocalDate.now())
        loadDataFromApi()
        if (!isCaregiverMode) {
            observeRealTimeData()
        }
    }

    // ============ Sync: Ring → Server → API fetch → Display ============

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            if (!isCaregiverMode) {
                try {
                    // Step 1 & 2: Read from ring + upload to server
                    val syncDay = ChronoUnit.DAYS.between(_selectedDate.value, LocalDate.now()).toInt().coerceIn(0, 6)
                    healthRepository.syncDashboardData(syncDay)
                    Timber.i("✅ Ring data synced to server for day $syncDay")
                } catch (e: Exception) {
                    Timber.e(e, "⚠️ Ring→Server sync failed, loading from API anyway")
                }
            }
            // Step 3: Fetch from server & display
            loadDataFromApi()
        }
    }

    // ============ PRIMARY: Load from API ============

    private fun loadDataFromApi() {
        viewModelScope.launch {
            _isLoading.value = true

            if (isCaregiverMode) {
                loadCaregiverHeartRate()
            } else {
                loadOwnHeartRate()
            }
        }
    }

    private suspend fun loadCaregiverHeartRate() {
        try {
            val (dateFrom, dateTo) = getDateRange()
            Timber.i("📡 Fetching caregiver HR: $dateFrom to $dateTo for patient $patientUserId")

            when (val result = careRepository.getPatientHeartRate(patientUserId!!, dateFrom, dateTo)) {
                is AuthResult.Success -> {
                    if (result.data.isNotEmpty()) {
                        processHeartRateData(result.data)
                        Timber.i("✅ Caregiver heart rate loaded: ${result.data.size} records")
                    } else {
                        Timber.w("⚠️ No caregiver heart rate data")
                        clearHeartRateData()
                    }
                }
                is AuthResult.Error -> {
                    Timber.e("❌ Caregiver heart rate error: ${result.message}")
                    clearHeartRateData()
                }
            }
            // No HRV endpoint for caregiver
            clearHrvData()
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to load caregiver heart rate")
            clearHeartRateData()
            clearHrvData()
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun loadOwnHeartRate() {
        try {
            val (dateFrom, dateTo) = getDateRange()

            Timber.i("📡 Fetching HR & HRV: $dateFrom to $dateTo")

            // Fetch both in parallel
            val heartRateDeferred = viewModelScope.async {
                healthRepository.getMetricData(MetricType.HEART_RATE, dateFrom, dateTo)
            }
            val hrvDeferred = viewModelScope.async {
                healthRepository.getMetricData(MetricType.HRV, dateFrom, dateTo)
            }

            val deviceOffset = ChronoUnit.DAYS.between(_selectedDate.value, LocalDate.now()).toInt().coerceIn(0, 6)

            // Process Heart Rate
            heartRateDeferred.await().fold(
                onSuccess = { metricsData ->
                    if (metricsData.isNotEmpty()) {
                        processHeartRateData(metricsData)
                        Timber.i("✅ Heart rate loaded: ${metricsData.size} records")
                    } else {
                        Timber.w("⚠️ No heart rate data from API")
                        clearHeartRateData()
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "❌ Heart rate API error")
                    loadHeartRateFromDevice(deviceOffset)
                }
            )

            // Process HRV
            hrvDeferred.await().fold(
                onSuccess = { metricsData ->
                    if (metricsData.isNotEmpty()) {
                        processHrvData(metricsData)
                        Timber.i("✅ HRV loaded: ${metricsData.size} records")
                    } else {
                        Timber.w("⚠️ No HRV data from API")
                        clearHrvData()
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "❌ HRV API error")
                    loadHrvFromDevice(deviceOffset)
                }
            )

        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to load data from API")
            val deviceOffset = ChronoUnit.DAYS.between(_selectedDate.value, LocalDate.now()).toInt().coerceIn(0, 6)
            loadHeartRateFromDevice(deviceOffset)
            loadHrvFromDevice(deviceOffset)
        } finally {
            _isLoading.value = false
        }
    }

    // ============ Date Range ============

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
                _currentPersianDate.value = "\u200F۷ روز اخیر"
                Pair(startDate.format(formatter), today.format(formatter))
            }
            "ماهانه" -> {
                val startDate = today.minusDays(27)
                _currentPersianDate.value = "\u200F۴ هفته اخیر"
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

        _currentPersianDate.value = when {
            date == today -> "امروز $dayOfMonth $monthName".rtl()
            date == today.minusDays(1) -> "دیروز $dayOfMonth $monthName".rtl()
            else -> "$dayOfMonth $monthName".rtl()
        }
    }

    // ============ Heart Rate Processing ============

    private fun processHeartRateData(metricsData: List<MetricData>) {
        when (_selectedTimeRange.value) {
            "روزانه" -> processDailyHeartRate(metricsData)
            "هفتگی" -> processWeeklyHeartRate(metricsData)
            "ماهانه" -> processMonthlyHeartRate(metricsData)
        }
    }

    private fun processDailyHeartRate(metricsData: List<MetricData>) {
        val allValues = metricsData.flatMap { it.values }

        if (allValues.isEmpty()) {
            clearHeartRateData()
            return
        }

        val validData = allValues.filter { it > 1 }

        if (validData.isEmpty()) {
            clearHeartRateData()
            return
        }

        _avgHeartRate.value = validData.average().toInt()
        _minHeartRate.value = validData.minOrNull() ?: 0
        _maxHeartRate.value = validData.maxOrNull() ?: 0
        _currentHeartRate.value = validData.lastOrNull() ?: 0
        _heartRateData.value = validData

        _chartData.value = buildHourlyChartData(allValues)
    }

    private fun processWeeklyHeartRate(metricsData: List<MetricData>) {
        val allValidValues = metricsData.flatMap { it.values }.filter { it > 1 }

        if (allValidValues.isEmpty()) {
            clearHeartRateData()
            return
        }

        _avgHeartRate.value = allValidValues.average().toInt()
        _minHeartRate.value = allValidValues.minOrNull() ?: 0
        _maxHeartRate.value = allValidValues.maxOrNull() ?: 0
        _currentHeartRate.value = allValidValues.lastOrNull() ?: 0
        _heartRateData.value = allValidValues

        // Build 7-day list: oldest on the left, today on the right (RTL-friendly)
        val today = LocalDate.now()
        val persianLabels = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
        val todayPersianIndex = when (today.dayOfWeek.value) {
            6 -> 0; 7 -> 1; else -> today.dayOfWeek.value + 1
        }
        val chartPoints = (6 downTo 0).map { i ->
            val date = today.minusDays(i.toLong())
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val metric = metricsData.find { it.recordDate.startsWith(dateStr) }
            val dayValues = metric?.values?.filter { it > 1 } ?: emptyList()

            HeartRateRangePoint(
                timeLabel = persianLabels[(todayPersianIndex - i + 7) % 7],
                min = dayValues.minOrNull() ?: 0,
                max = dayValues.maxOrNull() ?: 0,
                isAlert = (dayValues.maxOrNull() ?: 0) > 120 || (dayValues.minOrNull() ?: 0) < 50
            )
        }

        _chartData.value = chartPoints
    }

    private fun processMonthlyHeartRate(metricsData: List<MetricData>) {
        val allValidValues = metricsData.flatMap { it.values }.filter { it > 1 }

        if (allValidValues.isEmpty()) {
            clearHeartRateData()
            return
        }

        _avgHeartRate.value = allValidValues.average().toInt()
        _minHeartRate.value = allValidValues.minOrNull() ?: 0
        _maxHeartRate.value = allValidValues.maxOrNull() ?: 0
        _currentHeartRate.value = allValidValues.lastOrNull() ?: 0
        _heartRateData.value = allValidValues

        // Build list for last 28 days, chunked into 4 weeks (oldest first)
        val today = LocalDate.now()
        val startDate = today.minusDays(27)
        val allDays = (0 until 28).map { dayOffset ->
            val date = startDate.plusDays(dayOffset.toLong())
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val metric = metricsData.find { it.recordDate.startsWith(dateStr) }
            metric?.values?.filter { it > 1 } ?: emptyList()
        }

        val weeklyGroups = allDays.chunked(7)
        val chartPoints = weeklyGroups.mapIndexed { weekIndex, weekDays ->
            val weekValues = weekDays.flatten()

            HeartRateRangePoint(
                timeLabel = "هفته ${(weekIndex + 1).toString().toFarsiDigits()}",
                min = weekValues.minOrNull() ?: 0,
                max = weekValues.maxOrNull() ?: 0,
                isAlert = (weekValues.maxOrNull() ?: 0) > 120 || (weekValues.minOrNull() ?: 0) < 50
            )
        }

        _chartData.value = chartPoints
    }

    private fun buildHourlyChartData(hrData: List<Int>): List<HeartRateRangePoint> {
        val result = mutableListOf<HeartRateRangePoint>()
        val minutesPerSlot = 30
        val totalSlots = 48

        for (slot in 0 until totalSlots) {
            val startIdx = slot * minutesPerSlot
            val endIdx = minOf(startIdx + minutesPerSlot, hrData.size)

            if (startIdx >= hrData.size) break

            val slotData = hrData.subList(startIdx, endIdx).filter { it > 1 }
            if (slotData.isEmpty()) continue

            val min = slotData.minOrNull() ?: 0
            val max = slotData.maxOrNull() ?: 0
            val isAlert = max > 120 || min < 50

            val hour = slot / 2
            val minute = (slot % 2) * 30

            result.add(
                HeartRateRangePoint(
                    timeLabel = String.format("%02d:%02d", hour, minute),
                    min = min,
                    max = max,
                    isAlert = isAlert
                )
            )
        }

        return result
    }

    // ============ HRV Processing ============

    private fun processHrvData(metricsData: List<MetricData>) {
        val allValues = metricsData.flatMap { it.values }

        if (allValues.isEmpty()) {
            clearHrvData()
            return
        }

        val validData = allValues.filter { it > 0 }

        if (validData.isEmpty()) {
            clearHrvData()
            return
        }

        // Stats — always from filtered data
        _avgHrv.value = validData.average().toInt()
        _minHrv.value = validData.minOrNull() ?: 0
        _maxHrv.value = validData.maxOrNull() ?: 0
        _currentHrv.value = validData.lastOrNull() ?: 0

        // Chart data — depends on time range
        _hrvChartData.value = when (_selectedTimeRange.value) {
            "روزانه" -> {
                // Store all 48 slots (keep zeros for time positioning)
                val dayValues = metricsData.firstOrNull()?.values ?: allValues
                if (dayValues.size > 48 && dayValues.size % 48 == 0) {
                    dayValues.take(48)
                } else {
                    dayValues
                }
            }
            "هفتگی" -> {
                // Last 7 days, oldest first — one average per day
                val today = LocalDate.now()
                (6 downTo 0).map { i ->
                    val date = today.minusDays(i.toLong())
                    val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val metric = metricsData.find { it.recordDate.startsWith(dateStr) }
                    val dayValid = metric?.values?.filter { it > 0 } ?: emptyList()
                    if (dayValid.isNotEmpty()) dayValid.average().toInt() else 0
                }
            }
            "ماهانه" -> {
                // Last 28 days chunked into 4 weeks
                val today = LocalDate.now()
                val startDate = today.minusDays(27)
                val allDayAvgs = (0 until 28).map { dayOffset ->
                    val date = startDate.plusDays(dayOffset.toLong())
                    val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val metric = metricsData.find { it.recordDate.startsWith(dateStr) }
                    val dayValid = metric?.values?.filter { it > 0 } ?: emptyList()
                    if (dayValid.isNotEmpty()) dayValid.average().toInt() else 0
                }
                allDayAvgs.chunked(7).map { weekAvgs ->
                    val validWeek = weekAvgs.filter { it > 0 }
                    if (validWeek.isNotEmpty()) validWeek.average().toInt() else 0
                }
            }
            else -> validData
        }

        Timber.d("📊 HRV Stats - Avg: ${_avgHrv.value}, Min: ${_minHrv.value}, Max: ${_maxHrv.value}, Current: ${_currentHrv.value}, ChartPoints: ${_hrvChartData.value.size}")
    }

    // ============ Device Fallback ============

    private fun loadHeartRateFromDevice(offset: Int) {
        viewModelScope.launch {
            try {
                val result = deviceManager.getRecordData(offset)

                if (result is RecordDataResult.Success) {
                    result.heartRate?.heartRateSource?.let { hrData ->
                        val validData = hrData.filter { it > 1 }

                        if (validData.isNotEmpty()) {
                            _heartRateData.value = validData
                            _avgHeartRate.value = validData.average().toInt()
                            _minHeartRate.value = validData.minOrNull() ?: 0
                            _maxHeartRate.value = validData.maxOrNull() ?: 0
                            _currentHeartRate.value = validData.lastOrNull() ?: 0
                            _chartData.value = buildHourlyChartData(hrData)

                            Timber.i("✅ HR loaded from device for day $offset")
                        } else {
                            clearHeartRateData()
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading HR from device")
                clearHeartRateData()
            }
        }
    }

    private fun loadHrvFromDevice(offset: Int) {
        viewModelScope.launch {
            try {
                val result = deviceManager.getRecordData(offset)

                if (result is RecordDataResult.Success) {
                    result.hrv?.hrvSource?.let { hrvData ->
                        val validData = hrvData.filter { it > 0 }

                        if (validData.isNotEmpty()) {
                            _avgHrv.value = validData.average().toInt()
                            _minHrv.value = validData.minOrNull() ?: 0
                            _maxHrv.value = validData.maxOrNull() ?: 0
                            _currentHrv.value = validData.lastOrNull() ?: 0

                            Timber.i("✅ HRV loaded from device for day $offset")
                        } else {
                            clearHrvData()
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading HRV from device")
                clearHrvData()
            }
        }
    }

    // ============ UI Actions ============

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
        loadDataFromApi()
    }

    fun selectDay(date: LocalDate) {
        _selectedDate.value = date
        updateDateLabel(date)
        if (_selectedTimeRange.value == "روزانه") {
            loadDataFromApi()
        }
    }

    fun selectDate(date: UiPersianDate) {
        _selectedDate.value = LocalDate.parse(date.toGregorianIsoDate())
        _selectedTimeRange.value = "روزانه"
        updateDateLabel(_selectedDate.value)
        loadDataFromApi()
    }

    // ============ Clear Data ============

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

    // Real-time data observation
    private fun observeRealTimeData() {
        viewModelScope.launch {
            deviceManager.realTimeData.collect { data ->
                data.heart?.let { hr ->
                    if (hr > 0) {
                        _currentHeartRate.value = hr
                    }
                }
            }
        }
    }
}