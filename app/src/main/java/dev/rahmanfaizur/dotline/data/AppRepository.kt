package dev.rahmanfaizur.dotline.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.MediaStore
import android.provider.Telephony
import dev.rahmanfaizur.dotline.domain.Pins
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Default pinned apps, as component names ("pkg/.Activity"). */
data class DefaultPins(val favorites: List<String>, val dock: List<String>, val quick: List<String>)

/**
 * Installed, launchable apps via LauncherApps, the API Android gives home screens.
 * Needs no permission and keeps itself up to date when apps are installed or removed.
 */
class AppRepository(private val context: Context, private val scope: CoroutineScope) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val packageManager = context.packageManager

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private var loadJob: Job? = null

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
    }

    fun start() {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = scope.launch(Dispatchers.Default) { _apps.value = load() }
    }

    private fun load(): List<AppEntry> =
        userManager.userProfiles
            .flatMap { user -> launcherApps.getActivityList(null, user) }
            .filter { it.componentName.packageName != context.packageName }
            .map { AppEntry(it.label.toString().trim(), it.componentName, it.user, it) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })

    fun launch(app: AppEntry, sourceBounds: Rect? = null, options: Bundle? = null) {
        launcherApps.startMainActivity(app.component, app.user, sourceBounds, options)
    }

    fun openAppInfo(app: AppEntry) {
        launcherApps.startAppDetailsActivity(app.component, app.user, null, null)
    }

    /** Opens Android's own "Uninstall this app?" confirmation. */
    fun requestUninstall(app: AppEntry) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.component.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(Intent.EXTRA_USER, app.user)
        context.startActivity(intent)
    }

    /** Built-in apps can't be uninstalled, only disabled from App info. */
    fun isSystemApp(app: AppEntry): Boolean =
        app.info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0

    /** Defaults used until the user customises each list. Only main-profile apps. */
    fun defaults(apps: List<AppEntry>): DefaultPins {
        val me = Process.myUserHandle()
        val byPackage = apps.filter { it.user == me }.associateBy { it.component.packageName }
        val installed = byPackage.keys
        fun components(packages: List<String>) = packages.mapNotNull { byPackage[it]?.component?.flattenToString() }

        // Enough for the largest home (8); home shows as many as Settings says.
        val favorites = Pins.pick(Pins.FAVORITE_PACKAGES, installed, count = 8)
        val dock = listOfNotNull(
            defaultPackage(Intent(Intent.ACTION_DIAL)),
            Telephony.Sms.getDefaultSmsPackage(context),
            "com.brave.browser".takeIf { it in installed }
                ?: defaultPackage(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))),
            defaultPackage(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)),
        ).filter { it in installed }.distinct().take(Pins.MAX)
        val quick = Pins.pick(Pins.QUICK_PACKAGES, installed, exclude = (favorites + dock).toSet())

        return DefaultPins(components(favorites), components(dock), components(quick))
    }

    /** Package that handles [intent] by default; null if the user hasn't picked one. */
    private fun defaultPackage(intent: Intent): String? =
        packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
            ?.takeIf { it != "android" } // "android" = the "Open with" chooser
}
