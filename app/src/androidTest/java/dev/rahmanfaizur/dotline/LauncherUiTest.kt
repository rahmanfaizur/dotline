package dev.rahmanfaizur.dotline

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import org.junit.Assert.assertNotEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.rahmanfaizur.dotline.data.TileStyle
import dev.rahmanfaizur.dotline.data.WallpaperKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the real launcher on a device or emulator.
 * Relies on Chrome being installed, which it is on every Google emulator image, and on
 * the debug build not being the default home app (so the set-home tile shows).
 */
@RunWith(AndroidJUnit4::class)
class LauncherUiTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    /** An app row inside the drawer (the same app may also be a tile on the home screen underneath). */
    private fun inDrawer(text: String) = rule.onNode(hasText(text) and hasAnyAncestor(hasTestTag("drawer")))

    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as DotlineApp

    /** Every test starts with the set-home tile not dismissed, and leaves it that way. */
    @Before @After fun resetHomePrompt() {
        runBlocking {
            app.settings.setHomePromptDismissed(false)
            app.settings.setWallpaper(WallpaperKind.Black)
            app.settings.setTileStyle(TileStyle.Smoke)
            app.settings.setDrawerBackground(dev.rahmanfaizur.dotline.data.DrawerBackground.Frosted)
        }
    }

    private fun openSearch() {
        rule.onNodeWithTag("searchPill").performClick()
        rule.onNodeWithTag("drawer").assertIsDisplayed()
    }

    @Test fun homeShowsClockAndSearch() {
        rule.onNodeWithTag("clock").assertIsDisplayed()
        rule.onNodeWithTag("searchPill").assertIsDisplayed()
    }

    @Test fun swipeUpOpensAllApps() {
        rule.onNodeWithTag("home").performTouchInput { swipeUp() }
        rule.onNodeWithTag("drawer").assertIsDisplayed()
    }

    @Test fun searchRanksMatchingAppFirst() {
        openSearch()
        rule.onNodeWithTag("searchField").performTextInput("chr")
        inDrawer("Chrome").assertIsDisplayed()
        rule.onNodeWithText("Enter to open").assertIsDisplayed()
    }

    @Test fun unknownSearchSaysNoMatch() {
        openSearch()
        rule.onNodeWithTag("searchField").performTextInput("zzqxv")
        rule.onNodeWithText("No apps match").assertIsDisplayed()
    }

    @Test fun longPressAppShowsMenu() {
        openSearch()
        rule.onNodeWithTag("searchField").performTextInput("chr")
        inDrawer("Chrome").performTouchInput { longClick() }
        rule.onNodeWithTag("appMenu").assertIsDisplayed()
        rule.onNodeWithText("App info").assertIsDisplayed()
    }

    @Test fun longPressEmptyHomeOpensSettings() {
        // Empty space between the app tiles and the search pill.
        rule.onNodeWithTag("home").performTouchInput { longClick(Offset(width * 0.5f, height * 0.7f)) }
        rule.onNodeWithTag("settings").assertIsDisplayed()
        rule.onNodeWithText("Rounded square").performScrollTo().assertIsDisplayed()
    }

    @Test fun swipeRightShowsQuickPageWithSettingsLink() {
        rule.onNodeWithTag("home").performTouchInput { swipeRight() }
        rule.onNodeWithTag("quickPage").assertIsDisplayed()
        rule.onNodeWithTag("openSettings").performClick()
        rule.onNodeWithTag("settings").assertIsDisplayed()
    }

    @Test fun setHomeTileShowsUntilNotNow() {
        // The tile waits for saved settings to load, so it can appear a moment after launch.
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("setHomeTile").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("setHomeTile").assertIsDisplayed()
        rule.onNodeWithTag("setHome").assertIsDisplayed()
        rule.onNodeWithTag("notNow").performClick()
        rule.onNodeWithTag("setHomeTile").assertDoesNotExist()
    }

    @Test fun settingsOffersSetAsHome() {
        rule.onNodeWithTag("home").performTouchInput { longClick(Offset(width * 0.5f, height * 0.7f)) }
        rule.onNodeWithTag("setAsHome").assertIsDisplayed()
    }

    @Test fun backOnHomeKeepsLauncherOpen() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        assertFalse(rule.activity.isFinishing)
        rule.onNodeWithTag("clock").assertIsDisplayed()
    }

    private fun sectionLetter() =
        rule.onNodeWithTag("sectionLetter").fetchSemanticsNode().config[SemanticsProperties.Text].first().text

    @Test fun draggingAlongLettersMovesTheList() {
        rule.onNodeWithTag("home").performTouchInput { swipeUp() }
        rule.onNodeWithTag("drawer").assertIsDisplayed()
        val first = sectionLetter()
        // Finger down on the first letter, slide across and down to the last row (still holding).
        rule.onNodeWithTag("letterStrip").performTouchInput {
            down(Offset(30f, 30f))
            moveTo(Offset(width / 2f, 30f))
            moveTo(Offset(width - 30f, height - 30f))
        }
        rule.waitForIdle()
        assertNotEquals(first, sectionLetter())
        rule.onNodeWithTag("letterStrip").performTouchInput { up() }
    }

    @Test fun lookSectionChangesWallpaperAndTiles() {
        rule.onNodeWithTag("home").performTouchInput { longClick(Offset(width * 0.5f, height * 0.7f)) }
        rule.onNodeWithTag("lookPreview").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("wallpaper:Sunset").performScrollTo().performClick()
        rule.onNodeWithTag("tiles:Clear glass").performScrollTo().performClick()
        rule.waitUntil(5_000) {
            val now = runBlocking { app.settings.settings.first() }
            now.wallpaper == WallpaperKind.Sunset && now.tileStyle == TileStyle.Glass
        }
    }

    @Test fun lockScreenOpensPhoneWallpaperScreenOrSetsDirectly() = runBlocking {
        val intent = app.wallpapers.lockScreenIntent(WallpaperKind.DotField, 0xFFE8453C.toInt(), 108, 240)
        if (intent != null) {
            // The phone has its own "set wallpaper" screen: it must be able to open our image.
            assertTrue(intent.resolveActivity(app.packageManager) != null)
            assertTrue(intent.flags and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        }
        val wm = android.app.WallpaperManager.getInstance(app)
        val before = wm.getWallpaperId(android.app.WallpaperManager.FLAG_LOCK)
        assertTrue(app.wallpapers.setLockScreen(WallpaperKind.DotField, 0xFFE8453C.toInt(), 108, 240))
        assertNotEquals(before, wm.getWallpaperId(android.app.WallpaperManager.FLAG_LOCK))
    }

    @Test fun appListWorksOnEveryBackground() {
        runBlocking { app.settings.setWallpaper(WallpaperKind.Sunset) }
        for (bg in dev.rahmanfaizur.dotline.data.DrawerBackground.entries) {
            runBlocking { app.settings.setDrawerBackground(bg) }
            rule.onNodeWithTag("searchPill").performClick()
            rule.onNodeWithTag("searchField").performTextInput("chr")
            inDrawer("Chrome").assertIsDisplayed()
            rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
            rule.waitForIdle()
        }
    }
}
