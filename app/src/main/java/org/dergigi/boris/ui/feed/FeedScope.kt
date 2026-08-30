package org.dergigi.boris.ui.feed

import org.dergigi.boris.data.UserSettings

enum class FeedLevel {
    Nostrverse,
    Foaf,
    Friends,
    Mine,
}

data class FeedScope(
    val nostrverse: Boolean,
    val friends: Boolean,
    val mine: Boolean,
    val foaf: Boolean = false,
) {
    fun visible(level: FeedLevel): Boolean = when (level) {
        FeedLevel.Nostrverse -> nostrverse
        FeedLevel.Foaf -> foaf
        FeedLevel.Friends -> friends
        FeedLevel.Mine -> mine
    }

    fun toggle(level: FeedLevel): FeedScope {
        val next = when (level) {
            FeedLevel.Nostrverse -> copy(nostrverse = !nostrverse)
            FeedLevel.Foaf -> copy(foaf = !foaf)
            FeedLevel.Friends -> copy(friends = !friends)
            FeedLevel.Mine -> copy(mine = !mine)
        }
        return if (!next.nostrverse && !next.friends && !next.mine && !next.foaf) this else next
    }

    companion object {
        val LOGGED_OUT = FeedScope(nostrverse = true, friends = false, mine = false, foaf = false)

        fun fromSettings(settings: UserSettings): FeedScope = FeedScope(
            nostrverse = settings.defaultExploreScopeNostrverse,
            friends = settings.defaultExploreScopeFriends,
            mine = settings.defaultExploreScopeMine,
            foaf = settings.defaultExploreScopeFoaf,
        )
    }
}

fun UserSettings.withExploreScope(scope: FeedScope): UserSettings =
    withBoolean("defaultExploreScopeNostrverse", scope.nostrverse)
        .withBoolean("defaultExploreScopeFriends", scope.friends)
        .withBoolean("defaultExploreScopeMine", scope.mine)
        .withBoolean("defaultExploreScopeFoaf", scope.foaf)

fun classifyFeedLevel(
    authorHex: String,
    sessionHex: String?,
    friendPubkeys: Set<String>,
    foafPubkeys: Set<String> = emptySet(),
): FeedLevel {
    val author = authorHex.lowercase()
    if (sessionHex != null && author == sessionHex.lowercase()) return FeedLevel.Mine
    if (author in friendPubkeys) return FeedLevel.Friends
    if (author in foafPubkeys) return FeedLevel.Foaf
    return FeedLevel.Nostrverse
}

/** People followed by [friendPubkeys], minus you and those friends. */
fun foafPubkeys(
    selfHex: String?,
    friendPubkeys: Set<String>,
    contactsOf: (String) -> Set<String>,
): Set<String> {
    val self = selfHex?.lowercase()
    val friends = friendPubkeys.map { it.lowercase() }.toSet()
    val out = mutableSetOf<String>()
    for (friend in friends) {
        for (hex in contactsOf(friend)) {
            val key = hex.lowercase()
            if (key != self && key !in friends) out += key
        }
    }
    return out
}

fun foafFetchAuthors(foaf: Set<String>, cap: Int = 80): Set<String> =
    if (foaf.size <= cap) foaf else foaf.sorted().take(cap).toSet()
