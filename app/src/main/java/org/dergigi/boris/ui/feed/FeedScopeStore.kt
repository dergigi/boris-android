package org.dergigi.boris.ui.feed

import android.content.Context

object FeedScopeStore {
    const val PREFS_NAME = "boris_feed"
    const val KEY_NOSTRVERSE = "nostrverse"
    const val KEY_FRIENDS = "friends"
    const val KEY_MINE = "mine"

    fun load(context: Context): FeedScope? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_NOSTRVERSE)) return null
        val scope = FeedScope(
            nostrverse = prefs.getBoolean(KEY_NOSTRVERSE, false),
            friends = prefs.getBoolean(KEY_FRIENDS, true),
            mine = prefs.getBoolean(KEY_MINE, false),
        )
        if (!scope.nostrverse && !scope.friends && !scope.mine) return null
        return scope
    }

    fun save(context: Context, scope: FeedScope) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOSTRVERSE, scope.nostrverse)
            .putBoolean(KEY_FRIENDS, scope.friends)
            .putBoolean(KEY_MINE, scope.mine)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
