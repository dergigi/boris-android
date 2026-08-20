package org.dergigi.boris.data

import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip23
import org.dergigi.boris.nostr.RelayQuery
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class ReaderRepository(
    private val client: OkHttpClient = defaultClient,
) {
    fun fetch(url: String): ReadableContent {
        val content = when (val target = NostrLink.parse(url)) {
            is NostrTarget.Article -> fetchArticle(target.ref)
            is NostrTarget.Note -> fetchNote(target)
            is NostrTarget.Profile -> throw IOException("Profile links cannot be opened as articles")
            null -> {
                val targetUrl = UrlExtractor.normalize(url)
                rssContent(url, targetUrl) ?: run {
                    val origin = UrlExtractor.preferHttps(targetUrl)
                    withCover(fetchOrigin(origin))
                }
            }
        }
        val ready = content.copy(markdown = content.markdown?.let(UrlExtractor::embedImageLinks))
        ArticlePreview.remember(ready)
        OfflineStore.markDownloaded(url)
        return ready
    }

    // D-09: honest Boris UA first; one browser-UA retry on 401/403 or an
    // empty/thin extract. D-15: a live fail falls back to the origin cache.
    private fun fetchOrigin(origin: String): ReadableContent {
        val first = originAttempt(origin, HttpUserAgents.BORIS_UA)
        val second = when (first) {
            is OriginResult.Article -> return first.content
            is OriginResult.Blocked, is OriginResult.NoArticle ->
                originAttempt(origin, HttpUserAgents.BROWSER_UA)
            is OriginResult.Unreachable -> first
        }
        if (second is OriginResult.Article) return second.content
        if (second is OriginResult.NoArticle) {
            throw ReaderFetchException(ERROR_NO_ARTICLE, second.detail)
        }
        val cached = executeFromCache(originRequest(origin, HttpUserAgents.BORIS_UA))
            ?: throw ReaderFetchException(ERROR_UNREACHABLE, reachDetail(second, first))
        val content = parse(origin, cached)
        if (content.markdown == null) throw ReaderFetchException(ERROR_NO_ARTICLE, "Cached page had no readable article")
        return content
    }

    private fun originAttempt(origin: String, userAgent: String): OriginResult = try {
        client.newCall(originRequest(origin, userAgent)).execute().use { response ->
            when {
                response.code == 401 || response.code == 403 -> OriginResult.Blocked(response.code)
                !response.isSuccessful -> OriginResult.Unreachable("HTTP ${response.code}")
                !looksLikeHtml(response) -> OriginResult.NoArticle(
                    "Not HTML (${response.header("Content-Type") ?: "unknown"})",
                )
                else -> {
                    val text = readCapped(response)
                    val content = if (text.isBlank()) null else parse(origin, text)
                    if (content?.markdown == null) {
                        OriginResult.NoArticle(
                            if (text.isBlank()) "Empty page" else "No readable article in the page",
                        )
                    } else {
                        OriginResult.Article(content)
                    }
                }
            }
        }
    } catch (e: IOException) {
        OriginResult.Unreachable(networkDetail(e))
    }

    private fun originRequest(origin: String, userAgent: String): Request =
        Request.Builder()
            .url(origin)
            .header("User-Agent", userAgent)
            .header("Accept", "text/html,application/xhtml+xml")
            .get()
            .build()

    private fun looksLikeHtml(response: Response): Boolean {
        val contentType = response.header("Content-Type")?.lowercase() ?: return true
        return "html" in contentType || "xml" in contentType || contentType.startsWith("text/")
    }

    private fun readCapped(response: Response): String {
        val source = response.body?.source() ?: return ""
        source.request(MAX_BODY_BYTES)
        val n = minOf(source.buffer.size, MAX_BODY_BYTES)
        return source.buffer.readUtf8(n)
    }

    private sealed interface OriginResult {
        data class Article(val content: ReadableContent) : OriginResult
        data class Blocked(val code: Int) : OriginResult
        data class NoArticle(val detail: String? = null) : OriginResult
        data class Unreachable(val detail: String) : OriginResult
    }

    private fun reachDetail(vararg results: OriginResult): String? =
        results.firstNotNullOfOrNull { result ->
            when (result) {
                is OriginResult.Unreachable -> result.detail
                is OriginResult.Blocked -> "HTTP ${result.code}"
                is OriginResult.NoArticle -> result.detail
                is OriginResult.Article -> null
            }
        }

    private fun networkDetail(e: IOException): String {
        val name = e.javaClass.simpleName.removeSuffix("Exception")
        val msg = e.message?.trim()?.takeIf { it.isNotEmpty() }
        return if (msg == null) name else "$name: $msg"
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

    private fun fetchArticle(article: NostrArticleRef): ReadableContent {
        val event = RelayQuery.fetchArticle(article.pointer)
            ?: throw IOException("Article not found")
        val published = event.tagValue("published_at")?.toLongOrNull() ?: event.createdAt
        val image = Nip23.image(event)
        val markdown = image?.let { ArticleCover.stripLeadingImage(event.content, it) } ?: event.content
        return ReadableContent(
            url = article.uri,
            title = event.tagValue("title")?.ifBlank { null },
            markdown = markdown,
            publishedAt = published,
            articleCoordinate = article.coordinate,
            eventId = event.id,
            authorPubkey = event.pubkey,
            imageUrl = image,
            summary = Nip23.summary(event),
            sourceZapTags = zapTags(event),
        )
    }

    private fun fetchNote(note: NostrTarget.Note): ReadableContent {
        val event = RelayQuery.fetchEvent(note.eventId, note.relays)
            ?: throw IOException("Note not found")
        if (event.kind == Nip01Event.KIND_LONG_FORM) {
            val identifier = event.tagValue("d")
            val article = identifier
                ?.let { NostrArticle.fromCoordinate("${event.kind}:${event.pubkey}:$it", note.relays) }
            val image = Nip23.image(event)
            val markdown = image?.let { ArticleCover.stripLeadingImage(event.content, it) } ?: event.content
            return ReadableContent(
                url = article?.uri ?: note.uri,
                title = event.tagValue("title")?.ifBlank { null },
                markdown = markdown,
                publishedAt = event.tagValue("published_at")?.toLongOrNull() ?: event.createdAt,
                articleCoordinate = article?.coordinate,
                eventId = event.id,
                authorPubkey = event.pubkey,
                imageUrl = image,
                summary = Nip23.summary(event),
                sourceZapTags = zapTags(event),
            )
        }
        if (event.kind != Nip01Event.KIND_TEXT_NOTE) {
            throw IOException("This nostr event is not a note or article")
        }
        val title = event.content.lineSequence()
            .map { it.trim() }
            .firstOrNull { line ->
                if (line.isBlank()) return@firstOrNull false
                val bare = line.trimEnd('.', ',', ';')
                !(bare.startsWith("http", ignoreCase = true) && UrlExtractor.isImageUrl(bare))
            }
            ?.let { line ->
                if (line.length <= 80) line else line.take(79).trimEnd() + "…"
            } ?: "Note"
        return ReadableContent(
            url = note.uri,
            title = title,
            markdown = noteMarkdown(event.content),
            publishedAt = event.createdAt,
            eventId = event.id,
            authorPubkey = event.pubkey,
            sourceZapTags = zapTags(event),
        )
    }

    /**
     * Renders an RSS item straight from its feed content. Returns null when
     * the URL is not a known feed item or the feed only ships a teaser, in
     * which case the regular web fetch takes over.
     */
    private fun rssContent(vararg urls: String): ReadableContent? {
        val feeds = SettingsSync.settings.value.rssFeeds
        if (feeds.isEmpty()) return null
        val item = urls.distinct().firstNotNullOfOrNull { RssRepository.itemFor(it, feeds) }
            ?: return null
        val html = item.contentHtml ?: return null
        val markdown = HtmlToMarkdown.convert(html)
        if (markdown.length < MIN_ARTICLE_MARKDOWN_CHARS) return null
        val body = item.imageUrl?.let { ArticleCover.stripLeadingImage(markdown, it) } ?: markdown
        return ReadableContent(
            url = item.link,
            title = item.title,
            markdown = UrlExtractor.upgradeImageHttpUrls(body),
            publishedAt = item.publishedAt.takeIf { it > 0 },
            imageUrl = item.imageUrl?.let(UrlExtractor::preferHttps),
            summary = item.summary,
        )
    }

    private fun zapTags(event: Nip01Event): List<List<String>> =
        event.tags.filter { it.size >= 2 && it[0] == "zap" }

    internal fun noteMarkdown(content: String): String =
        UrlExtractor.embedImageLinks(content.replace("\n", "  \n"))

    internal fun parse(targetUrl: String, text: String): ReadableContent {
        val preview = OgMeta.parse(text, targetUrl)
        val title = htmlTitleRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
        val markdown = (
            ArticleExtractor.markdown(text, targetUrl)
                ?: HtmlToMarkdown.convert(text, targetUrl)
            )
            .let(UrlExtractor::upgradeImageHttpUrls)
            .takeIf { it.length >= MIN_ARTICLE_MARKDOWN_CHARS }
        val cover = preview.imageUrl?.let(UrlExtractor::preferHttps)
        return ReadableContent(
            url = targetUrl,
            title = preview.title ?: title?.let(HtmlToMarkdown::decode),
            markdown = cover?.let { image ->
                markdown?.let { ArticleCover.stripLeadingImage(it, image) }
            } ?: markdown,
            publishedAt = PublishedTime.fromHtml(text),
            imageUrl = cover,
            summary = preview.description,
        )
    }

    private fun withCover(content: ReadableContent): ReadableContent {
        if (!content.imageUrl.isNullOrBlank() && !content.summary.isNullOrBlank()) return content
        val preview = runCatching { OgMetaClient.fetch(content.url) }.getOrNull() ?: return content
        val image = content.imageUrl ?: preview.imageUrl
        val markdown = if (content.markdown != null && image != null) {
            ArticleCover.stripLeadingImage(content.markdown, image)
        } else {
            content.markdown
        }
        return content.copy(
            imageUrl = image?.let(UrlExtractor::preferHttps),
            summary = content.summary ?: preview.description,
            markdown = markdown?.let(UrlExtractor::upgradeImageHttpUrls),
        )
    }

    companion object {
        @Volatile
        private var httpCacheDir: File? = null

        @Volatile
        private var httpCacheBytes: Long = HTTP_CACHE_BYTES

        /** Must run before the first ReaderRepository is constructed. */
        fun init(cacheDir: File, maxBytes: Long = HTTP_CACHE_BYTES) {
            httpCacheDir = cacheDir
            httpCacheBytes = maxBytes
        }

        /** Bytes of a FORCE_CACHE hit for [url], or 0 if uncached. */
        fun cachedBodyBytes(url: String): Long {
            val normalized = UrlExtractor.normalize(url)
            val candidates = listOf(UrlExtractor.preferHttps(normalized))
            for (candidate in candidates) {
                val request = Request.Builder()
                    .url(candidate)
                    .cacheControl(CacheControl.FORCE_CACHE)
                    .get()
                    .build()
                val length = runCatching {
                    defaultClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@use -1L
                        response.body?.contentLength() ?: -1L
                    }
                }.getOrDefault(-1L)
                if (length > 0) return length
            }
            return 0L
        }

        private val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .apply {
                    val dir = httpCacheDir ?: return@apply
                    cache(Cache(dir, httpCacheBytes))
                    // Origins often send no-store; force successful responses into
                    // the cache so previously opened articles render offline.
                    addNetworkInterceptor { chain ->
                        val response = chain.proceed(chain.request())
                        if (!response.isSuccessful) return@addNetworkInterceptor response
                        response.newBuilder()
                            .removeHeader("Pragma")
                            .removeHeader("Cache-Control")
                            .header("Cache-Control", "public, max-age=$FRESH_SECONDS")
                            .build()
                    }
                }
                .build()
        }

        private const val HTTP_CACHE_BYTES = 50L * 1024L * 1024L
        private const val FRESH_SECONDS = 300

        // Converted bodies shorter than this are teasers or cookie walls, for
        // RSS items and web extracts alike; never render them as Ready.
        internal const val MIN_ARTICLE_MARKDOWN_CHARS = 500

        // D-13: the only two sentences the reader error state may show.
        internal const val ERROR_UNREACHABLE = "Could not reach this page."
        internal const val ERROR_NO_ARTICLE = "Could not find an article on this page."

        private const val MAX_BODY_BYTES = 2 * 1024 * 1024L

        private val htmlTitleRegex = Regex(
            """<title[^>]*>(.*?)</title>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
    }
}

internal class ReaderFetchException(
    message: String,
    val detail: String? = null,
    cause: Throwable? = null,
) : IOException(message, cause)
