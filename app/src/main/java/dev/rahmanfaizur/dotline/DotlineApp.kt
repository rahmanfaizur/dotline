package dev.rahmanfaizur.dotline

import android.app.Application
import dev.rahmanfaizur.dotline.data.AppRepository
import dev.rahmanfaizur.dotline.data.IconLoader
import dev.rahmanfaizur.dotline.data.SettingsStore
import dev.rahmanfaizur.dotline.data.Shortcuts
import dev.rahmanfaizur.dotline.data.SystemInfo
import dev.rahmanfaizur.dotline.data.Wallpapers
import kotlinx.coroutines.MainScope

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

    override fun onCreate() {
        super.onCreate()
        apps = AppRepository(this, appScope).also { it.start() }
        icons = IconLoader(this)
        settings = SettingsStore(this)
        system = SystemInfo(this)
        wallpapers = Wallpapers(this)
        shortcuts = Shortcuts(this)
    }
}
