package org.dergigi.boris.ui.reader

import org.dergigi.boris.data.MarkdownInline
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import kotlin.math.abs

data class ArticleOutlineItem(
    val id: String,
    val level: Int,
    val title: String,
    val startOffset: Int,
)

object ArticleOutline {
    const val ID_PREFIX = "outline:"

    fun parse(markdown: String): List<ArticleOutlineItem> {
        if (markdown.isBlank()) return emptyList()
        val root = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(markdown)
        val items = mutableListOf<ArticleOutlineItem>()
        walk(root, markdown, items)
        return items
    }

    fun idAt(items: List<ArticleOutlineItem>, startOffset: Int): String? =
        items.firstOrNull { it.startOffset == startOffset }?.id

    fun idForHeading(items: List<ArticleOutlineItem>, startOffset: Int, title: String): String? {
        idAt(items, startOffset)?.let { return it }
        val cleaned = cleanTitle(title)
        if (cleaned.isBlank()) return null
        return items
            .filter { it.title.equals(cleaned, ignoreCase = true) }
            .minByOrNull { abs(it.startOffset - startOffset) }
            ?.id
    }

    fun isId(id: String): Boolean = id.startsWith(ID_PREFIX)

    fun painted(id: String, text: String): PaintedHighlight? {
        if (id.isBlank() || text.isBlank()) return null
        return PaintedHighlight(id = id, quote = text, mine = false, outline = true)
    }

    fun activeId(
        items: List<ArticleOutlineItem>,
        yInViewport: (String) -> Float?,
        threshold: Float,
    ): String? {
        var current = items.firstOrNull()?.id
        for (item in items) {
            val y = yInViewport(item.id) ?: continue
            if (y <= threshold) current = item.id else break
        }
        return current
    }

    internal fun cleanTitle(raw: String): String {
        var text = raw.trim()
            .replace(LEADING_HASHES, "")
            .replace(TRAILING_HASHES, "")
        text = MarkdownInline.plain(text)
        text = EMPHASIS.replace(text, "")
        return WHITESPACE.replace(text, " ").trim()
    }

    private fun walk(node: ASTNode, markdown: String, items: MutableList<ArticleOutlineItem>) {
        val level = headingLevel(node.type)
        if (level != null) {
            val title = titleOf(markdown, node)
            if (title.isNotBlank()) {
                items += ArticleOutlineItem(
                    id = "$ID_PREFIX${items.size}",
                    level = level,
                    title = title,
                    startOffset = node.startOffset,
                )
            }
            return
        }
        for (child in node.children) walk(child, markdown, items)
    }

    private fun headingLevel(type: IElementType): Int? = when (type) {
        MarkdownElementTypes.ATX_1, MarkdownElementTypes.SETEXT_1 -> 1
        MarkdownElementTypes.ATX_2, MarkdownElementTypes.SETEXT_2 -> 2
        MarkdownElementTypes.ATX_3 -> 3
        MarkdownElementTypes.ATX_4 -> 4
        MarkdownElementTypes.ATX_5 -> 5
        MarkdownElementTypes.ATX_6 -> 6
        else -> null
    }

    private fun titleOf(markdown: String, node: ASTNode): String {
        val content = node.children.firstOrNull { child ->
            child.type == MarkdownTokenTypes.ATX_CONTENT ||
                child.type == MarkdownTokenTypes.SETEXT_CONTENT
        }
        val raw = if (content != null) {
            markdown.substring(content.startOffset, content.endOffset)
        } else {
            markdown.substring(node.startOffset, node.endOffset)
        }
        return cleanTitle(raw)
    }

    private val LEADING_HASHES = Regex("^#{1,6}\\s+")
    private val TRAILING_HASHES = Regex("\\s+#*\\s*$")
    private val EMPHASIS = Regex("[*_~`]+")
    private val WHITESPACE = Regex("\\s+")
}
