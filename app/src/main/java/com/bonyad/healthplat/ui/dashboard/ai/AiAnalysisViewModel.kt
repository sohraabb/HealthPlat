package com.bonyad.healthplat.ui.dashboard.ai

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bonyad.healthplat.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
class AiAnalysisViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(AiAnalysisState())
    val uiState: StateFlow<AiAnalysisState> = _uiState.asStateFlow()

    init {
        loadMockData()
    }

    private fun loadMockData() {
        _uiState.value = AiAnalysisState(
            overallScore = 66,
            summaryText = "سبک زندگی شما نشان‌دهنده شروع بهبود در ثبات خواب و مدیریت استرس است. سطح فعالیت شما خوب است اما می‌تواند منظم‌تر باشد.",
            lastAnalysisDate = "آخرین تحلیل: امروز ساعت ۱۶:۳۰",
            metrics = listOf(
                AiMetric(
                    title = "کیفیت خواب",
                    value = "میانگین ۶.۵ ساعت",
                    status = "نیازمند بهبود",
                    description = "خواب معمولاً کاملاً پایدار نیست. پیشنهاد می‌شود با زمان‌بندی دقیق‌تر، برنامه خواب شما به حد مطلوب برسد.",
                    advice = "سعی کنید هر شب ساعت ۱۱:۰۰ بخوابید و ۷:۰۰ صبح بیدار شوید.",
                    iconRes = R.drawable.hospital, // Replace with your icon
                    statusColor = Color(0xFFE99C2E)
                ),
                AiMetric(
                    title = "فعالیت بدنی",
                    value = "۷۳۰۰ قدم در روز - ۳-۴ بار در هفته",
                    status = "خوب",
                    description = "سطح فعالیت بدنی شما در حد متوسط است. برای بهبود می‌توانید یک جلسه ورزشی دیگر در هفته اضافه کنید.",
                    advice = "یک جلسه ورزشی دیگر در هفته اضافه کنید، مثلاً پیاده‌روی ۲۰ دقیقه‌ای.",
                    iconRes = R.drawable.hospital,
                    statusColor = Color(0xFF00BFA5)
                )
                // Add Heart Health and Stress Management similarly...
            )
        )
    }
}