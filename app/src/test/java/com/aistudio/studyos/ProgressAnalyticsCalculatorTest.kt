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
        assertEquals(2,s.consistencyDays); assertEquals(35,s.averageMinutesOnStudyDays)
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

}
