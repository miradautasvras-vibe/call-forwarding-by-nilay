package com.nilay.callforward

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneDisabled
import androidx.compose.material.icons.rounded.PhoneForwarded
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Cool Pastel & Emerald Green Palette
private val EmeraldPrimary = Color(0xFF059669)
private val EmeraldDark = Color(0xFF047857)
private val EmeraldSoft = Color(0xFFD1FAE5)
private val PastelBackground = Color(0xFFF1F7F6)
private val PastelMintCard = Color(0xFFE7F4EE)
private val PastelIceBlueCard = Color(0xFFE9F2F8)
private val PastelLavenderCard = Color(0xFFEEEFF8)
private val PastelSlateOff = Color(0xFFE2ECE9)
private val PastelBorder = Color(0xFFD1E2DE)
private val DeepSlateText = Color(0xFF1E293B)
private val MutedSlateText = Color(0xFF475569)
private val SoftCoralWarn = Color(0xFFFFF1F2)
private val CoralBorder = Color(0xFFFECDD3)
private val CoralText = Color(0xFFBE123C)

private val AppColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldSoft,
    onPrimaryContainer = EmeraldDark,
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = PastelIceBlueCard,
    onSecondaryContainer = DeepSlateText,
    background = PastelBackground,
    onBackground = DeepSlateText,
    surface = Color.White,
    onSurface = DeepSlateText,
    surfaceVariant = PastelMintCard,
    onSurfaceVariant = MutedSlateText
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.ensureChannels(this)
        ReminderScheduler.syncAlarms(this)

        setContent {
            MaterialTheme(colorScheme = AppColorScheme) {
                CallForwardAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CallForwardAppScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var appState by remember { mutableStateOf(PrefsStore.loadState(context)) }
    var simOptions by remember { mutableStateOf(MmiDialer.getAvailableSims(context)) }
    var hasAllPermissions by remember { mutableStateOf(checkCorePermissions(context)) }

    // Dialog states
    var editingPreset by remember { mutableStateOf<ReceptionistPreset?>(null) }
    var pendingLiveSwitchPreset by remember { mutableStateOf<ReceptionistPreset?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var pickingTimeFor by remember { mutableStateOf<String?>(null) } // "night" or "morning"
    var advancedExpanded by remember { mutableStateOf(false) }

    fun reload() {
        appState = PrefsStore.loadState(context)
        simOptions = MmiDialer.getAvailableSims(context)
        hasAllPermissions = checkCorePermissions(context)
    }

    fun showMessage(msg: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Refresh UI when returning from dialer, widget toggle, or settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                reload()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        reload()
        if (MmiDialer.hasCallPermission(context)) {
            showMessage("Permissions ready")
        } else {
            showMessage("Phone permission is needed to dial Jio forwarding codes")
        }
    }

    Scaffold(
        containerColor = PastelBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Call Forwarding by Nilay",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepSlateText
                    )
                    Text(
                        text = "Missed · Canceled · Unreachable",
                        fontSize = 12.sp,
                        color = MutedSlateText
                    )
                }
                FilledTonalButton(
                    onClick = { pinHomeScreenWidget(context, ::showMessage) },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = PastelMintCard,
                        contentColor = EmeraldDark
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Widgets,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Widget", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // 2. Permission Banner (only shown if missing permissions)
            if (!hasAllPermissions) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftCoralWarn),
                    border = BorderStroke(1.dp, CoralBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = CoralText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "One-time setup needed",
                                fontWeight = FontWeight.Bold,
                                color = CoralText,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "Allow Phone & Notifications so the widget can dial Jio codes and send nighttime reminders.",
                            fontSize = 13.sp,
                            color = DeepSlateText
                        )
                        Button(
                            onClick = {
                                val perms = mutableListOf(
                                    Manifest.permission.CALL_PHONE,
                                    Manifest.permission.READ_PHONE_STATE
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                permissionLauncher.launch(perms.toTypedArray())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Grant Permissions")
                        }
                    }
                }
            }

            // 3. Hero Status & Big Toggle Card
            HeroStatusCard(
                state = appState,
                onPrimaryToggle = {
                    val result = ForwardingController.toggle(context)
                    reload()
                    result.onSuccess { showMessage(it) }
                }
            )

            // 4. Receptionists Section (3 Slots)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, PastelBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Receptionists",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepSlateText
                        )
                        Text(
                            text = "Tap to select · Pencil to edit",
                            fontSize = 11.sp,
                            color = MutedSlateText
                        )
                    }

                    appState.presets.forEach { preset ->
                        val isActive = preset.slotIndex == appState.activeSlotIndex
                        ReceptionistCard(
                            preset = preset,
                            isActive = isActive,
                            isForwardingOn = appState.isForwardingOn,
                            onSelect = {
                                if (preset.number.isBlank()) {
                                    editingPreset = preset
                                } else if (!isActive) {
                                    if (appState.isForwardingOn) {
                                        pendingLiveSwitchPreset = preset
                                    } else {
                                        PrefsStore.setActiveSlot(context, preset.slotIndex)
                                        ForwardingController.refreshAllSurfaces(context)
                                        reload()
                                        showMessage("${preset.name} selected for next toggle")
                                    }
                                }
                            },
                            onEdit = { editingPreset = preset }
                        )
                    }

                    HorizontalDivider(color = PastelBorder, modifier = Modifier.padding(vertical = 2.dp))

                    // Share / Import Presets Row (for syncing to spouse's phone)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val text = PrefsStore.exportPresetsText(context)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Call Forward Presets", text))
                                showMessage("Copied! Paste in WhatsApp to share with another phone")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, PastelBorder)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy Presets", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, PastelBorder)
                        ) {
                            Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Paste Presets", fontSize = 12.sp)
                        }
                    }
                }
            }

            // 5. Simple Daily Reminders Card (Cool Pastel Ice-Blue)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = PastelIceBlueCard),
                border = BorderStroke(1.dp, Color(0xFFCDE1EE))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Simple Reminders",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepSlateText
                    )

                    // Night Reminder
                    ReminderRow(
                        icon = Icons.Rounded.NightsStay,
                        title = "Night Reminder",
                        subtitle = "Alerts only if forwarding is still ON",
                        timeText = formatHourMinute(appState.reminders.nightHour, appState.reminders.nightMinute),
                        enabled = appState.reminders.nightEnabled,
                        onToggle = { checked ->
                            val updated = appState.reminders.copy(nightEnabled = checked)
                            PrefsStore.saveReminders(context, updated)
                            ReminderScheduler.syncAlarms(context)
                            reload()
                        },
                        onTimeClick = { pickingTimeFor = "night" }
                    )

                    HorizontalDivider(color = Color(0xFFCDE1EE))

                    // Morning Reminder
                    ReminderRow(
                        icon = Icons.Rounded.WbSunny,
                        title = "Morning Reminder",
                        subtitle = "Alerts only if forwarding is still OFF",
                        timeText = formatHourMinute(appState.reminders.morningHour, appState.reminders.morningMinute),
                        enabled = appState.reminders.morningEnabled,
                        onToggle = { checked ->
                            val updated = appState.reminders.copy(morningEnabled = checked)
                            PrefsStore.saveReminders(context, updated)
                            ReminderScheduler.syncAlarms(context)
                            reload()
                        },
                        onTimeClick = { pickingTimeFor = "morning" }
                    )

                    // Battery Unrestricted Helper
                    TextButton(
                        onClick = { openBatterySettings(context) },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BatteryChargingFull,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = EmeraldDark
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Keep reminders on time (Battery: Unrestricted)",
                            fontSize = 12.sp,
                            color = EmeraldDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 6. SIM Lock & Carrier Verification Card (Soft Pastel Lavender)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = PastelLavenderCard),
                border = BorderStroke(1.dp, Color(0xFFD9DCF0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.SimCard,
                            contentDescription = null,
                            tint = DeepSlateText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "SIM & Carrier Check",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepSlateText
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        simOptions.forEach { sim ->
                            val selected = sim.accountId == appState.selectedSimAccountId
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    PrefsStore.setSelectedSim(context, sim.accountId, sim.label)
                                    reload()
                                    showMessage("Locked to ${sim.label}")
                                },
                                label = { Text(sim.label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFD9DCF0))

                    // Always-available Force Clear All button
                    FilledTonalButton(
                        onClick = {
                            val result = ForwardingController.turnOff(context)
                            reload()
                            result.onSuccess { showMessage("Sent ${appState.codes.deactivateCode} to clear all rules") }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White,
                            contentColor = DeepSlateText
                        )
                    ) {
                        Icon(Icons.Rounded.CallEnd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Force Clear All Forwarding (${appState.codes.deactivateCode})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "Verify live status with Jio:",
                        fontSize = 12.sp,
                        color = MutedSlateText,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VerifyChip(
                            label = "Missed
${appState.codes.verifyUnanswered}",
                            modifier = Modifier.weight(1f),
                            onClick = { ForwardingController.verifyCode(context, appState.codes.verifyUnanswered) }
                        )
                        VerifyChip(
                            label = "Canceled
${appState.codes.verifyBusy}",
                            modifier = Modifier.weight(1f),
                            onClick = { ForwardingController.verifyCode(context, appState.codes.verifyBusy) }
                        )
                        VerifyChip(
                            label = "Unreach.
${appState.codes.verifyUnreachable}",
                            modifier = Modifier.weight(1f),
                            onClick = { ForwardingController.verifyCode(context, appState.codes.verifyUnreachable) }
                        )
                    }

                    TextButton(
                        onClick = { MmiDialer.openSystemCallSettings(context) },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MutedSlateText
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Open Android Phone Call Settings",
                            fontSize = 12.sp,
                            color = MutedSlateText
                        )
                    }
                }
            }

            // 7. Expandable Advanced Carrier Codes Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, PastelBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { advancedExpanded = !advancedExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Advanced: Carrier MMI Codes",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DeepSlateText
                        )
                        Icon(
                            imageVector = if (advancedExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = MutedSlateText
                        )
                    }

                    AnimatedVisibility(visible = advancedExpanded) {
                        AdvancedCodesEditor(
                            codes = appState.codes,
                            onSave = { newCodes ->
                                PrefsStore.saveCodes(context, newCodes)
                                reload()
                                showMessage("Carrier codes saved")
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }

    // Dialog 1: Edit Receptionist + Contact Picker
    editingPreset?.let { presetToEdit ->
        EditPresetDialog(
            preset = presetToEdit,
            onDismiss = { editingPreset = null },
            onSave = { name, number ->
                PrefsStore.savePreset(context, presetToEdit.slotIndex, name, number)
                ForwardingController.refreshAllSurfaces(context)
                reload()
                editingPreset = null
                showMessage("Saved ${name.ifBlank { "Receptionist" }}")
            }
        )
    }

    // Dialog 2: Live Switch Confirmation when Forwarding is Already ON
    pendingLiveSwitchPreset?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingLiveSwitchPreset = null },
            title = { Text("Switch live forwarding?") },
            text = {
                Text("Forwarding is currently ON. Dial Jio right now to route missed, canceled, and unreachable calls to ${target.name} (${target.number})?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingLiveSwitchPreset = null
                        val result = ForwardingController.turnOn(context, target.slotIndex)
                        reload()
                        result.onSuccess { showMessage(it) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Switch Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingLiveSwitchPreset = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog 3: Paste / Import Presets
    if (showImportDialog) {
        ImportPresetsDialog(
            onDismiss = { showImportDialog = false },
            onImport = { text ->
                val count = PrefsStore.importPresetsText(context, text)
                ForwardingController.refreshAllSurfaces(context)
                reload()
                showImportDialog = false
                if (count > 0) {
                    showMessage("Imported $count receptionist preset(s)")
                } else {
                    showMessage("Could not find valid Name|Number lines")
                }
            }
        )
    }

    // Dialog 4: Time Picker for Night / Morning Reminder
    pickingTimeFor?.let { mode ->
        val initialHour = if (mode == "night") appState.reminders.nightHour else appState.reminders.morningHour
        val initialMinute = if (mode == "night") appState.reminders.nightMinute else appState.reminders.morningMinute
        val timeState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { pickingTimeFor = null },
            title = {
                Text(if (mode == "night") "Night Reminder Time" else "Morning Reminder Time")
            },
            text = {
                TimePicker(state = timeState)
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = if (mode == "night") {
                            appState.reminders.copy(
                                nightEnabled = true,
                                nightHour = timeState.hour,
                                nightMinute = timeState.minute
                            )
                        } else {
                            appState.reminders.copy(
                                morningEnabled = true,
                                morningHour = timeState.hour,
                                morningMinute = timeState.minute
                            )
                        }
                        PrefsStore.saveReminders(context, updated)
                        ReminderScheduler.syncAlarms(context)
                        reload()
                        pickingTimeFor = null
                        showMessage("Reminder scheduled for ${formatHourMinute(timeState.hour, timeState.minute)}")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Save Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { pickingTimeFor = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HeroStatusCard(
    state: AppState,
    onPrimaryToggle: () -> Unit
) {
    val isOn = state.isForwardingOn
    val preset = state.activePreset
    val containerColor = if (isOn) EmeraldPrimary else PastelSlateOff
    val contentColor = if (isOn) Color.White else DeepSlateText
    val subColor = if (isOn) EmeraldSoft else MutedSlateText

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (isOn) null else BorderStroke(1.dp, PastelBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isOn) Color.White.copy(alpha = 0.2f) else Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOn) Icons.Rounded.PhoneForwarded else Icons.Rounded.PhoneDisabled,
                            contentDescription = null,
                            tint = if (isOn) Color.White else MutedSlateText,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isOn) "Forwarding ON" else "Forwarding OFF",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                        Text(
                            text = if (preset.number.isNotBlank()) {
                                "${preset.name} · ${preset.number}"
                            } else {
                                "Add a receptionist number below"
                            },
                            fontSize = 13.sp,
                            color = subColor
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isOn) Color.White.copy(alpha = 0.22f) else Color.White
                ) {
                    Text(
                        text = if (isOn) "LIVE" else "IDLE",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOn) Color.White else MutedSlateText
                    )
                }
            }

            if (state.lastToggledAtMillis > 0L) {
                val formatted = SimpleDateFormat("h:mm a, dd MMM", Locale.getDefault())
                    .format(Date(state.lastToggledAtMillis))
                Text(
                    text = "Last updated: $formatted · ${state.selectedSimLabel}",
                    fontSize = 12.sp,
                    color = subColor
                )
            }

            Button(
                onClick = onPrimaryToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOn) Color.White else EmeraldPrimary,
                    contentColor = if (isOn) EmeraldDark else Color.White
                )
            ) {
                Text(
                    text = if (isOn) "Turn OFF Forwarding (Night)" else "Turn ON Clinic Forwarding",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ReceptionistCard(
    preset: ReceptionistPreset,
    isActive: Boolean,
    isForwardingOn: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit
) {
    val bgColor = when {
        isActive && isForwardingOn -> EmeraldSoft
        isActive -> PastelMintCard
        else -> Color(0xFFF8FBFA)
    }
    val borderColor = if (isActive) EmeraldPrimary else PastelBorder

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(if (isActive) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isActive) Icons.Rounded.CheckCircle else Icons.Rounded.Person,
                contentDescription = null,
                tint = if (isActive) EmeraldPrimary else MutedSlateText,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = preset.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = DeepSlateText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isActive) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = EmeraldPrimary
                        ) {
                            Text(
                                text = "Active",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                Text(
                    text = preset.number.ifBlank { "Tap edit to set 10-digit number" },
                    fontSize = 13.sp,
                    color = MutedSlateText
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit receptionist",
                    tint = EmeraldDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ReminderRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    timeText: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onTimeClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EmeraldDark,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepSlateText
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MutedSlateText
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    modifier = Modifier.clickable { onTimeClick() },
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFCDE1EE))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = EmeraldDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = timeText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DeepSlateText
                        )
                    }
                }
            }
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = EmeraldPrimary
            )
        )
    }
}

