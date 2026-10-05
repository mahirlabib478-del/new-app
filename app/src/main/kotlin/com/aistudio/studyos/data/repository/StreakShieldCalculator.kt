package com.aistudio.studyos.data.repository

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Calendar used by streak logic.
 *
 * Study OS is currently Bangladesh-focused, so streak calendar days are anchored
 * to Bangladesh time instead of the device's mutable system timezone.
 */
object StudyStreakClock {
    val zone: ZoneId = ZoneId.of("Asia/Dhaka")

    fun today(): LocalDate = LocalDate.now(zone)

    fun dateFromTimestamp(timestampMillis: Long): LocalDate =
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
        // Today or yesterday is already a valid continuous streak. Also avoid
        // penalizing a device clock/time-zone adjustment that puts the date ahead.
        if (daysSinceLastStudy <= 1L) {
            return StreakShieldResolution(safeStreak, lastActiveDate, 0, 0)
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
