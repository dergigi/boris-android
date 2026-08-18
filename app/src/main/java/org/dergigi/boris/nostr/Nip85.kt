package org.dergigi.boris.nostr

import org.dergigi.boris.data.NostrArticle
import org.dergigi.boris.data.NostrLink
import org.dergigi.boris.data.NostrTarget
import org.dergigi.boris.data.UrlExtractor
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64
import kotlin.math.roundToInt

/**
 * Reading progress events (kind 39802), interoperable with the Boris webapp.
 * One replaceable event per article, content is plain JSON `{"progress":0.66,"ts":1734635012}`.
 */
object Nip85 {
    const val KIND = Nip01Event.KIND_READING_PROGRESS
    private const val URL_PREFIX = "url:"

    /**
     * Replaceable-event identifier matching the webapp: article coordinate for nostr
     * articles, `url:` + base64url of the raw URL for web content. Notes are not synced.
     */
    fun dTag(url: String): String? = when (val target = NostrLink.parse(url)) {
        is NostrTarget.Article -> target.ref.coordinate
        is NostrTarget.Note -> null
        is NostrTarget.Profile -> null
        null -> URL_PREFIX + Base64.getUrlEncoder().withoutPadding()
            .encodeToString(url.toByteArray(Charsets.ISO_8859_1))
    }

    fun tags(url: String): List<List<String>>? {
        val d = dTag(url) ?: return null
        return if (d.startsWith(URL_PREFIX)) {
            listOf(listOf("d", d), listOf("r", url))
        } else {
            listOf(listOf("d", d), listOf("a", d))
        }
    }

    fun contentJson(fraction: Float, ts: Long): String {
        val rounded = (fraction.coerceIn(0f, 1f) * 10000).roundToInt() / 10000.0
        val progress = if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
        return "{\"progress\":$progress,\"ts\":$ts}"
    }

    fun unsignedJson(
        url: String,
        fraction: Float,
        ts: Long,
        pubkeyHex: String? = null,
        createdAt: Long = System.currentTimeMillis() / 1000,
    ): String? {
        val tags = tags(url) ?: return null
        val tagsJson = JSONArray()
        tags.forEach { tag -> tagsJson.put(JSONArray(tag)) }
        val obj = JSONObject()
            .put("kind", KIND)
            .put("content", contentJson(fraction, ts))
            .put("tags", tagsJson)
            .put("created_at", createdAt)
        if (!pubkeyHex.isNullOrBlank()) {
            obj.put("pubkey", pubkeyHex)
        }
        return obj.toString()
    }

    /** Canonical [org.dergigi.boris.data.ReadingPositionStore] key for a progress event. */
    fun positionKey(event: Nip01Event): String? =
        event.tagValue("a")?.let { NostrArticle.fromCoordinate(it)?.coordinate }
            ?: event.tagValue("r")?.let(UrlExtractor::normalize)
            ?: event.tagValue("d")?.let(::keyFromDTag)

    fun keyFromDTag(d: String): String? = if (d.startsWith(URL_PREFIX)) {
        runCatching {
            String(Base64.getUrlDecoder().decode(d.removePrefix(URL_PREFIX)), Charsets.ISO_8859_1)
        }.getOrNull()?.let(UrlExtractor::normalize)
    } else {
        NostrArticle.fromCoordinate(d)?.coordinate
    }

    fun progress(event: Nip01Event): Float? = progress(event.content)

    fun progress(content: String): Float? = progressRegex.find(content)
        ?.groupValues?.get(1)?.toFloatOrNull()
        ?.takeIf { it in 0f..1f }

    /** When progress was recorded: content `ts` if present, else the event timestamp. */
    fun timestamp(event: Nip01Event): Long =
        tsRegex.find(event.content)?.groupValues?.get(1)?.toLongOrNull() ?: event.createdAt

    private val progressRegex =
        Regex("\"progress\"\\s*:\\s*(-?[0-9]*\\.?[0-9]+(?:[eE][+-]?[0-9]+)?)")
    private val tsRegex = Regex("\"ts\"\\s*:\\s*([0-9]+)")
}
