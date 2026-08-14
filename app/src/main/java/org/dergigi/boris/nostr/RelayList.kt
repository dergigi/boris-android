package org.dergigi.boris.nostr

data class RelayList(
    val read: List<String>,
    val write: List<String>,
) {
    companion object {
        val FALLBACK = listOf(
            "wss://relay.damus.io",
            "wss://nos.lol",
            "wss://relay.primal.net",
            "wss://wot.dergigi.com",
        )

        fun fallback(): RelayList = RelayList(FALLBACK, FALLBACK)

        fun parse(events: List<Nip01Event>): RelayList {
            val newest = events
                .filter { it.kind == Nip01Event.KIND_RELAY_LIST }
                .maxByOrNull { it.createdAt }
                ?: return fallback()
            val read = mutableListOf<String>()
            val write = mutableListOf<String>()
            for (tag in newest.tags) {
                if (tag.size < 2 || tag[0] != "r") continue
                val url = tag[1].trim()
                if (!url.startsWith("wss://", ignoreCase = true)) continue
                when (tag.getOrNull(2)?.lowercase()) {
                    null, "" -> {
                        read.add(url)
                        write.add(url)
                    }
                    "read" -> read.add(url)
                    "write" -> write.add(url)
                }
            }
            return RelayList(
                read = read.ifEmpty { FALLBACK },
                write = write.ifEmpty { FALLBACK },
            )
        }
    }
}
