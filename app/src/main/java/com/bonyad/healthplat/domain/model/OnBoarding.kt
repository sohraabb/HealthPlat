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
            description = "حلقهٔ سلامت لحظه\u200Cهای روزمره رو به بینش\u200Cهای مفید تبدیل می\u200Cکنه",
            imageRes = R.drawable.onboarding_1
        ),
        Onboarding(
            title = "هر روز یک قدم کوچیک",
            description = "هدف\u200Cهای شخصی و یادآورهای ملایم برای مسیر پایدار",
            imageRes = R.drawable.onboarding_2
        ),
        Onboarding(
            title = "مراقبت از عزیزان",
            description = "به\u200Cروزرسانی\u200Cهای ساده روزانه برای خانواده و پزشک، با کنترل  اشتراک\u200Cگذاری",
            imageRes = R.drawable.onboarding_3
        )
    )
}