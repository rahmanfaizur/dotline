package dev.rahmanfaizur.dotline.domain

/** Pure rules for the pinned app lists (home tiles, dock, quick-open). */
object Pins {
    const val MAX = 4

    /** Home tiles out of the box: the user's main apps, then common fallbacks. */
    val FAVORITE_PACKAGES = listOf(
        "com.whatsapp", "com.instagram.android", "com.spotify.music", "com.discord",
        "com.google.android.youtube", "com.google.android.gm", "com.android.chrome",
        "com.google.android.apps.photos", "com.google.android.apps.maps",
    )

    /** Quick page out of the box. */
    val QUICK_PACKAGES = listOf(
        "com.phonepe.app", "com.openai.chatgpt", "com.google.android.gm", "com.google.android.youtube",
        "com.google.android.apps.maps", "com.google.android.apps.photos", "com.google.android.calendar",
        "com.google.android.deskclock",
    )

    /** First [count] of [preferred] that are installed and not in [exclude], in preferred order. */
    fun pick(preferred: List<String>, installed: Set<String>, exclude: Set<String> = emptySet(), count: Int = MAX) =
        preferred.asSequence().filter { it in installed && it !in exclude }.distinct().take(count).toList()

    enum class Outcome { Added, Removed, Full }

    /** Adds [key] if absent (unless full), removes it if present. */
    fun toggle(list: List<String>, key: String, max: Int = MAX): Pair<List<String>, Outcome> = when {
        key in list -> (list - key) to Outcome.Removed
        list.size >= max -> list to Outcome.Full
        else -> (list + key) to Outcome.Added
    }
}
