package dev.rahmanfaizur.dotline.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import dev.rahmanfaizur.dotline.domain.Halftone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/** What's behind the home screen. */
enum class WallpaperKind { Black, Photo, DotField, Halftone, Sunset, Glow, Horizon }

/**
 * Draws Dotline's wallpapers on the phone, at the screen's exact size. Nothing is downloaded;
 * [WallpaperKind.Photo] and [WallpaperKind.Halftone] use the photo the user picked.
 */
object WallpaperArt {

    /** Null for [WallpaperKind.Black], which needs no picture. [lock] = the lock-screen variant. */
    fun render(kind: WallpaperKind, w: Int, h: Int, accent: Int, lock: Boolean, photo: Bitmap?): Bitmap? {
        if (kind == WallpaperKind.Black || w <= 0 || h <= 0) return null
        val bmp = createBitmap(w, h)
        val c = Canvas(bmp)
        val wf = w.toFloat(); val hf = h.toFloat()
        when (kind) {
            WallpaperKind.Photo -> if (photo != null) fill(c, photo, w, h) else sunset(c, wf, hf)
            WallpaperKind.DotField -> dotField(c, wf, hf, accent, lock)
            WallpaperKind.Halftone -> halftone(c, w, h, accent, photo ?: sunsetBitmap(w, h))
            WallpaperKind.Sunset -> sunset(c, wf, hf)
            WallpaperKind.Glow -> glow(c, wf, hf, accent, lock)
            WallpaperKind.Horizon -> horizon(c, wf, hf, accent, lock)
            WallpaperKind.Black -> Unit
        }
        return bmp
    }

    // One brush per thread: thumbnails render in parallel, and a shared Paint mixed their colours.
    private val brushes = ThreadLocal.withInitial { Paint(Paint.ANTI_ALIAS_FLAG) }
    private val paint: Paint get() = brushes.get()!!

