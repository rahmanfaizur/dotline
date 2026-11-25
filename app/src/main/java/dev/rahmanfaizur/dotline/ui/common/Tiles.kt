package dev.rahmanfaizur.dotline.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.rahmanfaizur.dotline.ui.theme.Dot
import kotlinx.coroutines.delay

val TileShape = RoundedCornerShape(22.dp)

/** Makes a whole tile tappable, with the ripple kept inside its rounded corners. */
fun Modifier.tileTap(label: String, onClick: () -> Unit): Modifier =
    clip(TileShape).clickable(onClickLabel = label, onClick = onClick)

/** The black rounded card every Bento block sits on. */
@Composable
fun Tile(
    modifier: Modifier = Modifier,
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .tile(TileShape)
            .padding(padding),
        verticalArrangement = Arrangement.SpaceBetween,
        content = content,
    )
}

/** Small uppercase grey label, e.g. "BATTERY". */
@Composable
fun Caption(text: String, modifier: Modifier = Modifier, color: Color = Dot.Muted) {
    BasicText(text.uppercase(), style = Dot.caption.copy(color = color), modifier = modifier)
}

/** A red (accent) dot, used as a bullet. */
@Composable
fun AccentDot(color: Color, size: Dp = 7.dp) {
    Box(Modifier.size(size).background(color, CircleShape))
}

/** Ten dots, [filled] of them lit: a battery bar in the dot style. */
@Composable
fun DotBar(filled: Int, lit: Color, modifier: Modifier = Modifier, dot: Dp = 7.dp) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(10) { i ->
            Box(Modifier.size(dot).background(if (i < filled) lit else Dot.SurfaceHigh, CircleShape))
        }
    }
}

@Composable
fun Gap(size: Dp) = Spacer(Modifier.size(size))

/** Current time in ms, updated on every minute boundary. */
@Composable
fun rememberMinuteTicker(): State<Long> {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            val t = System.currentTimeMillis()
            now.longValue = t
            delay(60_000L - t % 60_000L + 50L)
        }
    }
    return now
}
