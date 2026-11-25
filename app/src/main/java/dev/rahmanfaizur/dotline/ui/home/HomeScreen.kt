package dev.rahmanfaizur.dotline.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rahmanfaizur.dotline.ui.theme.Dot
import kotlinx.coroutines.delay
import java.util.Date
import java.util.Locale

/**
 * Temporary home for chunk 1: dot clock + date, swipe up or tap search to open all apps.
 * Chunk 2 replaces the middle with the Bento tiles.
 */
@Composable
fun HomeScreen(onOpenApps: (withSearch: Boolean) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L - now % 1_000L) // tick on the second boundary
        }
    }
    val is24h = DateFormat.is24HourFormat(context)
    val time = remember(now / 60_000, is24h) {
        DateFormat.format(if (is24h) "HH:mm" else "h:mm", Date(now)).toString()
    }
    val date = remember(now / 60_000) {
        DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEE d MMM"), Date(now)).toString()
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Dot.Black)
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { _, dy -> total += dy },
                    onDragEnd = { if (total < -120f) onOpenApps(false) },
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Column(Modifier.padding(top = 40.dp)) {
            BasicText(time, style = Dot.dots(84))
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(Dot.Red, CircleShape))
                Spacer(Modifier.size(8.dp))
                BasicText(date.uppercase(), style = Dot.caption.copy(fontSize = 12.sp))
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BasicText("↑  SWIPE UP FOR ALL APPS", style = Dot.caption.copy(color = Dot.Faint, fontSize = 10.sp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Dot.Surface, RoundedCornerShape(50))
                    .clickable { onOpenApps(true) }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                BasicText("Search apps", style = Dot.label.copy(color = Dot.Muted, fontSize = 15.sp))
            }
        }
    }
}
