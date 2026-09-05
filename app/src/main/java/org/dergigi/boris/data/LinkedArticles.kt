package org.dergigi.boris.data

import org.dergigi.boris.nostr.Nip01Event
import java.net.URI

data class LinkedArticleRef(
    val url: String,
    val createdAt: Long,
)

object LinkedArticles {
    fun fromNotes(notes: Collection<Nip01Event>): List<LinkedArticleRef> {
        val seen = LinkedHashSet<String>()
        return notes
            .filter { it.kind == Nip01Event.KIND_TEXT_NOTE || it.kind == Nip01Event.KIND_COMMENT }
            .flatMap { note ->
                urlsFromText(note.content)
                    .map { url -> LinkedArticleRef(url, note.createdAt) }
            }
            .filter { seen.add(keyFor(it.url)) }
    }

    fun isArticleLike(
        url: String,
        preview: OgPreview?,
        cached: ReadableContent? = ArticleCache.load(url),
    ): Boolean {
        when (NostrLink.parse(url)) {
            is NostrTarget.Article -> return true
            is NostrTarget.Note, is NostrTarget.Profile -> return false
            null -> Unit
        }
        if (!isPlausibleWebArticle(url)) return false
        if (cached?.body?.isNotBlank() == true) return true
        val meta = preview ?: ArticlePreview.get(url) ?: return false
        val hasTitle = !meta.title.isNullOrBlank()
        val hasSummary = !meta.description.isNullOrBlank()
        return hasTitle && (hasSummary || pathLooksArticleLike(url))
    }

    internal fun urlsFromText(text: String): List<String> {
        val (protected, _) = NostrMentions.protectCode(text)
        val linkRanges = NostrMentions.markdownLinkUrlRanges(protected)
        val urls = linkedMapOf<String, String>()
        linkRanges.forEach { range ->
            articleUrlCandidate(protected.substring(range))?.let { url ->
                urls[keyFor(url)] = url
            }
        }
        httpRegex.findAll(protected).forEach { match ->
            if (linkRanges.any { match.range.first in it || match.range.last in it }) return@forEach
            articleUrlCandidate(match.value)?.let { url ->
                urls[keyFor(url)] = url
            }
        }
        naddrRegex.findAll(protected).forEach { match ->
            if (linkRanges.any { match.range.first in it || match.range.last in it }) return@forEach
            articleUrlCandidate(match.value)?.let { url ->
                urls[keyFor(url)] = url
            }
        }
        return urls.values.toList()
    }

    private fun articleUrlCandidate(raw: String): String? {
        val cleaned = raw
            .trim()
            .trim('<', '>')
            .substringBefore('"')
            .substringBefore("'")
            .trim()
            .trimEnd('.', ',', ';', ':', '!', '?', ')', ']')
        if (cleaned.isBlank()) return null
        val url = UrlExtractor.articleUrl(cleaned) ?: return null
        return if (NostrLink.parse(url) is NostrTarget.Profile) null else url
    }

    private fun isPlausibleWebArticle(url: String): Boolean {
        val parsed = runCatching {
            URI(if (url.contains("://")) url.trim() else "https://${url.trim()}")
        }.getOrNull() ?: return false
        val scheme = parsed.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        val host = parsed.host?.lowercase()?.removePrefix("www.") ?: return false
        if (host in ignoredHosts) return false
        if (UrlExtractor.isImageUrl(url)) return false
        val extension = parsed.path
            .orEmpty()
            .substringAfterLast('/', "")
            .substringAfterLast('.', "")
            .lowercase()
        return extension !in ignoredExtensions
    }

    private fun pathLooksArticleLike(url: String): Boolean {
        val path = runCatching {
            URI(if (url.contains("://")) url.trim() else "https://${url.trim()}")
        }.getOrNull()?.path.orEmpty().trim('/')
        if (path.isBlank()) return false
        val segments = path.split('/').filter { it.isNotBlank() }
        if (segments.any { it.lowercase() in articleMarkers }) return true
        if (segments.size >= 2) return true
        return segments.singleOrNull()?.contains(Regex("""\d{4}|[-_]""")) == true
    }

    private fun keyFor(url: String): String =
        when (val target = NostrLink.parse(url)) {
            is NostrTarget.Article -> "a:${target.ref.coordinate}"
            is NostrTarget.Note -> "e:${target.eventId}"
            is NostrTarget.Profile -> "p:${target.pubkeyHex}"
            null -> "r:${ArticleUrl.normalize(url)}"
        }

    private val bech32Body = "023456789acdefghjklmnpqrstuvwxyz"
    private val httpRegex = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
    private val naddrRegex = Regex("""(?<![/\w])(?:nostr:(?://)?)?naddr1[$bech32Body]+""", RegexOption.IGNORE_CASE)

    private val ignoredHosts = setOf(
        "bit.ly",
        "bitly.com",
        "buff.ly",
        "goo.gl",
        "is.gd",
        "lnkd.in",
        "ow.ly",
        "rebrand.ly",
        "shorturl.at",
        "t.co",
        "tinyurl.com",
        "x.com",
        "twitter.com",
        "youtube.com",
        "youtu.be",
    )

    private val ignoredExtensions = setOf(
        "apk",
        "avi",
        "csv",
        "dmg",
        "doc",
        "docx",
        "epub",
        "gz",
        "m4a",
        "mov",
        "mp3",
        "mp4",
        "pdf",
        "ppt",
        "pptx",
        "tar",
        "tgz",
        "wav",
        "webm",
        "xls",
        "xlsx",
        "zip",
    )

    private val articleMarkers = setOf("article", "articles", "blog", "essays", "post", "posts", "read")
}
