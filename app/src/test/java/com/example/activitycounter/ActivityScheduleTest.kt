package com.example.activitycounter

import com.example.activitycounter.model.ActivityItem
import com.example.activitycounter.model.ActivityStatus.COMPLETED
import com.example.activitycounter.model.ActivityStatus.LATE
import com.example.activitycounter.model.ActivityStatus.MISSED
import com.example.activitycounter.model.ActivityStatus.PENDING
import com.example.activitycounter.model.daysLeftInCycle
import com.example.activitycounter.model.periodLabel
import com.example.activitycounter.model.periodName
import com.example.activitycounter.model.sortedByUrgency
import com.example.activitycounter.model.startOfCurrentWeekMillis
import com.example.activitycounter.model.status
import com.example.activitycounter.model.toggleCompleted
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

// October 2026: Mon 5, Wed 7, Mon 12, Mon 19, Sun 25 (DST ends), Mon 26.
class ActivityScheduleTest {
    private val tz = TimeZone.getTimeZone("Europe/Riga")

    private fun at(month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long =
        Calendar.getInstance(tz).apply {
            clear()
            set(2026, month - 1, day, hour, minute)
        }.timeInMillis

    private fun activity(periodDays: Int, createdAt: Long, lastCompletedAt: Long = 0L) =
        ActivityItem(name = "Test", periodDays = periodDays, createdAt = createdAt, lastCompletedAt = lastCompletedAt)

    @Test
    fun weekly_neutralThenYellowThenRed() {
        val a = activity(7, createdAt = at(10, 7))
        assertEquals(PENDING, a.status(at(10, 11, 23, 59), tz))
        assertEquals(LATE, a.status(at(10, 12, 0, 1), tz))
        assertEquals(LATE, a.status(at(10, 18, 23, 59), tz))
        assertEquals(MISSED, a.status(at(10, 19, 0, 1), tz))
        assertEquals(MISSED, a.status(at(11, 30), tz))
    }

    @Test
    fun weekly_completedStaysGreenUntilMondayThenResets() {
        val a = activity(7, createdAt = at(10, 7), lastCompletedAt = at(10, 20))
        assertEquals(COMPLETED, a.status(at(10, 25, 23, 59), tz))
        assertEquals(PENDING, a.status(at(10, 26, 0, 1), tz))
        assertEquals(LATE, a.status(at(11, 2, 0, 1), tz))
    }

    @Test
    fun undo_restoresRed() {
        val now = at(10, 20)
        val missed = activity(7, createdAt = at(10, 7))
        assertEquals(MISSED, missed.status(now, tz))

        val completed = missed.toggleCompleted(now, tz)
        assertEquals(COMPLETED, completed.status(now, tz))
        assertEquals(MISSED, completed.toggleCompleted(now, tz).status(now, tz))
    }

    @Test
    fun undo_restoresYellowWithEarlierCompletion() {
        val now = at(10, 20)
        val late = activity(7, createdAt = at(10, 7), lastCompletedAt = at(10, 8))
        assertEquals(LATE, late.status(now, tz))

        val undone = late.toggleCompleted(now, tz).toggleCompleted(now, tz)
        assertEquals(LATE, undone.status(now, tz))
        assertEquals(at(10, 8), undone.lastCompletedAt)
    }

    @Test
    fun threeDays_cyclesAnchoredToCreationDay() {
        // Cycles: [Oct 7, Oct 10), [Oct 10, Oct 13), [Oct 13, Oct 16)
        val a = activity(3, createdAt = at(10, 7, 15))
        assertEquals(PENDING, a.status(at(10, 9, 23, 59), tz))
        assertEquals(LATE, a.status(at(10, 10, 0, 1), tz))
        assertEquals(MISSED, a.status(at(10, 13, 0, 1), tz))

        val done = a.copy(lastCompletedAt = at(10, 11))
        assertEquals(COMPLETED, done.status(at(10, 12, 23, 59), tz))
        assertEquals(PENDING, done.status(at(10, 13, 0, 1), tz))
        assertEquals(LATE, done.status(at(10, 16, 0, 1), tz))
    }

    @Test
    fun daily_resetsAtMidnight() {
        val a = activity(1, createdAt = at(10, 7, 9), lastCompletedAt = at(10, 7, 20))
        assertEquals(COMPLETED, a.status(at(10, 7, 23, 59), tz))
        assertEquals(PENDING, a.status(at(10, 8, 0, 1), tz))
        assertEquals(LATE, a.status(at(10, 9, 0, 1), tz))
        assertEquals(MISSED, a.status(at(10, 10, 0, 1), tz))
    }

    @Test
    fun twoWeeks_alignedToMonday() {
        // Created Wed Oct 7 -> cycles start Mon Oct 5, Mon Oct 19, Mon Nov 2
        val a = activity(14, createdAt = at(10, 7))
        assertEquals(PENDING, a.status(at(10, 18, 23, 59), tz))
        assertEquals(LATE, a.status(at(10, 19, 0, 1), tz))
        assertEquals(MISSED, a.status(at(11, 2, 0, 1), tz))
    }

    @Test
    fun changingPeriod_recalculatesFromLastCompletion() {
        val weekly = activity(7, createdAt = at(10, 7), lastCompletedAt = at(10, 8))
        assertEquals(COMPLETED, weekly.status(at(10, 11), tz))
        // Daily: done Oct 8, Oct 9 neutral, Oct 10 yellow, Oct 11 red
        assertEquals(MISSED, weekly.copy(periodDays = 1).status(at(10, 11), tz))
    }

    @Test
    fun daysLeft_countsTodayUntilCycleEnd() {
        val weekly = activity(7, createdAt = at(10, 7))
        assertEquals(4, weekly.daysLeftInCycle(at(10, 8), tz))   // Thu: Thu, Fri, Sat, Sun
        assertEquals(1, weekly.daysLeftInCycle(at(10, 11, 23), tz)) // Sun: last day
        assertEquals(7, weekly.daysLeftInCycle(at(10, 12, 0, 1), tz)) // Mon: new week
        assertEquals(1, activity(1, createdAt = at(10, 7)).daysLeftInCycle(at(10, 9), tz))
    }

    @Test
    fun sortedByUrgency_statusThenShorterPeriodThenOriginalOrder() {
        val now = at(10, 20)
        val weeklyPending = activity(7, createdAt = at(10, 19)).copy(name = "weeklyPending")
        val dailyPending = activity(1, createdAt = at(10, 20)).copy(name = "dailyPending")
        val weeklyLate = activity(7, createdAt = at(10, 7), lastCompletedAt = at(10, 8)).copy(name = "weeklyLate")
        val threeDayDone = activity(3, createdAt = at(10, 20), lastCompletedAt = at(10, 20)).copy(name = "threeDayDone")
        val dailyDone = activity(1, createdAt = at(10, 20), lastCompletedAt = at(10, 20)).copy(name = "dailyDone")
        val weeklyPending2 = activity(7, createdAt = at(10, 19)).copy(name = "weeklyPending2")

        val sorted = listOf(weeklyPending, dailyPending, weeklyLate, threeDayDone, dailyDone, weeklyPending2)
            .sortedByUrgency(now, tz)

        assertEquals(
            listOf("weeklyLate", "dailyPending", "weeklyPending", "weeklyPending2", "dailyDone", "threeDayDone"),
            sorted.map { it.first.name }
        )
    }

    @Test
    fun resetValues_makeActivityNeutral() {
        val now = at(10, 20)
        val missed = activity(7, createdAt = at(10, 1), lastCompletedAt = at(10, 1))
        assertEquals(MISSED, missed.status(now, tz))
        // Same values the "reset all" query writes
        val reset = missed.copy(createdAt = now, lastCompletedAt = 0L, previousCompletedAt = 0L)
        assertEquals(PENDING, reset.status(now, tz))
    }

    @Test
    fun startOfCurrentWeek_isMondayMidnight() {
        assertEquals(at(10, 5, 0), startOfCurrentWeekMillis(at(10, 11, 18), tz))
        assertEquals(at(10, 12, 0), startOfCurrentWeekMillis(at(10, 12, 0, 1), tz))
    }

    @Test
    fun periodNamesAndLabels() {
        assertEquals("1 day", periodName(1))
        assertEquals("3 days", periodName(3))
        assertEquals("1 week", periodName(7))
        assertEquals("2 weeks", periodName(14))
        assertEquals("Every day", periodLabel(1))
        assertEquals("Every 5 days", periodLabel(5))
        assertEquals("Every week", periodLabel(7))
        assertEquals("Every 3 weeks", periodLabel(21))
    }
}
