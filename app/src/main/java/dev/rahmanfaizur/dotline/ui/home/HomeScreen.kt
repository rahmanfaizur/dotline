package dev.rahmanfaizur.dotline.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rahmanfaizur.dotline.data.AppEntry
import dev.rahmanfaizur.dotline.data.BatteryState
import dev.rahmanfaizur.dotline.data.ClockMode
import dev.rahmanfaizur.dotline.ui.AppIcon
import dev.rahmanfaizur.dotline.ui.common.AccentDot
import dev.rahmanfaizur.dotline.ui.common.Caption
import dev.rahmanfaizur.dotline.ui.common.DotBar
import dev.rahmanfaizur.dotline.ui.common.Gap
import dev.rahmanfaizur.dotline.ui.common.Tile
import dev.rahmanfaizur.dotline.ui.common.TileShape
import dev.rahmanfaizur.dotline.ui.common.rememberMinuteTicker
import dev.rahmanfaizur.dotline.ui.common.tile
import dev.rahmanfaizur.dotline.ui.common.tileTap
import dev.rahmanfaizur.dotline.data.Shortcut
import dev.rahmanfaizur.dotline.ui.theme.Dot
import dev.rahmanfaizur.dotline.ui.theme.LocalAccent
import java.util.Date
import java.util.Locale

/** What the home screen needs to draw itself. */
class HomeState(
    val favorites: List<AppEntry>,
    val dock: List<AppEntry>,
    val battery: BatteryState?,
    val nextAlarm: Long?,
    val clock: ClockMode,
    /** False for the moment before the app list arrives, so "Your apps go here" doesn't flash. */
    val appsLoaded: Boolean = true,
)

/** Shared click handlers for any app shown on screen. */
class AppActions(
    /** Opens the app; bounds = where its icon is, for the opening animation. */
    val launch: (AppEntry, android.graphics.Rect?) -> Unit,
    val longPress: (AppEntry) -> Unit,
)

/** Shown on home until Dotline is the default launcher or the user taps "Not now". */
class SetHomePrompt(val set: () -> Unit, val notNow: () -> Unit)

/**
 * Bento home: clock tile, battery + alarm tiles, four app tiles, search pill and dock.
 * Swipe up or tap search for all apps; long-press empty space for settings.
 */
@Composable
fun HomeScreen(
    state: HomeState,
    actions: AppActions,
    onOpenApps: (withSearch: Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    setHome: SetHomePrompt? = null,
    onTile: (Shortcut) -> Unit = {},
    nowPlaying: dev.rahmanfaizur.dotline.notify.NowPlaying? = null,
    playerStyle: dev.rahmanfaizur.dotline.data.PlayerStyle = dev.rahmanfaizur.dotline.data.PlayerStyle.Glow,
    playerActions: PlayerActions? = null,
) {
    val accent = LocalAccent.current
    val context = LocalContext.current
    val now by rememberMinuteTicker()
    val is24h = when (state.clock) {
        ClockMode.H24 -> true
        ClockMode.H12 -> false
        ClockMode.System -> DateFormat.is24HourFormat(context)
    }
    fun time(ms: Long) = DateFormat.format(if (is24h) "HH:mm" else "h:mm", Date(ms)).toString()
    val date = remember(now / 60_000) {
        DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEE d MMM"), Date(now)).toString()
    }

    Column(
        modifier
            .fillMaxSize()
            .testTag("home")
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { _, dy -> total += dy },
                    onDragEnd = { if (total < -120f) onOpenApps(false) },
                )
            }
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onOpenSettings() }) }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Hidden locally at once on "Not now"; the saved setting keeps it hidden next time.
        var setHomeHidden by remember { mutableStateOf(false) }
        if (setHome != null && !setHomeHidden) {
            SetHomeTile(SetHomePrompt(set = setHome.set, notNow = { setHomeHidden = true; setHome.notNow() }), accent)
        }

        // Clock
        Tile(Modifier.fillMaxWidth().tileTap("Open clock") { onTile(Shortcut.Clock) }.testTag("clockTile")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AccentDot(accent)
                Gap(8.dp)
                Caption(date)
            }
            Gap(10.dp)
            BasicText(time(now), style = Dot.dots(76), modifier = Modifier.testTag("clock"))
        }

        // Battery + alarm
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(Modifier.weight(1f).height(112.dp).tileTap("Battery settings") { onTile(Shortcut.Battery) }) {
                Caption(if (state.battery?.charging == true) "Charging" else "Battery")
                BasicText("${state.battery?.percent ?: "--"}%", style = Dot.dots(38))
                DotBar(filled = ((state.battery?.percent ?: 0) + 5) / 10, lit = Dot.ink)
            }
            Tile(Modifier.weight(1f).height(112.dp).tileTap("Open alarms") { onTile(Shortcut.Alarms) }) {
                Caption("Alarm")
                BasicText(state.nextAlarm?.let(::time) ?: "OFF", style = Dot.dots(38))
                Caption(
                    state.nextAlarm?.let { DateFormat.format("EEE", Date(it)).toString() } ?: "None set",
                    color = Dot.faint,
                )
            }
        }

        // Four favourite apps, two per row
        state.favorites.take(4).chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { app -> AppTile(app, actions, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (state.favorites.isEmpty() && state.appsLoaded) {
            Tile(Modifier.fillMaxWidth()) {
                Caption("Your apps go here")
                Gap(4.dp)
                BasicText("Long-press any app → Add to home", style = Dot.label.copy(color = Dot.muted))
            }
        }

        if (nowPlaying != null && playerActions != null) NowPlayingTile(nowPlaying, playerStyle, playerActions)

        Spacer(Modifier.weight(1f))

        // Search pill
        Box(
            Modifier
                .fillMaxWidth()
                .tile(RoundedCornerShape(50))
                .clickable { onOpenApps(true) }
                .testTag("searchPill")
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            BasicText("Search apps", style = Dot.label.copy(color = Dot.muted, fontSize = 15.sp))
        }

        // Dock
        if (state.dock.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .tile(RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                state.dock.take(4).forEach { app -> DockIcon(app, actions) }
            }
        }
    }
}

@Composable
private fun AppTile(app: AppEntry, actions: AppActions, modifier: Modifier) {
    Row(
        modifier
            .tile(TileShape)
            .appClicks(app, actions)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app, 38.dp)
        Gap(10.dp)
        BasicText(app.label, style = Dot.label.copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DockIcon(app: AppEntry, actions: AppActions) {
    Box(Modifier.appClicks(app, actions).padding(4.dp)) {
        AppIcon(app, 48.dp)
    }
}

@Composable
private fun SetHomeTile(prompt: SetHomePrompt, accent: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .tile(TileShape)
            .testTag("setHomeTile")
            .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccentDot(accent)
        Gap(10.dp)
        BasicText(
            "Make Dotline your home screen",
            style = Dot.label.copy(fontSize = 13.sp),
            modifier = Modifier.weight(1f),
        )
        BasicText(
            "Not now",
            style = Dot.label.copy(color = Dot.muted, fontSize = 13.sp),
            modifier = Modifier.clickable(onClick = prompt.notNow).testTag("notNow").padding(horizontal = 8.dp, vertical = 8.dp),
        )
        BasicText(
            "Set",
            style = Dot.label.copy(color = Dot.Black, fontSize = 13.sp),
            modifier = Modifier
                .background(accent, RoundedCornerShape(50))
                .clickable(onClick = prompt.set)
                .testTag("setHome")
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
