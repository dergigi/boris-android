package org.dergigi.boris.data

import android.content.Context

object SessionStore {
    const val PREFS_NAME = "boris_session"
    const val KEY_PUBKEY_HEX = "pubkey_hex"
    const val KEY_SIGNER_PACKAGE = "signer_package"
    const val KEY_KIND = "kind"
    const val KEY_REMOTE_SIGNER_PUBKEY = "remote_signer_pubkey"
    const val KEY_RELAYS = "relays"
    const val KEY_CLIENT_PRIVKEY = "client_privkey"
    const val KEY_BUNKER_SECRET = "bunker_secret"
    const val KIND_AMBER = "amber"
    const val KIND_BUNKER = "bunker"

    fun load(context: Context): Session? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val kind = prefs.getString(KEY_KIND, null)
        val hex = prefs.getString(KEY_PUBKEY_HEX, null)
        return if (kind == KIND_BUNKER) {
            Session.fromStoredBunker(
                hex,
                prefs.getString(KEY_REMOTE_SIGNER_PUBKEY, null),
                prefs.getString(KEY_RELAYS, null),
                prefs.getString(KEY_CLIENT_PRIVKEY, null),
                prefs.getString(KEY_BUNKER_SECRET, null),
            )
        } else {
            Session.fromStored(hex, prefs.getString(KEY_SIGNER_PACKAGE, null))
        }
    }

    fun save(context: Context, session: Session) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        when (session) {
            is Session.Amber -> {
                editor
                    .putString(KEY_KIND, KIND_AMBER)
                    .putString(KEY_PUBKEY_HEX, session.pubkeyHex)
                    .putString(KEY_SIGNER_PACKAGE, session.signerPackage)
                    .remove(KEY_REMOTE_SIGNER_PUBKEY)
                    .remove(KEY_RELAYS)
                    .remove(KEY_CLIENT_PRIVKEY)
                    .remove(KEY_BUNKER_SECRET)
                editor.apply()
                SecretBox.wipe(context)
            }
            is Session.Bunker -> {
                editor
                    .putString(KEY_KIND, KIND_BUNKER)
                    .putString(KEY_PUBKEY_HEX, session.pubkeyHex)
                    .remove(KEY_SIGNER_PACKAGE)
                    .putString(KEY_REMOTE_SIGNER_PUBKEY, session.remoteSignerPubkey)
                    .putString(KEY_RELAYS, session.relays.joinToString(","))
                    .putString(KEY_CLIENT_PRIVKEY, session.clientPrivkeyCiphertext)
                if (session.bunkerSecretCiphertext.isNullOrEmpty()) {
                    editor.remove(KEY_BUNKER_SECRET)
                } else {
                    editor.putString(KEY_BUNKER_SECRET, session.bunkerSecretCiphertext)
                }
                editor.apply()
            }
        }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        SecretBox.wipe(context)
    }

    fun keysClearedWhenSaving(kind: String): Set<String> = when (kind) {
        KIND_AMBER -> setOf(
            KEY_REMOTE_SIGNER_PUBKEY,
            KEY_RELAYS,
            KEY_CLIENT_PRIVKEY,
            KEY_BUNKER_SECRET,
        )
        KIND_BUNKER -> setOf(KEY_SIGNER_PACKAGE)
        else -> emptySet()
    }
}
