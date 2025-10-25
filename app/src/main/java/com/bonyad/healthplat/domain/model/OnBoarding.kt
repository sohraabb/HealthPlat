package com.bonyad.healthplat.domain.model

import com.bonyad.healthplat.R

data class Onboarding(
    val title: String,
    val description: String,
    val imageRes: Int
)

object OnBoardingData {
    val data = listOf(
        Onboarding(
            title = "سلامت، یک نگاه",
            description = "ساعت سلامت لحظه‌های روزمره رو به پیش‌بینش‌های مفید تبدیل می‌کنه",
            imageRes = R.drawable.onboarding_1
        ),
        Onboarding(
            title = "هر روز یک قدم کوچیک",
            description = "هدف‌های شخصی و راه‌کارهای میلیاردی برای مسیر پایدار",
            imageRes = R.drawable.onboarding_2
        ),
        Onboarding(
            title = "مراقبت از عزیزان",
            description = "به‌روزرسانی‌های ساده روزانه برای خانواده و پزشک، با کنترل اشتراک‌گذاری",
            imageRes = R.drawable.onboarding_3
        )
    )
}