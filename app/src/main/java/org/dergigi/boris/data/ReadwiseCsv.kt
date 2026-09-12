package org.dergigi.boris.data

import java.io.Reader
import java.net.URI

data class ReadwiseArticle(val url: String, val title: String?)
data class ReadwiseExport(val articles: List<ReadwiseArticle>, val skipped: Int)

/** Reader's library CSV, not the separate Readwise highlights export. */
object ReadwiseCsv {
    const val MAX_CHARS = 20 * 1024 * 1024
    const val MAX_ROWS = 20_000

    fun parse(reader: Reader): ReadwiseExport {
        val text = reader.readTextLimited().removePrefix("\uFEFF")
        val rows = rows(text).filterNot { row -> row.all(String::isBlank) }
        require(rows.isNotEmpty()) { "The CSV is empty." }
        val header = rows.first().map { it.lowercase().filter(Char::isLetterOrDigit) }
        val urlColumn = listOf("sourceurl", "originalurl", "url").firstNotNullOfOrNull { name ->
            header.indexOf(name).takeIf { it >= 0 }
        } ?: throw IllegalArgumentException("Choose a Reader library CSV containing a URL column.")
        val titleColumn = header.indexOf("title")
        val categoryColumn = header.indexOf("category")
        val articles = mutableListOf<ReadwiseArticle>()
        var skipped = 0
        for (row in rows.drop(1)) {
            val url = row.getOrNull(urlColumn)?.trim().orEmpty()
            val category = row.getOrNull(categoryColumn)?.trim()?.lowercase().orEmpty()
            if (!isArticleUrl(url) || (category.isNotEmpty() && category !in setOf("article", "rss"))) {
                skipped++
                continue
            }
            articles += ReadwiseArticle(url, row.getOrNull(titleColumn)?.trim()?.takeIf(String::isNotEmpty))
        }
        return ReadwiseExport(articles, skipped)
    }

    private fun isArticleUrl(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        return uri.scheme?.lowercase() in setOf("http", "https") &&
            !uri.host.isNullOrBlank() && uri.userInfo == null &&
            !uri.host.equals("read.readwise.io", ignoreCase = true)
    }

    private fun Reader.readTextLimited(): String {
        val out = StringBuilder()
        val buffer = CharArray(8192)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            require(out.length + count <= MAX_CHARS) { "CSV is too large (maximum 20 MB of text)." }
            out.append(buffer, 0, count)
        }
        return out.toString()
    }

    // RFC 4180 quoting: commas, embedded newlines and doubled quotes are preserved.
    private fun rows(text: String): List<List<String>> {
        val result = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var closedQuote = false
        var i = 0
        fun endField() {
            row += field.toString()
            field.setLength(0)
            closedQuote = false
        }
        fun endRow() {
            endField()
            result += row
            require(result.size <= MAX_ROWS + 1) { "CSV has too many rows (maximum 20,000)." }
            row = mutableListOf()
        }
        while (i < text.length) {
            val c = text[i++]
            if (quoted) {
                if (c == '"') {
                    if (i < text.length && text[i] == '"') { field.append('"'); i++ }
                    else { quoted = false; closedQuote = true }
                } else field.append(c)
            } else when (c) {
                '"' -> {
                    require(field.isEmpty() && !closedQuote) { "Malformed CSV quoting." }
                    quoted = true
                }
                ',' -> endField()
                '\r', '\n' -> {
                    if (c == '\r' && i < text.length && text[i] == '\n') i++
                    endRow()
                }
                else -> {
                    require(!closedQuote) { "Unexpected text after a quoted CSV field." }
                    field.append(c)
                }
            }
        }
        require(!quoted) { "The CSV ends inside a quoted field." }
        if (field.isNotEmpty() || row.isNotEmpty() || closedQuote) endRow()
        return result
    }
}
