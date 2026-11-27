package com.nilay.callforward

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val state = PrefsStore.loadState(appContext)

        when (intent.action) {
            ACTION_NIGHT_REMINDER -> {
                if (state.isForwardingOn) {
                    NotificationHelper.showNightReminderNotification(appContext, state.activePreset)
                }
                ReminderScheduler.syncAlarms(appContext)
            }

            ACTION_MORNING_REMINDER -> {
                if (!state.isForwardingOn) {
                    NotificationHelper.showMorningReminderNotification(appContext, state.activePreset)
                }
                ReminderScheduler.syncAlarms(appContext)
            }

            ACTION_NOTIF_TURN_OFF -> {
                ForwardingController.turnOff(appContext)
            }

            ACTION_NOTIF_TURN_ON -> {
                ForwardingController.turnOn(appContext)
            }
        }
    }

    companion object {
        const val ACTION_NIGHT_REMINDER = "com.nilay.callforward.ACTION_NIGHT_REMINDER"
        const val ACTION_MORNING_REMINDER = "com.nilay.callforward.ACTION_MORNING_REMINDER"
        const val ACTION_NOTIF_TURN_OFF = "com.nilay.callforward.ACTION_NOTIF_TURN_OFF"
        const val ACTION_NOTIF_TURN_ON = "com.nilay.callforward.ACTION_NOTIF_TURN_ON"
    }
}
