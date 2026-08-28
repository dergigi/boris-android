package org.dergigi.boris.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

data class Nip21Links(
    val authorPubkey: String? = null,
    val articleCoordinate: String? = null,
)

/** NIP-21 `<link rel="…" href="nostr:…">` tags on HTML pages. */
object Nip21Html {
    fun parse(html: String): Nip21Links {
        if (html.isBlank()) return Nip21Links()
        val links = runCatching { Jsoup.parse(html).select("link[href]") }.getOrNull()
            ?: return Nip21Links()
        val author = firstProfile(links, "author")
        val me = firstProfile(links, "me")
        val alternate = firstArticle(links, "alternate")
        return Nip21Links(
            authorPubkey = author ?: me ?: alternate?.pointer?.pubkey?.lowercase(),
            articleCoordinate = alternate?.coordinate,
        )
    }

    private fun firstProfile(links: Elements, rel: String): String? {
        for (el in links) {
            if (!hasRel(el, rel)) continue
            val href = nostrHref(el) ?: continue
            val target = NostrLink.parse(href)
            if (target is NostrTarget.Profile) return target.pubkeyHex.lowercase()
        }
        return null
    }

    private fun firstArticle(links: Elements, rel: String): NostrArticleRef? {
        for (el in links) {
            if (!hasRel(el, rel)) continue
            val href = nostrHref(el) ?: continue
            NostrArticle.parse(href)?.let { return it }
        }
        return null
    }

    private fun nostrHref(el: Element): String? {
        val href = el.attr("href").trim()
        return href.takeIf { it.startsWith("nostr:", ignoreCase = true) }
    }

    private fun hasRel(el: Element, rel: String): Boolean =
        el.attr("rel").split(Regex("\\s+")).any { it.equals(rel, ignoreCase = true) }
}
