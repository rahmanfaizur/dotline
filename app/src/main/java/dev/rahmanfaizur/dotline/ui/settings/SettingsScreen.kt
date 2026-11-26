package dev.rahmanfaizur.dotline.ui.settings

import androidx.compose.foundation.background
import dev.rahmanfaizur.dotline.ui.home.PlayerActions
import dev.rahmanfaizur.dotline.ui.home.NowPlayingTile
import dev.rahmanfaizur.dotline.notify.NowPlaying
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.remember
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rahmanfaizur.dotline.data.AppEntry
import dev.rahmanfaizur.dotline.data.ClockMode
import dev.rahmanfaizur.dotline.data.IconShape
import dev.rahmanfaizur.dotline.data.LauncherSettings
import dev.rahmanfaizur.dotline.ui.AppIcon
import dev.rahmanfaizur.dotline.ui.common.Caption
import dev.rahmanfaizur.dotline.ui.common.Gap
import dev.rahmanfaizur.dotline.ui.theme.Dot
import dev.rahmanfaizur.dotline.ui.theme.LocalAccent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import dev.rahmanfaizur.dotline.data.DrawerBackground
import dev.rahmanfaizur.dotline.data.Feature
import dev.rahmanfaizur.dotline.data.PlayerStyle
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.rahmanfaizur.dotline.data.TextTone
import dev.rahmanfaizur.dotline.data.TileStyle
import dev.rahmanfaizur.dotline.ui.common.useDarkText
import dev.rahmanfaizur.dotline.ui.theme.LocalDarkText
import androidx.compose.runtime.CompositionLocalProvider
import dev.rahmanfaizur.dotline.data.WallpaperKind
import dev.rahmanfaizur.dotline.ui.common.Backdrop
import dev.rahmanfaizur.dotline.ui.common.WallpaperArea
import dev.rahmanfaizur.dotline.ui.common.tile

class SettingsLists(
    val favorites: List<AppEntry>,
    val dock: List<AppEntry>,
    val quick: List<AppEntry>,
    val hidden: List<AppEntry>,
)

class SettingsActions(
    val removeFavorite: (AppEntry) -> Unit,
    val removeDock: (AppEntry) -> Unit,
    val removeQuick: (AppEntry) -> Unit,
    val unhide: (AppEntry) -> Unit,
    val setAccent: (Int) -> Unit,
    val setIconShape: (IconShape) -> Unit,
    val setClock: (ClockMode) -> Unit,
    val setShowQuickPage: (Boolean) -> Unit,
    val setWallpaper: (WallpaperKind) -> Unit,
    val pickPhoto: (WallpaperKind) -> Unit,
    val setTileStyle: (TileStyle) -> Unit,
    val setDrawerBackground: (DrawerBackground) -> Unit,
    val setTextTone: (TextTone) -> Unit,
    /** Puts the lock-screen version of the chosen wallpaper on the lock screen. */
    val useOnLockScreen: () -> Unit,
    val openNightClock: () -> Unit,
    val openScreensaverSettings: () -> Unit,
    val setFeature: (Feature, Boolean) -> Unit,
    val setNotificationDots: (Boolean) -> Unit,
    val setNowPlaying: (Boolean) -> Unit,
    val openNotificationAccess: () -> Unit,
    val setPlayerStyle: (PlayerStyle) -> Unit,
    val setSpotifyOnly: (Boolean) -> Unit,
    val setPlayerCard: (Boolean) -> Unit = {},
    val openSpotifyNotifications: () -> Unit = {},
    val setPhoneWallpaper: (Boolean) -> Unit = {},
    val setSwipeDownNotifications: (Boolean) -> Unit = {},
    val setSmartSearch: (Boolean) -> Unit = {},
    val setIconStyle: (dev.rahmanfaizur.dotline.data.IconStyle) -> Unit = {},
    val setClockStyle: (dev.rahmanfaizur.dotline.data.ClockStyle) -> Unit = {},
    val setHomeApps: (Int) -> Unit = {},
    val setCompact: (Boolean) -> Unit = {},
    val setFoldersOn: (Boolean) -> Unit = {},
    val openGuide: () -> Unit = {},
    /** Opens Android's share sheet with a problem report (the note is what the person typed). */
    val shareReport: (String) -> Unit = {},
    val saveBackup: () -> Unit = {},
    val loadBackup: () -> Unit = {},
    val removeDotApp: (AppEntry) -> Unit,
    val setAsHome: () -> Unit,
    val changeHome: () -> Unit,
    val close: () -> Unit,
)

