package dev.rahmanfaizur.dotline.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * The dot calendar for one month.
 * [blanks] empty cells before day 1, [days] in the month, [today] (1-based) or null if another month.
 */
data class MonthGrid(val blanks: Int, val days: Int, val today: Int?, val weekStart: DayOfWeek) {

    /** Weekday initials in display order, e.g. M T W T F S S. */
    val weekdayInitials: List<Char>
        get() = (0 until 7).map { weekStart.plus(it.toLong()).name.first() }

    companion object {
        fun of(month: YearMonth, today: LocalDate, weekStart: DayOfWeek): MonthGrid {
            val first = month.atDay(1).dayOfWeek
            val blanks = (first.value - weekStart.value + 7) % 7
            return MonthGrid(
                blanks = blanks,
                days = month.lengthOfMonth(),
                today = if (YearMonth.from(today) == month) today.dayOfMonth else null,
                weekStart = weekStart,
            )
        }
    }
}

/** "40 GB" style sizes, using 1000-based units like Android's Storage screen. */
object Sizes {
    fun short(bytes: Long): String {
        val gb = bytes / 1_000_000_000.0
        return when {
            gb >= 10 -> "${gb.toLong()} GB"
            gb >= 1 -> "%.1f GB".format(gb)
            else -> "${(bytes / 1_000_000).coerceAtLeast(0)} MB"
        }
    }

    fun percentUsed(free: Long, total: Long): Int =
        if (total <= 0) 0 else (((total - free) * 100.0) / total).toInt().coerceIn(0, 100)
}
