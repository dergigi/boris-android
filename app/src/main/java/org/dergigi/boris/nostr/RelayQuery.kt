package org.dergigi.boris.nostr

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.dergigi.boris.data.ArticleUrl
import org.dergigi.boris.data.ReadableContent
import org.dergigi.boris.data.SettingsSync

object RelayQuery {
    fun fetchRelayList(pubkeyHex: String): RelayList {
        val cached = EventCache.latest(Nip01Event.KIND_RELAY_LIST, pubkeyHex)
        val parsed = if (cached != null) {
            refreshOnce("relaylist:${pubkeyHex.lowercase()}") { fetchRelayListRemote(pubkeyHex) }
            RelayList.parse(listOf(cached))
        } else {
            fetchRelayListRemote(pubkeyHex)
        }
        return LocalRelays.withLocal(
            parsed,
            SettingsSync.settings.value.useLocalRelayAsCache,
            LocalRelays.citrineReachable(),
        )
    }

    private fun fetchRelayListRemote(pubkeyHex: String): RelayList {
        val filter = JSONObject()
            .put("kinds", JSONArray().put(Nip01Event.KIND_RELAY_LIST))
            .put("authors", JSONArray().put(pubkeyHex))
            .put("limit", 10)
        val events = query(RelayList.FALLBACK, listOf(filter))
            .filter { event ->
                event.kind == Nip01Event.KIND_RELAY_LIST &&
                    event.pubkey.equals(pubkeyHex, ignoreCase = true)
            }
        EventCache.putAll(events)
        return RelayList.parse(events)
    }

    fun fetchAppData(pubkeyHex: String): Nip01Event? {
        val relays = buildList {
            addAll(RelayList.FALLBACK)
            addAll(fetchRelayList(pubkeyHex).read)
        }.distinct()
        val filter = JSONObject()
            .put("kinds", JSONArray().put(Nip01Event.KIND_APP_DATA))
            .put("authors", JSONArray().put(pubkeyHex))
            .put("#d", JSONArray().put(Nip78.SETTINGS_D))
            .put("limit", 5)
        val remote = query(relays, listOf(filter))
            .filter { event ->
                event.kind == Nip01Event.KIND_APP_DATA &&
                    event.pubkey.equals(pubkeyHex, ignoreCase = true) &&
                    Nip78.hasSettingsD(event)
            }
            .maxByOrNull { it.createdAt }
        remote?.let { EventCache.put(it) }
        val cached = EventCache.latest(Nip01Event.KIND_APP_DATA, pubkeyHex, Nip78.SETTINGS_D)
        return listOfNotNull(remote, cached).maxByOrNull { it.createdAt }
    }

    fun fetchProfile(pubkeyHex: String): Profile? {
        val cached = EventCache.latest(Nip01Event.KIND_METADATA, pubkeyHex)
        if (cached != null) {
            refreshOnce("profile:${pubkeyHex.lowercase()}") { fetchProfileRemote(pubkeyHex) }
            return Profile.parse(cached.content)
        }
        return fetchProfileRemote(pubkeyHex)?.let { Profile.parse(it.content) }
    }

    private fun fetchProfileRemote(pubkeyHex: String): Nip01Event? {
        val relays = fetchRelayList(pubkeyHex).read
        val filter = JSONObject()
            .put("kinds", JSONArray().put(Nip01Event.KIND_METADATA))
            .put("authors", JSONArray().put(pubkeyHex))
            .put("limit", 5)
        val newest = query(relays, listOf(filter))
            .filter { event ->
                event.kind == Nip01Event.KIND_METADATA &&
                    event.pubkey.equals(pubkeyHex, ignoreCase = true)
            }
            .maxByOrNull { it.createdAt } ?: return null
        EventCache.put(newest)
        return newest
    }

    fun fetchProfilePicture(pubkeyHex: String): String? = fetchProfile(pubkeyHex)?.picture

