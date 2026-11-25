package dev.rahmanfaizur.dotline.data

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRouter2
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Where music is playing (for the speaker chip) and a way to open Android's own speaker picker.
 * Reads only device names Android already shows in the volume panel; no permission needed.
 */
class AudioOutput(context: Context) {
    private val app = context.applicationContext
    private val audio = app.getSystemService(AudioManager::class.java)

    private val external = setOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER,
    )

    /** The headphones' own name, "Headphones" or "This phone". */
    fun currentName(): String {
        val d = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull { it.type in external }
        return when {
            d == null -> "This phone"
            d.productName.isNullOrBlank() -> "Headphones"
            else -> d.productName.toString()
        }
    }

    fun names(): Flow<String> = callbackFlow {
        val cb = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) { trySend(currentName()) }
            override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) { trySend(currentName()) }
        }
        trySend(currentName())
        audio.registerAudioDeviceCallback(cb, null)
        awaitClose { audio.unregisterAudioDeviceCallback(cb) }
    }.distinctUntilChanged()

    /** Android's speaker picker for [packageName]; falls back to Bluetooth settings. */
    fun openPicker(packageName: String) {
        if (Build.VERSION.SDK_INT >= 34 && runCatching { MediaRouter2.getInstance(app).showSystemOutputSwitcher() }.getOrDefault(false)) return
        // Android 11–13: System UI's own output dialog (Realme and most phones keep it).
        val dialog = Intent("com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG")
            .setPackage("com.android.systemui")
            .putExtra("package_name", packageName)
        if (runCatching { app.sendBroadcast(dialog) }.isSuccess && Build.VERSION.SDK_INT >= 30) return
        runCatching { app.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
