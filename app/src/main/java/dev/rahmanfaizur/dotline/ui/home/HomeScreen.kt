package dev.rahmanfaizur.dotline.ui.home

import androidx.compose.runtime.saveable.rememberSaveable
import dev.rahmanfaizur.dotline.domain.BatteryClock
import androidx.compose.ui.draw.clip
import androidx.compose.animation.Crossfade
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
import androidx.compose.runtime.key
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
import dev.rahmanfaizur.dotline.ui.common.ProvideTileTone
import dev.rahmanfaizur.dotline.ui.common.rememberTileTone
import dev.rahmanfaizur.dotline.data.Shortcut
import dev.rahmanfaizur.dotline.ui.theme.Dot
import dev.rahmanfaizur.dotline.ui.theme.LocalAccent
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.aspectRatio
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
import dev.rahmanfaizur.dotline.data.ClockStyle
import dev.rahmanfaizur.dotline.ui.theme.LocalDarkText
import kotlin.math.roundToInt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.Color
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
    /** Home tiles in order, folders included; defaults to [favorites] as plain apps. */
    val items: List<HomeItem> = favorites.map { HomeItem.App(it) },
    /** Estimated time left (or until full while charging), null until there's enough to go on. */
    val batteryLeftMs: Long? = null,
)

/** What folders on home can do (all saved by the launcher). */
class FolderActions(
    /** Making folders by dropping one app on another (Settings → Home screen → Folders). */
    val enabled: Boolean = false,
    val make: (dropped: String, target: String) -> Unit = { _, _ -> },
    val add: (app: String, folderId: String) -> Unit = { _, _ -> },
    val remove: (folderId: String, app: String) -> Unit = { _, _ -> },
    val rename: (folderId: String, name: String) -> Unit = { _, _ -> },
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
    /** Swipe down on home; null when switched off in Settings. */
    onSwipeDown: (() -> Unit)? = null,
    clockStyle: ClockStyle = ClockStyle.Big,
    homeApps: Int = 4,
    compact: Boolean = false,
    folders: FolderActions = FolderActions(),
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
    val shownItems = state.items.take(homeApps)
    fun itemAt(s: AppSlot): HomeItem? = if (s.dock) state.dock.getOrNull(s.index)?.let { HomeItem.App(it) } else shownItems.getOrNull(s.index)
    // A drop of one home app on another, waiting for "Make folder" or "Swap".
    var pendingDrop by remember { mutableStateOf<Pair<AppSlot, AppSlot>?>(null) }
    var openFolder by remember { mutableStateOf<String?>(null) }
    val onDrop: (AppSlot, AppSlot) -> Unit = { a, b ->
        val ia = itemAt(a)
        val ib = itemAt(b)
        when {
            ia == null || ib == null -> Unit
            // Folders stay on home; they never go into the dock.
            (ia is HomeItem.FolderItem && b.dock) || (ib is HomeItem.FolderItem && a.dock) -> Unit
            ia is HomeItem.App && ib is HomeItem.FolderItem && !a.dock -> folders.add(ia.key, ib.folder.id)
            folders.enabled && !a.dock && !b.dock && ia is HomeItem.App && ib is HomeItem.App -> pendingDrop = a to b
            else -> onSwapApps(a, b)
        }
    }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val spacingPx = with(LocalDensity.current) { 10.dp.toPx() }

    // Block order while dragging; saved when the finger lifts.
    var order by remember(blockOrder) { mutableStateOf(blockOrder) }
    var dragging by remember { mutableStateOf<HomeBlock?>(null) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val heights = remember { HashMap<HomeBlock, Int>() }
    // Blocks glide to their new place instead of jumping (each keeps a small offset that springs to 0).
    val shifts = remember { HashMap<HomeBlock, Animatable<Float, AnimationVector1D>>() }
    fun shift(b: HomeBlock) = shifts.getOrPut(b) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val glide = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
    fun slide(b: HomeBlock, from: Float) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            shift(b).snapTo(from)
            shift(b).animateTo(0f, glide)
        }
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
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
                slide(next, (heights[b] ?: 0) + spacingPx)
            }
        } else if (dragY < 0 && i > 0) {
            val prev = vis[i - 1]
            val h = (heights[prev] ?: 0) + spacingPx
            if (-dragY > h / 2) {
                order = order.toMutableList().apply { remove(b); add(indexOf(prev), b) }
                dragY += h
                slide(prev, -((heights[b] ?: 0) + spacingPx))
            }
        }
    }

    Box(modifier.fillMaxSize().onGloballyPositioned { origin = it.boundsInRoot().topLeft }) {
    Column(
        Modifier
            .fillMaxSize()
            .testTag("home")
            .pointerInput(editing, onSwipeDown != null) {
                if (editing) return@pointerInput
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { _, dy -> total += dy },
                    onDragEnd = {
                        if (total < -120f) onOpenApps(false)
                        else if (total > 120f) onSwipeDown?.invoke()
                    },
                )
            }
            .pointerInput(editing) { if (!editing) detectTapGestures(onLongPress = { onEditChange(true) }) }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val pending = pendingDrop
        if (editing && pending != null) {
            DropChoice(
                onFolder = {
                    val d = itemAt(pending.first)
                    val t = itemAt(pending.second)
                    if (d != null && t != null) folders.make(d.key, t.key)
                    pendingDrop = null
                },
                onSwap = { onSwapApps(pending.first, pending.second); pendingDrop = null },
                onCancel = { pendingDrop = null },
            )
        } else if (editing) {
            EditBar(onSettings = { onEditChange(false); onOpenSettings() }, onDone = { onEditChange(false) })
        } else {
            // Hidden locally at once on "Not now"; the saved setting keeps it hidden next time.
            var setHomeHidden by remember { mutableStateOf(false) }
            if (setHome != null && !setHomeHidden) {
                SetHomeTile(SetHomePrompt(set = setHome.set, notNow = { setHomeHidden = true; setHome.notNow() }), accent)
            }
        }

        order.filter(::shown).forEachIndexed { i, block ->
            // Keyed by block, so a block keeps its state (and the finger's drag) when it changes place.
            key(block) {
            Box(
                Modifier
                    .onSizeChanged { heights[block] = it.height }
                    .zIndex(if (dragging == block) 2f else if (shift(block).isRunning) 1f else 0f)
                    .graphicsLayer {
                        translationY = (if (dragging == block) dragY else 0f) + shift(block).value
                        if (dragging == block) { scaleX = 1.02f; scaleY = 1.02f }
                    }
                    .wiggle(editing && dragging != block, i),
            ) {
                when (block) {
                    HomeBlock.Clock -> Tile(Modifier.fillMaxWidth().tileTap("Open clock", enabled = !editing) { onTile(Shortcut.Clock) }.testTag("clockTile")) {
                        val t = time(now)
                        when (clockStyle) {
                            ClockStyle.Big -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AccentDot(accent)
                                    Gap(8.dp)
                                    Caption(date)
                                }
                                Gap(10.dp)
                                BasicText(t, style = Dot.dots(if (compact) 60 else 76), modifier = Modifier.testTag("clock"))
                            }
                            // Hours over minutes, with the day and date beside them.
                            ClockStyle.Stacked -> Row(verticalAlignment = Alignment.CenterVertically) {
                                val (hh, mm) = t.split(":").let { it[0].padStart(2, '0') to it.getOrElse(1) { "" } }
                                Column(Modifier.testTag("clock")) {
                                    BasicText(hh, style = Dot.dots(64).copy(lineHeight = 60.sp))
                                    BasicText(mm, style = Dot.dots(64, accent).copy(lineHeight = 60.sp))
                                }
                                Gap(18.dp)
                                Column {
                                    Caption(DateFormat.format("EEEE", Date(now)).toString())
                                    Gap(4.dp)
                                    BasicText(DateFormat.format("d MMM", Date(now)).toString(), style = Dot.dots(28))
                                }
                            }
                            // One slim line: time on the left, date on the right.
                            ClockStyle.Small -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                BasicText(t, style = Dot.dots(40), modifier = Modifier.testTag("clock"))
                                Spacer(Modifier.weight(1f))
                                AccentDot(accent)
                                Gap(8.dp)
                                Caption(date)
                            }
                        }
                    }
                    HomeBlock.Status -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Tap flips between the level and the time left; long-press opens battery settings.
                        var showTime by rememberSaveable { mutableStateOf(false) }
                        val charging = state.battery?.charging == true
                        // Only flips when there's a real estimate; if it goes away, back to the level.
                        val canFlip = state.batteryLeftMs != null
                        if (!canFlip && showTime) showTime = false
                        Tile(
                            Modifier
                                .weight(1f)
                                .height(if (compact) 98.dp else 112.dp)
                                .clip(TileShape)
                                .combinedClickable(
                                    enabled = !editing,
                                    onClickLabel = if (showTime) "Show battery level" else if (canFlip) "Show time left" else null,
                                    onLongClickLabel = "Battery settings",
                                    onLongClick = { onTile(Shortcut.Battery) },
                                ) { if (canFlip || showTime) showTime = !showTime }
                                .testTag("batteryTile"),
                        ) {
                            val level = Dot.battery(state.battery?.percent, charging)
                            val big = if (compact) 30 else 38
                            Crossfade(showTime, label = "batteryFlip") { time ->
                                Column {
                                    if (!time) {
                                        Caption(if (charging) "Charging" else "Battery")
                                        BasicText("${state.battery?.percent ?: "--"}%", style = Dot.dots(big, level), modifier = Modifier.testTag("batteryLevel"))
                                    } else {
                                        val left = state.batteryLeftMs
                                        Caption(
                                            when {
                                                charging && left == 0L -> "Charged"
                                                charging -> "Full in"
                                                else -> "Time left"
                                            },
                                        )
                                        BasicText(
                                            if (charging && left == 0L) "FULL" else left?.let(BatteryClock::format) ?: "--:--",
                                            style = Dot.dots(big, level),
                                            modifier = Modifier.testTag("batteryTime"),
                                        )
                                    }
                                }
                            }
                            DotBar(filled = ((state.battery?.percent ?: 0) + 5) / 10, lit = level, charging = charging)
                        }
                        Tile(Modifier.weight(1f).height(if (compact) 98.dp else 112.dp).tileTap("Open alarms", enabled = !editing) { onTile(Shortcut.Alarms) }) {
                            Caption("Alarm")
                            BasicText(state.nextAlarm?.let(::time) ?: "OFF", style = Dot.dots(if (compact) 30 else 38))
                            Caption(
                                state.nextAlarm?.let { DateFormat.format("EEE", Date(it)).toString() } ?: "None set",
                                color = Dot.faint,
                            )
                        }
                    }
                    HomeBlock.Apps -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Your apps: two named tiles per row, or four icon-only tiles in compact mode.
                        val perRow = if (compact) 4 else 2
                        shownItems.withIndex().chunked(perRow).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { (index, item) ->
                                    val m = Modifier.weight(1f).homeApp(item, AppSlot(false, index), actions, edit, onDrop, onOpenFolder = { openFolder = it })
                                    ItemTile(item, compact, m)
                                }
                                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                        if (shownItems.isEmpty() && state.appsLoaded) {
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
                            // Settle into the nearest place: from where the finger let go, spring into the slot.
                            val left = dragY
                            dragging = null
                            dragY = 0f
                            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                shift(block).snapTo(left)
                                shift(block).animateTo(0f, glide)
                            }
                            if (order != blockOrder) onReorder(order)
                        },
                        modifier = Modifier.align(Alignment.TopCenter).offset(y = (-11).dp),
                    )
                }
            }
            }
        }

        Spacer(Modifier.weight(1f))

        // Search pill
        val searchTone = rememberTileTone()
        Box(
            Modifier
                .fillMaxWidth()
                .tile(RoundedCornerShape(50), searchTone)
                .clickable(enabled = !editing) { onOpenApps(true) }
                .testTag("searchPill")
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            ProvideTileTone(searchTone) { BasicText("Search apps", style = Dot.label.copy(color = Dot.muted, fontSize = 15.sp)) }
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
                    Box(Modifier.homeApp(HomeItem.App(app), AppSlot(true, index), actions, edit, onDrop).padding(4.dp)) {
                        AppIcon(app, 48.dp)
                    }
                }
            }
        }
    }

    // The app being dragged, drawn above everything and following the finger.
    val from = edit?.from
    val dragged = edit?.item
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
                if (from.dock && dragged is HomeItem.App) {
                    Box(Modifier.padding(4.dp)) { AppIcon(dragged.entry, 48.dp) }
                } else {
                    ItemTile(dragged, compact, Modifier.fillMaxSize().background(Dot.SurfaceHigh, TileShape))
                }
            }
        }
    }

    // An open folder: its apps over a dimmed home. Tap outside or press Back to close.
    val folderOpen = openFolder?.let { id -> state.items.firstOrNull { it is HomeItem.FolderItem && it.folder.id == id } as? HomeItem.FolderItem }
    if (openFolder != null && folderOpen == null) openFolder = null
    if (folderOpen != null) {
        androidx.activity.compose.BackHandler { openFolder = null }
        FolderPanel(
            item = folderOpen,
            actions = actions,
            onRename = { folders.rename(folderOpen.folder.id, it) },
            onRemove = { folders.remove(folderOpen.folder.id, it) },
            onClose = { openFolder = null },
        )
    }
    }
}

