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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import kotlinx.coroutines.launch

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
    modifier: Modifier = Modifier,
) {
    val results = remember(apps, query) { AppIndex.search(apps, query) { it.label } }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(Dot.Black)
            .testTag("drawer")
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            onGo = { results.firstOrNull()?.let(actions.launch) },
            focusRequester = focusRequester,
        )
        Spacer(Modifier.height(12.dp))
        if (query.isBlank()) {
            AlphabetGrid(apps, actions, Modifier.weight(1f))
        } else {
            SearchResults(results, actions, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onGo: () -> Unit,
    focusRequester: FocusRequester,
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
                    .background(Dot.Surface, RoundedCornerShape(50))
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
private fun AlphabetGrid(apps: List<AppEntry>, actions: AppActions, modifier: Modifier) {
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

    val current by remember(letters) {
        derivedStateOf { letters.getOrNull(gridState.firstVisibleItemIndex) ?: ' ' }
    }

    Column(modifier) {
        BasicText(current.toString(), style = Dot.dots(84, accent), modifier = Modifier.padding(bottom = 4.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            state = gridState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(ordered.size, key = { ordered[it].key }) { i -> AppCell(ordered[i], actions) }
        }

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            sections.forEach { s ->
                val on = s.letter == current
                BasicText(
                    s.letter.toString(),
                    style = Dot.caption.copy(color = if (on) accent else Dot.Faint, fontSize = 13.sp, textAlign = TextAlign.Center),
                    modifier = Modifier
                        .width(24.dp)
                        .clickable { firstIndex[s.letter]?.let { scope.launch { gridState.scrollToItem(it) } } }
                        .padding(vertical = 6.dp),
                )
            }
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
private fun SearchResults(results: List<AppEntry>, actions: AppActions, modifier: Modifier) {
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
                            .background(Dot.Surface, RoundedCornerShape(18.dp))
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
