package dev.rahmanfaizur.dotline.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HalftoneTest {
    @Test fun lumaRunsFromBlackToWhite() {
        assertEquals(0f, Halftone.luma(0xFF000000.toInt()), 0.001f)
        assertEquals(1f, Halftone.luma(0xFFFFFFFF.toInt()), 0.001f)
        assertTrue(Halftone.luma(0xFF00FF00.toInt()) > Halftone.luma(0xFF0000FF.toInt())) // green looks brighter than blue
    }

    @Test fun brighterCellsGetBiggerDots() {
        val cell = 30f
        assertTrue(Halftone.radius(0.8f, cell) > Halftone.radius(0.3f, cell))
        assertEquals(15f, Halftone.radius(1f, cell), 0.001f) // white fills the cell
        assertTrue(Halftone.radius(0f, cell) > 0f) // black still shows a tiny dot
    }

    @Test fun gridCoversTheWholeScreen() {
        val g = Halftone.grid(1080, 2400)
        assertEquals(34, g.columns)
        assertTrue(g.rows * g.cell >= 2400f)
    }

    @Test fun onlyTheBrightestSpotsAreHighlighted() {
        assertTrue(Halftone.isHighlight(0.98f, brightest = 1f))
        assertFalse(Halftone.isHighlight(0.6f, brightest = 1f))
        assertFalse(Halftone.isHighlight(0.5f, brightest = 0.5f)) // a dull photo gets no red
    }
}
