package dev.rahmanfaizur.dotline.tiles

import dev.rahmanfaizur.dotline.data.WallpaperKind
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperTileTest {
    @Test fun stepsThroughEveryLookAndWrapsAround() {
        var k = WallpaperKind.Black
        val seen = mutableListOf<WallpaperKind>()
        repeat(6) { k = WallpaperTile.nextWallpaper(k, hasPhoto = false); seen += k }
        assertEquals(
            listOf(WallpaperKind.DotField, WallpaperKind.Halftone, WallpaperKind.Sunset, WallpaperKind.Glow, WallpaperKind.Horizon, WallpaperKind.Black),
            seen,
        )
    }

    @Test fun includesMyPhotoOnlyWhenOneWasPicked() {
        assertEquals(WallpaperKind.Photo, WallpaperTile.nextWallpaper(WallpaperKind.Black, hasPhoto = true))
        assertEquals(WallpaperKind.DotField, WallpaperTile.nextWallpaper(WallpaperKind.Black, hasPhoto = false))
    }
}
