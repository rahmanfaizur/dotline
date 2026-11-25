package dev.rahmanfaizur.dotline.data

import android.content.Context
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

/** A rendered wallpaper plus its small blurred copy for glass tiles. */
class WallpaperImage(val full: Bitmap, val frosted: Bitmap)

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
        full?.let { WallpaperImage(it, WallpaperArt.frosted(it)) }
    }

    /** A small preview for the settings screen. */
    suspend fun thumbnail(kind: WallpaperKind, accent: Int, w: Int, h: Int, lock: Boolean = false): Bitmap? =
        withContext(Dispatchers.Default) {
            val photo = if (kind == WallpaperKind.Photo || kind == WallpaperKind.Halftone) loadPhoto() else null
            WallpaperArt.render(kind, w, h, accent, lock, photo).also { photo?.recycle() }
        }

    private companion object {
        const val MAX_SIDE = 2400
    }
}
