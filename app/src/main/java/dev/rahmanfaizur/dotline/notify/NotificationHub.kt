package dev.rahmanfaizur.dotline.notify

import android.graphics.Bitmap
import android.media.session.MediaController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** What's playing, for the home tile. [controller] drives play/pause/skip. */
class NowPlaying(
    val packageName: String,
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val playing: Boolean,
    val controller: MediaController,
)

/**
 * The only things Dotline keeps from notifications: which apps have one right now, and the
 * track that's playing. Never the text of a notification, and nothing is written to disk.
 */
class NotificationHub {
    internal val _active = MutableStateFlow<Set<String>>(emptySet())
    val active: StateFlow<Set<String>> = _active

    internal val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying

    /** Set by the app from Settings; when false the listener ignores everything. */
    @Volatile var wanted: Boolean = false

    fun clear() {
        _active.value = emptySet()
        _nowPlaying.value = null
    }
}
