package org.dergigi.boris.nostr

object Nip66 {
    const val KIND = Nip01Event.KIND_RELAY_DISCOVERY

    val MONITOR_RELAYS = listOf(
        "wss://relay.nostr.watch",
        "wss://relaypag.es",
        "wss://monitorlizard.nostr1.com",
    )

    private val IP_HOST = Regex("""^\d{1,3}(?:\.\d{1,3}){3}$""")

    fun normalize(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null
        return try {
            val raw = if (trimmed.contains("://")) trimmed else "wss://$trimmed"
            val uri = java.net.URI(raw)
            if (!uri.scheme.equals("wss", ignoreCase = true)) return null
            val host = uri.host?.lowercase() ?: return null
            if (!isAcceptableHost(host)) return null
            val port = if (uri.port != -1) ":${uri.port}" else ""
            val path = uri.path.orEmpty().trimEnd('/')
            "wss://$host$port$path"
        } catch (_: Exception) {
            null
        }
    }

    fun select(
        events: List<Nip01Event>,
        seed: List<String>,
        limit: Int = 12,
    ): List<String> {
        val ranked = events
            .filter { it.kind == KIND }
            .mapNotNull { event ->
                val url = normalize(dTag(event) ?: return@mapNotNull null) ?: return@mapNotNull null
                if (!isClearnet(event)) return@mapNotNull null
                if (requiresBarrier(event)) return@mapNotNull null
                url to (rttOpen(event) ?: Int.MAX_VALUE)
            }
            .groupBy({ it.first }, { it.second })
            .map { (url, rtts) -> url to rtts.min() }
            .sortedBy { it.second }
            .map { it.first }

        val out = LinkedHashSet<String>()
        for (url in seed) {
            normalize(url)?.let { out.add(it) }
        }
        for (url in ranked) {
            if (out.size >= limit) break
            out.add(url)
        }
        return out.toList()
    }

    internal fun dTag(event: Nip01Event): String? =
        event.tags.firstOrNull { it.size >= 2 && it[0] == "d" }?.get(1)

    internal fun isClearnet(event: Nip01Event): Boolean {
        val networks = event.tags
            .filter { it.size >= 2 && it[0] == "n" }
            .map { it[1].lowercase() }
        if (networks.isEmpty()) return true
        return networks.contains("clearnet")
    }

    internal fun requiresBarrier(event: Nip01Event): Boolean {
        val requirements = event.tags
            .filter { it.size >= 2 && it[0] == "R" }
            .map { it[1].lowercase() }
        return requirements.contains("auth") || requirements.contains("payment")
    }

    internal fun rttOpen(event: Nip01Event): Int? =
        event.tags.firstOrNull { it.size >= 2 && it[0] == "rtt-open" }?.get(1)?.toIntOrNull()

    private fun isAcceptableHost(host: String): Boolean {
        if (host == "localhost" || host.endsWith(".localhost")) return false
        if (host.endsWith(".onion") || host.endsWith(".i2p") || host.endsWith(".loki")) return false
        if (IP_HOST.matches(host)) return false
        return true
    }
}
