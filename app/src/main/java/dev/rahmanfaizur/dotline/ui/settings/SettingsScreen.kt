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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import dev.rahmanfaizur.dotline.data.DrawerBackground
import dev.rahmanfaizur.dotline.data.TileStyle
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
    /** Puts the lock-screen version of the chosen wallpaper on the lock screen. */
    val useOnLockScreen: () -> Unit,
    val setAsHome: () -> Unit,
    val changeHome: () -> Unit,
    val close: () -> Unit,
)

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

        LookSection(settings, backdrop, hasPhoto, photoVersion, thumbnail, actions, accent)

        Section("Lock screen") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    "Use this wallpaper on the lock screen too",
                    style = Dot.label.copy(fontSize = 14.sp),
                    modifier = Modifier.weight(1f),
                )
                Gap(10.dp)
                Chip("Set", selected = true, accent = accent, tag = "useOnLock", onClick = actions.useOnLockScreen)
            }
            BasicText(
                lockNote ?: "The lock screen clock stays your phone's own. Undo any time in the phone's wallpaper settings.",
                style = Dot.label.copy(color = Dot.Muted),
                modifier = Modifier.testTag("lockNote"),
            )
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
    Section("Look") {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            LookPreview(backdrop, settings.tileStyle)
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

        Caption("Tiles")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TileNames.forEach { (style, name) ->
                Chip(name, settings.tileStyle == style, accent, tag = "tiles:$name") { actions.setTileStyle(style) }
            }
        }
        if (backdrop == null && settings.tileStyle in listOf(TileStyle.Smoke, TileStyle.Glass, TileStyle.Tinted)) {
            BasicText("On a black wallpaper these tiles show as solid.", style = Dot.label.copy(color = Dot.Muted))
        }

        Caption("App list")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DrawerNames.forEach { (bg, name) ->
                Chip(name, settings.drawerBackground == bg, accent, tag = "drawerBg:$name") { actions.setDrawerBackground(bg) }
            }
        }
    }
}

/** A tiny home screen drawn with the real wallpaper and tile code. */
@Composable
private fun LookPreview(backdrop: Backdrop?, style: TileStyle) {
    WallpaperArea(
        backdrop = backdrop,
        style = style,
        modifier = Modifier
            .width(132.dp)
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
