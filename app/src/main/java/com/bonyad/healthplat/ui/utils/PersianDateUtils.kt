package com.bonyad.healthplat.ui.utils

import com.bonyad.healthplat.domain.model.PersianDateTime
import java.util.Calendar

object PersianDateUtils {

    private val persianWeekDays = mapOf(
        Calendar.SATURDAY to "شنبه",
        Calendar.SUNDAY to "یکشنبه",
        Calendar.MONDAY to "دوشنبه",
        Calendar.TUESDAY to "سه‌شنبه",
        Calendar.WEDNESDAY to "چهارشنبه",
        Calendar.THURSDAY to "پنجشنبه",
        Calendar.FRIDAY to "جمعه"
    )

    private val persianMonthNames = mapOf(
        1 to "فروردین",
        2 to "اردیبهشت",
        3 to "خرداد",
        4 to "تیر",
        5 to "مرداد",
        6 to "شهریور",
        7 to "مهر",
        8 to "آبان",
        9 to "آذر",
        10 to "دی",
        11 to "بهمن",
        12 to "اسفند"
    )

    // ──────────────────────────────────────────────
    //  Gregorian → Jalali
    // ──────────────────────────────────────────────
    fun georgianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val g_d_m = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        val days = 355666 + (365 * gy) + ((gy2 + 3) / 4) -
                ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + g_d_m[gm - 1]

        var jy = -1595 + (33 * (days / 12053))
        var d = days % 12053
        jy += 4 * (d / 1461)
        d %= 1461
        if (d > 365) {
            jy += (d - 1) / 365
            d = (d - 1) % 365
        }
        val jm = if (d < 186) 1 + (d / 31) else 7 + ((d - 186) / 30)
        val jd = 1 + if (d < 186) d % 31 else (d - 186) % 30
        return Triple(jy, jm, jd)
    }

    // ──────────────────────────────────────────────
    //  Jalali → Gregorian
    // ──────────────────────────────────────────────
    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        val jy1 = jy + 1595
        var days = -355668 + (365 * jy1) + ((jy1 / 33) * 8) + (((jy1 % 33) + 3) / 4) +
                jd + (if (jm < 7) (jm - 1) * 31 else ((jm - 7) * 30) + 186)

        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            gy += 100 * (--days / 36524)
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }

        val gd_m = intArrayOf(0, 31, if (gy % 4 == 0 && (gy % 100 != 0 || gy % 400 == 0)) 29 else 28,
            31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        var remaining = days.toInt()
        for (i in 1..12) {
            if (remaining < gd_m[i]) {
                gm = i
                break
            }
            remaining -= gd_m[i]
        }
        val gd = remaining + 1
        return Triple(gy, gm, gd)
    }

    // ──────────────────────────────────────────────
    //  Calendar helpers
    // ──────────────────────────────────────────────

    /**
     * Returns the current Jalali date as (year, month, day).
     */
    fun getCurrentJalaliDate(): Triple<Int, Int, Int> {
        val calendar = Calendar.getInstance()
        return georgianToJalali(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Returns the number of days in a given Jalali month.
     * Months 1-6 have 31 days, months 7-11 have 30 days,
     * month 12 has 29 days (30 in leap years).
     */
    fun getDaysInJalaliMonth(year: Int, month: Int): Int {
        return when {
            month in 1..6 -> 31
            month in 7..11 -> 30
            month == 12 -> if (isJalaliLeapYear(year)) 30 else 29
            else -> 30
        }
    }

    /**
     * Determines if a Jalali year is a leap year.
     */
    fun isJalaliLeapYear(year: Int): Boolean {
        val leapYears = listOf(1, 5, 9, 13, 17, 22, 26, 30)
        return leapYears.contains(year % 33)
    }

    /**
     * Returns the day-of-week index (0 = Saturday, 6 = Friday)
     * for the 1st day of a given Jalali month.
     */
    fun getFirstDayOfWeekForJalaliMonth(year: Int, month: Int): Int {
        // Convert Jalali 1st of month to Gregorian
        val (gy, gm, gd) = jalaliToGregorian(year, month, 1)

        val calendar = Calendar.getInstance()
        calendar.set(gy, gm - 1, gd)

        // Calendar.SATURDAY = 7, SUNDAY = 1, ..., FRIDAY = 6
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // Map to our 0-indexed system where Saturday = 0
        return when (dayOfWeek) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    /**
     * Returns the Persian month name for the given month number (1-12).
     */
    fun getMonthName(month: Int): String {
        return persianMonthNames[month] ?: ""
    }

    // ──────────────────────────────────────────────
    //  Existing methods
    // ──────────────────────────────────────────────

    fun getCurrentPersianDateTime(): PersianDateTime {
        val calendar = Calendar.getInstance()

        val gYear = calendar.get(Calendar.YEAR)
        val gMonth = calendar.get(Calendar.MONTH) + 1
        val gDay = calendar.get(Calendar.DAY_OF_MONTH)
        val (jy, jm, jd) = georgianToJalali(gYear, gMonth, gDay)

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val weekDay = persianWeekDays[calendar.get(Calendar.DAY_OF_WEEK)] ?: ""

        val time = String.format("%02d:%02d", hour, minute)
        val date = String.format("%04d/%02d/%02d", jy, jm, jd)
        return PersianDateTime(time, date, weekDay)
    }

    /**
     * Get formatted Persian date with month name
     * Format: "جمعه ۱۷ بهمن"
     */
    fun getFormattedPersianDate(): String {
        val calendar = Calendar.getInstance()

        val gYear = calendar.get(Calendar.YEAR)
        val gMonth = calendar.get(Calendar.MONTH) + 1
        val gDay = calendar.get(Calendar.DAY_OF_MONTH)
        val (jy, jm, jd) = georgianToJalali(gYear, gMonth, gDay)

        val weekDay = persianWeekDays[calendar.get(Calendar.DAY_OF_WEEK)] ?: ""
        val monthName = persianMonthNames[jm] ?: ""

        return "$weekDay $jd $monthName"
    }
}