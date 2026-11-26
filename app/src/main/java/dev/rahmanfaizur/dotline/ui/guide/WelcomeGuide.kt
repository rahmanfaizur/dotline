package dev.rahmanfaizur.dotline.ui.guide

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rahmanfaizur.dotline.ui.theme.Dot
import dev.rahmanfaizur.dotline.ui.theme.LocalAccent
import kotlinx.coroutines.launch

/** Who made Dotline and where its code lives (shown in the guide and in Settings → About). */
const val CREDIT = "Open source · by @rahmanfaizur"
const val SOURCE = "github.com/rahmanfaizur/dotline · GPL-3.0"

private class Card(val title: String, val lines: List<String>)

private val CARDS = listOf(
    Card("DOTLINE", listOf("Your new home screen.", "Fast, private, and made of dots.")),
    Card("SWIPE", listOf(
        "Up: all your apps, A to Z. Drag the right edge to fly through letters.",
        "Right: calendar, storage, RAM and quick apps.",
        "Down: your notifications.",
    )),
    Card("SEARCH", listOf(
        "Type a few letters and press Enter to open the top app.",
        "Try a sum like 12*8, or a setting like wifi or battery.",
    )),
    Card("MAKE IT YOURS", listOf(
        "Long-press an empty spot to move tiles, swap apps and make folders.",
        "Settings is in that same bar: wallpapers, tile styles, icons, clock, music player.",
    )),
    Card("PRIVATE", listOf(
        "No internet permission, so nothing can ever leave your phone.",
        "No ads, no tracking, no accounts.",
        "Extras like notification dots only work if you allow them, and can be switched off any time.",
    )),
)

/** A short swipeable guide: shown once on first open, and again from Settings → About → How to use. */
@Composable
fun WelcomeGuide(onDone: () -> Unit) {
    val pager = rememberPagerState { CARDS.size }
    val scope = rememberCoroutineScope()
    val accent = LocalAccent.current
    val last = pager.currentPage == CARDS.lastIndex
    BackHandler { onDone() }
    Column(
        Modifier
            .fillMaxSize()
            .background(Dot.Black)
            .clickable(enabled = false) {}
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("welcomeGuide"),
    ) {
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { page ->
            val card = CARDS[page]
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                BasicText(card.title, style = Dot.dots(44, if (page == 0) accent else Dot.Ink))
                Spacer(Modifier.height(24.dp))
                card.lines.forEach { line ->
                    Row(Modifier.padding(bottom = 14.dp)) {
                        Box(Modifier.padding(top = 7.dp).size(6.dp).background(accent, CircleShape))
                        Spacer(Modifier.size(12.dp))
                        BasicText(line, style = Dot.label.copy(color = Dot.Ink, fontSize = 17.sp, lineHeight = 24.sp))
                    }
                }
                if (page == CARDS.lastIndex) {
                    Spacer(Modifier.height(20.dp))
                    BasicText(CREDIT, style = Dot.caption.copy(color = Dot.Ink, fontSize = 12.sp))
                    Spacer(Modifier.height(4.dp))
                    BasicText(SOURCE, style = Dot.caption.copy(fontSize = 11.sp))
                }
            }
        }
        // Page dots
        Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalArrangement = Arrangement.Center) {
            repeat(CARDS.size) { i ->
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (i == pager.currentPage) 9.dp else 6.dp)
                        .background(if (i == pager.currentPage) accent else Dot.Faint, CircleShape),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (!last) {
                BasicText(
                    "Skip",
                    style = Dot.label.copy(color = Dot.Muted, fontSize = 15.sp),
                    modifier = Modifier.clickable(onClick = onDone).testTag("guideSkip").padding(12.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            BasicText(
                if (last) "Start" else "Next",
                style = Dot.label.copy(color = Dot.Black, fontSize = 15.sp),
                modifier = Modifier
                    .background(accent, RoundedCornerShape(50))
                    .clickable { if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }
                    .testTag(if (last) "guideStart" else "guideNext")
                    .padding(horizontal = 26.dp, vertical = 12.dp),
            )
        }
    }
}
