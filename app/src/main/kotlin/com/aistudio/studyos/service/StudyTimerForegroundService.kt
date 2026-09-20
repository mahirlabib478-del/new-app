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

class StudyTimerForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val endAtWallClockMillis = intent.getLongExtra(EXTRA_END_AT_WALL_CLOCK, 0L)
        val isBreak = intent.getBooleanExtra(EXTRA_IS_BREAK, false)
        val subject = intent.getStringExtra(EXTRA_SUBJECT).orEmpty().ifBlank { "Study Session" }

        if (endAtWallClockMillis <= System.currentTimeMillis()) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification(
            endAtWallClockMillis = endAtWallClockMillis,
            isBreak = isBreak,
            subject = subject
        )

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
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
        private const val EXTRA_IS_BREAK = "is_break"
        private const val EXTRA_SUBJECT = "subject"

        fun start(
            context: Context,
            endAtWallClockMillis: Long,
            isBreak: Boolean,
            subject: String
        ) {
            val intent = Intent(context, StudyTimerForegroundService::class.java).apply {
                putExtra(EXTRA_END_AT_WALL_CLOCK, endAtWallClockMillis)
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
