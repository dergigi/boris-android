package org.dergigi.boris.ui.reader

import org.dergigi.boris.nostr.Nip19
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NeventLinksTest {
    private val eventId = "d9b0ede779a36d555784d801ba87feac8e0d66a0cfd10b227a3aa57ca5b4ba9f"
    private val note = Nip19.noteEncode(eventId)

    @Test
    fun standaloneParagraphYieldsTheRef() {
        val md = "Before\n\nnostr:$note\n\nAfter"
        val paragraph = firstParagraphContaining(md, "nostr:")
        assertEquals(eventId, standaloneEventRef(md, paragraph)?.eventId)
    }

    @Test
    fun inlineNeventStaysText() {
        val md = "See nostr:$note here"
        val paragraph = firstParagraphContaining(md, "nostr:")
        assertNull(standaloneEventRef(md, paragraph))
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
