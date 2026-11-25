package dev.rahmanfaizur.dotline.ui.settings

import androidx.compose.foundation.background
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
    val setAsHome: () -> Unit,
    val changeHome: () -> Unit,
    val close: () -> Unit,
)

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    isDefaultHome: Boolean,
    lists: SettingsLists,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccent.current
    Column(
        modifier
            .fillMaxSize()
            .background(Dot.Black)
            .testTag("settings")
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText("SETTINGS", style = Dot.dots(40), modifier = Modifier.weight(1f))
            BasicText(
                "✕",
                style = Dot.label.copy(color = Dot.Muted, fontSize = 22.sp),
                modifier = Modifier.clickable(onClick = actions.close).padding(12.dp),
            )
        }

        Section("Home screen") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    if (isDefaultHome) "Dotline is your home screen" else "Dotline isn't your home screen yet",
                    style = Dot.label.copy(fontSize = 14.sp),
                    modifier = Modifier.weight(1f),
                )
                Gap(10.dp)
                if (isDefaultHome) {
                    Chip("Change", selected = false, accent = accent, tag = "changeHome", onClick = actions.changeHome)
                } else {
                    Chip("Set as home screen", selected = true, accent = accent, tag = "setAsHome", onClick = actions.setAsHome)
                }
            }
        }

        AppListSection("Home apps", lists.favorites, actions.removeFavorite)
        AppListSection("Dock", lists.dock, actions.removeDock)
        AppListSection("Quick open", lists.quick, actions.removeQuick)

        Section("Accent colour") {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Dot.Accents.forEachIndexed { i, color ->
                    val selected = settings.accent == i
                    Box(
                        Modifier
                            .size(34.dp)
                            .then(if (selected) Modifier.border(2.dp, Dot.Ink, CircleShape) else Modifier)
                            .padding(5.dp)
                            .background(color, CircleShape)
                            .clickable { actions.setAccent(i) }
                            .testTag("accent:${Dot.AccentNames[i]}"),
                    )
                }
            }
        }

        Section("Icon shape") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShapeOption("Round", CircleShape, settings.iconShape == IconShape.Round, accent) {
                    actions.setIconShape(IconShape.Round)
                }
                ShapeOption("Rounded square", Dot.iconShape(IconShape.Square), settings.iconShape == IconShape.Square, accent) {
                    actions.setIconShape(IconShape.Square)
                }
            }
        }

        Section("Clock") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Phone setting", settings.clock == ClockMode.System, accent) { actions.setClock(ClockMode.System) }
                Chip("24-hour", settings.clock == ClockMode.H24, accent) { actions.setClock(ClockMode.H24) }
                Chip("12-hour", settings.clock == ClockMode.H12, accent) { actions.setClock(ClockMode.H12) }
            }
        }

        Section("Quick page") {
            Row(
                Modifier.fillMaxWidth().clickable { actions.setShowQuickPage(!settings.showQuickPage) }.testTag("toggle:quickPage"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText("Swipe right for calendar and storage", style = Dot.label.copy(fontSize = 14.sp), modifier = Modifier.weight(1f))
                Switch(settings.showQuickPage, accent)
            }
        }

        Section("Hidden apps") {
            if (lists.hidden.isEmpty()) {
                BasicText("None. Long-press an app → Hide from all apps.", style = Dot.label.copy(color = Dot.Muted))
            } else {
                lists.hidden.forEach { app -> AppRow(app, "Show", actions.unhide) }
            }
        }

        Section("About") {
            BasicText("Dotline · no internet access · settings stay on this phone", style = Dot.label.copy(color = Dot.Muted))
        }
        Spacer(Modifier.size(24.dp))
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
        Caption(title)
        content()
    }
}

@Composable
private fun AppListSection(title: String, apps: List<AppEntry>, onRemove: (AppEntry) -> Unit) {
    Section("$title · ${apps.size}/4") {
        if (apps.isEmpty()) {
            BasicText("Empty. Long-press an app to add one.", style = Dot.label.copy(color = Dot.Muted))
        }
        apps.forEach { app -> AppRow(app, "Remove", onRemove) }
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
