package org.dergigi.boris.data

/**
 * Compose crashes ("Can't represent a size of N in Constraints") when a single
 * text node lays out taller than ~262k px. The reader renders each markdown
 * block as one text node, so a wall of text without blank lines can crash the
 * app (issue #131). Paragraphs beyond [MAX_LENGTH] get split into separate
 * blocks at sentence or word boundaries. [org.dergigi.boris.tts.TtsText]
 * splits the same markdown so reader nodes and TTS paragraph indices align.
 */
object LongParagraphs {
    const val MAX_LENGTH = 10_000

    fun split(markdown: String, maxLength: Int = MAX_LENGTH): String {
        if (markdown.length <= maxLength) return markdown
        val out = mutableListOf<String>()
        var inFence: String? = null
        var run = 0
        for (line in markdown.lines()) {
            val trimmed = line.trim()
            val fence = inFence
            if (fence != null) {
                if (trimmed.startsWith(fence)) inFence = null
                out += line
                continue
            }
            val fenceMarker = fenceMarker(trimmed)
            if (fenceMarker != null) {
                inFence = fenceMarker
                run = 0
                out += line
                continue
            }
            if (trimmed.isEmpty() || !accumulates(trimmed)) {
                run = 0
                out += line
                continue
            }
            // Soft-wrapped paragraph lines join into one text node; break the
            // run with a blank line before it grows past the cap.
            if (run > 0 && run + line.length > maxLength) {
                out += ""
                run = 0
            }
            if (line.length > maxLength) {
                val pieces = splitLine(line, maxLength)
                for ((index, piece) in pieces.withIndex()) {
                    if (index > 0) out += ""
                    out += piece
                }
                run = pieces.last().length + 1
            } else {
                out += line
                run += line.length + 1
            }
        }
        return out.joinToString("\n")
    }

    /** One line longer than the cap: split at sentence ends, then spaces. */
    private fun splitLine(line: String, maxLength: Int): List<String> {
        val prefix = QUOTE_PREFIX.find(line)?.value.orEmpty()
        val text = line.removePrefix(prefix)
        val budget = (maxLength - prefix.length).coerceAtLeast(1)
        val pieces = mutableListOf<String>()
        var start = 0
        while (text.length - start > budget) {
            val cut = start + breakIndex(text, start, budget)
            val piece = text.substring(start, cut).trim()
            if (piece.isNotEmpty()) pieces += prefix + piece
            start = cut
            while (start < text.length && text[start] == ' ') start++
        }
        val tail = text.substring(start).trim()
        if (tail.isNotEmpty()) pieces += prefix + tail
        return pieces.ifEmpty { listOf(line) }
    }

    private fun breakIndex(text: String, start: Int, budget: Int): Int {
        val end = start + budget
        val floor = start + budget / 2
        for (i in end - 2 downTo floor) {
            if (text[i] in SENTENCE_END && text[i + 1] == ' ') return i - start + 1
        }
        val space = text.lastIndexOf(' ', end - 1)
        if (space > floor) return space - start + 1
        return budget
    }

    /**
     * Lines that join into the surrounding text node. Headings, list items,
     * and table rows are their own (small) nodes and reset the run.
     */
    private fun accumulates(trimmed: String): Boolean =
        !trimmed.startsWith("#") &&
            !trimmed.startsWith("|") &&
            !LIST_MARK.containsMatchIn(trimmed)

    private fun fenceMarker(trimmed: String): String? = when {
        trimmed.startsWith("```") -> "```"
        trimmed.startsWith("~~~") -> "~~~"
        else -> null
    }

    private val QUOTE_PREFIX = Regex("""^\s*(?:>\s*)+""")
    private val LIST_MARK = Regex("""^\s*(?:[-*+]|\d+[.)])\s+""")
    private const val SENTENCE_END = ".!?…"
}
