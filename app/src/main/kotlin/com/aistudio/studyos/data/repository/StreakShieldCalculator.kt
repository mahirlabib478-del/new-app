package com.aistudio.studyos.data.repository

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Calendar used by streak logic.
 *
 * Streak calendar days follow the user's device timezone. This keeps the
 * calculation correct for Bangladesh and international users alike.
 *
 * The zone is resolved when the date is calculated, so the app follows the
 * device timezone currently configured by the user.
 */
object StudyStreakClock {
    val zone: ZoneId
        get() = ZoneId.systemDefault()

    fun today(): LocalDate = LocalDate.now(zone)

    fun dateFromTimestamp(timestampMillis: Long): LocalDate =
        dateFromTimestamp(timestampMillis, zone)

    fun dateFromTimestamp(timestampMillis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(timestampMillis).atZone(zone).toLocalDate()
}

/**
 * Resolves whether the streak can survive the gap between the last study day
 * and today. A shield protects exactly one missed calendar day.
 *
 * This calculator is deliberately side-effect free so its rules can be tested
 * independently from Room and SharedPreferences.
 */
data class StreakShieldResolution(
    val streakDays: Int,
    val lastActiveDate: String,
    val shieldsToConsume: Int,
    val missedDays: Int
)

object StreakShieldCalculator {
    fun resolve(
        streakDays: Int,
        lastActiveDate: String,
        today: LocalDate,
        availableShields: Int
    ): StreakShieldResolution {
        val safeStreak = streakDays.coerceAtLeast(0)
        val safeShields = availableShields.coerceIn(0, 2)

        if (safeStreak == 0 || lastActiveDate.isBlank()) {
            return StreakShieldResolution(safeStreak, lastActiveDate, 0, 0)
        }

        val lastDate = runCatching { LocalDate.parse(lastActiveDate) }.getOrNull()
            ?: return StreakShieldResolution(0, lastActiveDate, 0, 0)

        val daysSinceLastStudy = ChronoUnit.DAYS.between(lastDate, today)
        // Today or yesterday is already a valid continuous streak.
        if (daysSinceLastStudy <= 1L) {
            // A future stored date can happen after a device timezone/clock change.
            // Normalize it to today instead of allowing the next real session to
            // accidentally collapse a valid streak.
            val normalizedLastActiveDate =
                if (daysSinceLastStudy < 0L) today.toString() else lastActiveDate
            return StreakShieldResolution(safeStreak, normalizedLastActiveDate, 0, 0)
        }

        val missedDays = (daysSinceLastStudy - 1L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        // All-or-nothing: preserve the inventory unless every missed day can be
        // covered. This avoids silently spending a shield on a streak that breaks.
        if (safeShields < missedDays) {
            return StreakShieldResolution(0, lastActiveDate, 0, missedDays)
        }

        return StreakShieldResolution(
            streakDays = safeStreak,
            lastActiveDate = today.minusDays(1).toString(),
            shieldsToConsume = missedDays,
            missedDays = missedDays
        )
    }
}
