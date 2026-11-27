package com.nilay.callforward

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object ReminderScheduler {
    private const val REQ_NIGHT = 2001
    private const val REQ_MORNING = 2002

    fun syncAlarms(context: Context) {
        val state = PrefsStore.loadState(context)
        val r = state.reminders

        if (r.nightEnabled) {
            scheduleDailyAlarm(
                context = context,
                requestCode = REQ_NIGHT,
                action = ReminderReceiver.ACTION_NIGHT_REMINDER,
                hour = r.nightHour,
                minute = r.nightMinute
            )
        } else {
            cancelAlarm(context, REQ_NIGHT, ReminderReceiver.ACTION_NIGHT_REMINDER)
        }

        if (r.morningEnabled) {
            scheduleDailyAlarm(
                context = context,
                requestCode = REQ_MORNING,
                action = ReminderReceiver.ACTION_MORNING_REMINDER,
                hour = r.morningHour,
                minute = r.morningMinute
            )
        } else {
            cancelAlarm(context, REQ_MORNING, ReminderReceiver.ACTION_MORNING_REMINDER)
        }
    }

    private fun scheduleDailyAlarm(
        context: Context,
        requestCode: Int,
        action: String,
        hour: Int,
        minute: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis() + 5_000L) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
        }
    }

    private fun cancelAlarm(context: Context, requestCode: Int, action: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
