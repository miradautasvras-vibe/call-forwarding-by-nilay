package com.nilay.callforward

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class CallForwardTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        // Protect against accidental pocket taps on the lock screen
        unlockAndRun {
            ForwardingController.toggle(this)
            refreshTile()
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val state = PrefsStore.loadState(this)
        val preset = state.activePreset

        tile.state = if (state.isForwardingOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (state.isForwardingOn) "Forward ON" else "Forward OFF"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (preset.number.isNotBlank()) {
                if (state.isForwardingOn) "→ ${preset.name}" else preset.name
            } else {
                "Set number"
            }
        }
        tile.updateTile()
    }
}
