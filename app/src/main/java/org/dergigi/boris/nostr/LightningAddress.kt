package org.dergigi.boris.nostr

object LightningAddress {
    private val pattern = Regex("^[a-zA-Z0-9._+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")

    fun parse(raw: String?): String? {
        val trimmed = raw?.trim()?.ifEmpty { null } ?: return null
        return trimmed.takeIf { pattern.matches(it) }
    }

    fun uri(address: String): String? {
        val parsed = parse(address) ?: return null
        return "lightning:$parsed"
    }
}
