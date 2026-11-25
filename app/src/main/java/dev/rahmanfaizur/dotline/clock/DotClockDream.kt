package dev.rahmanfaizur.dotline.clock

import android.service.dreams.DreamService
import android.text.format.DateFormat
import dev.rahmanfaizur.dotline.DotlineApp
import dev.rahmanfaizur.dotline.data.ClockMode
import dev.rahmanfaizur.dotline.ui.theme.Dot
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Android's screensaver ("Daydream"): the dot clock while the phone charges or sits docked. */
class DotClockDream : DreamService() {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false // any touch ends it, like every screensaver
        isFullscreen = true
        isScreenBright = false
        val view = DotClockView(this)
        setContentView(view)
        val app = application as DotlineApp
        app.appScope.launch {
            val s = app.settings.settings.first()
            view.accent = Dot.accent(s.accent).toArgb()
            view.use24h = when (s.clock) {
                ClockMode.H24 -> true
                ClockMode.H12 -> false
                ClockMode.System -> DateFormat.is24HourFormat(this@DotClockDream)
            }
        }
    }
}
