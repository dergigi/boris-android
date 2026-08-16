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
    private val positions = LinkedHashMap<String, Float>()

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
                        positions[key] = obj.getDouble(key).toFloat()
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
        null -> UrlExtractor.normalize(url)
    }

    fun fraction(url: String): Float = synchronized(lock) { positions[key(url)] ?: 0f }

    /** All saved positions, most recently read first. Keys are canonical (see [key]). */
    fun entries(): List<Pair<String, Float>> = synchronized(lock) {
        positions.entries.reversed().map { it.key to it.value }
    }

    fun save(url: String, fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        synchronized(lock) {
            val k = key(url)
            positions.remove(k)
            positions[k] = clamped
            while (positions.size > MAX_ENTRIES) {
                positions.remove(positions.keys.first())
            }
            val target = file ?: return@synchronized
            runCatching {
                val obj = JSONObject()
                positions.forEach { (key, value) -> obj.put(key, value.toDouble()) }
                target.writeText(obj.toString())
            }
        }
        _version.value++
    }
}
