package org.dergigi.boris.data

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class PocketCsvTest {
    @Test
    fun importsSavesArchivesAndFavoritesWithoutFilteringStatus() {
        val export = PocketCsv.parse("""
            title,url,time_added,tags,status
            Saved,https://example.com/saved,1700000000,reading,unread
            Archived,https://example.com/archive,1700000001,,archive
            Favorite,https://example.com/favorite,1700000002,starred,favorite
        """.trimIndent().reader())
        assertEquals(listOf("Saved", "Archived", "Favorite"), export.articles.map { it.title })
        assertEquals(0, export.skipped)
    }

    @Test
    fun acceptsBomReorderedHeadersEscapedQuotesAndUnicode() {
        val export = PocketCsv.parse(("\uFEFFURL,Title,Tags\r\n" +
            "https://example.com/a,\"Hello, \"\"世界\"\"\nnext\",\"one,two\"\r\n").reader())
        assertEquals(ImportArticle("https://example.com/a", "Hello, \"世界\"\nnext"), export.articles.single())
    }

    @Test
    fun urlOnlyAndBlankTitlesUseTheFetchedTitleLater() {
        assertNull(PocketCsv.parse("url\nhttps://example.com/a".reader()).articles.single().title)
        assertNull(PocketCsv.parse("url,title\nhttps://example.com/a,".reader()).articles.single().title)
    }

    @Test
    fun retainsSourceQueriesFragmentsAndCase() {
        val url = "https://example.com/Article?edition=2&lang=pt#part"
        assertEquals(url, PocketCsv.parse("title,url\nTitle,$url".reader()).articles.single().url)
    }

    @Test
    fun skipsInvalidUrlsWithoutFetchingThem() {
        val export = PocketCsv.parse("""
            title,url
            Missing,
            File,file:///tmp/article
            Script,javascript:alert(1)
            Invalid,https:///article
            Credentials,https://user:password@example.com/article
            Relative,/article
            Valid,http://example.com/article
        """.trimIndent().reader())
        assertEquals(6, export.skipped)
        assertEquals("http://example.com/article", export.articles.single().url)
    }

    @Test
    fun rejectsNonCsvExportsAndMalformedCsv() {
        for (text in listOf("", "<html><a href=\"https://example.com\">Title</a></html>", "title,tags\nTitle,tag", "url,title\nhttps://example.com,\"Unfinished")) {
            assertThrows(IllegalArgumentException::class.java) { PocketCsv.parse(text.reader()) }
        }
    }

    @Test
    fun duplicateRowsStayInPreviewForTheSharedImporterToReport() {
        val export = PocketCsv.parse("url,title\nhttps://example.com/a,First\nhttps://example.com/a,Second".reader())
        assertEquals(2, export.articles.size)
        assertEquals(0, export.skipped)
    }

    @Test
    fun pocketAndReadwiseShareDuplicateDetectionAndDurableCopies() = runBlocking {
        val directory = Files.createTempDirectory("pocket-import-test").toFile()
        ImportedArticles.init(directory)
        try {
            val fetched = mutableListOf<String>()
            val fetch: (String) -> ReadableContent = { url ->
                fetched += url
                ReadableContent(url, "Fetched", markdown = "Text for $url")
            }
            ArticleImport.run(
                ReadwiseCsv.parse("Title,URL\nReader title,https://example.com/a".reader()),
                emptySet(), fetch, ImportedArticles::add, {},
            )
            val result = ArticleImport.run(
                PocketCsv.parse("title,url\nDuplicate,https://example.com/a#section\nPocket title,https://example.com/b\nAgain,https://example.com/b".reader()),
                ArticleImport.knownUrls(null), fetch, ImportedArticles::add, {},
            )
            assertEquals(1, result.imported)
            assertEquals(2, result.duplicates)
            assertEquals(listOf("https://example.com/a", "https://example.com/b"), fetched)
            ImportedArticles.init(directory)
            assertEquals(2, ImportedArticles.items().size)
            assertEquals("Reader title", ImportedArticles.load("https://example.com/a")!!.title)
            assertEquals("Pocket title", ImportedArticles.load("https://example.com/b")!!.title)
            assertEquals("Text for https://example.com/b", ImportedArticles.load("https://example.com/b")!!.body)
        } finally {
            ImportedArticles.clear()
            directory.deleteRecursively()
        }
    }
}
