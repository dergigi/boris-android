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

    fun parseSignedEvent(
        resultCode: Int,
        rejected: Boolean,
        event: Nip01Event?,
        sessionHex: String,
    ): SignerResult {
        if (rejected) return SignerResult.Rejected
        if (resultCode != Activity.RESULT_OK) return SignerResult.Cancelled
        if (event == null) return SignerResult.Cancelled
        if (event.kind != Nip01Event.KIND_HIGHLIGHT) return SignerResult.Cancelled
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
    ): SignerResult {
        if (rejected) return SignerResult.Rejected
        if (resultCode != Activity.RESULT_OK) return SignerResult.Cancelled
        val raw = eventJson?.takeIf { it.isNotBlank() } ?: resultJson?.takeIf { it.isNotBlank() }
            ?: return SignerResult.Cancelled
        val event = try {
            Nip01Event.parse(JSONObject(raw))
        } catch (_: Exception) {
            null
        }
        return parseSignedEvent(resultCode, rejected = false, event = event, sessionHex = sessionHex)
    }

    fun parseSignedEvent(resultCode: Int, data: Intent?, sessionHex: String): SignerResult {
        return parseSignedEvent(
            resultCode = resultCode,
            rejected = data?.getBooleanExtra("rejected", false) == true,
            eventJson = data?.getStringExtra("event"),
            resultJson = data?.getStringExtra("result"),
            sessionHex = sessionHex,
        )
    }
}
