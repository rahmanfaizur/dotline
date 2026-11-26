package dev.rahmanfaizur.dotline.ui.home

import android.os.SystemClock
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.rahmanfaizur.dotline.data.PlayerStyle
import dev.rahmanfaizur.dotline.notify.NowPlaying
import dev.rahmanfaizur.dotline.ui.common.Gap
import dev.rahmanfaizur.dotline.ui.common.TileShape
import dev.rahmanfaizur.dotline.ui.common.tile
import dev.rahmanfaizur.dotline.ui.theme.Dot
import dev.rahmanfaizur.dotline.ui.theme.LocalAccent
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private val SpotifyGreen = Color(0xFF1ED760)

/** Where it's playing and what the tile's buttons open. */
class PlayerActions(
    val sourceName: String,
    val outputName: String,
    val openPlayer: () -> Unit,
    val openOutput: () -> Unit,
)

/** Now playing on home, in the style picked in Settings → Notifications. */
@Composable
fun NowPlayingTile(np: NowPlaying, style: PlayerStyle, actions: PlayerActions, modifier: Modifier = Modifier) {
    val art = remember(np.art) { np.art?.asImageBitmap() }
    // The position only ticks while the launcher is in front (faster while playing).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val position by produceState(np.positionNow(SystemClock.elapsedRealtime()), np, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                value = np.positionNow(SystemClock.elapsedRealtime())
                delay(if (np.playing) 250 else 1_000)
            }
        }
    }
    val fraction = if (np.durationMs > 0) (position.toFloat() / np.durationMs).coerceIn(0f, 1f) else 0f
    val seek: (Float) -> Unit = { f -> if (np.durationMs > 0) np.controller.transportControls.seekTo((f * np.durationMs).toLong()) }
    val m = modifier.fillMaxWidth().testTag("nowPlaying")
    when (style) {
        PlayerStyle.Glow -> GlowPlayer(np, art, fraction, seek, actions, m)
        PlayerStyle.DotWave -> DotWavePlayer(np, art, position, fraction, seek, actions, m)
        PlayerStyle.Vinyl -> VinylPlayer(np, art, fraction, actions, m)
    }
}

// ---------- 1. Cover glow: the blurred cover fills the tile ----------

@Composable
private fun GlowPlayer(np: NowPlaying, art: ImageBitmap?, fraction: Float, seek: (Float) -> Unit, a: PlayerActions, modifier: Modifier) {
    Box(modifier.clip(TileShape).background(Dot.Surface)) {
        if (art != null) {
            Image(art, null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize().blur(28.dp))
        }
        Box(Modifier.matchParentSize().background(Color(0x75000000)))
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Source(a.sourceName, np.packageName)
                Spacer(Modifier.weight(1f))
                OutputChip(a.outputName, a.openOutput)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Cover(art, 46.dp, RoundedCornerShape(12.dp))
                Gap(12.dp)
                SongText(np, Modifier.weight(1f).clickable(onClickLabel = "Open player", onClick = a.openPlayer))
                Gap(10.dp)
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xE6FFFFFF))
                        .clickable(onClickLabel = if (np.playing) "Pause" else "Play") { playPause(np) }
                        .testTag("playPause"),
                    contentAlignment = Alignment.Center,
                ) { if (np.playing) PauseGlyph(Color(0xFF111111)) else PlayGlyph(Color(0xFF111111)) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Control("Previous", { np.controller.transportControls.skipToPrevious() }) { SkipGlyph(Color.White, forward = false) }
                WaveProgress(fraction, np.playing, seek, Modifier.weight(1f).height(28.dp))
                Control("Next", { np.controller.transportControls.skipToNext() }) { SkipGlyph(Color.White, forward = true) }
                np.likeAction?.let { like ->
                    Control(if (np.liked) "Unlike" else "Like", { np.controller.transportControls.sendCustomAction(like, null) }) {
                        HeartGlyph(Color.White, filled = np.liked)
                    }
                }
            }
        }
    }
}

