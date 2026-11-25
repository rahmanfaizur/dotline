package dev.rahmanfaizur.dotline.ui.home

import android.graphics.Rect
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import dev.rahmanfaizur.dotline.data.AppEntry

/** Where an app's icon is on screen, so the app can open by growing out of it. */
private class BoundsHolder { var rect: Rect? = null }

/** Tap opens the app (animating from its icon); long-press (with a buzz) opens its menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.appClicks(app: AppEntry, actions: AppActions): Modifier {
    val haptics = LocalHapticFeedback.current
    val holder = remember { BoundsHolder() } // plain holder: layout changes don't trigger recomposition
    return onGloballyPositioned { coords ->
        val b = coords.boundsInWindow()
        holder.rect = Rect(b.left.toInt(), b.top.toInt(), b.right.toInt(), b.bottom.toInt())
    }.combinedClickable(
        onClick = { actions.launch(app, holder.rect) },
        onLongClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            actions.longPress(app)
        },
        onLongClickLabel = "App options",
    )
}
