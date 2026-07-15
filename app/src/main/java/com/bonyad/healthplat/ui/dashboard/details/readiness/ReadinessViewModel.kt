package com.bonyad.healthplat.ui.dashboard.details.readiness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.R
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.AIAnalysisApiService
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
import java.time.format.DateTimeFormatter
import java.util.GregorianCalendar
import javax.inject.Inject
import com.bonyad.healthplat.ui.components.PersianDate as UiPersianDate

data class AspectItem(
    val key: String,
    val label: String,
    val score: Int,
    val value: String,
    val iconRes: Int
)

data class ReadinessUiState(
    val overallScore: Int = 0,
    val aspects: List<AspectItem> = emptyList(),
    val isLoading: Boolean = true,
    val hasError: Boolean = false
)

@HiltViewModel
class ReadinessViewModel @Inject constructor(
    private val aiApiService: AIAnalysisApiService,
    private val userPreferences: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadinessUiState())
    val uiState: StateFlow<ReadinessUiState> = _uiState.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _selectedTimeRange = MutableStateFlow("روزانه")
    val selectedTimeRange: StateFlow<String> = _selectedTimeRange.asStateFlow()

    private val _dateLabel = MutableStateFlow("")
    val dateLabel: StateFlow<String> = _dateLabel.asStateFlow()

    // Comprehensive mapping covering both English and Persian API keys
    private val aspectConfig = mapOf(
        // English keys
        "hrv" to Pair("تغییرپذیری ضربان", R.drawable.hrv),
        "HRV" to Pair("تغییرپذیری ضربان", R.drawable.hrv),
        "heart" to Pair("ضربان قلب استراحت", R.drawable.heart_simple),
        "heart_rate" to Pair("ضربان قلب استراحت", R.drawable.heart_simple),
        "resting_hr" to Pair("ضربان قلب استراحت", R.drawable.heart_simple),
        "activity" to Pair("فعالیت", R.drawable.shoes),
        "spo2" to Pair("اکسیژن خون", R.drawable.hospital),
        "stress" to Pair("اضطراب", R.drawable.stress),
        "sleep" to Pair("خواب", R.drawable.sleep_simple),
        // Persian keys (actual API keys)
        "خواب" to Pair("خواب", R.drawable.sleep_simple),
        "ضربان قلب" to Pair("ضربان قلب استراحت", R.drawable.heart_simple),
        "ضربان قلب استراحت" to Pair("ضربان قلب استراحت", R.drawable.heart_simple),
        "فعالیت" to Pair("فعالیت", R.drawable.shoes),
        "استرس" to Pair("اضطراب", R.drawable.stress),
        "اضطراب" to Pair("اضطراب", R.drawable.stress),
        "اکسیژن خون" to Pair("اکسیژن خون", R.drawable.hospital),
        "قلبی" to Pair("ضربان قلب استراحت", R.drawable.heart_simple),
        "تغییرپذیری ضربان قلب" to Pair("تغییرپذیری ضربان", R.drawable.hrv),
        "تغییرپذیری ضربان" to Pair("تغییرپذیری ضربان", R.drawable.hrv),
    )

    init {
        updateDateLabel(_selectedDate.value)
        fetchReadinessData()
    }

    fun selectDay(date: LocalDate) {
        _selectedDate.value = date
        updateDateLabel(date)
        fetchReadinessData()
    }

    fun selectDate(date: UiPersianDate) {
        _selectedDate.value = LocalDate.parse(date.toGregorianIsoDate())
        _selectedTimeRange.value = "روزانه"
        updateDateLabel(_selectedDate.value)
        fetchReadinessData()
    }

    fun setTimeRange(range: String) {
        _selectedTimeRange.value = range
        updateDateLabel(_selectedDate.value)
        fetchReadinessData()
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

    private fun fetchReadinessData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, hasError = false)

            try {
                val userId = userPreferences.getUserId().first()
                if (userId == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, hasError = true)
                    return@launch
                }

                val dateStr = _selectedDate.value.format(DateTimeFormatter.ISO_DATE)
                Timber.i("Fetching readiness for: $dateStr")

                val response = aiApiService.getReadinessScore(userId, dateStr)

                if (!response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(isLoading = false, hasError = true)
                    return@launch
                }

                val body = response.body()
                if (body == null || !body.isSuccess || body.data == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, hasError = true)
                    return@launch
                }

                val data = body.data

                // Use ALL keys from the API response directly
                val aspects = data.perAspectScores.map { (key, score) ->
                    val (label, iconRes) = aspectConfig[key]
                        ?: Pair(key, R.drawable.heart_simple)
                    AspectItem(
                        key = key,
                        label = label,
                        score = score.coerceIn(0, 100),
                        value = data.perAspectValues[key] ?: "${score}%",
                        iconRes = iconRes
                    )
                }

                Timber.i("Readiness loaded: score=${data.absReadinessScore}, keys=${data.perAspectScores.keys}")

                _uiState.value = ReadinessUiState(
                    overallScore = data.absReadinessScore.coerceIn(0, 100),
                    aspects = aspects,
                    isLoading = false,
                    hasError = false
                )

            } catch (e: Exception) {
                Timber.e(e, "Error fetching readiness detail")
                _uiState.value = _uiState.value.copy(isLoading = false, hasError = true)
            }
        }
    }

    fun retry() {
        fetchReadinessData()
    }
}
