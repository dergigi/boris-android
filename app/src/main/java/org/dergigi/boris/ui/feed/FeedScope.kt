package org.dergigi.boris.ui.feed

import org.dergigi.boris.data.UserSettings

enum class FeedLevel {
    Nostrverse,
    Friends,
    Mine,
}

data class FeedScope(
    val nostrverse: Boolean,
    val friends: Boolean,
    val mine: Boolean,
) {
    fun visible(level: FeedLevel): Boolean = when (level) {
        FeedLevel.Nostrverse -> nostrverse
        FeedLevel.Friends -> friends
        FeedLevel.Mine -> mine
    }

    fun toggle(level: FeedLevel): FeedScope {
        val next = when (level) {
            FeedLevel.Nostrverse -> copy(nostrverse = !nostrverse)
            FeedLevel.Friends -> copy(friends = !friends)
            FeedLevel.Mine -> copy(mine = !mine)
        }
        return if (!next.nostrverse && !next.friends && !next.mine) this else next
    }

    companion object {
        val LOGGED_OUT = FeedScope(nostrverse = true, friends = false, mine = false)

        fun fromSettings(settings: UserSettings): FeedScope = FeedScope(
            nostrverse = settings.defaultExploreScopeNostrverse,
            friends = settings.defaultExploreScopeFriends,
            mine = settings.defaultExploreScopeMine,
        )
    }
}

fun classifyFeedLevel(
    authorHex: String,
    sessionHex: String?,
    friendPubkeys: Set<String>,
): FeedLevel {
    val author = authorHex.lowercase()
    if (sessionHex != null && author == sessionHex.lowercase()) return FeedLevel.Mine
    if (author in friendPubkeys) return FeedLevel.Friends
    return FeedLevel.Nostrverse
}
