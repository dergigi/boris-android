package org.dergigi.boris.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.dergigi.boris.data.ReadableContent
import org.dergigi.boris.data.ReaderRepository
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
) : ViewModel() {
    private val repository = ReaderRepository()
    val url: String = decodeUrl(savedStateHandle.get<String>(URL_ARG).orEmpty())

    private val _state = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    private val _gallery = MutableStateFlow<ImageGalleryState?>(null)
    val gallery: StateFlow<ImageGalleryState?> = _gallery.asStateFlow()

    init {
        load()
    }

    fun openGallery(urls: List<String>, index: Int) {
        if (urls.isEmpty()) return
        _gallery.value = ImageGalleryState(
            urls = urls,
            initialIndex = index.coerceIn(0, urls.lastIndex),
        )
    }

    fun closeGallery() {
        _gallery.value = null
    }

    fun setGalleryIndex(index: Int) {
        val current = _gallery.value ?: return
        val next = index.coerceIn(0, current.urls.lastIndex)
        if (next == current.initialIndex) return
        _gallery.value = current.copy(initialIndex = next)
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
