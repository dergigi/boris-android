package org.dergigi.boris.data

import android.content.Context

object SessionStore {
    const val PREFS_NAME = "boris_session"
    const val KEY_PUBKEY_HEX = "pubkey_hex"
    const val KEY_SIGNER_PACKAGE = "signer_package"

    fun load(context: Context): Session? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Session.fromStored(
            prefs.getString(KEY_PUBKEY_HEX, null),
            prefs.getString(KEY_SIGNER_PACKAGE, null),
        )
    }

    fun save(context: Context, session: Session) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PUBKEY_HEX, session.pubkeyHex)
            .putString(KEY_SIGNER_PACKAGE, session.signerPackage)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
