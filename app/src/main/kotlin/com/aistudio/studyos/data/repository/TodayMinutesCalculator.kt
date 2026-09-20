package com.aistudio.studyos.data.repository

import java.util.Calendar

data class LocalDayRange(
    val startMillis: Long,
    val endMillis: Long
)

object TodayMinutesCalculator {
    fun currentLocalDayRange(now: Calendar = Calendar.getInstance()): LocalDayRange {
        val start = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return LocalDayRange(start.timeInMillis, end.timeInMillis)
    }

    fun sumForRange(
        durations: List<Pair<Long, Int>>,
        range: LocalDayRange
    ): Int = durations
        .filter { (timestamp, _) -> timestamp >= range.startMillis && timestamp < range.endMillis }
        .sumOf { (_, minutes) -> minutes.coerceAtLeast(0) }
}
