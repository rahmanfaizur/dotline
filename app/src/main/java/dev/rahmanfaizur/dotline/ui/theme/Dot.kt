package dev.rahmanfaizur.dotline.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.rahmanfaizur.dotline.R
import dev.rahmanfaizur.dotline.data.IconShape

/** Dotline's whole visual language: black, white, one accent colour (red by default). */
object Dot {
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF151515)
    val SurfaceHigh = Color(0xFF262626)
    val Ink = Color(0xFFF2F2F2)
    val Muted = Color(0xFF8F8F8F)
    val Faint = Color(0xFF5E5E5E)
    val Red = Color(0xFFE8453C)

    /** Accent choices in Settings; index 0 is the default red. */
    val Accents = listOf(
        Red, Color(0xFFF2F2F2), Color(0xFFF0B44C), Color(0xFF7FCF9A), Color(0xFF6FA8FF),
        Color(0xFFFF6FAE), Color(0xFFA78BFA),
    )
    val AccentNames = listOf("Red", "White", "Amber", "Green", "Blue", "Pink", "Violet")

    fun accent(index: Int) = Accents.getOrElse(index) { Red }

    fun iconShape(shape: IconShape): Shape = when (shape) {
        IconShape.Round -> CircleShape
        IconShape.Square -> RoundedCornerShape(28)
    }

    /** Doto with round dots (ROND = 100) at its heaviest weight. */
    @OptIn(ExperimentalTextApi::class)
    val DotFont = FontFamily(
        Font(
            resId = R.font.doto,
            weight = FontWeight.Black,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(900),
                FontVariation.Setting("ROND", 100f),
            ),
        ),
    )

    val Mono = FontFamily.Monospace

    /** True while a wallpaper is showing: text then gets a soft shadow to stay readable on bright pictures. */
    var onWallpaper by mutableStateOf(false)

    private val TextShadow = Shadow(Color(0x99000000), Offset(0f, 2f), blurRadius = 6f)
    private val InkDark = Color(0xFF101010)

    /**
     * Text colours that follow Settings → Look → Text (white, black or auto). The softer ones are the
     * same colour, see-through, so labels read as white or black on any wallpaper, never grey.
     */
    val ink: Color @Composable @ReadOnlyComposable get() = if (LocalDarkText.current) InkDark else Ink
    val muted: Color @Composable @ReadOnlyComposable get() = ink.copy(alpha = 0.72f)
    val faint: Color @Composable @ReadOnlyComposable get() = ink.copy(alpha = 0.5f)
    /**
     * Battery colour: red at 10% or less, orange at 20% or less, green while charging, otherwise
     * the normal text colour. Deeper shades when the text is black so they stay readable.
     */
    @Composable @ReadOnlyComposable
    fun battery(percent: Int?, charging: Boolean): Color {
        val dark = LocalDarkText.current
        return when {
            charging -> if (dark) Color(0xFF1B7F37) else Color(0xFF6EE08F)
            percent != null && percent <= 10 -> if (dark) Color(0xFFC0261D) else Color(0xFFFF5A4F)
            percent != null && percent <= 20 -> if (dark) Color(0xFFC25A00) else Color(0xFFFFA040)
            else -> ink
        }
    }

    /** Unlit dots (battery meter, days still to come). */
    val off: Color @Composable @ReadOnlyComposable get() = ink.copy(alpha = 0.22f)

    /**
     * On a wallpaper, white text gets a dark shadow and black text a light glow, stronger where the
     * picture behind fights the text (see [LocalTextHelp]).
     */
    private val shadow: Shadow? @Composable @ReadOnlyComposable
        get() {
            if (!onWallpaper) return null
            val help = LocalTextHelp.current
            return if (LocalDarkText.current) {
                if (help < 0.05f) null else Shadow(Color.White.copy(alpha = 0.55f + 0.4f * help), Offset.Zero, blurRadius = 6f + 8f * help)
            } else {
                if (help < 0.05f) TextShadow else Shadow(Color.Black.copy(alpha = 0.6f + 0.35f * help), Offset(0f, 1f), blurRadius = 6f + 8f * help)
            }
        }

    val label: TextStyle @Composable @ReadOnlyComposable
        get() = TextStyle(color = ink, fontSize = 12.sp, fontWeight = FontWeight.Medium, shadow = shadow)
    val caption: TextStyle @Composable @ReadOnlyComposable
        get() = TextStyle(color = muted, fontFamily = Mono, fontSize = 11.sp, letterSpacing = 1.sp, shadow = shadow)

    @Composable @ReadOnlyComposable
    fun dots(size: Int, color: Color = ink) =
        TextStyle(color = color, fontFamily = DotFont, fontWeight = FontWeight.Black, fontSize = size.sp, shadow = shadow)
}

/** True on home tiles when they should use black text (see Settings → Look → Text). */
val LocalDarkText = staticCompositionLocalOf { false }

/** How much a tile's text needs help to stand out from the wallpaper behind it (0 = none, 1 = a lot). */
val LocalTextHelp = staticCompositionLocalOf { 0f }

/** Accent colour chosen in Settings. */
val LocalAccent = staticCompositionLocalOf { Dot.Red }

/** Icon shape chosen in Settings (round by default). */
val LocalIconShape = staticCompositionLocalOf<Shape> { CircleShape }
