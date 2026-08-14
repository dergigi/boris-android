package com.readwithboris.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readwithboris.data.ReadableContent
import com.readwithboris.data.ReaderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class ReaderViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ReaderRepository = ReaderRepository(),
) : ViewModel() {
    val url: String = decodeUrl(savedStateHandle.get<String>(URL_ARG).orEmpty())

    private val _state = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (url.isBlank()) {
            _state.value = ReaderUiState.Error("No URL to read.", url)
            return
        }
        viewModelScope.launch {
            _state.value = ReaderUiState.Loading
            try {
                val content = withContext(Dispatchers.IO) { repository.fetch(url) }
                _state.value = ReaderUiState.Ready(content)
            } catch (e: Exception) {
                _state.value = ReaderUiState.Error(
                    e.message ?: "Failed to load this article.",
                    url,
                )
            }
        }
    }

    companion object {
        const val URL_ARG = "url"

        fun decodeUrl(encoded: String): String =
            URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
    }
}

sealed interface ReaderUiState {
    data object Loading : ReaderUiState
    data class Ready(val content: ReadableContent) : ReaderUiState
    data class Error(val message: String, val url: String) : ReaderUiState
}
