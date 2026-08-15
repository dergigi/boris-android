package org.dergigi.boris.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.RelayQuery
import java.io.IOException
import java.util.concurrent.TimeUnit

class ReaderRepository(
    private val client: OkHttpClient = defaultClient,
) {
    fun fetch(url: String): ReadableContent {
        when (val target = NostrLink.parse(url)) {
            is NostrTarget.Article -> return fetchArticle(target.ref)
            is NostrTarget.Note -> return fetchNote(target)
            null -> Unit
        }
        val targetUrl = UrlExtractor.normalize(url)
        val request = Request.Builder()
            .url(toProxyUrl(targetUrl))
            .header("Accept", "text/plain")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to fetch readable content (${response.code})")
            }
            val text = response.body?.string().orEmpty()
            return parse(targetUrl, text)
        }
    }

    private fun fetchArticle(article: NostrArticleRef): ReadableContent {
        val event = RelayQuery.fetchArticle(article.pointer)
            ?: throw IOException("Article not found")
        val published = event.tagValue("published_at")?.toLongOrNull() ?: event.createdAt
        return ReadableContent(
            url = article.uri,
            title = event.tagValue("title")?.ifBlank { null },
            markdown = event.content,
            publishedAt = published,
            articleCoordinate = article.coordinate,
            eventId = event.id,
            authorPubkey = event.pubkey,
        )
    }

    private fun fetchNote(note: NostrTarget.Note): ReadableContent {
        val event = RelayQuery.fetchEvent(note.eventId, note.relays)
            ?: throw IOException("Note not found")
        if (event.kind == Nip01Event.KIND_LONG_FORM) {
            val identifier = event.tagValue("d")
            val article = identifier
                ?.let { NostrArticle.fromCoordinate("${event.kind}:${event.pubkey}:$it", note.relays) }
            return ReadableContent(
                url = article?.uri ?: note.uri,
                title = event.tagValue("title")?.ifBlank { null },
                markdown = event.content,
                publishedAt = event.tagValue("published_at")?.toLongOrNull() ?: event.createdAt,
                articleCoordinate = article?.coordinate,
                eventId = event.id,
                authorPubkey = event.pubkey,
            )
        }
        if (event.kind != Nip01Event.KIND_TEXT_NOTE) {
            throw IOException("This nostr event is not a note or article")
        }
        val title = event.content.lineSequence().firstOrNull { it.isNotBlank() }?.let { line ->
            if (line.length <= 80) line else line.take(79).trimEnd() + "…"
        } ?: "Note"
        return ReadableContent(
            url = note.uri,
            title = title,
            markdown = noteMarkdown(event.content),
            publishedAt = event.createdAt,
            eventId = event.id,
            authorPubkey = event.pubkey,
        )
    }

    private fun noteMarkdown(content: String): String = content.replace("\n", "  \n")

    internal fun parse(targetUrl: String, text: String): ReadableContent {
        val hasMarkdownBlock = markdownBlockRegex.containsMatchIn(text)
        return if (hasMarkdownBlock) {
            val title = titleRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
            val markdown = markdownRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
            ReadableContent(
                url = targetUrl,
                title = title,
                markdown = markdown,
                publishedAt = PublishedTime.fromJinaHeader(text),
            )
        } else {
            val title = htmlTitleRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
            ReadableContent(
                url = targetUrl,
                title = title,
                html = text,
                publishedAt = PublishedTime.fromHtml(text),
            )
        }
    }

    private fun toProxyUrl(url: String): String = "https://r.jina.ai/$url"

    companion object {
        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()

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
