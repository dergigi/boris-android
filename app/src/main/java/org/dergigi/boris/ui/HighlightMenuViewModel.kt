package org.dergigi.boris.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dergigi.boris.R
import org.dergigi.boris.data.SecretBox
import org.dergigi.boris.data.Session
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.nostr.BunkerClient
import org.dergigi.boris.nostr.BunkerSignResult
import org.dergigi.boris.nostr.EventPublisher
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip09
import org.dergigi.boris.nostr.PendingUnsignedEvent
import org.dergigi.boris.nostr.RemoteSignerBridge
import org.dergigi.boris.nostr.SignerResult
import org.dergigi.boris.nostr.SignerResults

/** Handles the delete action of highlight card menus (NIP-09 deletion requests). */
class HighlightMenuViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _signIntent = MutableStateFlow<Intent?>(null)
    val signIntent: StateFlow<Intent?> = _signIntent.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _deleted = MutableStateFlow<Set<String>>(emptySet())
    val deleted: StateFlow<Set<String>> = _deleted.asStateFlow()

    private var pendingUnsigned: PendingUnsignedEvent? = null
    private var pendingId: String? = null

    fun consumeSignIntent() {
        _signIntent.value = null
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun canDelete(authorHex: String?): Boolean {
        if (authorHex.isNullOrBlank()) return false
        val session = SessionStore.load(getApplication()) ?: return false
        return session.pubkeyHex.equals(authorHex, ignoreCase = true)
    }

    fun delete(highlightId: String) {
        val session = SessionStore.load(getApplication()) ?: return
        val createdAt = System.currentTimeMillis() / 1000
        pendingId = highlightId
        when (session) {
            is Session.Amber -> {
                pendingUnsigned = PendingUnsignedEvent(
                    pubkey = session.pubkeyHex,
                    createdAt = createdAt,
                    kind = Nip01Event.KIND_DELETION,
                    tags = Nip09.tags(highlightId, Nip01Event.KIND_HIGHLIGHT),
                    content = Nip09.REASON,
                )
                _signIntent.value = RemoteSignerBridge.buildSignEventIntent(
                    Nip09.unsignedJson(highlightId, Nip01Event.KIND_HIGHLIGHT, session.pubkeyHex, createdAt),
                    session.signerPackage,
                    session.pubkeyHex,
                )
            }
            is Session.Bunker -> {
                pendingUnsigned = null
                val unsigned = Nip09.unsignedJson(highlightId, Nip01Event.KIND_HIGHLIGHT, null, createdAt)
                viewModelScope.launch {
                    signWithBunker(session, unsigned)
                }
            }
        }
    }

    fun onSignerResult(resultCode: Int, data: Intent?) {
        val app = getApplication<Application>()
        val session = SessionStore.load(app) ?: return
        val pending = pendingUnsigned
        pendingUnsigned = null
        when (val result = SignerResults.parseSignedEvent(resultCode, data, session.pubkeyHex, pending)) {
            is SignerResult.Signed -> onSignedDeletion(result.event)
            else -> cancel()
        }
    }

    private fun onSignedDeletion(event: Nip01Event) {
        val app = getApplication<Application>()
        val session = SessionStore.load(app) ?: return
        val id = pendingId ?: return
        pendingId = null
        if (event.kind != Nip01Event.KIND_DELETION) return
        if (!event.pubkey.equals(session.pubkeyHex, ignoreCase = true)) return
        if (!event.verify()) return
        _deleted.value = _deleted.value + id
        _message.value = app.getString(R.string.highlight_deleted)
        viewModelScope.launch(Dispatchers.IO) {
            EventPublisher.publish(session.pubkeyHex, event)
        }
    }

    private fun cancel() {
        pendingId = null
        _message.value = getApplication<Application>().getString(R.string.highlight_delete_cancelled)
    }

    private suspend fun signWithBunker(session: Session.Bunker, unsignedJson: String) {
        val app = getApplication<Application>()
        val privkey = SecretBox.unwrap(app, session.clientPrivkeyCiphertext)
        if (privkey == null) {
            cancel()
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
                is BunkerSignResult.Signed -> onSignedDeletion(result.event)
                else -> cancel()
            }
        } finally {
            privkey.fill(0)
        }
    }

    private fun openAuthUrl(url: String) {
        val app = getApplication<Application>()
        runCatching {
            app.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
