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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import dev.rahmanfaizur.dotline.data.HomeBlock
import dev.rahmanfaizur.dotline.ui.theme.LocalDarkText
import kotlin.math.roundToInt
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
 * Swipe up or tap search for all apps. Long-press empty space to move things around (and reach Settings).
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
    editing: Boolean = false,
    onEditChange: (Boolean) -> Unit = {},
    blockOrder: List<HomeBlock> = HomeBlock.Default,
    onReorder: (List<HomeBlock>) -> Unit = {},
    onSwapApps: (AppSlot, AppSlot) -> Unit = { _, _ -> },
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

    val edit = remember { HomeEditState() }.takeIf { editing }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val spacingPx = with(LocalDensity.current) { 10.dp.toPx() }

    // Block order while dragging; saved when the finger lifts.
    var order by remember(blockOrder) { mutableStateOf(blockOrder) }
    var dragging by remember { mutableStateOf<HomeBlock?>(null) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val heights = remember { HashMap<HomeBlock, Int>() }
    fun shown(b: HomeBlock) = b != HomeBlock.Player || (nowPlaying != null && playerActions != null)
    fun dragBlock(b: HomeBlock, dy: Float) {
        dragY += dy
        val vis = order.filter(::shown)
        val i = vis.indexOf(b)
        if (dragY > 0 && i < vis.lastIndex) {
            val next = vis[i + 1]
            val h = (heights[next] ?: 0) + spacingPx
            if (dragY > h / 2) {
                order = order.toMutableList().apply { remove(b); add(indexOf(next) + 1, b) }
                dragY -= h
            }
        } else if (dragY < 0 && i > 0) {
            val prev = vis[i - 1]
            val h = (heights[prev] ?: 0) + spacingPx
            if (-dragY > h / 2) {
                order = order.toMutableList().apply { remove(b); add(indexOf(prev), b) }
                dragY += h
            }
        }
    }

    Box(modifier.fillMaxSize().onGloballyPositioned { origin = it.boundsInRoot().topLeft }) {
    Column(
        Modifier
            .fillMaxSize()
            .testTag("home")
            .pointerInput(editing) {
                if (editing) return@pointerInput
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { _, dy -> total += dy },
                    onDragEnd = { if (total < -120f) onOpenApps(false) },
                )
            }
            .pointerInput(editing) { if (!editing) detectTapGestures(onLongPress = { onEditChange(true) }) }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (editing) {
            EditBar(onSettings = { onEditChange(false); onOpenSettings() }, onDone = { onEditChange(false) })
        } else {
            // Hidden locally at once on "Not now"; the saved setting keeps it hidden next time.
            var setHomeHidden by remember { mutableStateOf(false) }
            if (setHome != null && !setHomeHidden) {
                SetHomeTile(SetHomePrompt(set = setHome.set, notNow = { setHomeHidden = true; setHome.notNow() }), accent)
            }
        }

        order.filter(::shown).forEachIndexed { i, block ->
            Box(
                Modifier
                    .onSizeChanged { heights[block] = it.height }
                    .zIndex(if (dragging == block) 1f else 0f)
                    .graphicsLayer {
                        if (dragging == block) { translationY = dragY; scaleX = 1.02f; scaleY = 1.02f }
                    }
                    .wiggle(editing && dragging != block, i),
            ) {
                when (block) {
                    HomeBlock.Clock -> Tile(Modifier.fillMaxWidth().tileTap("Open clock", enabled = !editing) { onTile(Shortcut.Clock) }.testTag("clockTile")) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AccentDot(accent)
                            Gap(8.dp)
                            Caption(date)
                        }
                        Gap(10.dp)
                        BasicText(time(now), style = Dot.dots(76), modifier = Modifier.testTag("clock"))
                    }
                    HomeBlock.Status -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Tile(Modifier.weight(1f).height(112.dp).tileTap("Battery settings", enabled = !editing) { onTile(Shortcut.Battery) }) {
                            Caption(if (state.battery?.charging == true) "Charging" else "Battery")
                            BasicText("${state.battery?.percent ?: "--"}%", style = Dot.dots(38))
                            DotBar(filled = ((state.battery?.percent ?: 0) + 5) / 10, lit = Dot.ink)
                        }
                        Tile(Modifier.weight(1f).height(112.dp).tileTap("Open alarms", enabled = !editing) { onTile(Shortcut.Alarms) }) {
                            Caption("Alarm")
                            BasicText(state.nextAlarm?.let(::time) ?: "OFF", style = Dot.dots(38))
                            Caption(
                                state.nextAlarm?.let { DateFormat.format("EEE", Date(it)).toString() } ?: "None set",
                                color = Dot.faint,
                            )
                        }
                    }
                    HomeBlock.Apps -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Four favourite apps, two per row
                        state.favorites.take(4).withIndex().chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { (index, app) ->
                                    AppTile(app, Modifier.weight(1f).homeApp(app, AppSlot(false, index), actions, edit, onSwapApps))
                                }
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
                    }
                    HomeBlock.Player -> if (nowPlaying != null && playerActions != null) NowPlayingTile(nowPlaying, playerStyle, playerActions)
                }
                if (editing) {
                    BlockHandle(
                        block,
                        onStart = { dragging = block; dragY = 0f },
                        onDrag = { dragBlock(block, it) },
                        onEnd = {
                            dragging = null
                            dragY = 0f
                            if (order != blockOrder) onReorder(order)
                        },
                        modifier = Modifier.align(Alignment.TopCenter).offset(y = (-11).dp),
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Search pill
        Box(
            Modifier
                .fillMaxWidth()
                .tile(RoundedCornerShape(50))
                .clickable(enabled = !editing) { onOpenApps(true) }
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
                state.dock.take(4).forEachIndexed { index, app ->
                    Box(Modifier.homeApp(app, AppSlot(true, index), actions, edit, onSwapApps).padding(4.dp)) {
                        AppIcon(app, 48.dp)
                    }
                }
            }
        }
    }

    // The app being dragged, drawn above everything and following the finger.
    val from = edit?.from
    val dragged = edit?.app
    val rect = from?.let { edit.bounds[it] }
    if (dragged != null && rect != null) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDarkText provides false) {
            Box(
                Modifier
                    .offset { IntOffset((rect.left - origin.x + edit.offset.x).roundToInt(), (rect.top - origin.y + edit.offset.y).roundToInt()) }
                    .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
                    .graphicsLayer { scaleX = 1.08f; scaleY = 1.08f; shadowElevation = 16f; shape = TileShape; clip = false },
            ) {
                if (from.dock) {
                    Box(Modifier.padding(4.dp)) { AppIcon(dragged, 48.dp) }
                } else {
                    AppTile(dragged, Modifier.fillMaxSize().background(Dot.SurfaceHigh, TileShape))
                }
            }
        }
    }
    }
}

/** Top bar in edit mode. */
@Composable
private fun EditBar(onSettings: () -> Unit, onDone: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().tile(TileShape).padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText("Drag the grips to move · drag apps to swap", style = Dot.label.copy(fontSize = 12.sp), modifier = Modifier.weight(1f))
        BasicText(
            "Settings",
            style = Dot.label.copy(fontSize = 13.sp),
            modifier = Modifier.clickable(onClick = onSettings).testTag("editSettings").padding(horizontal = 10.dp, vertical = 8.dp),
        )
        BasicText(
            "Done",
            style = Dot.label.copy(color = Dot.Black, fontSize = 13.sp),
            modifier = Modifier
                .background(LocalAccent.current, RoundedCornerShape(50))
                .clickable(onClick = onDone)
                .testTag("editDone")
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun AppTile(app: AppEntry, modifier: Modifier) {
    Row(
        modifier
            .tile(TileShape)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app, 38.dp)
        Gap(10.dp)
        BasicText(app.label, style = Dot.label.copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
