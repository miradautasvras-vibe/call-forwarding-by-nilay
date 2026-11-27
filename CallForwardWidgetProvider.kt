package com.nilay.callforward

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallForwardWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_TOGGLE) {
            ForwardingController.toggle(context)
        }
    }

    companion object {
        const val ACTION_WIDGET_TOGGLE = "com.nilay.callforward.ACTION_WIDGET_TOGGLE"

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, CallForwardWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val state = PrefsStore.loadState(context)
            val preset = state.activePreset
            val isOn = state.isForwardingOn
            val hasNumber = preset.number.isNotBlank()

            val views = RemoteViews(context.packageName, R.layout.widget_call_forward)

            // Visual styling: Rich Emerald Green when ON, Cool Pastel Slate when OFF
            if (isOn) {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_bg_on)
                views.setTextViewText(R.id.widget_title, preset.name)
                views.setTextColor(R.id.widget_title, Color.WHITE)

                views.setTextViewText(R.id.widget_status_pill, "ON")
                views.setTextColor(R.id.widget_status_pill, Color.WHITE)
                views.setInt(R.id.widget_status_pill, "setBackgroundResource", R.drawable.widget_badge_on)

                val sinceText = formatShortTime(state.lastToggledAtMillis)
                val subtitle = if (sinceText != null) "Since $sinceText · Tap to turn OFF" else "Tap to turn OFF"
                views.setTextViewText(R.id.widget_subtitle, subtitle)
                views.setTextColor(R.id.widget_subtitle, Color.parseColor("#D1FAE5"))

                views.setImageViewResource(R.id.widget_settings_btn, R.drawable.ic_widget_settings)
            } else {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_bg_off)
                views.setTextViewText(
                    R.id.widget_title,
                    if (hasNumber) preset.name else "Set Number"
                )
                views.setTextColor(R.id.widget_title, Color.parseColor("#1E293B"))

                views.setTextViewText(R.id.widget_status_pill, "OFF")
                views.setTextColor(R.id.widget_status_pill, Color.parseColor("#334155"))
                views.setInt(R.id.widget_status_pill, "setBackgroundResource", R.drawable.widget_badge_off)

                views.setTextViewText(
                    R.id.widget_subtitle,
                    if (hasNumber) "Tap to start forwarding" else "Tap gear to add receptionists"
                )
                views.setTextColor(R.id.widget_subtitle, Color.parseColor("#475569"))

                views.setImageViewResource(R.id.widget_settings_btn, R.drawable.ic_widget_settings_dark)
            }

            // Main area toggles ON/OFF
            val toggleIntent = Intent(context, CallForwardWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_TOGGLE
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_main_area, togglePendingIntent)

            // Small settings gear opens the app to switch receptionists
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                1000 + appWidgetId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_settings_btn, openAppPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun formatShortTime(millis: Long): String? {
            if (millis <= 0L) return null
            return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
        }
    }
}
