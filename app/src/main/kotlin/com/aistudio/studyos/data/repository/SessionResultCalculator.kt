package com.aistudio.studyos.data.repository

/**
 * Single source of truth for study-time billing.
 *
 * Less than 30 seconds is not billable. Otherwise a partial minute is rounded
 * up exactly once, and all UI/database XP calculations use this same rule.
 */
object SessionResultCalculator {
    fun billableMinutes(seconds: Int): Int {
        val safeSeconds = seconds.coerceAtLeast(0)
        return if (safeSeconds < 30) 0 else (safeSeconds + 59) / 60
    }

    fun xpForMinutes(minutes: Int): Int = minutes.coerceAtLeast(0) * 3
}
