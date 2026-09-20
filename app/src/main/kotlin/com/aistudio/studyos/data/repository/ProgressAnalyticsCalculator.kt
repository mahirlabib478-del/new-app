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
    val averageMinutesOnStudyDays: Int
)

object ProgressAnalyticsCalculator {
    fun calculate(
        logs: List<SessionLogEntity>,
        activePlan: StudyPlanEntity?,
        nowMillis: Long = System.currentTimeMillis()
    ): ProgressAnalyticsSummary {
        val actual = logs.sumOf { it.durationMinutes.coerceAtLeast(0) }
        val planned = activePlan?.totalDurationMinutes?.coerceAtLeast(0) ?: 0
        val planCompleted = activePlan?.accumulatedBillableMinutes?.coerceAtLeast(0) ?: 0
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
        val dayMinutes = days.map { start ->
            val end = Calendar.getInstance().apply { timeInMillis = start.timeInMillis; add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
            logs.filter { it.timestamp in start.timeInMillis until end }.sumOf { it.durationMinutes.coerceAtLeast(0) }
        }
        val studyDays = dayMinutes.count { it > 0 }
        val avg = if (studyDays > 0) dayMinutes.filter { it > 0 }.average().toInt() else 0

        return ProgressAnalyticsSummary(
            plannedMinutes = planned,
            actualMinutes = planCompleted,
            planCompletionPercent = planPercent,
            activePlanTitle = activePlan?.title,
            activePlanRemainingMinutes = (planned - planCompleted).coerceAtLeast(0),
            consistencyDays = studyDays,
            averageMinutesOnStudyDays = avg
        )
    }
}
