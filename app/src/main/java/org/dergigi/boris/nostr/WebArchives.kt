package org.dergigi.boris.nostr

import java.net.URI
import org.json.JSONArray
import org.json.JSONObject

data class WebArchiveReceipt(
    val id: String,
    val urls: List<String>,
    val archivedAt: Long?,
    val naan: Boolean,
)

/** Discovery of existing kind 4554 copies; never requests a new capture. */
object WebArchives {
    const val KIND = 4554

    // Only strip fragments. Keep scheme, path, query order and tracking parameters:
    // more aggressive canonicalization can match a different document.
    fun sourceUrl(raw: String): String? = httpUrl(raw.trim())?.substringBefore('#')

    private fun httpUrl(raw: String): String? {
        val uri = runCatching { URI(raw) }.getOrNull() ?: return null
        return raw.takeIf {
            uri.scheme?.lowercase() in listOf("http", "https") &&
                !uri.host.isNullOrBlank() && uri.rawUserInfo == null
        }
    }

    fun filter(raw: String): JSONObject? {
        val url = sourceUrl(raw) ?: return null
        return JSONObject()
            .put("kinds", JSONArray().put(KIND))
            .put("#r", JSONArray().put(url))
            .put("limit", 30)
    }

    /** Events have already passed signature verification in RelayPool. */
    fun receipts(events: List<Nip01Event>, raw: String): List<WebArchiveReceipt> {
        val source = sourceUrl(raw) ?: return emptyList()
        return events.asSequence()
            .filter { event ->
                event.kind == KIND && event.tags.any {
                    it.firstOrNull() == "r" && it.getOrNull(1)?.let(::sourceUrl) == source
                }
            }
            .sortedWith(compareByDescending<Nip01Event> { it.createdAt }.thenBy { it.id })
            .mapNotNull { event ->
                val urls = event.tags.mapNotNull { tag ->
                    if (tag.firstOrNull() == "url") tag.getOrNull(1)?.let(::httpUrl) else null
                }.filter { sourceUrl(it) != source }.distinct().take(5)
                if (urls.isEmpty()) return@mapNotNull null
                WebArchiveReceipt(
                    id = event.id,
                    urls = urls,
                    archivedAt = event.tagValue("archived-at")?.toLongOrNull()
                        ?.takeIf { it in 1..253402300799L },
                    naan = event.tagValue("tool")?.equals("naan", ignoreCase = true) == true,
                )
            }
            .distinctBy { it.urls.toSet() }
            .take(10)
            .toList()
    }

    fun lookup(raw: String, readRelays: List<String>): List<WebArchiveReceipt> {
        val filter = filter(raw) ?: return emptyList()
        val events = RelayQuery.rawQuery(
            (readRelays + RelayList.FALLBACK).distinct(), listOf(filter), maxEvents = 300,
        )
        return receipts(events, raw)
    }
}
