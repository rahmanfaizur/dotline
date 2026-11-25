package dev.rahmanfaizur.dotline.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import dev.rahmanfaizur.dotline.data.AppEntry
import dev.rahmanfaizur.dotline.data.IconLoader
import dev.rahmanfaizur.dotline.ui.theme.Dot

val LocalIconLoader = staticCompositionLocalOf<IconLoader> { error("IconLoader not provided") }

/** The app's real icon, clipped to [shape] (round by default). */
@Composable
fun AppIcon(app: AppEntry, size: Dp, modifier: Modifier = Modifier, shape: Shape = CircleShape) {
    val loader = LocalIconLoader.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val bitmap by produceState(loader.peek(app, px), app.key, px) {
        value = loader.load(app, px)
    }
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(Dot.SurfaceHigh),
    ) {
        bitmap?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}
