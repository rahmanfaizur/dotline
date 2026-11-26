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
import androidx.compose.ui.test.swipeDown
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
            dev.rahmanfaizur.dotline.data.Feature.entries.forEach { app.features.set(it, true) }
            app.settings.setDrawerBackground(dev.rahmanfaizur.dotline.data.DrawerBackground.Frosted)
            app.settings.setHomeOrder(dev.rahmanfaizur.dotline.data.HomeBlock.Default)
        }
    }

    private fun openSearch() {
        rule.onNodeWithTag("searchPill").performClick()
        rule.onNodeWithTag("drawer").assertIsDisplayed()
    }

    @Test fun homeShowsClockAndSearch() {
        rule.onNodeWithTag("clock", useUnmergedTree = true).assertIsDisplayed()
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
        rule.onNodeWithTag("editSettings").performClick()
        rule.onNodeWithTag("settings").assertIsDisplayed()
        rule.onNodeWithTag("page:Look").performClick()
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
        rule.onNodeWithTag("editSettings").performClick()
        rule.onNodeWithTag("setAsHome").assertIsDisplayed()
    }

    @Test fun backOnHomeKeepsLauncherOpen() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        assertFalse(rule.activity.isFinishing)
        rule.onNodeWithTag("clock", useUnmergedTree = true).assertIsDisplayed()
    }

    private fun sectionLetter() =
        rule.onNodeWithTag("sectionLetter").fetchSemanticsNode().config[SemanticsProperties.Text].first().text

    @Test fun draggingTheScrollHandleFliesThroughApps() {
        rule.onNodeWithTag("home").performTouchInput { swipeUp() }
        rule.onNodeWithTag("drawer").assertIsDisplayed()
        // The handle only appears when the list is longer than the screen.
        org.junit.Assume.assumeTrue(rule.onAllNodesWithTag("fastScroller").fetchSemanticsNodes().isNotEmpty())
        val first = sectionLetter()
        rule.onNodeWithTag("fastScroller").performTouchInput {
            down(Offset(width / 2f, 4f))
            moveTo(Offset(width / 2f, height / 2f))
            moveTo(Offset(width / 2f, height - 4f))
        }
        rule.waitForIdle()
        assertNotEquals(first, sectionLetter())
        rule.onNodeWithTag("railBubble").assertExists() // the letter bubble shows while dragging
        rule.onNodeWithTag("fastScroller").performTouchInput { up() }
        rule.waitUntil(3_000) { rule.onAllNodesWithTag("railBubble").fetchSemanticsNodes().isEmpty() }
    }

    @Test fun lookSectionChangesWallpaperAndTiles() {
        rule.onNodeWithTag("home").performTouchInput { longClick(Offset(width * 0.5f, height * 0.7f)) }
        rule.onNodeWithTag("editSettings").performClick()
        rule.onNodeWithTag("page:Look").performClick()
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

    @Test fun everyTileHasSomethingToOpen() {
        dev.rahmanfaizur.dotline.data.Shortcut.entries.forEach { assertTrue("$it", app.shortcuts.available(it)) }
    }

    @Test fun quickPageShowsLiveRam() {
        rule.onNodeWithTag("home").performTouchInput { swipeRight() }
        // The tile is one tappable item, so its texts are merged into it.
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("ramTile") and hasText("%", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        // Captions are shown in capitals ("2.1 GB OF 4.1 GB").
        rule.waitUntil(5_000) {
            rule.onAllNodes(hasTestTag("ramTile") and hasText(" of ", substring = true, ignoreCase = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun pullingDownClosesAllApps() {
        rule.onNodeWithTag("home").performTouchInput { swipeUp() }
        rule.onNodeWithTag("drawer").assertIsDisplayed()
        rule.onNodeWithTag("sectionLetter").performTouchInput { swipeDown(startY = 0f, endY = 900f, durationMillis = 250) }
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("drawer").fetchSemanticsNodes().isEmpty() }
    }

    private fun openSettings() {
        rule.onNodeWithTag("home").performTouchInput { longClick(Offset(width * 0.5f, height * 0.7f)) }
        rule.onNodeWithTag("editSettings").performClick()
        rule.onNodeWithTag("settings").assertIsDisplayed()
    }

    @Test fun settingsPagesOpenAndComeBack() {
        openSettings()
        for (page in listOf("Look", "Home", "Lock", "Panel", "Notifications", "Hidden", "About")) {
            rule.onNodeWithTag("page:$page").performClick()
            rule.onNodeWithTag("settingsBack").assertIsDisplayed()
            rule.onNodeWithTag("settingsBack").performClick()
        }
        rule.onNodeWithTag("page:Home").performClick()
        rule.onNodeWithTag("changeHome").performScrollTo().assertIsDisplayed() // always a way to switch launcher
    }

    @Test fun dragTheClockGripMovesItDown() {
        rule.onNodeWithTag("home").performTouchInput { longClick(Offset(width * 0.5f, height * 0.7f)) }
        rule.onNodeWithTag("editDone").assertIsDisplayed()
        rule.onNodeWithTag("handle:Clock").performTouchInput {
            down(center)
            repeat(60) { moveBy(Offset(0f, 25f)) }
            up()
        }
        rule.waitUntil(5_000) {
            // One long drag carries it past both the battery row and the apps (it must not stop after one).
            runBlocking { app.settings.settings.first().homeOrder.indexOf(dev.rahmanfaizur.dotline.data.HomeBlock.Clock) } >= 2
        }
        rule.onNodeWithTag("editDone").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("editDone").fetchSemanticsNodes().isEmpty() }
    }

    @Test fun dragAnAppOntoAnotherSwapsThem() {
        val before = runBlocking { app.settings.settings.first() }
        rule.onNodeWithTag("home").performTouchInput { longClick(Offset(width * 0.5f, height * 0.7f)) }
        val target = rule.onNodeWithTag("slot:home:1").fetchSemanticsNode().boundsInRoot.center
        val start = rule.onNodeWithTag("slot:home:0").fetchSemanticsNode().boundsInRoot.center
        rule.onNodeWithTag("slot:home:0").performTouchInput {
            down(center)
            val step = (target - start) / 20f
            repeat(20) { moveBy(step) }
            up()
        }
        rule.waitUntil(5_000) { runBlocking { app.settings.settings.first().favorites } != before.favorites }
        val after = runBlocking { app.settings.settings.first().favorites }!!.toMutableList()
        before.favorites?.let { b -> assertTrue("first two home apps swapped", after[0] == b[1] && after[1] == b[0]) }
        // Put them back.
        after[0] = after[1].also { after[1] = after[0] }
        runBlocking { app.settings.setFavorites(after) }
    }

    @Test fun tilesStayReadableOnABrightWallpaper() {
        // Optional pause per text setting for screenshots (-e holdMs 4000).
        val hold = InstrumentationRegistry.getArguments().getString("holdMs")?.toLongOrNull() ?: 0L
        runBlocking {
            app.settings.setWallpaper(WallpaperKind.Sunset)
            app.settings.setTileStyle(TileStyle.Outline)
        }
        try {
            for (tone in dev.rahmanfaizur.dotline.data.TextTone.entries) {
                runBlocking { app.settings.setTextTone(tone) }
                rule.waitForIdle()
                rule.onNodeWithTag("clockTile").assertIsDisplayed()
                if (hold > 0) Thread.sleep(hold)
            }
        } finally {
            runBlocking { app.settings.setTextTone(dev.rahmanfaizur.dotline.data.TextTone.Auto) }
        }
    }

    @Test fun comingBackAlwaysLandsOnHome() {
        rule.onNodeWithTag("home").performTouchInput { swipeRight() }
        rule.onNodeWithTag("quickPage").assertIsDisplayed()
        // Leave (like opening Recents or an app) and come back.
        rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("clockTile").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("clockTile").assertIsDisplayed()
    }

    @Test fun phoneWallpaperFollowsDotline() {
        val wm = android.app.WallpaperManager.getInstance(app)
        val before = wm.getWallpaperId(android.app.WallpaperManager.FLAG_SYSTEM)
        runBlocking { app.settings.setWallpaper(WallpaperKind.Glow) }
        rule.waitUntil(10_000) { wm.getWallpaperId(android.app.WallpaperManager.FLAG_SYSTEM) != before }
    }

    @Test fun lockPageShowsLockAndNightClockPreviews() {
        openSettings()
        rule.onNodeWithTag("page:Lock").performClick()
        rule.onNodeWithTag("lockPreview").assertIsDisplayed()
        rule.onNodeWithTag("nightClockPreview").assertIsDisplayed()
    }

    @Test fun nowPlayingSettingsPreviewEveryPlayerStyle() {
        runBlocking { app.settings.setNowPlaying(true) }
        try {
            openSettings()
            rule.onNodeWithTag("page:Notifications").performClick()
            for (name in listOf("Cover glow", "Dot wave", "Vinyl")) {
                rule.onNodeWithTag("player:$name").performScrollTo().performClick()
                rule.onNodeWithTag("playerPreview").performScrollTo().assertIsDisplayed()
                rule.onAllNodes(hasText("Sample song", ignoreCase = true), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty().let(::assertTrue)
            }
        } finally {
            runBlocking {
                app.settings.setNowPlaying(false)
                app.settings.setPlayerStyle(dev.rahmanfaizur.dotline.data.PlayerStyle.Glow)
            }
        }
    }

    @Test fun panelTileSwitchRemovesItFromAndroid() {
        fun ramTileOffered() = app.packageManager.queryIntentServices(
            android.content.Intent("android.service.quicksettings.action.QS_TILE").setPackage(app.packageName), 0,
        ).any { it.serviceInfo.name.endsWith("RamTile") }
        assertTrue(ramTileOffered())
        openSettings()
        rule.onNodeWithTag("page:Panel").performClick()
        rule.onNodeWithTag("toggle:ramTile").performClick()
        rule.waitUntil(5_000) { !ramTileOffered() }
        rule.onNodeWithTag("toggle:ramTile").performClick()
        rule.waitUntil(5_000) { ramTileOffered() }
    }
}
