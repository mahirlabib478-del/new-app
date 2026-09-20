package com.aistudio.studyos

import com.aistudio.studyos.data.repository.GamificationCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GamificationCalculatorTest {
    @Test fun achievementsAreDeterministicAndOrdered() {
        val a = GamificationCalculator.achievements(300, 3, 2)
        assertEquals(6, a.size)
        assertTrue(a[0].unlocked)
        assertTrue(a[1].unlocked)
        assertTrue(a[2].unlocked)
        assertTrue(a[4].unlocked)
        assertFalse(a[3].unlocked)
        assertFalse(a[5].unlocked)
    }

    @Test fun emptyProgressUnlocksNothing() {
        assertTrue(GamificationCalculator.achievements(0, 0, 0).none { it.unlocked })
    }
}