    fun discoverContentRelays(
        seed: List<String> = RelayList.FALLBACK,
        limit: Int = 12,
    ): List<String> {
        val since = System.currentTimeMillis() / 1000 - DISCOVERY_WINDOW_SECONDS
        val filter = JSONObject()
            .put("kinds", JSONArray().put(Nip01Event.KIND_RELAY_DISCOVERY))
            .put("since", since)
            .put("limit", 200)
        val bootstrap = (Nip66.MONITOR_RELAYS + seed).mapNotNull { Nip66.normalize(it) }.distinct()
        val events = query(bootstrap, listOf(filter))
        return Nip66.select(events, seed, limit)
    }

    /**
     * Relays for global (nostrverse) queries: the fallback set plus NIP-66
     * discovered relays. Discovery runs once per session in the background and
     * is never a gate; until it completes this is just the fallback set.
     */
    fun globalReadRelays(): List<String> {
        refreshOnce("nip66:discovery") {
            discovered = discoverContentRelays().filterNot { it in RelayList.FALLBACK }
        }
        return (RelayList.FALLBACK + discovered).distinct()
    }

    /** NIP-66 discovered relays from this session, beyond the fallback set. */
    fun discoveredRelays(): List<String> = discovered

    @Volatile
    private var discovered: List<String> = emptyList()

    fun fetchContactPubkeys(pubkeyHex: String): Set<String> {
        val cached = EventCache.latest(Nip01Event.KIND_CONTACTS, pubkeyHex)
        if (cached != null) {
            refreshOnce("contacts:${pubkeyHex.lowercase()}") { fetchContactPubkeysRemote(pubkeyHex) }
            return cached.pPubkeys()
        }
        return fetchContactPubkeysRemote(pubkeyHex)
    }

    fun cachedContactPubkeys(pubkeyHex: String): Set<String> =
        EventCache.latest(Nip01Event.KIND_CONTACTS, pubkeyHex)?.pPubkeys() ?: emptySet()

    private fun fetchContactPubkeysRemote(pubkeyHex: String): Set<String> {
        val relays = buildList {
            addAll(RelayList.FALLBACK)
            addAll(fetchRelayList(pubkeyHex).read)
        }.distinct()
        val filter = JSONObject()
            .put("kinds", JSONArray().put(Nip01Event.KIND_CONTACTS))
            .put("authors", JSONArray().put(pubkeyHex))
            .put("limit", 5)
        val newest = query(relays, listOf(filter))
            .filter { event ->
                event.kind == Nip01Event.KIND_CONTACTS &&
                    event.pubkey.equals(pubkeyHex, ignoreCase = true)
            }
            .maxByOrNull { it.createdAt } ?: return cachedContactPubkeys(pubkeyHex)
        EventCache.put(newest)
        return newest.pPubkeys()
    }

    fun fetchRecentHighlights(
        readRelays: List<String>,
        limit: Int = 80,
        pubkeyHex: String? = null,
        authors: Collection<String> = emptyList(),
    ): List<Nip01Event> {
        val urls = relayUrls(readRelays)
        val keys = authorKeys(pubkeyHex, authors)
        if (urls.isNotEmpty()) {
            val filters = if (keys.isEmpty()) {
                listOf(highlightFilter(limit, emptyList()))
            } else {
                keys.chunked(AUTHOR_CHUNK).map { chunk -> highlightFilter(limit, chunk) }
            }
            val allowed = keys.toSet()
            val remote = query(urls, filters)
                .filter { event ->
                    event.kind == Nip01Event.KIND_HIGHLIGHT &&
                        event.content.isNotBlank() &&
                        (allowed.isEmpty() || event.pubkey.lowercase() in allowed)
                }
            EventCache.putAll(remote)
        }
        return cachedRecentHighlights(limit, pubkeyHex, authors)
    }

    fun cachedRecentHighlights(
        limit: Int = 80,
        pubkeyHex: String? = null,
        authors: Collection<String> = emptyList(),
    ): List<Nip01Event> {
        val allowed = authorKeys(pubkeyHex, authors).toSet()
        return EventCache.byKind(Nip01Event.KIND_HIGHLIGHT)
            .filter { event ->
                event.content.isNotBlank() &&
                    (allowed.isEmpty() || event.pubkey.lowercase() in allowed)
            }
            .sortedByDescending { it.createdAt }
            .take(limit)
    }

