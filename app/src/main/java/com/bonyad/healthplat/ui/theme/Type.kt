package com.bonyad.healthplat.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bonyad.healthplat.R


// Dana with English numbers (for mixed content)
val DanaFontFamily = FontFamily(
    Font(R.font.dana_light, FontWeight.Light),
    Font(R.font.dana_regular, FontWeight.Normal),
    Font(R.font.dana_medium, FontWeight.Medium),
    Font(R.font.dana_bold, FontWeight.Bold),
    Font(R.font.dana_black, FontWeight.Black)
)

// Dana with Farsi numbers (for Persian-only content)
val DanaFaNumFontFamily = FontFamily(
    Font(R.font.dana_fa_num_light, FontWeight.Light),
    Font(R.font.dana_fa_num_regular, FontWeight.Normal),
    Font(R.font.dana_fa_num_medium, FontWeight.Medium),
    Font(R.font.dana_fa_num_bold, FontWeight.Bold),
    Font(R.font.dana_fa_num_black, FontWeight.Black)
)

// Material 3 Typography with Dana font
// Using DanaFaNumFontFamily for better Persian experience
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = DanaFaNumFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = DanaFaNumFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = DanaFaNumFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = DanaFaNumFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DanaFaNumFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = DanaFaNumFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = DanaFaNumFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = DanaFaNumFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = DanaFaNumFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = DanaFaNumFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DanaFaNumFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = DanaFaNumFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = DanaFaNumFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = DanaFaNumFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = DanaFaNumFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// Extension function to easily switch between font families
fun Typography.withEnglishNumbers(): Typography = this.copy(
    displayLarge = displayLarge.copy(fontFamily = DanaFontFamily),
    displayMedium = displayMedium.copy(fontFamily = DanaFontFamily),
    displaySmall = displaySmall.copy(fontFamily = DanaFontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = DanaFontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = DanaFontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = DanaFontFamily),
    titleLarge = titleLarge.copy(fontFamily = DanaFontFamily),
    titleMedium = titleMedium.copy(fontFamily = DanaFontFamily),
    titleSmall = titleSmall.copy(fontFamily = DanaFontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = DanaFontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = DanaFontFamily),
    bodySmall = bodySmall.copy(fontFamily = DanaFontFamily),
    labelLarge = labelLarge.copy(fontFamily = DanaFontFamily),
    labelMedium = labelMedium.copy(fontFamily = DanaFontFamily),
    labelSmall = labelSmall.copy(fontFamily = DanaFontFamily)
)