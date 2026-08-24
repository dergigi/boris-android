package org.dergigi.boris.data

/**
 * Pragmatic HTML-to-markdown conversion for RSS feed content.
 * Feed bodies are simple article HTML, so a transform pipeline is
 * enough; no full DOM parsing needed.
 */
object HtmlToMarkdown {
    fun convert(element: org.jsoup.nodes.Element, baseUrl: String): String {
        val doc = element.ownerDocument()
        val pretty = doc?.outputSettings()?.prettyPrint() ?: true
        doc?.outputSettings()?.prettyPrint(false)
        return try {
            convert(element.outerHtml(), baseUrl)
        } finally {
            doc?.outputSettings()?.prettyPrint(pretty)
        }
    }

    fun convert(html: String, baseUrl: String? = null): String {
        val stash = mutableListOf<String>()
        fun stash(text: String): String {
            stash.add(text)
            return "\u0000${stash.size - 1}\u0000"
        }

        var s = html
            .replace(Regex("(?is)<script.*?</script>"), "")
            .replace(Regex("(?is)<style.*?</style>"), "")
            .replace(Regex("(?is)<head.*?</head>"), "")
            .replace(Regex("(?s)<!--.*?-->"), "")

        // Ghost and other email-style newsletters nest article prose inside
        // layout <table>s. Converting those to GFM smashes headings and
        // paragraphs into single cells (issue #97). Unwrap tables whose cells
        // hold block content, innermost first; real data tables are renamed so
        // an outer wrapper still reads as layout, then restored for the GFM
        // rule below.
        var rounds = 0
        while (rounds++ < 32) {
            var sawTable = false
            s = innermostTable.replace(s) { m ->
                sawTable = true
                val inner = m.groupValues[1]
                if (isLayoutTable(inner)) {
                    "\n\n" + inner.replace(tableChrome, "\n\n") + "\n\n"
                } else {
                    // Rename all structure tags so an enclosing layout
                    // table's unwrap cannot strip this table's rows.
                    m.value.replace(tableTag) { t ->
                        "<${t.groupValues[1]}data${t.groupValues[2]}"
                    }
                }
            }
            if (!sawTable) break
        }
        s = s.replace(dataTableTag) { t -> "<${t.groupValues[1]}${t.groupValues[2]}" }

        // Footnote refs before the anchor rule so <sup><a href="#id"> does not
        // become a plain link. Only ids with a matching <li id> get a pair.
        val footnoteIds = linkedMapOf<String, Int>()
        supRefRegex.findAll(s).forEach { m ->
            val id = m.groupValues[1]
            if (id in footnoteIds) return@forEach
            if (Regex("(?is)<li[^>]*\\bid=[\"']${Regex.escape(id)}[\"']").containsMatchIn(s)) {
                footnoteIds[id] = footnoteIds.size + 1
            }
        }
        s = supRefRegex.replace(s) { m ->
            footnoteIds[m.groupValues[1]]?.let { "[^$it]" } ?: m.value
        }
        footnoteIds.forEach { (id, n) ->
            s = s.replace(
                Regex("(?is)<li[^>]*\\bid=[\"']${Regex.escape(id)}[\"'][^>]*>(.*?)</li>"),
            ) {
                "\n\n[^$n]: ${stripTags(it.groupValues[1]).trim()}\n\n"
            }
        }

        s = s.replace(Regex("(?is)<pre[^>]*>\\s*<code[^>]*>(.*?)</code>\\s*</pre>")) {
            stash("\n\n```\n" + decode(it.groupValues[1]).trim('\n') + "\n```\n\n")
        }
        s = s.replace(Regex("(?is)<pre[^>]*>(.*?)</pre>")) {
            stash("\n\n```\n" + decode(stripTags(it.groupValues[1])).trim('\n') + "\n```\n\n")
        }
        s = s.replace(Regex("(?is)<code[^>]*>(.*?)</code>")) {
            stash("`" + decode(it.groupValues[1]) + "`")
        }
        s = s.replace(Regex("(?is)<img[^>]*>")) { image(it.value, baseUrl) }
        s = s.replace(Regex("(?is)<a\\s[^>]*href=[\"']([^\"']*)[\"'][^>]*>(.*?)</a>")) { m ->
            val text = stripTags(m.groupValues[2]).trim()
            when {
                text.isEmpty() -> m.groupValues[1]
                onlyMarkdownImage.matches(text) -> "\n\n$text\n\n"
                else -> "[$text](${m.groupValues[1]})"
            }
        }
        for (level in 1..6) {
            s = s.replace(Regex("(?is)<h$level[^>]*>(.*?)</h$level>")) { m ->
                "\n\n" + "#".repeat(level) + " " + stripTags(m.groupValues[1]).trim() + "\n\n"
            }
        }
        s = s.replace(Regex("(?is)<(strong|b)\\b[^>]*>(.*?)</\\1>")) { "**${it.groupValues[2]}**" }
        s = s.replace(Regex("(?is)<(em|i)\\b[^>]*>(.*?)</\\1>")) { "*${it.groupValues[2]}*" }
        s = s.replace(Regex("(?is)<blockquote[^>]*>(.*?)</blockquote>")) { m ->
            val inner = m.groupValues[1]
                .replace(Regex("(?i)</?p[^>]*>"), "\n")
                .trim()
            val quoted = stripTags(inner)
                .lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n") { "> $it" }
            "\n\n$quoted\n\n"
        }
        s = s.replace(Regex("(?is)<table[^>]*>(.*?)</table>")) { m ->
            val rows = Regex("(?is)<tr[^>]*>(.*?)</tr>").findAll(m.groupValues[1])
                .map { row ->
                    Regex("(?is)<t[hd][^>]*>(.*?)</t[hd]>").findAll(row.groupValues[1])
                        .map { cell -> stripTags(cell.groupValues[1]).replace('\n', ' ').trim() }
                        .toList()
                }
                .filter { it.isNotEmpty() }
                .toList()
            if (rows.isEmpty()) {
                ""
            } else {
                val lines = buildList {
                    add(rows.first().joinToString(" | ", "| ", " |"))
                    add(rows.first().joinToString(" | ", "| ", " |") { "---" })
                    rows.drop(1).forEach { add(it.joinToString(" | ", "| ", " |")) }
                }
                "\n\n" + lines.joinToString("\n") + "\n\n"
            }
        }
        s = s.replace(Regex("(?is)<ol[^>]*>(.*?)</ol>")) { m ->
            var n = 0
            val items = Regex("(?is)<li[^>]*>(.*?)</li>").replace(m.groupValues[1]) { li ->
                n += 1
                "\n$n. ${li.groupValues[1].trim()}"
            }
            "\n\n$items\n\n"
        }
        s = s.replace(Regex("(?is)<li[^>]*>(.*?)</li>")) { "\n- ${it.groupValues[1].trim()}" }
        s = s.replace(Regex("(?i)<br\\s*/?>"), "\n")
        s = s.replace(Regex("(?i)<hr[^>]*>"), "\n\n---\n\n")
        s = s.replace(
            Regex("(?i)</(p|div|figure|section|article|ul|ol|table|tr)>"),
            "\n\n",
        )
        s = stripTags(s)
        s = decode(s)
        // Jekyll and jsoup pretty-print with 4-space indent. Those spaces
        // survive tag stripping and become CommonMark indented code blocks.
        s = s.replace(Regex("(?m)^[ \\t]+"), "")
        s = s.replace(Regex("[ \\t]+\\n"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
        stash.forEachIndexed { i, text ->
            s = s.replace("\u0000$i\u0000", text)
        }
        val body = s.replace(Regex("\\n{3,}"), "\n\n").trim()
        val author = byline(html)
        return if (author == null || body.isEmpty()) body else "*$author*\n\n$body"
    }

    private fun byline(html: String): String? {
        val meta = Regex("(?is)<meta\\b[^>]*\\b(?:name|itemprop)=[\"']author[\"'][^>]*>")
            .find(html)?.value?.let { attr(it, "content") }
        val raw = meta ?: Regex("(?is)<(\\w+)\\b[^>]*\\b(?:rel|itemprop)=[\"']author[\"'][^>]*>(.*?)</\\1>")
            .find(html)?.groupValues?.getOrNull(2)?.let { stripTags(it) }
        return raw?.let(::decode)?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun image(tag: String, baseUrl: String?): String {
        val src = attr(tag, "src") ?: return ""
        val url = if (baseUrl.isNullOrBlank()) {
            src
        } else {
            UrlExtractor.articleUrl(src, baseUrl) ?: return ""
        }.let(UrlExtractor::preferHttps)
        val alt = attr(tag, "alt").orEmpty()
        return "\n\n![$alt]($url)\n\n"
    }

    private fun attr(tag: String, name: String): String? =
        Regex("(?i)\\b$name\\s*=\\s*[\"']([^\"']*)[\"']")
            .find(tag)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }

    private fun stripTags(s: String): String = s.replace(Regex("<[^>]+>"), "")

    /** A table without nested <table>s, so nesting resolves innermost first. */
    private val innermostTable =
        Regex("(?is)<table[^>]*>((?:(?!<table[\\s>]).)*?)</table>")

    private val tableChrome =
        Regex("(?is)</?(?:thead|tbody|tfoot|tr|caption|t[hd])[^>]*>")

    private val tableTag =
        Regex("(?i)<(/?)(table|thead|tbody|tfoot|caption|tr|t[hd])\\b")

    private val dataTableTag =
        Regex("(?i)<(/?)data(table|thead|tbody|tfoot|caption|tr|t[hd])\\b")

    private val blockInsideTable =
        Regex("(?is)<(?:p|h[1-6]|ul|ol|blockquote|figure|datatable)\\b")

    private val tableRow = Regex("(?is)<tr[^>]*>(.*?)</tr>")

    private val tableCellOpen = Regex("(?is)<t[hd][\\s>]")

    private fun isLayoutTable(inner: String): Boolean {
        if (blockInsideTable.containsMatchIn(inner)) return true
        val maxCells = tableRow.findAll(inner)
            .maxOfOrNull { row -> tableCellOpen.findAll(row.groupValues[1]).count() } ?: 0
        return maxCells < 2
    }

    private val onlyMarkdownImage = Regex("""^!\[[^\]]*]\([^)]+\)$""")

    private val supRefRegex =
        Regex("(?is)<sup[^>]*>\\s*<a[^>]*\\bhref=[\"']#([^\"']+)[\"'][^>]*>.*?</a>\\s*</sup>")

    fun decode(s: String): String = s
        .replace(Regex("&#(\\d+);")) { m ->
            decodeCodePoint(m.groupValues[1].toIntOrNull()) ?: m.value
        }
        .replace(Regex("&#[xX]([0-9a-fA-F]+);")) { m ->
            decodeCodePoint(m.groupValues[1].toIntOrNull(16)) ?: m.value
        }
        .replace("&nbsp;", " ")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")

    private fun decodeCodePoint(code: Int?): String? {
        if (code == null || !Character.isValidCodePoint(code)) return null
        if (code in Character.MIN_SURROGATE.code..Character.MAX_SURROGATE.code) return null
        return try {
            String(Character.toChars(code))
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
