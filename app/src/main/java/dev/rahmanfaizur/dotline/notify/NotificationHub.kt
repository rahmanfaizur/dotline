package dev.rahmanfaizur.dotline.notify

import android.graphics.Bitmap
import android.media.session.MediaController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** What's playing, for the home tile. [controller] drives play/pause/skip/seek. */
class NowPlaying(
    val packageName: String,
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val playing: Boolean,
    /** Null only for the sample song in Settings' preview. */
    val controller: MediaController?,
    /** Song length in ms (0 if the player doesn't say). */
    val durationMs: Long = 0,
    /** Position in ms at [positionAtMs] (elapsedRealtime), advancing at [speed] while playing. */
    val positionMs: Long = 0,
    val positionAtMs: Long = 0,
    val speed: Float = 1f,
    /** The player's own "like" button, if it offers one (Spotify does). */
    val likeAction: String? = null,
    val liked: Boolean = false,
) {
    fun positionNow(nowElapsed: Long): Long {
        if (!playing || positionAtMs == 0L) return positionMs
        val p = positionMs + ((nowElapsed - positionAtMs) * speed).toLong()
        return if (durationMs > 0) p.coerceIn(0, durationMs) else p.coerceAtLeast(0)
    }
}

/**
 * The only things Dotline keeps from notifications: which apps have one right now, and the
 * track that's playing. Never the text of a notification, and nothing is written to disk.
 */
class NotificationHub {
    internal val _active = MutableStateFlow<Set<String>>(emptySet())
    val active: StateFlow<Set<String>> = _active

    internal val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying

    internal val _connected = MutableStateFlow(false)
    /** True while Android has Dotline's listener bound, i.e. the user has allowed access. */
    val connected: StateFlow<Boolean> = _connected

    /** Set by the app from Settings; when false the listener ignores everything. */
    @Volatile var wanted: Boolean = false

    fun clear() {
        _active.value = emptySet()
        _nowPlaying.value = null
    }
}
