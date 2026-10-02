package com.aistudio.studyos.data.repository

import com.aistudio.studyos.data.local.entity.SessionLogEntity
import com.aistudio.studyos.data.local.entity.StudyPlanEntity
import java.util.Calendar

data class ProgressAnalyticsSummary(
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val planCompletionPercent: Int,
    val activePlanTitle: String?,
    val activePlanRemainingMinutes: Int,
    val consistencyDays: Int,
    val consistencyTargetDays: Int = 7,
    val consistencyPercent: Int,
    val weeklyStudyMinutes: Int,
    val averageMinutesOnStudyDays: Int
)

object ProgressAnalyticsCalculator {
    fun calculate(
        logs: List<SessionLogEntity>,
        activePlan: StudyPlanEntity?,
        nowMillis: Long = System.currentTimeMillis(),
        latestCompletedPlan: StudyPlanEntity? = null
    ): ProgressAnalyticsSummary {
        // Prefer the currently active plan. Once it is completed, fall back to
        // the latest completed plan so Progress does not lose the finished plan.
        val progressPlan = activePlan ?: latestCompletedPlan
        val planned = progressPlan?.totalDurationMinutes?.coerceAtLeast(0) ?: 0
        val planCompleted = progressPlan?.accumulatedBillableMinutes?.coerceAtLeast(0) ?: 0
        val planPercent = if (planned > 0) ((planCompleted * 100L) / planned).toInt().coerceIn(0, 100) else 0

        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val days = (0 until 7).map { offset ->
            Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                add(Calendar.DAY_OF_YEAR, -offset)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
        }
        // Build the seven local-calendar-day ranges once, then aggregate each log
        // in a single pass instead of rescanning the full log list for every day.
        val dayRanges = days.map { start ->
            val dayEnd = Calendar.getInstance().apply {
                timeInMillis = start.timeInMillis
                add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis
            // Include completed days fully, but never count sessions after "now"
            // on the current day (for example, imported future-dated logs).
            start.timeInMillis to minOf(dayEnd, nowMillis + 1L)
        }
        val dayMinutes = LongArray(7)
        logs.forEach { log ->
            val minutes = log.durationMinutes.coerceAtLeast(0)
            if (minutes > 0) {
                val dayIndex = dayRanges.indexOfFirst { (start, endExclusive) ->
                    log.timestamp >= start && log.timestamp < endExclusive
                }
                if (dayIndex >= 0) dayMinutes[dayIndex] =
                    (dayMinutes[dayIndex] + minutes.toLong()).coerceAtMost(Int.MAX_VALUE.toLong())
            }
        }
        val studyDays = dayMinutes.count { it > 0 }
        val avg = if (studyDays > 0) {
            (dayMinutes.filter { it > 0 }.sum() / studyDays).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        } else 0

        return ProgressAnalyticsSummary(
            plannedMinutes = planned,
            actualMinutes = planCompleted,
            planCompletionPercent = planPercent,
            activePlanTitle = progressPlan?.title,
            activePlanRemainingMinutes = (planned - planCompleted).coerceAtLeast(0),
            consistencyDays = studyDays,
            consistencyPercent = (studyDays * 100 / 7).coerceIn(0, 100),
            weeklyStudyMinutes = dayMinutes.sum().coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            averageMinutesOnStudyDays = avg
        )
    }
}
