package dev.rahmanfaizur.dotline.notify

import android.app.Notification
import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dev.rahmanfaizur.dotline.DotlineApp

/**
 * Android's notification access, used for two things only: dots on chosen app icons and the
 * Now playing tile. It works only after you allow it in Android's settings, does nothing while
 * both are switched off in Dotline, and keeps no notification text.
 */
class DotlineNotifications : NotificationListenerService() {
    private val hub get() = (application as DotlineApp).notifications
    private var sessions: MediaSessionManager? = null
    private var watched: MediaController? = null

    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { list -> pickSession(list) }
    private val mediaCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publishMedia()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publishMedia()
        override fun onSessionDestroyed() = pickSession(activeSessions())
    }

    override fun onListenerConnected() {
        instance = this
        sessions = getSystemService(MediaSessionManager::class.java)
        runCatching { sessions?.addOnActiveSessionsChangedListener(sessionsChanged, component()) }
        refresh()
    }

    override fun onListenerDisconnected() {
        instance = null
        runCatching { sessions?.removeOnActiveSessionsChangedListener(sessionsChanged) }
        watched?.unregisterCallback(mediaCallback)
        watched = null
        hub.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = refreshDots()
    override fun onNotificationRemoved(sbn: StatusBarNotification) = refreshDots()

    /** Re-reads everything; called when connected and when Dotline's switches change. */
    fun refresh() {
        refreshDots()
        pickSession(activeSessions())
    }

    private fun component() = ComponentName(this, DotlineNotifications::class.java)

    private fun activeSessions(): List<MediaController> =
        runCatching { sessions?.getActiveSessions(component()) }.getOrNull().orEmpty()

    private fun refreshDots() {
        if (!hub.wanted) { hub._active.value = emptySet(); return }
        val list = runCatching { activeNotifications }.getOrNull() ?: return
        // Ongoing ones (music playing, downloads) aren't news, so they don't earn a dot.
        hub._active.value = list
            .filter { it.packageName != packageName && (it.notification.flags and Notification.FLAG_ONGOING_EVENT) == 0 }
            .mapTo(HashSet()) { it.packageName }
    }

    private fun pickSession(list: List<MediaController>?) {
        val pick = list.orEmpty().firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: list.orEmpty().firstOrNull()
        if (pick?.sessionToken != watched?.sessionToken) {
            watched?.unregisterCallback(mediaCallback)
            watched = pick
            pick?.registerCallback(mediaCallback)
        }
        publishMedia()
    }

    private fun publishMedia() {
        val c = watched
        val meta = c?.metadata
        if (!hub.wanted || c == null || meta == null) { hub._nowPlaying.value = null; return }
        hub._nowPlaying.value = NowPlaying(
            packageName = c.packageName,
            title = meta.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
            artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty(),
            art = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART),
            playing = c.playbackState?.state == PlaybackState.STATE_PLAYING,
            controller = c,
        )
    }

    companion object {
        /** The running listener, if Android has bound it (only after the user allowed access). */
        @Volatile var instance: DotlineNotifications? = null
    }
}
