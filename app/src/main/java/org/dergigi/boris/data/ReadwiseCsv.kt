package org.dergigi.boris.data

import java.io.Reader
import java.net.URI

/** Reader's library CSV, not the separate Readwise highlights export. */
object ReadwiseCsv {
    const val MAX_CHARS = ImportCsv.MAX_CHARS
    const val MAX_ROWS = ImportCsv.MAX_ROWS

    fun parse(reader: Reader): ArticleExport {
        val rows = ImportCsv.read(reader)
        val header = ImportCsv.header(rows.first())
        val urlColumn = listOf("sourceurl", "originalurl", "url").firstNotNullOfOrNull { name ->
            header.indexOf(name).takeIf { it >= 0 }
        } ?: throw IllegalArgumentException("Choose a Reader library CSV containing a URL column.")
        val titleColumn = header.indexOf("title")
        val categoryColumn = header.indexOf("category")
        val articles = mutableListOf<ImportArticle>()
        var skipped = 0
        for (row in rows.drop(1)) {
            val url = row.getOrNull(urlColumn)?.trim().orEmpty()
            val category = row.getOrNull(categoryColumn)?.trim()?.lowercase().orEmpty()
            if (!isArticleUrl(url) || (category.isNotEmpty() && category !in setOf("article", "rss"))) {
                skipped++
                continue
            }
            articles += ImportArticle(url, row.getOrNull(titleColumn)?.trim()?.takeIf(String::isNotEmpty))
        }
        return ArticleExport(articles, skipped)
    }

    private fun isArticleUrl(url: String): Boolean = ImportCsv.isWebUrl(url) &&
        !URI(url).host.equals("read.readwise.io", ignoreCase = true)
}
