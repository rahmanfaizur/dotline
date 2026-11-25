package dev.rahmanfaizur.dotline.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rahmanfaizur.dotline.notify.NowPlaying
import dev.rahmanfaizur.dotline.ui.common.Gap
import dev.rahmanfaizur.dotline.ui.common.TileShape
import dev.rahmanfaizur.dotline.ui.common.tile
import dev.rahmanfaizur.dotline.ui.theme.Dot

/** The song that's playing, with previous / play-pause / next. Tap the text to open the player. */
@Composable
fun NowPlayingTile(np: NowPlaying, openPlayer: () -> Unit, modifier: Modifier = Modifier) {
    val art = remember(np.art) { np.art?.asImageBitmap() }
    Row(
        modifier
            .fillMaxWidth()
            .tile(TileShape)
            .testTag("nowPlaying")
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))) {
            if (art != null) Image(art, null, contentScale = ContentScale.Crop, modifier = Modifier.size(46.dp))
            else Box(Modifier.size(46.dp).tile(RoundedCornerShape(12.dp)))
        }
        Gap(12.dp)
        Column(Modifier.weight(1f).clickable(onClickLabel = "Open player", onClick = openPlayer)) {
            BasicText(np.title.ifBlank { "Playing" }, style = Dot.label.copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (np.artist.isNotBlank()) {
                BasicText(np.artist, style = Dot.label.copy(color = Dot.muted, fontSize = 12.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        val ink = Dot.ink
        Control("Previous", { np.controller.transportControls.skipToPrevious() }) { Skip(ink, forward = false) }
        Control(if (np.playing) "Pause" else "Play", {
            if (np.playing) np.controller.transportControls.pause() else np.controller.transportControls.play()
        }) { if (np.playing) PauseGlyph(ink) else PlayGlyph(ink) }
        Control("Next", { np.controller.transportControls.skipToNext() }) { Skip(ink, forward = true) }
    }
}

@Composable
private fun Control(label: String, onClick: () -> Unit, glyph: @Composable () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(50)).clickable(onClickLabel = label, onClick = onClick),
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
    drawRoundRect(c, Offset(size.width * 0.1f, 0f), androidx.compose.ui.geometry.Size(w, size.height), androidx.compose.ui.geometry.CornerRadius(2f))
    drawRoundRect(c, Offset(size.width * 0.6f, 0f), androidx.compose.ui.geometry.Size(w, size.height), androidx.compose.ui.geometry.CornerRadius(2f))
}

@Composable
private fun Skip(c: Color, forward: Boolean) = Canvas(Modifier.size(14.dp)) {
    val w = size.width; val h = size.height
    val p = Path().apply {
        if (forward) { moveTo(0f, 0f); lineTo(w * 0.8f, h / 2); lineTo(0f, h); close() }
        else { moveTo(w, 0f); lineTo(w * 0.2f, h / 2); lineTo(w, h); close() }
    }
    drawPath(p, c)
    drawRect(c, Offset(if (forward) w * 0.8f else 0f, 0f), androidx.compose.ui.geometry.Size(w * 0.2f, h))
}
