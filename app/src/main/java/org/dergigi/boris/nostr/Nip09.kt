package org.dergigi.boris.nostr

/** NIP-09 event deletion requests (kind 5). */
object Nip09 {
    const val REASON = "Deleted by user"

    fun tags(eventId: String, kind: Int): List<List<String>> = listOf(
        listOf("e", eventId),
        listOf("k", kind.toString()),
    )

    fun unsignedJson(
        eventId: String,
        kind: Int,
        pubkeyHex: String? = null,
        createdAt: Long = System.currentTimeMillis() / 1000,
    ): String = Nip01Event.unsignedJson(
        Nip01Event.KIND_DELETION,
        REASON,
        tags(eventId, kind),
        pubkeyHex,
        createdAt,
    )
}
