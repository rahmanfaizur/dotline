package dev.rahmanfaizur.dotline.clock

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import dev.rahmanfaizur.dotline.DotlineApp
import dev.rahmanfaizur.dotline.data.ClockMode
import dev.rahmanfaizur.dotline.ui.theme.Dot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The dot clock, full screen. It may show over the lock screen, the same way an alarm does:
 * it never unlocks anything, and closing it lands back on the normal lock screen.
 * The screen only stays on while the phone is charging.
 */
class NightClockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        val view = DotClockView(this).apply { setOnClickListener { finish() } }
        setContentView(view)
        val app = application as DotlineApp
        lifecycleScope.launch {
            val s = app.settings.settings.first()
            view.accent = Dot.accent(s.accent).toArgb()
            view.use24h = when (s.clock) {
                ClockMode.H24 -> true
                ClockMode.H12 -> false
                ClockMode.System -> DateFormat.is24HourFormat(this@NightClockActivity)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val battery = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = (battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
        if (plugged) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
