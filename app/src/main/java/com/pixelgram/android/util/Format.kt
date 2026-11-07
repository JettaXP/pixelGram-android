package com.pixelgram.android.util

import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

private val zone get() = ZoneId.systemDefault()

fun chatListTime(epochSeconds: Int, locale: Locale = Locale.getDefault()): String {
    if (epochSeconds <= 0) return ""
    val time = Instant.ofEpochSecond(epochSeconds.toLong()).atZone(zone)
    val today = LocalDate.now(zone)
    val date = time.toLocalDate()
    return when {
        date == today -> messageTime(epochSeconds)
        date.isAfter(today.minusDays(6)) -> date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
        date.year == today.year -> DateTimeFormatter.ofPattern("d MMM", locale).format(date)
        else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale).format(date)
    }
}

fun messageTime(epochSeconds: Int, seconds: Boolean = false): String {
    val time = Instant.ofEpochSecond(epochSeconds.toLong()).atZone(zone)
    return DateTimeFormatter.ofPattern(if (seconds) "HH:mm:ss" else "HH:mm").format(time)
}

private val zalgo = Regex("\\p{M}{3,}")

fun stripZalgo(text: String): String = text.replace(zalgo, "")

fun dayHeader(epochSeconds: Int, locale: Locale = Locale.getDefault()): String {
    val date = Instant.ofEpochSecond(epochSeconds.toLong()).atZone(zone).toLocalDate()
    val pattern = if (date.year == LocalDate.now(zone).year) "d MMMM" else "d MMMM yyyy"
    return DateTimeFormatter.ofPattern(pattern, locale).format(date)
}

fun fullDate(epochSeconds: Int): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(java.util.Date(epochSeconds * 1000L))

fun initials(title: String): String {
    val letters = title.trim().split(' ', '\t', '_', '-').mapNotNull { word -> word.firstOrNull { it.isLetterOrDigit() } }
    return when {
        letters.size >= 2 -> "${letters[0].uppercaseChar()}${letters[1].uppercaseChar()}"
        letters.size == 1 -> letters[0].uppercaseChar().toString()
        else -> "?"
    }
}

fun formatPhone(digits: String): String {
    if (digits.isEmpty()) return ""
    runCatching { Phones.formatInternational(digits) }.getOrNull()?.let { return it }
    val groups = listOf(1, 3, 3, 2, 2, 4)
    val out = StringBuilder()
    var index = 0
    for (size in groups) {
        if (index >= digits.length) break
        if (out.isNotEmpty()) out.append(' ')
        out.append(digits.substring(index, minOf(digits.length, index + size)))
        index += size
    }
    if (index < digits.length) out.append(digits.substring(index))
    return out.toString()
}