    fun fetchProfiles(
        readRelays: List<String>,
        pubkeys: List<String>,
    ): Map<String, Profile> {
        val urls = relayUrls(readRelays)
        val keys = pubkeys.map { it.lowercase() }.distinct()
        if (keys.isEmpty()) return emptyMap()
        val cached = keys.mapNotNull { key ->
            EventCache.latest(Nip01Event.KIND_METADATA, key)?.let { key to it }
        }.toMap()
        val missing = keys.filterNot { it in cached }
        val fetched = if (urls.isEmpty() || missing.isEmpty()) {
            emptyMap()
        } else {
            fetchProfileEvents(urls, missing)
        }
        if (urls.isNotEmpty()) {
            val stale = cached.keys.filter { refreshed.add("profile:$it") }
            if (stale.isNotEmpty()) {
                refreshPool.execute { runCatching { fetchProfileEvents(urls, stale) } }
            }
        }
        return (cached + fetched).mapValues { Profile.parse(it.value.content) }
    }

    private fun fetchProfileEvents(
        urls: List<String>,
        keys: List<String>,
    ): Map<String, Nip01Event> {
        val newest = mutableMapOf<String, Nip01Event>()
        for (chunk in keys.chunked(PROFILE_CHUNK)) {
            val filter = JSONObject()
                .put("kinds", JSONArray().put(Nip01Event.KIND_METADATA))
                .put("authors", JSONArray().apply { chunk.forEach { put(it) } })
                .put("limit", chunk.size)
            for (event in query(urls, listOf(filter))) {
                if (event.kind != Nip01Event.KIND_METADATA) continue
                val key = event.pubkey.lowercase()
                val existing = newest[key]
                if (existing == null || event.createdAt > existing.createdAt) {
                    newest[key] = event
                }
            }
        }
        EventCache.putAll(newest.values)
        return newest
    }

    fun fetchBookmarkList(pubkeyHex: String, readRelays: List<String>): Nip01Event? {
        val urls = relayUrls(readRelays)
        if (urls.isNotEmpty()) {
            val filter = JSONObject()
                .put("kinds", JSONArray().put(Nip01Event.KIND_BOOKMARKS))
                .put("authors", JSONArray().put(pubkeyHex))
                .put("limit", 5)
            val remote = query(urls, listOf(filter))
                .filter { event ->
                    event.kind == Nip01Event.KIND_BOOKMARKS &&
                        event.pubkey.equals(pubkeyHex, ignoreCase = true)
                }
                .maxByOrNull { it.createdAt }
            remote?.let { EventCache.put(it) }
        }
        return EventCache.latest(Nip01Event.KIND_BOOKMARKS, pubkeyHex)
    }

    fun fetchWebBookmarks(pubkeyHex: String, readRelays: List<String>): List<Nip01Event> {
        val urls = relayUrls(readRelays)
        if (urls.isNotEmpty()) {
            val filter = JSONObject()
                .put("kinds", JSONArray().put(Nip01Event.KIND_WEB_BOOKMARK))
                .put("authors", JSONArray().put(pubkeyHex))
                .put("limit", 200)
            val remote = query(urls, listOf(filter))
                .filter { event ->
                    event.kind == Nip01Event.KIND_WEB_BOOKMARK &&
                        event.pubkey.equals(pubkeyHex, ignoreCase = true) &&
                        !event.tagValue("d").isNullOrBlank()
                }
            EventCache.putAll(remote)
        }
        return cachedWebBookmarks(pubkeyHex)
    }

    fun cachedWebBookmarks(pubkeyHex: String): List<Nip01Event> =
        EventCache.byKindAndAuthor(setOf(Nip01Event.KIND_WEB_BOOKMARK), pubkeyHex)
            .filter { !it.tagValue("d").isNullOrBlank() }
            .groupBy { it.tagValue("d")!!.lowercase() }
            .mapNotNull { (_, events) -> events.maxByOrNull { it.createdAt } }
            .sortedByDescending { NipB0.publishedAt(it) }