    /** Centre-crop [src] to fill the screen, like a normal wallpaper. */
    private fun fill(c: Canvas, src: Bitmap, w: Int, h: Int) {
        val s = max(w.toFloat() / src.width, h.toFloat() / src.height)
        val cw = (w / s).roundToInt().coerceAtMost(src.width)
        val ch = (h / s).roundToInt().coerceAtMost(src.height)
        val left = (src.width - cw) / 2
        val top = (src.height - ch) / 2
        c.drawBitmap(src, Rect(left, top, left + cw, top + ch), Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
    }

    private fun dot(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        paint.shader = null
        paint.color = color
        c.drawCircle(x, y, r, paint)
    }

    /** 1. Black with a dot grid that fades in towards the bottom; the lock screen adds one accent dot. */
    private fun dotField(c: Canvas, w: Float, h: Float, accent: Int, lock: Boolean) {
        c.drawColor(Color.BLACK)
        val s = w / 26f
        var y = s / 2
        while (y < h) {
            val alpha = 0.05f + 0.32f * Math.pow((y / h).toDouble(), 1.6).toFloat()
            val col = Color.argb((alpha * 255).roundToInt(), 255, 255, 255)
            var x = s / 2
            while (x < w) { dot(c, x, y, s * 0.16f, col); x += s }
            y += s
        }
        if (lock) dot(c, w * 0.5f, h * 0.62f, w * 0.09f, accent)
    }

    /** 2. A photo redrawn as dots; the brightest spot turns the accent colour. */
    private fun halftone(c: Canvas, w: Int, h: Int, accent: Int, photo: Bitmap) {
        c.drawColor(Color.BLACK)
        val grid = Halftone.grid(w, h)
        // Shrinking the photo to one pixel per cell averages each cell's brightness for free.
        val small = Bitmap.createBitmap(grid.columns, grid.rows, Bitmap.Config.ARGB_8888)
        fill(Canvas(small), photo, grid.columns, grid.rows)
        val px = IntArray(grid.columns * grid.rows)
        small.getPixels(px, 0, grid.columns, 0, 0, grid.columns, grid.rows)
        small.recycle()
        val lumas = FloatArray(px.size) { Halftone.luma(px[it]) }
        val brightest = lumas.maxOrNull() ?: 0f
        val ink = Color.rgb(0xF2, 0xF2, 0xF2)
        for (row in 0 until grid.rows) for (col in 0 until grid.columns) {
            val l = lumas[row * grid.columns + col]
            dot(
                c,
                (col + 0.5f) * grid.cell,
                (row + 0.5f) * grid.cell,
                Halftone.radius(l, grid.cell),
                if (Halftone.isHighlight(l, brightest)) accent else ink,
            )
        }
    }

    private fun sunsetBitmap(w: Int, h: Int): Bitmap {
        // A small version is plenty as a halftone source.
        val sw = 270; val sh = (270f * h / w).roundToInt().coerceAtLeast(1)
        return createBitmap(sw, sh).also { sunset(Canvas(it), sw.toFloat(), sh.toFloat()) }
    }

    /** 3. Layered hills and a low sun in sunset colours. */
    private fun sunset(c: Canvas, w: Float, h: Float) {
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF2B1B4A.toInt(), 0xFFC2546A.toInt(), 0xFFF0A35E.toInt(), 0xFFF6C98A.toInt()),
            floatArrayOf(0f, 0.42f, 0.6f, 1f),
            Shader.TileMode.CLAMP,
        )
        c.drawRect(0f, 0f, w, h, paint)
        dot(c, w * 0.62f, h * 0.5f, w * 0.13f, 0xE6FFF0C8.toInt())
        ridge(c, w, h, h * 0.58f, h * 0.04f, 6f, 0xFF6B3554.toInt())
        ridge(c, w, h, h * 0.68f, h * 0.05f, 4f, 0xFF3A1F36.toInt())
        ridge(c, w, h, h * 0.80f, h * 0.03f, 9f, 0xFF1C1022.toInt())
    }

    private fun ridge(c: Canvas, w: Float, h: Float, base: Float, amp: Float, f: Float, color: Int) {
        val p = Path().apply {
            moveTo(0f, h)
            var x = 0f
            while (x <= w) {
                lineTo(x, base + sin(x / w * f) * amp + sin(x / w * f * 3.1f) * amp * 0.3f)
                x += max(2f, w / 300f)
            }
            lineTo(w, h); close()
        }
        paint.shader = null; paint.color = color
        c.drawPath(p, paint)
    }

    /** 4. Black with a soft glow in the accent colour rising from one corner. */
    private fun glow(c: Canvas, w: Float, h: Float, accent: Int, lock: Boolean) {
        c.drawColor(Color.BLACK)
        fun a(color: Int, alpha: Float) = ColorUtils.setAlphaComponent(color, (alpha * 255).roundToInt())
        paint.shader = RadialGradient(
            w * 0.1f, h * 0.95f, w * 1.1f,
            intArrayOf(a(accent, 0.75f), a(accent, 0.18f), a(accent, 0f)),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
        c.drawRect(0f, 0f, w, h, paint)
        val warm = ColorUtils.blendARGB(accent, Color.WHITE, 0.35f)
        paint.shader = RadialGradient(w * 0.95f, h * 0.08f, w * 0.7f, a(warm, 0.25f), a(warm, 0f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
        if (lock) c.drawColor(0x26000000)
    }

    /** 5. Curved rows of dots like a planet's edge; on the lock screen an accent "sun" rises above it. */
    private fun horizon(c: Canvas, w: Float, h: Float, accent: Int, lock: Boolean) {
        c.drawColor(Color.BLACK)
        val cx = w / 2; val cy = h * 1.32f; val big = h * 0.62f
        val step = w / 28f
        for (ring in 0 until 9) {
            val r = big - ring * step
            val n = (r / (w / 210f)).roundToInt().coerceAtLeast(2)
            val col = Color.argb(((0.75f - ring * 0.08f) * 255).roundToInt(), 255, 255, 255)
            val size = max(w / 600f, w / 150f - ring * (w / 1800f))
            for (i in 0 until n) {
                val ang = PI + i.toDouble() / (n - 1) * PI
                val x = cx + cos(ang).toFloat() * r
                val y = cy + sin(ang).toFloat() * r
                if (y > h || x < 0 || x > w) continue
                dot(c, x, y, size, col)
            }
        }
        if (lock) dot(c, cx, h * 0.6f, w * 0.06f, accent)
    }

    /** Average brightness of the top 70% of [small] (where the tiles are), 0..1. */
    fun brightness(small: Bitmap): Float {
        val h = (small.height * 0.7f).toInt().coerceAtLeast(1)
        val px = IntArray(small.width * h)
        small.getPixels(px, 0, small.width, 0, 0, small.width, h)
        return if (px.isEmpty()) 0f else px.sumOf { Halftone.luma(it).toDouble() }.toFloat() / px.size
    }

    /**
     * A small, blurred copy for frosted-glass tiles. Made once per wallpaper change;
     * drawing it scaled up is what keeps scrolling smooth.
     */
    fun frosted(src: Bitmap, shrink: Int = 8, radius: Int = 3): Bitmap {
        val w = (src.width / shrink).coerceAtLeast(1)
        val h = (src.height / shrink).coerceAtLeast(1)
        val small = src.scale(w, h)
        val px = IntArray(w * h)
        small.getPixels(px, 0, w, 0, 0, w, h)
        dev.rahmanfaizur.dotline.domain.BoxBlur.blur(px, w, h, radius)
        small.setPixels(px, 0, w, 0, 0, w, h)
        return small
    }
}
