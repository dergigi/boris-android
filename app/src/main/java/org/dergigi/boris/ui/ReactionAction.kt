package org.dergigi.boris.ui

import android.app.Application
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dergigi.boris.R
import org.dergigi.boris.data.ReadableContent
import org.dergigi.boris.data.Session
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.nostr.Archive
import org.dergigi.boris.nostr.ArticleReaction
import org.dergigi.boris.nostr.ArticleReactions
import org.dergigi.boris.nostr.EventPublisher
import org.dergigi.boris.nostr.EventSigner
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.PendingUnsignedEvent
import org.dergigi.boris.nostr.SignOutcome

/** Signs and publishes an emoji reaction (kind 7, or kind 17 for web URLs), or a NIP-09 deletion that removes one. */
class ReactionAction(
    private val app: Application,
    private val scope: CoroutineScope,
    private val onMessage: (String) -> Unit,
    private val onReacted: (reaction: ArticleReaction, eventId: String) -> Unit,
    private val onRemoved: () -> Unit,
) {
    private var busy = false
    private var pending: ArticleReaction? = null
    private val signer = EventSigner(
        app = app,
        scope = scope,
        onSignIntent = {},
    )

    fun inFlight(): Boolean = busy

    fun request(content: ReadableContent, reaction: ArticleReaction): Intent? {
        val session = SessionStore.load(app) ?: return null
        if (busy) {
            onMessage(app.getString(R.string.reader_reaction_failed))
            return null
        }
        val kind = ArticleReactions.kind(content)
        val tags = ArticleReactions.tags(content)
        if (kind == null || tags == null) {
            fail(app.getString(R.string.reader_reaction_failed))
            return null
        }
        busy = true
        pending = reaction
        return sign(
            session,
            PendingUnsignedEvent(
                pubkey = session.pubkeyHex,
                createdAt = System.currentTimeMillis() / 1000,
                kind = kind,
                tags = tags,
                content = reaction.emoji,
            ),
        )
    }

    fun remove(eventIds: List<String>): Intent? {
        val session = SessionStore.load(app) ?: return null
        if (busy) {
            onMessage(app.getString(R.string.reader_reaction_failed))
            return null
        }
        val tags = Archive.deleteTags(eventIds)
        if (tags.isEmpty()) {
            fail(app.getString(R.string.reader_reaction_failed))
            return null
        }
        busy = true
        pending = null
        return sign(
            session,
            PendingUnsignedEvent(
                pubkey = session.pubkeyHex,
                createdAt = System.currentTimeMillis() / 1000,
                kind = Nip01Event.KIND_DELETION,
                tags = tags,
                content = "",
            ),
        )
    }

    fun onSignerResult(resultCode: Int, data: Intent?): Boolean =
        signer.onSignerResult(resultCode, data)

    private fun sign(session: Session, unsigned: PendingUnsignedEvent): Intent? =
        signer.sign(session, unsigned) { outcome ->
            when (outcome) {
                is SignOutcome.Signed -> onSigned(session, outcome.event)
                SignOutcome.Rejected -> fail(app.getString(R.string.reader_reaction_rejected))
                SignOutcome.Cancelled -> fail(app.getString(R.string.reader_reaction_cancelled))
                SignOutcome.Failed -> fail(app.getString(R.string.reader_reaction_failed))
            }
        }

    private fun onSigned(session: Session, event: Nip01Event) {
        val reaction = pending
        val kindOk = if (reaction == null) event.kind == Nip01Event.KIND_DELETION else Archive.isArchiveKind(event.kind)
        if (!kindOk) {
            fail(app.getString(R.string.reader_reaction_failed))
            return
        }
        scope.launch(Dispatchers.IO) {
            val result = EventPublisher.publish(session.pubkeyHex, event)
            if (!result.accepted) {
                fail(app.getString(R.string.reader_reaction_failed))
                return@launch
            }
            busy = false
            pending = null
            if (reaction == null) {
                onRemoved()
                onMessage(app.getString(R.string.reader_reaction_removed))
            } else {
                onReacted(reaction, event.id)
                onMessage(app.getString(R.string.reader_reacted, reaction.emoji))
            }
        }
    }

    private fun fail(message: String) {
        busy = false
        pending = null
        onMessage(message)
    }
}
