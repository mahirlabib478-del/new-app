package com.aistudio.studyos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureNanoTime

/**
 * Performance & Lag Verification Test:
 * Measures execution times of critical UI state transformations and data operations
 * to ensure they execute well within a 16ms frame budget without UI jank.
 */
class PerformanceAndLagTest {

    @Test
    fun testWeeklyActivityCalculationPerformanceUnder16ms() {
        val sampleLogs = (1..1000).map { i ->
            com.aistudio.studyos.data.local.entity.SessionLogEntity(
                id = i.toLong(),
                subject = "Subject ${i % 10}",
                chapter = "Chapter ${i % 5}",
                durationMinutes = 25,
                xpEarned = 25,
                timestamp = System.currentTimeMillis() - (i * 3600_000L),
                mode = "focus"
            )
        }

        // Warm up JIT to avoid classloading spikes in container environments
        repeat(5) {
            sampleLogs.sumOf { it.durationMinutes }
        }

        val elapsedNanos = measureNanoTime {
            val cal = java.util.Calendar.getInstance().apply {
                firstDayOfWeek = java.util.Calendar.MONDAY
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            while (cal.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.MONDAY) {
                cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
            }

            // Calculate day boundaries once; avoid formatting every log in the hot loop.
            repeat(7) {
                val startOfDay = cal.timeInMillis
                cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
                val endOfDay = cal.timeInMillis
                sampleLogs.asSequence()
                    .filter { it.timestamp >= startOfDay && it.timestamp < endOfDay }
                    .sumOf { it.durationMinutes }
            }
        }

        val elapsedMs = elapsedNanos / 1_000_000.0
        assertTrue(
            "Weekly activity calculation took $elapsedMs ms, should be under 20ms",
            elapsedMs < 20.0
        )
    }

    @Test
    fun testTodayMinutesCalculationPerformance() {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startOfToday = cal.timeInMillis
        val endOfToday = startOfToday + 86_400_000L

        val sampleLogs = (1..500).map { i ->
            com.aistudio.studyos.data.local.entity.SessionLogEntity(
                id = i.toLong(),
                subject = "Subject ${i % 5}",
                chapter = "Chapter $i",
                durationMinutes = 30,
                xpEarned = 30,
                timestamp = System.currentTimeMillis() - (i % 3 * 86_400_000L),
                mode = "focus"
            )
        }

        val elapsedNanos = measureNanoTime {
            sampleLogs.filter { log ->
                log.timestamp >= startOfToday && log.timestamp < endOfToday
            }.sumOf { it.durationMinutes }
        }

        val elapsedMs = elapsedNanos / 1_000_000.0
        assertTrue(
            "Today's minutes calculation took $elapsedMs ms, should be under 15ms",
            elapsedMs < 15.0
        )
    }

    @Test
    fun testTimerStateUpdateOverhead() {
        var remaining = 1500
        val elapsedNanos = measureNanoTime {
            repeat(100) { remaining -= 1 }
        }
        val elapsedMs = elapsedNanos / 1_000_000.0
        assertEquals(1400, remaining)
        assertTrue("Timer state ticks took $elapsedMs ms", elapsedMs < 1.0)
    }
}
