package com.aistudio.studyos.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakShieldCalculatorTest {
    private val today = LocalDate.parse("2026-10-04")

    @Test
    fun noGapDoesNotConsumeShield() {
        val result = StreakShieldCalculator.resolve(12, "2026-10-03", today, 2)
        assertEquals(12, result.streakDays)
        assertEquals(0, result.shieldsToConsume)
        assertEquals("2026-10-03", result.lastActiveDate)
    }

    @Test
    fun oneMissedDayConsumesOneShield() {
        val result = StreakShieldCalculator.resolve(12, "2026-10-02", today, 2)
        assertEquals(12, result.streakDays)
        assertEquals(1, result.shieldsToConsume)
        assertEquals("2026-10-03", result.lastActiveDate)
    }

    @Test
    fun twoMissedDaysConsumeTwoShields() {
        val result = StreakShieldCalculator.resolve(12, "2026-10-01", today, 2)
        assertEquals(12, result.streakDays)
        assertEquals(2, result.shieldsToConsume)
        assertEquals("2026-10-03", result.lastActiveDate)
    }

    @Test
    fun insufficientShieldsBreakStreakWithoutSpendingAny() {
        val result = StreakShieldCalculator.resolve(12, "2026-10-01", today, 1)
        assertEquals(0, result.streakDays)
        assertEquals(0, result.shieldsToConsume)
        assertEquals("2026-10-01", result.lastActiveDate)
        assertEquals(2, result.missedDays)
    }

    @Test
    fun threeMissedDaysCannotBeCoveredByTwoShields() {
        val result = StreakShieldCalculator.resolve(12, "2026-09-30", today, 2)
        assertEquals(0, result.streakDays)
        assertEquals(0, result.shieldsToConsume)
        assertEquals(3, result.missedDays)
    }

    @Test
    fun invalidStoredDateBreaksStreakWithoutSpendingShields() {
        val result = StreakShieldCalculator.resolve(12, "not-a-date", today, 2)
        assertEquals(0, result.streakDays)
        assertEquals(0, result.shieldsToConsume)
        assertEquals("not-a-date", result.lastActiveDate)
    }

    @Test
    fun timestampUsesExplicitUserTimezone() {
        val timestamp = java.time.Instant.parse("2026-10-04T23:30:00Z").toEpochMilli()

        assertEquals(
            LocalDate.parse("2026-10-04"),
            StudyStreakClock.dateFromTimestamp(timestamp, java.time.ZoneId.of("America/New_York"))
        )
        assertEquals(
            LocalDate.parse("2026-10-05"),
            StudyStreakClock.dateFromTimestamp(timestamp, java.time.ZoneId.of("Asia/Dhaka"))
        )
    }

    @Test
    fun todayUsesConfiguredDeviceTimezoneInsteadOfDhakaConstant() {
        val original = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/New_York"))
            assertEquals(
                LocalDate.now(java.time.ZoneId.of("America/New_York")),
                StudyStreakClock.today()
            )
        } finally {
            java.util.TimeZone.setDefault(original)
        }
    }

}

