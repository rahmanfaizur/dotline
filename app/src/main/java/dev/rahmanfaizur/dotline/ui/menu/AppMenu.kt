package dev.rahmanfaizur.dotline.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rahmanfaizur.dotline.data.AppEntry
import dev.rahmanfaizur.dotline.ui.AppIcon
import dev.rahmanfaizur.dotline.ui.common.Caption
import dev.rahmanfaizur.dotline.ui.common.Gap
import dev.rahmanfaizur.dotline.ui.theme.Dot
import dev.rahmanfaizur.dotline.ui.theme.LocalAccent

/** Where the app currently is, so the menu can say "Add" or "Remove". */
class MenuTarget(
    val app: AppEntry,
    val onHome: Boolean,
    val inDock: Boolean,
    val inQuick: Boolean,
    val hidden: Boolean,
    val canUninstall: Boolean,
)

class MenuActions(
    val info: () -> Unit,
    val toggleHome: () -> Unit,
    val toggleDock: () -> Unit,
    val toggleQuick: () -> Unit,
    val toggleHidden: () -> Unit,
    val uninstall: () -> Unit,
    val dismiss: () -> Unit,
)

/** Bottom sheet over a dimmed screen. Tap outside or press Back to close. */
@Composable
fun AppMenu(target: MenuTarget, actions: MenuActions, note: String?, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    Box(
        modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .pointerInput(Unit) { detectTapGestures { actions.dismiss() } }
            .testTag("appMenu"),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .navigationBarsPadding()
                .background(Dot.Surface, RoundedCornerShape(26.dp))
                // Swallow taps on the card so they don't close the menu.
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(vertical = 10.dp),
        ) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(target.app, 44.dp)
                Gap(14.dp)
                BasicText(target.app.label, style = Dot.label.copy(fontSize = 17.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (note != null) {
                Caption(note, color = accent, modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp))
            }
            Item("App info", actions.info)
            Item(if (target.onHome) "Remove from home" else "Add to home", actions.toggleHome)
            Item(if (target.inDock) "Remove from dock" else "Add to dock", actions.toggleDock)
            Item(if (target.inQuick) "Remove from quick open" else "Add to quick open", actions.toggleQuick)
            Item(if (target.hidden) "Show in all apps" else "Hide from all apps", actions.toggleHidden)
            if (target.canUninstall) Item("Uninstall", actions.uninstall, color = accent)
        }
    }
}

@Composable
private fun Item(text: String, onClick: () -> Unit, color: Color = Dot.Ink) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("menu:$text")
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(text, style = Dot.label.copy(color = color, fontSize = 15.sp), modifier = Modifier.width(280.dp))
    }
}
