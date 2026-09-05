package org.dergigi.boris.data

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.dergigi.boris.nostr.LightningAddress
import org.dergigi.boris.nostr.ZapReceipts
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class LnurlPayParams(
    val callback: String,
    val minSendableMsats: Long,
    val maxSendableMsats: Long,
    val allowsNostr: Boolean,
    val nostrPubkey: String?,
    val commentAllowed: Int,
)

/** LUD-16 / LUD-06 pay flow: lightning address -> pay params -> bolt11 for a zap request. */
object LnurlPay {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    fun endpoint(lud16: String): String? {
        val address = LightningAddress.parse(lud16) ?: return null
        val (name, domain) = address.split("@", limit = 2)
        return "https://${domain.lowercase()}/.well-known/lnurlp/$name"
    }

    fun parseParams(json: String): LnurlPayParams? {
        val obj = runCatching { JSONObject(json) }.getOrNull() ?: return null
        if (obj.optString("status").equals("ERROR", ignoreCase = true)) return null
        if (!obj.optString("tag").equals("payRequest", ignoreCase = true)) return null
        val callback = obj.optString("callback").takeIf { it.startsWith("https://") } ?: return null
        return LnurlPayParams(
            callback = callback,
            minSendableMsats = obj.optLong("minSendable", 1_000L),
            maxSendableMsats = obj.optLong("maxSendable", Long.MAX_VALUE),
            allowsNostr = obj.optBoolean("allowsNostr", false),
            nostrPubkey = obj.optString("nostrPubkey").takeIf { it.length == 64 }?.lowercase(),
            commentAllowed = obj.optInt("commentAllowed", 0),
        )
    }

    /** Callback URL carrying the amount, the signed zap request, and a comment when the server allows one. */
    fun invoiceUrl(
        params: LnurlPayParams,
        amountMsats: Long,
        zapRequestJson: String?,
        comment: String?,
    ): HttpUrl? {
        val base = params.callback.toHttpUrlOrNull() ?: return null
        val builder = base.newBuilder().addQueryParameter("amount", amountMsats.toString())
        if (zapRequestJson != null && params.allowsNostr) builder.addQueryParameter("nostr", zapRequestJson)
        val trimmed = comment?.trim().orEmpty()
        if (trimmed.isNotEmpty() && params.commentAllowed > 0) {
            builder.addQueryParameter("comment", trimmed.take(params.commentAllowed))
        }
        return builder.build()
    }

    fun parseInvoice(json: String, expectedSats: Long): String? {
        val obj = runCatching { JSONObject(json) }.getOrNull() ?: return null
        if (obj.optString("status").equals("ERROR", ignoreCase = true)) return null
        val pr = obj.optString("pr").takeIf { it.startsWith("ln", ignoreCase = true) } ?: return null
        val sats = ZapReceipts.bolt11Sats(pr) ?: return null
        return pr.takeIf { sats == expectedSats }
    }

    fun fetchParams(lud16: String): LnurlPayParams? {
        val url = endpoint(lud16)?.let(PublicHttpDestination::toHttpUrl) ?: return null
        return get(url)?.let(::parseParams)
    }

    fun fetchInvoice(url: HttpUrl, expectedSats: Long): String? {
        val safe = PublicHttpDestination.toHttpUrl(url.toString()) ?: return null
        return get(safe)?.let { parseInvoice(it, expectedSats) }
    }

    private fun get(url: HttpUrl): String? {
        val request = Request.Builder().url(url).header("Accept", "application/json").get().build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) null else response.body?.string()
            }
        }.getOrNull()
    }
}