/** A home tile for an app or a folder (icon-only in compact mode). */
@Composable
private fun ItemTile(item: HomeItem, compact: Boolean, modifier: Modifier) {
    when (item) {
        is HomeItem.App -> if (compact) IconTile(item.entry, modifier) else AppTile(item.entry, modifier)
        is HomeItem.FolderItem -> if (compact) {
            Box(modifier.aspectRatio(1f).tile(TileShape), contentAlignment = Alignment.Center) { MiniGrid(item.apps, 44.dp) }
        } else {
            val tone = rememberTileTone()
            Row(
                modifier.tile(TileShape, tone).padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProvideTileTone(tone) {
                    MiniGrid(item.apps, 38.dp)
                    Gap(10.dp)
                    BasicText(item.folder.name, style = Dot.label.copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Up to four app icons in a 2×2 square: how a folder looks on home. */
@Composable
private fun MiniGrid(apps: List<AppEntry>, size: androidx.compose.ui.unit.Dp) {
    val cell = size / 2 - 1.dp
    Column(Modifier.size(size), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        apps.take(4).chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { row.forEach { AppIcon(it, cell) } }
        }
    }
}

/** The bar that appears after dropping one home app on another (with folders on). */
@Composable
private fun DropChoice(onFolder: () -> Unit, onSwap: () -> Unit, onCancel: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().tile(TileShape).padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText("Put them together?", style = Dot.label.copy(fontSize = 13.sp), modifier = Modifier.weight(1f))
        BasicText("Cancel", style = Dot.label.copy(fontSize = 13.sp), modifier = Modifier.clickable(onClick = onCancel).testTag("dropCancel").padding(horizontal = 8.dp, vertical = 8.dp))
        BasicText("Swap", style = Dot.label.copy(fontSize = 13.sp), modifier = Modifier.clickable(onClick = onSwap).testTag("dropSwap").padding(horizontal = 8.dp, vertical = 8.dp))
        BasicText(
            "Make folder",
            style = Dot.label.copy(color = Dot.Black, fontSize = 13.sp),
            modifier = Modifier
                .background(LocalAccent.current, RoundedCornerShape(50))
                .clickable(onClick = onFolder)
                .testTag("dropFolder")
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

/** A folder opened: its name (tap to rename) and its apps. Long-press an app to take it out. */
@Composable
private fun FolderPanel(
    item: HomeItem.FolderItem,
    actions: AppActions,
    onRename: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClose: () -> Unit,
) {
    var name by remember(item.folder.id) { mutableStateOf(item.folder.name) }
    fun close() { if (name != item.folder.name) onRename(name); onClose() }
    CompositionLocalProvider(LocalDarkText provides false) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xB3000000))
                .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { close() }
                .testTag("folderPanel"),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .fillMaxWidth(0.88f)
                    .background(Dot.Surface, RoundedCornerShape(28.dp))
                    .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    singleLine = true,
                    textStyle = Dot.dots(28),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(LocalAccent.current),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onRename(name) }),
                    modifier = Modifier.fillMaxWidth().testTag("folderName"),
                )
                item.apps.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { app ->
                            Column(
                                Modifier
                                    .weight(1f)
                                    .combinedClickable(
                                        onClick = { onClose(); actions.launch(app, null) },
                                        onLongClick = { onRemove(app.component.flattenToString()) },
                                        onLongClickLabel = "Take out of folder",
                                    )
                                    .testTag("inFolder:${app.label}")
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                AppIcon(app, 52.dp)
                                Gap(6.dp)
                                BasicText(app.label, style = Dot.label.copy(fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Caption("Long-press an app to take it out")
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

/** Compact mode: just the icon on a small square tile. */
@Composable
private fun IconTile(app: AppEntry, modifier: Modifier) {
    Box(modifier.aspectRatio(1f).tile(TileShape), contentAlignment = Alignment.Center) {
        AppIcon(app, 44.dp)
    }
}

@Composable
private fun AppTile(app: AppEntry, modifier: Modifier) {
    val tone = rememberTileTone()
    Row(
        modifier
            .tile(TileShape, tone)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { ProvideTileTone(tone) {
        AppIcon(app, 38.dp)
        Gap(10.dp)
        BasicText(app.label, style = Dot.label.copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    } }
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
