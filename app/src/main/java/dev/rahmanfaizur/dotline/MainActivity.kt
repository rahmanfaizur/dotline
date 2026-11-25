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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rahmanfaizur.dotline.data.AppEntry
import dev.rahmanfaizur.dotline.data.LauncherSettings
import dev.rahmanfaizur.dotline.data.StorageState
import dev.rahmanfaizur.dotline.domain.Pins
import dev.rahmanfaizur.dotline.ui.LocalIconLoader
import dev.rahmanfaizur.dotline.ui.drawer.DrawerScreen
import dev.rahmanfaizur.dotline.ui.home.AppActions
import dev.rahmanfaizur.dotline.ui.home.HomeScreen
import dev.rahmanfaizur.dotline.ui.home.HomeState
import dev.rahmanfaizur.dotline.ui.home.QuickPage
import dev.rahmanfaizur.dotline.ui.menu.AppMenu
import dev.rahmanfaizur.dotline.ui.menu.MenuActions
import dev.rahmanfaizur.dotline.ui.menu.MenuTarget
import dev.rahmanfaizur.dotline.ui.theme.Dot
import dev.rahmanfaizur.dotline.ui.theme.LocalAccent
import dev.rahmanfaizur.dotline.ui.theme.LocalIconShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val favoriteKeys = settings.favorites ?: defaults.favorites
    val dockKeys = settings.dock ?: defaults.dock
    val quickKeys = settings.quick ?: defaults.quick
    val visibleApps = remember(apps, settings.hidden) { apps.filter { it.component.flattenToString() !in settings.hidden } }

    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    var focusSearch by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<AppEntry?>(null) }
    var menuNote by remember { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    fun closeDrawer() {
        drawerOpen = false
        focusSearch = false
        query = ""
        focus.clearFocus()
    }

    fun closeMenu() {
        menuFor = null
        menuNote = null
    }

    val actions = remember(app) {
        AppActions(
            launch = { entry: AppEntry -> app.apps.launch(entry); closeDrawer() },
            longPress = { entry: AppEntry -> focus.clearFocus(); menuNote = null; menuFor = entry },
        )
    }

    // Page 0 = quick page (swipe right from home), page 1 = home.
    val pageCount = if (settings.showQuickPage) 2 else 1
    val homePage = pageCount - 1
    val pager = rememberPagerState(initialPage = homePage) { pageCount }
    var storage by remember { mutableStateOf<StorageState?>(null) }
    LaunchedEffect(pager.currentPage) {
        if (pager.currentPage != homePage) storage = withContext(Dispatchers.IO) { app.system.storage() }
    }

    // Home button: close everything and return to the home page.
    LaunchedEffect(homePresses) {
        if (homePresses > 0) {
            closeMenu()
            closeDrawer()
            if (pager.currentPage != homePage) pager.animateScrollToPage(homePage)
        }
    }

    Box(Modifier.fillMaxSize().background(Dot.Black)) {
        HorizontalPager(state = pager, beyondViewportPageCount = 0, key = { it }) { page ->
            if (page == homePage) {
                HomeScreen(
                    state = HomeState(
                        favorites = resolve(favoriteKeys),
                        dock = resolve(dockKeys),
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
                    quick = resolve(quickKeys),
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
                actions = actions,
            )
        }
        // A launcher sits at the bottom of the back stack; Back only closes overlays.
        BackHandler(enabled = drawerOpen) { closeDrawer() }

        AnimatedVisibility(visible = menuFor != null, enter = fadeIn(tween(120)), exit = fadeOut(tween(120))) {
            val entry = menuFor ?: return@AnimatedVisibility
            val key = entry.component.flattenToString()

            /** Shared add/remove logic for the three pinned lists. */
            fun toggle(list: List<String>, place: String, save: suspend (List<String>) -> Unit) {
                val (updated, outcome) = Pins.toggle(list, key)
                if (outcome == Pins.Outcome.Full) {
                    menuNote = "$place already has ${Pins.MAX} apps. Remove one first."
                } else {
                    app.appScope.launch { save(updated) }
                    closeMenu()
                }
            }

            AppMenu(
                target = MenuTarget(
                    app = entry,
                    onHome = key in favoriteKeys,
                    inDock = key in dockKeys,
                    inQuick = key in quickKeys,
                    hidden = key in settings.hidden,
                    canUninstall = !app.apps.isSystemApp(entry),
                ),
                actions = MenuActions(
                    info = { app.apps.openAppInfo(entry); closeMenu() },
                    toggleHome = { toggle(favoriteKeys, "Home", app.settings::setFavorites) },
                    toggleDock = { toggle(dockKeys, "Dock", app.settings::setDock) },
                    toggleQuick = { toggle(quickKeys, "Quick open", app.settings::setQuick) },
                    toggleHidden = {
                        val hidden = if (key in settings.hidden) settings.hidden - key else settings.hidden + key
                        app.appScope.launch { app.settings.setHidden(hidden) }
                        closeMenu()
                    },
                    uninstall = { app.apps.requestUninstall(entry); closeMenu() },
                    dismiss = ::closeMenu,
                ),
                note = menuNote,
            )
        }
        BackHandler(enabled = menuFor != null) { closeMenu() }
    }
}
