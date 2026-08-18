package org.dergigi.boris.data

/**
 * Pragmatic HTML-to-markdown conversion for RSS feed content.
 * Feed bodies are simple article HTML, so a transform pipeline is
 * enough; no full DOM parsing needed.
 */
object HtmlToMarkdown {
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
            if (text.isEmpty()) m.groupValues[1] else "[$text](${m.groupValues[1]})"
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
        s = s.replace(Regex("(?is)<li[^>]*>(.*?)</li>")) { "\n- ${it.groupValues[1].trim()}" }
        s = s.replace(Regex("(?i)<br\\s*/?>"), "\n")
        s = s.replace(Regex("(?i)<hr[^>]*>"), "\n\n---\n\n")
        s = s.replace(
            Regex("(?i)</(p|div|figure|section|article|ul|ol|table|tr)>"),
            "\n\n",
        )
        s = stripTags(s)
        s = decode(s)
        s = s.replace(Regex("[ \\t]+\\n"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
        stash.forEachIndexed { i, text ->
            s = s.replace("\u0000$i\u0000", text)
        }
        return s.replace(Regex("\\n{3,}"), "\n\n").trim()
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

    fun decode(s: String): String = s
        .replace(Regex("&#(\\d+);")) { m ->
            m.groupValues[1].toIntOrNull()?.let { String(Character.toChars(it)) } ?: m.value
        }
        .replace(Regex("&#[xX]([0-9a-fA-F]+);")) { m ->
            m.groupValues[1].toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: m.value
        }
        .replace("&nbsp;", " ")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
}
