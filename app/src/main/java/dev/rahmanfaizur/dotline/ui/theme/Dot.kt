package dev.rahmanfaizur.dotline.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.rahmanfaizur.dotline.R

/** Dotline's whole visual language: black, white, one red. */
object Dot {
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF151515)
    val SurfaceHigh = Color(0xFF1F1F1F)
    val Ink = Color(0xFFF2F2F2)
    val Muted = Color(0xFF8F8F8F)
    val Faint = Color(0xFF5E5E5E)
    val Red = Color(0xFFE8453C)

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
