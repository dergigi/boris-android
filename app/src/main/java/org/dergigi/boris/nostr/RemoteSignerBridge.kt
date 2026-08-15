package org.dergigi.boris.nostr

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

object RemoteSignerBridge {
    fun isSignerAvailable(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("nostrsigner:"))
        val infos = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(intent, 0)
        }
        return infos.isNotEmpty()
    }

    fun buildGetPublicKeyIntent(): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse("nostrsigner:")).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("type", "get_public_key")
        }
    }

    fun buildSignEventIntent(
        unsignedJson: String,
        signerPackage: String,
        currentUserHex: String,
    ): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse("nostrsigner:$unsignedJson")).apply {
            `package` = signerPackage
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("type", "sign_event")
            putExtra("current_user", currentUserHex)
        }
    }

    fun buildDecryptIntent(
        ciphertext: String,
        signerPackage: String,
        currentUserHex: String,
        peerPubkeyHex: String,
        nip44: Boolean,
    ): Intent {
        val encoded = if (ciphertext.contains('?')) Uri.encode(ciphertext) else ciphertext
        return Intent(Intent.ACTION_VIEW, Uri.parse("nostrsigner:$encoded")).apply {
            `package` = signerPackage
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("type", if (nip44) "nip44_decrypt" else "nip04_decrypt")
            putExtra("current_user", currentUserHex)
            putExtra("pubkey", peerPubkeyHex)
        }
    }
}
