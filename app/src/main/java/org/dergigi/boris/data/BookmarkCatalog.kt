package org.dergigi.boris.data

import org.dergigi.boris.nostr.BookmarkRef
import org.dergigi.boris.nostr.BookmarkRefKind
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip19
import org.dergigi.boris.nostr.Archive
import org.dergigi.boris.nostr.Lookmarks
import org.dergigi.boris.nostr.Nip51
import org.dergigi.boris.nostr.Nip84
import org.dergigi.boris.nostr.NipB0

enum class BookmarkBucket {
    All,
    Private,
    Public,
    Web,
    Look,
    Archive,
}

data class BookmarkItem(
    val id: String,
    val title: String,
    val url: String?,
    val host: String?,
    val imageUrl: String?,
    val createdAt: Long,
    val bucket: BookmarkBucket,
    val summary: String? = null,
    val highlightId: String? = null,
    val highlightQuote: String? = null,
) {
    val isHighlight: Boolean get() = !highlightId.isNullOrBlank()

    fun open(
        onArticle: (String) -> Unit,
        onHighlight: (url: String, highlightId: String, quote: String) -> Unit,
    ) {
        val dest = url ?: return
        val hid = highlightId
        if (!hid.isNullOrBlank()) {
            onHighlight(dest, hid, highlightQuote ?: title)
        } else {
            onArticle(dest)
        }
    }
}

data class BookmarkShelves(
    val private: List<BookmarkItem> = emptyList(),
    val public: List<BookmarkItem> = emptyList(),
    val web: List<BookmarkItem> = emptyList(),
    val look: List<BookmarkItem> = emptyList(),
    val archive: List<BookmarkItem> = emptyList(),
    val privateLocked: Boolean = false,
) {
    fun items(bucket: BookmarkBucket): List<BookmarkItem> = when (bucket) {
        BookmarkBucket.All -> merged()
        BookmarkBucket.Private -> private
        BookmarkBucket.Public -> public
        BookmarkBucket.Web -> web
        BookmarkBucket.Look -> look
        BookmarkBucket.Archive -> archive
    }

    /** Time-sorted union of every shelf; duplicate article targets keep the newest copy. */
    fun merged(): List<BookmarkItem> =
        (private + public + web + look + archive)
            .groupBy { it.targetKey() }
            .values
            .map { group -> group.maxBy { it.createdAt } }
            .sortedByDescending { it.createdAt }
}

private fun BookmarkItem.targetKey(): String =
    url?.let { "url:${ArticleUrl.normalize(it)}" } ?: id

object BookmarkCatalog {
    fun build(
        listEvent: Nip01Event?,
        hiddenTags: List<List<String>>?,
        webEvents: List<Nip01Event>,
        lookEvents: List<Nip01Event> = emptyList(),
        archiveEvents: List<Nip01Event> = emptyList(),
        articles: Map<String, Nip01Event> = emptyMap(),
        notes: Map<String, Nip01Event> = emptyMap(),
        previews: Map<String, OgPreview?> = emptyMap(),
    ): BookmarkShelves {
        val listUpdatedAt = listEvent?.createdAt ?: 0L
        val publicItems = listEvent
            ?.let { Nip51.publicRefs(it) }
            .orEmpty()
            .mapNotNull { ref -> itemFromRef(ref, BookmarkBucket.Public, listUpdatedAt, articles, notes, previews) }
            .dedupe()
        val privateItems = hiddenTags
            ?.let { Nip51.parseTags(it) }
            .orEmpty()
            .mapNotNull { ref -> itemFromRef(ref, BookmarkBucket.Private, listUpdatedAt, articles, notes, previews) }
            .dedupe()
        val webItems = webEvents
            .sortedByDescending { NipB0.publishedAt(it) }
            .mapNotNull { event -> itemFromWeb(event, previews) }
            .dedupe()
        val lookItems = lookEvents
            .filter(Lookmarks::isLook)
            .sortedByDescending { it.createdAt }
            .mapNotNull { event ->
                val ref = Lookmarks.targetRef(event) ?: return@mapNotNull null
                itemFromRef(ref, BookmarkBucket.Look, event.createdAt, articles, notes, previews)
            }
            .dedupe()
        val archiveItems = archiveEvents
            .filter(Archive::isArchive)
            .sortedByDescending { it.createdAt }
            .mapNotNull { event ->
                val ref = Archive.targetRef(event) ?: return@mapNotNull null
                itemFromRef(ref, BookmarkBucket.Archive, event.createdAt, articles, notes, previews)
            }
            .dedupe()
        return BookmarkShelves(
            private = privateItems,
            public = publicItems,
            web = webItems,
            look = lookItems,
            archive = archiveItems,
            privateLocked = hiddenTags == null && Nip51.looksEncrypted(listEvent?.content.orEmpty()),
        )
    }

