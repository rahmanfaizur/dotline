package dev.rahmanfaizur.dotline.data

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings

/** What tapping a home or quick-page tile opens. */
enum class Shortcut { Clock, Alarms, Battery, Calendar, Storage, Memory }

/**
 * Opens the right app or settings screen for a tile, trying the best choice first:
 * e.g. storage opens Files by Google, then the phone's own storage settings.
 */
class Shortcuts(context: Context) {
    private val app = context.applicationContext
    private val pm = app.packageManager

    fun open(shortcut: Shortcut): Boolean =
        candidates(shortcut).any { intent ->
            runCatching { app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
        }

    /** True if at least one way to open [shortcut] exists on this phone. */
    fun available(shortcut: Shortcut): Boolean =
        candidates(shortcut).any { it.resolveActivity(pm) != null }

    private fun candidates(s: Shortcut): List<Intent> = when (s) {
        // The clock app's main screen; its alarm list if it has no launcher entry.
        Shortcut.Clock -> listOfNotNull(clockPackage()?.let(pm::getLaunchIntentForPackage), showAlarms())
        Shortcut.Alarms -> listOf(showAlarms())
        Shortcut.Battery -> listOf(Intent(Intent.ACTION_POWER_USAGE_SUMMARY), Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
        // Today in the default calendar app.
        Shortcut.Calendar -> listOfNotNull(
            Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(
                CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build(),
                System.currentTimeMillis(),
            )),
            pm.getLaunchIntentForPackage(GOOGLE_CALENDAR),
        )
        Shortcut.Storage -> listOfNotNull(
            pm.getLaunchIntentForPackage(FILES_BY_GOOGLE),
            Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
        )
        // "About phone" shows the phone's RAM on most Android skins, Realme included.
        Shortcut.Memory -> listOf(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS), Intent(Settings.ACTION_SETTINGS))
    }

    private fun showAlarms() = Intent(AlarmClock.ACTION_SHOW_ALARMS)

    private fun clockPackage(): String? =
        pm.resolveActivity(showAlarms(), 0)?.activityInfo?.packageName?.takeIf { it != "android" }

    private companion object {
        const val FILES_BY_GOOGLE = "com.google.android.apps.nbu.files"
        const val GOOGLE_CALENDAR = "com.google.android.calendar"
    }
}
