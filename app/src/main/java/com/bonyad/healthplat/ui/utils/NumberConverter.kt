package com.bonyad.healthplat.ui.utils

val farsiDigits = mapOf(
    '0' to '۰', '1' to '۱', '2' to '۲', '3' to '۳', '4' to '۴',
    '5' to '۵', '6' to '۶', '7' to '۷', '8' to '۸', '9' to '۹'
)

fun String.toFarsiDigits(): String {
    return this.map { it -> farsiDigits[it] ?: it }.joinToString("")
}