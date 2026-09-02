package org.dergigi.boris.ui

import android.app.Application
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dergigi.boris.R
import org.dergigi.boris.data.LibrarySave
import org.dergigi.boris.data.LibrarySaveKind
import org.dergigi.boris.data.ReadableContent
import org.dergigi.boris.data.Session
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.nostr.CryptoOutcome
import org.dergigi.boris.nostr.EventPublisher
import org.dergigi.boris.nostr.EventSigner
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip51
import org.dergigi.boris.nostr.NipB0
import org.dergigi.boris.nostr.PendingUnsignedEvent
import org.dergigi.boris.nostr.RelayList
import org.dergigi.boris.nostr.RelayQuery
import org.dergigi.boris.nostr.SignOutcome

/** Signs and publishes a web bookmark or a private NIP-51 bookmark. */
class LibrarySaveAction(
    private val app: Application,
    private val scope: CoroutineScope,
    private val onMessage: (String) -> Unit,
    private val onSignIntent: (Intent) -> Unit,
    private val onSaved: () -> Unit = {},
) {
    private var busy = false
    private val signer = EventSigner(
        app = app,
        scope = scope,
        onSignIntent = {},
    )

    fun request(content: ReadableContent, privateBookmark: Boolean = true): Intent? {
        val session = SessionStore.load(app)
        if (session == null) {
            onMessage(app.getString(R.string.share_save_sign_in))
            return null
        }
        if (busy) return null
        busy = true
        return when (LibrarySave.kind(content, privateBookmark)) {
            LibrarySaveKind.Web -> requestWebBookmark(session, content)
            LibrarySaveKind.PrivateList -> requestPrivateBookmark(session, content)
            LibrarySaveKind.PublicList -> requestPublicBookmark(session, content)
        }
    }

    fun onSignerResult(resultCode: Int, data: Intent?) {
        signer.onSignerResult(resultCode, data)
    }

    private fun requestWebBookmark(session: Session, content: ReadableContent): Intent? {
        val createdAt = System.currentTimeMillis() / 1000
        val tags = NipB0.tags(content.url, content.title, createdAt)
        if (tags.isEmpty()) {
            fail(app.getString(R.string.reader_save_failed))
            return null
        }
        return sign(
            session,
            PendingUnsignedEvent(
                pubkey = session.pubkeyHex,
                createdAt = createdAt,
                kind = Nip01Event.KIND_WEB_BOOKMARK,
                tags = tags,
                content = "",
            ),
            emitIntent = false,
        )
    }

    private fun requestPublicBookmark(session: Session, content: ReadableContent): Intent? {
        val newTag = LibrarySave.hiddenTag(content)
        if (newTag == null) {
            fail(app.getString(R.string.reader_save_failed))
            return null
        }
        scope.launch {
            val list = withContext(Dispatchers.IO) { fetchBookmarkList(session.pubkeyHex) }
            val tags = list?.tags.orEmpty()
            if (Nip51.containsTag(tags, newTag)) {
                alreadySaved()
                return@launch
            }
            requestBookmarkListSign(session, tags + listOf(newTag), list?.content.orEmpty())
        }
        return null
    }

    private fun requestPrivateBookmark(session: Session, content: ReadableContent): Intent? {
        val newTag = LibrarySave.hiddenTag(content)
        if (newTag == null) {
            fail(app.getString(R.string.reader_save_failed))
            return null
        }
        scope.launch {
            val list = withContext(Dispatchers.IO) { fetchBookmarkList(session.pubkeyHex) }
            beginPrivate(session, list, newTag)
        }
        return null
    }

    private fun beginPrivate(
        session: Session,
        list: Nip01Event?,
        newTag: List<String>,
    ) {
        val ciphertext = list?.content.orEmpty()
        if (list != null && Nip51.looksEncrypted(ciphertext)) {
            signer.decrypt(
                session = session,
                ciphertext = ciphertext,
                peerPubkeyHex = session.pubkeyHex,
                nip44 = !Nip51.isNip04(ciphertext),
            ) { outcome ->
                when (outcome) {
                    is CryptoOutcome.Text -> continuePrivateAfterDecrypt(session, list, newTag, outcome.value)
                    CryptoOutcome.Rejected -> fail(app.getString(R.string.reader_save_rejected))
                    CryptoOutcome.Cancelled -> fail(app.getString(R.string.reader_save_cancelled))
                    CryptoOutcome.Failed -> fail(app.getString(R.string.reader_save_failed))
                }
            }?.let(onSignIntent)
            return
        }
        requestPrivateEncrypt(session, list, listOf(newTag))
    }

    private fun continuePrivateAfterDecrypt(
        session: Session,
        list: Nip01Event,
        newTag: List<String>,
        plaintext: String,
    ) {
        val tags = Nip51.parseTagArray(plaintext)
        if (tags == null) {
            fail(app.getString(R.string.reader_save_failed))
            return
        }
        if (Nip51.containsTag(tags, newTag)) {
            alreadySaved()
            return
        }
        requestPrivateEncrypt(session, list, tags + listOf(newTag))
    }

    private fun requestPrivateEncrypt(
        session: Session,
        list: Nip01Event?,
        hiddenTags: List<List<String>>,
    ) {
        signer.encrypt(
            session = session,
            plaintext = Nip51.encodeTagArray(hiddenTags),
            peerPubkeyHex = session.pubkeyHex,
        ) { outcome ->
            when (outcome) {
                is CryptoOutcome.Text -> {
                    requestBookmarkListSign(
                        session,
                        (list ?: emptyBookmarkList(session.pubkeyHex)).tags,
                        outcome.value,
                    )
                }
                CryptoOutcome.Rejected -> fail(app.getString(R.string.reader_save_rejected))
                CryptoOutcome.Cancelled -> fail(app.getString(R.string.reader_save_cancelled))
                CryptoOutcome.Failed -> fail(app.getString(R.string.reader_save_failed))
            }
        }?.let(onSignIntent)
    }

    private fun requestBookmarkListSign(
        session: Session,
        publicTags: List<List<String>>,
        content: String,
    ) {
        val createdAt = System.currentTimeMillis() / 1000
        sign(
            session,
            PendingUnsignedEvent(
                pubkey = session.pubkeyHex,
                createdAt = createdAt,
                kind = Nip01Event.KIND_BOOKMARKS,
                tags = publicTags,
                content = content,
            ),
            emitIntent = true,
        )
    }

    private fun sign(
        session: Session,
        unsigned: PendingUnsignedEvent,
        emitIntent: Boolean,
    ): Intent? {
        val intent = signer.sign(session, unsigned) { outcome ->
            when (outcome) {
                is SignOutcome.Signed -> onSigned(session, outcome.event)
                SignOutcome.Rejected -> fail(app.getString(R.string.reader_save_rejected))
                SignOutcome.Cancelled -> fail(app.getString(R.string.reader_save_cancelled))
                SignOutcome.Failed -> fail(app.getString(R.string.reader_save_failed))
            }
        }
        if (emitIntent) intent?.let(onSignIntent)
        return intent
    }

    private fun onSigned(session: Session, event: Nip01Event) {
        if (event.kind != Nip01Event.KIND_WEB_BOOKMARK && event.kind != Nip01Event.KIND_BOOKMARKS) {
            fail(app.getString(R.string.reader_save_failed))
            return
        }
        scope.launch(Dispatchers.IO) {
            val result = EventPublisher.publish(session.pubkeyHex, event)
            if (!result.accepted) {
                fail(app.getString(R.string.reader_save_failed))
                return@launch
            }
            busy = false
            onMessage(app.getString(R.string.reader_saved))
            onSaved()
        }
    }

    private suspend fun fetchBookmarkList(pubkeyHex: String): Nip01Event? {
        val relays = buildList {
            addAll(RelayList.FALLBACK)
            addAll(RelayQuery.fetchRelayList(pubkeyHex).read)
        }.distinct()
        return RelayQuery.fetchBookmarkList(pubkeyHex, relays)
    }

    private fun emptyBookmarkList(pubkeyHex: String): Nip01Event =
        Nip01Event(
            id = "0".repeat(64),
            pubkey = pubkeyHex,
            createdAt = 0,
            kind = Nip01Event.KIND_BOOKMARKS,
            tags = emptyList(),
            content = "",
            sig = "0".repeat(128),
        )

    private fun alreadySaved() {
        busy = false
        onMessage(app.getString(R.string.reader_already_saved))
        onSaved()
    }

    private fun fail(message: String) {
        busy = false
        onMessage(message)
    }
}
