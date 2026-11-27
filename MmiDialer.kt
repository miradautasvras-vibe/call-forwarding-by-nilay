package com.nilay.callforward

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat

data class SimOption(
    val accountId: String?,
    val label: String
)

object MmiDialer {

    fun hasCallPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasPhoneStatePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun getAvailableSims(context: Context): List<SimOption> {
        val options = mutableListOf(SimOption(null, "Default SIM"))
        if (!hasPhoneStatePermission(context)) return options

        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                ?: return options
            val accounts = telecomManager.callCapablePhoneAccounts
            accounts.forEachIndexed { index, handle ->
                val account = telecomManager.getPhoneAccount(handle)
                val carrierLabel = account?.label?.toString()?.takeIf { it.isNotBlank() } ?: "SIM ${index + 1}"
                options.add(
                    SimOption(
                        accountId = handle.id,
                        label = "SIM ${index + 1} · $carrierLabel"
                    )
                )
            }
        } catch (_: Exception) {
            // Keep default SIM option if OEM restricts account enumeration
        }
        return options
    }

    @SuppressLint("MissingPermission")
    private fun findPhoneAccountHandle(context: Context, accountId: String?): PhoneAccountHandle? {
        if (accountId.isNullOrBlank() || !hasPhoneStatePermission(context)) return null
        return try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            telecomManager?.callCapablePhoneAccounts?.firstOrNull { it.id == accountId }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Percent-encodes every '#' to '%23' so Android treats the string as a valid GSM MMI code
     * rather than stripping characters after the first '#'.
     */
    fun buildMmiUri(rawCode: String): Uri {
        val encoded = rawCode.trim().replace("#", Uri.encode("#"))
        return Uri.parse("tel:$encoded")
    }

    @SuppressLint("MissingPermission")
    fun dialMmiCode(context: Context, rawCode: String, simAccountId: String?): Result<Unit> {
        if (!hasCallPermission(context)) {
            return Result.failure(SecurityException("Phone call permission is required."))
        }

        val uri = buildMmiUri(rawCode)
        val handle = findPhoneAccountHandle(context, simAccountId)

        // Primary path: TelecomManager.placeCall works reliably from Widgets, QS Tiles,
        // and Notification action buttons without triggering background activity restrictions.
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (telecomManager != null) {
                val extras = Bundle()
                if (handle != null) {
                    extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                }
                telecomManager.placeCall(uri, extras)
                return Result.success(Unit)
            }
        } catch (_: Exception) {
            // Fallback to ACTION_CALL below
        }

        return try {
            val intent = Intent(Intent.ACTION_CALL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (handle != null) {
                    putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
                }
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openSystemCallSettings(context: Context) {
        val intents = listOf(
            Intent("android.telecom.action.SHOW_CALL_SETTINGS"),
            Intent(Intent.ACTION_MAIN).apply {
                setClassName("com.android.phone", "com.android.phone.CallFeaturesSetting")
            },
            Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)
        )
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // Try next fallback
            }
        }
    }
}