/** The pages Settings is split into; null is the list of pages. */
enum class SettingsPage(val title: String) {
    Look("Look"), Home("Home screen"), Lock("Lock screen & clock"), Panel("Panel tiles"),
    Notifications("Notifications"), Hidden("Hidden apps"), About("About"),
}

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    backdrop: Backdrop?,
    hasPhoto: Boolean,
    photoVersion: Long,
    thumbnail: suspend (WallpaperKind, Int, Int) -> Bitmap?,
    lockNote: String?,
    isDefaultHome: Boolean,
    lists: SettingsLists,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    features: Map<Feature, Boolean> = emptyMap(),
    notificationAccess: Boolean = false,
    dotAppEntries: List<AppEntry> = emptyList(),
    lockThumbnail: suspend (WallpaperKind, Int, Int) -> Bitmap? = { _, _, _ -> null },
) {
    val accent = LocalAccent.current
    var page by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    // Back on a page returns to the list; Back on the list closes Settings (handled by the launcher).
    BackHandler(enabled = page != null) { page = null }

    Column(
        modifier
            .fillMaxSize()
            .background(Dot.Black)
            .testTag("settings")
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Stays put while the rest scrolls.
        Row(
            Modifier.fillMaxWidth().padding(start = 6.dp, end = 4.dp, top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val p = page
            if (p != null) {
                BasicText(
                    "‹",
                    style = Dot.label.copy(color = Dot.Ink, fontSize = 30.sp),
                    modifier = Modifier.clickable(onClickLabel = "Back to settings") { page = null }
                        .testTag("settingsBack").padding(horizontal = 12.dp, vertical = 4.dp),
                )
                BasicText(p.title.uppercase(), style = Dot.dots(22), maxLines = 1, modifier = Modifier.weight(1f))
            } else {
                BasicText("SETTINGS", style = Dot.dots(32), modifier = Modifier.padding(start = 12.dp).weight(1f))
            }
            BasicText(
                "✕",
                style = Dot.label.copy(color = Dot.Muted, fontSize = 22.sp),
                modifier = Modifier.clickable(onClickLabel = "Close settings", onClick = actions.close).padding(14.dp),
            )
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (page) {
                null -> {
                    if (!isDefaultHome) {
                        Section("Home screen") {
                            SettingRow("Dotline isn't your home screen yet") {
                                Chip("Set as home screen", selected = true, accent = accent, tag = "setAsHome", onClick = actions.setAsHome)
                            }
                        }
                    }
                    Column(
                        Modifier.fillMaxWidth().background(Dot.Surface, RoundedCornerShape(22.dp)).padding(vertical = 6.dp),
                    ) {
                        PageRow(SettingsPage.Look, "${WallpaperNames.getValue(settings.wallpaper)} · ${TileNames.first { it.first == settings.tileStyle }.second}") { page = it }
                        PageRow(SettingsPage.Home, if (isDefaultHome) "Default launcher · clock · your apps" else "Not your launcher yet") { page = it }
                        PageRow(SettingsPage.Lock, "Lock wallpaper · Night clock · screensaver") { page = it }
                        PageRow(SettingsPage.Panel, onOff(features, Feature.WallpaperTile, Feature.RamTile)) { page = it }
                        PageRow(
                            SettingsPage.Notifications,
                            when {
                                !settings.notificationDots && !settings.nowPlaying -> "Off"
                                !notificationAccess -> "Needs access"
                                else -> listOfNotNull("Dots".takeIf { settings.notificationDots }, "Now playing".takeIf { settings.nowPlaying }).joinToString(" · ")
                            },
                        ) { page = it }
                        PageRow(SettingsPage.Hidden, if (lists.hidden.isEmpty()) "None" else "${lists.hidden.size} hidden") { page = it }
                        PageRow(SettingsPage.About, "Backup · no internet · private by design") { page = it }
                    }
                }

                SettingsPage.Look -> LookSection(settings, backdrop, hasPhoto, photoVersion, thumbnail, actions, accent)

                SettingsPage.Home -> {
                    Section("Launcher") {
                        SettingRow(if (isDefaultHome) "Default launcher: Dotline" else "Default launcher: another app") {
                            Chip("Change", selected = false, accent = accent, tag = "changeHome", onClick = actions.changeHome)
                        }
                    }
                    Section("Clock") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                dev.rahmanfaizur.dotline.data.ClockStyle.Big to "Big",
                                dev.rahmanfaizur.dotline.data.ClockStyle.Stacked to "Stacked",
                                dev.rahmanfaizur.dotline.data.ClockStyle.Small to "Small",
                            ).forEach { (st, name) ->
                                Chip(name, settings.clockStyle == st, accent, tag = "clockStyle:$name") { actions.setClockStyle(st) }
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Chip("Phone setting", settings.clock == ClockMode.System, accent) { actions.setClock(ClockMode.System) }
                            Chip("24-hour", settings.clock == ClockMode.H24, accent) { actions.setClock(ClockMode.H24) }
                            Chip("12-hour", settings.clock == ClockMode.H12, accent) { actions.setClock(ClockMode.H12) }
                        }
                    }
                    Section("Gestures") {
                        SwitchRow("Swipe down on home for notifications", settings.swipeDownNotifications, accent, "toggle:swipeDown") {
                            actions.setSwipeDownNotifications(!settings.swipeDownNotifications)
                        }
                    }
                    Section("Search") {
                        SwitchRow("Answer sums (12*8) and find phone settings (wifi, battery)", settings.smartSearch, accent, "toggle:smartSearch") {
                            actions.setSmartSearch(!settings.smartSearch)
                        }
                    }
                    Section("Quick page") {
                        SwitchRow("Swipe right for calendar, storage and RAM", settings.showQuickPage, accent, "toggle:quickPage") {
                            actions.setShowQuickPage(!settings.showQuickPage)
                        }
                    }
                    Section("Layout") {
                        Caption("Apps on home")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(4, 6, 8).forEach { n -> Chip("$n", settings.homeApps == n, accent, tag = "homeApps:$n") { actions.setHomeApps(n) } }
                        }
                        SwitchRow("Compact: smaller tiles, apps as icons only", settings.compact, accent, "toggle:compact") {
                            actions.setCompact(!settings.compact)
                        }
                        SwitchRow("Folders: drop an app on another while moving things", settings.foldersOn, accent, "toggle:folders") {
                            actions.setFoldersOn(!settings.foldersOn)
                        }
                    }
                    Section("Your apps") {
                        AppStrip("Home", lists.favorites, actions.removeFavorite)
                        AppStrip("Dock", lists.dock, actions.removeDock)
                        AppStrip("Quick open", lists.quick, actions.removeQuick)
                        BasicText("Tap an app here to remove it. Long-press any app to add it.", style = Dot.label.copy(color = Dot.Muted))
                    }
                }

                SettingsPage.Lock -> {
                    Section("Preview") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                LockPreview(settings.wallpaper, Triple(settings.accent, photoVersion, hasPhoto), lockThumbnail)
                                Gap(6.dp)
                                Caption("Lock screen")
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                NightClockPreview()
                                Gap(6.dp)
                                Caption("Night clock")
                            }
                        }
                    }
                    Section("Lock screen wallpaper") {
                        SettingRow("Use the Look wallpaper on the lock screen") {
                            Chip("Set", selected = true, accent = accent, tag = "useOnLock", onClick = actions.useOnLockScreen)
                        }
                        BasicText(
                            lockNote ?: "Android doesn't let apps change the lock screen clock, so it stays your phone's own.",
                            style = Dot.label.copy(color = Dot.Muted),
                            modifier = Modifier.testTag("lockNote"),
                        )
                    }
                    Section("Night clock") {
                        SettingRow("The dot clock, full screen. Shows over the lock screen like an alarm; tap to close.") {
                            Chip("Open", selected = false, accent = accent, tag = "openNightClock", onClick = actions.openNightClock)
                        }
                    }
                    Section("Screensaver") {
                        SwitchRow("Dot clock while charging", features[Feature.Screensaver] != false, accent, "toggle:screensaver") {
                            actions.setFeature(Feature.Screensaver, features[Feature.Screensaver] == false)
                        }
                        SettingRow("Pick it in your phone's screensaver settings") {
                            Chip("Open", selected = false, accent = accent, tag = "openScreensaver", onClick = actions.openScreensaverSettings)
                        }
                    }
                }

                SettingsPage.Panel -> {
                    Section("Tiles in your phone's pull-down panel") {
                        SwitchRow("Next wallpaper", features[Feature.WallpaperTile] != false, accent, "toggle:wallpaperTile") {
                            actions.setFeature(Feature.WallpaperTile, features[Feature.WallpaperTile] == false)
                        }
                        SwitchRow("RAM in use", features[Feature.RamTile] != false, accent, "toggle:ramTile") {
                            actions.setFeature(Feature.RamTile, features[Feature.RamTile] == false)
                        }
                        BasicText(
                            "To add them: pull down the panel, tap the pencil (edit), and drag the Dotline tiles in. Switching one off removes it.",
                            style = Dot.label.copy(color = Dot.Muted),
                        )
                    }
                }

                SettingsPage.Notifications -> {
                    Section("Notification access") {
                        SettingRow(if (notificationAccess) "Allowed in Android's settings" else "Not allowed yet") {
                            Chip(if (notificationAccess) "Manage" else "Allow", selected = !notificationAccess, accent = accent, tag = "notifAccess", onClick = actions.openNotificationAccess)
                        }
                        BasicText(
                            "Dotline only notes which apps have a notification and what song is playing. It never keeps notification text, and with no internet nothing can leave the phone. Turn both switches off and it ignores notifications completely.",
                            style = Dot.label.copy(color = Dot.Muted),
                        )
                    }
                    Section("Dots") {
                        SwitchRow("Dots on app icons", settings.notificationDots, accent, "toggle:dots") {
                            actions.setNotificationDots(!settings.notificationDots)
                        }
                        if (settings.notificationDots) {
                            if (dotAppEntries.isEmpty()) {
                                BasicText("No apps chosen.", style = Dot.label.copy(color = Dot.Muted))
                            } else {
                                dotAppEntries.forEach { app -> AppRow(app, "Remove", actions.removeDotApp) }
                            }
                            BasicText("Long-press any app → Show notification dot to add it.", style = Dot.label.copy(color = Dot.Muted))
                        }
                    }
                    Section("Now playing") {
                        SwitchRow("Song tile on home, with play, pause and skip", settings.nowPlaying, accent, "toggle:nowPlaying") {
                            actions.setNowPlaying(!settings.nowPlaying)
                        }
                        if (settings.nowPlaying) {
                            PlayerPreview(settings, backdrop, accent)
                            Caption("Style")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(PlayerStyle.Glow to "Cover glow", PlayerStyle.DotWave to "Dot wave", PlayerStyle.Vinyl to "Vinyl").forEach { (st, name) ->
                                    Chip(name, settings.playerStyle == st, accent, tag = "player:$name") { actions.setPlayerStyle(st) }
                                }
                            }
                            Caption("Show for")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Chip("Spotify only", settings.spotifyOnly, accent, tag = "player:spotifyOnly") { actions.setSpotifyOnly(true) }
                                Chip("Any music app", !settings.spotifyOnly, accent, tag = "player:anyApp") { actions.setSpotifyOnly(false) }
                            }
                            BasicText("The speaker chip on the player opens Android's own speaker picker.", style = Dot.label.copy(color = Dot.Muted))
                            Caption("Pull-down panel & lock screen")
                            SwitchRow("Show this player there too", settings.playerCard, accent, "toggle:playerCard") {
                                actions.setPlayerCard(!settings.playerCard)
                            }
                            if (settings.playerCard) {
                                SettingRow("To hide Spotify's own grey player, turn off its playback notifications. Music keeps playing.") {
                                    Chip("Spotify", selected = false, accent = accent, tag = "spotifyNotifications", onClick = actions.openSpotifyNotifications)
                                }
                            }
                        }
                    }
                }

                SettingsPage.Hidden -> Section("Hidden apps") {
                    if (lists.hidden.isEmpty()) {
                        BasicText("None. Long-press an app → Hide from all apps.", style = Dot.label.copy(color = Dot.Muted))
                    } else {
                        lists.hidden.forEach { app -> AppRow(app, "Show", actions.unhide) }
                    }
                }

                SettingsPage.About -> {
                    Section("How to use") {
                        SettingRow("A quick guide to gestures, search and making Dotline yours") {
                            Chip("Open", selected = false, accent = accent, tag = "openGuide", onClick = actions.openGuide)
                        }
                    }
                    Section("Report a problem") {
                        var note by rememberSaveable { mutableStateOf("") }
                        androidx.compose.foundation.text.BasicTextField(
                            value = note,
                            onValueChange = { note = it.take(1000) },
                            textStyle = Dot.label.copy(fontSize = 14.sp),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(accent),
                            decorationBox = { inner ->
                                Box(
                                    Modifier.fillMaxWidth().background(Dot.SurfaceHigh, RoundedCornerShape(14.dp)).padding(12.dp),
                                ) {
                                    if (note.isEmpty()) BasicText("What went wrong?", style = Dot.label.copy(color = Dot.Muted, fontSize = 14.sp))
                                    inner()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("reportNote"),
                        )
                        SettingRow("Send it with any app, like WhatsApp") {
                            Chip("Share", selected = false, accent = accent, tag = "shareReport", onClick = { actions.shareReport(note) })
                        }
                        BasicText(
                            "Adds your Dotline version, phone model, Android version and which features are on. Never your apps or anything personal. Or open an issue at ${dev.rahmanfaizur.dotline.domain.BugReport.ISSUES}",
                            style = Dot.label.copy(color = Dot.Muted, lineHeight = 18.sp),
                        )
                    }
                    Section("Privacy & safety") {
                        listOf(
                            "No internet permission: nothing Dotline sees can leave your phone. No ads, tracking or accounts.",
                            "Uninstall: only opens Android's own \"Uninstall?\" screen. You always confirm.",
                            "Set wallpaper: only puts a picture on home or lock screen. It can't read any wallpaper.",
                            "Notification access (optional, off): only which apps have a notification and the song playing. Never message text.",
                            "Notifications (optional, off): only for the music card in the pull-down panel.",
                            "Open the notification panel: only for swipe down on home.",
                            "Photo picker: Dotline gets only the one photo you choose.",
                            "No accessibility, device admin or screen reading. Your settings stay in the app's private storage.",
                            "To remove: Settings → Apps → Default apps → Home, pick another launcher, then uninstall Dotline.",
                        ).forEach { BasicText(it, style = Dot.label.copy(color = Dot.Muted, lineHeight = 18.sp)) }
                    }
                    Section("Backup") {
                        SettingRow("Save your Dotline setup to a file") {
                            Chip("Save", selected = false, accent = accent, tag = "backupSave", onClick = actions.saveBackup)
                        }
                        SettingRow("Load a saved setup") {
                            Chip("Load", selected = false, accent = accent, tag = "backupLoad", onClick = actions.loadBackup)
                        }
                        BasicText(
                            "Holds your layout, colours, folders and switches, plus which apps you pinned or hid. Not your photo. Keep the file somewhere private.",
                            style = Dot.label.copy(color = Dot.Muted),
                        )
                    }
                    Section("Dotline") {
                        BasicText("Your own launcher. It has no internet permission, so nothing can leave the phone.", style = Dot.label.copy(fontSize = 14.sp))
                        BasicText("Settings and your picked photo stay inside the app's private storage.", style = Dot.label.copy(color = Dot.Muted))
                        BasicText(dev.rahmanfaizur.dotline.ui.guide.CREDIT, style = Dot.label.copy(fontSize = 13.sp))
                        BasicText(dev.rahmanfaizur.dotline.ui.guide.SOURCE, style = Dot.label.copy(color = Dot.Muted))
                        BasicText("Font: Doto (SIL Open Font License).", style = Dot.label.copy(color = Dot.Muted))
                    }
                }
            }
            Spacer(Modifier.size(20.dp))
        }
    }
}

