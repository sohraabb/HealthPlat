package com.bonyad.healthplat.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bonyad.healthplat.ui.utils.PersianDateUtils
import com.bonyad.healthplat.ui.utils.toFarsiDigits

private data class CalendarColors(
    val background: Color,
    val headerBackground: Color,
    val accent: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val textDisabled: Color
)

private val DarkCalendarColors = CalendarColors(
    background = Color(0xFF131B2E),
    headerBackground = Color(0xFF0B121E),
    accent = Color(0xFF4ECDC4),
    textPrimary = Color(0xFFFFFFFF),
    textMuted = Color(0xFF6B7280),
    textDisabled = Color(0xFF4B5563)
)

private val LightCalendarColors = CalendarColors(
    background = Color(0xFFFFFFFF),
    headerBackground = Color(0xFFF5F5F5),
    accent = Color(0xFF4FA8A6),
    textPrimary = Color(0xFF2C2C2C),
    textMuted = Color(0xFF9E9E9E),
    textDisabled = Color(0xFFD1D5DB)
)

/**
 * Represents a Jalali (Persian) date with year, month, and day.
 */
data class PersianDate(
    val year: Int,
    val month: Int,
    val day: Int
) {
    /**
     * Converts to Gregorian ISO date string (yyyy-MM-dd) for API calls.
     */
    fun toGregorianIsoDate(): String {
        val (gy, gm, gd) = PersianDateUtils.jalaliToGregorian(year, month, day)
        return String.format("%04d-%02d-%02d", gy, gm, gd)
    }

    fun toFormattedPersian(): String {
        val monthName = PersianDateUtils.getMonthName(month)
        return "$day $monthName $year".toFarsiDigits()
    }
}

/**
 * Persian date picker dialog.
 *
 * @param selectedDate The currently selected Persian date
 * @param onDateSelected Called when a date is tapped
 * @param onDismiss Called when the dialog should close
 * @param maxDate Optional maximum selectable date (e.g., today)
 * @param minDate Optional minimum selectable date
 * @param useDarkTheme Whether to use a dark background (true) or light background (false, default)
 */
