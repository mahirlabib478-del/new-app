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

    // The requested anchor points are kept explicit; intermediate levels are
    // smoothly interpolated so progression does not jump abruptly.
    private val anchors = listOf(
        Anchor(1, 150, 60, 1, 30, 50),
        Anchor(10, 1800, 800, 4, 90, 500),
        Anchor(20, 3200, 1600, 8, 120, 1000),
        Anchor(30, 4800, 3000, 12, 150, 1800),
        Anchor(40, 6800, 4500, 17, 180, 2600),
        Anchor(50, 10000, 7000, 23, 210, 4000),
        Anchor(60, 14000, 9500, 28, 240, 5200),
        Anchor(70, 18000, 12000, 32, 255, 6500),
        Anchor(80, 22000, 14500, 36, 270, 7600),
        Anchor(90, 26000, 17000, 38, 285, 8800),
        Anchor(100, 30000, 20000, 40, 300, 10000)
    )

    fun targetsForLevel(level: Int): LevelMissionTargets {
        val safeLevel = level.coerceIn(1, 100)
        val lower = anchors.lastOrNull { it.level <= safeLevel } ?: anchors.first()
        val upper = anchors.firstOrNull { it.level >= safeLevel } ?: anchors.last()

        if (lower.level == upper.level) {
            return LevelMissionTargets(
                safeLevel, lower.xp, lower.minutes, lower.topics, lower.peak, lower.spent
            )
        }

        val fraction = (safeLevel - lower.level).toDouble() / (upper.level - lower.level).toDouble()
        fun lerp(a: Int, b: Int): Int = (a + (b - a) * fraction).roundToInt()

        return LevelMissionTargets(
            level = safeLevel,
            xpRequired = lerp(lower.xp, upper.xp),
            studyMinutesRequired = lerp(lower.minutes, upper.minutes),
            topicCountRequired = lerp(lower.topics, upper.topics),
            peakFocusMinutesRequired = lerp(lower.peak, upper.peak),
            xpSpentRequired = lerp(lower.spent, upper.spent)
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
