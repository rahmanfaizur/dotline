package dev.rahmanfaizur.dotline.domain

/**
 * A soft blur for the frosted-glass tiles. It runs once, on a small copy of the wallpaper,
 * when the wallpaper changes, so it costs nothing while you scroll.
 */
object BoxBlur {
    /** Blurs ARGB [pixels] (width [w], height [h]) in place; three box passes look close to a Gaussian. */
    fun blur(pixels: IntArray, w: Int, h: Int, radius: Int, passes: Int = 3) {
        require(pixels.size == w * h)
        if (radius < 1 || w == 0 || h == 0) return
        val tmp = IntArray(pixels.size)
        repeat(passes) {
            pass(pixels, tmp, w, h, radius, horizontal = true)
            pass(tmp, pixels, w, h, radius, horizontal = false)
        }
    }

    /** One sliding-window average along rows or columns; edges repeat the border pixel. */
    private fun pass(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val lines = if (horizontal) h else w
        val len = if (horizontal) w else h
        val span = 2 * r + 1
        for (line in 0 until lines) {
            fun at(i: Int): Int {
                val c = i.coerceIn(0, len - 1)
                return if (horizontal) src[line * w + c] else src[c * w + line]
            }
            var a = 0; var red = 0; var g = 0; var b = 0
            for (i in -r..r) {
                val p = at(i)
                a += p ushr 24; red += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF
            }
            for (i in 0 until len) {
                val out = ((a / span) shl 24) or ((red / span) shl 16) or ((g / span) shl 8) or (b / span)
                if (horizontal) dst[line * w + i] = out else dst[i * w + line] = out
                val add = at(i + r + 1)
                val drop = at(i - r)
                a += (add ushr 24) - (drop ushr 24)
                red += ((add shr 16) and 0xFF) - ((drop shr 16) and 0xFF)
                g += ((add shr 8) and 0xFF) - ((drop shr 8) and 0xFF)
                b += (add and 0xFF) - (drop and 0xFF)
            }
        }
    }
}
