package dev.rahmanfaizur.dotline.data

import android.content.ComponentName
import android.content.pm.LauncherActivityInfo
import android.os.UserHandle

/** One launchable app (an app can appear twice if it's also in a work profile). */
class AppEntry(
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    internal val info: LauncherActivityInfo,
) {
    /** Stable id: same app + same profile → same key. */
    val key: String = "${component.flattenToShortString()}#${user.hashCode()}"

    override fun equals(other: Any?) = other is AppEntry && other.key == key
    override fun hashCode() = key.hashCode()
    override fun toString() = "AppEntry($label)"
}
