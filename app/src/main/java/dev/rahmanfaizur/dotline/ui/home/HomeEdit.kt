package dev.rahmanfaizur.dotline.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.rahmanfaizur.dotline.data.AppEntry
import dev.rahmanfaizur.dotline.data.HomeBlock
import dev.rahmanfaizur.dotline.ui.theme.LocalAccent

/** A place an app sits on home: one of the four home tiles, or a dock spot. */
data class AppSlot(val dock: Boolean, val index: Int)

/** Edit mode: where things are and what's being dragged. Plain fields for bounds so layout never recomposes. */
class HomeEditState {
    val bounds = HashMap<AppSlot, Rect>()
    var from by mutableStateOf<AppSlot?>(null)
        private set
    var app by mutableStateOf<AppEntry?>(null)
        private set
    var offset by mutableStateOf(Offset.Zero)
    private var grab = Offset.Zero

    fun start(slot: AppSlot, entry: AppEntry, touch: Offset) {
        from = slot; app = entry; offset = Offset.Zero
        grab = (bounds[slot]?.topLeft ?: Offset.Zero) + touch
    }

    /** The slot under the finger, if it's a different one. */
    fun target(): AppSlot? {
        val p = grab + offset
        return bounds.entries.firstOrNull { it.key != from && it.value.contains(p) }?.key
    }

    fun clear() { from = null; app = null; offset = Offset.Zero }
}

/**
 * An app on home. Normally tap opens it and long-press shows its menu; in edit mode it follows
 * your finger and swaps places with whatever app it's dropped on (home tile or dock).
 */
fun Modifier.homeApp(
    app: AppEntry,
    slot: AppSlot,
    actions: AppActions,
    edit: HomeEditState?,
    onSwap: (AppSlot, AppSlot) -> Unit,
): Modifier = composed {
    if (edit == null) return@composed appClicks(app, actions)
    val accent = LocalAccent.current
    val hovered = edit.from != null && edit.from != slot && edit.target() == slot
    this
        .onGloballyPositioned { edit.bounds[slot] = it.boundsInRoot() }
        .graphicsLayer { alpha = if (edit.from == slot) 0.3f else 1f }
        .then(if (hovered) Modifier.border(2.dp, accent, RoundedCornerShape(22.dp)) else Modifier)
        .testTag(if (slot.dock) "slot:dock:${slot.index}" else "slot:home:${slot.index}")
        .pointerInput(slot, app) {
            detectDragGestures(
                onDragStart = { edit.start(slot, app, it) },
                onDrag = { change, d -> change.consume(); edit.offset += d },
                onDragEnd = {
                    edit.target()?.let { onSwap(slot, it) }
                    edit.clear()
                },
                onDragCancel = { edit.clear() },
            )
        }
}

/** A gentle wobble so it's obvious things can be moved. */
fun Modifier.wiggle(on: Boolean, seed: Int): Modifier = composed {
    if (!on) return@composed this
    val t = rememberInfiniteTransition(label = "wiggle")
    val r by t.animateFloat(
        initialValue = -0.6f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(260 + (seed % 3) * 40, easing = LinearEasing), RepeatMode.Reverse),
        label = "angle",
    )
    graphicsLayer { rotationZ = if (seed % 2 == 0) r else -r }
}

/** The grip on top of each block in edit mode: drag it up or down to move the block. */
@Composable
fun BlockHandle(block: HomeBlock, onDrag: (Float) -> Unit, onStart: () -> Unit, onEnd: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width = 64.dp, height = 22.dp)
            .background(Color(0xE61A1A1A), RoundedCornerShape(50))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(50))
            .testTag("handle:${block.name}")
            .pointerInput(block) {
                detectDragGestures(
                    onDragStart = { onStart() },
                    onDrag = { change, d -> change.consume(); onDrag(d.y) },
                    onDragEnd = onEnd,
                    onDragCancel = onEnd,
                )
            },
    ) {
        Row(Modifier.padding(top = 6.dp, start = 20.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { Box(Modifier.size(5.dp).background(Color(0xFFF2F2F2), CircleShape)) }
        }
    }
}
