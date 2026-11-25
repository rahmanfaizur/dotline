package dev.rahmanfaizur.dotline.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoxBlurTest {
    @Test fun flatColourStaysTheSame() {
        val c = 0xFF336699.toInt()
        val px = IntArray(20 * 10) { c }
        BoxBlur.blur(px, 20, 10, radius = 3)
        assertTrue(px.all { it == c })
    }

    @Test fun brightPixelSpreadsToItsNeighbours() {
        val w = 9; val h = 9
        val px = IntArray(w * h) { 0xFF000000.toInt() }
        px[4 * w + 4] = 0xFFFFFFFF.toInt()
        BoxBlur.blur(px, w, h, radius = 1, passes = 1)
        val centre = px[4 * w + 4] and 0xFF
        val neighbour = px[4 * w + 5] and 0xFF
        val far = px[0] and 0xFF
        assertTrue(centre in 1..254)
        assertEquals(centre, neighbour)
        assertEquals(0, far)
    }

    @Test fun alphaIsKeptOpaque() {
        val px = IntArray(16) { if (it % 2 == 0) 0xFFFF0000.toInt() else 0xFF0000FF.toInt() }
        BoxBlur.blur(px, 4, 4, radius = 2)
        assertTrue(px.all { (it ushr 24) == 0xFF })
    }
}
