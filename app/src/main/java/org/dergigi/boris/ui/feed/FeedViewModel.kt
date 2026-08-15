package org.dergigi.boris.ui.feed

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
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip19
import org.dergigi.boris.nostr.Nip84
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.nostr.RelayList
import org.dergigi.boris.nostr.RelayQuery

data class FeedItem(
    val id: String,
    val quote: String,
    val url: String?,
    val host: String?,
    val authorHex: String,
    val authorName: String,
    val authorPicture: String?,
    val createdAt: Long,
    val level: FeedLevel,
)

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data class Ready(val items: List<FeedItem>) : FeedUiState
    data object Empty : FeedUiState
    data object Error : FeedUiState
}

class FeedViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _scope = MutableStateFlow(initialScope())
    val scope: StateFlow<FeedScope> = _scope.asStateFlow()

    private val _loggedIn = MutableStateFlow(SessionStore.load(application) != null)
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    private var catalog: List<FeedItem> = emptyList()
    private var failed = false
    private var loadJob: Job? = null

    fun refresh() {
        val keepItems = catalog.isNotEmpty()
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val session = SessionStore.load(getApplication())
            _loggedIn.value = session != null
            _scope.value = resolveScope(session != null)
            if (keepItems) {
                _refreshing.value = true
            } else {
                _state.value = FeedUiState.Loading
            }
            try {
                val items = withContext(Dispatchers.IO) {
                    loadCatalog(session?.pubkeyHex)
                }
                catalog = items
                failed = false
                publish()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed = catalog.isEmpty()
                if (failed) {
                    _state.value = FeedUiState.Error
                }
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun toggle(level: FeedLevel) {
        if (!_loggedIn.value && level != FeedLevel.Nostrverse) return
        val next = _scope.value.toggle(level)
        if (next == _scope.value) return
        _scope.value = next
        if (_loggedIn.value) {
            FeedScopeStore.save(getApplication(), next)
        }
        publish()
    }

    private fun publish() {
        if (failed && catalog.isEmpty()) {
            _state.value = FeedUiState.Error
            return
        }
        val visible = catalog.filter { _scope.value.visible(it.level) }
        _state.value = if (catalog.isEmpty()) {
            FeedUiState.Empty
        } else {
            FeedUiState.Ready(visible)
        }
    }

    private fun resolveScope(loggedIn: Boolean): FeedScope {
        if (!loggedIn) return FeedScope.LOGGED_OUT
        return FeedScopeStore.load(getApplication())
            ?: FeedScope.fromSettings(SettingsSync.settings.value)
    }

    private fun initialScope(): FeedScope {
        val loggedIn = SessionStore.load(getApplication()) != null
        return resolveScope(loggedIn)
    }

    private suspend fun loadCatalog(pubkeyHex: String?): List<FeedItem> = coroutineScope {
        val relays = buildList {
            addAll(RelayList.FALLBACK)
            if (pubkeyHex != null) addAll(RelayQuery.fetchRelayList(pubkeyHex).read)
        }.distinct()
        val friendsDeferred = async {
            if (pubkeyHex == null) emptySet() else RelayQuery.fetchContactPubkeys(pubkeyHex)
        }
        val globalDeferred = async {
            RelayQuery.fetchRecentHighlights(RelayList.FALLBACK, HIGHLIGHT_LIMIT)
        }
        val mineDeferred = async {
            if (pubkeyHex == null) {
                emptyList()
            } else {
                RelayQuery.fetchRecentHighlights(relays, HIGHLIGHT_LIMIT, pubkeyHex)
            }
        }
        val friends = friendsDeferred.await()
        val friendsEvents = if (pubkeyHex == null || friends.isEmpty()) {
            emptyList()
        } else {
            RelayQuery.fetchRecentHighlights(
                relays,
                HIGHLIGHT_LIMIT,
                authors = friends,
            )
        }
        val merged = (globalDeferred.await() + mineDeferred.await() + friendsEvents)
            .distinctBy { it.id }
            .sortedByDescending { it.createdAt }
        val authors = merged.map { it.pubkey }.distinct().take(PROFILE_LIMIT)
        val profiles = RelayQuery.fetchProfiles(relays, authors)
        toItems(merged, profiles, pubkeyHex, friends)
    }

    private fun toItems(
        events: List<Nip01Event>,
        profiles: Map<String, Profile>,
        sessionHex: String?,
        friends: Set<String>,
    ): List<FeedItem> {
        return events.map { event ->
            val url = Nip84.articleUrl(event)
            val profile = profiles[event.pubkey.lowercase()]
            FeedItem(
                id = event.id,
                quote = event.content.trim(),
                url = url,
                host = url?.let { ArticleUrl.host(it) },
                authorHex = event.pubkey,
                authorName = displayName(event.pubkey, profile),
                authorPicture = profile?.picture,
                createdAt = event.createdAt,
                level = classifyFeedLevel(event.pubkey, sessionHex, friends),
            )
        }
    }

    private fun displayName(pubkeyHex: String, profile: Profile?): String {
        profile?.name?.takeIf { it.isNotBlank() }?.let { return it }
        return try {
            val npub = Nip19.npubEncode(pubkeyHex)
            if (npub.length > 16) npub.take(12) + "…" else npub
        } catch (_: Exception) {
            pubkeyHex.take(8)
        }
    }

    companion object {
        private const val HIGHLIGHT_LIMIT = 80
        private const val PROFILE_LIMIT = 40
    }
}
