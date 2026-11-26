package dev.rahmanfaizur.dotline.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import dev.rahmanfaizur.dotline.ui.theme.LocalAccent
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import dev.rahmanfaizur.dotline.data.AppEntry
import dev.rahmanfaizur.dotline.data.IconLoader
import dev.rahmanfaizur.dotline.ui.theme.Dot
import dev.rahmanfaizur.dotline.ui.theme.LocalIconShape

val LocalIconLoader = staticCompositionLocalOf<IconLoader> { error("IconLoader not provided") }

/** Colour for dot-style icons (ARGB), or 0 for the apps' own icons. Settings → Look → Icons. */
val LocalIconTint = staticCompositionLocalOf { 0 }

/** Packages whose icons show a notification dot right now (empty unless the user turned dots on). */
val LocalDots = compositionLocalOf<Set<String>> { emptySet() }

/** The app's real icon, clipped to [shape] (round by default). */
@Composable
fun AppIcon(app: AppEntry, size: Dp, modifier: Modifier = Modifier, shape: Shape = LocalIconShape.current) {
    val loader = LocalIconLoader.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val tint = LocalIconTint.current
    val bitmap by produceState(loader.peek(app, px, tint), app.key, px, tint) {
        value = loader.load(app, px, tint)
    }
    val dot = app.component.packageName in LocalDots.current
    Box(modifier.size(size)) {
        Box(Modifier.fillMaxSize().clip(shape).background(Dot.SurfaceHigh)) {
            bitmap?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize()) }
        }
        if (dot) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(size * 0.28f)
                    .background(Dot.Black, CircleShape)
                    .padding(size * 0.04f)
                    .background(LocalAccent.current, CircleShape)
                    .testTag("dot:${app.label}"),
            )
        }
    }
}
