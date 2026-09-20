package com.aistudio.studyos.data.repository

import com.aistudio.studyos.data.local.entity.ExamEntity
import com.aistudio.studyos.data.local.entity.StudyPlanEntity
import com.aistudio.studyos.data.local.entity.StudyPlanItem
import com.aistudio.studyos.data.local.entity.StudyPlanItemCodec

data class TodayRecommendation(
    val title: String,
    val detail: String,
    val actionLabel: String,
    val shouldOpenFocus: Boolean,
    val remainingGoalMinutes: Int,
    val nextItem: StudyPlanItem?,
    val isGoalComplete: Boolean
)

object TodayRecommendationCalculator {
    fun calculate(activePlan: StudyPlanEntity?, upcomingExams: List<ExamEntity>, todayMinutes: Int, dailyGoalMinutes: Int): TodayRecommendation {
        val safeToday = todayMinutes.coerceAtLeast(0)
        val safeGoal = dailyGoalMinutes.coerceAtLeast(0)
        val remainingGoal = (safeGoal - safeToday).coerceAtLeast(0)
        val plan = activePlan?.takeUnless { it.isCompleted || it.isArchived || it.isDraft }
        val items = plan?.let { StudyPlanItemCodec.decode(it.planItems) }.orEmpty()
        val nextIndex = plan?.currentBlockIndex?.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        val nextItem = if (items.isNotEmpty() && nextIndex != null) items.getOrNull(nextIndex) else null
        val hasRunningTimer = plan?.isTimerRunning == true

        if (plan != null && nextItem != null) {
            val exam = upcomingExams.firstOrNull { it -> !it.isCompleted && it.subject.trim().equals(nextItem.subject.trim(), ignoreCase = true) }
            val urgency = when {
                exam == null -> ""
                exam.daysRemaining <= 1 -> " Exam is due " + (if (exam.daysRemaining == 0) "today" else "tomorrow") + "."
                exam.daysRemaining <= 3 -> " Exam is in ${exam.daysRemaining} days."
                else -> ""
            }
            val remainingBlocks = (items.size - (nextIndex ?: 0)).coerceAtLeast(1)
            val budgetText = if (remainingGoal > 0) "$remainingGoal min left in daily goal." else "Daily goal is complete; this continues your saved plan."
            val blockWord = if (remainingBlocks == 1) "block" else "blocks"
            return TodayRecommendation(
                title = if (hasRunningTimer) "Continue your active session" else "Resume your study plan",
                detail = "${nextItem.subject} • ${nextItem.topic} • ${nextItem.minutes} min. $remainingBlocks $blockWord remain. $budgetText$urgency",
                actionLabel = if (hasRunningTimer) "Continue Active Session" else "Resume Study Session",
                shouldOpenFocus = true,
                remainingGoalMinutes = remainingGoal,
                nextItem = nextItem,
                isGoalComplete = safeGoal > 0 && safeToday >= safeGoal
            )
        }

        if (safeGoal > 0 && safeToday >= safeGoal) {
            return TodayRecommendation(
                title = "Daily goal complete",
                detail = "You have finished your ${safeGoal} min daily target. No unfinished saved-plan block is waiting.",
                actionLabel = "Plan Next Session",
                shouldOpenFocus = false,
                remainingGoalMinutes = 0,
                nextItem = null,
                isGoalComplete = true
            )
        }

        val exam = upcomingExams.firstOrNull { !it.isCompleted }
        val examHint = exam?.let {
            val whenText = when { it.daysRemaining <= 0 -> "today"; it.daysRemaining == 1 -> "tomorrow"; else -> "in ${it.daysRemaining} days" }
            " Next exam: ${it.subject} $whenText."
        }.orEmpty()
        return TodayRecommendation(
            title = "Ready for your next session",
            detail = if (remainingGoal > 0) "$remainingGoal min remain in daily goal.$examHint" else "Set a daily goal to get a time-based recommendation.$examHint",
            actionLabel = "Start Study Session",
            shouldOpenFocus = false,
            remainingGoalMinutes = remainingGoal,
            nextItem = null,
            isGoalComplete = false
        )
    }
}
