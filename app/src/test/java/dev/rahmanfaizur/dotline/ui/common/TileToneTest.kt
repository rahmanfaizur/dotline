package dev.rahmanfaizur.dotline.ui.common

import dev.rahmanfaizur.dotline.data.TextTone
import dev.rahmanfaizur.dotline.data.TileStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TileToneTest {
    private fun tone(behind: Float, style: TileStyle, setting: TextTone) = TileTone(false).apply { update(behind, style, setting) }

    @Test fun autoPicksBlackOnlyWhereTheTileIsBright() {
        assertTrue(tone(0.9f, TileStyle.Outline, TextTone.Auto).dark)
        assertFalse(tone(0.2f, TileStyle.Outline, TextTone.Auto).dark)
        assertFalse("dark tiles keep white text", tone(0.95f, TileStyle.Smoke, TextTone.Auto).dark)
    }

    @Test fun whiteAndBlackStayPutButGetHelpWhereNeeded() {
        val white = tone(0.9f, TileStyle.Outline, TextTone.White)
        assertFalse(white.dark)
        assertTrue("white text on a bright picture gets a shade", white.help > 0.5f)
        val black = tone(0.1f, TileStyle.Outline, TextTone.Black)
        assertTrue(black.dark)
        assertTrue("black text on a dark picture gets a light shade", black.help > 0.5f)
    }

    @Test fun noHelpWhenItAlreadyReadsWell() {
        assertEquals(0f, tone(0.1f, TileStyle.Outline, TextTone.White).help)
        assertEquals(0f, tone(0.95f, TileStyle.Outline, TextTone.Black).help)
        assertEquals(0f, tone(0.9f, TileStyle.Solid, TextTone.White).help)
    }
}