@Composable
private fun VerifyChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFD9DCF0))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = DeepSlateText,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun AdvancedCodesEditor(
    codes: CarrierCodes,
    onSave: (CarrierCodes) -> Unit
) {
    var activate by remember(codes) { mutableStateOf(codes.activateTemplate) }
    var deactivate by remember(codes) { mutableStateOf(codes.deactivateCode) }
    var v61 by remember(codes) { mutableStateOf(codes.verifyUnanswered) }
    var v67 by remember(codes) { mutableStateOf(codes.verifyBusy) }
    var v62 by remember(codes) { mutableStateOf(codes.verifyUnreachable) }

    Column(
        modifier = Modifier.padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Verified Jio GSM MMI defaults. Use {number} where the receptionist's 10-digit number goes.",
            fontSize = 11.sp,
            color = MutedSlateText
        )
        OutlinedTextField(
            value = activate,
            onValueChange = { activate = it },
            label = { Text("Activate Conditional Bundle") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = deactivate,
            onValueChange = { deactivate = it },
            label = { Text("Cancel All Forwarding") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = v61,
                onValueChange = { v61 = it },
                label = { Text("Missed") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = v67,
                onValueChange = { v67 = it },
                label = { Text("Canceled") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = v62,
                onValueChange = { v62 = it },
                label = { Text("Unreach.") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = {
                    val defaults = CarrierCodes()
                    activate = defaults.activateTemplate
                    deactivate = defaults.deactivateCode
                    v61 = defaults.verifyUnanswered
                    v67 = defaults.verifyBusy
                    v62 = defaults.verifyUnreachable
                    onSave(defaults)
                }
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Reset Jio Defaults")
            }
            Button(
                onClick = {
                    onSave(
                        CarrierCodes(
                            activateTemplate = activate,
                            deactivateCode = deactivate,
                            verifyUnanswered = v61,
                            verifyBusy = v67,
                            verifyUnreachable = v62
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Codes")
            }
        }
    }
}

@Composable
private fun EditPresetDialog(
    preset: ReceptionistPreset,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(preset.name) }
    var number by remember { mutableStateOf(preset.number) }

    // System Contacts Picker (no READ_CONTACTS permission required)
    val contactPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri: Uri = result.data?.data ?: return@rememberLauncherForActivityResult
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val pickedName = cursor.getString(0).orEmpty()
                    val pickedNumber = cursor.getString(1).orEmpty()
                    if (pickedName.isNotBlank()) name = pickedName
                    if (pickedNumber.isNotBlank()) number = PrefsStore.sanitizePhoneNumber(pickedNumber)
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Receptionist ${preset.slotIndex + 1}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_PICK,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                        )
                        contactPicker.launch(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Contacts, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pick from Contacts")
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Short Name (e.g., Priya)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("10-Digit Mobile Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    supportingText = { Text("Spaces and +91 are cleaned automatically") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, number) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ImportPresetsDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    val context = LocalContext.current
    val initialClipboard = remember {
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
    }
    var text by remember {
        mutableStateOf(
            if (initialClipboard.contains("|")) initialClipboard else ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paste Receptionist Presets") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Paste the text copied from the other phone:",
                    fontSize = 12.sp,
                    color = MutedSlateText
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("1|Priya|9876543210
2|Neha|9876543211") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(text) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun checkCorePermissions(context: Context): Boolean {
    val callOk = MmiDialer.hasCallPermission(context)
    val stateOk = MmiDialer.hasPhoneStatePermission(context)
    val notifOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    return callOk && stateOk && notifOk
}

private fun formatHourMinute(hour24: Int, minute: Int): String {
    val amPm = if (hour24 >= 12) "PM" else "AM"
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, amPm)
}

private fun pinHomeScreenWidget(context: Context, onFeedback: (String) -> Unit) {
    val manager = AppWidgetManager.getInstance(context)
    val provider = ComponentName(context, CallForwardWidgetProvider::class.java)
    if (manager.isRequestPinAppWidgetSupported) {
        manager.requestPinAppWidget(provider, null, null)
    } else {
        onFeedback("Long-press your home screen → Widgets → Call Forward")
    }
}

private fun openBatterySettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Ignore if settings page unavailable
    }
}
