package com.aistudio.studyos.data.repository

import org.junit.Assert.assertEquals
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
}
