package dev.rahmanfaizur.dotline

import android.app.Application
import dev.rahmanfaizur.dotline.data.AppRepository
import dev.rahmanfaizur.dotline.data.IconLoader
import dev.rahmanfaizur.dotline.data.SettingsStore
import dev.rahmanfaizur.dotline.data.Shortcuts
import dev.rahmanfaizur.dotline.data.Features
import dev.rahmanfaizur.dotline.data.SystemInfo
import dev.rahmanfaizur.dotline.data.Wallpapers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import dev.rahmanfaizur.dotline.notify.DotlineNotifications
import dev.rahmanfaizur.dotline.notify.NotificationHub

/** Holds the long-lived pieces so they survive the activity being recreated. */
class DotlineApp : Application() {
    val appScope = MainScope()

    lateinit var apps: AppRepository
        private set
    lateinit var icons: IconLoader
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var system: SystemInfo
        private set
    lateinit var wallpapers: Wallpapers
        private set
    lateinit var shortcuts: Shortcuts
        private set
    lateinit var features: Features
        private set
    val notifications = NotificationHub()

    override fun onCreate() {
        super.onCreate()
        apps = AppRepository(this, appScope).also { it.start() }
        icons = IconLoader(this)
        settings = SettingsStore(this)
        system = SystemInfo(this)
        wallpapers = Wallpapers(this)
        shortcuts = Shortcuts(this)
        features = Features(this)
        // The listener only works while Dots or Now playing is switched on.
        appScope.launch {
            settings.settings.map { it.notificationDots || it.nowPlaying }.distinctUntilChanged().collect { on ->
                notifications.wanted = on
                if (on) DotlineNotifications.instance?.refresh() else notifications.clear()
            }
        }
    }
}
