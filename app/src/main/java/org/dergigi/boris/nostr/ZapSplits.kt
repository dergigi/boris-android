package org.dergigi.boris.nostr

import java.util.Locale

/**
 * Zap split tags for highlights (NIP-57 Appendix G), mirroring the webapp:
 * the highlighter, Boris, and the author(s) each get a weighted zap tag.
 * If the source event already carries zap tags (multiple authors), the
 * author share is divided proportionally among them.
 */
object ZapSplits {
    // npub19802see0gnk3vjlus0dnmfdagusqrtmsxpl5yfmkwn9uvnfnqylqduhr0x
    const val BORIS_PUBKEY = "29dea8672f44ed164bfc83db3da5bd472001af70307f42277674cbc64d33013e"

    val ZAP_RELAY = RelayList.FALLBACK.first()

    fun tags(
        highlighterPubkey: String,
        sourceAuthorPubkey: String?,
        sourceZapTags: List<List<String>>,
        highlighterWeight: Double,
        borisWeight: Double,
        authorWeight: Double,
    ): List<List<String>> = buildList {
        if (highlighterWeight > 0) {
            add(listOf("zap", highlighterPubkey, ZAP_RELAY, plain(highlighterWeight)))
        }
        if (borisWeight > 0 && BORIS_PUBKEY != highlighterPubkey) {
            add(listOf("zap", BORIS_PUBKEY, ZAP_RELAY, fixed(borisWeight)))
        }
        if (authorWeight <= 0) return@buildList

        val existing = sourceZapTags.filter { it.size >= 2 && it[0] == "zap" }
        if (existing.isNotEmpty()) {
            val totalExisting = existing.sumOf { it.getOrNull(3)?.toDoubleOrNull() ?: 1.0 }
            for (tag in existing) {
                val pubkey = tag[1]
                if (pubkey == highlighterPubkey || pubkey == BORIS_PUBKEY) continue
                val weight = tag.getOrNull(3)?.toDoubleOrNull() ?: 1.0
                val relay = tag.getOrNull(2)?.takeIf { it.isNotBlank() } ?: ZAP_RELAY
                val adjusted = weight / totalExisting * authorWeight
                if (adjusted > 0) {
                    add(listOf("zap", pubkey, relay, fixed(adjusted)))
                }
            }
        } else if (
            !sourceAuthorPubkey.isNullOrBlank() &&
            sourceAuthorPubkey != highlighterPubkey &&
            sourceAuthorPubkey != BORIS_PUBKEY
        ) {
            add(listOf("zap", sourceAuthorPubkey, ZAP_RELAY, fixed(authorWeight)))
        }
    }

    private fun plain(weight: Double): String =
        if (weight % 1.0 == 0.0) weight.toLong().toString() else weight.toString()

    private fun fixed(weight: Double): String = String.format(Locale.US, "%.1f", weight)
}
