package org.dergigi.boris.data

import org.junit.Assert.*
import org.junit.Test

class ReadwiseCsvTest {
    @Test
    fun handlesBomCrLfQuotedCommasQuotesNewlinesAndUnicode() {
        val csv = "\uFEFFTitle,URL,Category\r\n\"Hello, \"\"世界\"\"\nnext\",https://example.com/a,article\r\n"
        val result = ReadwiseCsv.parse(csv.reader())
        assertEquals(listOf(ReadwiseArticle("https://example.com/a", "Hello, \"世界\"\nnext")), result.articles)
        assertEquals(0, result.skipped)
    }

    @Test
    fun prioritizesSourceUrlOverReaderUrlAndSupportsReorderedHeaders() {
        val result = ReadwiseCsv.parse("URL,Other,Source URL,Title\nhttps://read.readwise.io/read/123,x,https://example.com/a,Original".reader())
        assertEquals("https://example.com/a", result.articles.single().url)
        assertEquals("Original", result.articles.single().title)
    }

    @Test
    fun skipsUnsupportedCategoriesMissingSourcesAndUnsafeSchemes() {
        val csv = "Title,URL,Category\n" + listOf(
            "pdf,https://example.com/a.pdf,pdf", "epub,https://example.com/a.epub,epub",
            "email,https://example.com/email,email", "missing,,article",
            "internal,https://read.readwise.io/read/123,article", "bad,javascript:alert(1),article",
            "bad,file:///tmp/a,article", "bad,https:///a,article", "bad,https://user:pass@example.com/a,article",
            "good,https://example.com/good,rss",
        ).joinToString("\n")
        val result = ReadwiseCsv.parse(csv.reader())
        assertEquals(9, result.skipped)
        assertEquals("good", result.articles.single().title)
    }

    @Test
    fun acceptsUrlOnlyExportAndBlankLines() {
        val result = ReadwiseCsv.parse("url\n\nhttps://example.com\n".reader())
        assertEquals(listOf(ReadwiseArticle("https://example.com", null)), result.articles)
    }

    @Test
    fun rejectsWrongExportAndMalformedQuotingBeforeImport() {
        for (csv in listOf("", "Book Title,Highlight\nbook,quote", "URL,Title\nhttps://example.com,\"unfinished", "URL\n\"https://example.com\"oops")) {
            assertThrows(IllegalArgumentException::class.java) { ReadwiseCsv.parse(csv.reader()) }
        }
    }

    @Test
    fun boundsFileSizeAndRowCount() {
        assertThrows(IllegalArgumentException::class.java) {
            ReadwiseCsv.parse(("url\n" + "x".repeat(ReadwiseCsv.MAX_CHARS)).reader())
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReadwiseCsv.parse(("url\n" + "https://example.com\n".repeat(ReadwiseCsv.MAX_ROWS + 1)).reader())
        }
    }
}
