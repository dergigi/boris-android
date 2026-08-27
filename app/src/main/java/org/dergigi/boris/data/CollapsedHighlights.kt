package org.dergigi.boris.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Local ignore lists for collapsing feed highlights (#117). */
object CollapsedHighlights {
    private const val PREFS = "boris_collapsed_highlights"
    private const val ARTICLES = "articles"
    private const val AUTHORS = "authors"

    private val lock = Any()
    private var ready = false
    private val _articles = MutableStateFlow<Set<String>>(emptySet())
    private val _authors = MutableStateFlow<Set<String>>(emptySet())

    val articles: StateFlow<Set<String>> = _articles.asStateFlow()
    val authors: StateFlow<Set<String>> = _authors.asStateFlow()

    fun ensure(context: Context) {
        synchronized(lock) {
            if (ready) return
            val prefs = prefs(context)
            _articles.value = prefs.getStringSet(ARTICLES, emptySet())?.toSet().orEmpty()
            _authors.value = prefs.getStringSet(AUTHORS, emptySet())?.toSet().orEmpty()
            ready = true
        }
    }

    fun ignoreArticle(context: Context, key: String) {
        if (key.isBlank()) return
        ensure(context)
        writeArticles(context, _articles.value + key)
    }

    fun showArticle(context: Context, key: String) {
        ensure(context)
        writeArticles(context, _articles.value - key)
    }

    fun ignoreAuthor(context: Context, pubkeyHex: String) {
        val key = pubkeyHex.trim().lowercase()
        if (key.isEmpty()) return
        ensure(context)
        writeAuthors(context, _authors.value + key)
    }

    fun showAuthor(context: Context, pubkeyHex: String) {
        ensure(context)
        writeAuthors(context, _authors.value - pubkeyHex.trim().lowercase())
    }

    private fun writeArticles(context: Context, next: Set<String>) {
        _articles.value = next
        prefs(context).edit().putStringSet(ARTICLES, next).apply()
    }

    private fun writeAuthors(context: Context, next: Set<String>) {
        _authors.value = next
        prefs(context).edit().putStringSet(AUTHORS, next).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
