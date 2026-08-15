package org.dergigi.boris.ui.auth

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dergigi.boris.R
import org.dergigi.boris.data.SecretBox
import org.dergigi.boris.data.Session
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.nostr.BunkerClient
import org.dergigi.boris.nostr.BunkerResult
import org.dergigi.boris.nostr.BunkerUri
import org.dergigi.boris.nostr.Nip19
import org.dergigi.boris.nostr.RelayQuery
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

    private val _pictureUrl = MutableStateFlow<String?>(null)
    val pictureUrl: StateFlow<String?> = _pictureUrl.asStateFlow()

    private var pairJob: Job? = null
    private var pictureJob: Job? = null

    init {
        loadPicture()
    }

    fun refresh() {
        if (pairJob?.isActive == true) return
        _state.value = readState()
        loadPicture()
    }

    fun connectIntent(): Intent? {
        val current = _state.value
        val loggedOut = current is AuthUiState.LoggedOut ||
            (current is AuthUiState.Connecting && current.prior is AuthUiState.LoggedOut)
        if (!loggedOut) return null
        return RemoteSignerBridge.buildGetPublicKeyIntent()
    }

    fun connectBunker(uri: String) {
        if (pairJob?.isActive == true) return
        val current = _state.value
        val prior = when (current) {
            is AuthUiState.Connecting -> current.prior
            AuthUiState.LoggedOut -> AuthUiState.LoggedOut
            AuthUiState.MissingSigner -> AuthUiState.MissingSigner
            is AuthUiState.LoggedIn -> return
        }
        _message.value = null
        _state.value = AuthUiState.Connecting(prior)
        val app = getApplication<Application>()
        pairJob = viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                BunkerClient(onAuthUrl = ::openAuthUrl).pair(uri)
            }
            if (!isActive) return@launch
            when (result) {
                is BunkerResult.Success -> persistBunker(app, uri, result)
                BunkerResult.BadUri -> failPair(app, R.string.auth_bunker_bad_uri)
                BunkerResult.RelayTimeout -> failPair(app, R.string.auth_bunker_timeout)
                BunkerResult.Rejected -> failPair(app, R.string.auth_bunker_rejected)
                BunkerResult.MissingPubkey -> failPair(app, R.string.auth_bunker_missing_pubkey)
            }
        }
    }

    fun onSignerResult(resultCode: Int, data: Intent?) {
        val app = getApplication<Application>()
        when (val result = SignerResults.parse(resultCode, data)) {
            is SignerResult.Success -> {
                pairJob?.cancel()
                SessionStore.save(app, Session.Amber(result.pubkeyHex, result.signerPackage))
                _message.value = null
                _state.value = AuthUiState.LoggedIn(Nip19.npubEncode(result.pubkeyHex))
                loadPicture()
            }
            SignerResult.Rejected -> {
                _message.value = app.getString(R.string.auth_rejected)
            }
            SignerResult.Cancelled -> {
                _message.value = app.getString(R.string.auth_cancelled)
            }
            is SignerResult.Signed -> {
            }
        }
    }

    fun signOut() {
        pairJob?.cancel()
        val app = getApplication<Application>()
        val bunker = SessionStore.load(app) as? Session.Bunker
        val privkey = bunker?.let { SecretBox.unwrap(app, it.clientPrivkeyCiphertext) }
        val remote = bunker?.remoteSignerPubkey
        val relays = bunker?.relays.orEmpty()
        SessionStore.clear(app)
        pictureJob?.cancel()
        _pictureUrl.value = null
        _message.value = null
        _state.value = readState()
        if (privkey != null && remote != null && relays.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                BunkerClient(onAuthUrl = {}).logout(relays, remote, privkey)
            }
        }
    }

    private fun persistBunker(app: Application, uri: String, result: BunkerResult.Success) {
        val parsed = BunkerUri.parse(uri)
        if (parsed == null) {
            failPair(app, R.string.auth_bunker_bad_uri)
            return
        }
        try {
            val clientCipher = SecretBox.wrap(app, result.clientPrivkey)
            val secretCipher = parsed.secret?.toByteArray(Charsets.UTF_8)?.let { SecretBox.wrap(app, it) }
            SessionStore.save(
                app,
                Session.Bunker(
                    pubkeyHex = result.userHex,
                    remoteSignerPubkey = parsed.remoteSignerPubkey,
                    relays = parsed.relays,
                    clientPrivkeyCiphertext = clientCipher,
                    bunkerSecretCiphertext = secretCipher,
                ),
            )
            _message.value = null
            _state.value = AuthUiState.LoggedIn(Nip19.npubEncode(result.userHex))
            loadPicture()
        } catch (_: Exception) {
            failPair(app, R.string.auth_bunker_rejected)
        }
    }

    private fun failPair(app: Application, messageRes: Int) {
        _state.value = readState()
        _message.value = app.getString(messageRes)
    }

    private fun loadPicture() {
        val session = SessionStore.load(getApplication()) ?: run {
            _pictureUrl.value = null
            return
        }
        if (pictureJob?.isActive == true) return
        pictureJob = viewModelScope.launch(Dispatchers.IO) {
            val url = try {
                RelayQuery.fetchProfilePicture(session.pubkeyHex)
            } catch (_: Exception) {
                null
            }
            _pictureUrl.value = url
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
