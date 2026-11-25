package dev.rahmanfaizur.dotline

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import dev.rahmanfaizur.dotline.data.StorageState
import dev.rahmanfaizur.dotline.ui.home.QuickPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rahmanfaizur.dotline.data.AppEntry
import dev.rahmanfaizur.dotline.data.LauncherSettings
import dev.rahmanfaizur.dotline.ui.LocalIconLoader
import dev.rahmanfaizur.dotline.ui.drawer.DrawerScreen
import dev.rahmanfaizur.dotline.ui.home.AppActions
import dev.rahmanfaizur.dotline.ui.home.HomeScreen
import dev.rahmanfaizur.dotline.ui.home.HomeState
import dev.rahmanfaizur.dotline.ui.theme.Dot
import dev.rahmanfaizur.dotline.ui.theme.LocalAccent
import dev.rahmanfaizur.dotline.ui.theme.LocalIconShape

class MainActivity : ComponentActivity() {

    /** Bumped every time Home is pressed while we're already the home screen. */
    private var homePresses by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light status/nav bar icons: the whole launcher sits on black.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        val app = application as DotlineApp
        setContent {
            val settings by app.settings.settings.collectAsStateWithLifecycle(LauncherSettings())
            CompositionLocalProvider(
                LocalIconLoader provides app.icons,
                LocalAccent provides Dot.accent(settings.accent),
                LocalIconShape provides Dot.iconShape(settings.iconShape),
            ) {
                Launcher(app, settings, homePresses)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homePresses++
    }
}

@Composable
private fun Launcher(app: DotlineApp, settings: LauncherSettings, homePresses: Int) {
    val apps by app.apps.apps.collectAsStateWithLifecycle()
    // Created once; collection pauses automatically while the launcher isn't visible.
    val batteryFlow = remember(app) { app.system.battery() }
    val alarmFlow = remember(app) { app.system.nextAlarm() }
    val battery by batteryFlow.collectAsStateWithLifecycle(null)
    val nextAlarm by alarmFlow.collectAsStateWithLifecycle(null)

    val byComponent = remember(apps) { apps.associateBy { it.component.flattenToString() } }
    val defaults = remember(apps) { app.apps.defaults(apps) }
    fun resolve(keys: List<String>) = keys.mapNotNull { byComponent[it] }
    val visibleApps = remember(apps, settings.hidden) { apps.filter { it.component.flattenToString() !in settings.hidden } }

    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    var focusSearch by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current

    fun closeDrawer() {
        drawerOpen = false
        focusSearch = false
        query = ""
        focus.clearFocus()
    }

    val actions = remember(app) {
        AppActions(
            launch = { entry: AppEntry -> app.apps.launch(entry); closeDrawer() },
            longPress = { },
        )
    }

    // Home button: always come back to the clean home screen.
    LaunchedEffect(homePresses) { if (homePresses > 0) closeDrawer() }
    // A launcher sits at the bottom of the back stack; Back only closes the drawer.
    BackHandler { if (drawerOpen) closeDrawer() }

    // Page 0 = quick page (swipe right from home), page 1 = home.
    val pageCount = if (settings.showQuickPage) 2 else 1
    val homePage = pageCount - 1
    val pager = rememberPagerState(initialPage = homePage) { pageCount }
    val scope = rememberCoroutineScope()
    var storage by remember { mutableStateOf<StorageState?>(null) }
    LaunchedEffect(pager.currentPage) {
        if (pager.currentPage != homePage) storage = withContext(Dispatchers.IO) { app.system.storage() }
    }
    // Home button: also return from the quick page.
    LaunchedEffect(homePresses) { if (homePresses > 0 && pager.currentPage != homePage) pager.animateScrollToPage(homePage) }

    Box(Modifier.fillMaxSize().background(Dot.Black)) {
        HorizontalPager(state = pager, beyondViewportPageCount = 0, key = { it }) { page ->
            if (page == homePage) {
                HomeScreen(
                    state = HomeState(
                        favorites = resolve(settings.favorites ?: defaults.favorites),
                        dock = resolve(settings.dock ?: defaults.dock),
                        battery = battery,
                        nextAlarm = nextAlarm,
                        clock = settings.clock,
                    ),
                    actions = actions,
                    onOpenApps = { withSearch ->
                        focusSearch = withSearch
                        drawerOpen = true
                    },
                    onOpenSettings = { },
                )
            } else {
                QuickPage(
                    quick = resolve(settings.quick ?: defaults.quick),
                    storage = storage,
                    battery = battery,
                    actions = actions,
                    onOpenSettings = { },
                )
            }
        }
        // Back from the quick page goes home.
        BackHandler(enabled = !drawerOpen && pager.currentPage != homePage) {
            scope.launch { pager.animateScrollToPage(homePage) }
        }
        AnimatedVisibility(
            visible = drawerOpen,
            enter = fadeIn(tween(160)) + slideInVertically(tween(220)) { it / 8 },
            exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 8 },
        ) {
            DrawerScreen(
                apps = visibleApps,
                query = query,
                onQueryChange = { query = it },
                focusSearch = focusSearch,
                onLaunch = actions.launch,
            )
        }
    }
}
