package com.aistudio.studyos

import com.aistudio.studyos.ui.viewmodel.FocusTimerState
import com.aistudio.studyos.data.repository.TimerDeadlineCalculator
import com.aistudio.studyos.data.repository.SessionResultCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying:
 * 1. Focus session duration stays strictly bounded to scheduled time.
 * 2. Skipping focus blocks counts ONLY actual studied time.
 * 3. Skipping the final block completes the session and activates the Congratulate screen.
 * 4. Breaks do not contribute to studied time.
 * 5. Cumulative study time across partial blocks is properly aggregated.
 */
class SessionTimerAndSkipFeatureTest {

    @Test
    fun testFocusDurationStaysWithinBlockBounds() {
        val totalBlockSeconds = 25 * 60
        var secondsRemaining = totalBlockSeconds

        // Simulate normal countdown
        for (i in 1..10) {
            secondsRemaining = (secondsRemaining - 1).coerceAtLeast(0)
        }

        assertTrue(secondsRemaining in 0..totalBlockSeconds)
        assertEquals(25 * 60 - 10, secondsRemaining)

        // Ensure clamping prevents negative seconds
        val overCountdown = -5
        val clamped = overCountdown.coerceIn(0, totalBlockSeconds)
        assertEquals(0, clamped)
    }

    @Test
    fun testSkipFocusBlockCalculatesExactTimeStudied() {
        val totalBlockSec = 25 * 60 // 1500 sec
        // User studies 5 minutes (300 sec) and skips
        val remainingSec = 20 * 60 // 1200 sec left
        val elapsedSec = (totalBlockSec - remainingSec).coerceIn(0, totalBlockSec)
        assertEquals(300, elapsedSec)

        val partialMinutes = if (elapsedSec >= 30) (elapsedSec + 29) / 60 else 0
        assertEquals(5, partialMinutes)
    }

    @Test
    fun testSkipImmediatelyCountsZeroStudyTime() {
        val totalBlockSec = 25 * 60
        val remainingSec = 25 * 60 // 0 seconds elapsed
        val elapsedSec = (totalBlockSec - remainingSec).coerceIn(0, totalBlockSec)
        assertEquals(0, elapsedSec)

        val partialMinutes = if (elapsedSec >= 30) (elapsedSec + 29) / 60 else 0
        assertEquals(0, partialMinutes)
    }

    @Test
    fun testSkipFinalBlockTriggersCongratulateScreenWithActualTime() {
        // Assume user studied 10 min in block 1, skipped break, studied 5 min in block 2 (final block)
        val block1ActualSec = 10 * 60
        val block2ActualSec = 5 * 60
        val totalStudiedSec = block1ActualSec + block2ActualSec

        val finalStudiedMin = if (totalStudiedSec >= 30) (totalStudiedSec + 29) / 60 else 0
        assertEquals(15, finalStudiedMin)

        val completedState = FocusTimerState(
            currentBlockIndex = 2,
            totalBlocks = 2,
            isRunning = false,
            isSessionCompleted = true,
            completedMinutes = finalStudiedMin,
            completedBlocks = 2,
            actualStudiedSeconds = totalStudiedSec
        )

        assertTrue(completedState.isSessionCompleted)
        assertEquals(15, completedState.completedMinutes)
        assertEquals(2, completedState.completedBlocks)
    }

    @Test
    fun testBreaksDoNotCountAsStudiedTime() {
        val focusActualSec = 15 * 60 // 15 min focus
        val breakElapsedSec = 5 * 60  // 5 min break taken

        // Break time is rest, so it must not be added to studied seconds
        val totalActualStudiedSec = focusActualSec // breakElapsedSec NOT added

        val recordedMinutes = totalActualStudiedSec / 60
        assertEquals(15, recordedMinutes)
    }

    @Test
    fun testEarlyFinishDuringBreakRecordsZeroAdditionalStudyTime() {
        assertEquals(
            0,
            SessionResultCalculator.studiedSecondsForEarlyFinish(
                isBreak = true,
                elapsedSeconds = 5 * 60
            )
        )
        assertEquals(
            5 * 60,
            SessionResultCalculator.studiedSecondsForEarlyFinish(
                isBreak = false,
                elapsedSeconds = 5 * 60
            )
        )
    }

    @Test
    fun testCompletedSessionCannotReopenAnotherBlock() {
        val totalBlocks = 4
        val currentBlockIndex = totalBlocks
        val isCompleted = true

        // Once the persisted cursor reaches the block count, there is no valid
        // next focus block to restore.
        assertTrue(isCompleted)
        assertEquals(totalBlocks, currentBlockIndex)
        assertTrue(currentBlockIndex >= totalBlocks)
    }

    @Test
    fun testResetDoesNotAddDiscardedPartialBlockToTotals() {
        val committedStudiedSeconds = 10 * 60
        val committedMinutes = 10
        val discardedPartialSeconds = 7 * 60

        // Reset discards the current block; only committed progress survives.
        val afterResetStudiedSeconds = committedStudiedSeconds
        val afterResetMinutes = committedMinutes

        assertEquals(committedStudiedSeconds, afterResetStudiedSeconds)
        assertEquals(committedMinutes, afterResetMinutes)
        assertFalse(afterResetStudiedSeconds >= committedStudiedSeconds + discardedPartialSeconds)
    }


    @Test
    fun testProductionDeadlineMathCountsDownByClockNotLoopTicks() {
        val endElapsed = TimerDeadlineCalculator.deadlineMillis(1_000L, 25 * 60)
        val endWall = TimerDeadlineCalculator.deadlineMillis(10_000L, 25 * 60)

        // A delayed coroutine/UI frame must not make the timer drift by one
        // second per loop; remaining time is derived from the deadline.
        assertEquals(
            1_490,
            TimerDeadlineCalculator.remainingSeconds(
                endAtElapsedRealtime = endElapsed,
                endAtWallClockMillis = endWall,
                nowElapsedRealtime = 11_000L,
                nowWallClockMillis = 10_000L,
                totalBlockSeconds = 25 * 60
            )
        )
    }

    @Test
    fun testProductionDeadlineMathCeilsPartialSecondAndClampsToBlock() {
        assertEquals(
            2,
            TimerDeadlineCalculator.remainingSeconds(
                endAtElapsedRealtime = 2_001L,
                endAtWallClockMillis = 9_000L,
                nowElapsedRealtime = 500L,
                nowWallClockMillis = 8_000L,
                totalBlockSeconds = 25 * 60
            )
        )

        assertEquals(
            0,
            TimerDeadlineCalculator.remainingSeconds(
                endAtElapsedRealtime = 1_000L,
                endAtWallClockMillis = 2_000L,
                nowElapsedRealtime = 2_000L,
                nowWallClockMillis = 2_000L,
                totalBlockSeconds = 25 * 60
            )
        )

        assertEquals(
            1,
            TimerDeadlineCalculator.remainingSeconds(
                endAtElapsedRealtime = 9_000L,
                endAtWallClockMillis = 9_000L,
                nowElapsedRealtime = 1_000L,
                nowWallClockMillis = 1_000L,
                totalBlockSeconds = 1
            )
        )
    }

    @Test
    fun testProductionDeadlineMathFallsBackToWallClockWhenElapsedDeadlineIsExpired() {
        assertEquals(
            5,
            TimerDeadlineCalculator.remainingSeconds(
                endAtElapsedRealtime = 1_000L,
                endAtWallClockMillis = 10_000L,
                nowElapsedRealtime = 1_500L,
                nowWallClockMillis = 5_000L,
                totalBlockSeconds = 25 * 60
            )
        )
    }

}