/** A line that ripples where the song has played; tap or drag it to jump. */
@Composable
private fun WaveProgress(fraction: Float, playing: Boolean, seek: (Float) -> Unit, modifier: Modifier) {
    val phase = if (playing) {
        rememberInfiniteTransition(label = "wave").animateFloat(
            0f, (2 * PI).toFloat(), infiniteRepeatable(tween(1_400, easing = LinearEasing), RepeatMode.Restart), label = "phase",
        ).value
    } else 0f
    var dragging by remember { mutableStateOf<Float?>(null) }
    Canvas(
        modifier
            .padding(horizontal = 6.dp)
            .seekGestures(seek) { dragging = it }
            .testTag("progress"),
    ) {
        val at = size.width * (dragging ?: fraction)
        val mid = size.height / 2
        val amp = if (playing) 3.dp.toPx() else 0f
        val wave = Path().apply {
            moveTo(0f, mid)
            var x = 0f
            while (x <= at) { lineTo(x, mid + amp * sin(x / 5.dp.toPx() - phase)); x += 2f }
        }
        drawPath(wave, Color.White, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        drawLine(Color(0x59FFFFFF), Offset(at, mid), Offset(size.width, mid), 2.5.dp.toPx(), StrokeCap.Round)
        drawCircle(Color.White, 5.dp.toPx(), Offset(at, mid))
    }
}

// ---------- 2. Dot wave: Dotline tile, dot-font title, dots for progress ----------

@Composable
private fun DotWavePlayer(np: NowPlaying, art: ImageBitmap?, position: Long, fraction: Float, seek: (Float) -> Unit, a: PlayerActions, modifier: Modifier) {
    val accent = LocalAccent.current
    Column(modifier.tile(TileShape).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Cover(art, 54.dp, RoundedCornerShape(12.dp))
            Gap(12.dp)
            Column(Modifier.weight(1f).clickable(onClickLabel = "Open player", onClick = a.openPlayer)) {
                BasicText(np.title.ifBlank { "Playing" }.uppercase(), style = Dot.dots(20), maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText(np.artist, style = Dot.label.copy(color = Dot.muted, fontSize = 12.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutputChip(a.outputName, a.openOutput, compact = true)
        }
        DotProgress(fraction, seek, accent)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            BasicText(mmss(position), style = Dot.caption.copy(fontSize = 11.sp))
            Control("Previous", { np.controller.transportControls.skipToPrevious() }) { SkipGlyph(Dot.ink, forward = false) }
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(accent)
                    .clickable(onClickLabel = if (np.playing) "Pause" else "Play") { playPause(np) }
                    .testTag("playPause"),
                contentAlignment = Alignment.Center,
            ) { if (np.playing) PauseGlyph(Color.Black) else PlayGlyph(Color.Black) }
            Control("Next", { np.controller.transportControls.skipToNext() }) { SkipGlyph(Dot.ink, forward = true) }
            BasicText(mmss(np.durationMs), style = Dot.caption.copy(fontSize = 11.sp))
        }
    }
}

/** 26 dots; the played part lights up in the accent colour. Tap or drag to jump. */
@Composable
private fun DotProgress(fraction: Float, seek: (Float) -> Unit, accent: Color) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val off = Dot.SurfaceHigh
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(14.dp)
            .seekGestures(seek) { dragging = it }
            .testTag("progress"),
    ) {
        val n = 26
        val lit = ((dragging ?: fraction) * n).toInt()
        val step = size.width / n
        for (i in 0 until n) drawCircle(if (i < lit) accent else off, 3.5.dp.toPx(), Offset(step * (i + 0.5f), size.height / 2))
    }
}

// ---------- 3. Vinyl: a round cover that spins while playing ----------

