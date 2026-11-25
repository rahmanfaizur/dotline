package dev.rahmanfaizur.dotline.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDotsTest {
    private val installed = setOf("com.spotify.music", "com.discord", "com.whatsapp", "com.example.bank")

    @Test fun defaultsAreOnlyTheInstalledOnes() {
        assertEquals(setOf("com.spotify.music", "com.discord", "com.whatsapp"), NotificationDots.chosen(null, installed))
    }

    @Test fun ownPickReplacesDefaults() {
        assertEquals(setOf("com.discord"), NotificationDots.chosen(setOf("com.discord"), installed))
    }

    @Test fun offShowsNothing() {
        assertTrue(NotificationDots.visible(false, setOf("com.discord"), setOf("com.discord")).isEmpty())
    }

    @Test fun onlyChosenAppsWithNotificationsGetDots() {
        val active = setOf("com.discord", "com.example.bank") // a bank app notifying is never shown unless chosen
        assertEquals(setOf("com.discord"), NotificationDots.visible(true, active, setOf("com.discord", "com.whatsapp")))
    }
}
