package org.dergigi.boris.nostr

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
            runCatching { load(target.readText()) }
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
            target.writeText(encode(hints))
        }
    }

    private fun load(text: String) {
        decode(text).forEach { (pubkey, relays) ->
            val key = normalizePubkey(pubkey) ?: return@forEach
            val urls = relays.mapNotNull { LocalRelays.resolve(it) }.distinct().takeLast(MAX_HINTS)
            if (urls.isNotEmpty()) hints[key] = urls
        }
    }

    private fun normalizePubkey(value: String): String? {
        val hex = value.trim().lowercase()
        if (hex.length != 64) return null
        if (hex.any { it !in '0'..'9' && it !in 'a'..'f' }) return null
        return hex
    }

    private fun encode(map: Map<String, List<String>>): String {
        val body = map.entries.joinToString(",") { (pubkey, relays) ->
            val array = relays.joinToString(",") { "\"${escape(it)}\"" }
            "\"$pubkey\":[$array]"
        }
        return "{$body}"
    }

    private fun decode(text: String): LinkedHashMap<String, List<String>> {
        val out = LinkedHashMap<String, List<String>>()
        val trimmed = text.trim()
        if (trimmed.length < 2 || trimmed.first() != '{' || trimmed.last() != '}') return out
        var i = 1
        val end = trimmed.length - 1
        while (i < end) {
            while (i < end && trimmed[i] in ", \n\r\t") i++
            if (i >= end) break
            val key = readQuoted(trimmed, i) ?: break
            i = key.next
            while (i < end && trimmed[i] in " \n\r\t") i++
            if (i >= end || trimmed[i] != ':') break
            i++
            while (i < end && trimmed[i] in " \n\r\t") i++
            if (i >= end || trimmed[i] != '[') break
            i++
            val urls = mutableListOf<String>()
            while (i < end && trimmed[i] != ']') {
                while (i < end && trimmed[i] in ", \n\r\t") i++
                if (i < end && trimmed[i] == ']') break
                val value = readQuoted(trimmed, i) ?: break
                urls += value.text
                i = value.next
            }
            if (i < end && trimmed[i] == ']') i++
            if (urls.isNotEmpty()) out[key.text] = urls.toList()
        }
        return out
    }

    private data class Quoted(val text: String, val next: Int)

    private fun readQuoted(source: String, start: Int): Quoted? {
        if (start >= source.length || source[start] != '"') return null
        val value = StringBuilder()
        var i = start + 1
        while (i < source.length) {
            val char = source[i]
            when {
                char == '\\' && i + 1 < source.length -> {
                    value.append(source[i + 1])
                    i += 2
                }
                char == '"' -> return Quoted(value.toString(), i + 1)
                else -> {
                    value.append(char)
                    i++
                }
            }
        }
        return null
    }

    private fun escape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")
}
