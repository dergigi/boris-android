package org.dergigi.boris.tts

import org.dergigi.boris.data.ArticleUrl
import org.dergigi.boris.data.ContinueReading
import org.dergigi.boris.data.Footnotes
import org.dergigi.boris.data.LongParagraphs
import org.dergigi.boris.data.MarkdownInline
import org.dergigi.boris.data.NostrMentions
import org.dergigi.boris.data.ReadableContent

/** Turns an article into speakable paragraphs and maps reading position to a start index. */
object TtsText {
    fun paragraphs(content: ReadableContent): List<String> {
        val out = mutableListOf<String>()
        content.title?.let { addCleaned(out, it) }
        content.summary?.let { addCleaned(out, it) }
        for (block in splitMarkdownBlocks(rewrittenMarkdown(content))) {
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

    /** Home / resume: mid-article progress continues; finished or unread starts at the top. */
    fun listenStartIndex(fraction: Float, count: Int): Int {
        if (!ContinueReading.inProgress(fraction)) return 0
        return startIndex(fraction, count)
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

    fun sentences(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val parts = mutableListOf<String>()
        var start = 0
        var i = 0
        while (i < text.length) {
            val boundaryEnd = sentenceBoundaryEnd(text, i)
            if (boundaryEnd != null) {
                val piece = text.substring(start, boundaryEnd).trim()
                if (piece.isNotEmpty()) parts += piece
                i = boundaryEnd
                while (i < text.length && text[i].isWhitespace()) i++
                start = i
                continue
            }
            i++
        }
        val tail = text.substring(start).trim()
        if (tail.isNotEmpty()) parts += tail
        return parts
    }

    fun sentenceIndexAt(text: String, offset: Int): Int {
        if (text.isEmpty()) return 0
        val clamped = offset.coerceIn(0, text.length)
        var index = 0
        var i = 0
        while (i < text.length) {
            if (clamped <= i) return index
            val boundaryEnd = sentenceBoundaryEnd(text, i)
            if (boundaryEnd != null) {
                if (clamped < boundaryEnd) return index
                index++
                i = boundaryEnd
                while (i < text.length && text[i].isWhitespace()) i++
                continue
            }
            i++
        }
        return index
    }

    fun chunkStart(text: String, maxLength: Int, chunkIndex: Int): Int {
        if (chunkIndex <= 0) return 0
        val parts = chunks(text, maxLength)
        if (chunkIndex >= parts.size) return text.length
        var searchFrom = 0
        for (i in 0 until chunkIndex) {
            val found = text.indexOf(parts[i], searchFrom)
            if (found < 0) return searchFrom
            searchFrom = found + parts[i].length
        }
        val found = text.indexOf(parts[chunkIndex], searchFrom)
        return if (found >= 0) found else searchFrom
    }

    fun startIndexForMarkdownOffset(content: ReadableContent, markdownOffset: Int): Int? =
        startIndexForMarkdownOffset(content, rewrittenMarkdown(content), markdownOffset)

    /**
     * Mirrors the reader's markdown pipeline (minus event-ref embeds) so TTS
     * paragraph indices line up with rendered nodes, including the long
     * paragraph splits from [LongParagraphs] (issue #131).
     */
    private fun rewrittenMarkdown(content: ReadableContent): String =
        LongParagraphs.split(NostrMentions.rewrite(Footnotes.expand(content.body)))

    fun startIndexForMarkdownOffset(
        content: ReadableContent,
        markdown: String,
        markdownOffset: Int,
    ): Int? = markdownOffsetIndex(content, markdown).startIndexFor(markdownOffset)

    /**
     * Walks the article once (regex-heavy) so that per-node offset lookups during
     * composition are cheap. Computing this per markdown node froze the reader
     * on long articles (ANR, issue #77).
     */
    fun markdownOffsetIndex(content: ReadableContent, markdown: String): MarkdownOffsetIndex {
        var index = 0
        content.title?.let { if (clean(it) != null) index++ }
        content.summary?.let { if (clean(it) != null) index++ }
        val ranges = mutableListOf<MarkdownOffsetIndex.Range>()
        for (block in splitMarkdownBlocksWithRanges(markdown)) {
            if (clean(block.text) == null) continue
            ranges += MarkdownOffsetIndex.Range(block.start, block.end, index)
            index++
        }
        return MarkdownOffsetIndex(ranges)
    }

    class MarkdownOffsetIndex internal constructor(private val ranges: List<Range>) {
        fun startIndexFor(markdownOffset: Int): Int? =
            ranges.firstOrNull { markdownOffset in it.start..it.end }?.index

        internal data class Range(val start: Int, val end: Int, val index: Int)
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
        for (sentence in sentences(text)) {
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

    fun spokenDurationMs(text: String, rate: Double): Long {
        val words = text.split(Regex("\\s+")).count { it.isNotBlank() }.coerceAtLeast(1)
        val wpm = FOLLOW_ALONG_WPM * rate.coerceIn(0.4, 4.0)
        return (words / wpm * 60_000.0).toLong().coerceIn(MIN_SENTENCE_MS, MAX_PARAGRAPH_MS)
    }

    fun sentenceIndexForProgress(paragraph: String, elapsedMs: Long, rate: Double): Int {
        val total = spokenDurationMs(paragraph, rate).coerceAtLeast(1)
        val offset = ((elapsedMs.toDouble() / total) * paragraph.length)
            .toInt()
            .coerceIn(0, (paragraph.length - 1).coerceAtLeast(0))
        return sentenceIndexAt(paragraph, offset)
    }

    /**
     * Cumulative delays (ms) at which follow-along should leave each sentence
     * when the engine speaks a whole paragraph and never fires onRangeStart.
     */
    fun sentenceAdvanceAtMs(paragraph: String, rate: Double): List<Long> {
        val parts = sentences(paragraph)
        if (parts.size <= 1) return emptyList()
        val wpm = FOLLOW_ALONG_WPM * rate.coerceIn(0.4, 4.0)
        val msPerWord = 60_000.0 / wpm
        var at = 0L
        return parts.dropLast(1).map { sentence ->
            val words = sentence.split(Regex("\\s+")).count { it.isNotBlank() }.coerceAtLeast(1)
            at += (words * msPerWord).toLong().coerceIn(MIN_SENTENCE_MS, MAX_SENTENCE_MS)
            at
        }
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

    private fun sentenceIndex(text: String, offset: Int): Int = sentenceIndexAt(text, offset)

    private fun sentenceBoundaryEnd(text: String, punct: Int): Int? {
        val mark = text[punct]
        if (mark != '.' && mark != '!' && mark != '?' && mark != '…') return null
        var after = punct + 1
        while (after < text.length && text[after] in SENTENCE_CLOSERS) after++
        if (after < text.length && !text[after].isWhitespace()) return null
        if (mark == '!' || mark == '?' || mark == '…') return after
        val word = wordBefore(text, punct)
        if (word.length == 1 && word[0].isLetter()) return null
        if (word.isNotEmpty() && word.all { it.isDigit() }) return null
        if (word.lowercase() in ABBREVIATIONS) return null
        return after
    }

    private fun wordBefore(text: String, punct: Int): String {
        var end = punct
        while (end > 0 && text[end - 1] == '.') end--
        var start = end
        while (start > 0 && text[start - 1].isLetterOrDigit()) start--
        return text.substring(start, end)
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
        text = stripMarkdownImages(text)
        text = REFERENCE_LINK.replace(text) { it.groupValues[1] }
        text = MarkdownInline.plain(text)
        text = EMPHASIS.replace(text, "")
        text = speakSourceDomains(text)
        text = stripBareUrls(text)
        text = SPACE_BEFORE_PUNCT.replace(text) { it.groupValues[1] }
        text = WHITESPACE.replace(text, " ").trim()
        return text.takeIf { it.isNotEmpty() }
    }

    private fun speakSourceDomains(text: String): String =
        SOURCE_URL.replace(text) { match ->
            val label = match.groupValues[1]
            val (url, punctuation) = splitUrlTail(match.groupValues[2])
            val host = ArticleUrl.host(url) ?: return@replace label
            "$label $host$punctuation"
        }

    private fun stripBareUrls(text: String): String =
        BARE_URL.replace(text) { match ->
            val (_, punctuation) = splitUrlTail(match.value)
            " $punctuation"
        }

    private fun splitUrlTail(raw: String): Pair<String, String> {
        var end = raw.length
        end = trimSentencePunctuation(raw, end)
        while (end > 0 && isTrailingUrlCloser(raw, end - 1)) {
            end--
            end = trimSentencePunctuation(raw, end)
        }
        return raw.take(end) to raw.drop(end)
    }

    private fun trimSentencePunctuation(raw: String, end: Int): Int {
        var next = end
        while (next > 0 && raw[next - 1] in URL_TRAILING_PUNCT) next--
        return next
    }

    private fun isTrailingUrlCloser(raw: String, index: Int): Boolean {
        val char = raw[index]
        if (char == ')') return !hasMatchingOpenParen(raw, index)
        return char in URL_TRAILING_CLOSERS
    }

    private fun hasMatchingOpenParen(raw: String, closeIndex: Int): Boolean {
        var balance = 0
        for (index in 0 until closeIndex) {
            when (raw[index]) {
                '(' -> balance++
                ')' -> if (balance > 0) balance--
            }
        }
        return balance > 0
    }

    private fun stripMarkdownImages(text: String): String {
        val out = StringBuilder(text.length)
        var index = 0
        while (index < text.length) {
            if (index + 1 < text.length && text[index] == '!' && text[index + 1] == '[') {
                val altEnd = findClosingBracket(text, index + 1)
                if (altEnd >= 0) {
                    val imageEnd = when (text.getOrNull(altEnd + 1)) {
                        '(' -> findClosingImageDestination(text, altEnd + 1)
                        '[' -> findClosingBracket(text, altEnd + 1)
                        else -> altEnd
                    }
                    if (imageEnd >= altEnd) {
                        out.append(' ')
                        index = imageEnd + 1
                        continue
                    }
                }
            }
            out.append(text[index])
            index++
        }
        return out.toString()
    }

    private fun findClosingBracket(text: String, open: Int): Int {
        var depth = 1
        var index = open + 1
        while (index < text.length) {
            when (text[index]) {
                '\\' -> index++
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) return index
                }
            }
            index++
        }
        return -1
    }

    private fun findClosingImageDestination(text: String, open: Int): Int {
        var depth = 1
        var index = open + 1
        var quote: Char? = null
        var inAngleDestination = false
        while (index < text.length) {
            val char = text[index]
            when {
                char == '\\' -> index++
                quote != null -> if (char == quote) quote = null
                inAngleDestination -> if (char == '>') inAngleDestination = false
                char == '<' -> inAngleDestination = true
                char == '"' || char == '\'' -> quote = char
                char == '(' -> depth++
                char == ')' -> {
                    depth--
                    if (depth == 0) return index
                }
            }
            index++
        }
        return -1
    }

    private fun String.matchKey(): String =
        WHITESPACE.replace(this, " ").trim().lowercase()

    private fun isHeading(line: String): Boolean = line.startsWith("#")

    private fun isListItem(line: String): Boolean = LIST_MARK.containsMatchIn(line)

    private fun isTableRow(line: String): Boolean =
        line.startsWith("|") || (line.contains("|") && TABLE_SEPARATOR.matches(line))

    private fun isImageOnly(line: String): Boolean = stripMarkdownImages(line).isBlank()

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

    private const val FOLLOW_ALONG_WPM = 170.0
    private const val MIN_SENTENCE_MS = 400L
    private const val MAX_SENTENCE_MS = 12_000L
    private const val MAX_PARAGRAPH_MS = 180_000L

    private val ABBREVIATIONS = setOf(
        "abs", "art", "aufl", "bd", "bzw", "ca", "chr", "corp", "dept", "dipl",
        "dr", "etc", "evtl", "exkl", "fig", "hr", "inc", "inkl", "jr", "kap",
        "ltd", "mag", "max", "mind", "mio", "mr", "mrd", "mrs", "ms", "no",
        "nr", "pp", "prof", "resp", "sr", "st", "str", "tel", "usw", "vgl",
        "vol", "vs",
    )
    private val HEADING_MARK = Regex("""(?m)^#{1,6}\s+""")
    private val QUOTE_MARK = Regex("""(?m)^>\s?""")
    private val LIST_MARK = Regex("""^\s*(?:[-*+]|\d+[.)])\s+""")
    private val REFERENCE_LINK = Regex("""\[([^\]\n]+)]\[[^\]\n]*]""")
    private val REFERENCE_DEFINITION = Regex("""^[ \t]{0,3}\[(?!\^)[^\]\n]+]:[ \t]*.*$""")
    private val EMPHASIS = Regex("""[*_~`]+""")
    private val BARE_URL = Regex("""(?i)\b(?:https?://|www\.)[^\s<>\[\]]+""")
    private val SOURCE_URL = Regex(
        """(source:)\s*((?:https?://|www\.)[^\s<>\[\]]+)""",
        RegexOption.IGNORE_CASE,
    )
    private val SPACE_BEFORE_PUNCT = Regex("""\s+([.,;:!?)"'\]»”’])""")
    private val WHITESPACE = Regex("""\s+""")
    private val TABLE_SEPARATOR = Regex("""^[\s|:\-]+$""")
    private const val SENTENCE_CLOSERS = "\"'”’)]»"
    private const val URL_TRAILING_PUNCT = ".,;:!?"
    private const val URL_TRAILING_CLOSERS = "\"'”’]»"
    private val RULE = Regex("""^(?:-{3,}|\*{3,}|_{3,})$""")

    private data class MarkdownBlock(
        val text: String,
        val start: Int,
        val end: Int,
    )
}
