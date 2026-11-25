package dev.rahmanfaizur.dotline.ui.home

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
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/** Swipe right from home: calendar, free storage, battery health, quick-open apps. */
@Composable
fun QuickPage(
    quick: List<AppEntry>,
    storage: StorageState?,
    battery: BatteryState?,
    actions: AppActions,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(Dot.Black)
            .testTag("quickPage")
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CalendarTile(Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(Modifier.weight(1f).height(112.dp)) {
                Caption("Free storage")
                // Whole numbers only: the dot font has no good-looking decimal point.
                BasicText(storage?.let { "${Math.round(it.freeBytes / 1e9)}GB" } ?: "--", style = Dot.dots(34))
                Caption(storage?.let { "${Sizes.percentUsed(it.freeBytes, it.totalBytes)}% used" } ?: "", color = Dot.Faint)
            }
            Tile(Modifier.weight(1f).height(112.dp)) {
                Caption("Battery health")
                // Android 12 doesn't let apps read a health percentage, only this rating.
                BasicText(
                    when (battery?.healthGood) { true -> "GOOD"; false -> "CHECK"; null -> "--" },
                    style = Dot.dots(34),
                )
                Caption("Android's rating", color = Dot.Faint)
            }
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
            BasicText("⚙", style = Dot.label.copy(color = Dot.Muted, fontSize = 16.sp))
            Gap(10.dp)
            Caption("Launcher settings")
        }
    }
}

@Composable
private fun CalendarTile(modifier: Modifier) {
    val accent = LocalAccent.current
    val now by rememberMinuteTicker()
    val zone = ZoneId.systemDefault()
    val today = remember(now / 60_000) { Instant.ofEpochMilli(now).atZone(zone).toLocalDate() }
    val locale = Locale.getDefault()
    val grid = remember(today, locale) {
        MonthGrid.of(YearMonth.from(today), today, WeekFields.of(locale).firstDayOfWeek)
    }

    Tile(modifier) {
        Caption("${today.month.getDisplayName(TextStyle.FULL, locale)} ${today.year}")
        Gap(12.dp)
        val cells = List(grid.blanks) { 0 } + (1..grid.days).toList()
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                grid.weekdayInitials.forEach { c ->
                    BasicText(c.toString(), style = Dot.caption.copy(fontSize = 10.sp, color = Dot.Faint, textAlign = TextAlign.Center), modifier = Modifier.size(width = 20.dp, height = 14.dp))
                }
            }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    (0 until 7).forEach { i ->
                        val day = week.getOrNull(i) ?: 0
                        Box(Modifier.size(width = 20.dp, height = 12.dp), contentAlignment = Alignment.Center) {
                            if (day > 0) {
                                val isToday = day == grid.today
                                val color = when {
                                    isToday -> accent
                                    grid.today != null && day < grid.today -> Dot.Muted
                                    else -> Dot.SurfaceHigh
                                }
                                Box(Modifier.size(if (isToday) 11.dp else 8.dp).background(color, CircleShape))
                            }
                        }
                    }
                }
            }
        }
    }
}