    fun fetchLookmarks(pubkeyHex: String, readRelays: List<String>): List<Nip01Event> {
        val urls = relayUrls(readRelays)
        if (urls.isNotEmpty()) {
            val filter = JSONObject()
                .put("kinds", JSONArray().put(Nip01Event.KIND_REACTION))
                .put("authors", JSONArray().put(pubkeyHex))
                .put("limit", 200)
            val remote = query(urls, listOf(filter))
                .filter { event ->
                    Lookmarks.isLook(event) && event.pubkey.equals(pubkeyHex, ignoreCase = true)
                }
            EventCache.putAll(remote)
        }
        return cachedLookmarks(pubkeyHex)
    }

    fun cachedLookmarks(pubkeyHex: String): List<Nip01Event> =
        EventCache.byKindAndAuthor(setOf(Nip01Event.KIND_REACTION), pubkeyHex)
            .filter { Lookmarks.isLook(it) }
            .sortedByDescending { it.createdAt }

    fun fetchArchiveReactions(pubkeyHex: String, readRelays: List<String>): List<Nip01Event> {
        val urls = relayUrls(readRelays)
        if (urls.isNotEmpty()) {
            val filters = listOf(
                JSONObject()
                    .put("kinds", JSONArray().put(Nip01Event.KIND_REACTION))
                    .put("authors", JSONArray().put(pubkeyHex))
                    .put("limit", 200),
                JSONObject()
                    .put("kinds", JSONArray().put(Nip01Event.KIND_URL_REACTION))
                    .put("authors", JSONArray().put(pubkeyHex))
                    .put("limit", 200),
            )
            val remote = query(urls, filters)
                .filter { event ->
                    Archive.isArchive(event) && event.pubkey.equals(pubkeyHex, ignoreCase = true)
                }
            EventCache.putAll(remote)
        }
        return cachedArchiveReactions(pubkeyHex)
    }

    fun cachedArchiveReactions(pubkeyHex: String): List<Nip01Event> =
        EventCache.byKindAndAuthor(
            setOf(Nip01Event.KIND_REACTION, Nip01Event.KIND_URL_REACTION),
            pubkeyHex,
        )
            .filter { Archive.isArchive(it) }
            .sortedByDescending { it.createdAt }

    fun fetchLongFormArticles(pubkeyHex: String, readRelays: List<String>): List<Nip01Event> =
        fetchRecentWritings(readRelays, limit = 200, pubkeyHex = pubkeyHex)

    fun fetchRecentWritings(
        readRelays: List<String>,
        limit: Int = 80,
        pubkeyHex: String? = null,
        authors: Collection<String> = emptyList(),
    ): List<Nip01Event> {
        val urls = relayUrls(readRelays)
        val keys = authorKeys(pubkeyHex, authors)
        if (urls.isNotEmpty()) {
            val filters = if (keys.isEmpty()) {
                listOf(writingFilter(limit, emptyList()))
            } else {
                keys.chunked(AUTHOR_CHUNK).map { chunk -> writingFilter(limit, chunk) }
            }
            val allowed = keys.toSet()
            val remote = query(urls, filters)
                .filter { event ->
                    event.kind == Nip01Event.KIND_LONG_FORM &&
                        !Nip23.identifier(event).isNullOrBlank() &&
                        (allowed.isEmpty() || event.pubkey.lowercase() in allowed)
                }
            EventCache.putAll(remote)
        }
        return cachedRecentWritings(limit, pubkeyHex, authors)
    }

    fun cachedRecentWritings(
        limit: Int = 80,
        pubkeyHex: String? = null,
        authors: Collection<String> = emptyList(),
    ): List<Nip01Event> {
        val allowed = authorKeys(pubkeyHex, authors).toSet()
        return EventCache.byKind(Nip01Event.KIND_LONG_FORM)
            .filter { event ->
                !Nip23.identifier(event).isNullOrBlank() &&
                    (allowed.isEmpty() || event.pubkey.lowercase() in allowed)
            }
            .groupBy { "${it.pubkey.lowercase()}:${Nip23.identifier(it)!!.lowercase()}" }
            .mapNotNull { (_, events) -> events.maxByOrNull { it.createdAt } }
            .sortedByDescending { Nip23.publishedAt(it) }
            .take(limit)
    }

