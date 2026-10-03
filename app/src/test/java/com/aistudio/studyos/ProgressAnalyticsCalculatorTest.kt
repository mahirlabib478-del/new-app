package com.aistudio.studyos

import com.aistudio.studyos.data.local.entity.SessionLogEntity
import com.aistudio.studyos.data.local.entity.StudyPlanEntity
import com.aistudio.studyos.data.repository.ProgressAnalyticsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressAnalyticsCalculatorTest {
    private fun log(ts: Long, min: Int) = SessionLogEntity(subject="Math", chapter="A", durationMinutes=min, mode="regular", xpEarned=min, timestamp=ts)

    @Test fun emptyDataIsSafe() {
        val s = ProgressAnalyticsCalculator.calculate(emptyList(), null, 7 * 86_400_000L)
        assertEquals(0, s.consistencyDays); assertEquals(0, s.averageMinutesOnStudyDays); assertNull(s.activePlanTitle)
    }

    @Test fun activePlanUsesPersistedActualMinutes() {
        val plan = StudyPlanEntity(id=1,title="Plan",subject="Math",chapter="A",mode="regular",totalBlocks=4,totalDurationMinutes=100,accumulatedBillableMinutes=40)
        val s = ProgressAnalyticsCalculator.calculate(emptyList(), plan, 7 * 86_400_000L)
        assertEquals(100, s.plannedMinutes); assertEquals(40, s.actualMinutes); assertEquals(40, s.planCompletionPercent); assertEquals(60, s.activePlanRemainingMinutes)
    }

    @Test fun completedPlanRemainsVisibleWhenThereIsNoActivePlan() {
        val completed = StudyPlanEntity(
            id = 2,
            title = "Completed Plan",
            subject = "English",
            chapter = "Grammar",
            mode = "regular",
            totalBlocks = 3,
            totalDurationMinutes = 60,
            accumulatedBillableMinutes = 60,
            isCompleted = true
        )
        val s = ProgressAnalyticsCalculator.calculate(
            emptyList(),
            null,
            7 * 86_400_000L,
            latestCompletedPlan = completed
        )
        assertEquals(60, s.plannedMinutes)
        assertEquals(60, s.actualMinutes)
        assertEquals(100, s.planCompletionPercent)
        assertEquals("Completed Plan", s.activePlanTitle)
        assertEquals(0, s.activePlanRemainingMinutes)
    }

    @Test fun consistencyCountsDistinctStudyDaysAndAveragesOnlyActiveDays() {
        val day=86_400_000L
        val now=7*day
        val logs=listOf(log(now-1*day+1000,20),log(now-1*day+2000,10),log(now-3*day+1000,40))
        val s=ProgressAnalyticsCalculator.calculate(logs,null,now)
        assertEquals(2,s.consistencyDays); assertEquals(28,s.consistencyPercent); assertEquals(70,s.weeklyStudyMinutes); assertEquals(35,s.averageMinutesOnStudyDays)
    }

    @Test fun completionPercentAndRemainingMinutesAreCappedAtPlanBudget() {
        val plan = StudyPlanEntity(
            id = 3,
            title = "Over-completed",
            subject = "Science",
            chapter = "Cells",
            mode = "regular",
            totalBlocks = 2,
            totalDurationMinutes = 50,
            accumulatedBillableMinutes = 75
        )
        val s = ProgressAnalyticsCalculator.calculate(emptyList(), plan, 7 * 86_400_000L)
        assertEquals(50, s.plannedMinutes)
        assertEquals(75, s.actualMinutes)
        assertEquals(100, s.planCompletionPercent)
        assertEquals(0, s.activePlanRemainingMinutes)
    }

    @Test fun activePlanTakesPrecedenceOverLatestCompletedPlan() {
        val active = StudyPlanEntity(
            id = 4,
            title = "Current",
            subject = "Math",
            chapter = "Algebra",
            mode = "regular",
            totalBlocks = 2,
            totalDurationMinutes = 80,
            accumulatedBillableMinutes = 20
        )
        val completed = StudyPlanEntity(
            id = 5,
            title = "Older completed",
            subject = "English",
            chapter = "Grammar",
            mode = "regular",
            totalBlocks = 1,
            totalDurationMinutes = 30,
            accumulatedBillableMinutes = 30,
            isCompleted = true
        )
        val s = ProgressAnalyticsCalculator.calculate(
            emptyList(), active, 7 * 86_400_000L, latestCompletedPlan = completed
        )
        assertEquals("Current", s.activePlanTitle)
        assertEquals(80, s.plannedMinutes)
        assertEquals(20, s.actualMinutes)
    }

    @Test fun negativeDurationsDoNotReduceConsistencyOrAverage() {
        val day = 86_400_000L
        val now = 7 * day
        val logs = listOf(log(now - day + 1000, -25), log(now - day + 2000, 30))
        val s = ProgressAnalyticsCalculator.calculate(logs, null, now)
        assertEquals(1, s.consistencyDays)
        assertEquals(30, s.averageMinutesOnStudyDays)
    }

    private fun localDay(year: Int, month: Int, day: Int, hour: Int = 0): Long =
        java.util.Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test fun localMidnightBelongsToItsCalendarDayAndSevenDayWindowIsInclusive() {
        val todayStart = localDay(2026, java.util.Calendar.OCTOBER, 2)
        val now = todayStart + 12 * 60 * 60 * 1000L
        val logs = listOf(
            log(todayStart, 15),
            log(localDay(2026, java.util.Calendar.SEPTEMBER, 26, 23), 25),
            log(localDay(2026, java.util.Calendar.SEPTEMBER, 25, 23), 90),
            log(now + 60_000L, 40)
        )

        val summary = ProgressAnalyticsCalculator.calculate(logs, null, now)

        assertEquals(2, summary.consistencyDays)
        assertEquals(40, summary.weeklyStudyMinutes)
    }

    @Test fun optimizedAggregationTotalsEachOfSevenLocalDaysExactlyOnce() {
        val now = localDay(2026, java.util.Calendar.OCTOBER, 2, 12)
        val logs = (0 until 7).map { offset ->
            val timestamp = java.util.Calendar.getInstance().apply {
                timeInMillis = now
                add(java.util.Calendar.DAY_OF_YEAR, -offset)
                set(java.util.Calendar.HOUR_OF_DAY, 10)
            }.timeInMillis
            log(timestamp, offset + 1)
        } + listOf(
            log(localDay(2026, java.util.Calendar.SEPTEMBER, 24, 10), 100),
            log(now + 60_000L, 200)
        )

        val summary = ProgressAnalyticsCalculator.calculate(logs, null, now)

        assertEquals(7, summary.consistencyDays)
        assertEquals(28, summary.weeklyStudyMinutes)
        assertEquals(4, summary.averageMinutesOnStudyDays)
    }

    @Test fun currentDayIncludesTimestampNowButExcludesNextMillisecond() {
        val now = localDay(2026, java.util.Calendar.OCTOBER, 2, 12)
        val summary = ProgressAnalyticsCalculator.calculate(
            listOf(log(now, 12), log(now + 1L, 99)),
            null,
            now
        )

        assertEquals(1, summary.consistencyDays)
        assertEquals(12, summary.weeklyStudyMinutes)
    }

    @Test fun unusuallyLargeDailyTotalsSaturateInsteadOfOverflowing() {
        val now = localDay(2026, java.util.Calendar.OCTOBER, 2, 12)
        val summary = ProgressAnalyticsCalculator.calculate(
            listOf(log(now - 60_000L, Int.MAX_VALUE), log(now - 30_000L, 100)),
            null,
            now
        )

        assertEquals(Int.MAX_VALUE, summary.weeklyStudyMinutes)
        assertEquals(Int.MAX_VALUE, summary.averageMinutesOnStudyDays)
        assertEquals(1, summary.consistencyDays)
    }

    @Test fun calendarDayWindowHandlesDaylightSavingTransitions() {
        val zone = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/New_York"))
            val now = localDay(2026, java.util.Calendar.MARCH, 9, 12)
            val yesterday = localDay(2026, java.util.Calendar.MARCH, 8, 12)
            val summary = ProgressAnalyticsCalculator.calculate(
                listOf(log(now, 20), log(yesterday, 30)),
                null,
                now
            )

            assertEquals(2, summary.consistencyDays)
            assertEquals(50, summary.weeklyStudyMinutes)
        } finally {
            java.util.TimeZone.setDefault(zone)
        }
    }


    @Test fun midnightNowDoesNotCountPreviousDayOrFutureSessions() {
        val midnight = localDay(2026, java.util.Calendar.OCTOBER, 3)
        val summary = ProgressAnalyticsCalculator.calculate(
            listOf(
                log(midnight - 1L, 45),
                log(midnight, 10),
                log(midnight + 1L, 99)
            ),
            null,
            midnight
        )

        assertEquals(1, summary.consistencyDays)
        assertEquals(10, summary.weeklyStudyMinutes)
        assertEquals(10, summary.averageMinutesOnStudyDays)
    }
}
