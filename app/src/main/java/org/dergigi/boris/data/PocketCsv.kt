package org.dergigi.boris.data

import java.io.Reader

/** Pocket's saves CSV: title,url,time_added,tags,status. Import every saved status. */
object PocketCsv {
    fun parse(reader: Reader): ArticleExport {
        val rows = ImportCsv.read(reader)
        val header = ImportCsv.header(rows.first())
        val urlColumn = header.indexOf("url")
        require(urlColumn >= 0) { "Choose a Pocket CSV containing a URL column." }
        val titleColumn = header.indexOf("title")
        val articles = mutableListOf<ImportArticle>()
        var skipped = 0
        for (row in rows.drop(1)) {
            val url = row.getOrNull(urlColumn)?.trim().orEmpty()
            if (!ImportCsv.isWebUrl(url)) {
                skipped++
                continue
            }
            articles += ImportArticle(url, row.getOrNull(titleColumn)?.trim()?.takeIf(String::isNotEmpty))
        }
        return ArticleExport(articles, skipped)
    }
}
