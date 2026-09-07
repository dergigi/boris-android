package org.dergigi.boris.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
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
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _relationEpoch = MutableStateFlow(0)

    val state: StateFlow<SearchUiState> =
        combine(_query, _relationEpoch) { query, epoch -> query to epoch }
            .debounce(220)
            .distinctUntilChanged()
            .transformLatest { (raw, _) ->
                val trimmed = raw.trim()
                if (trimmed.length < 2) {
                    emit(SearchUiState(query = trimmed, results = emptyList()))
                } else {
                    emit(SearchUiState(query = trimmed, results = emptyList(), isLoading = true))
                    val hits = withContext(Dispatchers.Default) {
                        val sessionHex = SessionStore.load(getApplication())?.pubkeyHex?.lowercase()
                        val friends = sessionHex
                            ?.let { RelayQuery.cachedContactPubkeys(it) }
                            .orEmpty()
                        val foaf = sessionHex
                            ?.let { RelayQuery.cachedFoafPubkeys(it, friends) }
                            .orEmpty()
                        LocalSearch.query(
                            raw = trimmed,
                            limit = Int.MAX_VALUE,
                            sessionHex = sessionHex,
                            friendPubkeys = friends,
                            foafPubkeys = foaf,
                        )
                    }
                    emit(SearchUiState(query = trimmed, results = hits))
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SearchUiState(),
            )

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun clear() {
        _query.value = ""
    }

    /** Re-resolve mine/friends against the contact cache (e.g. on resume). */
    fun refreshRelation() {
        _relationEpoch.value += 1
    }
}
