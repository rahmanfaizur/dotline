package dev.rahmanfaizur.dotline

import android.app.ActivityOptions
import android.app.role.RoleManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalView
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
import dev.rahmanfaizur.dotline.ui.home.SetHomePrompt
import dev.rahmanfaizur.dotline.ui.menu.AppMenu
import dev.rahmanfaizur.dotline.ui.menu.MenuActions
import dev.rahmanfaizur.dotline.ui.menu.MenuTarget
import dev.rahmanfaizur.dotline.ui.settings.SettingsActions
import dev.rahmanfaizur.dotline.ui.settings.SettingsLists
import dev.rahmanfaizur.dotline.ui.settings.SettingsScreen
import dev.rahmanfaizur.dotline.ui.theme.Dot
import dev.rahmanfaizur.dotline.ui.theme.LocalAccent
import dev.rahmanfaizur.dotline.ui.theme.LocalIconShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    /** Bumped every time Home is pressed while we're already the home screen. */
    private var homePresses by mutableIntStateOf(0)

    /** Whether Dotline is the phone's default home app; re-checked every time we come back. */
    private var isDefaultHome by mutableStateOf(true)

    /** Set once Android's popup was shown and declined, so the next tap goes to Settings instead. */
    private var roleDeclined = false

    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshHomeRole()
        if (!isDefaultHome) roleDeclined = true
    }

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
                Launcher(
                    app = app,
                    settings = settings,
                    homePresses = homePresses,
                    home = HomeRole(isDefaultHome, ::askToBeHome, ::openHomeSettings),
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshHomeRole()
    }

    private fun refreshHomeRole() {
        isDefaultHome = getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_HOME) ?: true
    }

    /** Android's own "Set Dotline as default home app?" popup; falls back to Settings if it was declined. */
    private fun askToBeHome() {
        val roles = getSystemService(RoleManager::class.java)
        if (!roleDeclined && roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME)) {
            roleRequest.launch(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
        } else {
            openHomeSettings()
        }
    }

    /** The phone's "Default apps → Home" screen, to pick or switch launchers. */
    private fun openHomeSettings() {
        val opened = runCatching { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }.isSuccess ||
            runCatching { startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) }.isSuccess
        if (!opened) startActivity(Intent(Settings.ACTION_SETTINGS))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homePresses++
    }
}

/** Is Dotline the default home app, and how to change that. */
class HomeRole(
    val isDefault: Boolean,
    val ask: () -> Unit,
    val openSettings: () -> Unit,
)

@Composable
private fun Launcher(app: DotlineApp, settings: LauncherSettings, homePresses: Int, home: HomeRole) {
    val apps by app.apps.apps.collectAsStateWithLifecycle()
    val promptDismissed by app.settings.homePromptDismissed.collectAsStateWithLifecycle(true)
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
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val view = LocalView.current
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
            launch = { entry: AppEntry, bounds: android.graphics.Rect? ->
                // Grow the app out of the tapped icon (no bounds, e.g. Enter in search: plain open).
                val options = bounds?.let {
                    ActivityOptions.makeClipRevealAnimation(view, it.left, it.top, it.width(), it.height()).toBundle()
                }
                app.apps.launch(entry, bounds, options)
                closeDrawer()
            },
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
            settingsOpen = false
            if (pager.currentPage != homePage) pager.animateScrollToPage(homePage)
        }
    }

    Box(Modifier.fillMaxSize().background(Dot.Black)) {
        // Registered first so every overlay's Back wins over it: on the home screen itself
        // Back does nothing, like any launcher (it used to close Dotline).
        BackHandler(enabled = true) {}

        HorizontalPager(state = pager, beyondViewportPageCount = 0, key = { it }) { page ->
            if (page == homePage) {
                HomeScreen(
                    state = HomeState(
                        favorites = resolve(favoriteKeys),
                        dock = resolve(dockKeys),
                        battery = battery,
                        nextAlarm = nextAlarm,
                        clock = settings.clock,
                        appsLoaded = apps.isNotEmpty(),
                    ),
                    actions = actions,
                    onOpenApps = { withSearch ->
                        focusSearch = withSearch
                        drawerOpen = true
                    },
                    onOpenSettings = { settingsOpen = true },
                    setHome = if (!home.isDefault && !promptDismissed) {
                        SetHomePrompt(
                            set = home.ask,
                            notNow = { app.appScope.launch { app.settings.setHomePromptDismissed(true) } },
                        )
                    } else null,
                )
            } else {
                QuickPage(
                    quick = resolve(quickKeys),
                    storage = storage,
                    battery = battery,
                    actions = actions,
                    onOpenSettings = { settingsOpen = true },
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
                onClose = ::closeDrawer,
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

        AnimatedVisibility(
            visible = settingsOpen,
            enter = fadeIn(tween(160)) + slideInVertically(tween(220)) { it / 8 },
            exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 8 },
        ) {
            fun keyOf(e: AppEntry) = e.component.flattenToString()
            val store = app.settings
            SettingsScreen(
                settings = settings,
                isDefaultHome = home.isDefault,
                lists = SettingsLists(
                    favorites = resolve(favoriteKeys),
                    dock = resolve(dockKeys),
                    quick = resolve(quickKeys),
                    hidden = resolve(settings.hidden.toList()).sortedBy { it.label.lowercase() },
                ),
                actions = SettingsActions(
                    removeFavorite = { e -> app.appScope.launch { store.setFavorites(favoriteKeys - keyOf(e)) } },
                    removeDock = { e -> app.appScope.launch { store.setDock(dockKeys - keyOf(e)) } },
                    removeQuick = { e -> app.appScope.launch { store.setQuick(quickKeys - keyOf(e)) } },
                    unhide = { e -> app.appScope.launch { store.setHidden(settings.hidden - keyOf(e)) } },
                    setAccent = { i -> app.appScope.launch { store.setAccent(i) } },
                    setIconShape = { sh -> app.appScope.launch { store.setIconShape(sh) } },
                    setClock = { m -> app.appScope.launch { store.setClock(m) } },
                    setShowQuickPage = { on -> app.appScope.launch { store.setShowQuickPage(on) } },
                    setAsHome = home.ask,
                    changeHome = home.openSettings,
                    close = { settingsOpen = false },
                ),
            )
        }
        // Settings sits above the menu's Back handler, so it is registered last.
        BackHandler(enabled = settingsOpen && menuFor == null) { settingsOpen = false }
    }
}
