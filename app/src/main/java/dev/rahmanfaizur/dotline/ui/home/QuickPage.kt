package dev.rahmanfaizur.dotline.ui.home

import dev.rahmanfaizur.dotline.ui.common.TileShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.combinedClickable
import java.time.DayOfWeek
import androidx.compose.foundation.border
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rahmanfaizur.dotline.data.AppEntry
import dev.rahmanfaizur.dotline.data.BatteryState
import dev.rahmanfaizur.dotline.data.StorageState
import dev.rahmanfaizur.dotline.domain.MonthGrid
import dev.rahmanfaizur.dotline.domain.Sizes
import dev.rahmanfaizur.dotline.ui.AppIcon
import dev.rahmanfaizur.dotline.ui.common.Caption
import dev.rahmanfaizur.dotline.ui.common.Gap
import dev.rahmanfaizur.dotline.ui.common.Tile
import dev.rahmanfaizur.dotline.ui.common.rememberMinuteTicker
import dev.rahmanfaizur.dotline.ui.theme.Dot
import dev.rahmanfaizur.dotline.ui.theme.LocalAccent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.rahmanfaizur.dotline.data.MemoryState
import dev.rahmanfaizur.dotline.data.Shortcut
import dev.rahmanfaizur.dotline.ui.common.tileTap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/** Swipe right from home: calendar, free storage, live RAM, quick-open apps. Tiles open their app. */
@Composable
fun QuickPage(
    quick: List<AppEntry>,
    storage: StorageState?,
    actions: AppActions,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    readMemory: () -> MemoryState? = { null },
    onTile: (Shortcut) -> Unit = {},
    battery: BatteryState? = null,
    /** Long-press on battery health: its remove menu. Null hides the tile. */
    batteryHealthMenu: (() -> Unit)? = null,
) {
    // RAM updates every 2 s, only while this page is on screen and the launcher is in front.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val memory by produceState<MemoryState?>(null, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                value = withContext(Dispatchers.Default) { readMemory() }
                delay(2_000)
            }
        }
    }
    Column(
        modifier
            .fillMaxSize()
            .testTag("quickPage")
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CalendarTile(Modifier.fillMaxWidth().tileTap("Open calendar") { onTile(Shortcut.Calendar) })

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(Modifier.weight(1f).height(112.dp).tileTap("Open files") { onTile(Shortcut.Storage) }) {
                Caption("Free storage")
                // Whole numbers only: the dot font has no good-looking decimal point.
                BasicText(storage?.let { "${Math.round(it.freeBytes / 1e9)}GB" } ?: "--", style = Dot.dots(34))
                Caption(storage?.let { "${Sizes.percentUsed(it.freeBytes, it.totalBytes)}% used" } ?: "", color = Dot.faint)
            }
            Tile(Modifier.weight(1f).height(112.dp).tileTap("Memory details") { onTile(Shortcut.Memory) }.testTag("ramTile")) {
                Caption("RAM in use")
                BasicText(memory?.let { "${Sizes.percentUsed(it.totalBytes - it.usedBytes, it.totalBytes)}%" } ?: "--", style = Dot.dots(34))
                Caption(memory?.let { "${Sizes.short(it.usedBytes)} of ${Sizes.short(it.totalBytes)}" } ?: "", color = Dot.faint)
            }
        }

        if (battery != null && batteryHealthMenu != null) {
            BatteryHealthTile(
                battery,
                Modifier
                    .fillMaxWidth()
                    .clip(TileShape)
                    .combinedClickable(
                        onClickLabel = "Battery settings",
                        onLongClickLabel = "Remove battery health",
                        onLongClick = batteryHealthMenu,
                    ) { onTile(Shortcut.Battery) },
            )
        }

        if (quick.isNotEmpty()) {
            Tile(Modifier.fillMaxWidth()) {
                Caption("Quick open")
                Gap(12.dp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    quick.take(4).forEach { app ->
                        Column(
                            Modifier.appClicks(app, actions).padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AppIcon(app, 48.dp)
                            Gap(6.dp)
                            BasicText(
                                app.label,
                                style = Dot.label.copy(fontSize = 11.sp, textAlign = TextAlign.Center),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.size(width = 68.dp, height = 16.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenSettings)
                .testTag("openSettings")
                .padding(vertical = 14.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText("⚙", style = Dot.label.copy(color = Dot.muted, fontSize = 16.sp))
            Gap(10.dp)
            Caption("Launcher settings")
        }
    }
}

/** Temperature, health and charge cycles, as far as the phone reports them. */
@Composable
private fun BatteryHealthTile(battery: BatteryState, modifier: Modifier) {
    Tile(modifier.testTag("batteryHealth")) {
        Caption("Battery health")
        Gap(10.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val hot = (battery.tempC ?: 0) >= 40
            Stat("Temp", battery.tempC?.let { "${it}C" } ?: "--", if (hot) Dot.battery(15, false) else Dot.ink)
            Stat(
                "Health",
                when (battery.healthGood) { true -> "GOOD"; false -> "CHECK"; null -> "--" },
                if (battery.healthGood == false) Dot.battery(15, false) else Dot.ink,
            )
            Stat("Cycles", battery.cycles?.toString() ?: "--", Dot.ink)
        }
    }
}

@Composable
private fun Stat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Caption(label, color = Dot.faint)
        Gap(4.dp)
        BasicText(value, style = Dot.dots(24, color), maxLines = 1)
    }
}

/**
 * The month at a glance: today's date big, the days as dots (gone, today pulsing, to come; weekends
 * softer), and a thin line of one dot per day showing how far through the month we are.
 */
@Composable
private fun CalendarTile(modifier: Modifier) {
    val accent = LocalAccent.current
    val now by rememberMinuteTicker()
    val zone = ZoneId.systemDefault()
    val today = remember(now / 60_000) { Instant.ofEpochMilli(now).atZone(zone).toLocalDate() }
    val locale = Locale.getDefault()
    val weekFields = WeekFields.of(locale)
    val grid = remember(today, locale) {
        MonthGrid.of(YearMonth.from(today), today, weekFields.firstDayOfWeek)
    }
    val pulse by rememberInfiniteTransition(label = "today").animateFloat(
        initialValue = 0.25f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "todayPulse",
    )
    fun weekend(day: Int) = today.withDayOfMonth(day).dayOfWeek.let { it == DayOfWeek.SATURDAY || it == DayOfWeek.SUNDAY }

    Tile(modifier.testTag("calendarTile")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(today.dayOfMonth.toString(), style = Dot.dots(64), modifier = Modifier.testTag("calendarDay"))
            Gap(16.dp)
            Column {
                Caption(today.dayOfWeek.getDisplayName(TextStyle.FULL, locale), color = accent)
                Gap(3.dp)
                BasicText("${today.month.getDisplayName(TextStyle.FULL, locale)} ${today.year}", style = Dot.label.copy(fontSize = 15.sp))
                Gap(3.dp)
                val left = grid.days - today.dayOfMonth
                val leftText = when (left) { 0 -> "Last day"; 1 -> "1 day left"; else -> "$left days left" }
                Caption("$leftText · Week ${today.get(weekFields.weekOfWeekBasedYear())}", color = Dot.faint)
            }
        }
        Gap(16.dp)
        val cells = List(grid.blanks) { 0 } + (1..grid.days).toList()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                grid.weekdayInitials.forEachIndexed { i, c ->
                    val end = grid.weekStart.plus(i.toLong()).let { it == DayOfWeek.SATURDAY || it == DayOfWeek.SUNDAY }
                    BasicText(
                        c.toString(),
                        style = Dot.caption.copy(fontSize = 10.sp, color = if (end) Dot.faint.copy(alpha = 0.3f) else Dot.faint, textAlign = TextAlign.Center),
                        modifier = Modifier.size(width = 22.dp, height = 14.dp),
                    )
                }
            }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    (0 until 7).forEach { i ->
                        val day = week.getOrNull(i) ?: 0
                        Box(Modifier.size(width = 22.dp, height = 16.dp), contentAlignment = Alignment.Center) {
                            if (day > 0) {
                                val isToday = day == grid.today
                                val past = grid.today != null && day < grid.today
                                if (isToday) {
                                    Box(Modifier.size(16.dp).border(1.5.dp, accent.copy(alpha = pulse), CircleShape))
                                    Box(Modifier.size(9.dp).background(accent, CircleShape))
                                } else {
                                    val color = when {
                                        past -> if (weekend(day)) Dot.muted.copy(alpha = 0.45f) else Dot.muted
                                        weekend(day) -> Dot.off.copy(alpha = 0.12f)
                                        else -> Dot.off
                                    }
                                    Box(Modifier.size(8.dp).background(color, CircleShape))
                                }
                            }
                        }
                    }
                }
            }
        }
        Gap(14.dp)
        // How far through the month: one tiny dot per day.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            (1..grid.days).forEach { d ->
                val done = grid.today != null && d <= grid.today
                Box(Modifier.size(if (d == grid.today) 5.dp else 3.dp).background(if (done) accent.copy(alpha = if (d == grid.today) 1f else 0.6f) else Dot.off, CircleShape))
            }
        }
    }
}
