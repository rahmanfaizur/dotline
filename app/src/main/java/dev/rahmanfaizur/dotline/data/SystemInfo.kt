package dev.rahmanfaizur.dotline.data

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.app.ActivityManager
import android.os.StatFs
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

data class BatteryState(val percent: Int, val charging: Boolean, val healthGood: Boolean?)

data class StorageState(val freeBytes: Long, val totalBytes: Long)

/** RAM in use right now (what Android counts as not available) and the phone's total. */
data class MemoryState(val usedBytes: Long, val totalBytes: Long)

/**
 * Battery, next alarm and storage, read from public Android APIs.
 * None of these need a permission.
 */
class SystemInfo(private val context: Context) {

    fun battery(): Flow<BatteryState> = broadcasts(Intent.ACTION_BATTERY_CHANGED) { it?.toBattery() }

    /** Trigger time (epoch ms) of the next alarm-clock alarm, or null if none is set. */
    fun nextAlarm(): Flow<Long?> = broadcasts(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED) {
        context.getSystemService(AlarmManager::class.java).nextAlarmClock?.triggerTime
    }

    /** Cheap to call: one system query, no permission needed. */
    fun memory(): MemoryState {
        val info = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java).getMemoryInfo(info)
        return MemoryState(info.totalMem - info.availMem, info.totalMem)
    }

    fun storage(): StorageState {
        val stat = StatFs(Environment.getDataDirectory().path)
        return StorageState(stat.availableBytes, stat.totalBytes)
    }

    /** Emits once right away, then on every matching broadcast, until collection stops. */
    private fun <T> broadcasts(action: String, read: (Intent?) -> T?): Flow<T> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) { read(intent)?.let { trySend(it) } }
        }
        val sticky = ContextCompat.registerReceiver(
            context, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        read(sticky)?.let { trySend(it) }
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged()

    private fun Intent.toBattery(): BatteryState {
        val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val health = getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        return BatteryState(
            percent = if (level < 0) 0 else level * 100 / scale,
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
            healthGood = when (health) {
                BatteryManager.BATTERY_HEALTH_UNKNOWN -> null
                BatteryManager.BATTERY_HEALTH_GOOD -> true
                else -> false
            },
        )
    }
}
