package com.example.activitycounter.model

import java.util.Calendar
import java.util.TimeZone

// Declared in urgency order so the list can be sorted by status directly.
enum class ActivityStatus {
    MISSED,    // red: two or more periods passed without completion
    LATE,      // yellow: one period passed without completion
    PENDING,   // neutral: current period, not completed yet
    COMPLETED  // green: completed in the current period
}

val PERIOD_OPTIONS_DAYS = listOf(1, 2, 3, 4, 5, 6, 7, 14, 21, 28)

fun periodName(days: Int): String =
    if (days % 7 == 0) pluralize(days / 7, "week") else pluralize(days, "day")

fun periodLabel(days: Int): String {
    val (n, unit) = if (days % 7 == 0) days / 7 to "week" else days to "day"
    return if (n == 1) "Every $unit" else "Every $n ${unit}s"
}

private fun pluralize(n: Int, unit: String) = if (n == 1) "1 $unit" else "$n ${unit}s"

private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

// Days since 1970-01-01 in the given time zone (DST-aware).
private fun epochDay(millis: Long, tz: TimeZone): Long =
    (millis + tz.getOffset(millis)).floorDiv(DAY_MILLIS)

private fun startOfDayMillis(epochDay: Long, tz: TimeZone): Long =
    Calendar.getInstance(tz).apply {
        clear()
        set(1970, Calendar.JANUARY, 1)
        add(Calendar.DAY_OF_YEAR, epochDay.toInt())
    }.timeInMillis

// 1970-01-01 was a Thursday, so +3 makes Monday = 0.
private fun mondayOf(epochDay: Long): Long = epochDay - (epochDay + 3).mod(7L)

// Cycles tile back-to-back from the creation day; week-based periods start on Monday.
private fun ActivityItem.anchorDay(tz: TimeZone): Long {
    val createdDay = epochDay(createdAt, tz)
    return if (periodDays % 7 == 0) mondayOf(createdDay) else createdDay
}

private fun ActivityItem.cycleIndex(millis: Long, tz: TimeZone): Long =
    (epochDay(millis, tz) - anchorDay(tz)).floorDiv(periodDays.toLong())

fun startOfCurrentWeekMillis(now: Long, tz: TimeZone = TimeZone.getDefault()): Long =
    startOfDayMillis(mondayOf(epochDay(now, tz)), tz)

fun ActivityItem.status(now: Long, tz: TimeZone = TimeZone.getDefault()): ActivityStatus {
    val current = cycleIndex(now, tz)
    // The first cycle that counts as "due": the creation cycle, or the one after the last completion.
    val firstDueCycle = if (lastCompletedAt > 0L) {
        val completedCycle = cycleIndex(lastCompletedAt, tz)
        if (completedCycle == current) return ActivityStatus.COMPLETED
        completedCycle + 1
    } else {
        0L
    }
    return when (current - firstDueCycle) {
        in Long.MIN_VALUE..0L -> ActivityStatus.PENDING
        1L -> ActivityStatus.LATE
        else -> ActivityStatus.MISSED
    }
}

// Days left in the current cycle, counting today (1 = today is the last day).
fun ActivityItem.daysLeftInCycle(now: Long, tz: TimeZone = TimeZone.getDefault()): Int {
    val endDay = anchorDay(tz) + (cycleIndex(now, tz) + 1) * periodDays
    return (endDay - epochDay(now, tz)).toInt()
}

// Most urgent first; within the same status, shorter periods first, then original order.
fun List<ActivityItem>.sortedByUrgency(
    now: Long,
    tz: TimeZone = TimeZone.getDefault()
): List<Pair<ActivityItem, ActivityStatus>> =
    map { it to it.status(now, tz) }
        .sortedWith(compareBy({ it.second }, { it.first.periodDays }))

// Completes the activity for the current cycle, or undoes that completion if already done.
fun ActivityItem.toggleCompleted(now: Long, tz: TimeZone = TimeZone.getDefault()): ActivityItem =
    if (status(now, tz) == ActivityStatus.COMPLETED) {
        copy(lastCompletedAt = previousCompletedAt)
    } else {
        copy(previousCompletedAt = lastCompletedAt, lastCompletedAt = now)
    }
