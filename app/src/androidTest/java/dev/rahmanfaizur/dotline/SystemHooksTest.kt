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
}
