package dev.rahmanfaizur.dotline.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.rememberUpdatedState
import kotlin.math.abs
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.rahmanfaizur.dotline.data.AppEntry
import dev.rahmanfaizur.dotline.domain.AppIndex
import dev.rahmanfaizur.dotline.ui.AppIcon
import dev.rahmanfaizur.dotline.ui.home.AppActions
import dev.rahmanfaizur.dotline.ui.home.appClicks
import dev.rahmanfaizur.dotline.ui.theme.Dot
import dev.rahmanfaizur.dotline.ui.theme.LocalAccent
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import dev.rahmanfaizur.dotline.data.DrawerBackground
import dev.rahmanfaizur.dotline.ui.common.Backdrop
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 * All apps. Empty search: A–Z grid with a big letter for the section you're in and a
 * letter strip to jump. Typing: ranked results, Enter opens the top one.
 */
@Composable
fun DrawerScreen(
    apps: List<AppEntry>,
    query: String,
    onQueryChange: (String) -> Unit,
    focusSearch: Boolean,
    actions: AppActions,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    background: DrawerBackground = DrawerBackground.Black,
) {
    // Plain black when there's no wallpaper; otherwise the chosen see-through look.
    val bg = if (backdrop == null) DrawerBackground.Black else background
    val glassy = bg != DrawerBackground.Black
    val results = remember(apps, query) { AppIndex.search(apps, query) { it.label } }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val pull = rememberPullToClose(onClose)

    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { pull.height = it.height.toFloat() }
            // Read in the draw phase only, so following the finger never recomposes the list.
            .graphicsLayer {
                translationY = pull.offset
                alpha = 1f - (pull.offset / pull.height.coerceAtLeast(1f)).coerceIn(0f, 1f) * 0.6f
            }
            .testTag("drawer"),
    ) {
    DrawerGround(bg, backdrop)
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .then(if (bg == DrawerBackground.Sheet) Modifier.padding(top = SheetGap) else Modifier)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp),
    ) {
        // The top part (search + big letter) can be dragged down to close too.
        Column(Modifier.draggable(pull.headerDrag, Orientation.Vertical, onDragStopped = { v -> pull.settle(v) })) {
        Spacer(Modifier.height(12.dp))
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            onGo = { results.firstOrNull()?.let { actions.launch(it, null) } },
            focusRequester = focusRequester,
            glassy = glassy,
        )
        Spacer(Modifier.height(12.dp))
        }
        if (query.isBlank()) {
            AlphabetGrid(apps, actions, Modifier.weight(1f).nestedScroll(pull.connection), pull)
        } else {
            SearchResults(results, actions, Modifier.weight(1f).nestedScroll(pull.connection), glassy)
        }
    }
    }
}

/** The glass sheet opens right up to just below the status bar. */
private val SheetGap = 6.dp
private val GlassFill = Color(0x1AFFFFFF)
private val GlassEdge = Color(0x24FFFFFF)

/**
 * What's behind the app list. Frosted reuses the small blurred wallpaper copy made for the
 * tiles, scaled up, so it costs nothing extra while scrolling.
 */
@Composable
private fun DrawerGround(bg: DrawerBackground, backdrop: Backdrop?) {
    when {
        backdrop == null || bg == DrawerBackground.Black -> Box(Modifier.fillMaxSize().background(Dot.Black))
        bg == DrawerBackground.Frosted -> {
            Image(backdrop.frosted, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, filterQuality = FilterQuality.Low)
            Box(Modifier.fillMaxSize().background(Color(0x8C000000)))
        }
        bg == DrawerBackground.Dimmed -> {
            Image(backdrop.full, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color(0xA8000000)))
        }
        else -> { // Sheet
            Image(backdrop.full, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            val shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
            Box(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = SheetGap)
                    .clip(shape)
                    .border(1.dp, GlassEdge, shape),
            ) {
                Image(backdrop.frosted, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, filterQuality = FilterQuality.Low)
                Box(Modifier.fillMaxSize().background(Color(0x99000000)))
            }
        }
    }
}

