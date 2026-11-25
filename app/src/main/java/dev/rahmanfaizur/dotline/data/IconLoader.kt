package dev.rahmanfaizur.dotline.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders each app's real icon once into a square bitmap, full-bleed, so the UI can
 * clip it to a circle (or rounded square). Cached in memory, capped at ~16 MB.
 */
class IconLoader(context: Context) {

    private val densityDpi = context.resources.displayMetrics.densityDpi

    private val cache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    private fun cacheKey(app: AppEntry, sizePx: Int) = "${app.key}@$sizePx"

    /** Already-rendered icon, if any (lets the first frame show icons without flicker). */
    fun peek(app: AppEntry, sizePx: Int): Bitmap? = cache.get(cacheKey(app, sizePx))

    suspend fun load(app: AppEntry, sizePx: Int): Bitmap {
        peek(app, sizePx)?.let { return it }
        return withContext(Dispatchers.Default) {
            render(app, sizePx).also { cache.put(cacheKey(app, sizePx), it) }
        }
    }

    private fun render(app: AppEntry, size: Int): Bitmap {
        val drawable = app.info.getIcon(densityDpi)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

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
}
