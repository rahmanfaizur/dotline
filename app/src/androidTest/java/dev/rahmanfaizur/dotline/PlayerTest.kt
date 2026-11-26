package dev.rahmanfaizur.dotline

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.rahmanfaizur.dotline.data.PlayerStyle
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Plays a made-up song through a real media session and checks every player style. */
@RunWith(AndroidJUnit4::class)
class PlayerTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val instr = InstrumentationRegistry.getInstrumentation()
    private val app get() = instr.targetContext.applicationContext as DotlineApp
    private val listener get() = "${app.packageName}/${app.packageName}.notify.DotlineNotifications"
    private lateinit var session: MediaSession
    @Volatile private var paused = false

    private fun shell(cmd: String) = instr.uiAutomation.executeShellCommand(cmd).close()

    private fun cover(): Bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888).also {
        val p = Paint().apply { shader = LinearGradient(0f, 0f, 300f, 300f, 0xFF2B1B4A.toInt(), 0xFFF0A35E.toInt(), Shader.TileMode.CLAMP) }
        Canvas(it).drawRect(0f, 0f, 300f, 300f, p)
        Canvas(it).drawCircle(186f, 140f, 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xE6FFF0C8.toInt() })
    }

    private fun playing(state: Int) = PlaybackState.Builder()
        .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SEEK_TO)
        .setState(state, 71_000, 1f, SystemClock.elapsedRealtime())
        .build()

    @Before fun setUp() {
        instr.runOnMainSync {
            session = MediaSession(app, "dotline-test").apply {
                setMetadata(
                    MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, "Night Drive")
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, "Neon Rivers")
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, 214_000)
                        .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, cover())
                        .build(),
                )
                setCallback(object : MediaSession.Callback() {
                    override fun onPause() { paused = true; setPlaybackState(playing(PlaybackState.STATE_PAUSED)) }
                    override fun onPlay() { setPlaybackState(playing(PlaybackState.STATE_PLAYING)) }
                })
                setPlaybackState(playing(PlaybackState.STATE_PLAYING))
                isActive = true
            }
        }
        runBlocking {
            app.settings.setNowPlaying(true)
            app.settings.setSpotifyOnly(false) // the test session isn't Spotify
            app.settings.setHomePromptDismissed(true)
            app.settings.setWelcomeSeen(true)
        }
        shell("cmd notification allow_listener $listener")
    }

    @After fun tearDown() {
        instr.runOnMainSync { session.release() }
        runBlocking {
            app.settings.setNowPlaying(false)
            app.settings.setSpotifyOnly(true)
            app.settings.setPlayerStyle(PlayerStyle.Glow)
            app.settings.setHomePromptDismissed(false)
        }
        shell("cmd notification disallow_listener $listener")
    }

    @Test fun everyStyleShowsTheSongAndPlayPauseWorks() {
        // Optional pause per style, used only to take README screenshots (-e holdMs 4000).
        val hold = InstrumentationRegistry.getArguments().getString("holdMs")?.toLongOrNull() ?: 0L
        for (style in PlayerStyle.entries) {
            runBlocking { app.settings.setPlayerStyle(style) }
            rule.waitUntil(10_000) { rule.onAllNodesWithTag("nowPlaying").fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithTag("nowPlaying").assertIsDisplayed()
            rule.onNodeWithTag("outputChip", useUnmergedTree = true).assertIsDisplayed()
            if (hold > 0) Thread.sleep(hold)
        }
        rule.onNodeWithTag("playPause", useUnmergedTree = true).performClick()
        val end = System.currentTimeMillis() + 5_000
        while (!paused && System.currentTimeMillis() < end) Thread.sleep(50)
        assertTrue("pause reached the music app", paused)
    }

    private fun cardShown(): Boolean =
        app.getSystemService(android.app.NotificationManager::class.java).activeNotifications.any { it.id == dev.rahmanfaizur.dotline.notify.PlayerCard.ID }

    private fun waitFor(ms: Long, check: () -> Boolean): Boolean {
        val end = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < end) { if (check()) return true; Thread.sleep(100) }
        return check()
    }

    @Test fun playerCardShowsInThePanelAndItsButtonsWork() {
        shell("pm grant ${app.packageName} android.permission.POST_NOTIFICATIONS")
        try {
            runBlocking { app.settings.setPlayerCard(true) }
            assertTrue("card posted", waitFor(10_000) { cardShown() })
            // Optional pause for a screenshot of the panel (-e holdMs 8000).
            InstrumentationRegistry.getArguments().getString("holdMs")?.toLongOrNull()?.let { Thread.sleep(it) }
            app.sendBroadcast(
                android.content.Intent(app, dev.rahmanfaizur.dotline.notify.PlayerCardReceiver::class.java)
                    .setAction(dev.rahmanfaizur.dotline.notify.PlayerCard.ACTION_PLAY_PAUSE),
            )
            assertTrue("pause from the card reached the music app", waitFor(5_000) { paused })
        } finally {
            runBlocking { app.settings.setPlayerCard(false) }
        }
        assertTrue("card gone when switched off", waitFor(5_000) { !cardShown() })
    }
}
