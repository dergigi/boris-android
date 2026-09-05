package org.dergigi.boris.nostr

/** NIP-89 handler recommendation (kind 31989) for the official Boris app card. */
object Nip89 {
    const val KIND = Nip01Event.KIND_APP_RECOMMENDATION
    const val HANDLER_KIND = 31990
    const val HANDLER_D = "org.dergigi.boris"
    const val HANDLER_PUBKEY = "6e468422dfb74a5738702a8823b9b28168abab8655faacb6853cd0ee15deee93"
    const val HANDLER_RELAY = "wss://relay.dergigi.com"
    const val RECOMMEND_KIND = "30023"

    val handlerAddress: String = "$HANDLER_KIND:$HANDLER_PUBKEY:$HANDLER_D"

    fun alreadyRecommends(event: Nip01Event?): Boolean =
        event?.tags?.any(::isBorisHandler) == true

    fun recommendationTags(existing: List<List<String>>?): List<List<String>> {
        val kept = existing.orEmpty().filter { tag ->
            tag.getOrNull(0) != "d" && !isBorisHandler(tag)
        }
        return listOf(listOf("d", RECOMMEND_KIND)) + kept +
            listOf(listOf("a", handlerAddress, HANDLER_RELAY, "android"))
    }

    private fun isBorisHandler(tag: List<String>): Boolean =
        tag.getOrNull(0) == "a" && tag.getOrNull(1) == handlerAddress
}
