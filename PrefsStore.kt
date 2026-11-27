package com.nilay.callforward

import android.content.Context
import android.content.SharedPreferences

data class ReceptionistPreset(
    val slotIndex: Int,
    val name: String,
    val number: String
)

data class CarrierCodes(
    val activateTemplate: String = DEFAULT_ACTIVATE,
    val deactivateCode: String = DEFAULT_DEACTIVATE,
    val verifyUnanswered: String = DEFAULT_VERIFY_61,
    val verifyBusy: String = DEFAULT_VERIFY_67,
    val verifyUnreachable: String = DEFAULT_VERIFY_62
) {
    companion object {
        const val DEFAULT_ACTIVATE = "**004*{number}#"
        const val DEFAULT_DEACTIVATE = "##002#"
        const val DEFAULT_VERIFY_61 = "*#61#"
        const val DEFAULT_VERIFY_67 = "*#67#"
        const val DEFAULT_VERIFY_62 = "*#62#"
    }
}

data class ReminderSettings(
    val nightEnabled: Boolean = true,
    val nightHour: Int = 21,
    val nightMinute: Int = 0,
    val morningEnabled: Boolean = false,
    val morningHour: Int = 9,
    val morningMinute: Int = 30
)

data class AppState(
    val presets: List<ReceptionistPreset>,
    val activeSlotIndex: Int,
    val isForwardingOn: Boolean,
    val lastToggledAtMillis: Long,
    val selectedSimAccountId: String?,
    val selectedSimLabel: String,
    val codes: CarrierCodes,
    val reminders: ReminderSettings
) {
    val activePreset: ReceptionistPreset
        get() = presets.getOrElse(activeSlotIndex) { presets.first() }
}

object PrefsStore {
    private const val PREFS_NAME = "call_forward_by_nilay_prefs"

    private const val KEY_ACTIVE_SLOT = "active_slot_index"
    private const val KEY_IS_ON = "is_forwarding_on"
    private const val KEY_LAST_TOGGLED = "last_toggled_millis"
    private const val KEY_SIM_ID = "selected_sim_account_id"
    private const val KEY_SIM_LABEL = "selected_sim_label"

    private const val KEY_CODE_ACTIVATE = "code_activate"
    private const val KEY_CODE_DEACTIVATE = "code_deactivate"
    private const val KEY_CODE_VERIFY_61 = "code_verify_61"
    private const val KEY_CODE_VERIFY_67 = "code_verify_67"
    private const val KEY_CODE_VERIFY_62 = "code_verify_62"

