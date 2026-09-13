package org.dergigi.boris.data

/** Repairs common emphasis spacing glitches produced by article sources. */
object MarkdownSpacing {
    fun normalize(markdown: String): String {
        if ('*' !in markdown) return markdown
        val out = StringBuilder(markdown.length)
        var inFence: String? = null
        for (line in markdown.lines()) {
            val trimmed = line.trimStart()
            val fence = inFence
            if (fence != null) {
                out.append(line)
                if (trimmed.startsWith(fence)) inFence = null
            } else {
                val marker = fenceMarker(trimmed)
                if (marker != null) {
                    inFence = marker
                    out.append(line)
                } else {
                    out.append(normalizeLine(line))
                }
            }
            out.append('\n')
        }
        if (out.isNotEmpty()) out.setLength(out.length - 1)
        return out.toString()
    }

    private fun normalizeLine(line: String): String =
        line
            .replace(OPENING_ATTACHED_TO_PREVIOUS_WORD) { " *" }
            .replace(TRAILING_SPACE_BEFORE_CLOSING_MARKER) { match ->
                "*${match.groupValues[1]}* "
            }

    private fun fenceMarker(trimmed: String): String? = when {
        trimmed.startsWith("```") -> "```"
        trimmed.startsWith("~~~") -> "~~~"
        else -> null
    }

    private val OPENING_ATTACHED_TO_PREVIOUS_WORD =
        Regex("""(?<=[\p{L}\p{N}”"'’)])(?<!\*)\*(?!\*)\s+(?=\S[^\*\n]{0,120}(?<!\*)\*(?!\*))""")
    private val TRAILING_SPACE_BEFORE_CLOSING_MARKER =
        Regex("""(?<!\*)\*(?!\*)([^\*\n]{1,120}?\S)\s+(?<!\*)\*(?!\*)(?=\S)""")
}
