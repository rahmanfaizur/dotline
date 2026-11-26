package dev.rahmanfaizur.dotline.data

import android.annotation.SuppressLint
import android.content.Context

/**
 * Pulls down the notification panel, like swiping down from the top. Every launcher does this the
 * same way; it needs only the harmless "expand status bar" permission and can't read anything.
 */
object StatusBar {
    @SuppressLint("WrongConstant")
    fun expandNotifications(context: Context): Boolean = runCatching {
        val sb = context.getSystemService("statusbar") ?: return false
        sb.javaClass.getMethod("expandNotificationsPanel").invoke(sb)
        true
    }.getOrDefault(false)
}
