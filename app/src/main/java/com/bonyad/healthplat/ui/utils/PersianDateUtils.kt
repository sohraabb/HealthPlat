package com.bonyad.healthplat.ui.utils

import android.icu.util.Calendar
import com.bonyad.healthplat.domain.model.PersianDateTime

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

    private fun georgianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val g_d_m = intArrayOf(0,31,59,90,120,151,181,212,243,273,304,334)
        var jy: Int
        var jm: Int
        var jd: Int
        val gy2 = if (gm > 2) gy + 1 else gy
        val days = 355666 + (365 * gy) + ((gy2 + 3) / 4) -
                ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + g_d_m[gm - 1]

        jy = -1595 + (33 * (days / 12053))
        var d = days % 12053
        jy += 4 * (d / 1461)
        d %= 1461
        if (d > 365) {
            jy += (d - 1) / 365
            d = (d - 1) % 365
        }
        jm = if (d < 186) 1 + (d / 31) else 7 + ((d - 186) / 30)
        jd = 1 + if (d < 186) d % 31 else (d - 186) % 30
        return Triple(jy, jm, jd)
    }

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
}