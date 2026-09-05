package org.dergigi.boris.ui.settings

import android.app.Application
import android.content.Intent
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
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.data.SettingsSync
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.nostr.EventPublisher
import org.dergigi.boris.nostr.EventSigner
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.Nip78
import org.dergigi.boris.nostr.Nip89
import org.dergigi.boris.nostr.PendingUnsignedEvent
import org.dergigi.boris.nostr.RelayQuery
import org.dergigi.boris.nostr.SignOutcome

class SettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    val settings: StateFlow<UserSettings> = SettingsSync.settings

    private val _signIntent = MutableStateFlow<Intent?>(null)
    val signIntent: StateFlow<Intent?> = _signIntent.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var saveJob: Job? = null
    private var lastSynced: UserSettings = SettingsSync.settings.value
    private val signer = EventSigner(
        app = application,
        scope = viewModelScope,
        onSignIntent = { _signIntent.value = it },
    )

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
        signer.cancel()
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
        signer.onSignerResult(resultCode, data)
    }

    fun recommendBoris() {
        val app = getApplication<Application>()
        val session = SessionStore.load(app)
        if (session == null) {
            _message.value = app.getString(R.string.settings_about_recommend_sign_in)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val existing = runCatching {
                RelayQuery.fetchAppRecommendation(session.pubkeyHex)
            }.getOrNull()
            if (Nip89.alreadyRecommends(existing)) {
                _message.value = app.getString(R.string.settings_about_recommend_already)
                return@launch
            }
            val createdAt = System.currentTimeMillis() / 1000
            withContext(Dispatchers.Main) {
                signer.sign(
                    session,
                    PendingUnsignedEvent(
                        pubkey = session.pubkeyHex,
                        createdAt = createdAt,
                        kind = Nip01Event.KIND_APP_RECOMMENDATION,
                        tags = Nip89.recommendationTags(existing?.tags),
                        content = "",
                    ),
                ) { outcome ->
                    when (outcome) {
                        is SignOutcome.Signed -> {
                            if (outcome.event.kind != Nip01Event.KIND_APP_RECOMMENDATION) return@sign
                            viewModelScope.launch(Dispatchers.IO) {
                                EventPublisher.publish(session.pubkeyHex, outcome.event)
                                _message.value = app.getString(R.string.settings_about_recommend_done)
                            }
                        }
                        SignOutcome.Rejected ->
                            _message.value = app.getString(R.string.settings_about_recommend_rejected)
                        SignOutcome.Cancelled, SignOutcome.Failed ->
                            _message.value = app.getString(R.string.settings_about_recommend_cancelled)
                    }
                }
            }
        }
    }

    private fun requestSave(next: UserSettings) {
        val session = SessionStore.load(getApplication()) ?: return
        val content = next.toJson()
        val createdAt = System.currentTimeMillis() / 1000
        signer.sign(
            session,
            PendingUnsignedEvent(
                pubkey = session.pubkeyHex,
                createdAt = createdAt,
                kind = Nip01Event.KIND_APP_DATA,
                tags = Nip78.tags(),
                content = content,
            ),
        ) { outcome ->
            when (outcome) {
                is SignOutcome.Signed -> onSignedEvent(outcome.event)
                SignOutcome.Rejected -> {
                    revert()
                    _message.value = getApplication<Application>().getString(R.string.settings_rejected)
                }
                SignOutcome.Cancelled, SignOutcome.Failed -> {
                    revert()
                    _message.value = getApplication<Application>().getString(R.string.settings_cancelled)
                }
            }
        }
    }

    private fun onSignedEvent(event: Nip01Event) {
        val app = getApplication<Application>()
        val session = SessionStore.load(app) ?: return
        if (event.kind != Nip01Event.KIND_APP_DATA) return
        if (!Nip78.hasSettingsD(event)) return
        val saved = UserSettings.parse(event.content)
        SettingsSync.apply(saved)
        lastSynced = saved
        viewModelScope.launch(Dispatchers.IO) {
            EventPublisher.publish(session.pubkeyHex, event)
            SettingsSync.markSynced(saved)
        }
    }

    private fun revert() {
        SettingsSync.markSynced(lastSynced)
    }

    companion object {
        private const val SAVE_DEBOUNCE_MS = 300L
    }
}
