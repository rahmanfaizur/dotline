package dev.rahmanfaizur.dotline.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class MonthGridTest {

    private val nov2026 = YearMonth.of(2026, 11)
    private val today = LocalDate.of(2026, 11, 25)

    @Test fun `November 2026 starts on a Sunday, so six blanks when weeks start Monday`() {
        val g = MonthGrid.of(nov2026, today, DayOfWeek.MONDAY)
        assertEquals(6, g.blanks)
        assertEquals(30, g.days)
        assertEquals(25, g.today)
    }

    @Test fun `no blanks when weeks start on Sunday`() {
        assertEquals(0, MonthGrid.of(nov2026, today, DayOfWeek.SUNDAY).blanks)
    }

    @Test fun `today is null for a different month`() {
        assertNull(MonthGrid.of(YearMonth.of(2026, 12), today, DayOfWeek.MONDAY).today)
    }

    @Test fun `leap February has 29 days`() {
        assertEquals(29, MonthGrid.of(YearMonth.of(2028, 2), today, DayOfWeek.MONDAY).days)
    }

    @Test fun `weekday initials follow the week start`() {
        assertEquals("MTWTFSS", MonthGrid.of(nov2026, today, DayOfWeek.MONDAY).weekdayInitials.joinToString(""))
        assertEquals("SMTWTFS", MonthGrid.of(nov2026, today, DayOfWeek.SUNDAY).weekdayInitials.joinToString(""))
    }

    @Test fun `sizes read like the Storage screen`() {
        assertEquals("40 GB", Sizes.short(40_400_000_000))
        assertEquals("3.5 GB", Sizes.short(3_500_000_000))
        assertEquals("512 MB", Sizes.short(512_000_000))
    }

    @Test fun `percent used`() {
        assertEquals(64, Sizes.percentUsed(free = 39_000_000_000, total = 109_000_000_000))
        assertEquals(0, Sizes.percentUsed(free = 0, total = 0))
    }
}
