package org.dergigi.boris.nostr

import android.app.Activity
import android.content.Intent

sealed class SignerResult {
    data class Success(val pubkeyHex: String, val signerPackage: String) : SignerResult()
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
}
