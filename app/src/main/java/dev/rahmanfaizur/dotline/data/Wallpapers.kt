package dev.rahmanfaizur.dotline.data

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A rendered wallpaper, its small blurred copy for glass tiles, and how bright the
 * upper part (where the tiles sit) is, from 0 (black) to 1 (white).
 */
class WallpaperImage(val full: Bitmap, val frosted: Bitmap, val brightness: Float)

/**
 * The user's picked photo (a private copy inside the app) and the rendered wallpapers.
 * The photo comes from Android's photo picker, so Dotline only ever sees that one image.
 */
class Wallpapers(context: Context) {
    private val app = context.applicationContext
    private val photoFile = File(app.filesDir, "wallpaper/photo.jpg")

    private val _photoVersion = MutableStateFlow(if (photoFile.exists()) photoFile.lastModified() else 0L)
    /** Changes whenever a new photo is saved, so the wallpaper re-renders. */
    val photoVersion: StateFlow<Long> = _photoVersion

    val hasPhoto: Boolean get() = photoFile.exists()

    /** Copies the picked photo in, shrunk to at most 2400 px on its long side. */
    suspend fun savePhoto(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val bmp = ImageDecoder.decodeBitmap(ImageDecoder.createSource(app.contentResolver, uri)) { decoder, info, _ ->
                val long = max(info.size.width, info.size.height)
                if (long > MAX_SIDE) {
                    val s = MAX_SIDE.toFloat() / long
                    decoder.setTargetSize((info.size.width * s).roundToInt(), (info.size.height * s).roundToInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            photoFile.parentFile?.mkdirs()
            val tmp = File(photoFile.parentFile, "photo.tmp")
            tmp.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            bmp.recycle()
            check(tmp.renameTo(photoFile))
            _photoVersion.value = photoFile.lastModified().coerceAtLeast(_photoVersion.value + 1)
        }.isSuccess
    }

    private fun loadPhoto(): Bitmap? =
        if (photoFile.exists()) BitmapFactory.decodeFile(photoFile.path) else null

    /** Renders the home wallpaper for a [w]×[h] screen, or null for plain black. */
    suspend fun render(kind: WallpaperKind, accent: Int, w: Int, h: Int): WallpaperImage? = withContext(Dispatchers.Default) {
        val photo = if (kind == WallpaperKind.Photo || kind == WallpaperKind.Halftone) loadPhoto() else null
        val full = WallpaperArt.render(kind, w, h, accent, lock = false, photo = photo)
        photo?.recycle()
        full?.let {
            val frosted = WallpaperArt.frosted(it)
            WallpaperImage(it, frosted, WallpaperArt.brightness(frosted))
        }
    }

    /** A small preview for the settings screen. */
    suspend fun thumbnail(kind: WallpaperKind, accent: Int, w: Int, h: Int, lock: Boolean = false): Bitmap? =
        withContext(Dispatchers.Default) {
            val photo = if (kind == WallpaperKind.Photo || kind == WallpaperKind.Halftone) loadPhoto() else null
            WallpaperArt.render(kind, w, h, accent, lock, photo).also { photo?.recycle() }
        }

    /**
     * Saves the lock-screen version of [kind] and returns an intent that opens the phone's own
     * "set wallpaper" screen with it, where the user picks Lock screen / Home / Both.
     * Some phones (Realme/ColorOS) only show lock wallpapers set that way. Null if the phone has no such screen.
     */
    suspend fun lockScreenIntent(kind: WallpaperKind, accent: Int, w: Int, h: Int): Intent? = withContext(Dispatchers.Default) {
        runCatching {
            val photo = if (kind == WallpaperKind.Photo || kind == WallpaperKind.Halftone) loadPhoto() else null
            val bmp = WallpaperArt.render(kind, w, h, accent, lock = true, photo = photo)
                ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.BLACK) }
            photo?.recycle()
            val dir = File(app.cacheDir, "share").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() } // keep only the newest
            val file = File(dir, "dotline-lock-${System.currentTimeMillis()}.png")
            file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
            val uri = FileProvider.getUriForFile(app, "${app.packageName}.files", file)
            WallpaperManager.getInstance(app).getCropAndSetWallpaperIntent(uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.getOrNull()
    }

    /**
     * Sets the lock-screen wallpaper to the lock variant of [kind] (or plain black).
     * Uses the "set wallpaper" permission, which can only change the picture, never read it.
     */
    suspend fun setLockScreen(kind: WallpaperKind, accent: Int, w: Int, h: Int): Boolean = withContext(Dispatchers.Default) {
        runCatching {
            val photo = if (kind == WallpaperKind.Photo || kind == WallpaperKind.Halftone) loadPhoto() else null
            val bmp = WallpaperArt.render(kind, w, h, accent, lock = true, photo = photo)
                ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.BLACK) }
            photo?.recycle()
            WallpaperManager.getInstance(app).setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK)
            bmp.recycle()
        }.isSuccess
    }

    private companion object {
        const val MAX_SIDE = 2400
    }
}
