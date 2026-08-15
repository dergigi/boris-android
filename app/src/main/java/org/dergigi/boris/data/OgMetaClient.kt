package org.dergigi.boris.data

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object OgMetaClient {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun fetch(url: String): OgPreview? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml")
            .get()
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val source = response.body?.source() ?: return null
            source.request(PREFIX_BYTES.toLong())
            val n = minOf(source.buffer.size, PREFIX_BYTES.toLong())
            if (n == 0L) return null
            OgMeta.parse(source.buffer.readUtf8(n), url)
        }
    }

    private const val PREFIX_BYTES = 80_000
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
}
