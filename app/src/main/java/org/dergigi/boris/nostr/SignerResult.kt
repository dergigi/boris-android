package org.dergigi.boris.nostr

import android.app.Activity
import android.content.Intent
import org.json.JSONObject

sealed class SignerResult {
    data class Success(val pubkeyHex: String, val signerPackage: String) : SignerResult()
    data class Signed(val event: Nip01Event) : SignerResult()
    data object Rejected : SignerResult()
    data object Cancelled : SignerResult()
}

data class PendingUnsignedEvent(
    val pubkey: String,
    val createdAt: Long,
    val kind: Int,
    val tags: List<List<String>>,
    val content: String,
) {
    fun toUnsignedJson(includePubkey: Boolean = true): String =
        Nip01Event.unsignedJson(
            kind = kind,
            content = content,
            tags = tags,
            pubkeyHex = pubkey.takeIf { includePubkey && it.isNotBlank() },
            createdAt = createdAt,
        )
}

object SignerResults {
    fun parse(
        resultCode: Int,
        rejected: Boolean,
        result: String?,
        signature: String?,
        packageName: String?,
    ): SignerResult {
        if (rejected) return SignerResult.Rejected
        if (resultCode != Activity.RESULT_OK) return SignerResult.Cancelled
        val hex = (result ?: signature)?.let { Nip19.normalizePubkey(it) }
        val pkg = packageName.orEmpty()
        if (hex == null || pkg.isBlank()) return SignerResult.Cancelled
        return SignerResult.Success(hex, pkg)
    }

    fun parse(resultCode: Int, data: Intent?): SignerResult {
        return parse(
            resultCode = resultCode,
            rejected = data?.getBooleanExtra("rejected", false) == true,
            result = data?.getStringExtra("result"),
            signature = data?.getStringExtra("signature"),
            packageName = data?.getStringExtra("package"),
        )
    }

    fun parsePlaintext(resultCode: Int, data: Intent?): String? {
        if (data?.getBooleanExtra("rejected", false) == true) return null
        if (resultCode != Activity.RESULT_OK) return null
        return data?.getStringExtra("result")?.takeIf { it.isNotBlank() }
    }

    fun parseSignedEvent(
        resultCode: Int,
        rejected: Boolean,
        event: Nip01Event?,
        sessionHex: String,
        expectedKind: Int = Nip01Event.KIND_HIGHLIGHT,
    ): SignerResult {
        if (rejected) return SignerResult.Rejected
        if (resultCode != Activity.RESULT_OK) return SignerResult.Cancelled
        if (event == null) return SignerResult.Cancelled
        if (event.kind != expectedKind) return SignerResult.Cancelled
        if (!event.pubkey.equals(sessionHex, ignoreCase = true)) return SignerResult.Cancelled
        if (!event.verify()) return SignerResult.Cancelled
        return SignerResult.Signed(event)
    }

    fun parseSignedEvent(
        resultCode: Int,
        rejected: Boolean,
        eventJson: String?,
        resultJson: String?,
        sessionHex: String,
        signature: String? = null,
        pending: PendingUnsignedEvent? = null,
    ): SignerResult {
        if (rejected) return SignerResult.Rejected
        if (resultCode != Activity.RESULT_OK) return SignerResult.Cancelled
        val event = eventFromSigner(
            eventJson = eventJson,
            resultPayload = resultJson,
            signature = signature,
            pending = pending,
        )
        return parseSignedEvent(
            resultCode,
            rejected = false,
            event = event,
            sessionHex = sessionHex,
            expectedKind = pending?.kind ?: Nip01Event.KIND_HIGHLIGHT,
        )
    }

    fun parseSignedEvent(
        resultCode: Int,
        data: Intent?,
        sessionHex: String,
        pending: PendingUnsignedEvent? = null,
    ): SignerResult {
        return parseSignedEvent(
            resultCode = resultCode,
            rejected = data?.getBooleanExtra("rejected", false) == true,
            eventJson = data?.getStringExtra("event"),
            resultJson = data?.getStringExtra("result"),
            sessionHex = sessionHex,
            signature = data?.getStringExtra("signature"),
            pending = pending,
        )
    }

    private fun eventFromSigner(
        eventJson: String?,
        resultPayload: String?,
        signature: String?,
        pending: PendingUnsignedEvent?,
    ): Nip01Event? {
        parseEventJson(eventJson)?.let { return it }
        val payload = resultPayload?.takeIf { it.isNotBlank() }
        if (payload != null && payload.trimStart().startsWith("{")) {
            parseEventJson(payload)?.let { return it }
        }
        val sig = signature?.takeIf { isSchnorrHex(it) }
            ?: payload?.takeIf { isSchnorrHex(it) }
            ?: return null
        val unsigned = pending ?: return null
        return Nip01Event.complete(
            pubkey = unsigned.pubkey,
            createdAt = unsigned.createdAt,
            kind = unsigned.kind,
            tags = unsigned.tags,
            content = unsigned.content,
            sig = sig,
        )
    }

    private fun parseEventJson(raw: String?): Nip01Event? {
        val json = raw?.takeIf { it.isNotBlank() } ?: return null
        return try {
            Nip01Event.parse(JSONObject(json))
        } catch (_: Exception) {
            null
        }
    }

    private fun isSchnorrHex(value: String): Boolean {
        if (value.length != 128) return false
        return value.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    }
}
