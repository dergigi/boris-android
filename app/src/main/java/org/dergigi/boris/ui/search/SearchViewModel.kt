package org.dergigi.boris.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext
import org.dergigi.boris.data.LocalSearch
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.nostr.RelayQuery

data class SearchUiState(
    val query: String = "",
    val results: List<LocalSearch.Hit> = emptyList(),
    val isLoading: Boolean = false,
    val relaySearch: RelaySearchUiState = RelaySearchUiState(),
)

data class RelaySearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val searched: Boolean = false,
    val failed: Boolean = false,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _relationEpoch = MutableStateFlow(0)
    private val _relaySearch = MutableStateFlow(RelaySearchUiState())

    val state: StateFlow<SearchUiState> =
        combine(_query, _relationEpoch, _relaySearch) { query, epoch, relaySearch ->
            SearchInput(query, epoch, relaySearch)
        }
            .debounce(QUERY_DEBOUNCE_MS)
            .distinctUntilChanged()
            .transformLatest { input ->
                val raw = input.query
                val trimmed = raw.trim()
                val relaySearch = input.relaySearch.takeIf { it.query == trimmed } ?: RelaySearchUiState()
                if (trimmed.length < 2) {
                    lastHits = emptyList()
                    emit(SearchUiState(query = trimmed, relaySearch = relaySearch))
                    return@transformLatest
                }
                emit(
                    SearchUiState(
                        query = trimmed,
                        results = lastHits.filter { LocalSearch.hitMatches(it, trimmed) },
                        isLoading = true,
                        relaySearch = relaySearch,
                    ),
                )
                val hits = localHits(trimmed)
                lastHits = hits
                emit(SearchUiState(query = trimmed, results = hits, relaySearch = relaySearch))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SearchUiState(),
            )

    private var lastHits: List<LocalSearch.Hit> = emptyList()

    fun onQueryChange(value: String) {
        if (value.trim() != _query.value.trim()) {
            _relaySearch.value = RelaySearchUiState()
        }
        _query.value = value
    }

    fun clear() {
        _query.value = ""
        _relaySearch.value = RelaySearchUiState()
    }

    fun expandSearchToRelays() {
        val trimmed = _query.value.trim()
        if (trimmed.length < 2) return
        val current = _relaySearch.value
        if (current.query == trimmed && current.isLoading) return
        viewModelScope.launch {
            _relaySearch.value = RelaySearchUiState(query = trimmed, isLoading = true)
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    RelayQuery.searchNip50(trimmed)
                }.isSuccess
            }
            _relaySearch.value = RelaySearchUiState(
                query = trimmed,
                searched = ok,
                failed = !ok,
            )
        }
    }

    /** Re-resolve mine/friends against the contact cache (e.g. on resume). */
    fun refreshRelation() {
        _relationEpoch.value += 1
    }

    private suspend fun localHits(trimmed: String): List<LocalSearch.Hit> =
        withContext(Dispatchers.Default) {
            val sessionHex = SessionStore.load(getApplication())?.pubkeyHex?.lowercase()
            val friends = sessionHex
                ?.let { RelayQuery.cachedContactPubkeys(it) }
                .orEmpty()
            val foaf = sessionHex
                ?.let { RelayQuery.cachedFoafPubkeys(it, friends) }
                .orEmpty()
            LocalSearch.query(
                raw = trimmed,
                limit = LocalSearch.DEFAULT_LIMIT,
                sessionHex = sessionHex,
                friendPubkeys = friends,
                foafPubkeys = foaf,
            )
        }

    private data class SearchInput(
        val query: String,
        val relationEpoch: Int,
        val relaySearch: RelaySearchUiState,
    )

    companion object {
        private const val QUERY_DEBOUNCE_MS = 80L
    }
}
