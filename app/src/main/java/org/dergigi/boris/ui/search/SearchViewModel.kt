package org.dergigi.boris.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.dergigi.boris.data.LocalSearch

data class SearchUiState(
    val query: String = "",
    val results: List<LocalSearch.Hit> = emptyList(),
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val state: StateFlow<SearchUiState> = _query
        .debounce(220)
        .distinctUntilChanged()
        .mapLatest { raw ->
            val trimmed = raw.trim()
            if (trimmed.length < 2) {
                SearchUiState(query = trimmed, results = emptyList())
            } else {
                val hits = withContext(Dispatchers.Default) {
                    LocalSearch.query(trimmed)
                }
                SearchUiState(query = trimmed, results = hits)
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
}
