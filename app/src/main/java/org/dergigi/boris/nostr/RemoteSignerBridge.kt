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

    /**
     * Signs via the signer's content provider without showing UI (NIP-55).
     * Returns the signed event JSON, or null when the user has not granted
     * background permission for this event kind.
     */
    fun signEventSilently(
        context: Context,
        unsignedJson: String,
        signerPackage: String,
        currentUserHex: String,
    ): String? {
        return try {
            context.contentResolver.query(
                Uri.parse("content://$signerPackage.SIGN_EVENT"),
                arrayOf(unsignedJson, "", currentUserHex),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                if (cursor.getColumnIndex("rejected") >= 0) return null
                val index = cursor.getColumnIndex("event")
                if (index < 0) null else cursor.getString(index)
            }
        } catch (_: Exception) {
            null
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

    fun buildEncryptIntent(
        plaintext: String,
        signerPackage: String,
        currentUserHex: String,
        peerPubkeyHex: String,
    ): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse("nostrsigner:${Uri.encode(plaintext)}")).apply {
            `package` = signerPackage
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("type", "nip44_encrypt")
            putExtra("current_user", currentUserHex)
            putExtra("pubkey", peerPubkeyHex)
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
