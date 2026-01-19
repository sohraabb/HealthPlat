package com.bonyad.healthplat.ui.dashboard.ai

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.R
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.AIAnalysisApiService
import com.bonyad.healthplat.domain.model.ApiErrorType
import com.bonyad.healthplat.domain.model.HealthReportResponse
import com.bonyad.healthplat.domain.model.ReportAspect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.MissingFieldException
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
    val disclaimerText: String = "این گزارش بر اساس داده‌های دستگاه پوشیدنی تهیه شده است و جنبه آموزشی و راهنمایی عمومی دارد و به هیچ عنوان جایگزین تشخیص پزشکی نیست."
)

data class AiMetric(
    val title: String,
    val value: String,
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

    init {
        fetchAnalysis()
    }

    fun fetchAnalysis() {
        viewModelScope.launch {
            _uiState.value = AiAnalysisUiState.Loading

            try {
                val userId = userPreferences.getUserId().first()
                if (userId.isNullOrEmpty()) {
                    _uiState.value =
                        AiAnalysisUiState.Error("شناسه کاربر یافت نشد. لطفا دوباره وارد شوید.")
                    return@launch
                }

                // Try yesterday first, then fallback to day before
                val datesToTry = listOf(
                    LocalDate.now().minusDays(1),
                    LocalDate.now().minusDays(2),
                    LocalDate.now().minusDays(3)

                )

                for ((index, date) in datesToTry.withIndex()) {
                    val recordDate = date.format(DateTimeFormatter.ISO_DATE)
                    val response = apiService.getHealthReport(userId, recordDate)

                    // 🔹 404 → try fallback date
                    if (!response.isSuccessful) {
                        if (response.code() == 404 || response.code() == 500 && index < datesToTry.lastIndex) {
                            continue
                        }

                        if (response.code() == 404) {
                            _uiState.value = AiAnalysisUiState.Error(
                                message =
                                    "داده‌ای کافی برای تحلیل یافت نشد.\n\n" +
                                            "• دستگاه متصل نیست\n" +
                                            "• داده‌ها همگام‌سازی نشده‌اند\n" +
                                            "• هنوز یک روز کامل ثبت نشده است"
                            )
                            return@launch
                        }

                        _uiState.value =
                            AiAnalysisUiState.Error("خطا در ارتباط با سرور")
                        return@launch
                    }

                    val body = response.body()
                    if (body == null) {
                        _uiState.value =
                            AiAnalysisUiState.Error("پاسخ نامعتبر از سرور")
                        return@launch
                    }

                    if (!body.ok) {
                        if (body.data != null) {
                            _uiState.value =
                                AiAnalysisUiState.Error(body.error?.message ?: "خطای ناشناخته")
                            return@launch
                        } else {
                            _uiState.value =
                                AiAnalysisUiState.Error("داده کافی برای تحلیل موجود نیست")
                            return@launch
                        }


                    }

                    val report = body.data
                    if (report == null) {
                        _uiState.value =
                            AiAnalysisUiState.Error("داده‌ای برای نمایش وجود ندارد")
                        return@launch
                    }

                    // ✅ SUCCESS
                    _uiState.value =
                        AiAnalysisUiState.Success(mapReportToUiData(report))
                    return@launch
                }

            } catch (e: Exception) {
                Timber.e(e, "Error fetching AI analysis")
                _uiState.value =
                    AiAnalysisUiState.Error("عدم دسترسی به سرویس")
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

        return AiAnalysisData(
            overallScore = report.absReadinessScore,
            summaryText = report.overallSummary,
            metrics = metrics,
            lastAnalysisDate = "آخرین تحلیل: امروز ساعت $currentTime"
        )
    }

    private fun mapToMetric(title: String, aspect: ReportAspect, iconRes: Int): AiMetric {
        // Combine notable findings for description
        val description = aspect.notableFindings.joinToString(" ").ifEmpty {
            "اطلاعاتی ثبت نشده است"
        }

        // Combine lifestyle suggestions for advice
        val advice = aspect.lifestyleSuggestions.firstOrNull() ?: "توصیه‌ای موجود نیست"

        // Parse score string to determine color and status
        val (statusColor, statusText) = parseScoreStatus(aspect.score)

        return AiMetric(
            title = title,
            value = aspect.score,
            status = statusText,
            statusColor = statusColor,
            description = description,
            advice = advice,
            iconRes = iconRes
        )
    }

    private fun parseScoreStatus(scoreText: String): Pair<Color, String> {
        return when {
            scoreText.contains("عالی") || scoreText.contains("خوب") ->
                Pair(Color(0xFF00BFA5), "خوب") // Teal/Green
            scoreText.contains("متوسط") ->
                Pair(Color(0xFFE99C2E), "متوسط") // Orange/Amber
            scoreText.contains("توجه") || scoreText.contains("ضعیف") || scoreText.contains("بالا") ->
                Pair(Color(0xFFE99C2E), "متوسط-بالا") // Orange for attention needed
            else ->
                Pair(Color(0xFF4A90A4), "متوسط") // Default teal-ish
        }
    }
}