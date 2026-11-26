package dev.rahmanfaizur.dotline.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoldersTest {
    private val start = HomeLayout(listOf("a", "b", "c", "d"), emptyMap())

    @Test fun dropOneAppOnAnotherMakesAFolderInItsPlace() {
        val l = Folders.make(start, dropped = "c", target = "b", id = "f1")
        assertEquals(listOf("a", "folder:f1", "d"), l.favorites)
        assertEquals(listOf("b", "c"), l.folders["f1"]!!.apps)
    }

    @Test fun addAndRemove() {
        var l = Folders.make(start, "c", "b", "f1")
        l = Folders.add(l, "d", "f1")
        assertEquals(listOf("a", "folder:f1"), l.favorites)
        assertEquals(listOf("b", "c", "d"), l.folders["f1"]!!.apps)
        l = Folders.remove(l, "f1", "c")
        assertEquals(listOf("b", "d"), l.folders["f1"]!!.apps)
        // One app left: the folder turns back into that app, in the same place.
        l = Folders.remove(l, "f1", "b")
        assertEquals(listOf("a", "d"), l.favorites)
        assertTrue(l.folders.isEmpty())
    }

    @Test fun foldersCantGoInsideFolders() {
        val l = Folders.make(start, "c", "b", "f1")
        assertEquals(l, Folders.make(l, "a", "folder:f1", "f2"))
        assertEquals(l, Folders.add(l, "folder:f1", "f1"))
    }

    @Test fun savesAndLoads() {
        val f = Folder("f1", "Social\tstuff", listOf("x/y", "p/q"))
        val back = Folder.decode(Folder.encode(listOf(f)))
        assertEquals(listOf("x/y", "p/q"), back["f1"]!!.apps)
        assertEquals("Social stuff", back["f1"]!!.name)
        assertTrue(Folder.decode(null).isEmpty())
    }

    @Test fun renameKeepsItShortAndNeverBlank() {
        var l = Folders.make(start, "c", "b", "f1")
        l = Folders.rename(l, "f1", "   ")
        assertEquals("Folder", l.folders["f1"]!!.name)
        l = Folders.rename(l, "f1", "A very very long folder name here")
        assertEquals(24, l.folders["f1"]!!.name.length)
    }
}
