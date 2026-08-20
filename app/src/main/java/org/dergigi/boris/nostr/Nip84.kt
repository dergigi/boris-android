package org.dergigi.boris.nostr

import org.dergigi.boris.data.NostrArticle
import org.json.JSONArray
import org.json.JSONObject

object Nip84 {
    const val KIND = 9802
    const val ALT = "Highlight created by Boris Android. readwithboris.com"

    fun articleUrl(event: Nip01Event): String? {
        val http = event.tags.firstOrNull { tag ->
            tag.size >= 2 && tag[0] == "r" && tag[1].startsWith("http")
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
    ): List<List<String>> = buildList {
        if (!coordinate.isNullOrBlank()) {
            add(listOf("a", coordinate))
            if (!eventId.isNullOrBlank()) add(listOf("e", eventId))
            if (!authorPubkey.isNullOrBlank()) add(listOf("p", authorPubkey))
        } else if (!eventId.isNullOrBlank()) {
            add(listOf("e", eventId))
            if (!authorPubkey.isNullOrBlank()) add(listOf("p", authorPubkey))
        } else {
            add(listOf("r", url))
        }
        if (!context.isNullOrBlank()) add(listOf("context", context))
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
    ): String {
        val obj = JSONObject()
            .put("kind", KIND)
            .put("content", quote)
            .put("tags", tagsToJson(tags(url, context, coordinate, eventId, authorPubkey, zapSplits)))
            .put("created_at", createdAt)
        if (!pubkeyHex.isNullOrBlank()) {
            obj.put("pubkey", pubkeyHex)
        }
        return obj.toString()
    }

    fun extractContext(selectedText: String, articleContent: String): String? {
        if (selectedText.isEmpty() || articleContent.isEmpty()) return null
        val needle = selectedText.split(Regex("\\n+")).firstOrNull { it.isNotBlank() }?.trim()
            ?: selectedText
        val selectedIndex = articleContent.indexOf(needle)
        if (selectedIndex < 0) return null

        val paragraphs = articleContent.split(Regex("\n\n+"))
        var currentPos = 0
        var containingParagraph: String? = null
        for (paragraph in paragraphs) {
            val paragraphEnd = currentPos + paragraph.length
            if (selectedIndex >= currentPos && selectedIndex < paragraphEnd) {
                containingParagraph = paragraph
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

        val selectedSentenceIndex = reconstructed.indexOfFirst { it.contains(needle) }
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
