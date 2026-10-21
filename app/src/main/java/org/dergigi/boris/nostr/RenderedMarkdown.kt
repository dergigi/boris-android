package org.dergigi.boris.nostr

data class RenderedMarkdownIndex(
    val text: String,
    val sourceOffsets: List<Int>,
)

object RenderedMarkdown {
    fun index(markdown: String): RenderedMarkdownIndex {
        val text = StringBuilder()
        val offsets = mutableListOf<Int>()
        var lineStart = 0
        markdown.splitToSequence('\n').forEach { line ->
            val marker = MARKDOWN_LIST_MARKER.find(line)
            val contentStart = marker?.let { it.range.last + 1 } ?: 0
            appendInline(line, contentStart, line.length, lineStart, text, offsets)
            appendRenderedSpace(text, offsets, lineStart + line.length)
            lineStart += line.length + 1
        }
        while (text.isNotEmpty() && text.last() == ' ') {
            text.deleteAt(text.lastIndex)
            offsets.removeAt(offsets.lastIndex)
        }
        return RenderedMarkdownIndex(text.toString(), offsets)
    }

    fun normalize(markdown: String): String =
        QuoteMatch.normalizeWhitespace(index(markdown).text)

    private fun appendInline(
        line: String,
        start: Int,
        end: Int,
        lineStart: Int,
        text: StringBuilder,
        offsets: MutableList<Int>,
    ) {
        var index = start
        while (index < end) {
            val char = line[index]
            val sourceOffset = lineStart + index
            if (char.isWhitespace()) {
                appendRenderedSpace(text, offsets, sourceOffset)
                index++
                continue
            }
            val imageLabelStart = if (char == '!' && index + 1 < end && line[index + 1] == '[') index + 2 else -1
            val linkLabelStart = if (char == '[') index + 1 else imageLabelStart
            if (linkLabelStart >= 0) {
                val closeBracket = line.indexOf(']', linkLabelStart).takeIf { it >= 0 && it < end }
                val openParen = closeBracket?.plus(1)?.takeIf { it < end && line[it] == '(' }
                val closeParen = openParen?.let { line.indexOf(')', it + 1).takeIf { close -> close >= 0 && close < end } }
                if (closeBracket != null && closeParen != null) {
                    appendInline(line, linkLabelStart, closeBracket, lineStart, text, offsets)
                    index = closeParen + 1
                    continue
                }
            }
            when (char) {
                '*', '_', '`' -> index++
                '~' -> index += if (index + 1 < end && line[index + 1] == '~') 2 else 1
                else -> {
                    text.append(char)
                    offsets.add(sourceOffset)
                    index++
                }
            }
        }
    }

    private fun appendRenderedSpace(text: StringBuilder, offsets: MutableList<Int>, sourceOffset: Int) {
        if (text.isEmpty() || text.last() == ' ') return
        text.append(' ')
        offsets.add(sourceOffset)
    }
}

private val MARKDOWN_LIST_MARKER = Regex("""^\s*(?:[-+*]|\d+[.)])\s+""")
