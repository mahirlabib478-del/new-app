package com.aistudio.studyos

import com.aistudio.studyos.data.repository.LocalDayRange
import com.aistudio.studyos.data.repository.TodayMinutesCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class TodayMinutesCalculatorTest {

    @Test
    fun todayEngineCountsLogsInsideLocalDayAndExcludesAdjacentDays() {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val range = TodayMinutesCalculator.currentLocalDayRange(today)

        val yesterday = range.startMillis - 1
        val atStart = range.startMillis
        val duringToday = range.startMillis + 2 * 60 * 60 * 1000L
        val atNextDay = range.endMillis

        val minutes = TodayMinutesCalculator.sumForRange(
            listOf(
                yesterday to 50,
                atStart to 1,
                duringToday to 2,
                atNextDay to 99
            ),
            range
        )

        assertEquals(3, minutes)
    }

    @Test
    fun thirtySecondSkipBillableMinuteCanBeCountedToday() {
        val today = Calendar.getInstance()
        val range = TodayMinutesCalculator.currentLocalDayRange(today)
        val logTimestamp = range.startMillis + 30_000L

        val minutes = TodayMinutesCalculator.sumForRange(
            listOf(logTimestamp to 1),
            range
        )

        assertEquals(1, minutes)
    }
}