    /**
     * Outbox-routed variant of [fetchRecentHighlights]: fetches each author's
     * highlights from their write relays, with [fallbackRelays] as safety net.
     */
    fun fetchRecentHighlightsByAuthors(
        authors: Collection<String>,
        fallbackRelays: List<String>,
        limit: Int = 80,
    ): List<Nip01Event> {
        fetchRouted(
            authors = authors,
            fallbackRelays = fallbackRelays,
            filterFor = { chunk -> highlightFilter(limit, chunk) },
            accept = { it.kind == Nip01Event.KIND_HIGHLIGHT && it.content.isNotBlank() },
        )
        return cachedRecentHighlights(limit, authors = authors)
    }

    /** Outbox-routed variant of [fetchRecentWritings]. */
    fun fetchRecentWritingsByAuthors(
        authors: Collection<String>,
        fallbackRelays: List<String>,
        limit: Int = 80,
    ): List<Nip01Event> {
        fetchRouted(
            authors = authors,
            fallbackRelays = fallbackRelays,
            filterFor = { chunk -> writingFilter(limit, chunk) },
            accept = { it.kind == Nip01Event.KIND_LONG_FORM && !Nip23.identifier(it).isNullOrBlank() },
        )
        return cachedRecentWritings(limit, authors = authors)
    }

    private fun fetchRouted(
        authors: Collection<String>,
        fallbackRelays: List<String>,
        filterFor: (List<String>) -> JSONObject,
        accept: (Nip01Event) -> Boolean,
    ) {
        val keys = authorKeys(null, authors)
        if (keys.isEmpty()) return
        RelayListRepository.prefetch(keys)
        val routes = OutboxRouter.route(
            authors = keys,
            fallbackRelays = reachableRelays(relayUrls(fallbackRelays)),
            skip = { RelayHealth.inCooldown(it) },
        )
        val targets = routes.mapValues { (_, subset) ->
            subset.toList().chunked(AUTHOR_CHUNK).map(filterFor)
        }
        // The routed relay set is the hot path; keep those sockets open.
        RelayPool.markPersistent(routes.keys)
        val allowed = keys.toSet()
        val remote = queryPerRelay(targets)
            .filter { event -> accept(event) && event.pubkey.lowercase() in allowed }
        EventCache.putAll(remote)
    }

    fun fetchArticle(pointer: NaddrPointer): Nip01Event? {
        val cached = EventCache.latest(pointer.kind, pointer.pubkey, pointer.identifier)
        if (cached != null) {
            refreshOnce("article:${pointer.kind}:${pointer.pubkey.lowercase()}:${pointer.identifier}") {
                fetchArticleRemote(pointer)
            }
            return cached
        }
        return fetchArticleRemote(pointer)
    }

    private fun fetchArticleRemote(pointer: NaddrPointer): Nip01Event? {
        val relays = relayUrls(
            OutboxRouter.authorTargets(
                pubkeyHex = pointer.pubkey,
                base = buildList {
                    addAll(pointer.relays)
                    addAll(RelayList.FALLBACK)
                },
            ),
        )
        val filter = JSONObject()
            .put("kinds", JSONArray().put(pointer.kind))
            .put("authors", JSONArray().put(pointer.pubkey))
            .put("#d", JSONArray().put(pointer.identifier))
            .put("limit", 5)
        val newest = query(relays, listOf(filter))
            .filter { event ->
                event.kind == pointer.kind &&
                    event.pubkey.equals(pointer.pubkey, ignoreCase = true) &&
                    event.tagValue("d")?.trim() == pointer.identifier.trim()
            }
            .maxByOrNull { it.createdAt } ?: return null
        EventCache.put(newest)
        return newest
    }

    fun fetchEvent(eventId: String, relays: List<String> = emptyList()): Nip01Event? =
        fetchEvents(listOf(eventId), relays)[eventId.lowercase()]