    private const val KEY_NIGHT_ENABLED = "night_reminder_enabled"
    private const val KEY_NIGHT_HOUR = "night_reminder_hour"
    private const val KEY_NIGHT_MINUTE = "night_reminder_minute"
    private const val KEY_MORNING_ENABLED = "morning_reminder_enabled"
    private const val KEY_MORNING_HOUR = "morning_reminder_hour"
    private const val KEY_MORNING_MINUTE = "morning_reminder_minute"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun sanitizePhoneNumber(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.length == 12 && digits.startsWith("91") -> digits.substring(2)
            digits.length == 11 && digits.startsWith("0") -> digits.substring(1)
            else -> digits
        }
    }

    fun loadState(context: Context): AppState {
        val p = prefs(context)
        val presets = (0..2).map { i ->
            val defaultName = "Reception ${i + 1}"
            ReceptionistPreset(
                slotIndex = i,
                name = p.getString("preset_name_$i", defaultName).orEmpty().ifBlank { defaultName },
                number = p.getString("preset_number_$i", "").orEmpty()
            )
        }
        val activeSlot = p.getInt(KEY_ACTIVE_SLOT, 0).coerceIn(0, 2)
        val codes = CarrierCodes(
            activateTemplate = p.getString(KEY_CODE_ACTIVATE, CarrierCodes.DEFAULT_ACTIVATE)
                ?: CarrierCodes.DEFAULT_ACTIVATE,
            deactivateCode = p.getString(KEY_CODE_DEACTIVATE, CarrierCodes.DEFAULT_DEACTIVATE)
                ?: CarrierCodes.DEFAULT_DEACTIVATE,
            verifyUnanswered = p.getString(KEY_CODE_VERIFY_61, CarrierCodes.DEFAULT_VERIFY_61)
                ?: CarrierCodes.DEFAULT_VERIFY_61,
            verifyBusy = p.getString(KEY_CODE_VERIFY_67, CarrierCodes.DEFAULT_VERIFY_67)
                ?: CarrierCodes.DEFAULT_VERIFY_67,
            verifyUnreachable = p.getString(KEY_CODE_VERIFY_62, CarrierCodes.DEFAULT_VERIFY_62)
                ?: CarrierCodes.DEFAULT_VERIFY_62
        )
        val reminders = ReminderSettings(
            nightEnabled = p.getBoolean(KEY_NIGHT_ENABLED, true),
            nightHour = p.getInt(KEY_NIGHT_HOUR, 21),
            nightMinute = p.getInt(KEY_NIGHT_MINUTE, 0),
            morningEnabled = p.getBoolean(KEY_MORNING_ENABLED, false),
            morningHour = p.getInt(KEY_MORNING_HOUR, 9),
            morningMinute = p.getInt(KEY_MORNING_MINUTE, 30)
        )
        return AppState(
            presets = presets,
            activeSlotIndex = activeSlot,
            isForwardingOn = p.getBoolean(KEY_IS_ON, false),
            lastToggledAtMillis = p.getLong(KEY_LAST_TOGGLED, 0L),
            selectedSimAccountId = p.getString(KEY_SIM_ID, null),
            selectedSimLabel = p.getString(KEY_SIM_LABEL, "Default SIM") ?: "Default SIM",
            codes = codes,
            reminders = reminders
        )
    }

    fun savePreset(context: Context, slotIndex: Int, name: String, rawNumber: String) {
        val cleanName = name.trim().ifBlank { "Reception ${slotIndex + 1}" }
        val cleanNumber = sanitizePhoneNumber(rawNumber)
        prefs(context).edit()
            .putString("preset_name_$slotIndex", cleanName)
            .putString("preset_number_$slotIndex", cleanNumber)
            .apply()
    }

    fun setActiveSlot(context: Context, slotIndex: Int) {
        prefs(context).edit()
            .putInt(KEY_ACTIVE_SLOT, slotIndex.coerceIn(0, 2))
            .apply()
    }

    fun setForwardingStatus(context: Context, isOn: Boolean, activeSlotIndex: Int? = null) {
        val editor = prefs(context).edit()
            .putBoolean(KEY_IS_ON, isOn)
            .putLong(KEY_LAST_TOGGLED, System.currentTimeMillis())
        if (activeSlotIndex != null) {
            editor.putInt(KEY_ACTIVE_SLOT, activeSlotIndex.coerceIn(0, 2))
        }
        editor.apply()
    }

    fun setSelectedSim(context: Context, accountId: String?, label: String) {
        prefs(context).edit()
            .putString(KEY_SIM_ID, accountId)
            .putString(KEY_SIM_LABEL, label)
            .apply()
    }

    fun saveCodes(context: Context, codes: CarrierCodes) {
        prefs(context).edit()
            .putString(KEY_CODE_ACTIVATE, codes.activateTemplate.trim().ifBlank { CarrierCodes.DEFAULT_ACTIVATE })
            .putString(KEY_CODE_DEACTIVATE, codes.deactivateCode.trim().ifBlank { CarrierCodes.DEFAULT_DEACTIVATE })
            .putString(KEY_CODE_VERIFY_61, codes.verifyUnanswered.trim().ifBlank { CarrierCodes.DEFAULT_VERIFY_61 })
            .putString(KEY_CODE_VERIFY_67, codes.verifyBusy.trim().ifBlank { CarrierCodes.DEFAULT_VERIFY_67 })
            .putString(KEY_CODE_VERIFY_62, codes.verifyUnreachable.trim().ifBlank { CarrierCodes.DEFAULT_VERIFY_62 })
            .apply()
    }

    fun saveReminders(context: Context, reminders: ReminderSettings) {
        prefs(context).edit()
            .putBoolean(KEY_NIGHT_ENABLED, reminders.nightEnabled)
            .putInt(KEY_NIGHT_HOUR, reminders.nightHour)
            .putInt(KEY_NIGHT_MINUTE, reminders.nightMinute)
            .putBoolean(KEY_MORNING_ENABLED, reminders.morningEnabled)
            .putInt(KEY_MORNING_HOUR, reminders.morningHour)
            .putInt(KEY_MORNING_MINUTE, reminders.morningMinute)
            .apply()
    }

    fun exportPresetsText(context: Context): String {
        val state = loadState(context)
        val lines = mutableListOf("CallForwardPresets:")
        state.presets.forEach { preset ->
            lines.add("${preset.slotIndex + 1}|${preset.name}|${preset.number}")
        }
        return lines.joinToString("
")
    }

    fun importPresetsText(context: Context, text: String): Int {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("CallForwardPresets", ignoreCase = true) }
        var updatedCount = 0
        var fallbackSlot = 0
        for (line in lines) {
            if (fallbackSlot > 2) break
            val parts = line.split("|").map { it.trim() }
            if (parts.size >= 3) {
                val parsedSlot = (parts[0].toIntOrNull()?.minus(1))?.takeIf { it in 0..2 } ?: fallbackSlot
                savePreset(context, parsedSlot, parts[1], parts[2])
                updatedCount++
                fallbackSlot++
            } else if (parts.size == 2) {
                savePreset(context, fallbackSlot, parts[0], parts[1])
                updatedCount++
                fallbackSlot++
            }
        }
        return updatedCount
    }
}
