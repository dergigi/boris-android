package org.dergigi.boris.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object ArticleExtractor {
    fun article(html: String, baseUrl: String): Element? {
        val doc = Jsoup.parse(html, baseUrl)
        doc.select(CHROME).remove()
        val best = doc.select(ARTICLE).maxByOrNull { score(it) } ?: doc.body() ?: return null
        if (score(best) == 0) return null
        return best
    }

    fun markdown(html: String, baseUrl: String): String? =
        article(html, baseUrl)?.let { HtmlToMarkdown.convert(it, baseUrl) }

    private fun score(element: Element): Int {
        val text = element.text().trim()
        if (text.length < MIN_CHARS) return 0
        val linkText = element.select("a").text().length
        if (linkText.toDouble() / text.length > 0.5) return 0
        return text.length
    }

    private const val MIN_CHARS = 500

    private const val CHROME =
        "script, style, noscript, iframe, svg, canvas, form, nav, footer, aside, " +
            "[role=navigation], [role=banner], [role=contentinfo], [role=complementary], " +
            "#comments, .comments, .comment-list, .sidebar, .ad, .ads, .advertisement, " +
            "[aria-hidden=true]"

    private const val ARTICLE =
        "article, [itemprop=articleBody], [role=article], main, [role=main], " +
            "#content, .post-content, .entry-content, .article-body"
}
