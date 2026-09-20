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

    @Test fun consistencyCountsDistinctStudyDaysAndAveragesOnlyActiveDays() {
        val day=86_400_000L
        val now=7*day
        val logs=listOf(log(now-1*day+1000,20),log(now-1*day+2000,10),log(now-3*day+1000,40))
        val s=ProgressAnalyticsCalculator.calculate(logs,null,now)
        assertEquals(2,s.consistencyDays); assertEquals(35,s.averageMinutesOnStudyDays)
    }
}
