package dev.rahmanfaizur.dotline.domain

import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Turns a picture into dots, like a dot-matrix print: the picture is cut into square
 * cells and each cell becomes one dot whose area matches how bright the cell is.
 */
object Halftone {
    /** Columns of dots across the screen; about 34 looks good on a phone. */
    const val COLUMNS = 34

    /** Dimmest cells still get a pin-prick dot so the grid stays visible. */
    private const val MIN_LUMA = 0.06f

    class Grid(val cell: Float, val columns: Int, val rows: Int)

    fun grid(width: Int, height: Int, columns: Int = COLUMNS): Grid {
        val cell = width.toFloat() / columns
        return Grid(cell, columns, ceil(height / cell).toInt())
    }

    /** Perceived brightness of an ARGB colour, 0 (black) to 1 (white). */
    fun luma(argb: Int): Float {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f
    }

    /** Dot radius for a cell: area grows with brightness, and a full-white cell's dot just fills it. */
    fun radius(luma: Float, cell: Float): Float = cell * 0.5f * sqrt(luma.coerceIn(MIN_LUMA, 1f))

    /** The brightest spots (a sun, a lamp) are drawn in the accent colour. */
    fun isHighlight(luma: Float, brightest: Float): Boolean = luma > 0.75f && luma >= brightest * 0.92f
}
