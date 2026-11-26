package dev.rahmanfaizur.dotline

import android.app.ActivityOptions
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.rahmanfaizur.dotline.data.WallpaperKind
import dev.rahmanfaizur.dotline.ui.common.Backdrop
import dev.rahmanfaizur.dotline.ui.common.WallpaperArea
import dev.rahmanfaizur.dotline.clock.NightClockActivity
import android.content.ComponentName
import androidx.core.app.NotificationManagerCompat
import dev.rahmanfaizur.dotline.domain.NotificationDots
import dev.rahmanfaizur.dotline.notify.DotlineNotifications
import dev.rahmanfaizur.dotline.ui.LocalDots
import dev.rahmanfaizur.dotline.ui.home.PlayerActions
import dev.rahmanfaizur.dotline.ui.common.useDarkText
import dev.rahmanfaizur.dotline.ui.theme.LocalDarkText
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
import dev.rahmanfaizur.dotline.ui.common.LocalTextTone
import dev.rahmanfaizur.dotline.ui.home.AppActions
import dev.rahmanfaizur.dotline.ui.home.AppSlot
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

    /** Which wallpaper the photo being picked is for (My photo or Halftone). */
    private var photoFor: WallpaperKind? = null

    /** Android's photo picker: Dotline only receives the one photo chosen, no storage access. */
    private val photoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val kind = photoFor
        photoFor = null
        if (uri != null && kind != null) {
            val app = application as DotlineApp
            app.appScope.launch { if (app.wallpapers.savePhoto(uri)) app.settings.setWallpaper(kind) }
        }
    }

    private fun pickPhoto(kind: WallpaperKind) {
        photoFor = kind
        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    /** Android 13+ asks before an app may post; only used for the optional player card. */
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    private fun allowPlayerCard() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else if (!androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            runCatching {
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            }
        }
    }

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
                    pickPhoto = ::pickPhoto,
                    allowPlayerCard = ::allowPlayerCard,
                    notificationAccessOnResume = notificationAccess,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshHomeRole()
    }

    /** Whether the user has allowed Dotline's notification access in Android's settings. */
    private var notificationAccess by mutableStateOf(false)

    private fun refreshHomeRole() {
        notificationAccess = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
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
private fun Launcher(
    app: DotlineApp,
    settings: LauncherSettings,
    homePresses: Int,
    home: HomeRole,
    pickPhoto: (WallpaperKind) -> Unit,
    allowPlayerCard: () -> Unit,
    notificationAccessOnResume: Boolean,
) {
    val apps by app.apps.apps.collectAsStateWithLifecycle()
    // Allowed = Android said so when we came to the front, or the listener has just connected.
    val listenerConnected by app.notifications.connected.collectAsStateWithLifecycle()
    val notificationAccess = notificationAccessOnResume || listenerConnected
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
    var editingHome by rememberSaveable { mutableStateOf(false) }
    var focusSearch by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<AppEntry?>(null) }
    var menuNote by remember { mutableStateOf<String?>(null) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var lockNote by remember { mutableStateOf<String?>(null) }
    val features by app.features.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    val view = LocalView.current
    val context = androidx.compose.ui.platform.LocalContext.current
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
            editingHome = false
            if (pager.currentPage != homePage) pager.animateScrollToPage(homePage)
        }
    }

    // The wallpaper is drawn once per change (on a background thread) at the screen's exact size.
    var screen by remember { mutableStateOf(IntSize.Zero) }
    val photoVersion by app.wallpapers.photoVersion.collectAsStateWithLifecycle()
    val accentArgb = Dot.accent(settings.accent).toArgb()
    val backdrop by produceState<Backdrop?>(null, settings.wallpaper, accentArgb, photoVersion, screen) {
        value = if (screen.width == 0) null else {
            app.wallpapers.render(settings.wallpaper, accentArgb, screen.width, screen.height)
                ?.let { Backdrop(it.full.asImageBitmap(), it.frosted.asImageBitmap(), it.brightness) }
        }
    }
    SideEffect { Dot.onWallpaper = backdrop != null }

    // Notifications: only which apps have one, and what's playing; empty unless switched on and allowed.
    val activeNotifications by app.notifications.active.collectAsStateWithLifecycle()
    val playing by app.notifications.nowPlaying.collectAsStateWithLifecycle()
    val installedPackages = remember(apps) { apps.mapTo(HashSet()) { it.component.packageName } }
    val dotApps = remember(settings.dotApps, installedPackages) { NotificationDots.chosen(settings.dotApps, installedPackages) }
    val dots = NotificationDots.visible(settings.notificationDots && notificationAccess, activeNotifications, dotApps)
    val byPackage = remember(apps) { apps.associateBy { it.component.packageName } }
    val shownPlayer = playing?.takeIf {
        settings.nowPlaying && notificationAccess && (!settings.spotifyOnly || it.packageName == "com.spotify.music")
    }
    val outputFlow = remember(app) { app.audio.names() }
    val outputName by outputFlow.collectAsStateWithLifecycle("This phone")
    fun openNotificationAccess() {
        val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, ComponentName(context, DotlineNotifications::class.java).flattenToString())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(detail) }.isFailure) {
            runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }

    CompositionLocalProvider(LocalDots provides dots) {
    WallpaperArea(
        backdrop = backdrop,
        style = settings.tileStyle,
        modifier = Modifier.fillMaxSize().onSizeChanged { screen = it },
    ) {
        // Registered first so every overlay's Back wins over it: on the home screen itself
        // Back does nothing, like any launcher (it used to close Dotline).
        BackHandler(enabled = true) {}
        // Back leaves edit mode first.
        BackHandler(enabled = editingHome) { editingHome = false }

        val darkText = useDarkText(settings.textTone, settings.tileStyle, backdrop)
        CompositionLocalProvider(LocalDarkText provides darkText, LocalTextTone provides settings.textTone) {
        HorizontalPager(state = pager, beyondViewportPageCount = 0, key = { it }, userScrollEnabled = !editingHome) { page ->
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
                    onTile = { app.shortcuts.open(it) },
                    editing = editingHome,
                    onEditChange = { editingHome = it },
                    blockOrder = settings.homeOrder,
                    onReorder = { order -> app.appScope.launch { app.settings.setHomeOrder(order) } },
                    onSwapApps = { a, b ->
                        val shownFav = resolve(favoriteKeys).take(4).map { it.component.flattenToString() }
                        val shownDock = resolve(dockKeys).take(4).map { it.component.flattenToString() }
                        fun keyAt(s: AppSlot) = (if (s.dock) shownDock else shownFav).getOrNull(s.index)
                        val ka = keyAt(a)
                        val kb = keyAt(b)
                        if (ka != null && kb != null) {
                            val fav = favoriteKeys.toMutableList()
                            val dock = dockKeys.toMutableList()
                            val listA = if (a.dock) dock else fav
                            val listB = if (b.dock) dock else fav
                            val ia = listA.indexOf(ka)
                            val ib = listB.indexOf(kb)
                            // Swap the two spots (same list or home ↔ dock).
                            listA[ia] = kb
                            listB[ib] = ka
                            app.appScope.launch {
                                app.settings.setFavorites(fav.distinct())
                                app.settings.setDock(dock.distinct())
                            }
                        }
                    },
                    nowPlaying = shownPlayer,
                    playerStyle = settings.playerStyle,
                    playerActions = shownPlayer?.let { np ->
                        PlayerActions(
                            sourceName = byPackage[np.packageName]?.label ?: "Music",
                            outputName = outputName,
                            openPlayer = {
                                context.packageManager.getLaunchIntentForPackage(np.packageName)?.let {
                                    runCatching { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                                }
                            },
                            openOutput = { app.audio.openPicker(np.packageName) },
                        )
                    },
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
                    actions = actions,
                    onOpenSettings = { settingsOpen = true },
                    readMemory = { app.system.memory() },
                    onTile = { app.shortcuts.open(it) },
                )
            }
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
                backdrop = backdrop,
                background = settings.drawerBackground,
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
                    dot = if (settings.notificationDots) entry.component.packageName in dotApps else null,
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
                    toggleDot = {
                        val pkg = entry.component.packageName
                        app.appScope.launch { app.settings.setDotApps(if (pkg in dotApps) dotApps - pkg else dotApps + pkg) }
                        closeMenu()
                    },
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
                backdrop = backdrop,
                hasPhoto = app.wallpapers.hasPhoto,
                photoVersion = photoVersion,
                thumbnail = { k, w, h -> app.wallpapers.thumbnail(k, accentArgb, w, h) },
                lockThumbnail = { k, w, h -> app.wallpapers.thumbnail(k, accentArgb, w, h, lock = true) },
                lockNote = lockNote,
                features = features,
                notificationAccess = notificationAccess,
                dotAppEntries = apps.filter { it.component.packageName in dotApps }.distinctBy { it.component.packageName },
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
                    setWallpaper = { k ->
                        if (k == WallpaperKind.Photo && !app.wallpapers.hasPhoto) pickPhoto(k)
                        else app.appScope.launch { store.setWallpaper(k) }
                    },
                    pickPhoto = pickPhoto,
                    setTileStyle = { st -> app.appScope.launch { store.setTileStyle(st) } },
                    setDrawerBackground = { bg -> app.appScope.launch { store.setDrawerBackground(bg) } },
                    setTextTone = { t -> app.appScope.launch { store.setTextTone(t) } },
                    useOnLockScreen = {
                        lockNote = "Getting it ready…"
                        app.appScope.launch {
                            val w = screen.width; val h = screen.height
                            // Preferred: the phone's own wallpaper screen (the only way Realme shows it).
                            val intent = app.wallpapers.lockScreenIntent(settings.wallpaper, accentArgb, w, h)
                            val opened = intent != null && runCatching { context.startActivity(intent) }.isSuccess
                            lockNote = if (opened) {
                                "In the screen that opened, choose Lock screen (or Both)."
                            } else if (app.wallpapers.setLockScreen(settings.wallpaper, accentArgb, w, h)) {
                                "Done. Lock your phone to see it."
                            } else {
                                "Your phone didn't allow it. Set it from the phone's wallpaper settings instead."
                            }
                        }
                    },
                    openNightClock = {
                        context.startActivity(Intent(context, NightClockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    },
                    openScreensaverSettings = {
                        // Some phones hide Android's screensaver page; Display settings is the closest fallback.
                        val ok = runCatching { context.startActivity(Intent(Settings.ACTION_DREAM_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
                        if (!ok) runCatching { context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    },
                    setFeature = { f, on -> app.features.set(f, on) },
                    setNotificationDots = { on ->
                        app.appScope.launch { store.setNotificationDots(on) }
                        if (on && !notificationAccess) openNotificationAccess()
                    },
                    setNowPlaying = { on ->
                        app.appScope.launch { store.setNowPlaying(on) }
                        if (on && !notificationAccess) openNotificationAccess()
                    },
                    openNotificationAccess = ::openNotificationAccess,
                    setPlayerStyle = { st -> app.appScope.launch { store.setPlayerStyle(st) } },
                    setSpotifyOnly = { only -> app.appScope.launch { store.setSpotifyOnly(only) } },
                    setPlayerCard = { on ->
                        app.appScope.launch { store.setPlayerCard(on) }
                        if (on) allowPlayerCard()
                    },
                    openSpotifyNotifications = {
                        val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, "com.spotify.music")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(i) }
                    },
                    removeDotApp = { e -> app.appScope.launch { store.setDotApps(dotApps - e.component.packageName) } },
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
}