    fun fetchEvents(eventIds: Collection<String>, relays: List<String> = emptyList()): Map<String, Nip01Event> {
        val ids = eventIds.map { it.lowercase() }.filter { it.length == 64 }.distinct()
        if (ids.isEmpty()) return emptyMap()
        val found = mutableMapOf<String, Nip01Event>()
        val missing = mutableListOf<String>()
        for (id in ids) {
            val cached = EventCache.event(id)
            if (cached != null) found[id] = cached else missing.add(id)
        }
        if (missing.isEmpty()) return found
        val urls = relayUrls(
            buildList {
                addAll(relays)
                addAll(RelayList.FALLBACK)
            },
        )
        if (urls.isEmpty()) return found
        val fetched = mutableListOf<Nip01Event>()
        for (chunk in missing.chunked(EVENT_CHUNK)) {
            val filter = JSONObject()
                .put("ids", JSONArray().apply { chunk.forEach { put(it) } })
                .put("limit", chunk.size)
            for (event in query(urls, listOf(filter))) {
                found[event.id.lowercase()] = event
                fetched.add(event)
            }
        }
        EventCache.putAll(fetched)
        return found
    }

    fun fetchHighlightsForArticle(
        readRelays: List<String>,
        coordinate: String? = null,
        eventId: String? = null,
    ): List<Nip01Event> {
        val urls = relayUrls(readRelays)
        val filters = buildList {
            if (!coordinate.isNullOrBlank()) {
                add(
                    JSONObject()
                        .put("kinds", JSONArray().put(Nip01Event.KIND_HIGHLIGHT))
                        .put("#a", JSONArray().put(coordinate))
                        .put("limit", 200),
                )
            }
            if (!eventId.isNullOrBlank()) {
                add(
                    JSONObject()
                        .put("kinds", JSONArray().put(Nip01Event.KIND_HIGHLIGHT))
                        .put("#e", JSONArray().put(eventId))
                        .put("limit", 200),
                )
            }
        }
        if (filters.isEmpty()) return emptyList()
        if (urls.isNotEmpty()) {
            EventCache.putAll(
                query(urls, filters)
                    .filter { it.kind == Nip01Event.KIND_HIGHLIGHT && it.content.isNotBlank() },
            )
        }
        return cachedHighlightsForArticle(coordinate, eventId)
    }

    fun cachedHighlightsForArticle(
        coordinate: String? = null,
        eventId: String? = null,
    ): List<Nip01Event> {
        val coord = coordinate?.takeIf { it.isNotBlank() }
        val id = eventId?.trim()?.lowercase()?.takeIf { it.length == 64 }
        if (coord == null && id == null) return emptyList()
        return EventCache.byKind(Nip01Event.KIND_HIGHLIGHT)
            .filter { event ->
                event.content.isNotBlank() &&
                    event.tags.any { tag ->
                        tag.size >= 2 && (
                            (coord != null && tag[0] == "a" && tag[1] == coord) ||
                                (id != null && tag[0] == "e" && tag[1].lowercase() == id)
                            )
                    }
            }
            .distinctBy { it.id }
    }

    fun fetchHighlights(
        readRelays: List<String>,
        url: String,
        pubkeyHex: String? = null,
    ): List<Nip01Event> {
        val opened = url
        val normalized = ArticleUrl.normalize(url)
        val filters = listOf(opened, normalized).distinct().map { tagged ->
            JSONObject()
                .put("kinds", JSONArray().put(Nip01Event.KIND_HIGHLIGHT))
                .put("#r", JSONArray().put(tagged))
                .put("limit", 200)
                .also { filter ->
                    if (!pubkeyHex.isNullOrBlank()) {
                        filter.put("authors", JSONArray().put(pubkeyHex))
                    }
                }
        }
        val urls = relayUrls(readRelays)
        if (urls.isNotEmpty()) {
            EventCache.putAll(
                query(urls, filters).filter { event ->
                    event.kind == Nip01Event.KIND_HIGHLIGHT && event.content.isNotBlank()
                },
            )
        }
        return cachedHighlights(url, pubkeyHex)
    }

