package com.aistudio.studyos.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aistudio.studyos.StudyApplication
import com.aistudio.studyos.data.repository.StudyTimerBootRecovery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class StudyTimerBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val plan = StudyApplication.instance.repository.getActivePlan().firstOrNull()
                if (
                    StudyTimerBootRecovery.shouldResume(plan, System.currentTimeMillis())
                ) {
                    StudyTimerForegroundService.start(
                        context = context,
                        endAtWallClockMillis = plan.endAtWallClockMillis,
                        planId = plan.id,
                        isBreak = plan.isBreakPhase,
                        subject = plan.subject
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
