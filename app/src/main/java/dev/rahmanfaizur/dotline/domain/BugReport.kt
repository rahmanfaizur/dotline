package dev.rahmanfaizur.dotline.domain

import dev.rahmanfaizur.dotline.data.LauncherSettings

/**
 * The text a "Report a problem" share contains: what the person wrote, Dotline's version, the
 * phone model and Android version, and which features are switched on. Never which apps are
 * installed, pinned or hidden, never folder contents, never anything that identifies the person.
 */
object BugReport {
    const val ISSUES = "github.com/rahmanfaizur/dotline/issues"

    fun build(note: String, appVersion: String, phone: String, android: String, s: LauncherSettings): String {
        fun on(b: Boolean) = if (b) "on" else "off"
        val what = note.trim().take(1000).ifBlank { "(not described)" }
        return buildString {
            appendLine("Dotline problem report")
            appendLine()
            appendLine("What went wrong: $what")
            appendLine()
            appendLine("Dotline $appVersion · $phone · Android $android")
            appendLine("Look: wallpaper ${s.wallpaper.name}, tiles ${s.tileStyle.name}, text ${s.textTone.name}, app list ${s.drawerBackground.name}, icons ${s.iconStyle.name}, clock ${s.clockStyle.name}")
            appendLine("Home: ${s.homeApps} apps, compact ${on(s.compact)}, folders ${on(s.foldersOn)} (${s.folders.size}), quick page ${on(s.showQuickPage)}")
            appendLine("Extras: dots ${on(s.notificationDots)}, now playing ${on(s.nowPlaying)} (${s.playerStyle.name}, card ${on(s.playerCard)}), swipe down ${on(s.swipeDownNotifications)}, smart search ${on(s.smartSearch)}, phone wallpaper ${on(s.phoneWallpaper)}")
            appendLine()
            append("Also on GitHub: $ISSUES")
        }
    }
}
