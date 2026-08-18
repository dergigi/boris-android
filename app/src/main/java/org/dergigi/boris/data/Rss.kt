package org.dergigi.boris.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class RssItem(
    val feedUrl: String,
    val sourceTitle: String,
    val title: String,
    val link: String,
    val publishedAt: Long,
    val summary: String?,
    val imageUrl: String?,
    val contentHtml: String?,
)

object RssDates {
    fun parseSeconds(raw: String?): Long? {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return null
        runCatching { return OffsetDateTime.parse(s).toEpochSecond() }
        runCatching { return Instant.parse(s).epochSecond }
        runCatching {
            return ZonedDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME).toEpochSecond()
        }
        for (pattern in FALLBACK_PATTERNS) {
            runCatching {
                return SimpleDateFormat(pattern, Locale.US).parse(s)!!.time / 1000
            }
        }
        return null
    }

    private val FALLBACK_PATTERNS = listOf(
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEE, dd MMM yyyy HH:mm zzz",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd",
    )
}

data class RssParseResult(val isFeed: Boolean, val items: List<RssItem>)

internal fun xmlRootLocalName(xml: String): String? {
    var text = xml.trimStart('\uFEFF', ' ', '\n', '\r', '\t')
    while (true) {
        text = text.trimStart()
        val skip = when {
            text.startsWith("<?") -> text.indexOf("?>").takeIf { it >= 0 }?.plus(2)
            text.startsWith("<!--") -> text.indexOf("-->").takeIf { it >= 0 }?.plus(3)
            text.startsWith("<!") -> text.indexOf('>').takeIf { it >= 0 }?.plus(1)
            else -> null
        } ?: break
        text = text.substring(skip)
    }
    if (!text.startsWith("<")) return null
    val name = text.drop(1).takeWhile { it.isLetterOrDigit() || it == ':' || it == '_' || it == '-' || it == '.' }
    return name.substringAfterLast(':').takeIf { it.isNotEmpty() }
}

/** Parses RSS 2.0 and Atom feeds. */
object RssParser {
    fun parse(xml: String, feedUrl: String): List<RssItem> = parseDocument(xml, feedUrl).items

    fun parseDocument(xml: String, feedUrl: String): RssParseResult {
        val root = xmlRootLocalName(xml)?.lowercase()
        if (root != "rss" && root != "feed") return RssParseResult(false, emptyList())
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(StringReader(xml.trimStart('\uFEFF', ' ', '\n', '\r', '\t')))

        val items = mutableListOf<RssItem>()
        var sourceTitle: String? = null
        var inItem = false
        var title: String? = null
        var link: String? = null
        var publishedAt: Long? = null
        var description: String? = null
        var encoded: String? = null
        var atomContent: String? = null
        var imageUrl: String? = null

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when {
                    parser.name == "item" || parser.name == "entry" -> {
                        inItem = true
                        title = null
                        link = null
                        publishedAt = null
                        description = null
                        encoded = null
                        atomContent = null
                        imageUrl = null
                    }
                    inItem -> when (parser.name) {
                        "title" -> title = text(parser)
                        "link" -> {
                            val href = parser.getAttributeValue(null, "href")
                            if (href != null) {
                                val rel = parser.getAttributeValue(null, "rel")
                                if (link == null && (rel == null || rel == "alternate")) link = href
                            } else {
                                link = text(parser) ?: link
                            }
                        }
                        "pubDate", "published", "updated", "date" ->
                            publishedAt = publishedAt ?: RssDates.parseSeconds(text(parser))
                        "description", "summary" -> description = text(parser)
                        "encoded" -> encoded = text(parser)
                        "content" -> {
                            val url = parser.getAttributeValue(null, "url")
                            if (url != null) {
                                // media:content
                                val type = parser.getAttributeValue(null, "type")
                                if (imageUrl == null && (type == null || type.startsWith("image"))) {
                                    imageUrl = url
                                }
                            } else {
                                atomContent = text(parser) ?: atomContent
                            }
                        }
                        "enclosure" -> {
                            val type = parser.getAttributeValue(null, "type").orEmpty()
                            val url = parser.getAttributeValue(null, "url")
                            if (imageUrl == null && url != null && type.startsWith("image")) {
                                imageUrl = url
                            }
                        }
                        "thumbnail" -> imageUrl = imageUrl ?: parser.getAttributeValue(null, "url")
                    }
                    parser.name == "title" && sourceTitle == null -> sourceTitle = text(parser)
                }
                XmlPullParser.END_TAG -> if (parser.name == "item" || parser.name == "entry") {
                    inItem = false
                    val itemLink = link?.trim()
                    if (!itemLink.isNullOrEmpty()) {
                        val html = encoded ?: atomContent ?: description
                        items.add(
                            RssItem(
                                feedUrl = feedUrl,
                                sourceTitle = sourceTitle?.trim().orEmpty()
                                    .ifEmpty { ArticleUrl.host(feedUrl) ?: feedUrl },
                                title = HtmlToMarkdown.decode(title?.trim().orEmpty())
                                    .ifEmpty { itemLink },
                                link = itemLink,
                                publishedAt = publishedAt ?: 0L,
                                summary = summaryText(description ?: html),
                                imageUrl = imageUrl ?: firstImage(html),
                                contentHtml = html,
                            ),
                        )
                    }
                }
            }
            event = parser.next()
        }
        return RssParseResult(true, items)
    }

    /** Reads element text; gives up on nested markup (e.g. xhtml content). */
    private fun text(parser: XmlPullParser): String? =
        try {
            parser.nextText().takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }

    private fun summaryText(html: String?): String? {
        if (html.isNullOrBlank()) return null
        val plain = HtmlToMarkdown.decode(
            html.replace(Regex("(?is)<[^>]+>"), " "),
        ).replace(Regex("\\s+"), " ").trim()
        if (plain.isEmpty()) return null
        return if (plain.length <= 300) plain else plain.take(299).trimEnd() + "…"
    }

    private fun firstImage(html: String?): String? {
        if (html.isNullOrBlank()) return null
        val tag = Regex("(?is)<img[^>]*>").find(html)?.value ?: return null
        return Regex("(?i)\\bsrc\\s*=\\s*[\"']([^\"']+)[\"']")
            .find(tag)?.groupValues?.getOrNull(1)
    }
}
