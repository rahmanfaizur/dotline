package dev.rahmanfaizur.dotline

import android.app.Application
import dev.rahmanfaizur.dotline.data.AppRepository
import dev.rahmanfaizur.dotline.data.IconLoader
import dev.rahmanfaizur.dotline.data.SettingsStore
import dev.rahmanfaizur.dotline.data.Shortcuts
import dev.rahmanfaizur.dotline.data.Features
import dev.rahmanfaizur.dotline.data.AudioOutput
import dev.rahmanfaizur.dotline.data.SystemInfo
import dev.rahmanfaizur.dotline.data.Wallpapers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import dev.rahmanfaizur.dotline.notify.DotlineNotifications
import dev.rahmanfaizur.dotline.notify.NotificationHub
import dev.rahmanfaizur.dotline.notify.NowPlaying
import dev.rahmanfaizur.dotline.notify.PlayerCard
import android.app.PendingIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

private const val SPOTIFY = "com.spotify.music"

/** Holds the long-lived pieces so they survive the activity being recreated. */
@OptIn(ExperimentalCoroutinesApi::class)
class DotlineApp : Application() {
    private fun cardFor(np: NowPlaying, output: String): PlayerCard.Content {
        val label = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(np.packageName, 0)).toString()
        }.getOrDefault("Music")
        val open = np.controller?.sessionActivity ?: packageManager.getLaunchIntentForPackage(np.packageName)?.let {
            PendingIntent.getActivity(this, 8, it, PendingIntent.FLAG_IMMUTABLE)
        }
        return PlayerCard.Content(np.packageName, label, np.title, np.artist, np.art, np.playing, output, open)
    }

    val appScope = MainScope()

    lateinit var apps: AppRepository
        private set
    lateinit var icons: IconLoader
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var system: SystemInfo
        private set
    lateinit var wallpapers: Wallpapers
        private set
    lateinit var shortcuts: Shortcuts
        private set
    lateinit var features: Features
        private set
    val notifications = NotificationHub()
    lateinit var audio: AudioOutput
        private set
    lateinit var playerCard: PlayerCard
        private set

    override fun onCreate() {
        super.onCreate()
        apps = AppRepository(this, appScope).also { it.start() }
        icons = IconLoader(this)
        settings = SettingsStore(this)
        system = SystemInfo(this)
        wallpapers = Wallpapers(this)
        shortcuts = Shortcuts(this)
        features = Features(this)
        audio = AudioOutput(this)
        playerCard = PlayerCard(this)
        // The listener only works while Dots or Now playing is switched on.
        appScope.launch {
            settings.settings.map { it.notificationDots || it.nowPlaying }.distinctUntilChanged().collect { on ->
                notifications.wanted = on
                if (on) DotlineNotifications.instance?.refresh() else notifications.clear()
            }
        }
        // The optional player card: follows the song while switched on, gone the moment it's off.
        appScope.launch {
            settings.settings
                .map { if (it.nowPlaying && it.playerCard) it.spotifyOnly else null }
                .distinctUntilChanged()
                .flatMapLatest { spotifyOnly ->
                    if (spotifyOnly == null) flowOf(null)
                    else combine(notifications.nowPlaying, audio.names()) { np, output ->
                        np?.takeIf { !spotifyOnly || it.packageName == SPOTIFY }?.let { cardFor(it, output) }
                    }
                }
                .distinctUntilChanged()
                .collectLatest { card ->
                    if (card == null) playerCard.hide()
                    else withContext(Dispatchers.Default) { playerCard.show(card) }
                }
        }
    }
}
