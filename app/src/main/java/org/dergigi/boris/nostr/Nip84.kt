package org.dergigi.boris.nostr

import org.json.JSONArray
import org.json.JSONObject

object Nip84 {
    const val KIND = 9802
    const val ALT = "Highlight created by Boris Android. readwithboris.com"

    fun tags(url: String, context: String?): List<List<String>> = buildList {
        add(listOf("r", url))
        if (!context.isNullOrBlank()) add(listOf("context", context))
        add(listOf("alt", ALT))
    }

    fun unsignedJson(
        quote: String,
        url: String,
        context: String?,
        pubkeyHex: String? = null,
    ): String {
        val obj = JSONObject()
            .put("kind", KIND)
            .put("content", quote)
            .put("tags", tagsToJson(tags(url, context)))
            .put("created_at", System.currentTimeMillis() / 1000)
        if (!pubkeyHex.isNullOrBlank()) {
            obj.put("pubkey", pubkeyHex)
        }
        return obj.toString()
    }

    fun extractContext(selectedText: String, articleContent: String): String? {
        if (selectedText.isEmpty() || articleContent.isEmpty()) return null
        val selectedIndex = articleContent.indexOf(selectedText)
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

        val parts = paragraph.split(Regex("""([.!?]+\s+)""")).filter { it.trim().isNotEmpty() }
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

        val selectedSentenceIndex = reconstructed.indexOfFirst { it.contains(selectedText) }
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
