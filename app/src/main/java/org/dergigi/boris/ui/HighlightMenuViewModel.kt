package org.dergigi.boris.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dergigi.boris.R
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.nostr.EventPublisher
import org.dergigi.boris.nostr.EventSigner
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip09
import org.dergigi.boris.nostr.PendingUnsignedEvent
import org.dergigi.boris.nostr.SignOutcome

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

    private var pendingId: String? = null
    private val signer = EventSigner(
        app = application,
        scope = viewModelScope,
        onSignIntent = { _signIntent.value = it },
    )

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
        signer.sign(
            session,
            PendingUnsignedEvent(
                pubkey = session.pubkeyHex,
                createdAt = createdAt,
                kind = Nip01Event.KIND_DELETION,
                tags = Nip09.tags(highlightId, Nip01Event.KIND_HIGHLIGHT),
                content = Nip09.REASON,
            ),
        ) { outcome ->
            when (outcome) {
                is SignOutcome.Signed -> onSignedDeletion(outcome.event)
                else -> cancel()
            }
        }
    }

    fun onSignerResult(resultCode: Int, data: Intent?) {
        signer.onSignerResult(resultCode, data)
    }

    private fun onSignedDeletion(event: Nip01Event) {
        val app = getApplication<Application>()
        val session = SessionStore.load(app) ?: return
        val id = pendingId ?: return
        pendingId = null
        if (event.kind != Nip01Event.KIND_DELETION) return
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
}
