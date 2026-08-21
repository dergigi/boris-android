package org.dergigi.boris.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import java.io.File
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.roundToInt

object ArticleImages {
    const val SETTINGS_KEY = "offlineDownloadImages"
    const val DIR_NAME = "article_images"
    internal const val MAX_EDGE = 1600
    private const val WEBP_QUALITY = 80

    private val lock = Any()
    private var dir: File? = null

    fun init(target: File) {
        synchronized(lock) {
            dir = target
            target.mkdirs()
        }
    }

    fun enabled(): Boolean =
        SettingsSync.settings.value.offlineDownloadEnabled(SETTINGS_KEY)

    fun fileFor(url: String): File? {
        val root = synchronized(lock) { dir } ?: return null
        val key = cacheKey(url) ?: return null
        return File(root, "$key.webp")
    }

    fun cachedFile(url: String): File? {
        val file = fileFor(url) ?: return null
        if (!file.isFile || file.length() <= 0L) return null
        file.setLastModified(System.currentTimeMillis())
        return file
    }

    fun displaySource(url: String): Any {
        val https = UrlExtractor.preferHttps(url)
        return cachedFile(https) ?: https
    }

    fun shouldConvert(url: String): Boolean {
        val path = UrlExtractor.preferHttps(url).substringBefore('?').lowercase()
        return path.isNotBlank() && !path.endsWith(".gif") && !path.endsWith(".svg")
    }

    fun cacheKey(url: String): String? {
        val canonical = UrlExtractor.preferHttps(url).trim()
        if (canonical.isBlank()) return null
        return sha256Hex(canonical).take(16)
    }

    fun urlsFor(content: ReadableContent): List<String> {
        val out = ArrayList<String>()
        content.imageUrl?.takeIf { it.isNotBlank() }?.let { out.add(UrlExtractor.preferHttps(it)) }
        out.addAll(UrlExtractor.imageUrls(content.body, content.url))
        return out.distinct()
    }

    internal fun urlsToFetch(urls: Collection<String>): List<String> =
        urls.map { UrlExtractor.preferHttps(it) }
            .filter { it.isNotBlank() && needsFetch(it) }
            .distinct()

    internal fun needsFetch(url: String): Boolean {
        if (!shouldConvert(url)) return false
        val file = fileFor(url) ?: return false
        return !file.isFile || file.length() <= 0L
    }

    fun ensure(context: Context, urls: Collection<String>) {
        if (!enabled()) return
        for (url in urlsToFetch(urls)) {
            runCatching { convert(url) }
        }
        trim(context)
    }

    private fun convert(url: String) {
        if (!needsFetch(url)) return
        val dest = fileFor(url) ?: return
        val downloaded = ImageStore.download(url)
        if (skipMime(downloaded.contentType)) return
        val bitmap = decodeScaled(downloaded.bytes) ?: return
        val scaled = scaleToMax(bitmap, MAX_EDGE)
        try {
            dest.parentFile?.mkdirs()
            val tmp = File(dest.parentFile, "${dest.name}.tmp")
            tmp.outputStream().use { out ->
                scaled.compress(webpFormat(), WEBP_QUALITY, out)
            }
            if (!tmp.renameTo(dest)) {
                dest.delete()
                tmp.renameTo(dest)
            }
        } finally {
            if (scaled !== bitmap) bitmap.recycle()
            scaled.recycle()
        }
    }

    private fun decodeScaled(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    internal fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        if (width <= 0 || height <= 0 || maxEdge <= 0) return 1
        var sample = 1
        val longest = max(width, height)
        while (longest / sample > maxEdge * 2) {
            sample *= 2
        }
        return sample
    }

    private fun scaleToMax(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / longest
        val width = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun skipMime(contentType: String?): Boolean {
        val mime = contentType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        return mime == "image/gif" || mime == "image/svg+xml"
    }

    private fun webpFormat(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= 30) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }

    private fun trim(context: Context) {
        val root = synchronized(lock) { dir } ?: return
        val limit = CacheLimit.bytes(context)
        var used = CacheUsage.bytes(context)
        if (used <= limit) return
        val files = root.listFiles()?.filter { it.isFile }?.sortedBy { it.lastModified() }.orEmpty()
        for (file in files) {
            if (used <= limit) break
            val size = file.length()
            if (file.delete()) used -= size
        }
    }

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