/**
 * Pull-to-close. Once the list is at the top, pulling further moves the whole drawer down
 * with your finger; let go past a quarter of the screen (or flick down) and it slides away,
 * otherwise it springs back. Any scroll also hides the keyboard.
 */
@Stable
private class PullToClose(
    private val scope: CoroutineScope,
    private val focus: FocusManager,
    private val close: State<() -> Unit>,
) {
    var offset by mutableFloatStateOf(0f)
    var height = 1f

    private var settling: Job? = null

    private fun dragBy(dy: Float): Float {
        val before = offset
        offset = (offset + dy).coerceAtLeast(0f)
        return offset - before
    }

    /** Animates to closed or back open, from the finger's last speed (px/s, down is positive). */
    fun settle(velocityY: Float) {
        if (offset <= 0f) return
        val shut = offset > height * 0.25f || velocityY > 1800f
        settling?.cancel()
        settling = scope.launch {
            animate(offset, if (shut) height else 0f, velocityY, spring(stiffness = Spring.StiffnessMediumLow)) { v, _ -> offset = v }
            if (shut) close.value()
        }
    }

    val headerDrag = DraggableState { dy -> settling?.cancel(); dragBy(dy) }

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput) return Offset.Zero
            if (abs(available.y) > 1f) focus.clearFocus()
            // Dragging back up first un-pulls the drawer before the list scrolls again.
            if (offset > 0f && available.y < 0f) return Offset(0f, dragBy(available.y))
            return Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput && available.y > 0f) {
                settling?.cancel()
                return Offset(0f, dragBy(available.y))
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (offset <= 0f) return Velocity.Zero
            settle(available.y)
            return available // the drawer takes the fling; the list stays put
        }
    }
}

@Composable
private fun rememberPullToClose(onClose: () -> Unit): PullToClose {
    val focus = LocalFocusManager.current
    val close = rememberUpdatedState(onClose)
    val scope = rememberCoroutineScope()
    return remember { PullToClose(scope, focus, close) }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onGo: () -> Unit,
    focusRequester: FocusRequester,
    glassy: Boolean,
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(color = Dot.Ink, fontSize = 15.sp),
        cursorBrush = SolidColor(LocalAccent.current),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Go,
            autoCorrectEnabled = false,
            capitalization = KeyboardCapitalization.None,
        ),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .testTag("searchField"),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (glassy) Modifier.background(GlassFill, RoundedCornerShape(50)).border(1.dp, GlassEdge, RoundedCornerShape(50))
                        else Modifier.background(Dot.Surface, RoundedCornerShape(50)),
                    )
                    .padding(horizontal = 18.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) BasicText("Search apps", style = TextStyle(color = Dot.Muted, fontSize = 15.sp))
                    inner()
                }
                if (query.isNotEmpty()) {
                    BasicText(
                        "✕",
                        style = TextStyle(color = Dot.Muted, fontSize = 15.sp),
                        modifier = Modifier.clickable { onQueryChange("") }.padding(start = 12.dp),
                    )
                }
            }
        },
    )
}

