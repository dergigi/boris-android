package org.dergigi.boris.tts

import org.dergigi.boris.data.Footnotes
import org.dergigi.boris.data.MarkdownInline
import org.dergigi.boris.data.NostrMentions
import org.dergigi.boris.data.ReadableContent

/** Turns an article into speakable paragraphs and maps reading position to a start index. */
object TtsText {
    fun paragraphs(content: ReadableContent): List<String> {
        val out = mutableListOf<String>()
        content.title?.let { addCleaned(out, it) }
        content.summary?.let { addCleaned(out, it) }
        for (block in splitMarkdownBlocks(NostrMentions.rewrite(Footnotes.expand(content.body)))) {
            addCleaned(out, block)
        }
        return out
    }

    /** D-14: fractions below the noise floor start from the top. */
    fun startIndex(fraction: Float, count: Int): Int {
        if (count <= 0) return 0
        if (fraction < 0.01f) return 0
        return (fraction * count).toInt().coerceIn(0, count - 1)
    }

    fun startIndexForSelection(
        content: ReadableContent,
        ownerText: String,
        selectedText: String,
    ): Int {
        val paragraphs = paragraphs(content)
        if (paragraphs.isEmpty()) return 0
        val owner = clean(ownerText)?.matchKey().orEmpty()
        val selected = clean(selectedText)?.matchKey().orEmpty()
        val ownerIndex = owner
            .takeIf { it.isNotBlank() }
            ?.let { key -> paragraphs.indexOfFirst { it.matchKey().contains(key) } }
            ?: -1
        if (ownerIndex >= 0) return ownerIndex
        val selectedIndex = selected
            .takeIf { it.isNotBlank() }
            ?.let { key -> paragraphs.indexOfFirst { it.matchKey().contains(key) } }
            ?: -1
        return selectedIndex.coerceAtLeast(0)
    }

    fun applySentenceStart(
        paragraphs: List<String>,
        startIndex: Int,
        selectedText: String,
        ownerText: String = "",
        ownerOffset: Int = 0,
    ): List<String> {
        if (paragraphs.isEmpty()) return paragraphs
        val index = startIndex.coerceIn(0, paragraphs.lastIndex)
        val trimmed = fromSentence(paragraphs[index], selectedText, ownerText, ownerOffset)
        if (trimmed == paragraphs[index]) return paragraphs
        return paragraphs.toMutableList().also { it[index] = trimmed }
    }

    fun fromSentence(
        paragraph: String,
        selectedText: String,
        ownerText: String = "",
        ownerOffset: Int = 0,
    ): String {
        val parts = sentences(paragraph)
        if (parts.size <= 1) return paragraph
        val ownerSentences = sentences(ownerText)
        val byOffset = if (ownerText.isNotBlank() && ownerSentences.size == parts.size) {
            sentenceIndex(ownerText, ownerOffset)
        } else {
            -1
        }
        val index = when {
            byOffset in 1 until parts.size -> byOffset
            else -> {
                val selected = clean(selectedText)?.matchKey().orEmpty()
                if (selected.isBlank()) return paragraph
                parts.indexOfFirst { it.matchKey().contains(selected) }
            }
        }
        if (index <= 0) return paragraph
        return parts.drop(index).joinToString(" ")
    }

    fun sentences(text: String): List<String> =
        text.split(SENTENCE_BREAK)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    fun startIndexForMarkdownOffset(content: ReadableContent, markdownOffset: Int): Int? {
        var index = 0
        content.title?.let { if (clean(it) != null) index++ }
        content.summary?.let { if (clean(it) != null) index++ }
        val markdown = NostrMentions.rewrite(Footnotes.expand(content.body))
        for (block in splitMarkdownBlocksWithRanges(markdown)) {
            if (clean(block.text) == null) continue
            if (markdownOffset in block.start..block.end) return index
            index++
        }
        return null
    }

    /**
     * Splits one logical paragraph into speakable sub-chunks that fit the engine's
     * input limit. Splits on sentence punctuation first, then on spaces.
     */
    fun chunks(text: String, maxLength: Int): List<String> {
        if (maxLength <= 0 || text.length <= maxLength) return listOf(text)
        val pieces = mutableListOf<String>()
        val current = StringBuilder()
        fun flush() {
            if (current.isNotBlank()) pieces += current.toString()
            current.setLength(0)
        }
        for (sentence in text.split(SENTENCE_BREAK)) {
            if (sentence.length > maxLength) {
                flush()
                splitOnSpaces(sentence, maxLength, pieces)
                continue
            }
            if (current.isNotEmpty() && current.length + 1 + sentence.length > maxLength) flush()
            if (current.isNotEmpty()) current.append(' ')
            current.append(sentence)
        }
        flush()
        return pieces.ifEmpty { listOf(text.take(maxLength)) }
    }

    fun speechUnits(text: String, maxLength: Int): List<String> {
        val units = mutableListOf<String>()
        for (sentence in sentences(text)) {
            if (maxLength > 0 && sentence.length > maxLength) {
                splitOnSpaces(sentence, maxLength, units)
            } else {
                units += sentence
            }
        }
        return units.ifEmpty {
            if (maxLength > 0) listOf(text.take(maxLength)) else listOf(text)
        }
    }

    fun splitMarkdownBlocks(markdown: String): List<String> {
        return splitMarkdownBlocksWithRanges(markdown).map { it.text }
    }

