package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneOffset

class PublishedTimeTest {
    @Test
    fun parseIsoInstant() {
        assertEquals(1_705_312_800L, PublishedTime.parse("2024-01-15T10:00:00Z"))
    }

    @Test
    fun parseUnixSeconds() {
        assertEquals(1_705_312_800L, PublishedTime.parse("1705312800"))
    }

    @Test
    fun parseDateOnly() {
        assertEquals(1_705_276_800L, PublishedTime.parse("2024-01-15"))
    }

    @Test
    fun parseRejectsGarbage() {
        assertNull(PublishedTime.parse(""))
        assertNull(PublishedTime.parse("sometime last week"))
        assertNull(PublishedTime.parse("12"))
    }

    @Test
    fun fromJinaHeader() {
        val text = """
            Title: Hello
            URL Source: https://example.com/hello
            Published Time: 2024-01-15T10:00:00.000Z
            Markdown Content:
            Body
        """.trimIndent()
        assertEquals(1_705_312_800L, PublishedTime.fromJinaHeader(text))
    }

    @Test
    fun fromJinaHeaderMissing() {
        val text = """
            Title: Hello
            URL Source: https://example.com/hello
            Markdown Content:
            Body
        """.trimIndent()
        assertNull(PublishedTime.fromJinaHeader(text))
    }

    @Test
    fun fromHtmlMeta() {
        val html = """
            <html><head>
            <meta property="article:published_time" content="2024-01-15T10:00:00Z">
            </head></html>
        """.trimIndent()
        assertEquals(1_705_312_800L, PublishedTime.fromHtml(html))
    }

    @Test
    fun labelUsesUtcDate() {
        assertEquals("Jan 15, 2024", PublishedTime.label(1_705_312_800L, ZoneOffset.UTC))
    }
}
