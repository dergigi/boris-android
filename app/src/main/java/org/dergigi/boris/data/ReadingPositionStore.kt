package org.dergigi.boris.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.io.File

/** Device-local reading positions, keyed by article identity. Values are scroll fractions (0..1). */
object ReadingPositionStore {
    private const val MAX_ENTRIES = 500
    private val lock = Any()
    private var file: File? = null
    private val positions = LinkedHashMap<String, Entry>()

    private data class Entry(val fraction: Float, val updatedAt: Long)

    private val _version = MutableStateFlow(0)

    /** Bumped on every save so composables can observe changes. */
    val version: StateFlow<Int> = _version

    fun init(target: File) {
        synchronized(lock) {
            file = target
            positions.clear()
            if (target.exists()) {
                runCatching {
                    val obj = JSONObject(target.readText())
                    for (key in obj.keys()) {
                        val value = obj.optJSONObject(key)
                        positions[key] = if (value != null) {
                            Entry(value.optDouble("f", 0.0).toFloat(), value.optLong("t", 0L))
                        } else {
                            // Legacy format: plain fraction without timestamp.
                            Entry(obj.getDouble(key).toFloat(), 0L)
                        }
                    }
                }
            }
        }
    }

    /**
     * Canonical identity for a reader URL: nostr articles by coordinate, notes by
     * event id, web URLs normalized. Card URLs and fetched content URLs may differ
     * in relay hints or scheme; this makes them hit the same entry.
     */
    fun key(url: String): String = when (val target = NostrLink.parse(url)) {
        is NostrTarget.Article -> target.ref.coordinate
        is NostrTarget.Note -> target.eventId.lowercase()
        is NostrTarget.Profile -> target.uri
        null -> UrlExtractor.normalize(url)
    }

    fun fraction(url: String): Float = synchronized(lock) { positions[key(url)]?.fraction ?: 0f }

    /** Unix seconds when the position was last updated, 0 if unknown. */
    fun updatedAt(url: String): Long = synchronized(lock) { positions[key(url)]?.updatedAt ?: 0L }

    /** All saved positions, most recently read first. Keys are canonical (see [key]). */
    fun entries(): List<Pair<String, Float>> = synchronized(lock) {
        positions.entries.reversed()
            .sortedByDescending { it.value.updatedAt }
            .map { it.key to it.value.fraction }
    }

    fun save(url: String, fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        synchronized(lock) {
            put(key(url), Entry(clamped, System.currentTimeMillis() / 1000))
        }
        _version.value++
    }

    /**
     * Clears progress for [url] by writing 0 with a fresh timestamp so a later
     * remote sync cannot restore an older position on this device.
     */
    fun reset(url: String): Boolean {
        if (fraction(url) <= 0f) return false
        save(url, 0f)
        return true
    }

    /**
     * Applies a position synced from another device. Newest timestamp wins;
     * returns false when the local entry is same-aged or newer.
     */
    fun merge(key: String, fraction: Float, updatedAt: Long): Boolean {
        synchronized(lock) {
            val existing = positions[key]
            if (existing != null && existing.updatedAt >= updatedAt) return false
            put(key, Entry(fraction.coerceIn(0f, 1f), updatedAt))
        }
        _version.value++
        return true
    }

    private fun put(key: String, entry: Entry) {
        positions.remove(key)
        positions[key] = entry
        while (positions.size > MAX_ENTRIES) {
            positions.remove(positions.keys.first())
        }
        persist()
    }

    private fun persist() {
        val target = file ?: return
        runCatching {
            val obj = JSONObject()
            positions.forEach { (k, v) ->
                obj.put(k, JSONObject().put("f", v.fraction.toDouble()).put("t", v.updatedAt))
            }
            target.writeText(obj.toString())
        }
    }
}
