package com.workplanner.app.ui.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val turkish: Locale = Locale.forLanguageTag("tr-TR")

private fun formatter(pattern: String) = SimpleDateFormat(pattern, turkish)

fun formatReminder(millis: Long): String =
    formatter("d MMM • HH:mm").format(Date(millis))

fun formatReminderFriendly(millis: Long): String {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = millis }
    val time = formatter("HH:mm").format(Date(millis))
    val sameDay = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    if (sameDay) return "Bugün $time"

    val tomorrow = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
    val isTomorrow = tomorrow.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        tomorrow.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    if (isTomorrow) return "Yarın $time"

    return formatReminder(millis)
}

fun formatFullDate(millis: Long): String =
    formatter("d MMMM yyyy, EEEE").format(Date(millis))

fun formatShortDate(millis: Long): String =
    formatter("d MMM").format(Date(millis))

fun formatDateTime(millis: Long): String =
    formatter("d MMMM yyyy • HH:mm").format(Date(millis))

fun formatClock(millis: Long): String =
    formatter("HH:mm").format(Date(millis))

fun isOverdue(reminderAt: Long?, isDone: Boolean): Boolean =
    !isDone && reminderAt != null && reminderAt < System.currentTimeMillis()

/** "3 sa 20 dk" style label used for the next reminder. */
fun humanizeUntil(millis: Long): String {
    val diff = millis - System.currentTimeMillis()
    if (diff <= 0) return "geçti"
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
    return when {
        days > 0 -> "${days}g ${hours}s"
        hours > 0 -> "${hours}s ${minutes}dk"
        else -> "${minutes}dk"
    }
}

fun nowPlus(days: Int = 0, hours: Int = 0): Long =
    Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, days)
        add(Calendar.HOUR_OF_DAY, hours)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
