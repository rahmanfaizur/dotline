package dev.rahmanfaizur.dotline.domain

import java.text.Normalizer

/**
 * Pure logic for the all-apps page: A–Z sections and search ranking.
 * No Android types here, so it is fast to unit-test.
 */
object AppIndex {

    /** Section used for anything that doesn't start with a latin letter. */
    const val OTHER = '#'

    private val marks = Regex("\\p{Mn}+")
    private val wordSplit = Regex("[^a-z0-9]+")

    /** Lowercase, accents stripped ("Éclair" → "eclair"). */
    fun normalize(text: String): String =
        marks.replace(Normalizer.normalize(text.trim(), Normalizer.Form.NFD), "").lowercase()

    /** "WhatsApp" → 'W', "éclair" → 'E', "1Password" → '#'. */
    fun sectionOf(label: String): Char {
        val c = normalize(label).firstOrNull()?.uppercaseChar() ?: return OTHER
        return if (c in 'A'..'Z') c else OTHER
    }

    data class Section<T>(val letter: Char, val items: List<T>)

    /** Groups items (already sorted by label) into A–Z sections; '#' goes last. */
    fun <T> sections(items: List<T>, label: (T) -> String): List<Section<T>> =
        items.groupBy { sectionOf(label(it)) }
            .toSortedMap(compareBy { if (it == OTHER) Char.MAX_VALUE else it })
            .map { (letter, group) -> Section(letter, group) }

    /**
     * How well [label] matches [query]. 0 means no match; higher is better.
     *  - whole name, then start of name (shorter names first)
     *  - start of any word ("kite" → "Yellow Kite")
     *  - initials ("ps" → "Play Store")
     *  - anywhere in the name, then letters in order ("wtsp" → "WhatsApp")
     */
    fun score(label: String, query: String): Int {
        val q = normalize(query)
        if (q.isEmpty()) return 0
        val l = normalize(label)
        if (l == q) return 1000
        if (l.startsWith(q)) return 900 - (l.length - q.length).coerceAtMost(99)

        val words = l.split(wordSplit).filter { it.isNotEmpty() }
        val wordIndex = words.indexOfFirst { it.startsWith(q) }
        if (wordIndex >= 0) return 700 - (wordIndex * 10).coerceAtMost(99)

        if (q.length >= 2 && words.size >= 2 &&
            words.joinToString("") { it.take(1) }.startsWith(q)
        ) return 600

        val at = l.indexOf(q)
        if (at >= 0) return 400 - at.coerceAtMost(99)

        return if (isSubsequence(q, l)) 100 else 0
    }

    /** Items matching [query], best first; ties broken alphabetically. */
    fun <T> search(items: List<T>, query: String, label: (T) -> String): List<T> =
        items.asSequence()
            .map { it to score(label(it), query) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<T, Int>> { it.second }.thenBy { normalize(label(it.first)) })
            .map { it.first }
            .toList()

    private fun isSubsequence(needle: String, hay: String): Boolean {
        var i = 0
        for (c in hay) if (i < needle.length && c == needle[i]) i++
        return i == needle.length
    }
}
