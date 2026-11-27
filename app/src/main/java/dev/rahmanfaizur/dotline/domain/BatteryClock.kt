package dev.rahmanfaizur.dotline.domain

import kotlin.math.abs

/**
 * Guesses how long the battery will last, or how long until it's full, from how fast it moves.
 *
 * Two sources, best first:
 * 1. The real rate: how many minutes each percent took since this run (unplugged or charging) began.
 *    Needs a 2% move over at least 10 minutes to be trusted.
 * 2. Until then, the battery's remaining charge divided by the current it draws right now
 *    (smoothed, since that number jumps around).
 *
 * Times are elapsedRealtime milliseconds. Nothing is stored: a new run starts when the app restarts.
 */
class BatteryClock {
    private var startMs = 0L
    private var startPercent = -1
    private var charging: Boolean? = null
    private var smoothCurrent = 0.0

    /** Notes the battery level now. Plugging in or unplugging (or a level going the "wrong" way) starts a new run. */
    fun record(nowMs: Long, percent: Int, isCharging: Boolean) {
        val wrongWay = startPercent >= 0 && (if (isCharging) percent < startPercent else percent > startPercent)
        if (charging != isCharging || wrongWay || startPercent < 0) {
            charging = isCharging
            startMs = nowMs
            startPercent = percent
            smoothCurrent = 0.0
        }
    }

    /** Time left from the measured rate (until empty, or until full while charging), or null if it's too early to say. */
    fun fromRate(nowMs: Long, percent: Int): Long? {
        val isCharging = charging ?: return null
        val moved = if (isCharging) percent - startPercent else startPercent - percent
        val elapsed = nowMs - startMs
        if (moved < MIN_MOVE || elapsed < MIN_TIME_MS) return null
        val perPercent = elapsed.toDouble() / moved
        val left = if (isCharging) 100 - percent else percent
        return (left * perPercent).toLong().takeIf { it in 0..MAX_MS }
    }

    /**
     * Time until empty from the charge left (µAh) and the current drawn (µA, either sign).
     * Phones that report other units give silly answers; those are dropped (null).
     */
    fun fromCurrent(chargeLeftMicroAh: Long, currentMicroA: Long): Long? {
        if (chargeLeftMicroAh <= 0 || currentMicroA == 0L || currentMicroA == Long.MIN_VALUE) return null
        val draw = abs(currentMicroA).toDouble()
        smoothCurrent = if (smoothCurrent == 0.0) draw else smoothCurrent * 0.7 + draw * 0.3
        val ms = (chargeLeftMicroAh / smoothCurrent * 3_600_000.0).toLong()
        return ms.takeIf { it in MIN_SANE_MS..MAX_MS }
    }

    companion object {
        const val MIN_MOVE = 2
        const val MIN_TIME_MS = 10 * 60_000L
        private const val MIN_SANE_MS = 5 * 60_000L
        private const val MAX_MS = 7 * 24 * 3_600_000L

        /** "6:20" (hours:minutes), or "99+" for absurdly long. */
        fun format(ms: Long): String {
            val minutes = (ms + 30_000) / 60_000
            val h = minutes / 60
            return if (h > 99) "99+" else "%d:%02d".format(h, minutes % 60)
        }
    }
}
