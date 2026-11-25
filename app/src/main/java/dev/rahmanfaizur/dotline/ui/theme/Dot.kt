package dev.rahmanfaizur.dotline.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.staticCompositionLocalOf
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
    val Accents = listOf(Red, Color(0xFFF2F2F2), Color(0xFFF0B44C), Color(0xFF7FCF9A), Color(0xFF6FA8FF))
    val AccentNames = listOf("Red", "White", "Amber", "Green", "Blue")

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

    val label = TextStyle(color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val caption = TextStyle(color = Muted, fontFamily = Mono, fontSize = 11.sp, letterSpacing = 1.sp)
    fun dots(size: Int, color: Color = Ink) =
        TextStyle(color = color, fontFamily = DotFont, fontWeight = FontWeight.Black, fontSize = size.sp)
}

/** Accent colour chosen in Settings. */
val LocalAccent = staticCompositionLocalOf { Dot.Red }

/** Icon shape chosen in Settings (round by default). */
val LocalIconShape = staticCompositionLocalOf<Shape> { CircleShape }
