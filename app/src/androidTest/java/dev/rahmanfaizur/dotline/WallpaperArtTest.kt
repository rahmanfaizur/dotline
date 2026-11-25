package dev.rahmanfaizur.dotline

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.rahmanfaizur.dotline.data.WallpaperArt
import dev.rahmanfaizur.dotline.data.WallpaperKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Renders every built-in wallpaper on a real device (they use Android's drawing code). */
@RunWith(AndroidJUnit4::class)
class WallpaperArtTest {
    private val red = 0xFFE8453C.toInt()

    private fun pixels(b: Bitmap) = IntArray(b.width * b.height).also { b.getPixels(it, 0, b.width, 0, 0, b.width, b.height) }
    private fun darkShare(b: Bitmap) = pixels(b).count { Color.red(it) + Color.green(it) + Color.blue(it) < 60 }.toFloat() / (b.width * b.height)

    @Test fun blackNeedsNoPicture() {
        assertNull(WallpaperArt.render(WallpaperKind.Black, 108, 240, red, lock = false, photo = null))
    }

    @Test fun everyLookRendersAtScreenSize() {
        WallpaperKind.entries.filter { it != WallpaperKind.Black }.forEach { kind ->
            for (lock in listOf(false, true)) {
                val b = WallpaperArt.render(kind, 108, 240, red, lock, photo = null)
                assertNotNull("$kind lock=$lock", b)
                assertEquals(108, b!!.width)
                assertEquals(240, b.height)
            }
        }
    }

    @Test fun darkLooksStayMostlyBlack() {
        for (kind in listOf(WallpaperKind.DotField, WallpaperKind.Horizon, WallpaperKind.Glow)) {
            val b = WallpaperArt.render(kind, 216, 480, red, lock = false, photo = null)!!
            assertTrue("$kind dark share ${darkShare(b)}", darkShare(b) > 0.5f)
        }
    }

    @Test fun sunsetIsColourful() {
        val b = WallpaperArt.render(WallpaperKind.Sunset, 216, 480, red, lock = false, photo = null)!!
        assertTrue(darkShare(b) < 0.5f)
    }

    @Test fun lockDotFieldHasTheAccentDot() {
        val b = WallpaperArt.render(WallpaperKind.DotField, 216, 480, red, lock = true, photo = null)!!
        assertEquals(red, b.getPixel(108, (480 * 0.62f).toInt()))
    }

    @Test fun halftoneUsesThePickedPhoto() {
        val white = Bitmap.createBitmap(50, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val black = Bitmap.createBitmap(50, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLACK) }
        val fromWhite = WallpaperArt.render(WallpaperKind.Halftone, 216, 480, red, false, white)!!
        val fromBlack = WallpaperArt.render(WallpaperKind.Halftone, 216, 480, red, false, black)!!
        assertTrue(darkShare(fromWhite) < darkShare(fromBlack))
    }

    @Test fun frostedCopyIsSmall() {
        val b = WallpaperArt.render(WallpaperKind.Sunset, 1080, 2400, red, lock = false, photo = null)!!
        val f = WallpaperArt.frosted(b)
        assertEquals(135, f.width)
        assertEquals(300, f.height)
    }

    @Test fun brightnessReadsLightAndDark() {
        val white = Bitmap.createBitmap(20, 40, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val black = Bitmap.createBitmap(20, 40, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLACK) }
        assertTrue(WallpaperArt.brightness(white) > 0.95f)
        assertTrue(WallpaperArt.brightness(black) < 0.05f)
    }

    @Test fun autoTextGoesBlackOnlyOnBrightSeeThroughTiles() {
        val img = androidx.compose.ui.graphics.ImageBitmap(1, 1)
        val bright = dev.rahmanfaizur.dotline.ui.common.Backdrop(img, img, brightness = 0.85f)
        val dark = dev.rahmanfaizur.dotline.ui.common.Backdrop(img, img, brightness = 0.2f)
        val auto = dev.rahmanfaizur.dotline.data.TextTone.Auto
        val t = dev.rahmanfaizur.dotline.data.TileStyle.entries.associateBy { it.name }
        fun dark(style: String, b: dev.rahmanfaizur.dotline.ui.common.Backdrop?) =
            dev.rahmanfaizur.dotline.ui.common.useDarkText(auto, t.getValue(style), b)
        assertTrue(dark("Glass", bright))
        assertTrue(dark("Floating", bright))
        assertTrue(!dark("Smoke", bright))   // dark tiles keep white text
        assertTrue(!dark("Glass", dark))     // dark wallpaper keeps white text
        assertTrue(!dark("Glass", null))     // plain black home
        assertTrue(dev.rahmanfaizur.dotline.ui.common.useDarkText(
            dev.rahmanfaizur.dotline.data.TextTone.Black, t.getValue("Smoke"), null))
    }
}
