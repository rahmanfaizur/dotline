package dev.rahmanfaizur.dotline.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.util.LruCache
import android.os.Build
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders each app's real icon once into a square bitmap, full-bleed, so the UI can
 * clip it to a circle (or rounded square). Cached in memory, capped at ~16 MB.
 */
class IconLoader(context: Context) {

    private val densityDpi = context.resources.displayMetrics.densityDpi

    /** Tests turn this off to check the Android 12 way of making one-colour icons. */
    internal var useSystemMono = true

    private val cache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    /** [tint] = 0 for the app's own colours, otherwise its simple one-colour icon in that colour. */
    private fun cacheKey(app: AppEntry, sizePx: Int, tint: Int) = "${app.key}@$sizePx#$tint"

    /** Already-rendered icon, if any (lets the first frame show icons without flicker). */
    fun peek(app: AppEntry, sizePx: Int, tint: Int = 0): Bitmap? = cache.get(cacheKey(app, sizePx, tint))

    suspend fun load(app: AppEntry, sizePx: Int, tint: Int = 0): Bitmap {
        peek(app, sizePx, tint)?.let { return it }
        return withContext(Dispatchers.Default) {
            render(app, sizePx, tint).also { cache.put(cacheKey(app, sizePx, tint), it) }
        }
    }

    private fun render(app: AppEntry, size: Int, tint: Int): Bitmap {
        val drawable = app.info.getIcon(densityDpi)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dot-style: the app's own one-colour icon (Android 13+), drawn in the chosen colour on dark.
        val mono = if (tint != 0 && useSystemMono && Build.VERSION.SDK_INT >= 33 && drawable is AdaptiveIconDrawable) drawable.monochrome else null
        if (mono != null) {
            canvas.drawColor(0xFF1A1A1A.toInt())
            val bleed = size * 18 / 72
            mono.mutate()
            mono.colorFilter = PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN)
            mono.setBounds(-bleed, -bleed, size + bleed, size + bleed)
            mono.draw(canvas)
            return bitmap
        }
        // Older Android: make it from the icon's logo layer (its shape, in the chosen colour).
        if (tint != 0 && drawable is AdaptiveIconDrawable && drawable.foreground != null) {
            logoGlyph(drawable, size, tint)?.let { return it }
        }

        if (drawable is AdaptiveIconDrawable) {
            // Adaptive layers are 108dp with the visible area in the middle 72dp.
            // Draw them oversized so the visible part fills our shape edge to edge.
            val bleed = size * 18 / 72
            drawable.background?.run { setBounds(-bleed, -bleed, size + bleed, size + bleed); draw(canvas) }
            drawable.foreground?.run { setBounds(-bleed, -bleed, size + bleed, size + bleed); draw(canvas) }
        } else {
            // Old-style icon: put it on a dark plate with some padding.
            canvas.drawColor(0xFF1C1C1C.toInt())
            val pad = size * 16 / 100
            drawable.setBounds(pad, pad, size - pad, size - pad)
            drawable.draw(canvas)
        }
        return bitmap
    }

    /**
     * A one-colour version of an adaptive icon for phones without Android's own (Android 12 and older):
     * every pixel that differs from the icon's background colour becomes the logo, so details like
     * YouTube's play triangle or Chrome's rings stay. Null if nothing stands out.
     */
    private fun logoGlyph(icon: AdaptiveIconDrawable, size: Int, tint: Int): Bitmap? {
        val bleed = size * 18 / 72
        fun layer(d: android.graphics.drawable.Drawable?): IntArray {
            val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            d?.mutate()?.run { setBounds(-bleed, -bleed, size + bleed, size + bleed); draw(Canvas(b)) }
            return IntArray(size * size).also { b.getPixels(it, 0, size, 0, 0, size, size); b.recycle() }
        }
        val bg = layer(icon.background)
        val fg = layer(icon.foreground)
        // The background's typical colour (average of its opaque pixels).
        var r = 0L; var g = 0L; var b = 0L; var n = 0
        for (c in bg) if ((c ushr 24) > 200) { r += (c shr 16) and 0xFF; g += (c shr 8) and 0xFF; b += c and 0xFF; n++ }
        val br = if (n > 0) (r / n).toInt() else 0
        val bgG = if (n > 0) (g / n).toInt() else 0
        val bb = if (n > 0) (b / n).toInt() else 0
        val out = IntArray(size * size)
        val rgb = tint and 0xFFFFFF
        var lit = 0
        for (i in out.indices) {
            val f = fg[i]
            val fa = (f ushr 24) / 255f
            // Foreground over background, then how far that is from the background colour.
            val pr = ((f shr 16) and 0xFF) * fa + br * (1 - fa)
            val pg = ((f shr 8) and 0xFF) * fa + bgG * (1 - fa)
            val pb = (f and 0xFF) * fa + bb * (1 - fa)
            val d = maxOf(kotlin.math.abs(pr - br), kotlin.math.abs(pg - bgG), kotlin.math.abs(pb - bb)) / 255f
            val a = ((d - 0.12f) / 0.3f).coerceIn(0f, 1f)
            if (a > 0.5f) lit++
            // Glyph colour over the dark plate (0x1A1A1A).
            val k = a
            val pr2 = (0x1A + (((rgb shr 16) and 0xFF) - 0x1A) * k).toInt()
            val pg2 = (0x1A + (((rgb shr 8) and 0xFF) - 0x1A) * k).toInt()
            val pb2 = (0x1A + ((rgb and 0xFF) - 0x1A) * k).toInt()
            out[i] = (0xFF shl 24) or (pr2 shl 16) or (pg2 shl 8) or pb2
        }
        val share = lit.toFloat() / out.size
        if (share < 0.02f || share > 0.7f) return null
        return Bitmap.createBitmap(out, size, size, Bitmap.Config.ARGB_8888)
    }
}
