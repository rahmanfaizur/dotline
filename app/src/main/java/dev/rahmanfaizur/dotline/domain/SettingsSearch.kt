package dev.rahmanfaizur.dotline.domain

import android.provider.Settings

/** A phone setting search can jump to. [action] is the Android settings screen to open. */
data class SettingLink(val label: String, val action: String, val words: List<String>)

/** Typing "wifi", "blue", "batt"… in search offers the matching phone setting. */
object SettingsSearch {
    val all = listOf(
        SettingLink("Wi-Fi", Settings.ACTION_WIFI_SETTINGS, listOf("wifi", "wi-fi", "wlan", "internet")),
        SettingLink("Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS, listOf("bluetooth", "bt", "headphones", "earbuds")),
        SettingLink("Mobile data", Settings.ACTION_DATA_ROAMING_SETTINGS, listOf("data", "mobile", "sim", "network")),
        SettingLink("Hotspot", Settings.ACTION_WIRELESS_SETTINGS, listOf("hotspot", "tether")),
        SettingLink("Battery", Intent_POWER_USAGE, listOf("battery", "power", "charge")),
        SettingLink("Display & brightness", Settings.ACTION_DISPLAY_SETTINGS, listOf("display", "brightness", "screen", "dark")),
        SettingLink("Sound & volume", Settings.ACTION_SOUND_SETTINGS, listOf("sound", "volume", "ringtone", "vibrat")),
        SettingLink("Storage", Settings.ACTION_INTERNAL_STORAGE_SETTINGS, listOf("storage", "space", "memory")),
        SettingLink("Apps", Settings.ACTION_APPLICATION_SETTINGS, listOf("apps", "applications")),
        SettingLink("Notifications", "android.settings.NOTIFICATION_SETTINGS", listOf("notification", "notif")),
        SettingLink("Location", Settings.ACTION_LOCATION_SOURCE_SETTINGS, listOf("location", "gps")),
        SettingLink("Date & time", Settings.ACTION_DATE_SETTINGS, listOf("date", "time", "clock")),
        SettingLink("Airplane mode", Settings.ACTION_AIRPLANE_MODE_SETTINGS, listOf("airplane", "flight", "aeroplane")),
        SettingLink("Security & privacy", Settings.ACTION_SECURITY_SETTINGS, listOf("security", "privacy", "lock", "fingerprint")),
        SettingLink("Accessibility", Settings.ACTION_ACCESSIBILITY_SETTINGS, listOf("accessibility")),
        SettingLink("All settings", Settings.ACTION_SETTINGS, listOf("settings")),
    )

    /** Up to [max] settings whose words start with what was typed (at least 2 letters). */
    fun match(query: String, max: Int = 2): List<SettingLink> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        return all.filter { link -> link.words.any { it.startsWith(q) } || link.label.lowercase().startsWith(q) }.take(max)
    }
}

/** Battery usage screen (an Intent action string that isn't in every SDK's Settings class). */
private const val Intent_POWER_USAGE = "android.intent.action.POWER_USAGE_SUMMARY"
