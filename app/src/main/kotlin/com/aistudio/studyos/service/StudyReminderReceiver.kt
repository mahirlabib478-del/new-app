package com.aistudio.studyos.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aistudio.studyos.MainActivity
import com.aistudio.studyos.R
import com.aistudio.studyos.StudyApplication
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StudyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!StudyReminderScheduler.isEnabled(context)) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val application = context.applicationContext as StudyApplication
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                val repository = application.repositoryFor(uid)

                // A reminder should only protect the current calendar day's streak.
                // If the user already studied today, do not send a stale/duplicate
                // streak notification.
                val todayMinutes = repository.getTodayMinutesNow()
                if (todayMinutes <= 0) {
                    showNotification(context, repository.getUserProfileSnapshot())
                }
            } catch (_: Exception) {
                // Reminder delivery must never crash the receiver or prevent the
                // next day's reminder from being scheduled.
            } finally {
                StudyReminderScheduler.scheduleNext(context)
                pendingResult.finish()
            }
        }
    }

    private suspend fun showNotification(
        context: Context,
        profile: com.aistudio.studyos.data.local.entity.UserProfileEntity?
    ) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Streak reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Daily reminders to protect your study streak"
                }
            )
        }

        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            val streak = profile?.streakDays?.coerceAtLeast(0) ?: 0
            val title = if (streak > 0) {
                "Keep your $streak-day streak alive 🔥"
            } else {
                "Start your study streak 🔥"
            }
            val text = if (streak > 0) {
                "Study today to protect your streak. Don't let it break!"
            } else {
                "Complete a study session today and start your streak."

            val openIntent = android.app.PendingIntent.getActivity(
                context,
                5202,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID,
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_reminder)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(android.app.Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(openIntent)
                    .build()
            )
        }
    }

    companion object {
        private const val CHANNEL_ID = "streak_reminders"
        private const val NOTIFICATION_ID = 5203
    }
}
