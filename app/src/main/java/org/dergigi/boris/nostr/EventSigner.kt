package org.dergigi.boris.nostr

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dergigi.boris.data.SecretBox
import org.dergigi.boris.data.Session
import org.json.JSONObject

sealed class SignOutcome {
    data class Signed(val event: Nip01Event) : SignOutcome()
    data object Rejected : SignOutcome()
    data object Cancelled : SignOutcome()
    data object Failed : SignOutcome()
}

sealed class CryptoOutcome {
    data class Text(val value: String) : CryptoOutcome()
    data object Rejected : CryptoOutcome()
    data object Cancelled : CryptoOutcome()
    data object Failed : CryptoOutcome()
}

/**
 * Amber / bunker signing and NIP-44 crypto. Callers build a
 * [PendingUnsignedEvent] and handle [SignOutcome]; this owns the
 * intent / bunker fork, unwrap, and result parsing.
 */
class EventSigner(
    private val app: Application,
    private val scope: CoroutineScope,
    private val onSignIntent: (Intent) -> Unit,
) {
    private var pending: Pending? = null

    fun sign(
        session: Session,
        unsigned: PendingUnsignedEvent,
        onResult: (SignOutcome) -> Unit,
    ) {
        when (session) {
            is Session.Amber -> {
                pending = Pending.Sign(unsigned, session.pubkeyHex, onResult)
                onSignIntent(
                    RemoteSignerBridge.buildSignEventIntent(
                        unsigned.toUnsignedJson(includePubkey = true),
                        session.signerPackage,
                        session.pubkeyHex,
                    ),
                )
            }
            is Session.Bunker -> {
                pending = null
                scope.launch {
                    onResult(signWithBunker(app, session, unsigned.toUnsignedJson(includePubkey = false)))
                }
            }
        }
    }

    fun decrypt(
        session: Session,
        ciphertext: String,
        peerPubkeyHex: String,
        nip44: Boolean,
        onResult: (CryptoOutcome) -> Unit,
    ) {
        when (session) {
            is Session.Amber -> {
                pending = Pending.Crypto(onResult)
                onSignIntent(
                    RemoteSignerBridge.buildDecryptIntent(
                        ciphertext = ciphertext,
                        signerPackage = session.signerPackage,
                        currentUserHex = session.pubkeyHex,
                        peerPubkeyHex = peerPubkeyHex,
                        nip44 = nip44,
                    ),
                )
            }
            is Session.Bunker -> {
                pending = null
                scope.launch {
                    onResult(
                        decryptWithBunker(app, session, ciphertext, peerPubkeyHex, nip44),
                    )
                }
            }
        }
    }

    fun encrypt(
        session: Session,
        plaintext: String,
        peerPubkeyHex: String,
        onResult: (CryptoOutcome) -> Unit,
    ) {
        when (session) {
            is Session.Amber -> {
                pending = Pending.Crypto(onResult)
                onSignIntent(
                    RemoteSignerBridge.buildEncryptIntent(
                        plaintext = plaintext,
                        signerPackage = session.signerPackage,
                        currentUserHex = session.pubkeyHex,
                        peerPubkeyHex = peerPubkeyHex,
                    ),
                )
            }
            is Session.Bunker -> {
                pending = null
                scope.launch {
                    onResult(encryptWithBunker(app, session, plaintext, peerPubkeyHex))
                }
            }
        }
    }

    /** Returns true when a pending Amber request consumed the result. */
    fun onSignerResult(resultCode: Int, data: Intent?): Boolean {
        val current = pending ?: return false
        pending = null
        when (current) {
            is Pending.Sign -> {
                when (
                    val result = SignerResults.parseSignedEvent(
                        resultCode,
                        data,
                        current.sessionHex,
                        current.unsigned,
                    )
                ) {
                    is SignerResult.Signed -> current.onResult(SignOutcome.Signed(result.event))
                    SignerResult.Rejected -> current.onResult(SignOutcome.Rejected)
                    else -> current.onResult(SignOutcome.Cancelled)
                }
            }
            is Pending.Crypto -> {
                val rejected = data?.getBooleanExtra("rejected", false) == true
                val text = SignerResults.parsePlaintext(resultCode, data)
                current.onResult(
                    when {
                        text != null -> CryptoOutcome.Text(text)
                        rejected -> CryptoOutcome.Rejected
                        else -> CryptoOutcome.Cancelled
                    },
                )
            }
        }
        return true
    }

    fun cancel() {
        pending = null
    }

    private sealed class Pending {
        data class Sign(
            val unsigned: PendingUnsignedEvent,
            val sessionHex: String,
            val onResult: (SignOutcome) -> Unit,
        ) : Pending()

        data class Crypto(
            val onResult: (CryptoOutcome) -> Unit,
        ) : Pending()
    }

    companion object {
        /** Amber content-provider or bunker RPC. Never pops signer UI. */
        fun signSilently(
            context: Context,
            session: Session,
            unsigned: PendingUnsignedEvent,
        ): Nip01Event? {
            val event = when (session) {
                is Session.Amber -> {
                    val signed = RemoteSignerBridge.signEventSilently(
                        context,
                        unsigned.toUnsignedJson(includePubkey = true),
                        session.signerPackage,
                        session.pubkeyHex,
                    ) ?: return null
                    runCatching { Nip01Event.parse(JSONObject(signed)) }.getOrNull()
                }
                is Session.Bunker -> {
                    val outcome = signWithBunker(
                        context,
                        session,
                        unsigned.toUnsignedJson(includePubkey = false),
                        openAuth = false,
                    )
                    (outcome as? SignOutcome.Signed)?.event
                }
            } ?: return null
            if (!event.pubkey.equals(session.pubkeyHex, ignoreCase = true)) return null
            if (!event.verify()) return null
            return event
        }

        private suspend fun signWithBunker(
            context: Context,
            session: Session.Bunker,
            unsignedJson: String,
        ): SignOutcome = withContext(Dispatchers.IO) {
            signWithBunker(context, session, unsignedJson, openAuth = true)
        }

        private fun signWithBunker(
            context: Context,
            session: Session.Bunker,
            unsignedJson: String,
            openAuth: Boolean,
        ): SignOutcome {
            val privkey = SecretBox.unwrap(context, session.clientPrivkeyCiphertext)
                ?: return SignOutcome.Failed
            return try {
                val result = BunkerClient(
                    onAuthUrl = if (openAuth) {
                        { url -> openAuthUrl(context, url) }
                    } else {
                        {}
                    },
                ).signEvent(
                    session.relays,
                    session.remoteSignerPubkey,
                    privkey,
                    unsignedJson,
                )
                when (result) {
                    is BunkerSignResult.Signed -> {
                        val event = result.event
                        if (!event.pubkey.equals(session.pubkeyHex, ignoreCase = true) || !event.verify()) {
                            SignOutcome.Failed
                        } else {
                            SignOutcome.Signed(event)
                        }
                    }
                    BunkerSignResult.Rejected -> SignOutcome.Rejected
                    BunkerSignResult.RelayTimeout -> SignOutcome.Failed
                }
            } finally {
                privkey.fill(0)
            }
        }

        private suspend fun decryptWithBunker(
            context: Context,
            session: Session.Bunker,
            ciphertext: String,
            peerPubkeyHex: String,
            nip44: Boolean,
        ): CryptoOutcome = withContext(Dispatchers.IO) {
            val privkey = SecretBox.unwrap(context, session.clientPrivkeyCiphertext)
                ?: return@withContext CryptoOutcome.Failed
            try {
                when (
                    val result = BunkerClient(onAuthUrl = { url -> openAuthUrl(context, url) }).decrypt(
                        session.relays,
                        session.remoteSignerPubkey,
                        privkey,
                        peerPubkeyHex,
                        ciphertext,
                        nip44,
                    )
                ) {
                    is BunkerDecryptResult.Plaintext -> CryptoOutcome.Text(result.value)
                    BunkerDecryptResult.Rejected -> CryptoOutcome.Rejected
                    BunkerDecryptResult.RelayTimeout -> CryptoOutcome.Failed
                }
            } finally {
                privkey.fill(0)
            }
        }

        private suspend fun encryptWithBunker(
            context: Context,
            session: Session.Bunker,
            plaintext: String,
            peerPubkeyHex: String,
        ): CryptoOutcome = withContext(Dispatchers.IO) {
            val privkey = SecretBox.unwrap(context, session.clientPrivkeyCiphertext)
                ?: return@withContext CryptoOutcome.Failed
            try {
                when (
                    val result = BunkerClient(onAuthUrl = { url -> openAuthUrl(context, url) }).encrypt(
                        session.relays,
                        session.remoteSignerPubkey,
                        privkey,
                        peerPubkeyHex,
                        plaintext,
                    )
                ) {
                    is BunkerEncryptResult.Ciphertext -> CryptoOutcome.Text(result.value)
                    BunkerEncryptResult.Rejected -> CryptoOutcome.Rejected
                    BunkerEncryptResult.RelayTimeout -> CryptoOutcome.Failed
                }
            } finally {
                privkey.fill(0)
            }
        }

        private fun openAuthUrl(context: Context, url: String) {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }
}