    private fun splitMarkdownBlocksWithRanges(markdown: String): List<MarkdownBlock> {
        val rangedBlocks = mutableListOf<MarkdownBlock>()
        val current = StringBuilder()
        var currentStart = 0
        var currentEnd = 0
        var droppingReferenceDefinition = false
        var inFence: String? = null
        fun flush() {
            if (current.isNotBlank()) {
                val text = current.toString()
                rangedBlocks += MarkdownBlock(text, currentStart, currentEnd)
            }
            current.setLength(0)
        }
        var lineStart = 0
        for (line in markdown.lines()) {
            val trimmed = line.trim()
            val fence = inFence
            if (fence != null) {
                if (trimmed.startsWith(fence)) inFence = null
                lineStart += line.length + 1
                continue
            }
            val fenceStart = fenceMarker(trimmed)
            if (fenceStart != null) {
                flush()
                inFence = fenceStart
                lineStart += line.length + 1
                continue
            }
            if (droppingReferenceDefinition) {
                if (isReferenceDefinitionContinuation(line)) {
                    lineStart += line.length + 1
                    continue
                }
                droppingReferenceDefinition = false
            }
            when {
                trimmed.isEmpty() -> flush()
                isReferenceDefinition(line) -> {
                    flush()
                    droppingReferenceDefinition = true
                }
                isTableRow(trimmed) || isImageOnly(trimmed) || isRule(trimmed) -> flush()
                isHeading(trimmed) || isListItem(trimmed) -> {
                    flush()
                    rangedBlocks += MarkdownBlock(
                        text = trimmed,
                        start = lineStart + line.indexOf(trimmed),
                        end = lineStart + line.length,
                    )
                }
                else -> {
                    if (current.isEmpty()) currentStart = lineStart + line.indexOf(trimmed)
                    if (current.isNotEmpty()) current.append(' ')
                    current.append(trimmed)
                    currentEnd = lineStart + line.length
                }
            }
            lineStart += line.length + 1
        }
        flush()
        return rangedBlocks
    }

    private fun sentenceIndex(text: String, offset: Int): Int {
        if (text.isEmpty()) return 0
        val clamped = offset.coerceIn(0, text.length)
        var index = 0
        for (match in SENTENCE_BREAK.findAll(text)) {
            if (clamped < match.range.last + 1) return index
            index++
        }
        return index
    }

    private fun addCleaned(out: MutableList<String>, raw: String) {
        val text = clean(raw) ?: return
        out += text
    }

    private fun clean(raw: String): String? {
        var text = raw
        text = HEADING_MARK.replace(text, "")
        text = QUOTE_MARK.replace(text, "")
        text = LIST_MARK.replace(text, "")
        text = REFERENCE_LINK.replace(text) { it.groupValues[1] }
        text = MARKDOWN_IMAGE.replace(text, " ")
        text = MarkdownInline.plain(text)
        text = BARE_URL.replace(text, " ")
        text = EMPHASIS.replace(text, "")
        text = WHITESPACE.replace(text, " ").trim()
        return text.takeIf { it.isNotEmpty() }
    }

    private fun String.matchKey(): String =
        WHITESPACE.replace(this, " ").trim().lowercase()

    private fun isHeading(line: String): Boolean = line.startsWith("#")

    private fun isListItem(line: String): Boolean = LIST_MARK.containsMatchIn(line)

    private fun isTableRow(line: String): Boolean =
        line.startsWith("|") || (line.contains("|") && TABLE_SEPARATOR.matches(line))

    private fun isImageOnly(line: String): Boolean = IMAGE_ONLY.matches(line)

    private fun isRule(line: String): Boolean = RULE.matches(line)

    private fun isReferenceDefinition(line: String): Boolean = REFERENCE_DEFINITION.matches(line)

    private fun fenceMarker(line: String): String? = when {
        line.startsWith("```") -> "```"
        line.startsWith("~~~") -> "~~~"
        else -> null
    }

    private fun isReferenceDefinitionContinuation(line: String): Boolean {
        val trimmed = line.trim()
        return line.firstOrNull()?.isWhitespace() == true &&
            (trimmed.startsWith("\"") || trimmed.startsWith("'") || trimmed.startsWith("("))
    }

    private fun splitOnSpaces(sentence: String, maxLength: Int, out: MutableList<String>) {
        var start = 0
        while (start < sentence.length) {
            var end = minOf(start + maxLength, sentence.length)
            if (end < sentence.length) {
                val space = sentence.lastIndexOf(' ', end)
                if (space > start) end = space
            }
            val piece = sentence.substring(start, end).trim()
            if (piece.isNotEmpty()) out += piece
            start = end
            while (start < sentence.length && sentence[start] == ' ') start++
        }
    }

    private val SENTENCE_BREAK = Regex("""(?<=[.!?…])\s+""")
    private val HEADING_MARK = Regex("""(?m)^#{1,6}\s+""")
    private val QUOTE_MARK = Regex("""(?m)^>\s?""")
    private val LIST_MARK = Regex("""^\s*(?:[-*+]|\d+[.)])\s+""")
    private val REFERENCE_LINK = Regex("""\[([^\]\n]+)]\[[^\]\n]*]""")
    private val REFERENCE_DEFINITION = Regex("""^[ \t]{0,3}\[(?!\^)[^\]\n]+]:[ \t]*.*$""")
    private val MARKDOWN_IMAGE = Regex("""!\[[^\]\n]*]\([^)\s]+(?:\s+"[^"]*")?\)""")
    private val BARE_URL = Regex("""(?i)\b(?:https?://|www\.)[^\s<>)]+""")
    private val EMPHASIS = Regex("""[*_~`]+""")
    private val WHITESPACE = Regex("""\s+""")
    private val TABLE_SEPARATOR = Regex("""^[\s|:\-]+$""")
    private val IMAGE_ONLY = Regex("""^(?:!\[[^\]]*]\([^)\s]+\)\s*)+$""")
    private val RULE = Regex("""^(?:-{3,}|\*{3,}|_{3,})$""")

    private data class MarkdownBlock(
        val text: String,
        val start: Int,
        val end: Int,
    )
}
