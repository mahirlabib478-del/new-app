package com.aistudio.studyos

import com.aistudio.studyos.data.local.entity.StudyPlanEntity
import com.aistudio.studyos.data.repository.StudyTimerBootRecovery
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyTimerBootRecoveryTest {

    private fun plan(running: Boolean, endAt: Long) = StudyPlanEntity(
        title = "Test",
        subject = "Math",
        chapter = "Chapter 1",
        mode = "focus",
        totalBlocks = 2,
        isTimerRunning = running,
        endAtWallClockMillis = endAt
    )

    @Test
    fun runningFuturePlanResumesAfterReboot() {
        assertTrue(StudyTimerBootRecovery.shouldResume(plan(true, 20_000L), 10_000L))
    }

    @Test
    fun stoppedPlanDoesNotResume() {
        assertFalse(StudyTimerBootRecovery.shouldResume(plan(false, 20_000L), 10_000L))
    }

    @Test
    fun expiredPlanDoesNotResume() {
        assertFalse(StudyTimerBootRecovery.shouldResume(plan(true, 10_000L), 10_000L))
        assertFalse(StudyTimerBootRecovery.shouldResume(plan(true, 9_000L), 10_000L))
    }
}
