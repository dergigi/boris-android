package org.dergigi.boris.data

/**
 * Flattens common inline markdown in highlight quotes so cards show
 * readable labels instead of raw `[text](url)` syntax.
 */
object MarkdownInline {
    data class Link(val start: Int, val end: Int, val url: String)

    fun plain(text: String): String = flatten(text).first

    fun flatten(text: String): Pair<String, List<Link>> {
        val out = StringBuilder()
        val links = mutableListOf<Link>()
        var i = 0
        for (match in INLINE.findAll(text)) {
            out.append(text, i, match.range.first)
            val imgAlt = match.groups["imgAlt"]
            val label = match.groups["label"]
            val autoUrl = match.groups["autoUrl"]
            when {
                imgAlt != null -> {
                    if (imgAlt.value.isNotEmpty()) out.append(imgAlt.value)
                }
                label != null -> {
                    val url = match.groups["url"]?.value.orEmpty()
                    val start = out.length
                    out.append(label.value)
                    if (url.isNotEmpty()) links.add(Link(start, out.length, url))
                }
                autoUrl != null -> {
                    val start = out.length
                    out.append(autoUrl.value)
                    links.add(Link(start, out.length, autoUrl.value))
                }
            }
            i = match.range.last + 1
        }
        out.append(text, i, text.length)
        return out.toString() to links
    }

    private val INLINE = Regex(
        """!\[(?<imgAlt>[^\]\n]*)]\([^)\s]+\)|\[(?<label>[^\]\n]+)]\((?<url>[^)\s]+)(?:\s+"[^"]*")?\)|<(?<autoUrl>https?://[^>\s]+)>""",
    )
}
