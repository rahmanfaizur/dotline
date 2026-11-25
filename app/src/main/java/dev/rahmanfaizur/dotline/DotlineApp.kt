package dev.rahmanfaizur.dotline

import android.app.Application
import dev.rahmanfaizur.dotline.data.AppRepository
import dev.rahmanfaizur.dotline.data.IconLoader
import kotlinx.coroutines.MainScope

/** Holds the long-lived pieces so they survive the activity being recreated. */
class DotlineApp : Application() {
    private val appScope = MainScope()

    lateinit var apps: AppRepository
        private set
    lateinit var icons: IconLoader
        private set

    override fun onCreate() {
        super.onCreate()
        apps = AppRepository(this, appScope).also { it.start() }
        icons = IconLoader(this)
    }
}
