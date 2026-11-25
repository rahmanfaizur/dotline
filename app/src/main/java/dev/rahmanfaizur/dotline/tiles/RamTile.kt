package dev.rahmanfaizur.dotline.tiles

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dev.rahmanfaizur.dotline.DotlineApp
import dev.rahmanfaizur.dotline.domain.Sizes

/** "RAM" in the phone's pull-down panel: live use while the panel is open; tap for memory details. */
class RamTile : TileService() {
    private val app get() = application as DotlineApp

    override fun onStartListening() {
        val m = app.system.memory()
        val tile = qsTile ?: return
        tile.label = "RAM ${Sizes.percentUsed(m.totalBytes - m.usedBytes, m.totalBytes)}%"
        tile.subtitle = "${Sizes.short(m.usedBytes)} / ${Sizes.short(m.totalBytes)}"
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        val intent = Intent(Settings.ACTION_DEVICE_INFO_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