    fun cachedHighlights(url: String, pubkeyHex: String? = null): List<Nip01Event> {
        val articleNorm = ArticleUrl.normalize(url)
        return EventCache.byKind(Nip01Event.KIND_HIGHLIGHT)
            .filter { event ->
                event.content.isNotBlank() &&
                    (pubkeyHex.isNullOrBlank() || event.pubkey.equals(pubkeyHex, ignoreCase = true)) &&
                    event.tags.any { tag ->
                        tag.size >= 2 &&
                            tag[0] == "r" &&
                            ArticleUrl.normalize(tag[1]) == articleNorm
                    }
            }
    }

    fun cachedProfiles(pubkeys: List<String>): Map<String, Profile> =
        pubkeys.map { it.lowercase() }.distinct().mapNotNull { key ->
            EventCache.latest(Nip01Event.KIND_METADATA, key)?.let { key to Profile.parse(it.content) }
        }.toMap()

    fun fetchArchives(
        readRelays: List<String>,
        pubkeyHex: String,
        content: ReadableContent,
    ): List<Nip01Event> {
        val kind = Archive.kind(content)
        val filter = when (kind) {
            Nip01Event.KIND_REACTION -> {
                val eventId = content.eventId?.trim()?.takeIf { it.length == 64 } ?: return emptyList()
                JSONObject()
                    .put("kinds", JSONArray().put(Nip01Event.KIND_REACTION))
                    .put("authors", JSONArray().put(pubkeyHex))
                    .put("#e", JSONArray().put(eventId.lowercase()))
                    .put("limit", 20)
            }
            Nip01Event.KIND_URL_REACTION -> {
                JSONObject()
                    .put("kinds", JSONArray().put(Nip01Event.KIND_URL_REACTION))
                    .put("authors", JSONArray().put(pubkeyHex))
                    .put("#r", JSONArray().put(Archive.normalizeUrl(content.url)))
                    .put("limit", 20)
            }
            else -> return emptyList()
        }
        val urls = relayUrls(readRelays)
        if (urls.isNotEmpty()) {
            val remote = query(urls, listOf(filter))
                .filter { event ->
                    Archive.isArchive(event) && event.pubkey.equals(pubkeyHex, ignoreCase = true)
                }
            EventCache.putAll(remote)
        }
        return EventCache.byKindAndAuthor(setOf(kind), pubkeyHex)
            .filter { Archive.isArchive(it) && archiveMatches(it, kind, content) }
            .distinctBy { it.id }
    }

    private fun archiveMatches(event: Nip01Event, kind: Int, content: ReadableContent): Boolean =
        when (kind) {
            Nip01Event.KIND_REACTION -> {
                val target = content.eventId?.trim()?.lowercase()
                target != null &&
                    event.tags.any { it.size >= 2 && it[0] == "e" && it[1].lowercase() == target }
            }
            Nip01Event.KIND_URL_REACTION -> {
                val target = Archive.normalizeUrl(content.url)
                event.tags.any { it.size >= 2 && it[0] == "r" && Archive.normalizeUrl(it[1]) == target }
            }
            else -> false
        }

