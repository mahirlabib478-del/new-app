package com.aistudio.studyos

import com.aistudio.studyos.data.repository.SessionResultCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionResultCalculatorTest {

    @Test
    fun billableMinutesUsesOneCanonicalRoundingRule() {
        assertEquals(0, SessionResultCalculator.billableMinutes(0))
        assertEquals(0, SessionResultCalculator.billableMinutes(29))
        assertEquals(1, SessionResultCalculator.billableMinutes(30))
        assertEquals(1, SessionResultCalculator.billableMinutes(31))
        assertEquals(1, SessionResultCalculator.billableMinutes(60))
        assertEquals(2, SessionResultCalculator.billableMinutes(61))
        assertEquals(2, SessionResultCalculator.billableMinutes(89))
        assertEquals(2, SessionResultCalculator.billableMinutes(120))
    }

    @Test
    fun xpAlwaysMatchesCanonicalMinutes() {
        assertEquals(0, SessionResultCalculator.xpForMinutes(0))
        assertEquals(3, SessionResultCalculator.xpForMinutes(1))
        assertEquals(6, SessionResultCalculator.xpForMinutes(2))
    }
}
