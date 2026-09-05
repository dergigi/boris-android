package org.dergigi.boris.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.dergigi.boris.nostr.NwcUri

/** Wallet connection without the secret; safe to hand to UI. */
data class NwcConnection(
    val walletPubkey: String,
    val relays: List<String>,
    val lud16: String?,
)

/**
 * The device's NWC wallet. The connection secret is wrapped by the Android
 * Keystore ([SecretBox.WALLET_ALIAS]) and only unwrapped for a request.
 * Device-scoped: sign-out leaves it alone, only Disconnect clears it.
 */
object NwcStore {
    const val PREFS_NAME = "boris_wallet"
    private const val KEY_WALLET_PUBKEY = "wallet_pubkey"
    private const val KEY_RELAYS = "relays"
    private const val KEY_SECRET = "secret"
    private const val KEY_LUD16 = "lud16"

    private val _connection = MutableStateFlow<NwcConnection?>(null)
    val connection: StateFlow<NwcConnection?> = _connection.asStateFlow()

    fun load(context: Context): NwcConnection? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pubkey = prefs.getString(KEY_WALLET_PUBKEY, null)
        val relays = prefs.getString(KEY_RELAYS, null)?.split(",")?.filter { it.isNotBlank() }
        val loaded = if (pubkey == null || relays.isNullOrEmpty() || !prefs.contains(KEY_SECRET)) {
            null
        } else {
            NwcConnection(pubkey, relays, prefs.getString(KEY_LUD16, null))
        }
        _connection.value = loaded
        return loaded
    }

    fun secret(context: Context): ByteArray? {
        val boxed = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SECRET, null) ?: return null
        return SecretBox.unwrap(context, boxed, SecretBox.WALLET_ALIAS)?.takeIf { it.size == 32 }
    }

    fun save(context: Context, uri: NwcUri) {
        val secret = uri.secretBytes()
        val boxed = SecretBox.wrap(context, secret, SecretBox.WALLET_ALIAS)
        secret.fill(0)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_WALLET_PUBKEY, uri.walletPubkey)
            .putString(KEY_RELAYS, uri.relays.joinToString(","))
            .putString(KEY_SECRET, boxed)
            .apply { if (uri.lud16 == null) remove(KEY_LUD16) else putString(KEY_LUD16, uri.lud16) }
            .apply()
        _connection.value = NwcConnection(uri.walletPubkey, uri.relays, uri.lud16)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        SecretBox.wipe(context, SecretBox.WALLET_ALIAS)
        _connection.value = null
    }
}
