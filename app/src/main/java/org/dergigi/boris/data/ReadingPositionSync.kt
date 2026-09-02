package org.dergigi.boris.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.dergigi.boris.nostr.EventPublisher
import org.dergigi.boris.nostr.EventSigner
import org.dergigi.boris.nostr.Nip85
import org.dergigi.boris.nostr.PendingUnsignedEvent
import org.dergigi.boris.nostr.RelayList
import org.dergigi.boris.nostr.RelayQuery
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Syncs reading positions across devices via kind 39802 events (webapp-compatible).
 * Publishing signs silently: Amber via its content provider (needs background
 * permission), bunker via NIP-46. It never pops signer UI mid-reading; when
 * silent signing is unavailable the position stays device-local.
 */
object ReadingPositionSync {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshedAt = AtomicLong(0)
    private val lastPublished = ConcurrentHashMap<String, Float>()
    private const val REFRESH_INTERVAL_MS = 5 * 60_000L
    private const val MIN_PUBLISH_FRACTION = 0.01f

    fun refreshAsync(context: Context) {
        val app = context.applicationContext
        scope.launch { refresh(app) }
    }

    /** Pulls remote positions into [ReadingPositionStore]. Throttled; safe to call often. */
    fun refresh(context: Context) {
        if (!SettingsSync.settings.value.syncReadingPosition) return
        val session = SessionStore.load(context) ?: return
        val now = System.currentTimeMillis()
        val last = refreshedAt.get()
        if (now - last < REFRESH_INTERVAL_MS || !refreshedAt.compareAndSet(last, now)) return
        val relays = try {
            RelayQuery.fetchRelayList(session.pubkeyHex).read
        } catch (_: Exception) {
            RelayList.FALLBACK
        }
        val events = try {
            RelayQuery.fetchReadingPositions(session.pubkeyHex, relays)
        } catch (_: Exception) {
            return
        }
        for (event in events) {
            if (event.kind != Nip85.KIND) continue
            if (!event.pubkey.equals(session.pubkeyHex, ignoreCase = true)) continue
            val key = Nip85.positionKey(event) ?: continue
            val progress = Nip85.progress(event) ?: continue
            ReadingPositionStore.merge(key, progress, Nip85.timestamp(event))
        }
    }

    /** Publishes the locally stored position for [url], if it changed since the last publish. */
    fun publishAsync(context: Context, url: String) {
        val app = context.applicationContext
        scope.launch { publish(app, url) }
    }

    private fun publish(context: Context, url: String) {
        if (!SettingsSync.settings.value.syncReadingPosition) return
        val session = SessionStore.load(context) ?: return
        val fraction = ReadingPositionStore.fraction(url)
        if (fraction < MIN_PUBLISH_FRACTION) return
        val key = ReadingPositionStore.key(url)
        if (lastPublished[key] == fraction) return
        val ts = ReadingPositionStore.updatedAt(url)
            .takeIf { it > 0 } ?: System.currentTimeMillis() / 1000
        val tags = Nip85.tags(url) ?: return
        val event = EventSigner.signSilently(
            context,
            session,
            PendingUnsignedEvent(
                pubkey = session.pubkeyHex,
                createdAt = System.currentTimeMillis() / 1000,
                kind = Nip85.KIND,
                tags = tags,
                content = Nip85.contentJson(fraction, ts),
            ),
        ) ?: return
        if (event.kind != Nip85.KIND) return
        lastPublished[key] = fraction
        EventPublisher.publish(session.pubkeyHex, event)
    }
}
