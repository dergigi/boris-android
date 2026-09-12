package org.dergigi.boris.data

import java.io.File
import java.nio.file.Files
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ImportedArticlesTest {
    private lateinit var directory: File
    private val content = ReadableContent("https://example.com/article", "Saved title", markdown = "Offline article text")

    @Before fun setup() {
        directory = Files.createTempDirectory("boris-import-test").toFile()
        ImportedArticles.init(directory)
    }

    @After fun cleanup() {
        ImportedArticles.clear()
        directory.deleteRecursively()
    }

    @Test
    fun persistsAcrossRestartAndReadsWithoutNetworkOrArticleCache() {
        assertTrue(ImportedArticles.add(content))
        ImportedArticles.init(directory)
        ArticleCache.reset()
        val repository = ReaderRepository(OkHttpClient.Builder().addInterceptor { error("Unexpected network") }.build())
        assertEquals(content, repository.fetch(content.url))
        assertEquals(content, repository.peekCached(content.url))
        assertEquals("Saved title", ImportedArticles.items().single().title)
    }

    @Test
    fun duplicateVariantsDoNotOverwriteOriginalTitleOrBody() {
        assertTrue(ImportedArticles.add(content))
        assertFalse(ImportedArticles.add(content.copy(url = content.url + "#section", title = "Replacement")))
        assertEquals(content, ImportedArticles.load(content.url))
        assertEquals(1, ImportedArticles.items().size)
    }

    @Test
    fun catalogIncludesLocalItemsAndAvoidsCopiesAcrossShelves() {
        ImportedArticles.add(content)
        val local = ImportedArticles.items()
        val empty = BookmarkCatalog.build(null, hiddenTags = null, webEvents = emptyList(), localItems = local)
        assertEquals(local, empty.web)
        val event = org.dergigi.boris.nostr.Nip01Event("id", "author", 1, 10003,
            listOf(listOf("r", content.url)), "", "sig")
        val existing = BookmarkCatalog.build(event, hiddenTags = null, webEvents = emptyList(), localItems = local)
        assertEquals(1, existing.merged().size)
        assertTrue(existing.web.isEmpty())
    }

    @Test
    fun failedIndexCommitDoesNotAdvertiseAnImportedArticle() {
        File(directory, "index.json").mkdir()
        assertThrows(Exception::class.java) { ImportedArticles.add(content) }
        assertTrue(ImportedArticles.items().isEmpty())
        File(directory, "index.json").delete()
    }

    @Test
    fun clearRemovesOnlyImportStorageAndSurvivesRestart() {
        ImportedArticles.add(content)
        ImportedArticles.clear()
        ImportedArticles.init(directory)
        assertTrue(ImportedArticles.items().isEmpty())
        assertNull(ImportedArticles.load(content.url))
        assertEquals(listOf("index.json"), directory.listFiles()!!.map { it.name })
    }
}
