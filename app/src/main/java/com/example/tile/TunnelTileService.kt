package com.example.tile

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.MainActivity
import com.example.tunnel.TunnelEngine
import com.example.tunnel.TunnelState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class TunnelTileService : TileService() {

    private val tileScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val engine = TunnelEngine.instance
        engine.toggleConnect(applicationContext)
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val engine = TunnelEngine.instance
        val state = engine.state.value
        val profile = engine.activeProfile.value

        when (state) {
            is TunnelState.Connected -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = profile?.name ?: "Connected"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Connected"
                }
            }
            is TunnelState.Connecting -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Connecting..."
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Handshake"
                }
            }
            is TunnelState.Disconnecting -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Disconnecting..."
            }
            is TunnelState.Failed -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "Failed"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Tap to retry"
                }
            }
            is TunnelState.Idle -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = profile?.name ?: "Deep Current"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Disconnected"
                }
            }
        }
        tile.updateTile()
    }
}
