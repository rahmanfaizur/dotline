package dev.rahmanfaizur.dotline.tiles

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dev.rahmanfaizur.dotline.DotlineApp
import dev.rahmanfaizur.dotline.data.WallpaperKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** "Next wallpaper" in the phone's pull-down panel: each tap moves to the next Dotline wallpaper. */
class WallpaperTile : TileService() {
    private val app get() = application as DotlineApp

    override fun onStartListening() {
        app.appScope.launch { show(app.settings.settings.first().wallpaper) }
    }

    override fun onClick() {
        app.appScope.launch {
            val now = app.settings.settings.first().wallpaper
            val next = nextWallpaper(now, app.wallpapers.hasPhoto)
            app.settings.setWallpaper(next)
            show(next)
        }
    }

    private fun show(kind: WallpaperKind) {
        val tile = qsTile ?: return
        tile.label = "Wallpaper"
        tile.subtitle = NAMES[kind]
        tile.state = if (kind == WallpaperKind.Black) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        tile.updateTile()
    }

    companion object {
        private val NAMES = mapOf(
            WallpaperKind.Black to "Black", WallpaperKind.Photo to "My photo", WallpaperKind.DotField to "Dot field",
            WallpaperKind.Halftone to "Halftone", WallpaperKind.Sunset to "Sunset", WallpaperKind.Glow to "Glow",
            WallpaperKind.Horizon to "Horizon",
        )

        /** The order the tile steps through; My photo only once a photo has been picked. */
        fun nextWallpaper(now: WallpaperKind, hasPhoto: Boolean): WallpaperKind {
            val cycle = listOfNotNull(
                WallpaperKind.Black, WallpaperKind.Photo.takeIf { hasPhoto }, WallpaperKind.DotField,
                WallpaperKind.Halftone, WallpaperKind.Sunset, WallpaperKind.Glow, WallpaperKind.Horizon,
            )
            val i = cycle.indexOf(now)
            return cycle[(i + 1) % cycle.size]
        }
    }
}
