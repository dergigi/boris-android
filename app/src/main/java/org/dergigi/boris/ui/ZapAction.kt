package org.dergigi.boris.ui

import android.app.Application
import android.content.Intent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.dergigi.boris.R
import org.dergigi.boris.data.LnurlPay
import org.dergigi.boris.data.NwcStore
import org.dergigi.boris.data.ReadableContent
import org.dergigi.boris.data.Session
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.nostr.EventSigner
import org.dergigi.boris.nostr.Nip01Event
import org.dergigi.boris.nostr.NwcClient
import org.dergigi.boris.nostr.NwcResult
import org.dergigi.boris.nostr.PendingUnsignedEvent
import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.nostr.RelayQuery
import org.dergigi.boris.nostr.SignOutcome
import org.dergigi.boris.nostr.ZapRecipients
import org.dergigi.boris.nostr.ZapRequest
import kotlin.coroutines.resume

data class ZapRecipient(
    val pubkey: String,
    val name: String,
    val lud16: String,
    val weight: Double,
)

sealed interface ZapProgress {
    data object Resolving : ZapProgress

    /** Recipients that have a lightning address; [dropped] counts the ones that did not. */
    data class Ready(val recipients: List<ZapRecipient>, val dropped: Int) : ZapProgress

    data class Paying(val index: Int, val total: Int) : ZapProgress

    data class Done(val paidSats: Long, val failed: List<String>) : ZapProgress

    data class Failed(val message: String) : ZapProgress
}

/**
 * Article zap: resolve recipients from the article's zap tags (or its author),
 * then for each share fetch LNURL-pay params, sign a kind 9734 zap request,
 * fetch the invoice, and pay it through the connected NWC wallet.
 */
class ZapAction(
    private val app: Application,
    private val scope: CoroutineScope,
    private val onSignIntent: (Intent) -> Unit,
    private val onProgress: (ZapProgress?) -> Unit,
) {
    private val signer = EventSigner(app = app, scope = scope, onSignIntent = {})
    private var job: Job? = null

    fun resolve(content: ReadableContent) {
        job?.cancel()
        onProgress(ZapProgress.Resolving)
        job = scope.launch(Dispatchers.IO) {
            val targets = ZapRecipients.targets(content)
            if (targets.isEmpty()) {
                onProgress(ZapProgress.Failed(app.getString(R.string.zap_no_recipient)))
                return@launch
            }
            val profiles = try {
                RelayQuery.fetchProfiles(RelayQuery.globalReadRelays(), targets.map { it.pubkey })
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyMap()
            }
            val recipients = targets.mapNotNull { target ->
                val profile = profiles[target.pubkey]
                val lud16 = profile?.lud16 ?: return@mapNotNull null
                ZapRecipient(target.pubkey, Profile.displayName(target.pubkey, profile), lud16, target.weight)
            }
            if (recipients.isEmpty()) {
                onProgress(ZapProgress.Failed(app.getString(R.string.zap_no_lightning_address)))
            } else {
                onProgress(ZapProgress.Ready(recipients, targets.size - recipients.size))
            }
        }
    }

    fun pay(content: ReadableContent, recipients: List<ZapRecipient>, totalSats: Long, comment: String) {
        val session = SessionStore.load(app)
        if (session == null) {
            onProgress(ZapProgress.Failed(app.getString(R.string.share_save_sign_in)))
            return
        }
        val connection = NwcStore.load(app)
        val secret = NwcStore.secret(app)
        if (connection == null || secret == null) {
            onProgress(ZapProgress.Failed(app.getString(R.string.zap_no_wallet)))
            return
        }
        val shares = recipients.zip(ZapRecipients.shares(totalSats, recipients.map { it.weight }))
            .filter { it.second > 0 }
        job?.cancel()
        job = scope.launch {
            val client = NwcClient(connection.walletPubkey, connection.relays, secret)
            val relays = withContext(Dispatchers.IO) { RelayQuery.globalReadRelays() }
            var paid = 0L
            val failed = mutableListOf<String>()
            try {
                shares.forEachIndexed { index, (recipient, sats) ->
                    onProgress(ZapProgress.Paying(index, shares.size))
                    if (payOne(session, client, content, recipient, sats, comment, relays)) {
                        paid += sats
                    } else {
                        failed += recipient.name
                    }
                }
            } finally {
                secret.fill(0)
            }
            onProgress(ZapProgress.Done(paid, failed))
        }
    }

    fun onSignerResult(resultCode: Int, data: Intent?): Boolean = signer.onSignerResult(resultCode, data)

    fun cancel() {
        job?.cancel()
        signer.cancel()
        onProgress(null)
    }

    private suspend fun payOne(
        session: Session,
        client: NwcClient,
        content: ReadableContent,
        recipient: ZapRecipient,
        sats: Long,
        comment: String,
        relays: List<String>,
    ): Boolean {
        val params = withContext(Dispatchers.IO) { LnurlPay.fetchParams(recipient.lud16) } ?: return false
        val msats = sats * 1000
        if (msats < params.minSendableMsats || msats > params.maxSendableMsats) return false
        val zapRequest = if (params.allowsNostr) {
            val unsigned = PendingUnsignedEvent(
                pubkey = session.pubkeyHex,
                createdAt = System.currentTimeMillis() / 1000,
                kind = Nip01Event.KIND_ZAP_REQUEST,
                tags = ZapRequest.tags(content, recipient.pubkey, msats, relays),
                content = comment.trim(),
            )
            (sign(session, unsigned) as? SignOutcome.Signed)?.event?.toJsonString() ?: return false
        } else {
            null
        }
        val url = LnurlPay.invoiceUrl(params, msats, zapRequest, comment) ?: return false
        val invoice = withContext(Dispatchers.IO) { LnurlPay.fetchInvoice(url, sats) } ?: return false
        return withContext(Dispatchers.IO) { client.payInvoice(invoice) } is NwcResult.Ok
    }

    private suspend fun sign(session: Session, unsigned: PendingUnsignedEvent): SignOutcome =
        suspendCancellableCoroutine { continuation ->
            val intent = signer.sign(session, unsigned) { outcome ->
                if (continuation.isActive) continuation.resume(outcome)
            }
            intent?.let(onSignIntent)
            continuation.invokeOnCancellation { signer.cancel() }
        }
}
