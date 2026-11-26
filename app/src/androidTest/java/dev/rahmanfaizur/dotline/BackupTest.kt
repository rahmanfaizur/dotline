package dev.rahmanfaizur.dotline

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.rahmanfaizur.dotline.data.ClockStyle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Save the setup to text, change things, load it back. */
@RunWith(AndroidJUnit4::class)
class BackupTest {
    private val store get() = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as DotlineApp).settings

    @Test fun saveChangeAndLoadBack() = runBlocking<Unit> {
        store.setAccent(3)
        store.setClockStyle(ClockStyle.Stacked)
        store.setHidden(setOf("a/b"))
        val backup = store.exportBackup()
        try {
            store.setAccent(0)
            store.setClockStyle(ClockStyle.Small)
            store.setHidden(emptySet())
            assertTrue(store.importBackup(backup))
            val s = store.settings.first()
            assertEquals(3, s.accent)
            assertEquals(ClockStyle.Stacked, s.clockStyle)
            assertEquals(setOf("a/b"), s.hidden)
        } finally {
            store.setAccent(0)
            store.setClockStyle(ClockStyle.Big)
            store.setHidden(emptySet())
        }
    }

    @Test fun refusesFilesThatArentBackups() = runBlocking<Unit> {
        assertFalse(store.importBackup("hello"))
        assertFalse(store.importBackup("""{"app":"other","settings":{}}"""))
        // Wrong types are skipped, not crashed on.
        assertTrue(store.importBackup("""{"app":"dotline","version":1,"settings":{"accent":"red","compact":true}}"""))
        assertEquals(0, store.settings.first().accent)
        store.setCompact(false)
    }
}
