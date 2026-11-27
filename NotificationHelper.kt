package com.nilay.callforward

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotificationHelper {
    private const val CHANNEL_ACTIVE_ID = "channel_active_forwarding"
    private const val CHANNEL_REMINDER_ID = "channel_daily_reminders"

    const val NOTIF_ID_ACTIVE = 1001
    const val NOTIF_ID_NIGHT_REMINDER = 1002
    const val NOTIF_ID_MORNING_REMINDER = 1003

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val activeChannel = NotificationChannel(
            CHANNEL_ACTIVE_ID,
            "Active Forwarding Status",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows a quiet reminder while clinic call forwarding is active."
            setShowBadge(false)
        }

        val reminderChannel = NotificationChannel(
            CHANNEL_REMINDER_ID,
            "Morning & Night Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts you at night if forwarding is still ON, or in the morning if still OFF."
        }

        manager.createNotificationChannel(activeChannel)
        manager.createNotificationChannel(reminderChannel)
    }

    private fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            10,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun turnOffPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_NOTIF_TURN_OFF
        }
        return PendingIntent.getBroadcast(
            context,
            11,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun turnOnPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_NOTIF_TURN_ON
        }
        return PendingIntent.getBroadcast(
            context,
            12,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun showActiveOngoingNotification(context: Context, preset: ReceptionistPreset) {
        if (!canPostNotifications(context)) return
        ensureChannels(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ACTIVE_ID)
            .setSmallIcon(R.drawable.ic_qs_tile)
            .setContentTitle("Call forwarding ON · ${preset.name}")
            .setContentText("Missed, canceled & unreachable calls → ${preset.number}")
            .setOngoing(true)
            .setSilent(true)
            .setColor(0xFF059669.toInt())
            .setContentIntent(openAppPendingIntent(context))
            .addAction(0, "Turn OFF Now", turnOffPendingIntent(context))
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_ACTIVE, notification)
    }

    fun cancelActiveOngoingNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID_ACTIVE)
    }

    @SuppressLint("MissingPermission")
    fun showNightReminderNotification(context: Context, preset: ReceptionistPreset) {
        if (!canPostNotifications(context)) return
        ensureChannels(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER_ID)
            .setSmallIcon(R.drawable.ic_qs_tile)
            .setContentTitle("Clinic forwarding is still ON")
            .setContentText("Calls are still forwarding to ${preset.name}. Tap below to turn OFF for the night.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setColor(0xFF059669.toInt())
            .setContentIntent(openAppPendingIntent(context))
            .addAction(0, "Turn OFF Now", turnOffPendingIntent(context))
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_NIGHT_REMINDER, notification)
    }

    @SuppressLint("MissingPermission")
    fun showMorningReminderNotification(context: Context, preset: ReceptionistPreset) {
        if (!canPostNotifications(context)) return
        ensureChannels(context)

        val label = if (preset.number.isNotBlank()) {
            "Tap below to forward missed/canceled calls to ${preset.name}."
        } else {
            "Open the app to set a receptionist number and start forwarding."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDER_ID)
            .setSmallIcon(R.drawable.ic_qs_tile)
            .setContentTitle("Clinic forwarding is still OFF")
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setColor(0xFF059669.toInt())
            .setContentIntent(openAppPendingIntent(context))

        if (preset.number.isNotBlank()) {
            builder.addAction(0, "Turn ON Now", turnOnPendingIntent(context))
        }

        NotificationManagerCompat.from(context).notify(NOTIF_ID_MORNING_REMINDER, builder.build())
    }

    fun cancelReminderNotifications(context: Context) {
        val nm = NotificationManagerCompat.from(context)
        nm.cancel(NOTIF_ID_NIGHT_REMINDER)
        nm.cancel(NOTIF_ID_MORNING_REMINDER)
    }
}
