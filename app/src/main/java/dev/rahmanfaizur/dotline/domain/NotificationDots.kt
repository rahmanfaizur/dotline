package dev.rahmanfaizur.dotline.domain

/** Which app icons get a notification dot. Pure logic, so it's easy to test. */
object NotificationDots {
    /** Apps that get dots until the user picks their own (only those installed are used). */
    val DEFAULT_APPS = listOf(
        "com.spotify.music", "com.discord", "com.pinterest", "com.whatsapp", "com.instagram.android",
    )

    /** The apps the user wants dots for: their own pick, or the defaults that are installed. */
    fun chosen(picked: Set<String>?, installed: Set<String>): Set<String> =
        picked ?: DEFAULT_APPS.filter { it in installed }.toSet()

    /** Dots to draw: only when switched on, only for chosen apps that have a notification right now. */
    fun visible(enabled: Boolean, active: Set<String>, chosen: Set<String>): Set<String> =
        if (!enabled) emptySet() else active intersect chosen
}
