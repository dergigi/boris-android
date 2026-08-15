package org.dergigi.boris.data

import org.dergigi.boris.nostr.BookmarkRef
import org.dergigi.boris.nostr.BookmarkRefKind
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip19
import org.dergigi.boris.nostr.Nip51
import org.dergigi.boris.nostr.NipB0

enum class BookmarkBucket {
    Private,
    Public,
    Web,
}

data class BookmarkItem(
    val id: String,
    val title: String,
    val url: String?,
    val host: String?,
    val imageUrl: String?,
    val createdAt: Long,
    val bucket: BookmarkBucket,
)

data class BookmarkShelves(
    val private: List<BookmarkItem> = emptyList(),
    val public: List<BookmarkItem> = emptyList(),
    val web: List<BookmarkItem> = emptyList(),
    val privateLocked: Boolean = false,
) {
    fun items(bucket: BookmarkBucket): List<BookmarkItem> = when (bucket) {
        BookmarkBucket.Private -> private
        BookmarkBucket.Public -> public
        BookmarkBucket.Web -> web
    }
}

object BookmarkCatalog {
    fun build(
        listEvent: Nip01Event?,
        hiddenTags: List<List<String>>?,
        webEvents: List<Nip01Event>,
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
        return BookmarkShelves(
            private = privateItems,
            public = publicItems,
            web = webItems,
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
                BookmarkItem(
                    id = "e:$eventId",
                    title = noteTitle(event),
                    url = "nostr:$encoded",
                    host = "nostr",
                    imageUrl = null,
                    createdAt = event?.createdAt ?: createdAt,
                    bucket = bucket,
                )
            }
        }
    }

    private fun noteTitle(event: Nip01Event?): String {
        val line = event?.content?.lineSequence()?.firstOrNull { it.isNotBlank() } ?: return "Note"
        return if (line.length <= 80) line else line.take(79).trimEnd() + "…"
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
