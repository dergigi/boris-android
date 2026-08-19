package org.dergigi.boris.ui.reader

/** Quote from Android text selection, consumed once the reader can highlight it. */
object PendingHighlight {
    @Volatile
    private var quote: String? = null

    fun offer(quote: String) {
        this.quote = quote.trim().takeIf { it.isNotBlank() }
    }

    fun consume(): String? = quote.also { quote = null }
}
