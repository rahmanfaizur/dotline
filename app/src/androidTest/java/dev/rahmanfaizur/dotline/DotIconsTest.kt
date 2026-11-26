package dev.rahmanfaizur.dotline

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.rahmanfaizur.dotline.data.IconLoader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Dot-style icons: one colour on dark, both the Android 13+ way and the Android 12 fallback. */
@RunWith(AndroidJUnit4::class)
class DotIconsTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as DotlineApp
    private val white = 0xFFF2F2F2.toInt()

    /** Share of pixels that are either the dark plate or (close to) the tint. */
    private fun oneColourShare(b: android.graphics.Bitmap): Float {
        val px = IntArray(b.width * b.height).also { b.getPixels(it, 0, b.width, 0, 0, b.width, b.height) }
        return px.count { c ->
            val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val bl = c and 0xFF
            kotlin.math.abs(r - g) < 24 && kotlin.math.abs(g - bl) < 24 // grey-scale: plate, white or in between
        }.toFloat() / px.size
    }

    private fun renderAll(systemMono: Boolean): Int {
        val loader = IconLoader(app).apply { useSystemMono = systemMono }
        val apps = runBlocking {
            var list = app.apps.apps.value
            val end = System.currentTimeMillis() + 5_000
            while (list.isEmpty() && System.currentTimeMillis() < end) { Thread.sleep(100); list = app.apps.apps.value }
            list
        }
        assertTrue(apps.isNotEmpty())
        // Optional contact sheet for a visual check (-e dumpIcons true): saved in the app's cache folder.
        if (InstrumentationRegistry.getArguments().getString("dumpIcons") == "true") {
            val n = apps.size.coerceAtMost(24)
            val sheet = android.graphics.Bitmap.createBitmap(6 * 104, ((n + 5) / 6) * 104 * 2, android.graphics.Bitmap.Config.ARGB_8888)
            val c = android.graphics.Canvas(sheet)
            c.drawColor(android.graphics.Color.BLACK)
            apps.take(n).forEachIndexed { i, a ->
                val x = (i % 6) * 104f + 4; val y = (i / 6) * 208f + 4
                c.drawBitmap(runBlocking { loader.load(a, 96) }, x, y, null)
                c.drawBitmap(runBlocking { loader.load(a, 96, white) }, x, y + 104, null)
            }
            java.io.File(app.cacheDir, "icons-${if (systemMono) "13" else "12"}.png").outputStream().use { sheet.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
        return apps.count { a ->
            val own = runBlocking { loader.load(a, 96) }
            val dot = runBlocking { loader.load(a, 96, white) }
            !own.sameAs(dot) && oneColourShare(dot) > 0.97f
        }
    }

    @Test fun android13IconsTurnOneColour() { assertTrue(renderAll(systemMono = true) >= 3) }

    @Test fun android12FallbackTurnsOneColourToo() { assertTrue(renderAll(systemMono = false) >= 3) }
}