/** One continuous A–Z grid; the big letter tells you which section you're in. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlphabetGrid(apps: List<AppEntry>, actions: AppActions, modifier: Modifier, pull: PullToClose) {
    val accent = LocalAccent.current
    val sections = remember(apps) { AppIndex.sections(apps) { it.label } }
    val ordered = remember(sections) { sections.flatMap { it.items } }
    val letters = remember(sections) { sections.flatMap { s -> s.items.map { s.letter } } }
    val firstIndex = remember(sections) {
        var i = 0
        sections.associate { s -> (s.letter to i).also { i += s.items.size } }
    }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    val scrolledTo by remember(letters) {
        derivedStateOf { letters.getOrNull(gridState.firstVisibleItemIndex) ?: ' ' }
    }
    // While a finger is on the strip, show the letter under it (the list may not be able to
    // scroll all the way, e.g. the last few letters all fit on screen).
    var scrubbing by remember { mutableStateOf<Char?>(null) }
    val current = scrubbing ?: scrolledTo

    Column(modifier) {
        BasicText(
            current.toString(),
            style = Dot.dots(84, accent),
            modifier = Modifier
                .padding(bottom = 4.dp)
                .draggable(pull.headerDrag, Orientation.Vertical, onDragStopped = { v -> pull.settle(v) })
                .testTag("sectionLetter"),
        )

        Row(Modifier.weight(1f)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                state = gridState,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(ordered.size, key = { ordered[it].key }) { i -> AppCell(ordered[i], actions) }
            }

            LetterRail(
                letters = remember(sections) { sections.map { it.letter } },
                current = current,
                onPick = { letter -> firstIndex[letter]?.let { scope.launch { gridState.scrollToItem(it) } } },
                onScrub = { scrubbing = it },
            )
        }
    }
}

/**
 * A–Z down the right edge. Tap a letter, or keep your finger on the rail and slide: the list
 * follows the letter under your finger, with a light tick each time it changes.
 */
@Composable
private fun LetterRail(letters: List<Char>, current: Char, onPick: (Char) -> Unit, onScrub: (Char?) -> Unit) {
    val accent = LocalAccent.current
    val haptics = LocalHapticFeedback.current
    val pick by rememberUpdatedState(onPick)
    val scrub by rememberUpdatedState(onScrub)
    // Vertical centre of each letter; a plain map, so layout doesn't recompose anything.
    val centers = remember(letters) { HashMap<Char, Float>() }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(26.dp)
            .testTag("letterStrip")
            .pointerInput(letters) {
                awaitEachGesture {
                    var last: Char? = null
                    fun under(y: Float) {
                        val letter = centers.minByOrNull { abs(it.value - y) }?.key ?: return
                        if (letter != last) {
                            last = letter
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                            scrub(letter)
                            pick(letter)
                        }
                    }
                    val down = awaitFirstDown()
                    down.consume()
                    under(down.position.y)
                    try {
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            under(change.position.y)
                        }
                    } finally {
                        scrub(null)
                    }
                }
            }
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            val on = letter == current
            BasicText(
                letter.toString(),
                style = Dot.caption.copy(
                    color = if (on) accent else Dot.Muted,
                    fontSize = if (on) 13.sp else 11.sp,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier
                    .onGloballyPositioned { centers[letter] = it.positionInParent().y + it.size.height / 2f }
                    .semantics {
                        role = Role.Button
                        onClick(label = "Jump to $letter") { pick(letter); true }
                    },
            )
        }
    }
}

@Composable
private fun AppCell(app: AppEntry, actions: AppActions) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .appClicks(app, actions)
            .padding(vertical = 2.dp),
    ) {
        AppIcon(app, 52.dp)
        Spacer(Modifier.height(6.dp))
        BasicText(
            app.label,
            style = Dot.label.copy(fontSize = 11.sp, textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun SearchResults(results: List<AppEntry>, actions: AppActions, modifier: Modifier, glassy: Boolean) {
    val accent = LocalAccent.current
    if (results.isEmpty()) {
        BasicText("No apps match", style = Dot.caption, modifier = modifier.padding(top = 8.dp))
        return
    }
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(results, key = { it.key }) { app ->
            val top = app == results.first()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (top) Modifier
                            .background(if (glassy) GlassFill else Dot.Surface, RoundedCornerShape(18.dp))
                            .border(1.dp, accent, RoundedCornerShape(18.dp))
                        else Modifier,
                    )
                    .appClicks(app, actions)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                AppIcon(app, if (top) 44.dp else 36.dp)
                Spacer(Modifier.size(14.dp))
                Column {
                    BasicText(app.label, style = Dot.label.copy(fontSize = 15.sp))
                    if (top) BasicText("Enter to open", style = Dot.caption.copy(fontSize = 10.sp))
                }
            }
        }
    }
}
