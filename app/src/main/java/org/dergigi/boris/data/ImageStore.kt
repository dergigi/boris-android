package org.dergigi.boris.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object ImageStore {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    fun filenameFor(url: String, index: Int): String {
        val path = try {
            java.net.URI(url).path.orEmpty()
        } catch (_: Exception) {
            url.substringBefore('?')
        }
        val raw = path.substringAfterLast('/').substringBefore('?')
        val cleaned = raw.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return if (cleaned.contains('.')) cleaned else "image-${index + 1}.jpg"
    }

    fun mimeFor(filename: String, contentType: String? = null): String {
        val fromHeader = contentType?.substringBefore(';')?.trim().orEmpty()
        if (fromHeader.startsWith("image/")) return fromHeader
        return when (filename.substringAfterLast('.', "").lowercase()) {
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            else -> "image/jpeg"
        }
    }

    fun save(context: Context, url: String, index: Int): Uri {
        val fetched = fetch(url, index)
        return writeToPictures(context, fetched)
    }

    fun saveAll(context: Context, urls: List<String>): Int {
        var saved = 0
        urls.forEachIndexed { index, url ->
            runCatching { save(context, url, index) }.onSuccess { saved += 1 }
        }
        return saved
    }

    fun shareIntent(context: Context, url: String, index: Int): Intent {
        val fetched = fetch(url, index)
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, fetched.filename)
        file.writeBytes(fetched.bytes)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = fetched.mime
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = android.content.ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun fetch(url: String, index: Int): FetchedImage {
        val request = Request.Builder().url(UrlExtractor.preferHttps(url)).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to download image (${response.code})")
            }
            val bytes = response.body?.bytes() ?: throw IOException("Empty image")
            val filename = filenameFor(url, index)
            return FetchedImage(
                bytes = bytes,
                filename = filename,
                mime = mimeFor(filename, response.header("Content-Type")),
            )
        }
    }

    private fun writeToPictures(context: Context, image: FetchedImage): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, image.filename)
            put(MediaStore.Images.Media.MIME_TYPE, image.mime)
            if (Build.VERSION.SDK_INT >= 29) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/Boris",
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values,
        ) ?: throw IOException("Could not create image in Pictures")
        try {
            context.contentResolver.openOutputStream(uri)?.use { it.write(image.bytes) }
                ?: throw IOException("Could not write image")
            if (Build.VERSION.SDK_INT >= 29) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
            return uri
        } catch (e: Exception) {
            context.contentResolver.delete(uri, null, null)
            throw e
        }
    }

    private data class FetchedImage(
        val bytes: ByteArray,
        val filename: String,
        val mime: String,
    )
}
