package dev.rahmanfaizur.dotline.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawOutline
import dev.rahmanfaizur.dotline.ui.theme.LocalDarkText
import dev.rahmanfaizur.dotline.ui.theme.LocalTextHelp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.rahmanfaizur.dotline.data.TextTone
import dev.rahmanfaizur.dotline.data.TileStyle
import dev.rahmanfaizur.dotline.ui.theme.Dot
import kotlin.math.max
import kotlin.math.roundToInt

/** The wallpaper behind home, its small blurred copy used by glass tiles, and its brightness (0..1). */
class Backdrop(val full: ImageBitmap, val frosted: ImageBitmap, val brightness: Float = 0f) {
    /** Brightness of each pixel of the small blurred copy, read once, so tiles can check what's behind them. */
    private val lum: FloatArray by lazy {
        val w = frosted.width
        val h = frosted.height
        val px = IntArray(w * h)
        frosted.asAndroidBitmap().getPixels(px, 0, w, 0, 0, w, h)
        FloatArray(px.size) { i ->
            val c = px[i]
            (0.2126f * ((c shr 16) and 0xFF) + 0.7152f * ((c shr 8) and 0xFF) + 0.0722f * (c and 0xFF)) / 255f
        }
    }

    /**
     * Brightness (0..1) of the wallpaper behind [rect] (in screen px of an area of [areaSize]),
     * using the same centre-crop as the wallpaper on screen. Leans towards the brightest parts,
     * since that's where text gets lost.
     */
    fun brightnessBehind(areaSize: IntSize, rect: androidx.compose.ui.geometry.Rect): Float {
        val w = frosted.width
        val h = frosted.height
        val s = max(areaSize.width.toFloat() / full.width, areaSize.height.toFloat() / full.height)
        val ox = (areaSize.width - full.width * s) / 2f
        val oy = (areaSize.height - full.height * s) / 2f
        val k = w.toFloat() / full.width
        fun gx(x: Float) = (((x - ox) / s) * k).roundToInt().coerceIn(0, w - 1)
        fun gy(y: Float) = (((y - oy) / s) * k).roundToInt().coerceIn(0, h - 1)
        val x0 = gx(rect.left); val x1 = gx(rect.right)
        val y0 = gy(rect.top); val y1 = gy(rect.bottom)
        var sum = 0f; var n = 0; var top = 0f
        for (y in y0..y1) for (x in x0..x1) {
            val v = lum[y * w + x]
            sum += v; n++
            if (v > top) top = v
        }
        if (n == 0) return brightness
        return 0.6f * (sum / n) + 0.4f * top
    }
}

/** Text colour chosen in Settings → Look → Text (tiles then decide white or black one by one in Auto). */
val LocalTextTone = staticCompositionLocalOf { TextTone.Auto }

/**
 * One tile's text: white or black, plus how much help it needs to stand out ([help], 0..1),
 * worked out from the wallpaper right behind that tile.
 */
class TileTone(dark: Boolean) {
    var dark by mutableStateOf(dark)
    var help by mutableStateOf(0f)

    fun update(behind: Float, style: TileStyle, tone: TextTone) {
        // How bright the tile really looks once its own glass or tint is drawn over the wallpaper.
        val bg = when (style) {
            TileStyle.Solid -> 0.08f
            TileStyle.Tinted -> behind * 0.45f
            TileStyle.Smoke -> behind * 0.6f
            TileStyle.Glass -> behind * 0.69f + 0.12f
            TileStyle.Outline, TileStyle.Floating -> behind
        }
        val d = when (tone) {
            TextTone.White -> false
            TextTone.Black -> true
            TextTone.Auto -> bg > 0.58f
        }
        dark = d
        help = (if (d) (0.62f - bg) / 0.4f else (bg - 0.42f) / 0.4f).coerceIn(0f, 1f)
    }
}

@Composable
fun rememberTileTone(): TileTone {
    val start = LocalDarkText.current
    return remember { TileTone(start) }
}

/** Gives the tile's content its own text colour and outline strength. */
@Composable
fun ProvideTileTone(tone: TileTone, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDarkText provides tone.dark, LocalTextHelp provides tone.help, content = content)
}

/**
 * Whether home tiles should use black text. Auto only does it where the wallpaper itself
 * shows through bright (clear glass, outline, floating); dark tiles always keep white.
 */
fun useDarkText(tone: TextTone, style: TileStyle, backdrop: Backdrop?): Boolean = when (tone) {
    TextTone.White -> false
    TextTone.Black -> true
    TextTone.Auto -> backdrop != null && when (style) {
        // Floating and outline tiles show the picture as it is, so only really bright ones need black text.
        TileStyle.Outline, TileStyle.Floating -> backdrop.brightness > 0.6f
        TileStyle.Glass -> backdrop.brightness > 0.7f
        else -> false
    }
}

/** Where the wallpaper is drawn, so a tile can work out which part of it sits behind the tile. */
class BackdropArea {
    var coords: LayoutCoordinates? by mutableStateOf(null)
}

class TileLook(val style: TileStyle, val backdrop: Backdrop?, val area: BackdropArea?) {
    /** On plain black, see-through tiles would disappear, so they draw solid instead. */
    val effective: TileStyle
        get() = if (backdrop == null && style in SEE_THROUGH) TileStyle.Solid else style

    private companion object {
        val SEE_THROUGH = setOf(TileStyle.Smoke, TileStyle.Glass, TileStyle.Tinted)
    }
}

val LocalTileLook = staticCompositionLocalOf { TileLook(TileStyle.Solid, null, null) }

/**
 * Draws the wallpaper (or black) and lets every [tile] inside choose its look from it.
 * Used for the real home screen and for the small preview in Settings.
 */
