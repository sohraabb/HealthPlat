package com.bonyad.healthplat.ui.dashboard.ai

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.R
import com.bonyad.healthplat.data.local.UserPreferencesDataStore
import com.bonyad.healthplat.data.network.AIAnalysisApiService
import com.bonyad.healthplat.domain.model.ReportAspect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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

data class AiAnalysisState(
    val overallScore: Int = 66,
    val summaryText: String = "",
    val metrics: List<AiMetric> = emptyList(),
    val lastAnalysisDate: String = ""
)

data class AiMetric(
    val title: String,
    val value: String,
    val status: String, // e.g., "خوب", "متوسط", "نیازمند بهبود"
    val description: String,
    val advice: String,
    val iconRes: Int,
    val statusColor: Color
)

@HiltViewModel
class AiAnalysisViewModel @Inject constructor(
    private val apiService: AIAnalysisApiService,
    private val userPreferences: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAnalysisState())
    val uiState: StateFlow<AiAnalysisState> = _uiState.asStateFlow()

    init {
        fetchAnalysis()
    }

    fun fetchAnalysis() {
        viewModelScope.launch {
            try {
                val userId = userPreferences.getUserId().first()
                if (userId.isNullOrEmpty()) return@launch

                // Format: 2025-12-06 00:00:00.0000000
                // Simple version usually works, but adhering to requested format:
                val yesterday = LocalDate.now()
                    .minusDays(1)
                    .format(DateTimeFormatter.ISO_DATE)

                _uiState.value = _uiState.value.copy(summaryText = "در حال دریافت تحلیل...")

                val response = apiService.getHealthReport(userId, yesterday)

                if (response.isSuccessful && response.body() != null) {
                    val report = response.body()!!

                    val metrics = listOf(
                        mapToMetric("خواب", report.sleep, R.drawable.chemistry_flask),
                        mapToMetric("فعالیت بدنی", report.activity, R.drawable.walk),
                        mapToMetric("قلب", report.heart, R.drawable.heart_rate),
                        mapToMetric("مدیریت استرس", report.stress, R.drawable.care)
                    )

                    _uiState.value = AiAnalysisState(
                        overallScore = report.absReadinessScore,
                        summaryText = report.overallSummary,
                        metrics = metrics,
                        lastAnalysisDate = "آخرین تحلیل: امروز ساعت ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))}"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(summaryText = "خطا در دریافت اطلاعات. لطفا دوباره تلاش کنید.")
                }

            } catch (e: Exception) {
                Timber.e(e, "Error fetching AI report")
                _uiState.value = _uiState.value.copy(summaryText = "عدم دسترسی به اینترنت یا سرویس.")
            }
        }
    }

    private fun mapToMetric(title: String, aspect: ReportAspect, iconRes: Int): AiMetric {
        // Create a summary from findings (taking the first one)
        val description = aspect.notableFindings.firstOrNull() ?: "اطلاعاتی ثبت نشده است"

        // Create advice string (taking the first one)
        val advice = aspect.lifestyleSuggestions.firstOrNull() ?: "توصیه‌ای موجود نیست"

        // Map textual score to color
        val (color, statusText) = when {
            aspect.score.contains("عالی") -> Pair(Color(0xFF00BFA5), "عالی") // Green
            aspect.score.contains("خوب") -> Pair(Color(0xFF00BFA5), "خوب") // Green
            aspect.score.contains("توجه") -> Pair(Color(0xFFE99C2E), "نیازمند توجه") // Orange
            else -> Pair(Color.Gray, aspect.score)
        }

        return AiMetric(
            title = title,
            value = aspect.score, // Or construct a value string if needed
            status = statusText,
            description = description,
            advice = advice,
            iconRes = iconRes,
            statusColor = color
        )
    }
}