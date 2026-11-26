package dev.rahmanfaizur.dotline.domain

import dev.rahmanfaizur.dotline.data.LauncherSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BugReportTest {
    private val settings = LauncherSettings(
        favorites = listOf("com.secret.bank/.Main"),
        dock = listOf("com.private.chat/.Main"),
        hidden = setOf("com.hidden.app/.Main"),
        folders = mapOf("f1" to Folder("f1", "My private folder", listOf("com.other.app/.Main"))),
    )

    @Test fun includesWhatHappenedAndThePhone() {
        val r = BugReport.build("Clock froze", "1.0", "realme RMX3092", "12 (31)", settings)
        assertTrue("Clock froze" in r)
        assertTrue("realme RMX3092" in r && "Android 12" in r && "Dotline 1.0" in r)
        assertTrue(BugReport.ISSUES in r)
    }

    @Test fun neverIncludesAppsOrFolderNames() {
        val r = BugReport.build("", "1.0", "phone", "12", settings)
        listOf("secret", "private", "hidden", "other.app", "My private folder").forEach { assertFalse(it, it in r) }
        assertTrue("(not described)" in r)
    }
}