@Composable
fun WallpaperArea(
    backdrop: Backdrop?,
    style: TileStyle,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val area = remember { BackdropArea() }
    val look = remember(style, backdrop) { TileLook(style, backdrop, area) }
    Box(modifier.background(Dot.Black).onGloballyPositioned { area.coords = it }) {
        if (backdrop != null) {
            Image(backdrop.full, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        CompositionLocalProvider(LocalTileLook provides look) { content() }
    }
}

private val SmokeTint = Color(0x66000000)
// Clear glass = a little dark under a little white, so white text stays readable on bright pictures.
private val GlassShade = Color(0x38000000)
private val GlassTint = Color(0x1FFFFFFF)
private val TintedFill = Color(0x8C000000)

/**
 * The background for any tile, pill or dock, in the tile style chosen in Settings.
 * With a [tone], the tile also checks the wallpaper behind it and, where text would be hard to read,
 * adds a light shade (dark under white text, light under black text).
 */
fun Modifier.tile(shape: Shape = TileShape, tone: TileTone? = null): Modifier = composed {
    val look = LocalTileLook.current
    val setting = LocalTextTone.current
    val readable = if (tone == null) Modifier else {
        val backdrop = look.backdrop
        val area = look.area
        // Re-check only when the tile really moved (not every frame of a swipe) or the look changed.
        val last = remember(backdrop, look.effective, setting) { floatArrayOf(Float.NaN, Float.NaN, Float.NaN, Float.NaN) }
        Modifier
            .onGloballyPositioned { c ->
                val a = area?.coords
                if (backdrop == null || a == null || !a.isAttached || !c.isAttached) {
                    if (!last[0].isNaN() || last[1].isNaN()) tone.update(0f, look.effective, setting)
                    last[0] = Float.NaN; last[1] = 0f
                } else {
                    val topLeft = a.localPositionOf(c, Offset.Zero)
                    val w = c.size.width.toFloat()
                    val h = c.size.height.toFloat()
                    val moved = last[0].isNaN() || kotlin.math.abs(last[0] - topLeft.x) > 24f || kotlin.math.abs(last[1] - topLeft.y) > 24f ||
                        last[2] != w || last[3] != h
                    if (moved) {
                        last[0] = topLeft.x; last[1] = topLeft.y; last[2] = w; last[3] = h
                        val r = androidx.compose.ui.geometry.Rect(topLeft, androidx.compose.ui.geometry.Size(w, h))
                        tone.update(backdrop.brightnessBehind(a.size, r), look.effective, setting)
                    }
                }
            }
            .then(
                if (look.effective == TileStyle.Floating || look.effective == TileStyle.Solid) Modifier
                else Modifier.drawBehind {
                    val h = tone.help
                    if (h > 0.02f) {
                        val shade = if (tone.dark) Color.White.copy(alpha = 0.45f * h) else Color.Black.copy(alpha = 0.4f * h)
                        drawOutline(shape.createOutline(size, layoutDirection, this), shade)
                    }
                },
            )
    }
    when (look.effective) {
        TileStyle.Solid -> background(Dot.Surface, shape)
        TileStyle.Tinted -> background(TintedFill, shape).border(1.dp, Color(0x12FFFFFF), shape)
        TileStyle.Outline -> border(1.dp, Color(0x3DFFFFFF), shape)
        TileStyle.Floating -> this
        TileStyle.Smoke -> frosted(look, shape, SmokeTint).border(1.dp, Color(0x14FFFFFF), shape)
        TileStyle.Glass -> frosted(look, shape, GlassTint).border(1.dp, Color(0x24FFFFFF), shape)
    }.then(readable)
}

/**
 * Frosted glass: draws the matching piece of the pre-blurred wallpaper behind the tile.
 * Only the tile's position is tracked while scrolling; no blurring happens per frame.
 */
private fun Modifier.frosted(look: TileLook, shape: Shape, tint: Color): Modifier = composed {
    val backdrop = look.backdrop!!
    val area = look.area!!
    var pos by remember { mutableStateOf(Offset.Unspecified) }
    onGloballyPositioned { c ->
        val a = area.coords
        if (a != null && a.isAttached && c.isAttached) pos = a.localPositionOf(c, Offset.Zero)
    }.drawWithCache {
        val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
        val dst = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1))
        onDrawBehind {
            val p = pos
            val areaSize = area.coords?.size
            if (p != Offset.Unspecified && areaSize != null && areaSize.width > 0) {
                val full = backdrop.full
                val img = backdrop.frosted
                // Same centre-crop as the wallpaper Image: screen px per wallpaper px, and the crop offset.
                val s = max(areaSize.width.toFloat() / full.width, areaSize.height.toFloat() / full.height)
                val ox = (areaSize.width - full.width * s) / 2f
                val oy = (areaSize.height - full.height * s) / 2f
                val k = img.width.toFloat() / full.width
                val sx = (((p.x - ox) / s) * k).roundToInt().coerceIn(0, img.width - 1)
                val sy = (((p.y - oy) / s) * k).roundToInt().coerceIn(0, img.height - 1)
                val sw = ((size.width / s) * k).roundToInt().coerceIn(1, img.width - sx)
                val sh = ((size.height / s) * k).roundToInt().coerceIn(1, img.height - sy)
                clipPath(path) {
                    drawImage(img, IntOffset(sx, sy), IntSize(sw, sh), dstSize = dst, filterQuality = FilterQuality.Low)
                    if (tint == GlassTint) drawRect(GlassShade)
                    drawRect(tint)
                }
            } else {
                clipPath(path) { drawRect(SmokeTint) }
            }
        }
    }
}
