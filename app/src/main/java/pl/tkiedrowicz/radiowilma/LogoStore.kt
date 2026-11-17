// Radio Wilma Android - author: Sir Williams, license GNU GPL v3.
// The code was written with the help of an AI assistant (Claude, Anthropic).
package pl.tkiedrowicz.radiowilma

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import java.io.File

/** Station logos chosen by the user: small square PNG files in the app's private folder (one per station id). */
class LogoStore(private val context: Context) {
    private val dir = File(context.filesDir, "logos").apply { mkdirs() }

    private fun prefix(id: String): String = id.filter { it.isLetterOrDigit() }.take(48).ifEmpty { "x" } + "_"

    /** The newest logo file of the station, or null. The file name carries a timestamp, so a new logo is a new URI. */
    fun file(id: String): File? =
        dir.listFiles()
            ?.filter { it.name.startsWith(prefix(id)) && it.name.endsWith(".png") }
            ?.maxByOrNull { it.name }

    fun bitmap(id: String): Bitmap? = file(id)?.let { BitmapFactory.decodeFile(it.path) }

    /** Reads the picked image, fits it into a transparent SIZE x SIZE square (nothing is cropped) and stores it. */
    fun save(id: String, uri: Uri): Boolean {
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleFor(bounds.outWidth, bounds.outHeight) }
            val src = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return false
            store(id, src)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Same as [save], for an image downloaded from the internet (PNG/JPG/WEBP/GIF/ICO bytes). */
    fun saveBytes(id: String, data: ByteArray): Boolean {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
            if (bounds.outWidth < 16 || bounds.outHeight < 16) return false
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleFor(bounds.outWidth, bounds.outHeight) }
            val src = BitmapFactory.decodeByteArray(data, 0, data.size, opts) ?: return false
            store(id, src)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun sampleFor(w: Int, h: Int): Int {
        var sample = 1
        while (maxOf(w, h) / (sample * 2) >= SIZE) sample *= 2
        return sample
    }

    private fun store(id: String, src: Bitmap) {
        val scale = SIZE.toFloat() / maxOf(src.width, src.height)
        val w = maxOf(1, (src.width * scale).toInt())
        val h = maxOf(1, (src.height * scale).toInt())
        val scaled = Bitmap.createScaledBitmap(src, w, h, true)
        val square = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        Canvas(square).drawBitmap(scaled, (SIZE - w) / 2f, (SIZE - h) / 2f, null)
        remove(id)
        File(dir, prefix(id) + System.currentTimeMillis() + ".png").outputStream().use {
            square.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    fun has(id: String): Boolean = file(id) != null

    fun remove(id: String) {
        dir.listFiles()?.filter { it.name.startsWith(prefix(id)) }?.forEach { it.delete() }
    }

    companion object {
        const val SIZE = 256
    }
}
