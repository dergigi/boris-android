package org.dergigi.boris.ui.auth

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.dergigi.boris.R
import org.dergigi.boris.data.Session
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.nostr.Nip19
import org.dergigi.boris.nostr.RemoteSignerBridge
import org.dergigi.boris.nostr.SignerResult
import org.dergigi.boris.nostr.SignerResults

class AuthViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(readState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun refresh() {
        _state.value = readState()
    }

    fun connectIntent(): Intent? {
        if (_state.value !is AuthUiState.LoggedOut) return null
        return RemoteSignerBridge.buildGetPublicKeyIntent()
    }

    fun onSignerResult(resultCode: Int, data: Intent?) {
        val app = getApplication<Application>()
        when (val result = SignerResults.parse(resultCode, data)) {
            is SignerResult.Success -> {
                SessionStore.save(app, Session(result.pubkeyHex, result.signerPackage))
                _message.value = null
                _state.value = AuthUiState.LoggedIn(Nip19.npubEncode(result.pubkeyHex))
            }
            SignerResult.Rejected -> {
                _message.value = app.getString(R.string.auth_rejected)
            }
            SignerResult.Cancelled -> {
                _message.value = app.getString(R.string.auth_cancelled)
            }
        }
    }

    fun signOut() {
        SessionStore.clear(getApplication())
        _message.value = null
        _state.value = readState()
    }

    private fun readState(): AuthUiState {
        val app = getApplication<Application>()
        val session = SessionStore.load(app)
        if (session != null) {
            return AuthUiState.LoggedIn(Nip19.npubEncode(session.pubkeyHex))
        }
        return if (RemoteSignerBridge.isSignerAvailable(app)) {
            AuthUiState.LoggedOut
        } else {
            AuthUiState.MissingSigner
        }
    }
}
