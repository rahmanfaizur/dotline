package dev.rahmanfaizur.dotline.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryClockTest {
    private val min = 60_000L

    @Test fun tooEarlyGivesNothing() {
        val c = BatteryClock()
        c.record(0, 80, false)
        c.record(5 * min, 79, false)
        assertNull(c.fromRate(5 * min, 79))
    }

    @Test fun drainRateGivesTimeLeft() {
        val c = BatteryClock()
        c.record(0, 80, false)
        c.record(20 * min, 78, false) // 10 min per percent
        assertEquals(78 * 10 * min, c.fromRate(20 * min, 78))
    }

    @Test fun chargingCountsUpToFull() {
        val c = BatteryClock()
        c.record(0, 50, true)
        c.record(10 * min, 55, true) // 2 min per percent
        assertEquals(45 * 2 * min, c.fromRate(10 * min, 55))
    }

    @Test fun plugChangeStartsOver() {
        val c = BatteryClock()
        c.record(0, 80, false)
        c.record(30 * min, 70, false)
        c.record(31 * min, 70, true)
        assertNull(c.fromRate(31 * min, 70))
    }

    @Test fun levelGoingUpWhileUnpluggedStartsOver() {
        val c = BatteryClock()
        c.record(0, 60, false)
        c.record(60 * min, 95, false) // charged while we weren't looking
        assertNull(c.fromRate(60 * min, 95))
    }

    @Test fun currentGivesTimeLeft() {
        val c = BatteryClock()
        // 3000 mAh left at 500 mA = 6 hours.
        assertEquals(6 * 60 * min, c.fromCurrent(3_000_000, -500_000))
    }

    @Test fun currentInWrongUnitsIsDropped() {
        val c = BatteryClock()
        assertNull(c.fromCurrent(3_000_000, -500)) // mA reported as µA: 6000 hours
        assertNull(c.fromCurrent(0, -500_000))
    }

    @Test fun formatsHoursAndMinutes() {
        assertEquals("6:20", BatteryClock.format(6 * 60 * min + 20 * min))
        assertEquals("0:05", BatteryClock.format(5 * min))
        assertEquals("99+", BatteryClock.format(120 * 60 * min))
    }
}