    private fun itemFromRef(
        ref: BookmarkRef,
        bucket: BookmarkBucket,
        createdAt: Long,
        articles: Map<String, Nip01Event>,
        notes: Map<String, Nip01Event>,
        previews: Map<String, OgPreview?>,
    ): BookmarkItem? {
        return when (ref.kind) {
            BookmarkRefKind.Article -> {
                val article = NostrArticle.fromCoordinate(ref.value) ?: return null
                val event = articles[ref.value]
                val title = event?.tagValue("title")?.takeIf { it.isNotBlank() } ?: article.pointer.identifier
                val image = event?.tagValue("image")
                BookmarkItem(
                    id = "a:${ref.value}",
                    title = title,
                    url = article.uri,
                    host = article.pointer.identifier,
                    imageUrl = image,
                    createdAt = event?.createdAt ?: createdAt,
                    bucket = bucket,
                )
            }
            BookmarkRefKind.Url -> {
                val preview = previews[ref.value]
                BookmarkItem(
                    id = "r:${ArticleUrl.normalize(ref.value)}",
                    title = preview?.title?.takeIf { it.isNotBlank() }
                        ?: ArticleUrl.host(ref.value)
                        ?: ref.value,
                    url = ref.value,
                    host = preview?.siteName?.takeIf { it.isNotBlank() } ?: ArticleUrl.host(ref.value),
                    imageUrl = preview?.imageUrl,
                    createdAt = createdAt,
                    bucket = bucket,
                )
            }
            BookmarkRefKind.Note -> {
                val eventId = ref.value.lowercase()
                val encoded = try {
                    Nip19.noteEncode(eventId)
                } catch (_: Exception) {
                    return null
                }
                val event = notes[eventId]
                if (event?.kind == Nip01Event.KIND_HIGHLIGHT) {
                    return highlightItem(event, encoded, bucket)
                }
                BookmarkItem(
                    id = "e:$eventId",
                    title = NoteCover.title(event),
                    url = "nostr:$encoded",
                    host = "nostr",
                    imageUrl = NoteCover.image(event),
                    createdAt = event?.createdAt ?: createdAt,
                    bucket = bucket,
                )
            }
        }
    }

    private fun highlightItem(
        event: Nip01Event,
        encodedNote: String,
        bucket: BookmarkBucket,
    ): BookmarkItem {
        val quote = MarkdownInline.plain(event.content.trim()).ifBlank { "Highlight" }
        val articleUrl = Nip84.articleUrl(event)
        val context = event.tagValue("context")
            ?.trim()
            ?.let(MarkdownInline::plain)
            ?.takeIf { it.isNotBlank() && !it.equals(quote, ignoreCase = true) }
        return BookmarkItem(
            id = "e:${event.id.lowercase()}",
            title = quote,
            url = articleUrl ?: "nostr:$encodedNote",
            host = articleUrl?.let { ArticleUrl.host(it) } ?: "highlight",
            imageUrl = null,
            createdAt = event.createdAt,
            bucket = bucket,
            summary = context,
            highlightId = event.id,
            highlightQuote = quote,
        )
    }

    private fun itemFromWeb(
        event: Nip01Event,
        previews: Map<String, OgPreview?>,
    ): BookmarkItem? {
        val url = NipB0.url(event) ?: return null
        val preview = previews[url]
        val host = ArticleUrl.host(url) ?: return null
        return BookmarkItem(
            id = "w:${event.pubkey.lowercase()}:${event.tagValue("d")}",
            title = NipB0.title(event)
                ?: preview?.title?.takeIf { it.isNotBlank() }
                ?: host,
            url = url,
            host = preview?.siteName?.takeIf { it.isNotBlank() } ?: host,
            imageUrl = preview?.imageUrl,
            createdAt = NipB0.publishedAt(event),
            bucket = BookmarkBucket.Web,
        )
    }

    private fun List<BookmarkItem>.dedupe(): List<BookmarkItem> {
        val seen = LinkedHashSet<String>()
        return filter { seen.add(it.id) }
    }
}
