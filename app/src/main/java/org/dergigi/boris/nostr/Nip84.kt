package org.dergigi.boris.nostr

import org.dergigi.boris.data.NostrArticle
import org.dergigi.boris.data.NostrMentions
import org.dergigi.boris.data.UrlExtractor
import org.json.JSONArray
import org.json.JSONObject

object Nip84 {
    const val KIND = 9802
    const val ALT = "Highlight created by Boris Android. readwithboris.com"

    fun comment(event: Nip01Event): String? =
        event.tagValue("comment")?.takeIf { it.isNotBlank() }

    fun articleUrl(event: Nip01Event): String? {
        val rTags = event.tags.filter { it.size >= 2 && it[0] == "r" }
        val source = rTags.firstOrNull { it.size >= 3 && it[2] == "source" }?.get(1)
        if (source != null && source.startsWith("http")) return source
        val http = rTags.firstOrNull { tag ->
            tag[1].startsWith("http") && (tag.size < 3 || tag[2] != "mention")
        }?.get(1)
        if (http != null) return http
        val coordinate = event.tags.firstOrNull { tag ->
            tag.size >= 2 && tag[0] == "a" && tag[1].startsWith("30023:")
        }?.get(1)
        if (coordinate != null) return NostrArticle.fromCoordinate(coordinate)?.uri
        val eventId = event.tags.firstOrNull { tag ->
            tag.size >= 2 && tag[0] == "e" && tag[1].length == 64
        }?.get(1) ?: return null
        return try {
            "nostr:${Nip19.noteEncode(eventId)}"
        } catch (_: Exception) {
            null
        }
    }

    fun tags(
        url: String,
        context: String?,
        coordinate: String? = null,
        eventId: String? = null,
        authorPubkey: String? = null,
        zapSplits: List<List<String>> = emptyList(),
        comment: String? = null,
    ): List<List<String>> = buildList {
        val annotation = comment?.trim()?.takeIf { it.isNotBlank() }
        if (!coordinate.isNullOrBlank()) {
            add(listOf("a", coordinate))
            if (!eventId.isNullOrBlank()) add(listOf("e", eventId))
            if (!authorPubkey.isNullOrBlank()) add(listOf("p", authorPubkey))
        } else if (!eventId.isNullOrBlank()) {
            add(listOf("e", eventId))
            if (!authorPubkey.isNullOrBlank()) add(listOf("p", authorPubkey))
        }
        if (url.startsWith("http")) {
            add(sourceUrlTag(url, annotation != null))
        } else if (coordinate.isNullOrBlank() && eventId.isNullOrBlank()) {
            add(sourceUrlTag(url, annotation != null))
        }
        if (!context.isNullOrBlank()) add(listOf("context", context))
        if (annotation != null) {
            add(listOf("comment", annotation))
            addAll(commentMentionTags(annotation, url))
        }
        add(listOf("alt", ALT))
        addAll(zapSplits)
    }

    fun unsignedJson(
        quote: String,
        url: String,
        context: String?,
        pubkeyHex: String? = null,
        createdAt: Long = System.currentTimeMillis() / 1000,
        coordinate: String? = null,
        eventId: String? = null,
        authorPubkey: String? = null,
        zapSplits: List<List<String>> = emptyList(),
        comment: String? = null,
    ): String {
        val obj = JSONObject()
            .put("kind", KIND)
            .put("content", quote)
            .put(
                "tags",
                tagsToJson(
                    tags(url, context, coordinate, eventId, authorPubkey, zapSplits, comment),
                ),
            )
            .put("created_at", createdAt)
        if (!pubkeyHex.isNullOrBlank()) {
            obj.put("pubkey", pubkeyHex)
        }
        return obj.toString()
    }

    fun extractContext(
        selectedText: String,
        articleContent: String,
        selectedStart: Int? = null,
    ): String? {
        if (selectedText.isEmpty() || articleContent.isEmpty()) return null
        val start = locateSelection(articleContent, selectedText, selectedStart) ?: return null
        val sentences = sentenceWindow(articleContent, start, selectedText)
        if (sentences != null && quoteCount(sentences, selectedText) == 1) return sentences
        return uniqueWindow(articleContent, start, selectedText.length) ?: sentences
    }

    /** Start index of [selectedText] in [articleContent], preferring [selectedStart]. */
    fun locateSelection(
        articleContent: String,
        selectedText: String,
        selectedStart: Int? = null,
        ownerText: String = "",
        ownerOffset: Int = 0,
    ): Int? {
        if (selectedStart != null &&
            selectedStart >= 0 &&
            selectedStart + selectedText.length <= articleContent.length &&
            articleContent.regionMatches(selectedStart, selectedText, 0, selectedText.length)
        ) {
            return selectedStart
        }
        if (ownerText.isNotBlank()) {
            val owners = QuoteMatch.occurrences(articleContent, ownerText)
            for (hit in owners) {
                val at = hit.first + ownerOffset
                if (at >= 0 &&
                    at + selectedText.length <= articleContent.length &&
                    articleContent.regionMatches(at, selectedText, 0, selectedText.length)
                ) {
                    return at
                }
            }
            if (owners.size == 1 && ownerOffset >= 0) {
                return (owners[0].first + ownerOffset)
                    .coerceIn(0, (articleContent.length - selectedText.length).coerceAtLeast(0))
            }
        }
        val hits = QuoteMatch.occurrences(articleContent, selectedText)
        if (hits.isEmpty()) return null
        if (selectedStart == null) return hits[0].first
        return hits.minBy { kotlin.math.abs(it.first - selectedStart) }.first
    }

