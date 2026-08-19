package org.dergigi.boris.ui.reader

import androidx.compose.ui.geometry.Size
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MarkdownImagesTest {
    @Test
    fun fullWidthScalesASmallImageUpToTheColumn() {
        val box = markdownImageBox(
            container = Size(1080f, 2000f),
            intrinsic = Size(400f, 200f),
            fullWidth = true,
            maxHeightPx = 1400f,
        )
        assertEquals(1080f, box.width)
        assertEquals(540f, box.height)
    }

    @Test
    fun fullWidthScalesAWideImageDownToTheColumn() {
        val box = markdownImageBox(
            container = Size(1080f, 2000f),
            intrinsic = Size(2160f, 1080f),
            fullWidth = true,
            maxHeightPx = 1400f,
        )
        assertEquals(1080f, box.width)
        assertEquals(540f, box.height)
    }

    @Test
    fun containedKeepsIntrinsicSizeWhenItFits() {
        val box = markdownImageBox(
            container = Size(1080f, 2000f),
            intrinsic = Size(400f, 200f),
            fullWidth = false,
            maxHeightPx = 1400f,
        )
        assertEquals(400f, box.width)
        assertEquals(200f, box.height)
    }

    @Test
    fun containedCapsHeightAtSeventyPercent() {
        val box = markdownImageBox(
            container = Size(1080f, 2000f),
            intrinsic = Size(800f, 2000f),
            fullWidth = false,
            maxHeightPx = 1400f,
        )
        assertEquals(560f, box.width, 0.01f)
        assertEquals(1400f, box.height, 0.01f)
    }

    @Test
    fun standaloneParagraphYieldsTheImageUrl() {
        val md = "Before\n\n![clock](https://example.com/clock.png)\n\nAfter"
        val paragraph = firstParagraphWithImage(md)
        assertEquals("https://example.com/clock.png", standaloneMarkdownImageUrl(md, paragraph))
        assertEquals(listOf("https://example.com/clock.png"), standaloneMarkdownImageUrls(md, paragraph))
    }

    @Test
    fun adjacentStandaloneImagesYieldAllImageUrls() {
        val md = "Before\n\n![first](https://example.com/first.webp)![second](https://example.com/second.webp)\n\nAfter"
        val paragraph = firstParagraphWithImage(md)
        assertNull(standaloneMarkdownImageUrl(md, paragraph))
        assertEquals(
            listOf(
                "https://example.com/first.webp",
                "https://example.com/second.webp",
            ),
            standaloneMarkdownImageUrls(md, paragraph),
        )
    }

    @Test
    fun adjacentStandaloneImagesFallbackWhenAnyDestinationIsMissing() {
        val md = "Before\n\n![first](https://example.com/first.webp)![second]()\n\nAfter"
        val paragraph = firstParagraphWithImage(md)
        assertNull(standaloneMarkdownImageUrl(md, paragraph))
        assertEquals(emptyList<String>(), standaloneMarkdownImageUrls(md, paragraph))
    }

    @Test
    fun mixedParagraphKeepsTheImageInline() {
        val md = "See ![clock](https://example.com/clock.png) here"
        val paragraph = firstParagraphWithImage(md)
        assertNull(standaloneMarkdownImageUrl(md, paragraph))
        assertEquals(emptyList<String>(), standaloneMarkdownImageUrls(md, paragraph))
        assertEquals(
            "https://example.com/clock.png",
            markdownImageDestination(md, paragraph.children.first { it.type == MarkdownElementTypes.IMAGE }),
        )
    }

    private fun firstParagraphWithImage(markdown: String): ASTNode {
        val root = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(markdown)
        fun walk(node: ASTNode): ASTNode? {
            if (node.type == MarkdownElementTypes.PARAGRAPH &&
                node.children.any { it.type == MarkdownElementTypes.IMAGE }
            ) {
                return node
            }
            return node.children.firstNotNullOfOrNull(::walk)
        }
        return checkNotNull(walk(root))
    }
}
