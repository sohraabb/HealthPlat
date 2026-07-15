package com.bonyad.healthplat.ui.utils

val farsiDigits = mapOf(
    '0' to '۰', '1' to '۱', '2' to '۲', '3' to '۳', '4' to '۴',
    '5' to '۵', '6' to '۶', '7' to '۷', '8' to '۸', '9' to '۹'
)

fun String.toFarsiDigits(): String {
    return this.map { it -> farsiDigits[it] ?: it }.joinToString("")
}

/**
 * Wraps a Persian/RTL string with Right-to-Left Marks (U+200F) so that
 * neutral punctuation characters (`.`, `...`, `!`, `?`, etc.) stay at the
 * visual end of the text instead of jumping to the visual start due to BiDi.
 */
fun String.rtl(): String = "\u200F${this}\u200F"