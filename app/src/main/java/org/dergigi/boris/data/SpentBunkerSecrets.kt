package org.dergigi.boris.data

import android.content.Context
import java.security.MessageDigest

object SpentBunkerSecrets {
    const val PREFS_NAME = "boris_bunker_spent"
    const val KEY_FINGERPRINTS = "fingerprints"

    fun fingerprint(secret: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(secret.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun contains(context: Context, secret: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_FINGERPRINTS, emptySet())?.contains(fingerprint(secret)) == true
    }

    fun add(context: Context, secret: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val next = prefs.getStringSet(KEY_FINGERPRINTS, emptySet())?.toMutableSet() ?: mutableSetOf()
        next.add(fingerprint(secret))
        prefs.edit().putStringSet(KEY_FINGERPRINTS, next).apply()
    }
}