    private fun sentenceWindow(articleContent: String, selectedIndex: Int, selectedText: String): String? {
        val paragraphs = articleContent.split(Regex("\n\n+"))
        var currentPos = 0
        var containingParagraph: String? = null
        var paragraphStart = 0
        for (paragraph in paragraphs) {
            val paragraphEnd = currentPos + paragraph.length
            if (selectedIndex >= currentPos && selectedIndex < paragraphEnd) {
                containingParagraph = paragraph
                paragraphStart = currentPos
                break
            }
            currentPos = paragraphEnd + 2
        }
        val paragraph = containingParagraph ?: return null

        val parts = splitKeepingDelimiters(paragraph, Regex("""[.!?]+\s+"""))
            .filter { it.trim().isNotEmpty() }
        val reconstructed = mutableListOf<String>()
        for (part in parts) {
            if (part.matches(Regex("""^[.!?]+\s*$"""))) {
                if (reconstructed.isNotEmpty()) {
                    reconstructed[reconstructed.lastIndex] = reconstructed.last() + part
                }
            } else {
                reconstructed.add(part)
            }
        }

        val localStart = selectedIndex - paragraphStart
        var cursor = 0
        val selectedSentenceIndex = reconstructed.indexOfFirst { sentence ->
            val at = paragraph.indexOf(sentence, cursor).takeIf { it >= 0 } ?: cursor
            cursor = at + sentence.length
            localStart >= at && localStart < at + sentence.length
        }.takeIf { it >= 0 } ?: reconstructed.indexOfFirst { it.contains(selectedText) }
        if (selectedSentenceIndex < 0) return null

        val contextParts = mutableListOf<String>()
        if (selectedSentenceIndex > 0) {
            contextParts.add(reconstructed[selectedSentenceIndex - 1].trim())
        }
        contextParts.add(reconstructed[selectedSentenceIndex].trim())
        if (selectedSentenceIndex < reconstructed.lastIndex) {
            contextParts.add(reconstructed[selectedSentenceIndex + 1].trim())
        }
        return if (contextParts.size > 1) contextParts.joinToString(" ") else null
    }

    private fun uniqueWindow(articleContent: String, start: Int, quoteLength: Int): String? {
        if (quoteLength <= 0) return null
        var radius = 40
        while (radius <= 200) {
            val window = clipWindow(articleContent, start, quoteLength, radius)
            if (window != null && QuoteMatch.occurrences(articleContent, window).size == 1) {
                return window
            }
            radius += 40
        }
        return clipWindow(articleContent, start, quoteLength, 80)
    }

    private fun clipWindow(articleContent: String, start: Int, quoteLength: Int, radius: Int): String? {
        val from = (start - radius).coerceAtLeast(0)
        val to = (start + quoteLength + radius).coerceAtMost(articleContent.length)
        if (to <= from) return null
        return articleContent.substring(from, to).trim().takeIf { it.isNotEmpty() }
    }

    private fun quoteCount(haystack: String, quote: String): Int =
        QuoteMatch.occurrences(haystack, quote).size

    private fun splitKeepingDelimiters(input: String, delimiter: Regex): List<String> {
        val out = mutableListOf<String>()
        var last = 0
        delimiter.findAll(input).forEach { match ->
            if (match.range.first > last) {
                out.add(input.substring(last, match.range.first))
            }
            out.add(match.value)
            last = match.range.last + 1
        }
        if (last < input.length) out.add(input.substring(last))
        return out
    }

    private fun sourceUrlTag(url: String, annotated: Boolean): List<String> =
        if (annotated) listOf("r", url, "source") else listOf("r", url)

    private fun commentMentionTags(comment: String, sourceUrl: String): List<List<String>> = buildList {
        for (profile in NostrMentions.profilesIn(comment)) {
            add(listOf("p", profile.pubkey, profile.relays.firstOrNull().orEmpty(), "mention"))
        }
        for (mentioned in UrlExtractor.urls(comment)) {
            if (mentioned == sourceUrl) continue
            add(listOf("r", mentioned, "mention"))
        }
    }

    private fun tagsToJson(tags: List<List<String>>): JSONArray {
        val out = JSONArray()
        for (tag in tags) {
            val row = JSONArray()
            for (value in tag) row.put(value)
            out.put(row)
        }
        return out
    }
}