    fun publish(writeRelays: List<String>, event: Nip01Event): PublishResult {
        cacheEvent(event)
        val targets = reachableRelays(relayUrls(writeRelays))
        if (targets.isEmpty()) return PublishResult(remoteOk = false, localOk = false)
        val remoteOk = AtomicBoolean(false)
        val localOk = AtomicBoolean(false)
        val responses = CountDownLatch(targets.size)
        val active = mutableListOf<PooledRelay>()
        for (url in targets) {
            val relay = RelayPool.acquire(url)
            active.add(relay)
            val signaled = AtomicBoolean(false)
            relay.publish(event) { ok ->
                if (ok) {
                    if (LocalRelays.isLocal(url)) localOk.set(true) else remoteOk.set(true)
                }
                if (signaled.compareAndSet(false, true)) responses.countDown()
            }
        }
        try {
            responses.await(PUBLISH_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
        }
        active.forEach { it.cancelPublish(event.id) }
        return PublishResult(remoteOk = remoteOk.get(), localOk = localOk.get())
    }

    internal fun rawQuery(urls: List<String>, filters: List<JSONObject>): List<Nip01Event> =
        query(urls, filters)

    private fun query(urls: List<String>, filters: List<JSONObject>): List<Nip01Event> =
        queryPerRelay(urls.associateWith { filters })

    /** Like [query], but each relay receives its own filter set. */
    private fun queryPerRelay(targets: Map<String, List<JSONObject>>): List<Nip01Event> {
        val reachable = skipCooldowns(reachableRelays(targets.keys.toList()))
        if (reachable.isEmpty()) return emptyList()
        val events = ConcurrentHashMap<String, Nip01Event>()
        val eose = CountDownLatch(reachable.size)
        val active = mutableListOf<Pair<PooledRelay, String>>()
        for (url in reachable) {
            val filters = targets[url].orEmpty()
            if (filters.isEmpty()) {
                eose.countDown()
                continue
            }
            val relay = RelayPool.acquire(url)
            val subId = newId()
            active.add(relay to subId)
            val eoseSignaled = AtomicBoolean(false)
            relay.subscribe(
                subId = subId,
                filters = filters,
                onEvent = { event -> events[event.id] = event },
                onEose = {
                    if (eoseSignaled.compareAndSet(false, true)) eose.countDown()
                },
            )
        }
        try {
            eose.await(QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
        }
        active.forEach { (relay, subId) -> relay.unsubscribe(subId) }
        return events.values.toList()
    }

    /**
     * Drops relays that are in failure cooldown, but never an explicitly targeted
     * single relay, and never all of them.
     */
    private fun skipCooldowns(urls: List<String>): List<String> {
        if (urls.size <= 1) return urls
        val active = urls.filterNot { RelayHealth.inCooldown(it) }
        return active.ifEmpty { urls }
    }

    private fun highlightFilter(limit: Int, authors: List<String>): JSONObject =
        kindFilter(Nip01Event.KIND_HIGHLIGHT, limit, authors)

    private fun writingFilter(limit: Int, authors: List<String>): JSONObject =
        kindFilter(Nip01Event.KIND_LONG_FORM, limit, authors)

    private fun kindFilter(kind: Int, limit: Int, authors: List<String>): JSONObject {
        val filter = JSONObject()
            .put("kinds", JSONArray().put(kind))
            .put("limit", limit)
        if (authors.isNotEmpty()) {
            filter.put("authors", JSONArray().apply { authors.forEach { put(it) } })
        }
        return filter
    }

    private fun authorKeys(pubkeyHex: String?, authors: Collection<String>): List<String> = buildList {
        if (!pubkeyHex.isNullOrBlank()) add(pubkeyHex.lowercase())
        authors.forEach { key ->
            if (key.isNotBlank()) add(key.lowercase())
        }
    }.distinct()

    private fun relayUrls(urls: List<String>): List<String> =
        urls.mapNotNull { LocalRelays.resolve(it) }.distinct()

    private fun reachableRelays(urls: List<String>): List<String> =
        if (OfflineSync.hasNetwork()) urls else urls.filter { LocalRelays.isLocal(it) }

    private fun cacheEvent(event: Nip01Event) {
        if (event.kind == Nip01Event.KIND_DELETION) {
            EventCache.applyDeletion(event)
        }
        EventCache.put(event)
    }

    private fun newId(): String = UUID.randomUUID().toString().take(12)

    /** Runs a stale-while-revalidate refresh at most once per key per app session. */
    private fun refreshOnce(key: String, action: () -> Unit) {
        if (refreshed.add(key)) {
            refreshPool.execute { runCatching { action() } }
        }
    }

    private val refreshed = ConcurrentHashMap.newKeySet<String>()
    private val refreshPool = Executors.newFixedThreadPool(2) { runnable ->
        Thread(runnable, "relay-refresh").apply { isDaemon = true }
    }

    private const val QUERY_TIMEOUT_MS = 8_000L
    private const val PUBLISH_TIMEOUT_MS = 8_000L
    private const val DISCOVERY_WINDOW_SECONDS = 48L * 60L * 60L
    private const val PROFILE_CHUNK = 25
    private const val EVENT_CHUNK = 25
    private const val AUTHOR_CHUNK = 50

}
