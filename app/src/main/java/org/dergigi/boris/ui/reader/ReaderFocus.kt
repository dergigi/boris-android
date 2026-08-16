package org.dergigi.boris.ui.reader

/**
 * Carries the quote for a highlight opened from a card. The event id goes in the
 * reader route; the quote stays here so we can paint and jump before relays answer.
 */
object ReaderFocus {
    data class Target(val highlightId: String, val quote: String)

    @Volatile
    private var target: Target? = null

    fun offer(highlightId: String, quote: String) {
        val id = highlightId.trim().lowercase()
        if (id.isEmpty()) {
            target = null
            return
        }
        target = Target(id, quote.trim())
    }

    fun peek(): Target? = target

    fun clear() {
        target = null
    }
}
