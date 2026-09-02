package org.dergigi.boris.ui

import android.app.Application
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dergigi.boris.R
import org.dergigi.boris.data.ArchivedArticles
import org.dergigi.boris.data.Session
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.data.archiveContent
import org.dergigi.boris.nostr.Archive
import org.dergigi.boris.nostr.EventPublisher
import org.dergigi.boris.nostr.EventSigner
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.PendingUnsignedEvent
import org.dergigi.boris.nostr.SignOutcome

/** Signs and publishes a mark-as-read archive reaction. */
class MarkAsReadAction(
    private val app: Application,
    private val scope: CoroutineScope,
    private val onMessage: (String) -> Unit,
    private val onArchived: (key: String?, url: String) -> Unit,
) {
    private var pendingUrl: String? = null
    private val signer = EventSigner(
        app = app,
        scope = scope,
        onSignIntent = {},
    )

    fun request(url: String, title: String?, imageUrl: String?): Intent? {
        val session = SessionStore.load(app) ?: return null
        if (pendingUrl != null) {
            onMessage(app.getString(R.string.reader_archive_failed))
            return null
        }
        val content = archiveContent(url, title, imageUrl)
        val createdAt = System.currentTimeMillis() / 1000
        val kind = Archive.kind(content)
        val tags = Archive.tags(content)
        if (kind == null || tags == null) {
            fail(app.getString(R.string.reader_archive_failed))
            return null
        }
        pendingUrl = url
        return signer.sign(
            session,
            PendingUnsignedEvent(
                pubkey = session.pubkeyHex,
                createdAt = createdAt,
                kind = kind,
                tags = tags,
                content = Archive.EMOJI,
            ),
        ) { outcome ->
            when (outcome) {
                is SignOutcome.Signed -> onSigned(session, outcome.event, url)
                SignOutcome.Rejected -> fail(app.getString(R.string.reader_archive_rejected))
                SignOutcome.Cancelled -> fail(app.getString(R.string.reader_archive_cancelled))
                SignOutcome.Failed -> fail(app.getString(R.string.reader_archive_failed))
            }
        }
    }

    fun onSignerResult(resultCode: Int, data: Intent?) {
        signer.onSignerResult(resultCode, data)
    }

    private fun onSigned(session: Session, event: Nip01Event, url: String) {
        if (!Archive.isArchive(event)) {
            fail(app.getString(R.string.reader_archive_failed))
            return
        }
        scope.launch(Dispatchers.IO) {
            val result = EventPublisher.publish(session.pubkeyHex, event)
            if (!result.accepted) {
                fail(app.getString(R.string.reader_archive_failed))
                return@launch
            }
            pendingUrl = null
            val key = Archive.targetRef(event)?.let(ArchivedArticles::key) ?: ArchivedArticles.key(url)
            onArchived(key, url)
            onMessage(app.getString(R.string.reader_archived))
        }
    }

    private fun fail(message: String) {
        pendingUrl = null
        onMessage(message)
    }
}
