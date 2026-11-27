package com.nilay.callforward

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val appContext = context.applicationContext
            ReminderScheduler.syncAlarms(appContext)
            val state = PrefsStore.loadState(appContext)
            if (state.isForwardingOn) {
                NotificationHelper.showActiveOngoingNotification(appContext, state.activePreset)
            }
            ForwardingController.refreshAllSurfaces(appContext)
        }
    }
}
