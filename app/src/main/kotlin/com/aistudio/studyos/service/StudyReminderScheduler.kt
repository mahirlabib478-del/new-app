package com.aistudio.studyos.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object StudyReminderScheduler {
    private const val PREFS = "study_reminder"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_HOUR = "hour"
    private const val KEY_MINUTE = "minute"
    private const val REQUEST_CODE = 5201

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun getHour(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_HOUR, 19)

    fun getMinute(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_MINUTE, 0)

    fun setReminder(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .apply()
        if (enabled) scheduleNext(context) else cancel(context)
    }

    fun scheduleNext(context: Context) {
        if (!isEnabled(context)) return
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val trigger = nextTriggerMillis(getHour(context), getMinute(context))
        val pendingIntent = pendingIntent(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            // Keep the reminder alive even before exact-alarm access is granted.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
            }
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // User-visible alarm; reliable while the device is idle/dozing.
            val alarmClockInfo = AlarmManager.AlarmClockInfo(trigger, pendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, trigger, pendingIntent)
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    fun rescheduleAfterPermissionGrant(context: Context) {
        if (isEnabled(context)) scheduleNext(context)
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, StudyReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val now = java.util.Calendar.getInstance()
        val next = now.clone() as java.util.Calendar
        next.set(java.util.Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        next.set(java.util.Calendar.MINUTE, minute.coerceIn(0, 59))
        next.set(java.util.Calendar.SECOND, 0)
        next.set(java.util.Calendar.MILLISECOND, 0)
        if (next.timeInMillis <= now.timeInMillis) {
            next.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis
    }
}
