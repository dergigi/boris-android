package org.dergigi.boris.nostr

import org.dergigi.boris.data.ReadableContent

data class ZapTarget(
    val pubkey: String,
    val relay: String?,
    val weight: Double,
)

/** Who an article zap goes to: the article's own `zap` tags (NIP-57 Appendix G), else its author. */
object ZapRecipients {
    fun targets(content: ReadableContent): List<ZapTarget> {
        val tagged = content.sourceZapTags
            .filter { it.size >= 2 && it[0] == "zap" && hexKey.matches(it[1]) }
            .map { tag ->
                ZapTarget(
                    pubkey = tag[1].lowercase(),
                    relay = tag.getOrNull(2)?.takeIf { it.startsWith("wss://", ignoreCase = true) },
                    weight = tag.getOrNull(3)?.toDoubleOrNull()?.takeIf { it > 0 } ?: 1.0,
                )
            }
            .distinctBy { it.pubkey }
        if (tagged.isNotEmpty()) return tagged
        val author = content.authorPubkey?.lowercase()?.takeIf { hexKey.matches(it) } ?: return emptyList()
        return listOf(ZapTarget(author, null, 1.0))
    }

    /**
     * Whole-sat shares proportional to [weights]. Fractions are floored and the
     * remainder goes to the largest weight, so shares always sum to [totalSats].
     * A share can be 0 when the split is too fine; callers skip those.
     */
    fun shares(totalSats: Long, weights: List<Double>): List<Long> {
        if (weights.isEmpty() || totalSats <= 0) return weights.map { 0L }
        val sum = weights.sumOf { it.coerceAtLeast(0.0) }
        if (sum <= 0.0) return weights.map { 0L }
        val floors = weights.map { (totalSats * it.coerceAtLeast(0.0) / sum).toLong() }
        val remainder = totalSats - floors.sum()
        if (remainder == 0L) return floors
        val largest = weights.indices.maxByOrNull { weights[it] } ?: 0
        return floors.mapIndexed { i, sats -> if (i == largest) sats + remainder else sats }
    }

    private val hexKey = Regex("^[0-9a-f]{64}$", RegexOption.IGNORE_CASE)
}
