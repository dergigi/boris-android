package org.dergigi.boris.data

import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
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
            null -> {
                val targetUrl = UrlExtractor.normalize(url)
                rssContent(url, targetUrl) ?: run {
                    val request = Request.Builder()
                        .url(toProxyUrl(targetUrl))
                        .header("Accept", "text/plain")
                        .get()
                        .build()
                    val text = try {
                        execute(request)
                    } catch (e: IOException) {
                        executeFromCache(request) ?: throw e
                    }
                    withCover(parse(targetUrl, text))
                }
            }
        }
        ArticlePreview.remember(content)
        OfflineStore.markDownloaded(url)
        return content
    }

    private fun execute(request: Request): String =
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to fetch readable content (${response.code})")
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
        if (markdown.length < MIN_RSS_MARKDOWN_CHARS) return null
        val body = item.imageUrl?.let { ArticleCover.stripLeadingImage(markdown, it) } ?: markdown
        return ReadableContent(
            url = item.link,
            title = item.title,
            markdown = body,
            publishedAt = item.publishedAt.takeIf { it > 0 },
            imageUrl = item.imageUrl,
            summary = item.summary,
        )
    }

    private fun zapTags(event: Nip01Event): List<List<String>> =
        event.tags.filter { it.size >= 2 && it[0] == "zap" }

    internal fun noteMarkdown(content: String): String =
        UrlExtractor.embedImageLinks(content.replace("\n", "  \n"))

    internal fun parse(targetUrl: String, text: String): ReadableContent {
        val hasMarkdownBlock = markdownBlockRegex.containsMatchIn(text)
        return if (hasMarkdownBlock) {
            val title = titleRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
            val rawMarkdown = markdownRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
            val image = ArticleCover.imageFromJina(text)
            val markdown = if (rawMarkdown != null && image != null) {
                ArticleCover.stripLeadingImage(rawMarkdown, image)
            } else {
                rawMarkdown
            }
            ReadableContent(
                url = targetUrl,
                title = title,
                markdown = markdown,
                publishedAt = PublishedTime.fromJinaHeader(text),
                imageUrl = image,
                summary = ArticleCover.descriptionFromJina(text),
            )
        } else {
            val preview = OgMeta.parse(text, targetUrl)
            val title = htmlTitleRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
            ReadableContent(
                url = targetUrl,
                title = preview.title ?: title,
                html = text,
                publishedAt = PublishedTime.fromHtml(text),
                imageUrl = preview.imageUrl,
                summary = preview.description,
            )
        }
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
            imageUrl = image,
            summary = content.summary ?: preview.description,
            markdown = markdown,
        )
    }

    private fun toProxyUrl(url: String): String = "https://r.jina.ai/$url"

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

        private val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .apply {
                    val dir = httpCacheDir ?: return@apply
                    cache(Cache(dir, httpCacheBytes))
                    // jina sends no cache headers; force responses into the cache
                    // so previously opened articles render offline.
                    addNetworkInterceptor { chain ->
                        chain.proceed(chain.request()).newBuilder()
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

        /** Feed bodies shorter than this are teasers; fetch the web page instead. */
        private const val MIN_RSS_MARKDOWN_CHARS = 500

        private val markdownBlockRegex = Regex("""Markdown Content:\s""", RegexOption.IGNORE_CASE)
        private val titleRegex = Regex(
            """Title:\s*(.*?)(?:\s+URL Source:|\s+Markdown Content:)""",
            RegexOption.IGNORE_CASE,
        )
        private val markdownRegex = Regex(
            """Markdown Content:\s*([\s\S]*)$""",
            RegexOption.IGNORE_CASE,
        )
        private val htmlTitleRegex = Regex(
            """<title[^>]*>(.*?)</title>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
    }
}
