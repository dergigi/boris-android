package org.dergigi.boris.ui.reader

import android.app.Application
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dergigi.boris.R
import org.dergigi.boris.data.ReadableContent
import org.dergigi.boris.data.Session
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.nostr.EventPublisher
import org.dergigi.boris.nostr.EventSigner
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip84
import org.dergigi.boris.nostr.OutboxRouter
import org.dergigi.boris.nostr.PendingUnsignedEvent
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.nostr.RelayQuery
import org.dergigi.boris.nostr.SocialGraphs
import org.dergigi.boris.nostr.SignOutcome
import org.dergigi.boris.nostr.ZapSplits

class ReaderHighlights(
    private val app: Application,
    private val scope: CoroutineScope,
    private val onMessage: (String) -> Unit,
    private val onSignIntent: (Intent) -> Unit,
    private val currentContent: () -> ReadableContent?,
    private val onLoggedIn: (Boolean) -> Unit,
    private val onPublishSaveState: () -> Unit,
) {
    private val _highlights = MutableStateFlow<List<PaintedHighlight>>(emptyList())
    val highlights: StateFlow<List<PaintedHighlight>> = _highlights.asStateFlow()

    private val _highlightCount = MutableStateFlow(0)
    val highlightCount: StateFlow<Int> = _highlightCount.asStateFlow()

    private val _highlightsLoaded = MutableStateFlow(false)
    val highlightsLoaded: StateFlow<Boolean> = _highlightsLoaded.asStateFlow()

    private var highlightJob: Job? = null
    private var pendingExternalQuote: String? = null
    // Callers launch the Intent returned by create(); only tryExternal emits via onSignIntent.
    // Wiring the signer to onSignIntent as well launched Amber twice per highlight (#135).
    private val signer = EventSigner(
        app = app,
        scope = scope,
        onSignIntent = {},
    )

    fun clear(loaded: Boolean) {
        highlightJob?.cancel()
        _highlights.value = emptyList()
        _highlightCount.value = 0
        _highlightsLoaded.value = loaded
    }

    fun cancel() {
        highlightJob?.cancel()
    }

    fun offerExternal(quote: String) {
        pendingExternalQuote = quote.trim().takeIf { it.isNotBlank() }
        tryExternal()
    }

    fun tryExternal() {
        val quote = pendingExternalQuote ?: return
        if (currentContent() == null) return
        pendingExternalQuote = null
        create(quote)?.let(onSignIntent)
    }

    fun create(
        quote: String,
        ownerText: String = "",
        ownerOffset: Int = 0,
        comment: String? = null,
    ): Intent? {
        val trimmed = quote.trim()
        if (trimmed.isBlank()) {
            onMessage(app.getString(R.string.highlight_cancelled))
            return null
        }
        val session = SessionStore.load(app) ?: run {
            onMessage(app.getString(R.string.highlight_sign_in))
            return null
        }
        val content = currentContent() ?: return null
        val selectedStart = Nip84.locateSelection(
            articleContent = content.body,
            selectedText = trimmed,
            ownerText = ownerText,
            ownerOffset = ownerOffset,
        )
        val context = Nip84.extractContext(trimmed, content.body, selectedStart)
        val zapSplits = zapSplitTags(content, session.pubkeyHex)
        val createdAt = System.currentTimeMillis() / 1000
        return signer.sign(
            session,
            PendingUnsignedEvent(
                pubkey = session.pubkeyHex,
                createdAt = createdAt,
                kind = Nip01Event.KIND_HIGHLIGHT,
                tags = Nip84.tags(
                    content.url,
                    context,
                    content.articleCoordinate,
                    content.eventId,
                    content.authorPubkey,
                    zapSplits,
                    comment,
                ),
                content = trimmed,
            ),
        ) { outcome ->
            when (outcome) {
                is SignOutcome.Signed -> onSigned(session, outcome.event)
                SignOutcome.Rejected -> onMessage(app.getString(R.string.highlight_rejected))
                SignOutcome.Cancelled, SignOutcome.Failed -> {
                    onMessage(app.getString(R.string.highlight_cancelled))
                }
            }
        }
    }

    fun onSignerResult(resultCode: Int, data: Intent?): Boolean =
        signer.onSignerResult(resultCode, data)

    fun startFetch(content: ReadableContent) {
        highlightJob?.cancel()
        val session = SessionStore.load(app)
        onLoggedIn(session != null)
        _highlightsLoaded.value = false
        onPublishSaveState()
        highlightJob = scope.launch(Dispatchers.IO) {
            try {
                val cachedGraph = SocialGraphs.cached(session?.pubkeyHex)
                val cached = cachedHighlightsFor(content)
                if (cached.isNotEmpty()) {
                    paint(cached, session?.pubkeyHex, cachedGraph.friends, cachedGraph.foaf)
                }
                val live = SocialGraphs.fetch(session?.pubkeyHex)
                val relays = OutboxRouter.authorTargets(
                    pubkeyHex = content.authorPubkey?.trim().orEmpty(),
                    base = live.relays,
                ).distinct()
                val contacts = live.friends
                val foafKeys = live.foaf
                val events = when {
                    !content.articleCoordinate.isNullOrBlank() || !content.eventId.isNullOrBlank() -> {
                        RelayQuery.fetchHighlightsForArticle(
                            relays,
                            content.articleCoordinate,
                            content.eventId,
                        )
                    }
                    else -> {
                        if (session != null) primeOwnHighlights(relays, session.pubkeyHex)
                        RelayQuery.fetchHighlights(relays, content.url)
                    }
                }
                paint(events, session?.pubkeyHex, contacts, foafKeys)
                val authors = events.map { it.pubkey }.distinct()
                if (authors.isNotEmpty()) {
                    RelayQuery.fetchProfiles(relays, authors)
                    paint(events, session?.pubkeyHex, contacts, foafKeys)
                }
            } catch (_: Exception) {
            } finally {
                _highlightsLoaded.value = true
            }
        }
    }

    private fun onSigned(session: Session, event: Nip01Event) {
        if (event.kind != Nip01Event.KIND_HIGHLIGHT) return
        val settings = SettingsSync.settings.value
        val visibleSettings = settings.withOwnHighlightsVisible()
        if (visibleSettings !== settings) SettingsSync.apply(visibleSettings)
        val painted = PaintedHighlight(
            id = event.id,
            quote = event.content,
            mine = true,
            pubkey = event.pubkey,
            createdAt = event.createdAt,
            context = event.tagValue("context"),
            comment = Nip84.comment(event),
        )
        if (_highlights.value.none { it.id == event.id }) {
            _highlights.value = _highlights.value + painted
            _highlightCount.value = _highlightCount.value + 1
        }
        scope.launch(Dispatchers.IO) {
            EventPublisher.publish(session.pubkeyHex, event)
        }
    }

    private fun cachedHighlightsFor(content: ReadableContent): List<Nip01Event> =
        when {
            !content.articleCoordinate.isNullOrBlank() || !content.eventId.isNullOrBlank() -> {
                RelayQuery.cachedHighlightsForArticle(content.articleCoordinate, content.eventId)
            }
            else -> RelayQuery.cachedHighlights(content.url)
        }

    private fun paint(
        events: List<Nip01Event>,
        pubkeyHex: String?,
        friends: Set<String>,
        foaf: Set<String> = emptySet(),
    ) {
        val profiles = RelayQuery.cachedProfiles(events.map { it.pubkey })
        _highlightCount.value = events.size
        _highlights.value = events.map { event ->
            val mine = pubkeyHex != null && event.pubkey.equals(pubkeyHex, ignoreCase = true)
            val key = event.pubkey.lowercase()
            val friend = !mine && key in friends
            PaintedHighlight(
                id = event.id,
                quote = event.content,
                mine = mine,
                friend = friend,
                foaf = !mine && !friend && key in foaf,
                pubkey = event.pubkey,
                createdAt = event.createdAt,
                context = event.tagValue("context"),
                comment = Nip84.comment(event),
                authorName = Profile.displayName(event.pubkey, profiles[key]),
                authorPicture = profiles[key]?.picture,
            )
        }
    }

    private fun primeOwnHighlights(relays: List<String>, pubkeyHex: String) {
        val now = System.currentTimeMillis()
        if (now - ownHighlightsPrimedAt < OWN_HIGHLIGHTS_PRIME_MS) return
        ownHighlightsPrimedAt = now
        runCatching { RelayQuery.fetchRecentHighlights(relays, limit = 300, pubkeyHex = pubkeyHex) }
    }

    private fun zapSplitTags(content: ReadableContent, highlighterPubkey: String): List<List<String>> {
        val settings = SettingsSync.settings.value
        if (!settings.zapSplitsEnabled) return emptyList()
        return ZapSplits.tags(
            highlighterPubkey = highlighterPubkey,
            sourceAuthorPubkey = content.authorPubkey,
            sourceZapTags = content.sourceZapTags,
            highlighterWeight = settings.zapSplitHighlighterWeight,
            borisWeight = settings.zapSplitBorisWeight,
            authorWeight = settings.zapSplitAuthorWeight,
        )
    }

    companion object {
        private const val OWN_HIGHLIGHTS_PRIME_MS = 15 * 60 * 1000L

        @Volatile
        private var ownHighlightsPrimedAt = 0L
    }
}
