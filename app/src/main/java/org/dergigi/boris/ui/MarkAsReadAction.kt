package org.dergigi.boris.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dergigi.boris.R
import org.dergigi.boris.data.ArchivedArticles
import org.dergigi.boris.data.SecretBox
import org.dergigi.boris.data.Session
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.data.archiveContent
import org.dergigi.boris.nostr.Archive
import org.dergigi.boris.nostr.BunkerClient
import org.dergigi.boris.nostr.BunkerSignResult
import org.dergigi.boris.nostr.EventPublisher
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.PendingUnsignedEvent
import org.dergigi.boris.nostr.RemoteSignerBridge
import org.dergigi.boris.nostr.SignerResult
import org.dergigi.boris.nostr.SignerResults

/** Signs and publishes a mark-as-read archive reaction. */
class MarkAsReadAction(
    private val app: Application,
    private val scope: CoroutineScope,
    private val onMessage: (String) -> Unit,
    private val onArchived: (key: String?, url: String) -> Unit,
) {
    private var pending: Pending? = null

    fun request(url: String, title: String?, imageUrl: String?): Intent? {
        val session = SessionStore.load(app) ?: return null
        if (pending != null) {
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
        return when (session) {
            is Session.Amber -> {
                val unsigned = Archive.unsignedJson(content, session.pubkeyHex, createdAt)
                if (unsigned == null) {
                    fail(app.getString(R.string.reader_archive_failed))
                    return null
                }
                pending = Pending(
                    url = url,
                    unsigned = PendingUnsignedEvent(
                        pubkey = session.pubkeyHex,
                        createdAt = createdAt,
                        kind = kind,
                        tags = tags,
                        content = Archive.EMOJI,
                    ),
                )
                RemoteSignerBridge.buildSignEventIntent(unsigned, session.signerPackage, session.pubkeyHex)
            }
            is Session.Bunker -> {
                val unsigned = Archive.unsignedJson(content, pubkeyHex = null, createdAt)
                if (unsigned == null) {
                    fail(app.getString(R.string.reader_archive_failed))
                    return null
                }
                pending = Pending(url = url)
                scope.launch { signWithBunker(session, unsigned, url) }
                null
            }
        }
    }

    fun onSignerResult(resultCode: Int, data: Intent?) {
        val session = SessionStore.load(app) ?: return
        val current = pending ?: return
        when (
            val result = SignerResults.parseSignedEvent(
                resultCode,
                data,
                session.pubkeyHex,
                current.unsigned,
            )
        ) {
            is SignerResult.Signed -> onSigned(session, result.event, current.url)
            SignerResult.Rejected -> fail(app.getString(R.string.reader_archive_rejected))
            SignerResult.Cancelled, is SignerResult.Success -> {
                fail(app.getString(R.string.reader_archive_cancelled))
            }
        }
    }

    private suspend fun signWithBunker(
        session: Session.Bunker,
        unsignedJson: String,
        url: String,
    ) {
        val privkey = SecretBox.unwrap(app, session.clientPrivkeyCiphertext)
        if (privkey == null) {
            fail(app.getString(R.string.reader_archive_failed))
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
                is BunkerSignResult.Signed -> onSigned(session, result.event, url)
                BunkerSignResult.Rejected -> fail(app.getString(R.string.reader_archive_rejected))
                BunkerSignResult.RelayTimeout -> fail(app.getString(R.string.reader_archive_failed))
            }
        } finally {
            privkey.fill(0)
        }
    }

    private fun onSigned(session: Session, event: Nip01Event, url: String) {
        if (!event.pubkey.equals(session.pubkeyHex, ignoreCase = true) ||
            !event.verify() ||
            !Archive.isArchive(event)
        ) {
            fail(app.getString(R.string.reader_archive_failed))
            return
        }
        scope.launch(Dispatchers.IO) {
            val result = EventPublisher.publish(session.pubkeyHex, event)
            if (!result.accepted) {
                fail(app.getString(R.string.reader_archive_failed))
                return@launch
            }
            pending = null
            val key = Archive.targetRef(event)?.let(ArchivedArticles::key) ?: ArchivedArticles.key(url)
            onArchived(key, url)
            onMessage(app.getString(R.string.reader_archived))
        }
    }

    private fun fail(message: String) {
        pending = null
        onMessage(message)
    }

    private fun openAuthUrl(url: String) {
        runCatching {
            app.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private data class Pending(
        val url: String,
        val unsigned: PendingUnsignedEvent? = null,
    )
}
