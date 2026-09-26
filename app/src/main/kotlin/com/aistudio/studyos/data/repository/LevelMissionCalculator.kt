package com.aistudio.studyos.data.repository

import com.aistudio.studyos.data.local.entity.UserProfileEntity
import kotlin.math.roundToInt

data class LevelMissionTargets(
    val level: Int,
    val xpRequired: Int,
    val studyMinutesRequired: Int,
    val topicCountRequired: Int,
    val peakFocusMinutesRequired: Int,
    val xpSpentRequired: Int
)

data class LevelMissionProgress(
    val targets: LevelMissionTargets,
    val xpEarned: Int,
    val studyMinutes: Int,
    val topicCount: Int,
    val peakFocusMinutes: Int,
    val xpSpent: Int
) {
    val xpComplete: Boolean get() = xpEarned >= targets.xpRequired
    val studyTimeComplete: Boolean get() = studyMinutes >= targets.studyMinutesRequired
    val topicBreadthComplete: Boolean get() = topicCount >= targets.topicCountRequired
    val peakFocusComplete: Boolean get() = peakFocusMinutes >= targets.peakFocusMinutesRequired
    val shopInvestmentComplete: Boolean get() = xpSpent >= targets.xpSpentRequired
    val completedCount: Int
        get() = listOf(
            xpComplete,
            studyTimeComplete,
            topicBreadthComplete,
            peakFocusComplete,
            shopInvestmentComplete
        ).count { it }
    val allComplete: Boolean get() = completedCount == 5
}

object LevelMissionCalculator {
    private data class Anchor(
        val level: Int,
        val xp: Int,
        val minutes: Int,
        val topics: Int,
        val peak: Int,
        val spent: Int
    )

    // Keep the original progression anchors for XP, study time, peak focus,
    // and shop spending. Topic breadth has its own curve so changing topic
    // progression cannot accidentally change the other four missions.
    private val anchors = listOf(
        Anchor(1, 150, 60, 1, 30, 50),
        Anchor(10, 1800, 800, 3, 90, 500),
        Anchor(25, 4000, 2500, 5, 150, 1500),
        Anchor(50, 10000, 7000, 8, 210, 4000),
        Anchor(100, 30000, 20000, 12, 300, 10000)
    )

    private val topicAnchors = listOf(
        1 to 1,
        10 to 4,
        20 to 8,
        30 to 12,
        40 to 17,
        50 to 23,
        60 to 28,
        70 to 32,
        80 to 36,
        90 to 38,
        100 to 40
    )

    private fun interpolate(lowerLevel: Int, lowerValue: Int, upperLevel: Int, upperValue: Int, level: Int): Int {
        if (lowerLevel == upperLevel) return lowerValue
        val fraction = (level - lowerLevel).toDouble() / (upperLevel - lowerLevel).toDouble()
        return (lowerValue + (upperValue - lowerValue) * fraction).roundToInt()
    }

    fun targetsForLevel(level: Int): LevelMissionTargets {
        val safeLevel = level.coerceIn(1, 100)
        val lower = anchors.lastOrNull { it.level <= safeLevel } ?: anchors.first()
        val upper = anchors.firstOrNull { it.level >= safeLevel } ?: anchors.last()

        val topicLower = topicAnchors.lastOrNull { it.first <= safeLevel } ?: topicAnchors.first()
        val topicUpper = topicAnchors.firstOrNull { it.first >= safeLevel } ?: topicAnchors.last()
        val topicCount = interpolate(
            topicLower.first,
            topicLower.second,
            topicUpper.first,
            topicUpper.second,
            safeLevel
        )

        return LevelMissionTargets(
            level = safeLevel,
            xpRequired = interpolate(lower.level, lower.xp, upper.level, upper.xp, safeLevel),
            studyMinutesRequired = interpolate(lower.level, lower.minutes, upper.level, upper.minutes, safeLevel),
            topicCountRequired = topicCount,
            peakFocusMinutesRequired = interpolate(lower.level, lower.peak, upper.level, upper.peak, safeLevel),
            xpSpentRequired = interpolate(lower.level, lower.spent, upper.level, upper.spent, safeLevel)
        )
    }

    fun calculate(
        level: Int,
        profile: UserProfileEntity,
        topicCount: Int,
        peakFocusMinutes: Int
    ): LevelMissionProgress {
        val targets = targetsForLevel(level)
        // Lifetime earned XP is tracked independently from the spendable XP balance,
        // so shop purchases cannot distort the XP mission.
        return LevelMissionProgress(
            targets = targets,
            xpEarned = profile.totalXpEarned.coerceAtLeast(0),
            studyMinutes = profile.totalStudyMinutes.coerceAtLeast(0),
            topicCount = topicCount.coerceAtLeast(0),
            peakFocusMinutes = peakFocusMinutes.coerceAtLeast(0),
            xpSpent = profile.totalXpSpent.coerceAtLeast(0)
        )
    }

    fun rankTitle(level: Int): String = when {
        level <= 9 -> "Apprentice"
        level <= 24 -> "Scholar"
        level <= 49 -> "Adept"
        level <= 74 -> "Master"
        level <= 99 -> "Sage"
        else -> "Legend"
    }
}
