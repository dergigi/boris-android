package org.dergigi.boris.data

import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Fetches and caches RSS feeds. Fetched items are kept in memory so the
 * reader can render an item straight from feed content instead of
 * re-parsing the web page.
 */
object RssRepository {
    private const val CACHE_BYTES = 20L * 1024 * 1024

    @Volatile
    private var cacheDir: File? = null

    private val itemsByLink = ConcurrentHashMap<String, RssItem>()

    /** Must run before the first fetch. */
    fun init(dir: File) {
        cacheDir = dir
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .apply {
                val dir = cacheDir ?: return@apply
                cache(Cache(dir, CACHE_BYTES))
                // Many feeds send no-cache headers; force responses into the
                // cache so feeds and their items stay readable offline.
                addNetworkInterceptor { chain ->
                    chain.proceed(chain.request()).newBuilder()
                        .removeHeader("Pragma")
                        .removeHeader("Cache-Control")
                        .header("Cache-Control", "max-age=600")
                        .build()
                }
            }
            .build()
    }

    fun fetch(feedUrl: String): List<RssItem> = loadFeed(feedUrl).items

    fun discoverRootFeed(articleUrl: String): String? {
        for (feedUrl in RssDiscovery.feedCandidates(articleUrl)) {
            try {
                if (loadFeed(feedUrl).isFeed) return feedUrl
            } catch (_: Exception) {
                // Try the next common feed location.
            }
        }
        return null
    }

    private fun loadFeed(feedUrl: String): RssParseResult {
        val request = Request.Builder()
            .url(feedUrl)
            .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
            .get()
            .build()
        val xml = try {
            execute(request)
        } catch (e: IOException) {
            executeFromCache(request) ?: throw e
        }
        return rememberFeed(xml, feedUrl)
    }

    private fun rememberFeed(xml: String, feedUrl: String): RssParseResult {
        val parsed = RssParser.parseDocument(xml, feedUrl)
        if (parsed.isFeed) remember(parsed.items)
        return parsed
    }

    /**
     * Finds a previously fetched item for [url]. On a cold start the
     * in-memory index is rebuilt from the HTTP cache of [feedUrls].
     */
    fun itemFor(url: String, feedUrls: List<String>): RssItem? {
        itemsByLink[url]?.let { return it }
        if (itemsByLink.isEmpty()) {
            feedUrls.forEach { feed ->
                runCatching { fetchCached(feed) }
            }
        }
        return itemsByLink[url]
    }

    private fun fetchCached(feedUrl: String): List<RssItem> {
        val request = Request.Builder()
            .url(feedUrl)
            .cacheControl(CacheControl.FORCE_CACHE)
            .get()
            .build()
        val xml = executeFromCache(request) ?: return emptyList()
        return remember(RssParser.parse(xml, feedUrl))
    }

    private fun remember(items: List<RssItem>): List<RssItem> {
        items.forEach { itemsByLink[it.link] = it }
        return items
    }

    private fun execute(request: Request): String =
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to fetch feed (${response.code})")
            }
            response.body?.string().orEmpty()
        }

    private fun executeFromCache(request: Request): String? = try {
        val cached = request.newBuilder()
            .cacheControl(CacheControl.FORCE_CACHE)
            .build()
        client.newCall(cached).execute().use { response ->
            if (response.isSuccessful) response.body?.string() else null
        }
    } catch (_: IOException) {
        null
    }
}