@Composable
private fun VinylPlayer(np: NowPlaying, art: ImageBitmap?, fraction: Float, a: PlayerActions, modifier: Modifier) {
    val accent = LocalAccent.current
    // Keeps its angle when paused and carries on from there.
    var angle by remember { mutableFloatStateOf(0f) }
    if (np.playing) {
        val turn by rememberInfiniteTransition(label = "spin").animateFloat(
            0f, 360f, infiniteRepeatable(tween(8_000, easing = LinearEasing)), label = "angle",
        )
        angle = turn
    }
    val ring = Dot.SurfaceHigh
    Row(modifier.tile(TileShape).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(86.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(86.dp)) {
                val w = 3.dp.toPx()
                val arcSize = Size(size.width - 2 * w, size.height - 2 * w)
                drawArc(ring, 0f, 360f, false, topLeft = Offset(w, w), size = arcSize, style = Stroke(w))
                drawArc(accent, -90f, 360f * fraction, false, topLeft = Offset(w, w), size = arcSize, style = Stroke(w, cap = StrokeCap.Round))
            }
            Box(Modifier.graphicsLayer { rotationZ = angle }) { Cover(art, 72.dp, CircleShape) }
            Box(Modifier.size(10.dp).background(Dot.Black, CircleShape).border(2.dp, ring, CircleShape))
        }
        Gap(14.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.clickable(onClickLabel = "Open player", onClick = a.openPlayer)) {
                BasicText(np.title.ifBlank { "Playing" }, style = Dot.label.copy(fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText("${np.artist} · ${a.sourceName}", style = Dot.label.copy(color = Dot.muted, fontSize = 12.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Control("Previous", { np.controller.transportControls.skipToPrevious() }) { SkipGlyph(Dot.ink, forward = false) }
                Control(if (np.playing) "Pause" else "Play", { playPause(np) }, Modifier.testTag("playPause")) {
                    if (np.playing) PauseGlyph(Dot.ink) else PlayGlyph(Dot.ink)
                }
                Control("Next", { np.controller.transportControls.skipToNext() }) { SkipGlyph(Dot.ink, forward = true) }
                Spacer(Modifier.weight(1f))
                OutputChip(a.outputName, a.openOutput, compact = true)
            }
        }
    }
}

// ---------- shared pieces ----------

/** Tap anywhere on a progress bar to jump there, or drag and let go. */
private fun Modifier.seekGestures(seek: (Float) -> Unit, onDrag: (Float?) -> Unit): Modifier =
    pointerInput(Unit) { detectTapGestures { seek((it.x / size.width).coerceIn(0f, 1f)) } }
        .pointerInput(Unit) {
            var last = 0f
            detectHorizontalDragGestures(
                onDragStart = { last = (it.x / size.width).coerceIn(0f, 1f); onDrag(last) },
                onDragEnd = { seek(last); onDrag(null) },
                onDragCancel = { onDrag(null) },
            ) { change, _ -> last = (change.position.x / size.width).coerceIn(0f, 1f); onDrag(last) }
        }

private fun playPause(np: NowPlaying) =
    if (np.playing) np.controller.transportControls.pause() else np.controller.transportControls.play()

private fun mmss(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

@Composable
private fun Cover(art: ImageBitmap?, size: Dp, shape: Shape) {
    Box(Modifier.size(size).clip(shape).background(Dot.SurfaceHigh)) {
        if (art != null) Image(art, null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
    }
}

@Composable
private fun SongText(np: NowPlaying, modifier: Modifier) {
    Column(modifier) {
        BasicText(np.title.ifBlank { "Playing" }, style = Dot.label.copy(color = Color.White, fontSize = 16.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (np.artist.isNotBlank()) {
            BasicText(np.artist, style = Dot.label.copy(color = Color(0xFFD6D6D6), fontSize = 12.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Source(name: String, packageName: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).background(if (packageName == "com.spotify.music") SpotifyGreen else Color.White, CircleShape))
        Gap(6.dp)
        BasicText(name, style = Dot.label.copy(color = Color.White, fontSize = 11.sp))
    }
}

/** Where it's playing; tap to open Android's speaker picker. */
@Composable
private fun OutputChip(name: String, onClick: () -> Unit, compact: Boolean = false) {
    Row(
        Modifier
            .widthIn(max = if (compact) 120.dp else 180.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0x29FFFFFF))
            .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(50))
            .clickable(onClickLabel = "Choose speaker", onClick = onClick)
            .testTag("outputChip")
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpeakerGlyph(Color.White)
        Gap(5.dp)
        BasicText(name, style = Dot.label.copy(color = Color.White, fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Control(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, glyph: @Composable () -> Unit) {
    Box(
        modifier.size(40.dp).clip(CircleShape).clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { glyph() }
}

// Drawn, not font glyphs, so they look the same on every phone.
@Composable
private fun PlayGlyph(c: Color) = Canvas(Modifier.size(16.dp)) {
    drawPath(Path().apply { moveTo(size.width * 0.15f, 0f); lineTo(size.width, size.height / 2); lineTo(size.width * 0.15f, size.height); close() }, c)
}

@Composable
private fun PauseGlyph(c: Color) = Canvas(Modifier.size(16.dp)) {
    val w = size.width * 0.3f
    drawRoundRect(c, Offset(size.width * 0.1f, 0f), Size(w, size.height), CornerRadius(2f))
    drawRoundRect(c, Offset(size.width * 0.6f, 0f), Size(w, size.height), CornerRadius(2f))
}

@Composable
private fun SkipGlyph(c: Color, forward: Boolean) = Canvas(Modifier.size(14.dp)) {
    val w = size.width; val h = size.height
    val p = Path().apply {
        if (forward) { moveTo(0f, 0f); lineTo(w * 0.8f, h / 2); lineTo(0f, h); close() }
        else { moveTo(w, 0f); lineTo(w * 0.2f, h / 2); lineTo(w, h); close() }
    }
    drawPath(p, c)
    drawRect(c, Offset(if (forward) w * 0.8f else 0f, 0f), Size(w * 0.2f, h))
}

@Composable
private fun HeartGlyph(c: Color, filled: Boolean) = Canvas(Modifier.size(17.dp)) {
    val w = size.width; val h = size.height
    val p = Path().apply {
        moveTo(w / 2, h * 0.92f)
        cubicTo(w * 0.05f, h * 0.6f, -w * 0.05f, h * 0.15f, w * 0.28f, h * 0.1f)
        cubicTo(w * 0.42f, h * 0.08f, w * 0.5f, h * 0.22f, w / 2, h * 0.28f)
        cubicTo(w * 0.5f, h * 0.22f, w * 0.58f, h * 0.08f, w * 0.72f, h * 0.1f)
        cubicTo(w * 1.05f, h * 0.15f, w * 0.95f, h * 0.6f, w / 2, h * 0.92f)
        close()
    }
    if (filled) drawPath(p, c) else drawPath(p, c, style = Stroke(1.8.dp.toPx()))
}

@Composable
private fun SpeakerGlyph(c: Color) = Canvas(Modifier.size(12.dp)) {
    val w = size.width; val h = size.height
    drawPath(Path().apply { moveTo(0f, h * 0.35f); lineTo(w * 0.3f, h * 0.35f); lineTo(w * 0.6f, h * 0.1f); lineTo(w * 0.6f, h * 0.9f); lineTo(w * 0.3f, h * 0.65f); lineTo(0f, h * 0.65f); close() }, c)
    drawArc(c, -50f, 100f, false, topLeft = Offset(w * 0.45f, h * 0.2f), size = Size(w * 0.5f, h * 0.6f), style = Stroke(1.4.dp.toPx()))
}
