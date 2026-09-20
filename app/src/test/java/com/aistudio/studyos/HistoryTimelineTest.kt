package com.aistudio.studyos

import com.aistudio.studyos.data.local.entity.SessionLogEntity
import com.aistudio.studyos.ui.screens.buildHistoryDays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class HistoryTimelineTest {
    private fun log(id: Long, timestamp: Long, minutes: Int) = SessionLogEntity(
        id = id,
        subject = "Subject $id",
        chapter = "Topic",
        durationMinutes = minutes,
        mode = "regular",
        xpEarned = minutes,
        timestamp = timestamp
    )

    @Test
    fun emptyHistoryReturnsNoDays() {
        assertTrue(buildHistoryDays(emptyList(), Locale.US).isEmpty())
    }

    @Test
    fun historyGroupsSessionsByDayAndSumsMinutes() {
        val day = 86_400_000L
        val logs = listOf(
            log(1, 3 * day + 10_000, 25),
            log(2, 3 * day + 20_000, 35),
            log(3, 2 * day + 10_000, 40)
        )

        val days = buildHistoryDays(logs, Locale.US)

        assertEquals(2, days.size)
        assertEquals(60, days[0].minutes)
        assertEquals(2, days[0].sessions)
        assertEquals(40, days[1].minutes)
        assertEquals(1, days[1].sessions)
    }

    @Test
    fun sessionsInsideEachDayStayNewestFirst() {
        val day = 86_400_000L
        val logs = listOf(
            log(1, day + 10_000, 10),
            log(2, day + 30_000, 20),
            log(3, day + 20_000, 15)
        )

        val days = buildHistoryDays(logs, Locale.US)

        assertEquals(listOf(2L, 3L, 1L), days.single().logs.map { it.id })
    }
}
