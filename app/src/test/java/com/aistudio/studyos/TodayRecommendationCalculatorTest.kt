package com.aistudio.studyos

import com.aistudio.studyos.data.local.entity.ExamEntity
import com.aistudio.studyos.data.local.entity.StudyPlanEntity
import com.aistudio.studyos.data.local.entity.StudyPlanItem
import com.aistudio.studyos.data.local.entity.StudyPlanItemCodec
import com.aistudio.studyos.data.repository.TodayRecommendationCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayRecommendationCalculatorTest {
    private fun plan(items: List<StudyPlanItem>, currentBlockIndex: Int = 0, isTimerRunning: Boolean = false) = StudyPlanEntity(id=1,title="Saved",subject=items.first().subject,chapter=items.first().topic,mode="regular",totalBlocks=items.size,currentBlockIndex=currentBlockIndex,durationPerBlockMinutes=items.first().minutes,planItems=StudyPlanItemCodec.encode(items),totalDurationMinutes=items.sumOf { it.minutes },isTimerRunning=isTimerRunning)

    @Test fun activeRunningPlanAlwaysGetsPriority() {
        val r = TodayRecommendationCalculator.calculate(plan(listOf(StudyPlanItem("Math","Calculus",25),StudyPlanItem("Math","Limits",20)),1,true),listOf(ExamEntity(subject="Physics",examDate="Tomorrow",daysRemaining=1)),0,60)
        assertEquals("Continue your active session", r.title); assertEquals("Limits", r.nextItem?.topic); assertTrue(r.shouldOpenFocus)
    }

    @Test fun nextUnfinishedPlanItemIsSelectedWithinSavedPlanBudget() {
        val r = TodayRecommendationCalculator.calculate(plan(listOf(StudyPlanItem("Math","Algebra",25),StudyPlanItem("Physics","Optics",15),StudyPlanItem("Chemistry","Atoms",20)),1),emptyList(),40,60)
        assertEquals("Optics", r.nextItem?.topic); assertEquals(20,r.remainingGoalMinutes); assertTrue(r.detail.contains("2 blocks remain"))
    }

    @Test fun completedDailyGoalDoesNotInventAnotherPlanBlock() {
        val r = TodayRecommendationCalculator.calculate(null,emptyList(),60,60)
        assertTrue(r.isGoalComplete); assertNull(r.nextItem); assertFalse(r.shouldOpenFocus); assertEquals("Plan Next Session",r.actionLabel)
    }

    @Test fun completedOrDraftPlanIsNotRecommended() {
        val items=listOf(StudyPlanItem("Biology","Cells",25))
        assertFalse(TodayRecommendationCalculator.calculate(plan(items).copy(isCompleted=true),emptyList(),0,60).shouldOpenFocus)
        assertFalse(TodayRecommendationCalculator.calculate(plan(items).copy(isDraft=true),emptyList(),0,60).shouldOpenFocus)
    }

    @Test fun matchingUrgentExamAddsContextWithoutChangingPlanItem() {
        val r=TodayRecommendationCalculator.calculate(plan(listOf(StudyPlanItem("Physics","Optics",25))),listOf(ExamEntity(subject="Physics",examDate="Tomorrow",daysRemaining=1,priority="High")),0,60)
        assertEquals("Optics",r.nextItem?.topic); assertTrue(r.detail.contains("Exam is due tomorrow"))
    }
}
