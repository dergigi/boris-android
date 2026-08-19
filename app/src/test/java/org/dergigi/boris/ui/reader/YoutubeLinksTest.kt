package org.dergigi.boris.ui.reader

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YoutubeLinksTest {
    @Test
    fun parsesWatchVShortsAndYoutuBe() {
        assertEquals("7s8glZ-efMg", youtubeVideoId("https://youtu.be/7s8glZ-efMg?si=abc"))
        assertEquals("7s8glZ-efMg", youtubeVideoId("https://www.youtube.com/watch?v=7s8glZ-efMg"))
        assertEquals("7s8glZ-efMg", youtubeVideoId("https://m.youtube.com/watch?v=7s8glZ-efMg&t=12s"))
        assertEquals("7s8glZ-efMg", youtubeVideoId("https://youtube.com/shorts/7s8glZ-efMg"))
        assertEquals("7s8glZ-efMg", youtubeVideoId("https://www.youtube.com/embed/7s8glZ-efMg"))
        assertEquals("7s8glZ-efMg", youtubeVideoId("https://www.youtube.com/live/7s8glZ-efMg"))
    }

    @Test
    fun rejectsNonYoutubeUrls() {
        assertNull(youtubeVideoId("https://vimeo.com/123456"))
        assertNull(youtubeVideoId("https://example.com/watch?v=7s8glZ-efMg"))
        assertNull(youtubeVideoId("https://youtube.com/watch?v=short"))
    }

    @Test
    fun previewUsesHqThumbnail() {
        val preview = youtubePreview("https://youtu.be/7s8glZ-efMg?si=abc")
        assertEquals("https://youtu.be/7s8glZ-efMg?si=abc", preview?.watchUrl)
        assertEquals("https://i.ytimg.com/vi/7s8glZ-efMg/hqdefault.jpg", preview?.thumbnailUrl)
    }

    @Test
    fun standaloneParagraphYieldsThePreview() {
        val md = "Before\n\nhttps://youtu.be/7s8glZ-efMg?si=QpiWYDXqNR17y00b\n\nAfter"
        val paragraph = firstParagraphContaining(md, "youtu")
        val preview = standaloneYoutubePreview(md, paragraph)
        assertEquals("https://youtu.be/7s8glZ-efMg?si=QpiWYDXqNR17y00b", preview?.watchUrl)
    }

    @Test
    fun standaloneMarkdownLinkYieldsThePreview() {
        val md = "[Watch](https://www.youtube.com/watch?v=7s8glZ-efMg)"
        assertEquals(
            "https://www.youtube.com/watch?v=7s8glZ-efMg",
            standaloneYoutubePreview(md)?.watchUrl,
        )
    }

    @Test
    fun inlineYoutubeLinkStaysText() {
        val md = "See https://youtu.be/7s8glZ-efMg here"
        val paragraph = firstParagraphContaining(md, "youtu")
        assertNull(standaloneYoutubePreview(md, paragraph))
        assertNull(standaloneYoutubePreview("See [this](https://youtu.be/7s8glZ-efMg) clip"))
    }

    private fun firstParagraphContaining(markdown: String, needle: String): ASTNode {
        val root = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(markdown)
        fun walk(node: ASTNode): ASTNode? {
            if (node.type == MarkdownElementTypes.PARAGRAPH &&
                markdown.substring(node.startOffset, node.endOffset).contains(needle)
            ) {
                return node
            }
            return node.children.firstNotNullOfOrNull(::walk)
        }
        return checkNotNull(walk(root))
    }
}
