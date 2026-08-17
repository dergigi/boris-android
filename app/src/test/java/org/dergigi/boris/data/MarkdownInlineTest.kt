package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownInlineTest {
    @Test
    fun flattensMarkdownLinksToLabels() {
        val raw =
            """remember that [leaving an SSD unpowered for too long](https://www.howtogeek.com/your-unpowered-ssd-is-a-ticking-time-bomb/) can be a huge problem"""
        val (plain, links) = MarkdownInline.flatten(raw)
        assertEquals(
            "remember that leaving an SSD unpowered for too long can be a huge problem",
            plain,
        )
        assertEquals(1, links.size)
        assertEquals("https://www.howtogeek.com/your-unpowered-ssd-is-a-ticking-time-bomb/", links[0].url)
        assertEquals(
            "leaving an SSD unpowered for too long",
            plain.substring(links[0].start, links[0].end),
        )
    }

    @Test
    fun flattensImagesAndAutolinks() {
        assertEquals(
            "see chart here",
            MarkdownInline.plain("see ![chart](https://example.com/a.png) here"),
        )
        val (plain, links) = MarkdownInline.flatten("go <https://example.com> now")
        assertEquals("go https://example.com now", plain)
        assertEquals("https://example.com", links.single().url)
    }

    @Test
    fun leavesPlainTextAlone() {
        assertEquals("no links here", MarkdownInline.plain("no links here"))
        assertEquals(emptyList<MarkdownInline.Link>(), MarkdownInline.flatten("no links here").second)
    }
}