@Composable
fun PersianDatePickerDialog(
    selectedDate: PersianDate,
    onDateSelected: (PersianDate) -> Unit,
    onDismiss: () -> Unit,
    maxDate: PersianDate? = null,
    minDate: PersianDate? = null,
    useDarkTheme: Boolean = false
) {
    val colors = if (useDarkTheme) DarkCalendarColors else LightCalendarColors

    val today = remember {
        val (jy, jm, jd) = PersianDateUtils.getCurrentJalaliDate()
        PersianDate(jy, jm, jd)
    }

    var displayYear by remember { mutableIntStateOf(selectedDate.year) }
    var displayMonth by remember { mutableIntStateOf(selectedDate.month) }

    // Track navigation direction for animation
    var navigationDirection by remember { mutableIntStateOf(0) } // -1 = prev, 1 = next

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            shape = RoundedCornerShape(20.dp),
            color = colors.background
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Month/Year header with navigation arrows
                    CalendarHeader(
                        year = displayYear,
                        month = displayMonth,
                        colors = colors,
                        onPreviousMonth = {
                            navigationDirection = -1
                            if (displayMonth == 1) {
                                displayMonth = 12
                                displayYear--
                            } else {
                                displayMonth--
                            }
                        },
                        onNextMonth = {
                            navigationDirection = 1
                            if (displayMonth == 12) {
                                displayMonth = 1
                                displayYear++
                            } else {
                                displayMonth++
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Day-of-week headers
                    WeekDayHeaders(colors = colors)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar grid with animation
                    AnimatedContent(
                        targetState = Pair(displayYear, displayMonth),
                        transitionSpec = {
                            val direction = navigationDirection
                            (slideInHorizontally { width -> direction * width } + fadeIn())
                                .togetherWith(slideOutHorizontally { width -> -direction * width } + fadeOut())
                        },
                        label = "calendar_animation"
                    ) { (year, month) ->
                        CalendarGrid(
                            year = year,
                            month = month,
                            selectedDate = selectedDate,
                            today = today,
                            maxDate = maxDate,
                            minDate = minDate,
                            colors = colors,
                            onDateClick = { day ->
                                onDateSelected(PersianDate(year, month, day))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    year: Int,
    month: Int,
    colors: CalendarColors,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthName = PersianDateUtils.getMonthName(month)
    val yearStr = year.toString().toFarsiDigits()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.headerBackground)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left arrow (previous month)
        IconButton(onClick = onPreviousMonth, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "ماه قبل",
                tint = colors.textMuted
            )
        }

        // Month name and year
        Text(
            text = "$monthName $yearStr",
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        // Right arrow (next month)
        IconButton(onClick = onNextMonth, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "ماه بعد",
                tint = colors.textMuted
            )
        }
    }
}

@Composable
private fun WeekDayHeaders(colors: CalendarColors) {
    // Persian week starts with Saturday (شنبه), ends Friday (جمعه).
    // Order must match the grid's index convention (Saturday = index 0).
    // In the RTL row, index 0 renders rightmost, so شنبه sits on the right and جمعه on the left.
    val dayLabels = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        dayLabels.forEach { label ->
            Text(
                text = label,
                color = colors.textMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    year: Int,
    month: Int,
    selectedDate: PersianDate,
    today: PersianDate,
    maxDate: PersianDate?,
    minDate: PersianDate?,
    colors: CalendarColors,
    onDateClick: (Int) -> Unit
) {
    val daysInMonth = PersianDateUtils.getDaysInJalaliMonth(year, month)
    // Get what day of the week the 1st falls on (0=Saturday, 6=Friday)
    val firstDayOfWeek = PersianDateUtils.getFirstDayOfWeekForJalaliMonth(year, month)

    // Build the grid: empty cells for offset + day numbers
    val cells = mutableListOf<Int?>()
    repeat(firstDayOfWeek) { cells.add(null) }
    for (day in 1..daysInMonth) { cells.add(day) }
    // Pad to fill last row
    while (cells.size % 7 != 0) { cells.add(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .height(((cells.size / 7) * 48).dp),
        userScrollEnabled = false
    ) {
        items(cells) { day ->
            if (day == null) {
                Box(modifier = Modifier.aspectRatio(1f))
            } else {
                val isSelected = selectedDate.year == year &&
                        selectedDate.month == month &&
                        selectedDate.day == day
                val isToday = today.year == year &&
                        today.month == month &&
                        today.day == day

                val currentDate = PersianDate(year, month, day)
                val isEnabled = isDateInRange(currentDate, minDate, maxDate)

                DayCell(
                    day = day,
                    isSelected = isSelected,
                    isToday = isToday,
                    isEnabled = isEnabled,
                    colors = colors,
                    onClick = { if (isEnabled) onDateClick(day) }
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    isEnabled: Boolean,
    colors: CalendarColors,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> colors.accent
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> colors.background
        !isEnabled -> colors.textDisabled
        isToday -> colors.accent
        else -> colors.textPrimary
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .then(
                if (isEnabled) {
                    Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onClick
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString().toFarsiDigits(),
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Compares two PersianDate values for range checking.
 */
private fun isDateInRange(
    date: PersianDate,
    minDate: PersianDate?,
    maxDate: PersianDate?
): Boolean {
    val dateVal = date.year * 10000 + date.month * 100 + date.day
    if (minDate != null) {
        val minVal = minDate.year * 10000 + minDate.month * 100 + minDate.day
        if (dateVal < minVal) return false
    }
    if (maxDate != null) {
        val maxVal = maxDate.year * 10000 + maxDate.month * 100 + maxDate.day
        if (dateVal > maxVal) return false
    }
    return true
}
