package dev.rahmanfaizur.dotline.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Parts of Dotline that live outside the app; switching one off hides it from Android entirely. */
enum class Feature(val className: String) {
    WallpaperTile("dev.rahmanfaizur.dotline.tiles.WallpaperTile"),
    RamTile("dev.rahmanfaizur.dotline.tiles.RamTile"),
    Screensaver("dev.rahmanfaizur.dotline.clock.DotClockDream"),
}

class Features(context: Context) {
    private val app = context.applicationContext
    private val pm = app.packageManager

    private val _state = MutableStateFlow(read())
    val state: StateFlow<Map<Feature, Boolean>> = _state

    private fun component(f: Feature) = ComponentName(app.packageName, f.className)

    private fun read() = Feature.entries.associateWith { f ->
        when (pm.getComponentEnabledSetting(component(f))) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
            else -> true // default = on, as declared in the manifest
        }
    }

    fun set(f: Feature, on: Boolean) {
        pm.setComponentEnabledSetting(
            component(f),
            if (on) PackageManager.COMPONENT_ENABLED_STATE_DEFAULT else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
        _state.value = read()
    }
}
