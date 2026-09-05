package org.dergigi.boris.data

import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

object OgMetaClient {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .dns(PublicDns)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    fun fetch(url: String): OgPreview? {
        val safeUrl = PublicHttpDestination.toHttpUrl(url) ?: return null
        return fetch(safeUrl, 0)?.also { preview ->
            OgPreviewCache.put(url, preview)
            OgPreviewCache.put(safeUrl.toString(), preview)
        }
    }

    private fun fetch(url: HttpUrl, redirects: Int): OgPreview? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", HttpUserAgents.BROWSER_UA)
            .header("Accept", "text/html,application/xhtml+xml")
            .get()
            .build()
        return client.newCall(request).execute().use { response ->
            if (response.isRedirect) {
                if (redirects >= MAX_REDIRECTS) return null
                val location = response.header("Location") ?: return null
                val nextUrl = url.resolve(location)?.let {
                    PublicHttpDestination.toHttpUrl(it.toString())
                } ?: return null
                return fetch(nextUrl, redirects + 1)
            }
            if (!response.isSuccessful) return null
            val source = response.body?.source() ?: return null
            source.request(PREFIX_BYTES.toLong())
            val n = minOf(source.buffer.size, PREFIX_BYTES.toLong())
            if (n == 0L) return null
            OgMeta.parse(source.buffer.readUtf8(n), url.toString())
        }
    }

    private object PublicDns : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val addresses = Dns.SYSTEM.lookup(hostname)
            if (addresses.isEmpty() || addresses.any { !PublicHttpDestination.isPublicAddress(it) }) {
                throw UnknownHostException("Blocked non-public destination: $hostname")
            }
            return addresses
        }
    }

    private const val PREFIX_BYTES = 80_000
    private const val MAX_REDIRECTS = 5
}
