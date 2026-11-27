package dev.rahmanfaizur.dotline.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class IconShape { Round, Square }
enum class ClockMode { System, H24, H12 }

/** How tiles sit on the wallpaper. On plain black, the see-through styles show as Solid. */
/** How Now playing looks on home. */
enum class PlayerStyle { Glow, DotWave, Vinyl }

/** Behind the app list. On a black wallpaper all of them show as black. */
enum class DrawerBackground { Frosted, Dimmed, Sheet, Black }

/** Text colour on home tiles. Auto picks black only on see-through tiles over a bright wallpaper. */
enum class TextTone { Auto, White, Black }

/** Font for all of Dotline's text (the dot clock and dot letters stay dotted). Default is the look it always had. */
enum class TextFont { Default, Italic, ItalicSerif, BoldItalicSerif, BoldSerif }

enum class TileStyle { Smoke, Glass, Tinted, Outline, Floating, Solid }

/**
 * Everything the user can change. App lists hold component names ("pkg/.Activity").
 * A null list means "not customised yet": the launcher picks sensible defaults.
 */
data class LauncherSettings(
    val favorites: List<String>? = null,
    val dock: List<String>? = null,
    val quick: List<String>? = null,
    val hidden: Set<String> = emptySet(),
    val accent: Int = 0,
    val iconShape: IconShape = IconShape.Round,
    val clock: ClockMode = ClockMode.System,
    val showQuickPage: Boolean = true,
    val wallpaper: WallpaperKind = WallpaperKind.Black,
    val tileStyle: TileStyle = TileStyle.Smoke,
    val drawerBackground: DrawerBackground = DrawerBackground.Frosted,
    val textTone: TextTone = TextTone.Auto,
    val textFont: TextFont = TextFont.Default,
    /** Off until the user turns them on (they need notification access). */
    val notificationDots: Boolean = false,
    val nowPlaying: Boolean = false,
    /** Apps that get dots; null = the defaults that are installed. */
    val dotApps: Set<String>? = null,
    val playerStyle: PlayerStyle = PlayerStyle.Glow,
    /** Now playing only for Spotify (true) or for any music app. */
    val spotifyOnly: Boolean = true,
    /** Also show the player as a card in the pull-down panel and on the lock screen. */
    val playerCard: Boolean = false,
    /** Keep the player on home when the music app is closed: the last song, or a "Play something" card. */
    val keepPlayer: Boolean = true,
    /** Order of the blocks on home (long-press empty space → drag the grips). */
    val homeOrder: List<HomeBlock> = HomeBlock.Default,
    /** Also make it the phone's wallpaper, so Recents and app animations match Dotline. */
    val phoneWallpaper: Boolean = true,
    /** True once a wallpaper was picked in Dotline; until then the phone's own wallpaper is never touched. */
    val wallpaperChosen: Boolean = false,
    /** Swipe down on home opens the notification panel. */
    val swipeDownNotifications: Boolean = true,
    /** Search also answers sums and offers phone settings. */
    val smartSearch: Boolean = true,
    val iconStyle: IconStyle = IconStyle.Original,
    val clockStyle: ClockStyle = ClockStyle.Big,
    /** How many app tiles home shows: 4, 6 or 8. */
    val homeApps: Int = 4,
    /** Smaller tiles; home apps as icons only. */
    val compact: Boolean = false,
    /** Making folders by dropping an app on another in edit mode. */
    val foldersOn: Boolean = false,
    val folders: Map<String, dev.rahmanfaizur.dotline.domain.Folder> = emptyMap(),
)

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** The movable parts of home, top to bottom by default. */
/** App icons: their own colours, or their simple one-colour icon in white or the accent colour. */
enum class IconStyle { Original, White, Accent }

/** How the clock tile looks on home. */
enum class ClockStyle { Big, Stacked, Small }

enum class HomeBlock { Clock, Status, Apps, Player;
    companion object {
        val Default = entries.toList()
        /** Saved order, ignoring unknown names and adding any block that's missing. */
        fun parse(saved: String?): List<HomeBlock> {
            val list = saved?.split(",")?.mapNotNull { n -> entries.firstOrNull { it.name == n } }?.distinct() ?: return Default
            return list + entries.filter { it !in list }
        }
    }
}

