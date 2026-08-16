package org.dergigi.boris.ui.you

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dergigi.boris.data.ArticleUrl
import org.dergigi.boris.data.NostrArticle
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip23
import org.dergigi.boris.nostr.Nip84
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.nostr.RelayList
import org.dergigi.boris.nostr.RelayQuery
import org.dergigi.boris.ui.feed.FeedLevel
import org.dergigi.boris.ui.feed.classifyFeedLevel

data class YouHighlight(
    val id: String,
    val quote: String,
    val context: String? = null,
    val url: String?,
    val host: String?,
    val createdAt: Long,
)

data class YouWriting(
    val id: String,
    val title: String,
    val summary: String?,
    val imageUrl: String?,
    val url: String,
    val publishedAt: Long,
)

sealed interface YouUiState {
    data object Loading : YouUiState
    data class Ready(
        val highlights: List<YouHighlight>,
        val writings: List<YouWriting>,
    ) : YouUiState
    data object Error : YouUiState
}

class YouViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow<YouUiState>(YouUiState.Loading)
    val state: StateFlow<YouUiState> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    private val _relation = MutableStateFlow(FeedLevel.Nostrverse)
    val relation: StateFlow<FeedLevel> = _relation.asStateFlow()

    private var loadJob: Job? = null
    private var pubkeyHex: String = ""

    fun refresh(pubkeyHex: String = this.pubkeyHex) {
        val key = pubkeyHex.trim().lowercase().takeIf { it.length == 64 }
            ?: SessionStore.load(getApplication())?.pubkeyHex
        if (key.isNullOrBlank()) {
            loadJob?.cancel()
            this.pubkeyHex = ""
            _profile.value = null
            _relation.value = FeedLevel.Nostrverse
            _state.value = YouUiState.Ready(emptyList(), emptyList())
            _refreshing.value = false
            return
        }
        val samePerson = key == this.pubkeyHex
        if (!samePerson) {
            _profile.value = null
        }
        this.pubkeyHex = key
        _relation.value = relationFor(key, remote = false)
        val keepItems = samePerson && _state.value is YouUiState.Ready
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            var showing = keepItems
            if (!keepItems) {
                val cached = withContext(Dispatchers.IO) { loadCached(key) }
                if (cached != null) {
                    _profile.value = cached.profile
                    _relation.value = cached.relation
                    _state.value = YouUiState.Ready(cached.highlights, cached.writings)
                    showing = true
                }
            }
            if (showing) {
                _refreshing.value = true
            } else {
                _state.value = YouUiState.Loading
            }
            try {
                val loaded = withContext(Dispatchers.IO) {
                    // Their content lives on their write relays (NIP-65 outbox).
                    val list = RelayQuery.fetchRelayList(key)
                    val relays = buildList {
                        addAll(RelayList.FALLBACK)
                        addAll(list.write)
                        addAll(list.read)
                    }.distinct()
                    coroutineScope {
                        val highlights = async {
                            RelayQuery.fetchRecentHighlights(relays, HIGHLIGHT_LIMIT, key)
                                .map { event ->
                                    val url = Nip84.articleUrl(event)
                                    YouHighlight(
                                        id = event.id,
                                        quote = event.content.trim(),
                                        context = event.tagValue("context"),
                                        url = url,
                                        host = url?.let { ArticleUrl.host(it) },
                                        createdAt = event.createdAt,
                                    )
                                }
                        }
                        val writings = async {
                            RelayQuery.fetchLongFormArticles(key, relays)
                                .mapNotNull { event -> writingFrom(event) }
                        }
                        val profile = async { RelayQuery.fetchProfile(key) }
                        val relation = async { relationFor(key, remote = true) }
                        Loaded(highlights.await(), writings.await(), profile.await(), relation.await())
                    }
                }
                _profile.value = loaded.profile
                _relation.value = loaded.relation
                _state.value = YouUiState.Ready(loaded.highlights, loaded.writings)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (_state.value !is YouUiState.Ready) {
                    _state.value = YouUiState.Error
                }
            } finally {
                _refreshing.value = false
            }
        }
    }

    private fun loadCached(key: String): Loaded? {
        val highlights = RelayQuery.cachedRecentHighlights(HIGHLIGHT_LIMIT, key).map { event ->
            val url = Nip84.articleUrl(event)
            YouHighlight(
                id = event.id,
                quote = event.content.trim(),
                context = event.tagValue("context"),
                url = url,
                host = url?.let { ArticleUrl.host(it) },
                createdAt = event.createdAt,
            )
        }
        val writings = RelayQuery.cachedRecentWritings(WRITING_LIMIT, key).mapNotNull { event ->
            writingFrom(event)
        }
        val profile = RelayQuery.cachedProfiles(listOf(key))[key]
        if (highlights.isEmpty() && writings.isEmpty() && profile == null) return null
        return Loaded(highlights, writings, profile, relationFor(key, remote = false))
    }

    private fun relationFor(profileHex: String, remote: Boolean): FeedLevel {
        val sessionHex = SessionStore.load(getApplication())?.pubkeyHex
        val friends = when {
            sessionHex == null -> emptySet()
            remote -> RelayQuery.fetchContactPubkeys(sessionHex)
            else -> RelayQuery.cachedContactPubkeys(sessionHex)
        }
        return classifyFeedLevel(profileHex, sessionHex, friends)
    }

    private data class Loaded(
        val highlights: List<YouHighlight>,
        val writings: List<YouWriting>,
        val profile: Profile?,
        val relation: FeedLevel,
    )

    companion object {
        private const val HIGHLIGHT_LIMIT = 80
        private const val WRITING_LIMIT = 200
        private const val UNTITLED = "Untitled"

        internal fun writingFrom(event: Nip01Event): YouWriting? {
            val identifier = Nip23.identifier(event) ?: return null
            val article = NostrArticle.fromCoordinate(
                "${Nip01Event.KIND_LONG_FORM}:${event.pubkey}:$identifier",
            ) ?: return null
            return YouWriting(
                id = event.id,
                title = Nip23.title(event) ?: UNTITLED,
                summary = Nip23.summary(event),
                imageUrl = Nip23.image(event),
                url = article.uri,
                publishedAt = Nip23.publishedAt(event),
            )
        }
    }
}
