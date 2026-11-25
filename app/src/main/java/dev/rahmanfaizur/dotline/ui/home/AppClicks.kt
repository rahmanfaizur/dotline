package dev.rahmanfaizur.dotline.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import dev.rahmanfaizur.dotline.data.AppEntry

/** Tap opens the app; long-press (with a buzz) opens its menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.appClicks(app: AppEntry, actions: AppActions): Modifier {
    val haptics = LocalHapticFeedback.current
    return combinedClickable(
        onClick = { actions.launch(app) },
        onLongClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            actions.longPress(app)
        },
        onLongClickLabel = "App options",
    )
}
