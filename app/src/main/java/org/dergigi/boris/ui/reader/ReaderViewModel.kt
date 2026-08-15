package org.dergigi.boris.ui.reader

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dergigi.boris.R
import org.dergigi.boris.data.ReadableContent
import org.dergigi.boris.data.ReaderRepository
import org.dergigi.boris.data.SecretBox
import org.dergigi.boris.data.Session
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.nostr.BunkerClient
import org.dergigi.boris.nostr.BunkerSignResult
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip84
import org.dergigi.boris.nostr.PendingUnsignedEvent
import org.dergigi.boris.nostr.RelayList
import org.dergigi.boris.nostr.RelayQuery
import org.dergigi.boris.nostr.RemoteSignerBridge
import org.dergigi.boris.nostr.SignerResult
import org.dergigi.boris.nostr.SignerResults
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class PaintedHighlight(
    val id: String,
    val quote: String,
    val mine: Boolean,
)

class ReaderViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val repository = ReaderRepository()
    val url: String = decodeUrl(savedStateHandle.get<String>(URL_ARG).orEmpty())

    private val _state = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    private val _gallery = MutableStateFlow<ImageGalleryState?>(null)
    val gallery: StateFlow<ImageGalleryState?> = _gallery.asStateFlow()

    private val _highlights = MutableStateFlow<List<PaintedHighlight>>(emptyList())
    val highlights: StateFlow<List<PaintedHighlight>> = _highlights.asStateFlow()

    private val _highlightCount = MutableStateFlow(0)
    val highlightCount: StateFlow<Int> = _highlightCount.asStateFlow()

    private val _loggedIn = MutableStateFlow(SessionStore.load(application) != null)
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var highlightJob: Job? = null
    private var pendingUnsigned: PendingUnsignedEvent? = null

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

    fun consumeMessage() {
        _message.value = null
    }

    fun load() {
        if (url.isBlank()) {
            _state.value = ReaderUiState.Error("No URL to read.", url)
            _highlights.value = emptyList()
            _highlightCount.value = 0
            return
        }
        viewModelScope.launch {
            _state.value = ReaderUiState.Loading
            _highlights.value = emptyList()
            _highlightCount.value = 0
            try {
                val content = withContext(Dispatchers.IO) { repository.fetch(url) }
                _state.value = ReaderUiState.Ready(content)
                startHighlightFetch(content)
            } catch (e: Exception) {
                highlightJob?.cancel()
                _highlights.value = emptyList()
                _highlightCount.value = 0
                _state.value = ReaderUiState.Error(
                    e.message ?: "Failed to load this article.",
                    url,
                )
            }
        }
    }

    fun highlight(quote: String): Intent? {
        val trimmed = quote.trim()
        val app = getApplication<Application>()
        if (trimmed.isBlank()) {
            _message.value = app.getString(R.string.highlight_cancelled)
            return null
        }
        val session = SessionStore.load(app) ?: return null
        val content = (_state.value as? ReaderUiState.Ready)?.content ?: return null
        val context = Nip84.extractContext(trimmed, content.body)
        when (session) {
            is Session.Amber -> {
                val createdAt = System.currentTimeMillis() / 1000
                val tags = Nip84.tags(
                    content.url,
                    context,
                    content.articleCoordinate,
                    content.eventId,
                    content.authorPubkey,
                )
                pendingUnsigned = PendingUnsignedEvent(
                    pubkey = session.pubkeyHex,
                    createdAt = createdAt,
                    kind = Nip01Event.KIND_HIGHLIGHT,
                    tags = tags,
                    content = trimmed,
                )
                val unsigned = Nip84.unsignedJson(
                    trimmed,
                    content.url,
                    context,
                    session.pubkeyHex,
                    createdAt,
                    content.articleCoordinate,
                    content.eventId,
                    content.authorPubkey,
                )
                return RemoteSignerBridge.buildSignEventIntent(
                    unsigned,
                    session.signerPackage,
                    session.pubkeyHex,
                )
            }
            is Session.Bunker -> {
                pendingUnsigned = null
                val unsigned = Nip84.unsignedJson(
                    trimmed,
                    content.url,
                    context,
                    pubkeyHex = null,
                    coordinate = content.articleCoordinate,
                    eventId = content.eventId,
                    authorPubkey = content.authorPubkey,
                )
                viewModelScope.launch {
                    signWithBunker(session, unsigned)
                }
                return null
            }
        }
    }

    fun onSignerResult(resultCode: Int, data: Intent?) {
        val app = getApplication<Application>()
        val session = SessionStore.load(app) ?: return
        val pending = pendingUnsigned
        pendingUnsigned = null
        when (val result = SignerResults.parseSignedEvent(resultCode, data, session.pubkeyHex, pending)) {
            is SignerResult.Signed -> onSignedEvent(result.event)
            SignerResult.Rejected -> {
                _message.value = app.getString(R.string.highlight_rejected)
            }
            SignerResult.Cancelled -> {
                _message.value = app.getString(R.string.highlight_cancelled)
            }
            is SignerResult.Success -> {
                _message.value = app.getString(R.string.highlight_cancelled)
            }
        }
    }

    fun onSignedEvent(event: Nip01Event) {
        val app = getApplication<Application>()
        val session = SessionStore.load(app) ?: return
        if (event.kind != Nip01Event.KIND_HIGHLIGHT) return
        if (!event.pubkey.equals(session.pubkeyHex, ignoreCase = true)) return
        if (!event.verify()) return
        val painted = PaintedHighlight(event.id, event.content, mine = true)
        if (_highlights.value.none { it.id == event.id }) {
            _highlights.value = _highlights.value + painted
            _highlightCount.value = _highlightCount.value + 1
        }
        viewModelScope.launch(Dispatchers.IO) {
            val published = try {
                val relays = RelayQuery.fetchRelayList(session.pubkeyHex)
                RelayQuery.publish(relays.write, event)
            } catch (_: Exception) {
                false
            }
            if (!published) {
                _highlights.value = _highlights.value.filterNot { it.id == event.id }
                _highlightCount.value = (_highlightCount.value - 1).coerceAtLeast(0)
                _message.value = app.getString(R.string.highlight_not_published)
            }
        }
    }

    private suspend fun signWithBunker(session: Session.Bunker, unsignedJson: String) {
        val app = getApplication<Application>()
        val privkey = SecretBox.unwrap(app, session.clientPrivkeyCiphertext)
        if (privkey == null) {
            _message.value = app.getString(R.string.highlight_cancelled)
            return
        }
        try {
            val result = withContext(Dispatchers.IO) {
                BunkerClient(onAuthUrl = ::openAuthUrl).signEvent(
                    session.relays,
                    session.remoteSignerPubkey,
                    privkey,
                    unsignedJson,
                )
            }
            when (result) {
                is BunkerSignResult.Signed -> onSignedEvent(result.event)
                BunkerSignResult.Rejected -> {
                    _message.value = app.getString(R.string.highlight_rejected)
                }
                BunkerSignResult.RelayTimeout -> {
                    _message.value = app.getString(R.string.highlight_cancelled)
                }
            }
        } finally {
            privkey.fill(0)
        }
    }

    private fun startHighlightFetch(content: ReadableContent) {
        highlightJob?.cancel()
        val session = SessionStore.load(getApplication())
        _loggedIn.value = session != null
        highlightJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val relays = buildList {
                    addAll(RelayList.FALLBACK)
                    if (session != null) addAll(RelayQuery.fetchRelayList(session.pubkeyHex).read)
                }.distinct()
                val events = if (!content.articleCoordinate.isNullOrBlank()) {
                    RelayQuery.fetchHighlightsForArticle(
                        relays,
                        content.articleCoordinate,
                        content.eventId,
                    )
                } else {
                    RelayQuery.fetchHighlights(relays, content.url)
                }
                _highlightCount.value = events.size
                _highlights.value = events.map { event ->
                    PaintedHighlight(
                        id = event.id,
                        quote = event.content,
                        mine = session != null && event.pubkey.equals(session.pubkeyHex, ignoreCase = true),
                    )
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun openAuthUrl(url: String) {
        val app = getApplication<Application>()
        runCatching {
            app.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
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