/** Settings saved on the phone only (DataStore file in the app's private folder). */
class SettingsStore(context: Context) {
    private val store = context.applicationContext.settingsStore

    val settings: Flow<LauncherSettings> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it.toSettings() }

    /** Whether "Not now" was tapped on the set-home tile. Starts as true so the tile never flashes before loading. */
    val homePromptDismissed: Flow<Boolean> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it[HOME_PROMPT_DISMISSED] ?: false }

    suspend fun setFavorites(list: List<String>) = store.edit { it[FAVORITES] = list.join() }
    /** Back to the default home apps. */
    suspend fun resetFavorites() = store.edit { it.remove(FAVORITES); it.remove(FOLDERS) }
    suspend fun setDock(list: List<String>) = store.edit { it[DOCK] = list.join() }
    suspend fun setQuick(list: List<String>) = store.edit { it[QUICK] = list.join() }
    suspend fun setHidden(set: Set<String>) = store.edit { it[HIDDEN] = set }
    suspend fun setAccent(index: Int) = store.edit { it[ACCENT] = index }
    suspend fun setIconShape(shape: IconShape) = store.edit { it[ICON_SHAPE] = shape.name }
    suspend fun setClock(mode: ClockMode) = store.edit { it[CLOCK] = mode.name }
    suspend fun setShowQuickPage(show: Boolean) = store.edit { it[QUICK_PAGE] = show }
    suspend fun setWallpaper(kind: WallpaperKind) = store.edit { it[WALLPAPER] = kind.name }
    /** Back to "never picked" (plain black, phone wallpaper left alone). */
    suspend fun resetWallpaper() = store.edit { it.remove(WALLPAPER) }
    suspend fun setTileStyle(style: TileStyle) = store.edit { it[TILE_STYLE] = style.name }
    suspend fun setNotificationDots(on: Boolean) = store.edit { it[NOTIF_DOTS] = on }
    suspend fun setNowPlaying(on: Boolean) = store.edit { it[NOW_PLAYING] = on }
    suspend fun setPlayerStyle(style: PlayerStyle) = store.edit { it[PLAYER_STYLE] = style.name }
    suspend fun setSpotifyOnly(only: Boolean) = store.edit { it[SPOTIFY_ONLY] = only }
    suspend fun setPlayerCard(on: Boolean) = store.edit { it[PLAYER_CARD] = on }
    suspend fun setKeepPlayer(on: Boolean) = store.edit { it[KEEP_PLAYER] = on }
    suspend fun setFoldersOn(on: Boolean) = store.edit { it[FOLDERS_ON] = on }
    /** Home apps and folders change together (one save, so they never disagree). */
    suspend fun setHomeLayout(favorites: List<String>, folders: Collection<dev.rahmanfaizur.dotline.domain.Folder>) = store.edit {
        it[FAVORITES] = favorites.join()
        it[FOLDERS] = dev.rahmanfaizur.dotline.domain.Folder.encode(folders)
    }
    suspend fun setHomeApps(count: Int) = store.edit { it[HOME_APPS] = count }
    suspend fun setCompact(on: Boolean) = store.edit { it[COMPACT] = on }
    suspend fun setClockStyle(style: ClockStyle) = store.edit { it[CLOCK_STYLE] = style.name }
    suspend fun setIconStyle(style: IconStyle) = store.edit { it[ICON_STYLE] = style.name }
    suspend fun setSmartSearch(on: Boolean) = store.edit { it[SMART_SEARCH] = on }
    suspend fun setSwipeDownNotifications(on: Boolean) = store.edit { it[SWIPE_DOWN] = on }
    suspend fun setPhoneWallpaper(on: Boolean) = store.edit { it[PHONE_WALLPAPER] = on }
    suspend fun setHomeOrder(order: List<HomeBlock>) = store.edit { it[HOME_ORDER] = order.joinToString(",") { b -> b.name } }
    suspend fun setDotApps(apps: Set<String>) = store.edit { it[DOT_APPS] = apps }
    suspend fun setTextTone(tone: TextTone) = store.edit { it[TEXT_TONE] = tone.name }
    suspend fun setTextFont(font: TextFont) = store.edit { it[TEXT_FONT] = font.name }
    suspend fun setDrawerBackground(bg: DrawerBackground) = store.edit { it[DRAWER_BG] = bg.name }
    suspend fun setHomePromptDismissed(dismissed: Boolean) = store.edit { it[HOME_PROMPT_DISMISSED] = dismissed }

    /** Whether the welcome guide was seen. Starts as true so it never flashes before loading. */
    val welcomeSeen: Flow<Boolean> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it[WELCOME_SEEN] ?: false }
    suspend fun setWelcomeSeen(seen: Boolean) = store.edit { it[WELCOME_SEEN] = seen }

    private fun Preferences.toSettings() = LauncherSettings(
        favorites = this[FAVORITES]?.split(),
        dock = this[DOCK]?.split(),
        quick = this[QUICK]?.split(),
        hidden = this[HIDDEN] ?: emptySet(),
        accent = this[ACCENT] ?: 0,
        iconShape = this[ICON_SHAPE]?.let { runCatching { IconShape.valueOf(it) }.getOrNull() } ?: IconShape.Round,
        clock = this[CLOCK]?.let { runCatching { ClockMode.valueOf(it) }.getOrNull() } ?: ClockMode.System,
        showQuickPage = this[QUICK_PAGE] ?: true,
        wallpaper = this[WALLPAPER]?.let { runCatching { WallpaperKind.valueOf(it) }.getOrNull() } ?: WallpaperKind.Black,
        tileStyle = this[TILE_STYLE]?.let { runCatching { TileStyle.valueOf(it) }.getOrNull() } ?: TileStyle.Smoke,
        drawerBackground = this[DRAWER_BG]?.let { runCatching { DrawerBackground.valueOf(it) }.getOrNull() }
            ?: DrawerBackground.Frosted,
        notificationDots = this[NOTIF_DOTS] ?: false,
        nowPlaying = this[NOW_PLAYING] ?: false,
        dotApps = this[DOT_APPS],
        playerStyle = this[PLAYER_STYLE]?.let { runCatching { PlayerStyle.valueOf(it) }.getOrNull() } ?: PlayerStyle.Glow,
        spotifyOnly = this[SPOTIFY_ONLY] ?: true,
        playerCard = this[PLAYER_CARD] ?: false,
        keepPlayer = this[KEEP_PLAYER] ?: true,
        homeOrder = HomeBlock.parse(this[HOME_ORDER]),
        phoneWallpaper = this[PHONE_WALLPAPER] ?: true,
        wallpaperChosen = this[WALLPAPER] != null,
        swipeDownNotifications = this[SWIPE_DOWN] ?: true,
        smartSearch = this[SMART_SEARCH] ?: true,
        homeApps = (this[HOME_APPS] ?: 4).takeIf { it in listOf(4, 6, 8) } ?: 4,
        compact = this[COMPACT] ?: false,
        foldersOn = this[FOLDERS_ON] ?: false,
        folders = dev.rahmanfaizur.dotline.domain.Folder.decode(this[FOLDERS]),
        clockStyle = this[CLOCK_STYLE]?.let { runCatching { ClockStyle.valueOf(it) }.getOrNull() } ?: ClockStyle.Big,
        iconStyle = this[ICON_STYLE]?.let { runCatching { IconStyle.valueOf(it) }.getOrNull() } ?: IconStyle.Original,
        textTone = this[TEXT_TONE]?.let { runCatching { TextTone.valueOf(it) }.getOrNull() } ?: TextTone.Auto,
        textFont = this[TEXT_FONT]?.let { runCatching { TextFont.valueOf(it) }.getOrNull() } ?: TextFont.Default,
    )

    /**
     * Backup: every Dotline setting (layout, colours, wallpaper choice, folders, switches) as JSON.
     * Your photo, notification access and the default-launcher choice aren't included.
     */
    suspend fun exportBackup(): String {
        val p = store.data.first()
        val out = org.json.JSONObject()
        for (k in BOOL_KEYS + INT_KEYS + STRING_KEYS) p[k]?.let { out.put(k.name, it) }
        for (k in SET_KEYS) p[k]?.let { out.put(k.name, org.json.JSONArray(it.toList())) }
        return org.json.JSONObject().put("app", "dotline").put("version", 1).put("settings", out).toString(2)
    }

    /** Loads a backup made by [exportBackup]. Unknown or wrongly typed entries are skipped. False if it isn't one. */
    suspend fun importBackup(text: String): Boolean {
        val s = runCatching {
            val o = org.json.JSONObject(text)
            require(o.optString("app") == "dotline")
            o.getJSONObject("settings")
        }.getOrNull() ?: return false
        store.edit { p ->
            for (k in BOOL_KEYS) if (s.has(k.name)) (s.opt(k.name) as? Boolean)?.let { p[k] = it }
            for (k in INT_KEYS) if (s.has(k.name)) (s.opt(k.name) as? Int)?.let { p[k] = it }
            for (k in STRING_KEYS) if (s.has(k.name)) (s.opt(k.name) as? String)?.let { p[k] = it }
            for (k in SET_KEYS) if (s.has(k.name)) (s.opt(k.name) as? org.json.JSONArray)?.let { a ->
                p[k] = (0 until a.length()).mapNotNull { a.opt(it) as? String }.toSet()
            }
        }
        return true
    }

    private fun List<String>.join() = joinToString("\n")
    private fun String.split(): List<String> = if (isEmpty()) emptyList() else split("\n")

    private companion object {
        val FAVORITES = stringPreferencesKey("favorites")
        val DOCK = stringPreferencesKey("dock")
        val QUICK = stringPreferencesKey("quick")
        val HIDDEN = stringSetPreferencesKey("hidden")
        val ACCENT = intPreferencesKey("accent")
        val ICON_SHAPE = stringPreferencesKey("icon_shape")
        val CLOCK = stringPreferencesKey("clock")
        val QUICK_PAGE = booleanPreferencesKey("quick_page")
        val WALLPAPER = stringPreferencesKey("wallpaper")
        val TILE_STYLE = stringPreferencesKey("tile_style")
        val DRAWER_BG = stringPreferencesKey("drawer_bg")
        val TEXT_TONE = stringPreferencesKey("text_tone")
        val TEXT_FONT = stringPreferencesKey("text_font")
        val NOTIF_DOTS = booleanPreferencesKey("notification_dots")
        val NOW_PLAYING = booleanPreferencesKey("now_playing")
        val DOT_APPS = stringSetPreferencesKey("dot_apps")
        val PLAYER_STYLE = stringPreferencesKey("player_style")
        val SPOTIFY_ONLY = booleanPreferencesKey("spotify_only")
        val PLAYER_CARD = booleanPreferencesKey("player_card")
        val KEEP_PLAYER = booleanPreferencesKey("keep_player")
        val HOME_ORDER = stringPreferencesKey("home_order")
        val PHONE_WALLPAPER = booleanPreferencesKey("phone_wallpaper")
        val SWIPE_DOWN = booleanPreferencesKey("swipe_down_notifications")
        val SMART_SEARCH = booleanPreferencesKey("smart_search")
        val ICON_STYLE = stringPreferencesKey("icon_style")
        val CLOCK_STYLE = stringPreferencesKey("clock_style")
        val HOME_APPS = intPreferencesKey("home_apps")
        val COMPACT = booleanPreferencesKey("compact")
        val FOLDERS_ON = booleanPreferencesKey("folders_on")
        val FOLDERS = stringPreferencesKey("folders")
        val HOME_PROMPT_DISMISSED = booleanPreferencesKey("home_prompt_dismissed")
        val WELCOME_SEEN = booleanPreferencesKey("welcome_seen")

        // What a backup holds, by type.
        val BOOL_KEYS = listOf(QUICK_PAGE, NOTIF_DOTS, NOW_PLAYING, SPOTIFY_ONLY, PLAYER_CARD, KEEP_PLAYER, PHONE_WALLPAPER, SWIPE_DOWN, SMART_SEARCH, COMPACT, FOLDERS_ON)
        val INT_KEYS = listOf(ACCENT, HOME_APPS)
        val STRING_KEYS = listOf(
            FAVORITES, DOCK, QUICK, ICON_SHAPE, CLOCK, WALLPAPER, TILE_STYLE, DRAWER_BG, TEXT_TONE, TEXT_FONT,
            PLAYER_STYLE, HOME_ORDER, ICON_STYLE, CLOCK_STYLE, FOLDERS,
        )
        val SET_KEYS = listOf(HIDDEN, DOT_APPS)
    }
}
