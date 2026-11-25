package dev.rahmanfaizur.dotline.clock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.format.DateFormat
import android.view.View
import androidx.core.content.res.ResourcesCompat
import dev.rahmanfaizur.dotline.R
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * A big round-dot clock on black, for the screensaver and Night clock. Kept dim (it's for the
 * dark) and nudged to a new spot every minute so an AMOLED screen never burns in.
 */
class DotClockView(context: Context) : View(context) {
    var accent: Int = 0xFFE8453C.toInt()
        set(value) { field = value; invalidate() }
    var use24h: Boolean = DateFormat.is24HourFormat(context)
        set(value) { field = value; invalidate() }

    private val dots: Typeface? = runCatching { ResourcesCompat.getFont(context, R.font.doto) }.getOrNull()
    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = dots
        fontVariationSettings = "'ROND' 100, 'wght' 900"
        color = 0xFFBDBDBD.toInt()
        textAlign = Paint.Align.CENTER
    }
    private val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        color = 0xFF6E6E6E.toInt()
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.12f
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var shiftX = 0f
    private var shiftY = 0f
    private var lastMinute = -1L

    private val tick = object : Runnable {
        override fun run() {
            invalidate()
            val now = System.currentTimeMillis()
            postDelayed(this, 60_000L - now % 60_000L + 50L)
        }
    }

    init {
        setBackgroundColor(Color.BLACK)
        keepScreenOn = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(tick)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val now = System.currentTimeMillis()
        val minute = now / 60_000L
        if (minute != lastMinute) {
            // A small new spot each minute, within a tenth of the screen.
            lastMinute = minute
            shiftX = (Random.nextFloat() - 0.5f) * width * 0.1f
            shiftY = (Random.nextFloat() - 0.5f) * height * 0.1f
        }
        val cx = width / 2f + shiftX
        val cy = height / 2f + shiftY
        timePaint.textSize = width * 0.24f
        datePaint.textSize = width * 0.035f
        val time = DateFormat.format(if (use24h) "HH:mm" else "h:mm", Date(now)).toString()
        val date = DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEE d MMM"), Date(now))
            .toString().uppercase(Locale.getDefault())
        canvas.drawText(time, cx, cy + timePaint.textSize * 0.35f, timePaint)
        val dateY = cy + timePaint.textSize * 0.35f + datePaint.textSize * 3f
        canvas.drawText(date, cx, dateY, datePaint)
        dotPaint.color = accent
        canvas.drawCircle(cx, dateY + datePaint.textSize * 2f, datePaint.textSize * 0.35f, dotPaint)
    }
}
