package org.dergigi.boris.nostr

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object HintedRelays {
    const val MAX_HINTS = 8
    private const val MAX_ENTRIES = 500

    private val lock = Any()
    private var file: File? = null
    private val hints = LinkedHashMap<String, List<String>>()

    fun init(target: File) {
        synchronized(lock) {
            file = target
            hints.clear()
            if (!target.exists()) return
            runCatching {
                val obj = JSONObject(target.readText())
                for (key in obj.keys()) {
                    val pubkey = normalizePubkey(key) ?: continue
                    val array = obj.optJSONArray(key) ?: continue
                    val relays = buildList {
                        for (i in 0 until array.length()) {
                            val url = LocalRelays.resolve(array.optString(i)) ?: continue
                            add(url)
                        }
                    }.distinct().takeLast(MAX_HINTS)
                    if (relays.isNotEmpty()) hints[pubkey] = relays
                }
            }
        }
    }

    fun remember(pubkeyHex: String, relays: List<String>) {
        if (relays.isEmpty()) return
        val key = normalizePubkey(pubkeyHex) ?: return
        val incoming = relays.mapNotNull { LocalRelays.resolve(it) }.distinct()
        if (incoming.isEmpty()) return
        synchronized(lock) {
            val merged = ((hints[key] ?: emptyList()) + incoming).distinct()
            val capped = merged.takeLast(MAX_HINTS)
            hints.remove(key)
            hints[key] = capped
            while (hints.size > MAX_ENTRIES) {
                hints.remove(hints.keys.first())
            }
            persist()
        }
    }

    fun forPubkey(pubkeyHex: String): List<String> {
        val key = normalizePubkey(pubkeyHex) ?: return emptyList()
        return synchronized(lock) { hints[key]?.toList().orEmpty() }
    }

    internal fun clear() {
        synchronized(lock) {
            hints.clear()
        }
    }

    private fun persist() {
        val target = file ?: return
        runCatching {
            target.parentFile?.mkdirs()
            val obj = JSONObject()
            hints.forEach { (pubkey, relays) ->
                obj.put(pubkey, JSONArray().apply { relays.forEach { put(it) } })
            }
            target.writeText(obj.toString())
        }
    }

    private fun normalizePubkey(value: String): String? {
        val hex = value.trim().lowercase()
        if (hex.length != 64) return null
        if (hex.any { it !in '0'..'9' && it !in 'a'..'f' }) return null
        return hex
    }
}
