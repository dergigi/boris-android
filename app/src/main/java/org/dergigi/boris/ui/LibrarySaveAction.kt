package org.dergigi.boris.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dergigi.boris.R
import org.dergigi.boris.data.LibrarySave
import org.dergigi.boris.data.ReadableContent
import org.dergigi.boris.data.SecretBox
import org.dergigi.boris.data.Session
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.nostr.BunkerClient
import org.dergigi.boris.nostr.BunkerDecryptResult
import org.dergigi.boris.nostr.BunkerEncryptResult
import org.dergigi.boris.nostr.BunkerSignResult
import org.dergigi.boris.nostr.EventPublisher
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip51
import org.dergigi.boris.nostr.NipB0
import org.dergigi.boris.nostr.PendingUnsignedEvent
import org.dergigi.boris.nostr.RelayList
import org.dergigi.boris.nostr.RelayQuery
import org.dergigi.boris.nostr.RemoteSignerBridge
import org.dergigi.boris.nostr.SignerResult
import org.dergigi.boris.nostr.SignerResults

/** Signs and publishes a web bookmark or a private NIP-51 bookmark. */
class LibrarySaveAction(
    private val app: Application,
    private val scope: CoroutineScope,
    private val onMessage: (String) -> Unit,
    private val onSignIntent: (Intent) -> Unit,
    private val onSaved: () -> Unit = {},
) {
    private var pendingLibrary: Pending? = null
    private var pendingUnsigned: PendingUnsignedEvent? = null
    private var busy = false

    fun request(content: ReadableContent): Intent? {
        val session = SessionStore.load(app)
        if (session == null) {
            onMessage(app.getString(R.string.share_save_sign_in))
            return null
        }
        if (busy) return null
        busy = true
        return when {
            LibrarySave.isWeb(content) -> requestWebBookmark(session, content)
            else -> requestPrivateBookmark(session, content)
        }
    }

    fun onSignerResult(resultCode: Int, data: Intent?) {
        val session = SessionStore.load(app) ?: return
        when (val step = pendingLibrary) {
            is Pending.Decrypt -> {
                pendingLibrary = null
                val plaintext = SignerResults.parsePlaintext(resultCode, data)
                if (plaintext == null) {
                    fail(app.getString(R.string.reader_save_cancelled))
                    return
                }
                continuePrivateAfterDecrypt(session, step.list, step.newTag, plaintext)
            }
            is Pending.Encrypt -> {
                pendingLibrary = null
                val ciphertext = SignerResults.parsePlaintext(resultCode, data)
                if (ciphertext == null) {
                    fail(app.getString(R.string.reader_save_cancelled))
                    return
                }
                requestBookmarkListSign(session, step.list.tags, ciphertext)
            }
            null -> {
                val pending = pendingUnsigned
                pendingUnsigned = null
                when (val result = SignerResults.parseSignedEvent(resultCode, data, session.pubkeyHex, pending)) {
                    is SignerResult.Signed -> onSigned(session, result.event)
                    SignerResult.Rejected -> fail(app.getString(R.string.reader_save_rejected))
                    SignerResult.Cancelled, is SignerResult.Success -> {
                        fail(app.getString(R.string.reader_save_cancelled))
                    }
                }
            }
        }
    }

    private fun requestWebBookmark(session: Session, content: ReadableContent): Intent? {
        val createdAt = System.currentTimeMillis() / 1000
        val tags = NipB0.tags(content.url, content.title, createdAt)
        if (tags.isEmpty()) {
            fail(app.getString(R.string.reader_save_failed))
            return null
        }
        return when (session) {
            is Session.Amber -> {
                pendingUnsigned = PendingUnsignedEvent(
                    pubkey = session.pubkeyHex,
                    createdAt = createdAt,
                    kind = Nip01Event.KIND_WEB_BOOKMARK,
                    tags = tags,
                    content = "",
                )
                NipB0.unsignedJson(content.url, content.title, session.pubkeyHex, createdAt)?.let { unsigned ->
                    RemoteSignerBridge.buildSignEventIntent(unsigned, session.signerPackage, session.pubkeyHex)
                } ?: run {
                    fail(app.getString(R.string.reader_save_failed))
                    null
                }
            }
            is Session.Bunker -> {
                val unsigned = NipB0.unsignedJson(content.url, content.title, pubkeyHex = null, createdAt)
                if (unsigned == null) {
                    fail(app.getString(R.string.reader_save_failed))
                    return null
                }
                scope.launch { signWithBunker(session, unsigned) }
                null
            }
        }
    }

    private fun requestPrivateBookmark(session: Session, content: ReadableContent): Intent? {
        val newTag = LibrarySave.hiddenTag(content)
        if (newTag == null) {
            fail(app.getString(R.string.reader_save_failed))
            return null
        }
        return when (session) {
            is Session.Amber -> {
                scope.launch {
                    val list = withContext(Dispatchers.IO) { fetchBookmarkList(session.pubkeyHex) }
                    beginPrivateAmber(session, list, newTag)
                }
                null
            }
            is Session.Bunker -> {
                scope.launch { savePrivateWithBunker(session, newTag) }
                null
            }
        }
    }

    private fun beginPrivateAmber(
        session: Session.Amber,
        list: Nip01Event?,
        newTag: List<String>,
    ) {
        val ciphertext = list?.content.orEmpty()
        if (list != null && Nip51.looksEncrypted(ciphertext)) {
            pendingLibrary = Pending.Decrypt(list, newTag)
            onSignIntent(
                RemoteSignerBridge.buildDecryptIntent(
                    ciphertext = ciphertext,
                    signerPackage = session.signerPackage,
                    currentUserHex = session.pubkeyHex,
                    peerPubkeyHex = session.pubkeyHex,
                    nip44 = !Nip51.isNip04(ciphertext),
                ),
            )
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
        if (session is Session.Amber) {
            requestPrivateEncrypt(session, list, tags + listOf(newTag))
        }
    }

    private fun requestPrivateEncrypt(
        session: Session.Amber,
        list: Nip01Event?,
        hiddenTags: List<List<String>>,
    ) {
        pendingLibrary = Pending.Encrypt(list ?: emptyBookmarkList(session.pubkeyHex), hiddenTags)
        onSignIntent(
            RemoteSignerBridge.buildEncryptIntent(
                plaintext = Nip51.encodeTagArray(hiddenTags),
                signerPackage = session.signerPackage,
                currentUserHex = session.pubkeyHex,
                peerPubkeyHex = session.pubkeyHex,
            ),
        )
    }

    private fun requestBookmarkListSign(
        session: Session,
        publicTags: List<List<String>>,
        content: String,
    ) {
        val createdAt = System.currentTimeMillis() / 1000
        when (session) {
            is Session.Amber -> {
                pendingUnsigned = PendingUnsignedEvent(
                    pubkey = session.pubkeyHex,
                    createdAt = createdAt,
                    kind = Nip01Event.KIND_BOOKMARKS,
                    tags = publicTags,
                    content = content,
                )
                onSignIntent(
                    RemoteSignerBridge.buildSignEventIntent(
                        Nip51.unsignedJson(publicTags, content, session.pubkeyHex, createdAt),
                        session.signerPackage,
                        session.pubkeyHex,
                    ),
                )
            }
            is Session.Bunker -> {
                scope.launch {
                    signWithBunker(
                        session,
                        Nip51.unsignedJson(publicTags, content, pubkeyHex = null, createdAt),
                    )
                }
            }
        }
    }

    private suspend fun savePrivateWithBunker(session: Session.Bunker, newTag: List<String>) {
        val privkey = SecretBox.unwrap(app, session.clientPrivkeyCiphertext)
        if (privkey == null) {
            fail(app.getString(R.string.reader_save_cancelled))
            return
        }
        try {
            val list = withContext(Dispatchers.IO) { fetchBookmarkList(session.pubkeyHex) }
            val hidden = if (list != null && Nip51.looksEncrypted(list.content)) {
                val decrypted = withContext(Dispatchers.IO) {
                    BunkerClient(onAuthUrl = ::openAuthUrl).decrypt(
                        session.relays,
                        session.remoteSignerPubkey,
                        privkey,
                        session.pubkeyHex,
                        list.content,
                        nip44 = !Nip51.isNip04(list.content),
                    )
                }
                when (decrypted) {
                    is BunkerDecryptResult.Plaintext -> Nip51.parseTagArray(decrypted.value)
                    else -> null
                }
            } else {
                emptyList()
            }
            if (hidden == null) {
                fail(app.getString(R.string.reader_save_failed))
                return
            }
            if (Nip51.containsTag(hidden, newTag)) {
                alreadySaved()
                return
            }
            val encrypted = withContext(Dispatchers.IO) {
                BunkerClient(onAuthUrl = ::openAuthUrl).encrypt(
                    session.relays,
                    session.remoteSignerPubkey,
                    privkey,
                    session.pubkeyHex,
                    Nip51.encodeTagArray(hidden + listOf(newTag)),
                )
            }
            val ciphertext = when (encrypted) {
                is BunkerEncryptResult.Ciphertext -> encrypted.value
                else -> null
            }
            if (ciphertext == null) {
                fail(app.getString(R.string.reader_save_rejected))
                return
            }
            val createdAt = System.currentTimeMillis() / 1000
            signWithBunker(
                session,
                Nip51.unsignedJson(list?.tags.orEmpty(), ciphertext, pubkeyHex = null, createdAt),
            )
        } finally {
            privkey.fill(0)
        }
    }

    private suspend fun signWithBunker(session: Session.Bunker, unsignedJson: String) {
        val privkey = SecretBox.unwrap(app, session.clientPrivkeyCiphertext)
        if (privkey == null) {
            fail(app.getString(R.string.reader_save_cancelled))
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
                is BunkerSignResult.Signed -> onSigned(session, result.event)
                BunkerSignResult.Rejected -> fail(app.getString(R.string.reader_save_rejected))
                BunkerSignResult.RelayTimeout -> fail(app.getString(R.string.reader_save_failed))
            }
        } finally {
            privkey.fill(0)
        }
    }

    private fun onSigned(session: Session, event: Nip01Event) {
        if (!event.pubkey.equals(session.pubkeyHex, ignoreCase = true) || !event.verify()) {
            fail(app.getString(R.string.reader_save_failed))
            return
        }
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
            pendingLibrary = null
            pendingUnsigned = null
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
        pendingLibrary = null
        pendingUnsigned = null
        onMessage(app.getString(R.string.reader_already_saved))
        onSaved()
    }

    private fun fail(message: String) {
        busy = false
        pendingLibrary = null
        pendingUnsigned = null
        onMessage(message)
    }

    private fun openAuthUrl(url: String) {
        runCatching {
            app.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private sealed class Pending {
        data class Decrypt(val list: Nip01Event, val newTag: List<String>) : Pending()
        data class Encrypt(val list: Nip01Event, val hiddenTags: List<List<String>>) : Pending()
    }
}
