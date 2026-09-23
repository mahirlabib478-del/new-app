package com.aistudio.studyos.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aistudio.studyos.MainActivity
import com.aistudio.studyos.R
import com.aistudio.studyos.StudyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StudyTimerForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var expiryJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val endAtWallClockMillis = intent?.getLongExtra(EXTRA_END_AT_WALL_CLOCK, 0L) ?: 0L
        val planId = intent?.getLongExtra(EXTRA_PLAN_ID, 0L) ?: 0L
        val isBreak = intent?.getBooleanExtra(EXTRA_IS_BREAK, false) ?: false
        val subject = intent?.getStringExtra(EXTRA_SUBJECT).orEmpty().ifBlank { "Study Session" }

        val notification = buildNotification(
            endAtWallClockMillis = endAtWallClockMillis,
            isBreak = isBreak,
            subject = subject
        )

        // Always satisfy the startForeground contract first to prevent ForegroundServiceDidNotStartInTimeException
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (intent == null || endAtWallClockMillis <= System.currentTimeMillis()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
            return START_NOT_STICKY
        }

        scheduleExpiry(planId, endAtWallClockMillis)
        return START_REDELIVER_INTENT
    }

    private fun scheduleExpiry(planId: Long, endAtWallClockMillis: Long) {
        expiryJob?.cancel()
        expiryJob = serviceScope.launch {
            val waitMillis = (endAtWallClockMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            delay(waitMillis)
            if (planId > 0L) {
                StudyApplication.instance.repository.expireRunningPlanIfNeeded(
                    planId = planId,
                    expectedEndAtWallClockMillis = endAtWallClockMillis
                )
            }
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        expiryJob?.cancel()
        serviceScope.coroutineContext[Job]?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    private fun buildNotification(
        endAtWallClockMillis: Long,
        isBreak: Boolean,
        subject: String
    ): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            1001,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(if (isBreak) "Study break" else "Study session running")
            .setContentText(subject)
            .setSubText(if (isBreak) "Rest • hydrate • reset" else "Stay focused")
            .setWhen(endAtWallClockMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setShowWhen(true)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(openIntent)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Study session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing Pomodoro study session timer"
                setShowBadge(false)
            }
        )
    }

    companion object {
        private const val CHANNEL_ID = "study_session_timer"
        private const val NOTIFICATION_ID = 4101
        private const val EXTRA_END_AT_WALL_CLOCK = "end_at_wall_clock"
        private const val EXTRA_PLAN_ID = "plan_id"
        private const val EXTRA_IS_BREAK = "is_break"
        private const val EXTRA_SUBJECT = "subject"

        fun start(
            context: Context,
            endAtWallClockMillis: Long,
            planId: Long,
            isBreak: Boolean,
            subject: String
        ) {
            val intent = Intent(context, StudyTimerForegroundService::class.java).apply {
                putExtra(EXTRA_END_AT_WALL_CLOCK, endAtWallClockMillis)
                putExtra(EXTRA_PLAN_ID, planId)
                putExtra(EXTRA_IS_BREAK, isBreak)
                putExtra(EXTRA_SUBJECT, subject)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, StudyTimerForegroundService::class.java))
        }
    }
}
