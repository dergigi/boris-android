package org.dergigi.boris.nostr

import java.net.URI

object LocalRelays {
    const val CITRINE = "ws://127.0.0.1:4869"

    private const val PROBE_TTL_MS = 5_000L
    private const val PROBE_TIMEOUT_MS = 800L

    @Volatile
    private var cachedReachable = false

    @Volatile
    private var cachedAt = 0L

    private val probeLock = Any()

    fun isLocal(url: String): Boolean = canonical(url) != null

    fun canonical(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null
        return try {
            val raw = if (trimmed.contains("://")) trimmed else "ws://$trimmed"
            val uri = URI(raw)
            val scheme = uri.scheme?.lowercase() ?: return null
            if (scheme != "ws" && scheme != "wss") return null
            val host = uri.host?.lowercase()?.trim('[', ']') ?: return null
            if (!isLocalHost(host)) return null
            val hostOut = if (host.contains(':')) "[$host]" else host
            val port = if (uri.port != -1) ":${uri.port}" else ""
            val path = uri.path.orEmpty().trimEnd('/')
            "$scheme://$hostOut$port$path"
        } catch (_: Exception) {
            null
        }
    }

    fun resolve(url: String): String? = canonical(url) ?: Nip66.normalize(url)

    fun withLocal(list: RelayList, enabled: Boolean, citrineUp: Boolean): RelayList {
        if (!enabled || !citrineUp) return list
        return RelayList(
            read = prependCitrine(list.read),
            write = prependCitrine(list.write),
        )
    }

    fun citrineReachable(force: Boolean = false): Boolean {
        val now = System.currentTimeMillis()
        if (!force && now - cachedAt < PROBE_TTL_MS) return cachedReachable
        synchronized(probeLock) {
            val inner = System.currentTimeMillis()
            if (!force && inner - cachedAt < PROBE_TTL_MS) return cachedReachable
            cachedReachable = probe(CITRINE)
            cachedAt = inner
            return cachedReachable
        }
    }

    fun remoteOnly(urls: List<String>): List<String> = urls.filter { !isLocal(it) }

    private fun prependCitrine(urls: List<String>): List<String> {
        if (urls.any(::isLocal)) return urls
        return listOf(CITRINE) + urls
    }

    private fun isLocalHost(host: String): Boolean =
        host == "127.0.0.1" || host == "localhost" || host == "::1"

    private fun probe(url: String): Boolean = RelayProbe.isReachable(url, PROBE_TIMEOUT_MS)
}
