package dev.rahmanfaizur.dotline.data

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent

/**
 * Media buttons, like a headset's, for a music app that's closed: Android hands them to the app that
 * played last, which wakes up and carries on with its last song.
 */
object MediaKeys {
    fun play(context: Context, packageName: String) = press(context, packageName, KeyEvent.KEYCODE_MEDIA_PLAY)
    fun next(context: Context, packageName: String) = press(context, packageName, KeyEvent.KEYCODE_MEDIA_NEXT)
    fun previous(context: Context, packageName: String) = press(context, packageName, KeyEvent.KEYCODE_MEDIA_PREVIOUS)

    private fun press(context: Context, packageName: String, code: Int) {
        val t = SystemClock.uptimeMillis()
        val down = KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0)
        val up = KeyEvent(t, t, KeyEvent.ACTION_UP, code, 0)
        // To whichever app Android routes media buttons to (the last one that played)...
        runCatching {
            val audio = context.getSystemService(AudioManager::class.java)
            audio.dispatchMediaKeyEvent(down)
            audio.dispatchMediaKeyEvent(up)
        }
        // ...and, for Play only (twice is harmless; two Nexts would skip two songs), straight to the
        // app's own media button receiver, in case Android has forgotten which app played last.
        if (code == KeyEvent.KEYCODE_MEDIA_PLAY) for (e in listOf(down, up)) runCatching {
            context.sendBroadcast(Intent(Intent.ACTION_MEDIA_BUTTON).setPackage(packageName).putExtra(Intent.EXTRA_KEY_EVENT, e))
        }
    }
}
