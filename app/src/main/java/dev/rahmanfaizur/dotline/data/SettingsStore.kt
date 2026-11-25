package dev.rahmanfaizur.dotline.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

enum class IconShape { Round, Square }
enum class ClockMode { System, H24, H12 }

/**
 * Everything the user can change. App lists hold component names ("pkg/.Activity").
 * A null list means "not customised yet": the launcher picks sensible defaults.
 */
data class LauncherSettings(
    val favorites: List<String>? = null,
    val dock: List<String>? = null,
    val quick: List<String>? = null,
    val hidden: Set<String> = emptySet(),
    val accent: Int = 0,
    val iconShape: IconShape = IconShape.Round,
    val clock: ClockMode = ClockMode.System,
    val showQuickPage: Boolean = true,
)

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** Settings saved on the phone only (DataStore file in the app's private folder). */
class SettingsStore(context: Context) {
    private val store = context.applicationContext.settingsStore

    val settings: Flow<LauncherSettings> = store.data
        .catch { emit(emptyPreferences()) }
        .map { it.toSettings() }

    suspend fun setFavorites(list: List<String>) = store.edit { it[FAVORITES] = list.join() }
    suspend fun setDock(list: List<String>) = store.edit { it[DOCK] = list.join() }
    suspend fun setQuick(list: List<String>) = store.edit { it[QUICK] = list.join() }
    suspend fun setHidden(set: Set<String>) = store.edit { it[HIDDEN] = set }
    suspend fun setAccent(index: Int) = store.edit { it[ACCENT] = index }
    suspend fun setIconShape(shape: IconShape) = store.edit { it[ICON_SHAPE] = shape.name }
    suspend fun setClock(mode: ClockMode) = store.edit { it[CLOCK] = mode.name }
    suspend fun setShowQuickPage(show: Boolean) = store.edit { it[QUICK_PAGE] = show }

    private fun Preferences.toSettings() = LauncherSettings(
        favorites = this[FAVORITES]?.split(),
        dock = this[DOCK]?.split(),
        quick = this[QUICK]?.split(),
        hidden = this[HIDDEN] ?: emptySet(),
        accent = this[ACCENT] ?: 0,
        iconShape = this[ICON_SHAPE]?.let { runCatching { IconShape.valueOf(it) }.getOrNull() } ?: IconShape.Round,
        clock = this[CLOCK]?.let { runCatching { ClockMode.valueOf(it) }.getOrNull() } ?: ClockMode.System,
        showQuickPage = this[QUICK_PAGE] ?: true,
    )

    private fun List<String>.join() = joinToString("\n")
    private fun String.split(): List<String> = if (isEmpty()) emptyList() else split("\n")

    private companion object {
        val FAVORITES = stringPreferencesKey("favorites")
        val DOCK = stringPreferencesKey("dock")
        val QUICK = stringPreferencesKey("quick")
        val HIDDEN = stringSetPreferencesKey("hidden")
        val ACCENT = intPreferencesKey("accent")
        val ICON_SHAPE = stringPreferencesKey("icon_shape")
        val CLOCK = stringPreferencesKey("clock")
        val QUICK_PAGE = booleanPreferencesKey("quick_page")
    }
}
