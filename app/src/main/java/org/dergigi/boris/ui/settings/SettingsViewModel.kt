package org.dergigi.boris.ui.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dergigi.boris.R
import org.dergigi.boris.data.SecretBox
import org.dergigi.boris.data.Session
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.nostr.EventPublisher
import org.dergigi.boris.nostr.BunkerClient
import org.dergigi.boris.nostr.BunkerSignResult
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip78
import org.dergigi.boris.nostr.PendingUnsignedEvent
import org.dergigi.boris.nostr.RemoteSignerBridge
import org.dergigi.boris.nostr.SignerResult
import org.dergigi.boris.nostr.SignerResults

class SettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    val settings: StateFlow<UserSettings> = SettingsSync.settings

    private val _signIntent = MutableStateFlow<Intent?>(null)
    val signIntent: StateFlow<Intent?> = _signIntent.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var saveJob: Job? = null
    private var pendingUnsigned: PendingUnsignedEvent? = null
    private var lastSynced: UserSettings = SettingsSync.settings.value

    init {
        viewModelScope.launch {
            SettingsSync.settings.collect { current ->
                if (!SettingsSync.dirty) lastSynced = current
            }
        }
    }

    fun consumeSignIntent() {
        _signIntent.value = null
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun cancelPending() {
        saveJob?.cancel()
        pendingUnsigned = null
        _signIntent.value = null
    }

    fun update(transform: (UserSettings) -> UserSettings) {
        val next = transform(SettingsSync.settings.value)
        SettingsSync.apply(next)
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            requestSave(next)
        }
    }

    fun onSignerResult(resultCode: Int, data: Intent?) {
        val app = getApplication<Application>()
        val session = SessionStore.load(app) ?: return
        val pending = pendingUnsigned
        pendingUnsigned = null
        when (val result = SignerResults.parseSignedEvent(resultCode, data, session.pubkeyHex, pending)) {
            is SignerResult.Signed -> onSignedEvent(result.event)
            SignerResult.Rejected -> {
                revert()
                _message.value = app.getString(R.string.settings_rejected)
            }
            SignerResult.Cancelled -> {
                revert()
                _message.value = app.getString(R.string.settings_cancelled)
            }
            is SignerResult.Success -> {
                revert()
                _message.value = app.getString(R.string.settings_cancelled)
            }
        }
    }

    private fun requestSave(next: UserSettings) {
        val app = getApplication<Application>()
        val session = SessionStore.load(app) ?: return
        val content = next.toJson()
        val createdAt = System.currentTimeMillis() / 1000
        val tags = Nip78.tags()
        when (session) {
            is Session.Amber -> {
                pendingUnsigned = PendingUnsignedEvent(
                    pubkey = session.pubkeyHex,
                    createdAt = createdAt,
                    kind = Nip01Event.KIND_APP_DATA,
                    tags = tags,
                    content = content,
                )
                val unsigned = Nip78.unsignedJson(content, session.pubkeyHex, createdAt)
                _signIntent.value = RemoteSignerBridge.buildSignEventIntent(
                    unsigned,
                    session.signerPackage,
                    session.pubkeyHex,
                )
            }
            is Session.Bunker -> {
                pendingUnsigned = null
                val unsigned = Nip78.unsignedJson(content, pubkeyHex = null, createdAt = createdAt)
                viewModelScope.launch {
                    signWithBunker(session, unsigned)
                }
            }
        }
    }

    private fun onSignedEvent(event: Nip01Event) {
        val app = getApplication<Application>()
        val session = SessionStore.load(app) ?: return
        if (event.kind != Nip01Event.KIND_APP_DATA) return
        if (!event.pubkey.equals(session.pubkeyHex, ignoreCase = true)) return
        if (!Nip78.hasSettingsD(event)) return
        if (!event.verify()) return
        val saved = UserSettings.parse(event.content)
        SettingsSync.apply(saved)
        lastSynced = saved
        viewModelScope.launch(Dispatchers.IO) {
            EventPublisher.publish(session.pubkeyHex, event)
            SettingsSync.markSynced(saved)
        }
    }

    private suspend fun signWithBunker(session: Session.Bunker, unsignedJson: String) {
        val app = getApplication<Application>()
        val privkey = SecretBox.unwrap(app, session.clientPrivkeyCiphertext)
        if (privkey == null) {
            revert()
            _message.value = app.getString(R.string.settings_cancelled)
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
                is BunkerSignResult.Signed -> onSignedEvent(result.event)
                BunkerSignResult.Rejected -> {
                    revert()
                    _message.value = app.getString(R.string.settings_rejected)
                }
                BunkerSignResult.RelayTimeout -> {
                    revert()
                    _message.value = app.getString(R.string.settings_cancelled)
                }
            }
        } finally {
            privkey.fill(0)
        }
    }

    private fun revert() {
        SettingsSync.markSynced(lastSynced)
    }

    private fun openAuthUrl(url: String) {
        val app = getApplication<Application>()
        runCatching {
            app.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    companion object {
        private const val SAVE_DEBOUNCE_MS = 300L
    }
}