private fun onOff(features: Map<Feature, Boolean>, vararg f: Feature): String {
    val on = f.count { features[it] != false }
    return if (on == f.size) "All on" else if (on == 0) "Off" else "$on of ${f.size} on"
}

@Composable
private fun PageRow(page: SettingsPage, summary: String, open: (SettingsPage) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open ${page.title}") { open(page) }
            .testTag("page:${page.name}")
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(page.title.uppercase(), style = Dot.dots(18))
            Gap(2.dp)
            BasicText(summary, style = Dot.label.copy(color = Dot.Muted, fontSize = 12.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        BasicText("›", style = Dot.label.copy(color = Dot.Muted, fontSize = 22.sp))
    }
}

@Composable
private fun SwitchRow(text: String, on: Boolean, accent: Color, tag: String, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(text, style = Dot.label.copy(fontSize = 14.sp), modifier = Modifier.weight(1f))
        Gap(10.dp)
        Switch(on, accent)
    }
}

@Composable
private fun SettingRow(text: String, action: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicText(text, style = Dot.label.copy(fontSize = 14.sp), modifier = Modifier.weight(1f))
        Gap(10.dp)
        action()
    }
}

/** One pinned list as a row of icons; tapping an icon removes it. */
@Composable
private fun AppStrip(title: String, apps: List<AppEntry>, onRemove: (AppEntry) -> Unit) {
    Caption("$title · ${apps.size}/4")
    if (apps.isEmpty()) {
        BasicText("Empty", style = Dot.label.copy(color = Dot.Faint))
        return
    }
    Row(Modifier.fillMaxWidth()) {
        apps.take(4).forEach { app ->
            Column(
                Modifier
                    .weight(1f)
                    .clickable(onClickLabel = "Remove ${app.label}") { onRemove(app) }
                    .testTag("Remove:${app.label}")
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box {
                    AppIcon(app, 44.dp)
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                            .background(Dot.SurfaceHigh, CircleShape)
                            .border(1.dp, Dot.Black, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { BasicText("✕", style = Dot.label.copy(color = Dot.Ink, fontSize = 8.sp)) }
                }
                Gap(4.dp)
                BasicText(
                    app.label,
                    style = Dot.label.copy(fontSize = 10.sp, color = Dot.Muted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
        repeat(4 - apps.size.coerceAtMost(4)) { Spacer(Modifier.weight(1f)) }
    }
}

private val WallpaperNames = mapOf(
    WallpaperKind.Black to "Black",
    WallpaperKind.Photo to "My photo",
    WallpaperKind.DotField to "Dot field",
    WallpaperKind.Halftone to "Halftone",
    WallpaperKind.Sunset to "Sunset",
    WallpaperKind.Glow to "Glow",
    WallpaperKind.Horizon to "Horizon",
)

private val TileNames = listOf(
    TileStyle.Smoke to "Smoke glass",
    TileStyle.Glass to "Clear glass",
    TileStyle.Tinted to "Tinted",
    TileStyle.Outline to "Outline",
    TileStyle.Floating to "Floating",
    TileStyle.Solid to "Solid",
)

private val DrawerNames = listOf(
    DrawerBackground.Frosted to "Frosted",
    DrawerBackground.Dimmed to "Dimmed",
    DrawerBackground.Sheet to "Glass sheet",
    DrawerBackground.Black to "Black",
)

/** Wallpaper + tile style, with a small live preview of the home screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LookSection(
    settings: LauncherSettings,
    backdrop: Backdrop?,
    hasPhoto: Boolean,
    photoVersion: Long,
    thumbnail: suspend (WallpaperKind, Int, Int) -> Bitmap?,
    actions: SettingsActions,
    accent: Color,
) {
    Section("Preview") {
        // Two live previews: home and the app list, side by side.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LookPreview(backdrop, settings.tileStyle, useDarkText(settings.textTone, settings.tileStyle, backdrop))
                Gap(6.dp)
                Caption("Home")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AppListPreview(backdrop, settings.drawerBackground, accent)
                Gap(6.dp)
                Caption("App list")
            }
        }

        Caption("Wallpaper")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WallpaperKind.entries.forEach { kind ->
                WallpaperOption(
                    kind = kind,
                    selected = settings.wallpaper == kind,
                    accent = accent,
                    key = Triple(settings.accent, photoVersion, hasPhoto),
                    thumbnail = thumbnail,
                    needsPhoto = kind == WallpaperKind.Photo && !hasPhoto,
                    onClick = { actions.setWallpaper(kind) },
                )
            }
        }
        if (settings.wallpaper == WallpaperKind.Photo || settings.wallpaper == WallpaperKind.Halftone) {
            Chip(
                if (hasPhoto) "Change photo" else "Choose a photo",
                selected = false, accent = accent, tag = "pickPhoto",
                onClick = { actions.pickPhoto(settings.wallpaper) },
            )
        }

        SwitchRow("Also use it as the phone's wallpaper (shows behind Recents)", settings.phoneWallpaper, accent, "toggle:phoneWallpaper") {
            actions.setPhoneWallpaper(!settings.phoneWallpaper)
        }

        Caption("Tiles")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TileNames.forEach { (style, name) ->
                Chip(name, settings.tileStyle == style, accent, tag = "tiles:$name") { actions.setTileStyle(style) }
            }
        }
        if (backdrop == null && settings.tileStyle in listOf(TileStyle.Smoke, TileStyle.Glass, TileStyle.Tinted)) {
            BasicText("On a black wallpaper these tiles show as solid.", style = Dot.label.copy(color = Dot.Muted))
        }

        Caption("Text on tiles")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(TextTone.Auto to "Auto", TextTone.White to "White", TextTone.Black to "Black").forEach { (tone, name) ->
                Chip(name, settings.textTone == tone, accent, tag = "text:$name") { actions.setTextTone(tone) }
            }
        }
        if (settings.textTone == TextTone.Black && settings.tileStyle in listOf(TileStyle.Smoke, TileStyle.Tinted, TileStyle.Solid)) {
            BasicText("Black text is hard to read on dark tiles. Try Clear glass, Outline or Floating.", style = Dot.label.copy(color = Dot.Muted))
        }

        Caption("App list")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DrawerNames.forEach { (bg, name) ->
                Chip(name, settings.drawerBackground == bg, accent, tag = "drawerBg:$name") { actions.setDrawerBackground(bg) }
            }
        }

        Caption("Accent colour")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Dot.Accents.forEachIndexed { i, color ->
                val selected = settings.accent == i
                Box(
                    Modifier
                        .size(36.dp)
                        .then(if (selected) Modifier.border(2.dp, Dot.Ink, CircleShape) else Modifier)
                        .padding(5.dp)
                        .background(color, CircleShape)
                        .clickable(onClickLabel = Dot.AccentNames[i]) { actions.setAccent(i) }
                        .testTag("accent:${Dot.AccentNames[i]}"),
                )
            }
        }

        Caption("Icons")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                dev.rahmanfaizur.dotline.data.IconStyle.Original to "App colours",
                dev.rahmanfaizur.dotline.data.IconStyle.White to "Dot white",
                dev.rahmanfaizur.dotline.data.IconStyle.Accent to "Dot accent",
            ).forEach { (st, name) ->
                Chip(name, settings.iconStyle == st, accent, tag = "icons:$name") { actions.setIconStyle(st) }
            }
        }
        if (settings.iconStyle != dev.rahmanfaizur.dotline.data.IconStyle.Original) {
            BasicText("Uses each app's simple one-colour icon (Android 13 and newer). Apps without one keep their own icon.", style = Dot.label.copy(color = Dot.Muted))
        }

        Caption("Icon shape")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ShapeOption("Round", CircleShape, settings.iconShape == IconShape.Round, accent) {
                actions.setIconShape(IconShape.Round)
            }
            ShapeOption("Rounded square", Dot.iconShape(IconShape.Square), settings.iconShape == IconShape.Square, accent) {
                actions.setIconShape(IconShape.Square)
            }
        }
    }
}

/** A tiny home screen drawn with the real wallpaper and tile code. */
@Composable
private fun LookPreview(backdrop: Backdrop?, style: TileStyle, darkText: Boolean) {
    CompositionLocalProvider(LocalDarkText provides darkText) {
    WallpaperArea(
        backdrop = backdrop,
        style = style,
        modifier = Modifier
            .width(112.dp)
            .aspectRatio(9f / 20f)
            .clip(RoundedCornerShape(18.dp))
            .border(2.dp, Dot.SurfaceHigh, RoundedCornerShape(18.dp))
            .testTag("lookPreview"),
    ) {
        Column(Modifier.fillMaxSize().padding(7.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth().tile(RoundedCornerShape(10.dp)).padding(7.dp)) {
                BasicText("17:48", style = Dot.dots(22))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(2) { Box(Modifier.weight(1f).height(34.dp).tile(RoundedCornerShape(10.dp))) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(2) {
                    Row(
                        Modifier.weight(1f).tile(RoundedCornerShape(10.dp)).padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) { Box(Modifier.size(12.dp).background(Dot.Ink, CircleShape)) }
                }
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxWidth().height(16.dp).tile(RoundedCornerShape(50)))
            Box(Modifier.fillMaxWidth().height(22.dp).tile(RoundedCornerShape(50)))
        }
    }
    }
}

/** A tiny app list drawn with the chosen background, so the choice shows before you leave Settings. */
@Composable
private fun AppListPreview(backdrop: Backdrop?, bg: DrawerBackground, accent: Color) {
    val mode = if (backdrop == null) DrawerBackground.Black else bg
    val outer = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .width(112.dp)
            .aspectRatio(9f / 20f)
            .clip(outer)
            .border(2.dp, Dot.SurfaceHigh, outer)
            .background(Dot.Black)
            .testTag("appListPreview"),
    ) {
        if (backdrop != null) when (mode) {
            DrawerBackground.Frosted -> {
                Image(backdrop.frosted, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, filterQuality = FilterQuality.Low)
                Box(Modifier.fillMaxSize().background(Color(0x8C000000)))
            }
            DrawerBackground.Dimmed -> {
                Image(backdrop.full, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Color(0xA8000000)))
            }
            DrawerBackground.Sheet -> {
                Image(backdrop.full, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                val sheet = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                Box(Modifier.fillMaxSize().padding(top = 10.dp).clip(sheet)) {
                    Image(backdrop.frosted, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, filterQuality = FilterQuality.Low)
                    Box(Modifier.fillMaxSize().background(Color(0x99000000)))
                }
            }
            DrawerBackground.Black -> Unit
        }
        val glassy = mode != DrawerBackground.Black
        Column(Modifier.fillMaxSize().padding(start = 7.dp, end = 7.dp, top = 15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier.fillMaxWidth().height(14.dp)
                    .background(if (glassy) Color(0x1AFFFFFF) else Dot.Surface, RoundedCornerShape(50)),
            )
            BasicText("A", style = Dot.dots(20, accent))
            repeat(5) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    repeat(4) { Box(Modifier.size(15.dp).background(Dot.Ink.copy(alpha = 0.85f), CircleShape)) }
                }
            }
        }
        // The scroll handle on the right edge.
        Box(Modifier.align(Alignment.CenterEnd).padding(end = 2.dp).size(width = 2.dp, height = 18.dp).background(Dot.Ink.copy(alpha = 0.5f), RoundedCornerShape(50)))
    }
}

@Composable
private fun WallpaperOption(
    kind: WallpaperKind,
    selected: Boolean,
    accent: Color,
    key: Any,
    thumbnail: suspend (WallpaperKind, Int, Int) -> Bitmap?,
    needsPhoto: Boolean,
    onClick: () -> Unit,
) {
    val name = WallpaperNames.getValue(kind)
    val image by produceState<ImageBitmap?>(null, kind, key) {
        value = if (needsPhoto) null else thumbnail(kind, 108, 240)?.asImageBitmap()
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).testTag("wallpaper:$name"),
    ) {
        val shape = RoundedCornerShape(10.dp)
        Box(
            Modifier
                .size(width = 50.dp, height = 104.dp)
                .border(if (selected) 2.dp else 1.dp, if (selected) accent else Dot.SurfaceHigh, shape)
                .padding(3.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Dot.Black),
            contentAlignment = Alignment.Center,
        ) {
            image?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            if (needsPhoto) BasicText("+", style = Dot.label.copy(color = Dot.Muted, fontSize = 22.sp))
        }
        Gap(4.dp)
        BasicText(name, style = Dot.label.copy(fontSize = 11.sp, color = if (selected) Dot.Ink else Dot.Muted))
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Dot.Surface, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicText(title.uppercase(), style = Dot.dots(15, Dot.Muted))
        content()
    }
}

@Composable
private fun AppRow(app: AppEntry, actionLabel: String, onAction: (AppEntry) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(app, 32.dp)
        Gap(12.dp)
        BasicText(app.label, style = Dot.label.copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        BasicText(
            actionLabel,
            style = Dot.caption.copy(color = Dot.Muted),
            modifier = Modifier.clickable { onAction(app) }.padding(8.dp).testTag("$actionLabel:${app.label}"),
        )
    }
}

@Composable
private fun ShapeOption(label: String, shape: Shape, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Column(
        Modifier
            .border(if (selected) 2.dp else 1.dp, if (selected) accent else Dot.SurfaceHigh, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("shape:$label")
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(36.dp).background(Dot.Ink, shape))
        Gap(8.dp)
        BasicText(label, style = Dot.label.copy(fontSize = 12.sp))
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, accent: Color, tag: String = "clock:$label", onClick: () -> Unit) {
    BasicText(
        label,
        style = Dot.label.copy(color = if (selected) Dot.Black else Dot.Ink, fontSize = 13.sp),
        modifier = Modifier
            .background(if (selected) accent else Dot.SurfaceHigh, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

@Composable
private fun Switch(on: Boolean, accent: Color) {
    Box(
        Modifier
            .size(width = 46.dp, height = 26.dp)
            .background(if (on) accent else Dot.SurfaceHigh, RoundedCornerShape(50))
            .padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(20.dp).background(Dot.Ink, CircleShape))
    }
}

/** The lock screen as it will look: the lock version of the wallpaper under the phone's own clock. */
@Composable
private fun LockPreview(kind: WallpaperKind, key: Any, lockThumbnail: suspend (WallpaperKind, Int, Int) -> Bitmap?) {
    val image by produceState<ImageBitmap?>(null, kind, key) {
        value = lockThumbnail(kind, 270, 600)?.asImageBitmap()
    }
    val outer = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .width(112.dp)
            .aspectRatio(9f / 20f)
            .clip(outer)
            .border(2.dp, Dot.SurfaceHigh, outer)
            .background(Dot.Black)
            .testTag("lockPreview"),
    ) {
        image?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Column(Modifier.fillMaxWidth().padding(top = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // The phone draws its own clock here; a plain one stands in for it.
            BasicText("17:48", style = Dot.label.copy(color = Color.White, fontSize = 22.sp))
            BasicText("Thu 26 Nov", style = Dot.label.copy(color = Color(0xCCFFFFFF), fontSize = 8.sp))
        }
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).size(width = 30.dp, height = 3.dp).background(Color(0x99FFFFFF), RoundedCornerShape(50)))
    }
}

/** The Night clock: a dim dot clock on black. */
@Composable
private fun NightClockPreview() {
    val outer = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .width(112.dp)
            .aspectRatio(9f / 20f)
            .clip(outer)
            .border(2.dp, Dot.SurfaceHigh, outer)
            .background(Color.Black)
            .testTag("nightClockPreview"),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText("17", style = Dot.dots(34, Color(0x8CFFFFFF)))
            BasicText("48", style = Dot.dots(34, Color(0x8CFFFFFF)))
        }
    }
}

/** The chosen player style with a sample song, drawn by the real player over your wallpaper and tiles. */
@Composable
private fun PlayerPreview(settings: LauncherSettings, backdrop: Backdrop?, accent: Color) {
    val cover = remember(accent) { sampleCover(accent.toArgb()) }
    val sample = remember(cover) {
        NowPlaying(
            packageName = "com.spotify.music",
            title = "Sample song",
            artist = "Dotline",
            art = cover,
            playing = true,
            controller = null,
            durationMs = 214_000,
            positionMs = 78_000,
            positionAtMs = 0,
        )
    }
    val actions = remember { PlayerActions("Spotify", "Phone speaker", openPlayer = {}, openOutput = {}) }
    CompositionLocalProvider(LocalDarkText provides useDarkText(settings.textTone, settings.tileStyle, backdrop)) {
        WallpaperArea(
            backdrop = backdrop,
            style = settings.tileStyle,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).testTag("playerPreview"),
        ) {
            Box(Modifier.padding(12.dp)) { NowPlayingTile(sample, settings.playerStyle, actions) }
        }
    }
}

/** A made-up album cover in the accent colour, so the preview never needs a real song. */
private fun sampleCover(accent: Int): Bitmap {
    val size = 160
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    paint.shader = android.graphics.LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), accent, 0xFF101010.toInt(), android.graphics.Shader.TileMode.CLAMP)
    c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
    paint.shader = null
    paint.color = 0x55FFFFFF
    for (y in 0 until 8) for (x in 0 until 8) {
        val r = 1.5f + (x + y) * 0.35f
        c.drawCircle(14f + x * 19f, 14f + y * 19f, r, paint)
    }
    return bmp
}

