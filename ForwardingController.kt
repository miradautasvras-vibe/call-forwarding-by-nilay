package com.nilay.callforward

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.quicksettings.TileService
import android.widget.Toast

object ForwardingController {

    fun turnOn(context: Context, slotIndex: Int? = null): Result<String> {
        val appContext = context.applicationContext
        val state = PrefsStore.loadState(appContext)
        val targetSlot = slotIndex ?: state.activeSlotIndex
        val preset = state.presets.getOrElse(targetSlot) { state.activePreset }

        if (preset.number.isBlank()) {
            Toast.makeText(
                appContext,
                "Please save a receptionist number first.",
                Toast.LENGTH_SHORT
            ).show()
            openAppSafe(appContext)
            return Result.failure(IllegalStateException("No receptionist number configured"))
        }

        if (!MmiDialer.hasCallPermission(appContext)) {
            Toast.makeText(
                appContext,
                "Open Call Forward to allow Phone permission.",
                Toast.LENGTH_SHORT
            ).show()
            openAppSafe(appContext)
            return Result.failure(SecurityException("Missing CALL_PHONE permission"))
        }

        val code = state.codes.activateTemplate.replace("{number}", preset.number)
        val dialResult = MmiDialer.dialMmiCode(appContext, code, state.selectedSimAccountId)
        return dialResult.fold(
            onSuccess = {
                PrefsStore.setForwardingStatus(appContext, isOn = true, activeSlotIndex = targetSlot)
                NotificationHelper.cancelReminderNotifications(appContext)
                NotificationHelper.showActiveOngoingNotification(appContext, preset)
                refreshAllSurfaces(appContext)
                Result.success("Forwarding ON → ${preset.name}")
            },
            onFailure = { error ->
                Toast.makeText(
                    appContext,
                    "Could not dial code: ${error.localizedMessage ?: "error"}",
                    Toast.LENGTH_SHORT
                ).show()
                Result.failure(error)
            }
        )
    }

    fun turnOff(context: Context): Result<String> {
        val appContext = context.applicationContext
        val state = PrefsStore.loadState(appContext)

        if (!MmiDialer.hasCallPermission(appContext)) {
            Toast.makeText(
                appContext,
                "Open Call Forward to allow Phone permission.",
                Toast.LENGTH_SHORT
            ).show()
            openAppSafe(appContext)
            return Result.failure(SecurityException("Missing CALL_PHONE permission"))
        }

        val dialResult = MmiDialer.dialMmiCode(
            appContext,
            state.codes.deactivateCode,
            state.selectedSimAccountId
        )
        return dialResult.fold(
            onSuccess = {
                PrefsStore.setForwardingStatus(appContext, isOn = false)
                NotificationHelper.cancelActiveOngoingNotification(appContext)
                NotificationHelper.cancelReminderNotifications(appContext)
                refreshAllSurfaces(appContext)
                Result.success("All call forwarding cleared")
            },
            onFailure = { error ->
                Toast.makeText(
                    appContext,
                    "Could not dial cancel code: ${error.localizedMessage ?: "error"}",
                    Toast.LENGTH_SHORT
                ).show()
                Result.failure(error)
            }
        )
    }

    fun toggle(context: Context): Result<String> {
        val state = PrefsStore.loadState(context.applicationContext)
        return if (state.isForwardingOn) {
            turnOff(context)
        } else {
            turnOn(context)
        }
    }

    fun verifyCode(context: Context, code: String): Result<Unit> {
        val appContext = context.applicationContext
        val state = PrefsStore.loadState(appContext)
        if (!MmiDialer.hasCallPermission(appContext)) {
            Toast.makeText(appContext, "Please grant Phone permission first.", Toast.LENGTH_SHORT).show()
            return Result.failure(SecurityException("Missing CALL_PHONE permission"))
        }
        return MmiDialer.dialMmiCode(appContext, code, state.selectedSimAccountId)
    }

    fun refreshAllSurfaces(context: Context) {
        val appContext = context.applicationContext
        CallForwardWidgetProvider.updateAllWidgets(appContext)
        try {
            TileService.requestListeningState(
                appContext,
                ComponentName(appContext, CallForwardTileService::class.java)
            )
        } catch (_: Exception) {
            // Safe no-op if tile hasn't been added yet
        }
    }

    private fun openAppSafe(context: Context) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Ignore if background activity launch is blocked by OS
        }
    }
}
