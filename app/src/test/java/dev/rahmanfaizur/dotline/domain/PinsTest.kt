package dev.rahmanfaizur.dotline.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PinsTest {

    @Test fun `picks preferred apps in order, skipping ones not installed`() {
        val installed = setOf("com.discord", "com.whatsapp", "com.google.android.gm", "com.spotify.music")
        assertEquals(
            listOf("com.whatsapp", "com.spotify.music", "com.discord", "com.google.android.gm"),
            Pins.pick(Pins.FAVORITE_PACKAGES, installed),
        )
    }

    @Test fun `never picks more than the limit`() {
        val installed = Pins.FAVORITE_PACKAGES.toSet()
        assertEquals(4, Pins.pick(Pins.FAVORITE_PACKAGES, installed).size)
    }

    @Test fun `excluded apps are skipped so quick page doesn't repeat home`() {
        val installed = setOf("com.phonepe.app", "com.google.android.gm", "com.google.android.youtube")
        assertEquals(
            listOf("com.phonepe.app", "com.google.android.youtube"),
            Pins.pick(Pins.QUICK_PACKAGES, installed, exclude = setOf("com.google.android.gm")),
        )
    }

    @Test fun `nothing installed gives an empty list`() {
        assertEquals(emptyList<String>(), Pins.pick(Pins.FAVORITE_PACKAGES, emptySet()))
    }

    @Test fun `toggle adds, removes, and refuses when full`() {
        val (added, o1) = Pins.toggle(listOf("a"), "b")
        assertEquals(listOf("a", "b") to Pins.Outcome.Added, added to o1)

        val (removed, o2) = Pins.toggle(listOf("a", "b"), "a")
        assertEquals(listOf("b") to Pins.Outcome.Removed, removed to o2)

        val full = listOf("a", "b", "c", "d")
        assertEquals(full to Pins.Outcome.Full, Pins.toggle(full, "e"))
    }
}
