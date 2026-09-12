package org.dergigi.boris.data

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ReadwiseImportTest {
    private val a = ReadwiseArticle("https://example.com/a", "Exported title")
    private val b = ReadwiseArticle("https://example.com/b", null)

    @Test
    fun skipsKnownAndRepeatedUrlsAndPreservesSourceAndTitle() = runBlocking {
        val saved = mutableListOf<ReadableContent>()
        val fetched = mutableListOf<String>()
        val result = ReadwiseImport.run(
            ReadwiseExport(listOf(a, a.copy(url = a.url + "#part"), b), 2),
            setOf("http://www.example.com/b/"),
            fetch = { fetched += it; ReadableContent("https://redirect.example/new", "Fetched", markdown = "Text") },
            save = { saved += it; true }, onProgress = {},
        )
        assertEquals(listOf(a.url), fetched)
        assertEquals(a.url, saved.single().url)
        assertEquals(a.title, saved.single().title)
        assertEquals(1, result.imported)
        assertEquals(2, result.duplicates)
        assertEquals(2, result.skipped)
        assertEquals(3, result.processed)
    }

    @Test
    fun failedDownloadsAndWritesAreNotCountedAndOtherArticlesContinue() = runBlocking {
        val c = ReadwiseArticle("https://example.com/c", null)
        val result = ReadwiseImport.run(ReadwiseExport(listOf(a, b, c), 0), emptySet(),
            fetch = { if (it == a.url) throw IOException("offline") else ReadableContent(it, markdown = "body") },
            save = { if (it.url == b.url) throw IOException("disk full") else true }, onProgress = {})
        assertEquals(listOf(a, b), result.failed)
        assertEquals(1, result.imported)
        assertEquals(3, result.processed)
    }

    @Test
    fun emptyBodiesAreFailuresAndFetchedTitleIsFallback() = runBlocking {
        val saved = mutableListOf<ReadableContent>()
        val result = ReadwiseImport.run(ReadwiseExport(listOf(a, b), 0), emptySet(),
            fetch = { ReadableContent(it, "Fetched title", markdown = if (it == a.url) "" else "Body") },
            save = { saved += it; true }, onProgress = {})
        assertEquals(listOf(a), result.failed)
        assertEquals("Fetched title", saved.single().title)
    }

    @Test
    fun cancellationIsNotSwallowedAsAnArticleFailure() {
        assertThrows(CancellationException::class.java) {
            runBlocking {
                ReadwiseImport.run(ReadwiseExport(listOf(a, b), 0), emptySet(),
                    fetch = { throw CancellationException() }, save = { fail("Should not save"); true }, onProgress = {})
            }
        }
    }
}
