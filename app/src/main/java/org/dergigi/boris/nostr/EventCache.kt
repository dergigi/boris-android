package org.dergigi.boris.nostr

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Two-layer event cache: a RAM map by event id with newest-wins for
 * replaceable/addressable kinds, persisted as JSON files so previously
 * loaded content renders offline. Lookup path: RAM, then disk (loaded
 * once at init), then relays (callers fall through to RelayQuery).
 */
object EventCache {
    private val byId = ConcurrentHashMap<String, Nip01Event>()
    private val newest = ConcurrentHashMap<String, Nip01Event>()

    @Volatile
    private var dir: File? = null

    @Volatile
    private var initialized = false
    private val loadedLatch = CountDownLatch(1)

    private val diskExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "event-cache-disk").apply { isDaemon = true }
    }
    private val dirtyKinds = ConcurrentHashMap.newKeySet<Int>()

    fun init(directory: File) {
        if (initialized) return
        initialized = true
        dir = directory
        diskExecutor.execute {
            try {
                loadAll(directory)
            } finally {
                loadedLatch.countDown()
            }
        }
    }

    fun put(event: Nip01Event) {
        awaitLoaded()
        if (putInternal(event)) scheduleSave(event.kind)
    }

    fun putAll(events: Collection<Nip01Event>) {
        if (events.isEmpty()) return
        awaitLoaded()
        val changed = mutableSetOf<Int>()
        for (event in events) {
            if (putInternal(event)) changed.add(event.kind)
        }
        changed.forEach(::scheduleSave)
    }

    fun event(id: String): Nip01Event? {
        awaitLoaded()
        return byId[id.lowercase()]
    }

    fun latest(kind: Int, pubkeyHex: String, identifier: String? = null): Nip01Event? {
        awaitLoaded()
        return newest[newestKey(kind, pubkeyHex.lowercase(), identifier)]
    }

    fun byKindAndAuthor(kinds: Set<Int>, pubkeyHex: String): List<Nip01Event> {
        awaitLoaded()
        val key = pubkeyHex.lowercase()
        return byId.values.filter { it.kind in kinds && it.pubkey.lowercase() == key }
    }

    /** Removes events referenced by a NIP-09 deletion (e and a tags). */
    fun applyDeletion(deletion: Nip01Event) {
        awaitLoaded()
        val changed = mutableSetOf<Int>()
        for (tag in deletion.tags) {
            if (tag.size < 2) continue
            when (tag[0]) {
                "e" -> {
                    val id = tag[1].lowercase()
                    val existing = byId[id] ?: continue
                    if (!existing.pubkey.equals(deletion.pubkey, ignoreCase = true)) continue
                    byId.remove(id)
                    newestKeyFor(existing)?.let { key -> newest.remove(key, existing) }
                    changed.add(existing.kind)
                }
                "a" -> {
                    val parts = tag[1].split(":", limit = 3)
                    val kind = parts.getOrNull(0)?.toIntOrNull() ?: continue
                    val pubkey = parts.getOrNull(1)?.lowercase() ?: continue
                    if (!pubkey.equals(deletion.pubkey, ignoreCase = true)) continue
                    val key = newestKey(kind, pubkey, parts.getOrNull(2))
                    val existing = newest.remove(key) ?: continue
                    byId.remove(existing.id.lowercase())
                    changed.add(existing.kind)
                }
            }
        }
        changed.forEach(::scheduleSave)
    }

    internal fun clear() {
        byId.clear()
        newest.clear()
        dirtyKinds.clear()
    }

    private fun putInternal(event: Nip01Event): Boolean {
        val id = event.id.lowercase()
        val key = newestKeyFor(event) ?: return byId.putIfAbsent(id, event) == null
        synchronized(newest) {
            val existing = newest[key]
            if (existing != null && existing.createdAt >= event.createdAt) return false
            newest[key] = event
            if (existing != null) byId.remove(existing.id.lowercase())
            byId[id] = event
            return true
        }
    }

    private fun newestKeyFor(event: Nip01Event): String? = when {
        event.kind == Nip01Event.KIND_METADATA ||
            event.kind == Nip01Event.KIND_CONTACTS ||
            event.kind in 10000..19999 -> "${event.kind}:${event.pubkey.lowercase()}"
        event.kind in 30000..39999 ->
            "${event.kind}:${event.pubkey.lowercase()}:${event.tagValue("d").orEmpty()}"
        else -> null
    }

    private fun newestKey(kind: Int, pubkeyLower: String, identifier: String?): String = when {
        kind in 30000..39999 -> "$kind:$pubkeyLower:${identifier.orEmpty()}"
        else -> "$kind:$pubkeyLower"
    }

    private fun awaitLoaded() {
        if (!initialized || loadedLatch.count == 0L) return
        runCatching { loadedLatch.await(LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
    }

    private fun scheduleSave(kind: Int) {
        if (dir == null || kind !in PERSIST_KINDS) return
        dirtyKinds.add(kind)
        diskExecutor.execute {
            try {
                Thread.sleep(SAVE_DEBOUNCE_MS)
            } catch (_: InterruptedException) {
            }
            val pending = mutableListOf<Int>()
            val iterator = dirtyKinds.iterator()
            while (iterator.hasNext()) {
                pending.add(iterator.next())
                iterator.remove()
            }
            pending.forEach(::saveKind)
        }
    }

    private fun saveKind(kind: Int) {
        val directory = dir ?: return
        runCatching {
            directory.mkdirs()
            val events = byId.values.asSequence()
                .filter { it.kind == kind }
                .sortedByDescending { it.createdAt }
                .take(cap(kind))
                .toList()
            val array = JSONArray()
            events.forEach { array.put(JSONObject(it.toJsonString())) }
            val tmp = File(directory, "kind_$kind.json.tmp")
            tmp.writeText(array.toString())
            tmp.renameTo(File(directory, "kind_$kind.json"))
        }
    }

    private fun loadAll(directory: File) {
        val files = directory.listFiles { file ->
            file.name.startsWith("kind_") && file.name.endsWith(".json")
        } ?: return
        for (file in files) {
            runCatching {
                val array = JSONArray(file.readText())
                for (i in 0 until array.length()) {
                    Nip01Event.parse(array.getJSONObject(i))?.let(::putInternal)
                }
            }
        }
    }

    private fun cap(kind: Int): Int = when (kind) {
        Nip01Event.KIND_METADATA -> 500
        Nip01Event.KIND_LONG_FORM -> 100
        else -> 300
    }

    private val PERSIST_KINDS = setOf(
        Nip01Event.KIND_METADATA,
        Nip01Event.KIND_TEXT_NOTE,
        Nip01Event.KIND_REACTION,
        Nip01Event.KIND_URL_REACTION,
        Nip01Event.KIND_RELAY_LIST,
        Nip01Event.KIND_BOOKMARKS,
        Nip01Event.KIND_LONG_FORM,
        Nip01Event.KIND_APP_DATA,
        Nip01Event.KIND_WEB_BOOKMARK,
    )

    private const val SAVE_DEBOUNCE_MS = 500L
    private const val LOAD_TIMEOUT_SECONDS = 5L
}
