package org.dergigi.boris.nostr

data class SocialGraph(
    val pubkey: String?,
    val relays: List<String>,
    val friends: Set<String>,
    val foaf: Set<String>,
)

object SocialGraphs {
    fun cached(pubkey: String?): SocialGraph {
        if (pubkey == null) return empty()
        val friends = RelayQuery.cachedContactPubkeys(pubkey)
        return SocialGraph(
            pubkey = pubkey,
            relays = RelayList.FALLBACK,
            friends = friends,
            foaf = RelayQuery.cachedFoafPubkeys(pubkey, friends),
        )
    }

    fun fetch(pubkey: String?): SocialGraph {
        if (pubkey == null) return empty()
        val relays = buildList {
            addAll(RelayList.FALLBACK)
            addAll(RelayQuery.fetchRelayList(pubkey).read)
        }.distinct()
        val friends = RelayQuery.fetchContactPubkeys(pubkey)
        return SocialGraph(
            pubkey = pubkey,
            relays = relays,
            friends = friends,
            foaf = RelayQuery.fetchFoafPubkeys(pubkey, friends),
        )
    }

    private fun empty() = SocialGraph(
        pubkey = null,
        relays = RelayList.FALLBACK,
        friends = emptySet(),
        foaf = emptySet(),
    )
}
