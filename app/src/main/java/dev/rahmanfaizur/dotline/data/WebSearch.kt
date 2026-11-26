package dev.rahmanfaizur.dotline.data

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Hands a search to the phone's default browser, which searches with its own engine (Brave Search
 * in Brave, Google in Chrome, and so on). Dotline itself never goes online.
 */
object WebSearch {
    fun open(context: Context, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return false
        val browser = defaultBrowser(context)
        val ownEngine = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, q)
        browser?.let { ownEngine.component = searchScreen(context, it, ownEngine) }
        val google = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(q)))
        // The browser's own search first; if it doesn't take one, a Google page in that browser; then anything that can.
        val tries = if (browser != null) {
            listOf(Intent(ownEngine).setPackage(browser.packageName), Intent(google).setPackage(browser.packageName), google)
        } else {
            listOf(ownEngine, google)
        }
        return tries.any { intent ->
            runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
        }
    }

    /** The default browser's screen for links, or null when no browser is set (Android would then ask which one). */
    private fun defaultBrowser(context: Context): ComponentName? =
        context.packageManager
            .resolveActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")), PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.takeIf { it.packageName != "android" }
            ?.let { ComponentName(it.packageName, it.name) }

    /**
     * Some browsers have more than one screen that takes a search (Brave: the browser and a search
     * widget), so Android would ask which. Prefer the one that also opens links: that's the browser itself.
     */
    private fun searchScreen(context: Context, browser: ComponentName, search: Intent): ComponentName? {
        val screens = context.packageManager
            .queryIntentActivities(Intent(search).setPackage(browser.packageName), PackageManager.MATCH_DEFAULT_ONLY)
            .map { ComponentName(it.activityInfo.packageName, it.activityInfo.name) }
        return screens.firstOrNull { it == browser } ?: screens.firstOrNull()
    }
}
