package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebArchivesTest {
    private val source = "https://example.com/article?q=one&lang=en"
    private val copy = "https://cdn.example/archive.html"
    private val mirror = "https://mirror.example/archive.html"

    private fun event(
        id: String = "one",
        createdAt: Long = 100,
        tags: List<List<String>> = listOf(
            listOf("r", source), listOf("url", copy), listOf("tool", "naan"),
            listOf("archived-at", "90"),
        ),
    ) = Nip01Event(id, "author", createdAt, WebArchives.KIND, tags, "", "")

    @Test
    fun lookupStripsOnlyFragmentAndKeepsQueryIdentity() {
        val filter = WebArchives.filter(" $source#section ")!!
        assertEquals(4554, filter.getJSONArray("kinds").getInt(0))
        assertEquals(listOf(source), filter.getJSONArray("#r").let { (0 until it.length()).map(it::getString) })
        assertTrue(WebArchives.receipts(listOf(event()), "$source#section").isNotEmpty())
        for (different in listOf(
            source.replace("https:", "http:"),
            source.replace("one", "two"),
            source.replace("q=one&lang=en", "lang=en&q=one"),
            source.replace("/article?", "/article/?"),
        )) assertTrue(WebArchives.receipts(listOf(event()), different).isEmpty())
    }

    @Test
    fun invalidSourcesNeverProduceQueries() {
        for (url in listOf("nostr:note1abc", "javascript:alert(1)", "https:///path", "https://user:pass@example.com/a", "garbage")) {
            assertNull(WebArchives.filter(url))
        }
    }

    @Test
    fun parsesDateAttributionAndDeduplicatedMirrors() {
        val receipt = WebArchives.receipts(listOf(event(tags = event().tags + listOf(
            listOf("url", mirror), listOf("url", copy),
        ))), source).single()
        assertTrue(receipt.naan)
        assertEquals(90L, receipt.archivedAt)
        assertEquals(listOf(copy, mirror), receipt.urls)
    }

    @Test
    fun rejectsWrongKindsTargetsAndUnsafeArchiveLinks() {
        val badLinks = listOf("javascript:alert(1)", "file:///tmp/a", "data:text/html,hello", "https:///a", "https://user@example.com/a", source)
        val bad = badLinks.map { event(tags = listOf(listOf("r", source), listOf("url", it))) } + listOf(
            event().copy(kind = 1),
            event(tags = listOf(listOf("r", "https://unrelated.example/"), listOf("url", copy))),
            event(tags = listOf(emptyList(), listOf("r"), listOf("url"))),
        )
        assertTrue(WebArchives.receipts(bad, source).isEmpty())
        assertEquals(listOf(mirror), WebArchives.receipts(listOf(event(tags = listOf(
            listOf("r", source), listOf("url", "javascript:alert(1)"), listOf("url", mirror),
        ))), source).single().urls)
    }

    @Test
    fun missingOrInvalidMetadataIsOptionalAndDoesNotClaimNaan() {
        for (date in listOf("", "not-a-date", "-1", "0", Long.MAX_VALUE.toString())) {
            val receipt = WebArchives.receipts(listOf(event(tags = listOf(
                listOf("r", source), listOf("url", copy), listOf("archived-at", date),
            ))), source).single()
            assertNull(receipt.archivedAt)
            assertFalse(receipt.naan)
        }
    }

    @Test
    fun newestReceiptsFirstWithDuplicatesRemovedAndBoundedChoices() {
        val events = (1..15).map { n -> event(
            id = "$n", createdAt = n.toLong(),
            tags = listOf(listOf("r", source), listOf("url", "$copy?n=$n")),
        ) }
        val receipts = WebArchives.receipts(events.reversed() + events, source)
        assertEquals(10, receipts.size)
        assertEquals("15", receipts.first().id)
        assertEquals("6", receipts.last().id)
    }
}
