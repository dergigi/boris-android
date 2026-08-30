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
import kotlinx.coroutines.awaitAll
import org.dergigi.boris.data.ArticleUrl
import org.dergigi.boris.data.NostrArticle
import org.dergigi.boris.data.RssItem
import org.dergigi.boris.data.RssRepository
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip23
import org.dergigi.boris.nostr.Nip84
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.nostr.RelayList
import org.dergigi.boris.nostr.RelayQuery

data class FeedItem(
    val id: String,
    val quote: String,
    val context: String? = null,
    val url: String?,
    val host: String?,
    val authorHex: String,
    val authorName: String,
    val authorPicture: String?,
    val createdAt: Long,
    val level: FeedLevel,
)

data class FeedWriting(
    val id: String,
    val title: String,
    val summary: String?,
    val imageUrl: String?,
    val url: String,
    val authorHex: String,
    val authorName: String,
    val authorPicture: String?,
    val publishedAt: Long,
    val level: FeedLevel,
)

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data class Ready(
        val highlights: List<FeedItem>,
        val writings: List<FeedWriting>,
        val hasHighlights: Boolean,
        val hasWritings: Boolean,
    ) : FeedUiState
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

    private val _rss = MutableStateFlow<List<RssItem>>(emptyList())
    val rss: StateFlow<List<RssItem>> = _rss.asStateFlow()

    private val _rssLoading = MutableStateFlow(false)
    val rssLoading: StateFlow<Boolean> = _rssLoading.asStateFlow()

    private var highlights: List<FeedItem> = emptyList()
    private var writings: List<FeedWriting> = emptyList()
    private var failed = false
    private var loadJob: Job? = null
    private var rssJob: Job? = null

    init {
        viewModelScope.launch {
            var lastScope = FeedScope.fromSettings(SettingsSync.settings.value)
            var lastFeeds = SettingsSync.settings.value.rssFeeds
            SettingsSync.settings.collect { settings ->
                if (settings.rssFeeds != lastFeeds) {
                    lastFeeds = settings.rssFeeds
                    refreshRss()
                }
                val next = FeedScope.fromSettings(settings)
                if (next == lastScope) return@collect
                lastScope = next
                if (_loggedIn.value) {
                    _scope.value = next
                    publish()
                }
            }
        }
    }

    fun refresh() {
        refreshRss()
        val keepItems = highlights.isNotEmpty() || writings.isNotEmpty()
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val session = SessionStore.load(getApplication())
            val loggedIn = session != null
            if (loggedIn != _loggedIn.value) {
                _loggedIn.value = loggedIn
                _scope.value = resolveScope(loggedIn)
            }
            var showing = keepItems
            if (!keepItems) {
                val cached = withContext(Dispatchers.IO) { loadCatalogFromCache(session?.pubkeyHex) }
                if (cached != null) {
                    highlights = cached.highlights
                    writings = cached.writings
                    failed = false
                    publish()
                    showing = true
                }
            }
            if (showing) {
                _refreshing.value = true
            } else {
                _state.value = FeedUiState.Loading
            }
            try {
                val catalog = withContext(Dispatchers.IO) {
                    loadCatalog(session?.pubkeyHex)
                }
                highlights = catalog.highlights
                writings = catalog.writings
                failed = false
                publish()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed = highlights.isEmpty() && writings.isEmpty()
                if (failed) {
                    _state.value = FeedUiState.Error
                }
            } finally {
                _refreshing.value = false
            }
        }
    }

    private fun refreshRss() {
        val feeds = SettingsSync.settings.value.rssFeeds
        if (feeds.isEmpty()) {
            _rss.value = emptyList()
            return
        }
        rssJob?.cancel()
        rssJob = viewModelScope.launch {
            if (_rss.value.isEmpty()) _rssLoading.value = true
            try {
                val items = withContext(Dispatchers.IO) {
                    coroutineScope {
                        feeds.map { feed ->
                            async {
                                runCatching { RssRepository.fetch(feed) }
                                    .getOrDefault(emptyList())
                            }
                        }.awaitAll()
                    }
                }.flatten().distinctBy { it.link }.sortedByDescending { it.publishedAt }
                if (items.isNotEmpty()) _rss.value = items
            } finally {
                _rssLoading.value = false
            }
        }
    }

    fun toggle(level: FeedLevel) {
        if (!_loggedIn.value && level != FeedLevel.Nostrverse) return
        val next = _scope.value.toggle(level)
        if (next == _scope.value) return
        _scope.value = next
        publish()
    }

    private fun publish() {
        if (failed && highlights.isEmpty() && writings.isEmpty()) {
            _state.value = FeedUiState.Error
            return
        }
        val visibleHighlights = highlights.filter { _scope.value.visible(it.level) }
        val visibleWritings = writings.filter { _scope.value.visible(it.level) }
        _state.value = if (highlights.isEmpty() && writings.isEmpty()) {
            FeedUiState.Empty
        } else {
            FeedUiState.Ready(
                highlights = visibleHighlights,
                writings = visibleWritings,
                hasHighlights = highlights.isNotEmpty(),
                hasWritings = writings.isNotEmpty(),
            )
        }
    }

    private fun resolveScope(loggedIn: Boolean): FeedScope {
        if (!loggedIn) return FeedScope.LOGGED_OUT
        return FeedScope.fromSettings(SettingsSync.settings.value)
    }

    private fun initialScope(): FeedScope {
        val loggedIn = SessionStore.load(getApplication()) != null
        return resolveScope(loggedIn)
    }

    private fun loadCatalogFromCache(pubkeyHex: String?): Catalog? {
        val friends = if (pubkeyHex == null) emptySet() else RelayQuery.cachedContactPubkeys(pubkeyHex)
        val foaf = if (pubkeyHex == null) {
            emptySet()
        } else {
            RelayQuery.cachedFoafPubkeys(pubkeyHex, friends)
        }
        val foafAuthors = foafFetchAuthors(foaf)
        val highlightEvents = buildList {
            addAll(RelayQuery.cachedRecentHighlights(HIGHLIGHT_LIMIT))
            if (pubkeyHex != null) {
                addAll(RelayQuery.cachedRecentHighlights(HIGHLIGHT_LIMIT, pubkeyHex))
                if (friends.isNotEmpty()) {
                    addAll(RelayQuery.cachedRecentHighlights(HIGHLIGHT_LIMIT, authors = friends))
                }
                if (foafAuthors.isNotEmpty()) {
                    addAll(RelayQuery.cachedRecentHighlights(HIGHLIGHT_LIMIT, authors = foafAuthors))
                }
            }
        }.distinctBy { it.id }.sortedByDescending { it.createdAt }
        val writingEvents = buildList {
            addAll(RelayQuery.cachedRecentWritings(WRITING_LIMIT))
            if (pubkeyHex != null) {
                addAll(RelayQuery.cachedRecentWritings(WRITING_LIMIT, pubkeyHex))
                if (friends.isNotEmpty()) {
                    addAll(RelayQuery.cachedRecentWritings(WRITING_LIMIT, authors = friends))
                }
                if (foafAuthors.isNotEmpty()) {
                    addAll(RelayQuery.cachedRecentWritings(WRITING_LIMIT, authors = foafAuthors))
                }
            }
        }.distinctBy { it.id }.sortedByDescending { Nip23.publishedAt(it) }
        if (highlightEvents.isEmpty() && writingEvents.isEmpty()) return null
        val authors = (highlightEvents + writingEvents).map { it.pubkey }.distinct().take(PROFILE_LIMIT)
        val profiles = RelayQuery.cachedProfiles(authors)
        return Catalog(
            highlights = toHighlightItems(highlightEvents, profiles, pubkeyHex, friends, foaf),
            writings = toWritingItems(writingEvents, profiles, pubkeyHex, friends, foaf),
        )
    }

    private suspend fun loadCatalog(pubkeyHex: String?): Catalog = coroutineScope {
        val relays = buildList {
            addAll(RelayList.FALLBACK)
            if (pubkeyHex != null) addAll(RelayQuery.fetchRelayList(pubkeyHex).read)
        }.distinct()
        val friendsDeferred = async {
            if (pubkeyHex == null) emptySet() else RelayQuery.fetchContactPubkeys(pubkeyHex)
        }
        val globalHighlights = async {
            RelayQuery.fetchRecentHighlights(RelayQuery.globalReadRelays(), HIGHLIGHT_LIMIT)
        }
        val mineHighlights = async {
            if (pubkeyHex == null) {
                emptyList()
            } else {
                RelayQuery.fetchRecentHighlights(relays, HIGHLIGHT_LIMIT, pubkeyHex)
            }
        }
        val globalWritings = async {
            RelayQuery.fetchRecentWritings(RelayQuery.globalReadRelays(), WRITING_LIMIT)
        }
        val mineWritings = async {
            if (pubkeyHex == null) {
                emptyList()
            } else {
                RelayQuery.fetchRecentWritings(relays, WRITING_LIMIT, pubkeyHex)
            }
        }
        val friends = friendsDeferred.await()
        val foaf = if (pubkeyHex == null) {
            emptySet()
        } else {
            RelayQuery.fetchFoafPubkeys(pubkeyHex, friends)
        }
        val foafAuthors = foafFetchAuthors(foaf)
        val (friendsHighlights, friendsWritings) = coroutineScope {
            val highlights = async {
                if (pubkeyHex == null || friends.isEmpty()) {
                    emptyList()
                } else {
                    RelayQuery.fetchRecentHighlightsByAuthors(friends, relays, HIGHLIGHT_LIMIT)
                }
            }
            val writings = async {
                if (pubkeyHex == null || friends.isEmpty()) {
                    emptyList()
                } else {
                    RelayQuery.fetchRecentWritingsByAuthors(friends, relays, WRITING_LIMIT)
                }
            }
            highlights.await() to writings.await()
        }
        val (foafHighlights, foafWritings) = coroutineScope {
            val highlights = async {
                if (pubkeyHex == null || foafAuthors.isEmpty()) {
                    emptyList()
                } else {
                    RelayQuery.fetchRecentHighlightsByAuthors(foafAuthors, relays, HIGHLIGHT_LIMIT)
                }
            }
            val writings = async {
                if (pubkeyHex == null || foafAuthors.isEmpty()) {
                    emptyList()
                } else {
                    RelayQuery.fetchRecentWritingsByAuthors(foafAuthors, relays, WRITING_LIMIT)
                }
            }
            highlights.await() to writings.await()
        }
        val highlightEvents = (
            globalHighlights.await() + mineHighlights.await() + friendsHighlights + foafHighlights
            )
            .distinctBy { it.id }
            .sortedByDescending { it.createdAt }
        val writingEvents = (
            globalWritings.await() + mineWritings.await() + friendsWritings + foafWritings
            )
            .distinctBy { it.id }
            .sortedByDescending { Nip23.publishedAt(it) }
        val authors = (highlightEvents + writingEvents)
            .map { it.pubkey }
            .distinct()
            .take(PROFILE_LIMIT)
        val profiles = RelayQuery.fetchProfiles(relays, authors)
        Catalog(
            highlights = toHighlightItems(highlightEvents, profiles, pubkeyHex, friends, foaf),
            writings = toWritingItems(writingEvents, profiles, pubkeyHex, friends, foaf),
        )
    }

    private fun toHighlightItems(
        events: List<Nip01Event>,
        profiles: Map<String, Profile>,
        sessionHex: String?,
        friends: Set<String>,
        foaf: Set<String>,
    ): List<FeedItem> {
        return events.map { event ->
            val url = Nip84.articleUrl(event)
            val profile = profiles[event.pubkey.lowercase()]
            FeedItem(
                id = event.id,
                quote = event.content.trim(),
                context = event.tagValue("context"),
                url = url,
                host = url?.let { ArticleUrl.host(it) },
                authorHex = event.pubkey,
                authorName = authorName(event.pubkey, profile),
                authorPicture = profile?.picture,
                createdAt = event.createdAt,
                level = classifyFeedLevel(event.pubkey, sessionHex, friends, foaf),
            )
        }
    }

    private fun toWritingItems(
        events: List<Nip01Event>,
        profiles: Map<String, Profile>,
        sessionHex: String?,
        friends: Set<String>,
        foaf: Set<String>,
    ): List<FeedWriting> {
        val now = System.currentTimeMillis() / 1000
        return events.mapNotNull { event ->
            writingFrom(event, profiles[event.pubkey.lowercase()], sessionHex, friends, foaf, now)
        }
    }

    private data class Catalog(
        val highlights: List<FeedItem>,
        val writings: List<FeedWriting>,
    )

    companion object {
        private const val HIGHLIGHT_LIMIT = 80
        private const val WRITING_LIMIT = 80
        private const val PROFILE_LIMIT = 80
        private const val UNTITLED = "Untitled"
        private const val FUTURE_SLACK_SECONDS = 24L * 60L * 60L

        internal fun authorName(pubkeyHex: String, profile: Profile?): String =
            Profile.displayName(pubkeyHex, profile)

        internal fun writingFrom(
            event: Nip01Event,
            profile: Profile?,
            sessionHex: String?,
            friends: Set<String>,
            foaf: Set<String> = emptySet(),
            nowSeconds: Long,
        ): FeedWriting? {
            if (Nip23.publishedAt(event) > nowSeconds + FUTURE_SLACK_SECONDS) return null
            val identifier = Nip23.identifier(event) ?: return null
            val article = NostrArticle.fromCoordinate(
                "${Nip01Event.KIND_LONG_FORM}:${event.pubkey}:$identifier",
            ) ?: return null
            return FeedWriting(
                id = event.id,
                title = Nip23.title(event) ?: UNTITLED,
                summary = Nip23.summary(event),
                imageUrl = Nip23.image(event),
                url = article.uri,
                authorHex = event.pubkey,
                authorName = authorName(event.pubkey, profile),
                authorPicture = profile?.picture,
                publishedAt = Nip23.publishedAt(event),
                level = classifyFeedLevel(event.pubkey, sessionHex, friends, foaf),
            )
        }
    }
}
