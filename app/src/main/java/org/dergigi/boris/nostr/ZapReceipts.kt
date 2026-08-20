package org.dergigi.boris.nostr

data class ZapSupporter(
    val pubkey: String,
    val totalSats: Long,
    val zapCount: Int,
    val legend: Boolean,
)

/** NIP-57 kind-9735 zap receipt parsing and supporter aggregation. */
object ZapReceipts {
    const val MIN_SATS = 2_100L
    const val LEGEND_SATS = 69_420L

    private val bolt11Amount = Regex("^ln(?:bc|tb|tbs|bcrt)(\\d+)([munp])?1", RegexOption.IGNORE_CASE)
    private val hexKey = Regex("^[0-9a-f]{64}$", RegexOption.IGNORE_CASE)
    private val requestPubkey = Regex("\"pubkey\"\\s*:\\s*\"([0-9a-fA-F]{64})\"")
    private val requestAmount = Regex("\\[\\s*\"amount\"\\s*,\\s*\"(\\d+)\"")

    /** Zapper pubkey: the uppercase P tag, else the pubkey of the embedded zap request. */
    fun sender(event: Nip01Event): String? {
        val tagged = event.tags.firstOrNull { it.size >= 2 && it[0] == "P" }?.get(1)
        if (tagged != null && hexKey.matches(tagged)) return tagged.lowercase()
        val description = description(event) ?: return null
        return requestPubkey.find(description)?.groupValues?.get(1)?.lowercase()
    }

    /** Sats from the bolt11 invoice, falling back to the zap request amount tag (msats). */
    fun amountSats(event: Nip01Event): Long {
        val invoice = event.tags.firstOrNull { it.size >= 2 && it[0] == "bolt11" }?.get(1)
        invoice?.let { bolt11Sats(it) }?.let { return it }
        val description = description(event) ?: return 0L
        val msats = requestAmount.find(description)?.groupValues?.get(1)?.toLongOrNull() ?: return 0L
        return msats / 1000L
    }

    internal fun bolt11Sats(invoice: String): Long? {
        val match = bolt11Amount.find(invoice.trim()) ?: return null
        val digits = match.groupValues[1].toLongOrNull() ?: return null
        val msats = when (match.groupValues[2].lowercase()) {
            "m" -> digits * 100_000_000L
            "u" -> digits * 100_000L
            "n" -> digits * 100L
            "p" -> digits / 10L
            else -> digits * 100_000_000_000L
        }
        return msats / 1000L
    }

    /** Aggregates receipts into supporters with at least [minSats], biggest first. */
    fun supporters(
        events: List<Nip01Event>,
        minSats: Long = MIN_SATS,
    ): List<ZapSupporter> {
        val totals = LinkedHashMap<String, Pair<Long, Int>>()
        val seen = HashSet<String>()
        for (event in events) {
            if (event.kind != Nip01Event.KIND_ZAP_RECEIPT) continue
            if (!seen.add(event.id)) continue
            val pubkey = sender(event) ?: continue
            val sats = amountSats(event)
            if (sats <= 0L) continue
            val (sum, count) = totals[pubkey] ?: (0L to 0)
            totals[pubkey] = (sum + sats) to (count + 1)
        }
        return totals.entries
            .filter { it.value.first >= minSats }
            .sortedByDescending { it.value.first }
            .map { (pubkey, value) ->
                ZapSupporter(
                    pubkey = pubkey,
                    totalSats = value.first,
                    zapCount = value.second,
                    legend = value.first >= LEGEND_SATS,
                )
            }
    }

    /** Aggregates every positive zap amount for lightweight supporter attribution. */
    fun allSupporters(events: List<Nip01Event>): List<ZapSupporter> =
        supporters(events, minSats = 1L)

    private fun description(event: Nip01Event): String? =
        event.tags.firstOrNull { it.size >= 2 && it[0] == "description" }?.get(1)
}
