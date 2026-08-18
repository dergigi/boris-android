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

    fun splitMarkdownBlocks(markdown: String): List<String> {
        val withoutFences = FENCE.replace(markdown, "\n")
        val blocks = mutableListOf<String>()
        val current = StringBuilder()
        var droppingReferenceDefinition = false
        fun flush() {
            if (current.isNotBlank()) blocks += current.toString()
            current.setLength(0)
        }
        for (line in withoutFences.lines()) {
            val trimmed = line.trim()
            if (droppingReferenceDefinition) {
                if (isReferenceDefinitionContinuation(line)) continue
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
                    blocks += trimmed
                }
                else -> {
                    if (current.isNotEmpty()) current.append(' ')
                    current.append(trimmed)
                }
            }
        }
        flush()
        return blocks
    }

    private fun addCleaned(out: MutableList<String>, raw: String) {
        var text = raw
        text = HEADING_MARK.replace(text, "")
        text = QUOTE_MARK.replace(text, "")
        text = LIST_MARK.replace(text, "")
        text = REFERENCE_LINK.replace(text) { it.groupValues[1] }
        text = MarkdownInline.plain(text)
        text = EMPHASIS.replace(text, "")
        text = WHITESPACE.replace(text, " ").trim()
        if (text.isNotEmpty()) out += text
    }

    private fun isHeading(line: String): Boolean = line.startsWith("#")

    private fun isListItem(line: String): Boolean = LIST_MARK.containsMatchIn(line)

    private fun isTableRow(line: String): Boolean =
        line.startsWith("|") || (line.contains("|") && TABLE_SEPARATOR.matches(line))

    private fun isImageOnly(line: String): Boolean = IMAGE_ONLY.matches(line)

    private fun isRule(line: String): Boolean = RULE.matches(line)

    private fun isReferenceDefinition(line: String): Boolean = REFERENCE_DEFINITION.matches(line)

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

    private val FENCE = Regex("""(?s)(?:```|~~~)[^\n]*\n.*?(?:```|~~~)""")
    private val SENTENCE_BREAK = Regex("""(?<=[.!?…])\s+""")
    private val HEADING_MARK = Regex("""(?m)^#{1,6}\s+""")
    private val QUOTE_MARK = Regex("""(?m)^>\s?""")
    private val LIST_MARK = Regex("""^\s*(?:[-*+]|\d+[.)])\s+""")
    private val REFERENCE_LINK = Regex("""\[([^\]\n]+)]\[[^\]\n]*]""")
    private val REFERENCE_DEFINITION = Regex("""^[ \t]{0,3}\[(?!\^)[^\]\n]+]:[ \t]*.*$""")
    private val EMPHASIS = Regex("""[*_~`]+""")
    private val WHITESPACE = Regex("""\s+""")
    private val TABLE_SEPARATOR = Regex("""^[\s|:\-]+$""")
    private val IMAGE_ONLY = Regex("""^(?:!\[[^\]]*]\([^)\s]+\)\s*)+$""")
    private val RULE = Regex("""^(?:-{3,}|\*{3,}|_{3,})$""")
}
