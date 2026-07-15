package com.bonyad.healthplat.ui.dashboard.ai

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.R
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.AIAnalysisApiService
import com.bonyad.healthplat.domain.model.HealthReportResponse
import com.bonyad.healthplat.domain.model.ReportAspect
import com.bonyad.healthplat.ui.components.PersianDate
import com.bonyad.healthplat.ui.utils.rtl
import com.bonyad.healthplat.ui.utils.PersianDateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

sealed class AiAnalysisUiState {
    object Loading : AiAnalysisUiState()
    data class Success(val data: AiAnalysisData) : AiAnalysisUiState()
    data class Error(val message: String, val errorType: String? = null) : AiAnalysisUiState()
}

data class AiAnalysisData(
    val overallScore: Int,
    val summaryText: String,
    val metrics: List<AiMetric>,
    val lastAnalysisDate: String,
    val disclaimerText: String = "این گزارش بر اساس داده\u200Cهای دستگاه پوشیدنی تهیه شده است و جنبه آموزشی و راهنمایی عمومی دارد و به هیچ عنوان جایگزین تشخیص پزشکی نیست.".rtl()
)

data class AiMetric(
    val title: String,
    val status: String,
    val statusColor: Color,
    val description: String,
    val advice: String,
    val iconRes: Int
)

@HiltViewModel
class AiAnalysisViewModel @Inject constructor(
    private val apiService: AIAnalysisApiService,
    private val userPreferences: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiAnalysisUiState>(AiAnalysisUiState.Loading)
    val uiState: StateFlow<AiAnalysisUiState> = _uiState.asStateFlow()

    /**
     * The currently selected Persian date for the report.
     * Defaults to today.
     */
    private val _selectedPersianDate = MutableStateFlow<PersianDate>(getTodayPersianDate())
    val selectedPersianDate: StateFlow<PersianDate> = _selectedPersianDate.asStateFlow()

    init {
        fetchAnalysis()
    }

    /**
     * Fetch analysis for a specific Persian date (called from date picker).
     */
    fun fetchAnalysisForDate(persianDate: PersianDate) {
        _selectedPersianDate.value = persianDate
        val gregorianDate = persianDate.toGregorianIsoDate()
        fetchAnalysisInternal(specificDate = gregorianDate)
    }

    /**
     * Retry / initial fetch — uses the currently selected date.
     */
    fun fetchAnalysis() {
        val gregorianDate = _selectedPersianDate.value.toGregorianIsoDate()
        fetchAnalysisInternal(specificDate = gregorianDate)
    }

    private fun fetchAnalysisInternal(specificDate: String) {
        viewModelScope.launch {
            _uiState.value = AiAnalysisUiState.Loading

            try {
                val userId = userPreferences.getUserId().first()
                if (userId.isNullOrEmpty()) {
                    _uiState.value =
                        AiAnalysisUiState.Error("شناسه کاربر یافت نشد. لطفا دوباره وارد شوید.".rtl())
                    return@launch
                }

                val response = apiService.getHealthReport(userId, specificDate)

                if (!response.isSuccessful) {
                    if (response.code() == 404) {
                        _uiState.value = AiAnalysisUiState.Error(
                            message =
                                "داده\u200Cای کافی برای تحلیل یافت نشد.\n\n" +
                                        "• دستگاه متصل نیست\n" +
                                        "• داده\u200Cها همگام\u200Cسازی نشده\u200Cاند\n" +
                                        "• هنوز یک روز کامل ثبت نشده است"
                        )
                    } else {
                        _uiState.value =
                            AiAnalysisUiState.Error("خطا در ارتباط با سرور")
                    }
                    return@launch
                }

                val body = response.body()
                if (body == null) {
                    _uiState.value = AiAnalysisUiState.Error("پاسخ نامعتبر از سرور")
                    return@launch
                }

                if (!body.isSuccess) {
                    _uiState.value = AiAnalysisUiState.Error(
                        body.errors?.message ?: "داده کافی برای تحلیل موجود نیست"
                    )
                    return@launch
                }

                val report = body.data
                if (report == null) {
                    _uiState.value = AiAnalysisUiState.Error("داده\u200Cای برای نمایش وجود ندارد")
                    return@launch
                }

                _uiState.value = AiAnalysisUiState.Success(mapReportToUiData(report))

            } catch (e: Exception) {
                Timber.e(e, "Error fetching AI analysis")
                _uiState.value = AiAnalysisUiState.Error("عدم دسترسی به سرویس")
            }
        }
    }

    private fun mapReportToUiData(report: HealthReportResponse): AiAnalysisData {
        val currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        val metrics = listOf(
            mapToMetric(
                title = "کیفیت خواب",
                aspect = report.sleep,
                iconRes = R.drawable.sleep_icon
            ),
            mapToMetric(
                title = "فعالیت بدنی",
                aspect = report.activity,
                iconRes = R.drawable.walk
            ),
            mapToMetric(
                title = "قلب",
                aspect = report.heart,
                iconRes = R.drawable.heart_rate
            ),
            mapToMetric(
                title = "مدیریت استرس",
                aspect = report.stress,
                iconRes = R.drawable.care
            )
        )

        // Show the selected Persian date in the last analysis text
        val selectedDate = _selectedPersianDate.value
        val dateLabel = selectedDate.toFormattedPersian()

        return AiAnalysisData(
            overallScore = report.absReadinessScore,
            summaryText = report.overallSummary,
            metrics = metrics,
            lastAnalysisDate = "تحلیل: $dateLabel — ساعت $currentTime"
        )
    }

    private fun mapToMetric(title: String, aspect: ReportAspect, iconRes: Int): AiMetric {
        val description = aspect.notableFindings.joinToString(" ").ifEmpty {
            "اطلاعاتی ثبت نشده است"
        }

        // Join ALL lifestyle suggestions instead of only the first one
        val advice = aspect.lifestyleSuggestions.joinToString("\n").ifEmpty {
            "توصیه\u200Cای موجود نیست"
        }

        val (statusColor, statusText) = parseScoreStatus(aspect.score)

        return AiMetric(
            title = title,
            status = statusText,
            statusColor = statusColor,
            description = description,
            advice = advice,
            iconRes = iconRes
        )
    }

    private fun parseScoreStatus(score: Int): Pair<Color, String> {
        return when (score) {
            3    -> Pair(Color(0xFF00BFA5), "عالی")
            2    -> Pair(Color(0xFF00BFA5), "خوب")
            1    -> Pair(Color(0xFFE99C2E), "متوسط")
            else -> Pair(Color(0xFFE57373), "نیازمند توجه")
        }
    }

    companion object {
        /**
         * Returns today's date as a PersianDate.
         */
        private fun getTodayPersianDate(): PersianDate {
            val today = LocalDate.now()
            val (jy, jm, jd) = PersianDateUtils.georgianToJalali(
                today.year, today.monthValue, today.dayOfMonth
            )
            return PersianDate(jy, jm, jd)
        }
    }
}