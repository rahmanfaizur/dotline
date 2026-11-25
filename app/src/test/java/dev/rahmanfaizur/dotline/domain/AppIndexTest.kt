package dev.rahmanfaizur.dotline.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppIndexTest {

    private val apps = listOf(
        "Blinkit", "Brave", "Camera", "ChatGPT", "Discord", "Gmail", "Instagram",
        "PhonePe", "Play Store", "Settings", "Slack", "Spotify", "Wallet",
        "WhatsApp", "Yellow Kite", "YouTube", "1Password", "Éclair Notes",
    )

    private fun find(q: String) = AppIndex.search(apps, q) { it }

    @Test fun `section is the first letter, uppercased`() {
        assertEquals('W', AppIndex.sectionOf("WhatsApp"))
        assertEquals('Y', AppIndex.sectionOf("yellow kite"))
    }

    @Test fun `accents are folded into their base letter`() {
        assertEquals('E', AppIndex.sectionOf("Éclair Notes"))
        assertEquals(listOf("Éclair Notes"), find("ecl"))
    }

    @Test fun `digits and symbols go to the other section, which sorts last`() {
        assertEquals(AppIndex.OTHER, AppIndex.sectionOf("1Password"))
        val sections = AppIndex.sections(apps.sorted()) { it }
        assertEquals(AppIndex.OTHER, sections.last().letter)
        assertEquals('B', sections.first().letter)
    }

    @Test fun `every app lands in exactly one section`() {
        val sections = AppIndex.sections(apps) { it }
        assertEquals(apps.size, sections.sumOf { it.items.size })
    }

    @Test fun `prefix beats contains`() {
        // "sp" starts Spotify; "Settings" doesn't contain it at all.
        assertEquals("Spotify", find("sp").first())
    }

    @Test fun `word start finds names in the middle`() {
        assertEquals("Yellow Kite", find("kite").first())
    }

    @Test fun `initials work for multi-word names`() {
        assertEquals("Play Store", find("ps").first())
    }

    @Test fun `letters in order still match as a last resort`() {
        assertTrue("WhatsApp" in find("wtsp"))
    }

    @Test fun `exact name ranks first`() {
        assertEquals("Slack", find("slack").first())
    }

    @Test fun `blank query matches nothing`() {
        assertTrue(find("  ").isEmpty())
    }

    @Test fun `search is case-insensitive`() {
        assertEquals("Gmail", find("GM").first())
    }
}
