package dev.rahmanfaizur.dotline.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.rahmanfaizur.dotline.DotlineApp
import dev.rahmanfaizur.dotline.R
import dev.rahmanfaizur.dotline.domain.BoxBlur
import kotlin.math.PI
import kotlin.math.sin

/**
 * Dotline's Cover glow player as a card in the pull-down panel and on the lock screen.
 * Off unless switched on in Settings → Notifications. It only shows what's already on the
 * home tile (song, artist, cover) and its buttons drive the same media controls.
 */
class PlayerCard(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    /** What the card shows; equal cards are never re-posted. */
    data class Content(
        val packageName: String,
        val source: String,
        val title: String,
        val artist: String,
        val art: Bitmap?,
        val playing: Boolean,
        val output: String,
        val open: PendingIntent?,
    )

    fun canPost(): Boolean = manager.areNotificationsEnabled()

    fun show(c: Content) {
        if (!canPost()) return
        ensureChannel()
        val glow = glow(c.art)
        val cover = c.art?.let { rounded(it, 128, 26f) }
        val big = RemoteViews(context.packageName, R.layout.player_card).apply {
            fill(c, glow, cover)
            setTextViewText(R.id.source, c.source)
            setTextViewText(R.id.outputName, c.output)
            setImageViewBitmap(R.id.wave, wave())
            setOnClickPendingIntent(R.id.prev, action(ACTION_PREV))
            setOnClickPendingIntent(R.id.next, action(ACTION_NEXT))
            setOnClickPendingIntent(R.id.output, outputIntent(c.packageName))
            c.open?.let { setOnClickPendingIntent(R.id.song, it) }
        }
        val small = RemoteViews(context.packageName, R.layout.player_card_small).apply { fill(c, glow, cover) }
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_player)
            .setContentTitle(c.title.ifBlank { "Playing" })
            .setContentText(c.artist)
            .setCustomContentView(small)
            .setCustomBigContentView(big)
            .setContentIntent(c.open)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(c.playing)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .build()
        runCatching { manager.notify(ID, n) } // Notifications can be switched off at any moment.
    }

    fun hide() = manager.cancel(ID)

    private fun RemoteViews.fill(c: Content, glow: Bitmap, cover: Bitmap?) {
        setImageViewBitmap(R.id.glow, glow)
        if (cover != null) setImageViewBitmap(R.id.cover, cover) else setImageViewResource(R.id.cover, R.drawable.np_chip)
        setTextViewText(R.id.title, c.title.ifBlank { "Playing" })
        setTextViewText(R.id.artist, c.artist)
        setImageViewResource(R.id.playPause, if (c.playing) R.drawable.ic_np_pause else R.drawable.ic_np_play)
        setContentDescription(R.id.playPause, if (c.playing) "Pause" else "Play")
        setOnClickPendingIntent(R.id.playPause, action(ACTION_PLAY_PAUSE))
    }

    private fun ensureChannel() {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Player card", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Dotline's player in the pull-down panel and on the lock screen"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
    }

    private fun action(name: String): PendingIntent = PendingIntent.getBroadcast(
        context, name.hashCode(),
        Intent(context, PlayerCardReceiver::class.java).setAction(name),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun outputIntent(pkg: String): PendingIntent = PendingIntent.getActivity(
        context, 7,
        Intent(context, OutputPickerActivity::class.java).putExtra(OutputPickerActivity.EXTRA_PACKAGE, pkg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** The cover, tiny and blurred, darkened so white text stays readable. Small, so it's cheap to send. */
    private fun glow(art: Bitmap?): Bitmap {
        val w = 96; val h = 48
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        if (art != null) {
            val side = minOf(art.width, art.height)
            val src = android.graphics.Rect((art.width - side) / 2, (art.height - side) / 4, (art.width + side) / 2, (art.height - side) / 4 + side / 2)
            canvas.drawBitmap(art, src, android.graphics.Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
            val px = IntArray(w * h)
            out.getPixels(px, 0, w, 0, 0, w, h)
            BoxBlur.blur(px, w, h, radius = 5)
            out.setPixels(px, 0, w, 0, 0, w, h)
            canvas.drawColor(0x75000000)
        } else {
            canvas.drawColor(0xFF1C1C1C.toInt())
        }
        return out
    }

    private fun rounded(src: Bitmap, size: Int, radius: Float): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val side = minOf(src.width, src.height).toFloat()
        val scale = size / side
        val shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(android.graphics.Matrix().apply {
                postTranslate(-(src.width - side) / 2f, -(src.height - side) / 2f)
                postScale(scale, scale)
            })
        }
        Canvas(out).drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader })
        return out
    }

    /** A soft white ripple, the same line as on the home tile. */
    private fun wave(): Bitmap {
        val w = 300; val h = 28
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; strokeCap = Paint.Cap.ROUND; color = 0xCCFFFFFF.toInt() }
        val path = Path()
        for (x in 6..w - 6) {
            val y = h / 2f + sin(x / 300f * 2 * PI * 6).toFloat() * 5f
            if (x == 6) path.moveTo(x.toFloat(), y) else path.lineTo(x.toFloat(), y)
        }
        Canvas(out).drawPath(path, p)
        return out
    }

    companion object {
        const val CHANNEL = "player_card"
        const val ID = 4201
        const val ACTION_PLAY_PAUSE = "dev.rahmanfaizur.dotline.card.PLAY_PAUSE"
        const val ACTION_PREV = "dev.rahmanfaizur.dotline.card.PREV"
        const val ACTION_NEXT = "dev.rahmanfaizur.dotline.card.NEXT"
    }
}

/** The card's buttons. Not exported: only Dotline's own card can send these. */
class PlayerCardReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val np = (context.applicationContext as DotlineApp).notifications.nowPlaying.value ?: return
        val t = np.controller?.transportControls ?: return
        when (intent.action) {
            PlayerCard.ACTION_PLAY_PAUSE -> if (np.playing) t.pause() else t.play()
            PlayerCard.ACTION_PREV -> t.skipToPrevious()
            PlayerCard.ACTION_NEXT -> t.skipToNext()
        }
    }
}

/** Opens Android's speaker picker from the card, then gets out of the way. */
class OutputPickerActivity : android.app.Activity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: "com.spotify.music"
        (application as DotlineApp).audio.openPicker(pkg)
        finish()
    }

    companion object { const val EXTRA_PACKAGE = "package" }
}
