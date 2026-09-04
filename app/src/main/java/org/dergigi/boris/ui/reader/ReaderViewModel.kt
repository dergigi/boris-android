package org.dergigi.boris.ui.reader

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dergigi.boris.data.ArticlePreview
import org.dergigi.boris.data.HtmlToMarkdown
import org.dergigi.boris.data.LibrarySave
import org.dergigi.boris.data.NostrEventRefs
import org.dergigi.boris.data.ReadableContent
import org.dergigi.boris.data.OpenedHighlight
import org.dergigi.boris.data.ReaderFetchException
import org.dergigi.boris.data.ReaderHighlightException
import org.dergigi.boris.data.ReaderImageException
import org.dergigi.boris.data.ResolvedEventRef
import org.dergigi.boris.data.UrlExtractor
import org.dergigi.boris.data.takeIfActive
import org.dergigi.boris.data.ReaderRepository
import org.dergigi.boris.data.RssRepository
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.nostr.Archive
import org.dergigi.boris.nostr.ArticleReaction
import org.dergigi.boris.nostr.ArticleReactions
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.nostr.RelayList
import org.dergigi.boris.nostr.RelayQuery
import org.dergigi.boris.ui.ArchiveAction
import org.dergigi.boris.ui.LibrarySaveAction
import org.dergigi.boris.ui.ReactionAction

class ReaderViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val repository = ReaderRepository()
    // NavType.StringType already decodes query args once; do not URLDecoder again.
    val url: String = savedStateHandle.get<String>(URL_ARG).orEmpty()
    val focusHighlightId: String =
        savedStateHandle.get<String>(HIGHLIGHT_ARG).orEmpty().trim().lowercase()

    private val _state = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    private val _gallery = MutableStateFlow<ImageGalleryState?>(null)
    val gallery: StateFlow<ImageGalleryState?> = _gallery.asStateFlow()

    val highlights: StateFlow<List<PaintedHighlight>> get() = readerHighlights.highlights
    val highlightCount: StateFlow<Int> get() = readerHighlights.highlightCount
    val highlightsLoaded: StateFlow<Boolean> get() = readerHighlights.highlightsLoaded

    private val _loggedIn = MutableStateFlow(SessionStore.load(application) != null)
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    private val _canSave = MutableStateFlow(false)
    val canSave: StateFlow<Boolean> = _canSave.asStateFlow()

    private val _inLibrary = MutableStateFlow(false)
    val inLibrary: StateFlow<Boolean> = _inLibrary.asStateFlow()

    private val _archived = MutableStateFlow(false)
    val archived: StateFlow<Boolean> = _archived.asStateFlow()

    private val _reaction = MutableStateFlow<ArticleReaction?>(null)
    val reaction: StateFlow<ArticleReaction?> = _reaction.asStateFlow()

    private val _canReact = MutableStateFlow(false)
    val canReact: StateFlow<Boolean> = _canReact.asStateFlow()

    private val _author = MutableStateFlow<Profile?>(null)
    val author: StateFlow<Profile?> = _author.asStateFlow()

    private val _rssFeedSuggestion = MutableStateFlow<String?>(null)
    val rssFeedSuggestion: StateFlow<String?> = _rssFeedSuggestion.asStateFlow()

    private val _signIntent = MutableStateFlow<Intent?>(null)
    val signIntent: StateFlow<Intent?> = _signIntent.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _eventRefs = MutableStateFlow<Map<String, ResolvedEventRef>>(emptyMap())
    val eventRefs: StateFlow<Map<String, ResolvedEventRef>> = _eventRefs.asStateFlow()

    private var membershipJob: Job? = null
    private var archiveJob: Job? = null
    private var authorJob: Job? = null
    private var rssFeedJob: Job? = null
    private var eventRefJob: Job? = null
    private var loadJob: Job? = null
    private var saving = false
    private val readerHighlights = ReaderHighlights(
        app = application,
        scope = viewModelScope,
        onMessage = { _message.value = it },
        onSignIntent = { _signIntent.value = it },
        currentContent = { (_state.value as? ReaderUiState.Ready)?.content },
        onLoggedIn = { _loggedIn.value = it },
        onPublishSaveState = { publishSaveState() },
    )
    private val librarySave = LibrarySaveAction(
        app = application,
        scope = viewModelScope,
        onMessage = { text ->
            _message.value = text
            if (!_inLibrary.value) {
                saving = false
                publishSaveState()
            }
        },
        onSignIntent = { _signIntent.value = it },
        onSaved = {
            _inLibrary.value = true
            saving = false
            publishSaveState()
        },
    )
    private val archiveAction = ArchiveAction(
        app = application,
        scope = viewModelScope,
        onMessage = { text ->
            _message.value = text
            archiving = false
        },
        onArchived = { _, _, eventId ->
            archiveIds = listOf(eventId)
            _archived.value = true
            archiving = false
        },
        onUnarchived = {
            archiveIds = emptyList()
            _archived.value = false
            archiving = false
        },
    )
    private var archiving = false
    private var archiveIds = emptyList<String>()
    private val reactionAction = ReactionAction(
        app = application,
        scope = viewModelScope,
        onMessage = { text -> _message.value = text },
        onReacted = { reaction, eventId ->
            reactionIds = listOf(eventId)
            _reaction.value = reaction
        },
        onRemoved = {
            reactionIds = emptyList()
            _reaction.value = null
        },
    )
    private var reactionIds = emptyList<String>()
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

    fun consumeSignIntent() {
        _signIntent.value = null
    }

    /** Bypasses the parsed-article cache and re-runs the full fetch + parse. */
    fun refresh() = load(refresh = true)

    fun load(refresh: Boolean = false) {
        if (url.isBlank()) {
            _state.value = readerErrorState("No URL to read.", url)
            readerHighlights.clear(loaded = true)
            _inLibrary.value = false
            resetArchive()
            resetAuthor()
            publishSaveState()
            return
        }
        val imageUrl = UrlExtractor.preferHttps(url).takeIf { UrlExtractor.isImageUrl(it) }
        loadJob?.cancel()
        readerHighlights.cancel()
        membershipJob?.cancel()
        archiveJob?.cancel()
        authorJob?.cancel()
        rssFeedJob?.cancel()
        eventRefJob?.cancel()
        _eventRefs.value = emptyMap()
        if (imageUrl != null) {
            _state.value = ReaderUiState.ImageOnly(imageUrl)
            openGallery(listOf(imageUrl), 0)
            readerHighlights.clear(loaded = true)
            _inLibrary.value = false
            resetArchive()
            resetAuthor()
            publishSaveState()
            return
        }
        loadJob = viewModelScope.launch {
            _state.value = readerLoadingState(url)
            readerHighlights.clear(loaded = false)
            _inLibrary.value = false
            _rssFeedSuggestion.value = null
            resetArchive()
            resetAuthor()
            publishSaveState()
            try {
                val content = withContext(Dispatchers.IO) { repository.fetch(url, refresh) }
                _state.value = ReaderUiState.Ready(content)
                readerHighlights.tryExternal()
                readerHighlights.startFetch(content)
                startMembershipCheck(content)
                startArchiveCheck(content)
                startAuthorFetch(content)
                startRssFeedDiscovery(content)
                startEventRefFetch(content)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ReaderImageException) {
                readerHighlights.cancel()
                membershipJob?.cancel()
                archiveJob?.cancel()
                authorJob?.cancel()
                rssFeedJob?.cancel()
                eventRefJob?.cancel()
                readerHighlights.clear(loaded = true)
                _state.value = ReaderUiState.ImageOnly(e.imageUrl)
                openGallery(listOf(e.imageUrl), 0)
                publishSaveState()
            } catch (e: ReaderHighlightException) {
                readerHighlights.cancel()
                membershipJob?.cancel()
                archiveJob?.cancel()
                rssFeedJob?.cancel()
                eventRefJob?.cancel()
                readerHighlights.clear(loaded = true)
                _state.value = ReaderUiState.Highlight(e.highlight, url)
                startAuthorFetch(e.highlight.authorPubkey)
                publishSaveState()
            } catch (e: Exception) {
                readerHighlights.cancel()
                membershipJob?.cancel()
                archiveJob?.cancel()
                authorJob?.cancel()
                rssFeedJob?.cancel()
                eventRefJob?.cancel()
                readerHighlights.clear(loaded = true)
                _state.value = readerErrorState(
                    message = e.message ?: "Could not reach this page.",
                    url = url,
                    detail = (e as? ReaderFetchException)?.detail,
                )
                publishSaveState()
            }
        }
    }

    fun offerExternalHighlight(quote: String) {
        readerHighlights.offerExternal(quote)
    }

    fun highlight(quote: String, ownerText: String = "", ownerOffset: Int = 0): Intent? =
        readerHighlights.create(quote, ownerText, ownerOffset)

    fun dismissRssFeedSuggestion() {
        _rssFeedSuggestion.value = null
    }

    fun archive(): Intent? {
        if (archiving || archiveAction.inFlight()) return null
        val content = (_state.value as? ReaderUiState.Ready)?.content ?: return null
        archiving = true
        return if (_archived.value) {
            archiveAction.unarchive(archiveIds)
        } else {
            archiveAction.request(content)
        }
    }

    fun archiveInFlight(): Boolean = archiving || archiveAction.inFlight()

    /** Publishes [reaction]; `null` removes the current one. Tapping the active reaction removes it. */
    fun react(reaction: ArticleReaction?): Intent? {
        if (reactionAction.inFlight()) return null
        val content = (_state.value as? ReaderUiState.Ready)?.content ?: return null
        return if (reaction == null || reaction == _reaction.value) {
            if (reactionIds.isEmpty()) null else reactionAction.remove(reactionIds)
        } else {
            reactionAction.request(content, reaction)
        }
    }

    fun saveToLibrary(privateBookmark: Boolean = true): Intent? {
        if (saving || _inLibrary.value) return null
        val content = (_state.value as? ReaderUiState.Ready)?.content ?: return null
        saving = true
        publishSaveState()
        return librarySave.request(content, privateBookmark)
    }

    fun onSignerResult(resultCode: Int, data: Intent?) {
        if (librarySave.onSignerResult(resultCode, data)) return
        if (archiveAction.onSignerResult(resultCode, data)) return
        if (reactionAction.onSignerResult(resultCode, data)) return
        readerHighlights.onSignerResult(resultCode, data)
    }

    /** Loads the reader's own reaction state for this article: the archive mark and the emoji reaction. */
    private fun startArchiveCheck(content: ReadableContent) {
        archiveJob?.cancel()
        val session = SessionStore.load(getApplication())
        _loggedIn.value = session != null
        if (session == null || Archive.kind(content) == null) {
            resetArchive()
            return
        }
        _canReact.value = ArticleReactions.kind(content) != null
        archiveJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val relays = buildList {
                    addAll(RelayList.FALLBACK)
                    addAll(RelayQuery.fetchRelayList(session.pubkeyHex).read)
                }.distinct()
                val events = RelayQuery.fetchArchives(relays, session.pubkeyHex, content)
                archiveIds = events.map { it.id }
                _archived.value = events.isNotEmpty()
                if (_canReact.value) {
                    val reactions = RelayQuery.fetchArticleReactions(relays, session.pubkeyHex, content)
                    reactionIds = reactions.map { it.id }
                    _reaction.value = ArticleReactions.currentReaction(reactions, content, session.pubkeyHex)
                }
            } catch (_: Exception) {
                archiveIds = emptyList()
                _archived.value = false
                reactionIds = emptyList()
                _reaction.value = null
            }
        }
    }

    private fun startEventRefFetch(content: ReadableContent) {
        eventRefJob?.cancel()
        val refs = NostrEventRefs.collect(content.body)
        if (refs.isEmpty()) {
            _eventRefs.value = emptyMap()
            return
        }
        eventRefJob = viewModelScope.launch(Dispatchers.IO) {
            val relays = refs.flatMap { it.relays }.distinct()
            val events = try {
                RelayQuery.fetchEvents(refs.map { it.eventId }, relays)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyMap()
            }
            val authors = events.values.map { it.pubkey }.distinct()
            val profiles = try {
                RelayQuery.fetchProfiles(
                    (RelayQuery.globalReadRelays() + relays).distinct(),
                    authors,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyMap()
            }
            val resolved = NostrEventRefs.resolvedFrom(events, profiles)
                .takeIfActive(isActive) ?: return@launch
            ensureActive()
            _eventRefs.value = resolved
        }
    }

    private fun startAuthorFetch(content: ReadableContent) {
        startAuthorFetch(content.authorPubkey)
    }

    private fun startAuthorFetch(authorPubkey: String?) {
        authorJob?.cancel()
        val pubkey = authorPubkey?.trim()?.takeIf { it.length == 64 }
        if (pubkey == null) {
            resetAuthor()
            return
        }
        authorJob = viewModelScope.launch(Dispatchers.IO) {
            _author.value = try {
                RelayQuery.fetchProfile(pubkey)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun startRssFeedDiscovery(content: ReadableContent) {
        rssFeedJob?.cancel()
        _rssFeedSuggestion.value = null
        rssFeedJob = viewModelScope.launch(Dispatchers.IO) {
            val feedUrl = RssRepository.discoverRootFeed(content.url) ?: return@launch
            if (feedUrl !in SettingsSync.settings.value.rssFeeds) {
                _rssFeedSuggestion.value = feedUrl
            }
        }
    }

    private fun resetAuthor() {
        authorJob?.cancel()
        _author.value = null
    }

    private fun resetArchive() {
        archiveJob?.cancel()
        archiveIds = emptyList()
        _archived.value = false
        archiving = false
        reactionIds = emptyList()
        _reaction.value = null
        _canReact.value = false
    }

    private fun publishSaveState() {
        _canSave.value = _loggedIn.value &&
            !_inLibrary.value &&
            !saving &&
            _state.value is ReaderUiState.Ready
    }


    private fun startMembershipCheck(content: ReadableContent) {
        membershipJob?.cancel()
        val session = SessionStore.load(getApplication())
        _loggedIn.value = session != null
        if (session == null) {
            _inLibrary.value = false
            publishSaveState()
            return
        }
        membershipJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val relays = buildList {
                    addAll(RelayList.FALLBACK)
                    addAll(RelayQuery.fetchRelayList(session.pubkeyHex).read)
                }.distinct()
                val list = RelayQuery.fetchBookmarkList(session.pubkeyHex, relays)
                val web = RelayQuery.fetchWebBookmarks(session.pubkeyHex, relays)
                _inLibrary.value = LibrarySave.isSaved(content, list, web)
            } catch (_: Exception) {
                _inLibrary.value = false
            }
            publishSaveState()
        }
    }

    companion object {
        const val URL_ARG = "url"
        const val HIGHLIGHT_ARG = "highlight"
    }
}

sealed interface ReaderUiState {
    data class Loading(
        val url: String = "",
        val title: String? = null,
        val imageUrl: String? = null,
    ) : ReaderUiState
    data class Ready(val content: ReadableContent) : ReaderUiState
    data class Highlight(
        val highlight: OpenedHighlight,
        val eventUrl: String,
    ) : ReaderUiState
    data class ImageOnly(val url: String) : ReaderUiState
    data class Error(
        val message: String,
        val url: String,
        val detail: String? = null,
        val title: String? = null,
        val imageUrl: String? = null,
    ) : ReaderUiState
}

internal fun readerLoadingState(url: String): ReaderUiState.Loading {
    val preview = ArticlePreview.get(url)
    return ReaderUiState.Loading(
        url = url,
        title = preview?.title?.let { HtmlToMarkdown.decode(it) }?.trim()?.takeIf { it.isNotEmpty() },
        imageUrl = preview?.imageUrl?.trim()?.takeIf { it.isNotEmpty() },
    )
}

internal fun readerErrorState(
    message: String,
    url: String,
    detail: String? = null,
): ReaderUiState.Error {
    val preview = ArticlePreview.get(url)
    return ReaderUiState.Error(
        message = message,
        url = url,
        detail = detail,
        title = preview?.title?.let { HtmlToMarkdown.decode(it) }?.trim()?.takeIf { it.isNotEmpty() },
        imageUrl = preview?.imageUrl?.trim()?.takeIf { it.isNotEmpty() },
    )
}
