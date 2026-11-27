package dev.rahmanfaizur.dotline.ui.common

import kotlin.math.abs
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
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
fun Modifier.tileTap(label: String, enabled: Boolean = true, onClick: () -> Unit): Modifier =
    clip(TileShape).clickable(enabled = enabled, onClickLabel = label, onClick = onClick)

/** The black rounded card every Bento block sits on. */
@Composable
fun Tile(
    modifier: Modifier = Modifier,
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tone = rememberTileTone()
    Column(
        modifier
            .tile(TileShape, tone)
            .padding(padding),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        val scope = this
        ProvideTileTone(tone) { scope.content() }
    }
}

/** Small uppercase grey label, e.g. "BATTERY". */
@Composable
fun Caption(text: String, modifier: Modifier = Modifier, color: Color = Dot.muted) {
    BasicText(text.uppercase(), style = Dot.caption.copy(color = color), modifier = modifier)
}

/** A red (accent) dot, used as a bullet. */
@Composable
fun AccentDot(color: Color, size: Dp = 7.dp) {
    Box(Modifier.size(size).background(color, CircleShape))
}

/** Ten dots, [filled] of them lit: a battery bar in the dot style. */
@Composable
fun DotBar(filled: Int, lit: Color, modifier: Modifier = Modifier, dot: Dp = 7.dp, charging: Boolean = false) {
    // While charging, a slow wave of light runs from the last lit dot to the end, over and over.
    val wave = if (charging && filled < 10) {
        val t = rememberInfiniteTransition(label = "chargeWave")
        t.animateFloat(
            initialValue = filled - 1.5f, targetValue = 11.5f,
            animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "chargePhase",
        )
    } else null
    val off = Dot.off
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(10) { i ->
            Box(
                Modifier.size(dot).drawBehind {
                    val c = when {
                        i < filled -> lit
                        wave != null -> lerp(off, lit, (1f - abs(wave.value - i) / 1.6f).coerceIn(0f, 1f) * 0.85f)
                        else -> off
                    }
                    drawCircle(c)
                },
            )
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
