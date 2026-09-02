package org.dergigi.boris.ui

import android.app.Application
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dergigi.boris.R
import org.dergigi.boris.data.ArchivedArticles
import org.dergigi.boris.data.ReadableContent
import org.dergigi.boris.data.Session
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.data.archiveContent
import org.dergigi.boris.nostr.Archive
import org.dergigi.boris.nostr.EventPublisher
import org.dergigi.boris.nostr.EventSigner
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.PendingUnsignedEvent
import org.dergigi.boris.nostr.SignOutcome

/** Signs and publishes a mark-as-read archive reaction, or a NIP-09 unarchive. */
class ArchiveAction(
    private val app: Application,
    private val scope: CoroutineScope,
    private val onMessage: (String) -> Unit,
    private val onArchived: (key: String?, url: String, eventId: String) -> Unit,
    private val onUnarchived: () -> Unit = {},
) {
    private var busy = false
    private var pendingUrl: String? = null
    private var unarchiving = false
    private val signer = EventSigner(
        app = app,
        scope = scope,
        onSignIntent = {},
    )

    fun inFlight(): Boolean = busy

    fun request(url: String, title: String?, imageUrl: String?): Intent? =
        request(archiveContent(url, title, imageUrl), url)

    fun request(content: ReadableContent, url: String = content.url): Intent? {
        val session = SessionStore.load(app) ?: return null
        if (busy) {
            onMessage(app.getString(R.string.reader_archive_failed))
            return null
        }
        val createdAt = System.currentTimeMillis() / 1000
        val kind = Archive.kind(content)
        val tags = Archive.tags(content)
        if (kind == null || tags == null) {
            fail(app.getString(R.string.reader_archive_failed))
            return null
        }
        busy = true
        unarchiving = false
        pendingUrl = url
        return sign(
            session,
            PendingUnsignedEvent(
                pubkey = session.pubkeyHex,
                createdAt = createdAt,
                kind = kind,
                tags = tags,
                content = Archive.EMOJI,
            ),
        )
    }

    fun unarchive(eventIds: List<String>): Intent? {
        val session = SessionStore.load(app) ?: return null
        if (busy) {
            onMessage(app.getString(R.string.reader_archive_failed))
            return null
        }
        val createdAt = System.currentTimeMillis() / 1000
        val tags = Archive.deleteTags(eventIds)
        if (tags.isEmpty()) {
            fail(app.getString(R.string.reader_archive_failed))
            return null
        }
        busy = true
        unarchiving = true
        pendingUrl = null
        return sign(
            session,
            PendingUnsignedEvent(
                pubkey = session.pubkeyHex,
                createdAt = createdAt,
                kind = Nip01Event.KIND_DELETION,
                tags = tags,
                content = "unarchive",
            ),
        )
    }

    fun onSignerResult(resultCode: Int, data: Intent?): Boolean =
        signer.onSignerResult(resultCode, data)

    private fun sign(session: Session, unsigned: PendingUnsignedEvent): Intent? =
        signer.sign(session, unsigned) { outcome ->
            when (outcome) {
                is SignOutcome.Signed -> onSigned(session, outcome.event)
                SignOutcome.Rejected -> fail(app.getString(R.string.reader_archive_rejected))
                SignOutcome.Cancelled -> fail(app.getString(R.string.reader_archive_cancelled))
                SignOutcome.Failed -> fail(app.getString(R.string.reader_archive_failed))
            }
        }

    private fun onSigned(session: Session, event: Nip01Event) {
        val undo = unarchiving
        if (undo) {
            if (event.kind != Nip01Event.KIND_DELETION) {
                fail(app.getString(R.string.reader_archive_failed))
                return
            }
        } else if (!Archive.isArchive(event)) {
            fail(app.getString(R.string.reader_archive_failed))
            return
        }
        val url = pendingUrl
        scope.launch(Dispatchers.IO) {
            val result = EventPublisher.publish(session.pubkeyHex, event)
            if (!result.accepted) {
                fail(app.getString(R.string.reader_archive_failed))
                return@launch
            }
            busy = false
            unarchiving = false
            pendingUrl = null
            if (undo) {
                onUnarchived()
            } else {
                val key = Archive.targetRef(event)?.let(ArchivedArticles::key)
                    ?: url?.let(ArchivedArticles::key)
                onArchived(key, url.orEmpty(), event.id)
                onMessage(app.getString(R.string.reader_archived))
            }
        }
    }

    private fun fail(message: String) {
        busy = false
        unarchiving = false
        pendingUrl = null
        onMessage(message)
    }
}

typealias MarkAsReadAction = ArchiveAction
