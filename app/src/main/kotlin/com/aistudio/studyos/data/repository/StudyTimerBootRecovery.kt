package com.aistudio.studyos.data.repository

import com.aistudio.studyos.data.local.entity.StudyPlanEntity

object StudyTimerBootRecovery {
    fun shouldResume(plan: StudyPlanEntity?, nowMillis: Long): Boolean {
        return plan != null &&
            plan.isTimerRunning &&
            plan.endAtWallClockMillis > nowMillis
    }
}
