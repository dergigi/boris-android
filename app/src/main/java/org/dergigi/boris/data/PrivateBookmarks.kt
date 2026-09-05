package org.dergigi.boris.data

/**
 * Decrypted NIP-51 private bookmark tags for the current process.
 *
 * Plaintext never touches disk. Entries are keyed by owner pubkey and the exact
 * ciphertext they were decrypted from, so a republished list or another identity
 * falls back to "locked" on its own. [clear] runs on sign-out.
 */
object PrivateBookmarks {
    private data class Entry(val ciphertext: String, val tags: List<List<String>>)

    private val entries = HashMap<String, Entry>()

    @Synchronized
    fun remember(pubkeyHex: String, ciphertext: String, tags: List<List<String>>) {
        if (ciphertext.isBlank()) return
        entries[pubkeyHex.lowercase()] = Entry(ciphertext, tags)
    }

    @Synchronized
    fun tagsFor(pubkeyHex: String, ciphertext: String): List<List<String>>? {
        val entry = entries[pubkeyHex.lowercase()] ?: return null
        return entry.tags.takeIf { entry.ciphertext == ciphertext }
    }

    @Synchronized
    fun clear() {
        entries.clear()
    }
}
