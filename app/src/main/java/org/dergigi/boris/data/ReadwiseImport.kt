package org.dergigi.boris.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.dergigi.boris.nostr.EventCache
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip51
import org.dergigi.boris.nostr.RelayQuery

data class ReadwiseImportProgress(
    val processed: Int = 0,
    val total: Int = 0,
    val imported: Int = 0,
    val duplicates: Int = 0,
    val skipped: Int = 0,
    val failed: List<ReadwiseArticle> = emptyList(),
)

object ReadwiseImport {
    /** Use the library already on this device, including unlocked private bookmarks. */
    fun knownUrls(pubkey: String?): Set<String> {
        val local = ImportedArticles.items().mapNotNull { it.url }
        if (pubkey == null) return local.toSet()
        val list = EventCache.latest(Nip01Event.KIND_BOOKMARKS, pubkey)
        val hidden = list?.let { PrivateBookmarks.tagsFor(pubkey, it.content) }
        check(hidden != null || !Nip51.looksEncrypted(list?.content.orEmpty())) {
            "Unlock your private bookmarks in Library before importing so Boris can check for duplicates."
        }
        val shelves = BookmarkCatalog.build(
            listEvent = list, hiddenTags = hidden,
            webEvents = RelayQuery.cachedWebBookmarks(pubkey),
            lookEvents = RelayQuery.cachedLookmarks(pubkey),
            archiveEvents = RelayQuery.cachedArchiveReactions(pubkey),
        )
        return (local + shelves.merged().mapNotNull { it.url }).toSet()
    }

    suspend fun run(
        export: ReadwiseExport,
        knownUrls: Set<String>,
        fetch: (String) -> ReadableContent,
        save: (ReadableContent) -> Boolean,
        onProgress: (ReadwiseImportProgress) -> Unit,
    ): ReadwiseImportProgress {
        val seen = knownUrls.mapTo(hashSetOf(), ArticleUrl::normalize)
        var progress = ReadwiseImportProgress(total = export.articles.size, skipped = export.skipped)
        onProgress(progress)
        for (article in export.articles) {
            currentCoroutineContext().ensureActive()
            progress = if (!seen.add(ArticleUrl.normalize(article.url))) {
                progress.copy(duplicates = progress.duplicates + 1)
            } else {
                try {
                    val fetched = fetch(article.url)
                    currentCoroutineContext().ensureActive()
                    require(fetched.body.isNotBlank()) { "No readable text." }
                    val content = fetched.copy(url = article.url, title = article.title ?: fetched.title)
                    if (save(content)) progress.copy(imported = progress.imported + 1)
                    else progress.copy(duplicates = progress.duplicates + 1)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    progress.copy(failed = progress.failed + article)
                }
            }
            progress = progress.copy(processed = progress.processed + 1)
            onProgress(progress)
        }
        return progress
    }
}
