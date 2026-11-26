package dev.rahmanfaizur.dotline.domain

/** A folder on home: its id, name and the apps in it (as app keys). */
data class Folder(val id: String, val name: String, val apps: List<String>) {
    val key: String get() = PREFIX + id

    companion object {
        const val PREFIX = "folder:"
        fun isFolder(key: String) = key.startsWith(PREFIX)
        fun idOf(key: String) = key.removePrefix(PREFIX)

        /** Saved as one line per folder: id, tab, name, tab, comma-separated app keys. */
        fun encode(folders: Collection<Folder>): String =
            folders.joinToString("\n") { f -> listOf(f.id, f.name.replace('\t', ' ').replace('\n', ' '), f.apps.joinToString(",")).joinToString("\t") }

        fun decode(text: String?): Map<String, Folder> = text.orEmpty().lines().mapNotNull { line ->
            val parts = line.split("\t")
            if (parts.size != 3 || parts[0].isBlank()) null
            else Folder(parts[0], parts[1], parts[2].split(",").filter { it.isNotBlank() })
        }.associateBy { it.id }
    }
}

/** The home apps list plus its folders, changed together. */
data class HomeLayout(val favorites: List<String>, val folders: Map<String, Folder>)

/** Pure rules for folders on home. */
object Folders {
    /** Dropping [dropped] onto [target] (both on home) makes a folder where [target] was. */
    fun make(layout: HomeLayout, dropped: String, target: String, id: String, name: String = "Folder"): HomeLayout {
        if (dropped == target || Folder.isFolder(dropped) || Folder.isFolder(target)) return layout
        if (target !in layout.favorites) return layout
        val folder = Folder(id, name, listOf(target, dropped))
        val favs = layout.favorites.map { if (it == target) folder.key else it }.filter { it != dropped }
        return HomeLayout(favs, layout.folders + (id to folder))
    }

    /** Dropping [app] onto a folder adds it (and takes it off home if it was there). */
    fun add(layout: HomeLayout, app: String, folderId: String): HomeLayout {
        val f = layout.folders[folderId] ?: return layout
        if (Folder.isFolder(app) || app in f.apps) return layout
        return HomeLayout(layout.favorites - app, layout.folders + (folderId to f.copy(apps = f.apps + app)))
    }

    /** Takes [app] out of a folder. A folder left with one app turns back into that app. */
    fun remove(layout: HomeLayout, folderId: String, app: String): HomeLayout {
        val f = layout.folders[folderId] ?: return layout
        val left = f.apps - app
        return when (left.size) {
            0 -> HomeLayout(layout.favorites - f.key, layout.folders - folderId)
            1 -> HomeLayout(layout.favorites.map { if (it == f.key) left[0] else it }, layout.folders - folderId)
            else -> HomeLayout(layout.favorites, layout.folders + (folderId to f.copy(apps = left)))
        }
    }

    fun rename(layout: HomeLayout, folderId: String, name: String): HomeLayout {
        val f = layout.folders[folderId] ?: return layout
        val clean = name.trim().take(24).ifBlank { "Folder" }
        return layout.copy(folders = layout.folders + (folderId to f.copy(name = clean)))
    }
}
