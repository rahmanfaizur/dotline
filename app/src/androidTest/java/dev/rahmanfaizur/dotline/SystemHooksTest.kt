package dev.rahmanfaizur.dotline

import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Checks that Android can find the pieces Dotline offers outside its own screens. */
@RunWith(AndroidJUnit4::class)
class SystemHooksTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val pm = context.packageManager

    private fun servicesFor(action: String) =
        pm.queryIntentServices(Intent(action).setPackage(context.packageName), PackageManager.GET_META_DATA)
            .map { it.serviceInfo.name.substringAfterLast('.') }

    @Test fun panelTilesAreOffered() {
        val tiles = servicesFor("android.service.quicksettings.action.QS_TILE")
        assertTrue(tiles.toString(), "WallpaperTile" in tiles && "RamTile" in tiles)
    }

    @Test fun screensaverIsOffered() {
        assertTrue("DotClockDream" in servicesFor("android.service.dreams.DreamService"))
    }

    @Test fun nightClockOpensAndClosesOnTap() {
        androidx.test.core.app.ActivityScenario.launch(dev.rahmanfaizur.dotline.clock.NightClockActivity::class.java).use { sc ->
            assertTrue(sc.state.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED))
            sc.onActivity {
                val content = it.findViewById<android.view.ViewGroup>(android.R.id.content)
                content.getChildAt(0).performClick()
                assertTrue(it.isFinishing) // a tap closes it, back to wherever you were
            }
        }
    }

    private fun shell(cmd: String) {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(cmd).close()
    }

    private fun waitFor(ms: Long = 8_000, check: () -> Boolean): Boolean {
        val end = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < end) { if (check()) return true; Thread.sleep(100) }
        return check()
    }

    @Test fun notificationDotsOnlyWhileSwitchedOn() {
        val app = context.applicationContext as DotlineApp
        val listener = "${context.packageName}/${context.packageName}.notify.DotlineNotifications"
        kotlinx.coroutines.runBlocking {
            app.settings.setDotApps(setOf("com.android.shell"))
            app.settings.setNotificationDots(true)
        }
        try {
            shell("cmd notification allow_listener $listener")
            shell("cmd notification post -t Hello dotline_test Test")
            assertTrue("dot should appear", waitFor { "com.android.shell" in app.notifications.active.value })
            kotlinx.coroutines.runBlocking { app.settings.setNotificationDots(false) }
            assertTrue("switching off clears everything", waitFor { app.notifications.active.value.isEmpty() })
        } finally {
            kotlinx.coroutines.runBlocking { app.settings.setNotificationDots(false); app.settings.setDotApps(emptySet()) }
            shell("cmd notification disallow_listener $listener")
        }
    }
}
