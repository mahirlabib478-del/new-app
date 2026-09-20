package com.aistudio.studyos.data.repository

/**
 * Pure countdown math used by the production focus timer.
 *
 * The UI ticker is only a renderer; the persisted deadline remains the
 * source of truth. Keeping this calculation pure makes deadline/clock
 * edge cases directly testable without depending on coroutine timing.
 */
object TimerDeadlineCalculator {
    fun remainingSeconds(
        endAtElapsedRealtime: Long,
        endAtWallClockMillis: Long,
        nowElapsedRealtime: Long,
        nowWallClockMillis: Long,
        totalBlockSeconds: Int
    ): Int {
        val total = totalBlockSeconds.coerceAtLeast(0)
        if (total == 0) return 0

        val remainingMillis = when {
            endAtElapsedRealtime > nowElapsedRealtime ->
                endAtElapsedRealtime - nowElapsedRealtime
            endAtWallClockMillis > nowWallClockMillis ->
                endAtWallClockMillis - nowWallClockMillis
            else -> 0L
        }

        return ceilSeconds(remainingMillis).coerceIn(0, total)
    }

    fun deadlineMillis(nowMillis: Long, durationSeconds: Int): Long {
        return nowMillis + durationSeconds.coerceAtLeast(1) * 1000L
    }

    private fun ceilSeconds(milliseconds: Long): Int {
        val safe = milliseconds.coerceAtLeast(0L)
        return ((safe + 999L) / 1000L).toInt()
    }
}
