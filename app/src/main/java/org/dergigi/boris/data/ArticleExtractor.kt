package org.dergigi.boris.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object ArticleExtractor {
    fun article(html: String, baseUrl: String): Element? {
        val doc = Jsoup.parse(html, baseUrl)
        doc.select(CHROME).remove()
        val best = pick(doc) ?: return null
        clean(best)
        if (score(best) == 0) return null
        return best
    }

    fun markdown(html: String, baseUrl: String): String? =
        article(html, baseUrl)?.let { HtmlToMarkdown.convert(it, baseUrl) }

    private fun pick(doc: org.jsoup.nodes.Document): Element? {
        val candidates = doc.select(ARTICLE).filter { score(it) > 0 }
        val leaves = candidates.filter { cand ->
            candidates.none { other -> other !== cand && cand.contains(other) }
        }
        return leaves.maxByOrNull(::score) ?: doc.body()?.takeIf { score(it) > 0 }
    }

    private fun clean(root: Element) {
        root.select(CHROME).remove()
        root.select("figcaption, .credit, span.credit").toList().forEach { el ->
            if (CREDIT.containsMatchIn(el.text())) el.remove()
        }
        root.allElements.toList().forEach { el ->
            if (el === root || el.parent() == null) return@forEach
            if (unlikely(el)) el.remove()
        }
        root.select("div, ul, ol, section").toList().asReversed().forEach { el ->
            if (el === root || el.parent() == null) return@forEach
            if (uselessBlock(el)) el.remove()
        }
        pruneLeading(root)
    }

    private fun pruneLeading(root: Element) {
        for (child in root.children().toList()) {
            if (isProse(child)) break
            if (child.tagName() == "figure" || child.selectFirst("img, picture") != null) continue
            if (isLeadingJunk(child)) child.remove() else break
        }
    }

    private fun score(element: Element): Int {
        val text = element.text().trim()
        if (text.length < ReaderRepository.MIN_ARTICLE_MARKDOWN_CHARS) return 0
        val linkText = element.select("a").text().length
        if (linkText.toDouble() / text.length > 0.5) return 0
        return text.length
    }

    private fun unlikely(el: Element): Boolean {
        val hay = "${el.id()} ${el.className()} ${el.attr("itemprop")}"
        return UNLIKELY.containsMatchIn(hay) && !LIKELY.containsMatchIn(hay)
    }

    private fun uselessBlock(el: Element): Boolean {
        val text = el.text().trim()
        if (text.isEmpty()) return el.selectFirst("img, picture") == null
        val density = el.select("a").text().length.toDouble() / text.length
        return if (text.length < 80) density > 0.2 else density > 0.5
    }

    private fun isProse(el: Element): Boolean {
        val text = el.text().trim()
        if (text.length < 40 || !text.contains(' ')) return false
        return el.select("a").text().length.toDouble() / text.length <= 0.3
    }

    private fun shareOnly(text: String): Boolean {
        val tokens = text.lowercase().split(Regex("""[^\p{L}\p{N}]+""")).filter { it.isNotEmpty() }
        return tokens.isNotEmpty() && tokens.all { it in SHARE_TOKENS }
    }

    private fun isLeadingJunk(el: Element): Boolean {
        val text = el.text().trim()
        if (text.isEmpty()) return el.selectFirst("img, picture") == null
        if (text.length < 80 && (CREDIT.containsMatchIn(text) || shareOnly(text))) {
            return true
        }
        val density = el.select("a").text().length.toDouble() / text.length
        if (text.length < 80 && density > 0.2) return true
        return text.length < 24 && !el.tagName().startsWith("h")
    }

    // Brave Speedreader / Mozilla Readability: drop nodes whose class or id
    // looks like chrome, unless it also looks like the article body.
    private val UNLIKELY = Regex(
        """(?i)(-ad-|banner|breadcrumbs|combx|comment|community|disqus|extra|gdpr|""" +
            """legends|menu|related|remark|replies|rss|shoutbox|sidebar|skyscraper|""" +
            """social|sponsor|supplemental|ad-break|agegate|pagination|pager|popup|""" +
            """masthead|share|newsletter|utility-bar|follow-us)""",
    )
    private val LIKELY = Regex("""(?i)(and|article|body|column|content|main|shadow)""")
    private val CREDIT = Regex("""(?i)image\s+credit""")
    private val SHARE_TOKENS = setOf(
        "copy", "link", "facebook", "reddit", "email", "follow", "us",
        "newsletter", "share", "this", "x", "twitter", "0",
    )

    private const val CHROME =
        "script, style, noscript, iframe, svg, canvas, form, nav, footer, aside, header, button, " +
            "[role=navigation], [role=banner], [role=contentinfo], [role=complementary], [role=menu], " +
            "#comments, .comments, .comment-list, .sidebar, .ad, .ads, .advertisement, " +
            "#utility-bar, [data-component-name=UtilityBar], [data-analytics-id=article-social], " +
            "[aria-hidden=true]"

    private const val ARTICLE =
        "article, [itemprop=articleBody], [role=article], main, [role=main], " +
            "#content, #article-body, .post-content, .entry-content, .article-body"
}
