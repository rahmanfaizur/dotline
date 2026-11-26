package dev.rahmanfaizur.dotline.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchTest {
    @Test fun findsSettingsByStartOfAWord() {
        assertEquals("Wi-Fi", SettingsSearch.match("wif").first().label)
        assertEquals("Bluetooth", SettingsSearch.match("blue").first().label)
        assertEquals("Battery", SettingsSearch.match("BATT").first().label)
        assertEquals("Display & brightness", SettingsSearch.match("bright").first().label)
    }

    @Test fun needsTwoLettersAndAtMostTwoResults() {
        assertTrue(SettingsSearch.match("w").isEmpty())
        assertTrue(SettingsSearch.match("zzz").isEmpty())
        assertTrue(SettingsSearch.match("s").size <= 2 && SettingsSearch.match("so").size <= 2)
    }
}
