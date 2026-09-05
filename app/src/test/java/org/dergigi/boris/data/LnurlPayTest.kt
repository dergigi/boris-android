package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LnurlPayTest {
    private val params = LnurlPayParams(
        callback = "https://getalby.com/lnurlp/gigi/callback",
        minSendableMsats = 1_000,
        maxSendableMsats = 100_000_000_000,
        allowsNostr = true,
        nostrPubkey = "ab".repeat(32),
        commentAllowed = 32,
    )

    @Test
    fun lightningAddressBecomesWellKnownEndpoint() {
        assertEquals("https://getalby.com/.well-known/lnurlp/gigi", LnurlPay.endpoint("gigi@getalby.com"))
        assertEquals("https://getalby.com/.well-known/lnurlp/Gigi", LnurlPay.endpoint("  Gigi@GetAlby.com "))
        assertNull(LnurlPay.endpoint("not an address"))
    }

    @Test
    fun parsesPayRequestParams() {
        val parsed = LnurlPay.parseParams(
            """{"callback":"https://getalby.com/lnurlp/gigi/callback","maxSendable":100000000000,
               "minSendable":1000,"metadata":"[]","tag":"payRequest","allowsNostr":true,
               "nostrPubkey":"${"AB".repeat(32)}","commentAllowed":32}""",
        )
        assertEquals(params, parsed)
    }

    @Test
    fun rejectsErrorsAndNonPayRequests() {
        assertNull(LnurlPay.parseParams("""{"status":"ERROR","reason":"nope"}"""))
        assertNull(LnurlPay.parseParams("""{"tag":"withdrawRequest","callback":"https://x"}"""))
        assertNull(LnurlPay.parseParams("""{"tag":"payRequest","callback":"http://insecure"}"""))
        assertNull(LnurlPay.parseParams("not json"))
    }

    @Test
    fun invoiceUrlEncodesAmountZapRequestAndComment() {
        val zapRequest = """{"kind":9734,"content":"hi & bye","tags":[["p","x"]]}"""
        val url = LnurlPay.invoiceUrl(params, 21_000, zapRequest, "  thanks ")
        requireNotNull(url)
        assertEquals("21000", url.queryParameter("amount"))
        assertEquals(zapRequest, url.queryParameter("nostr"))
        assertEquals("thanks", url.queryParameter("comment"))
        assertTrue(url.toString().startsWith("https://getalby.com/lnurlp/gigi/callback?amount=21000&nostr="))
    }

    @Test
    fun invoiceUrlSkipsWhatTheServerDoesNotAllow() {
        val strict = params.copy(allowsNostr = false, commentAllowed = 0)
        val url = LnurlPay.invoiceUrl(strict, 21_000, "{}", "thanks")
        requireNotNull(url)
        assertNull(url.queryParameter("nostr"))
        assertNull(url.queryParameter("comment"))
        val short = LnurlPay.invoiceUrl(params.copy(commentAllowed = 3), 1_000, null, "thanks")
        assertEquals("tha", short?.queryParameter("comment"))
    }

    @Test
    fun invoiceMustMatchTheRequestedAmount() {
        val invoice = "lnbc210n1pjexample"
        assertEquals(invoice, LnurlPay.parseInvoice("""{"pr":"$invoice","routes":[]}""", 21))
        assertNull(LnurlPay.parseInvoice("""{"pr":"$invoice"}""", 22))
        assertNull(LnurlPay.parseInvoice("""{"status":"ERROR","reason":"no"}""", 21))
        assertFalse(LnurlPay.parseInvoice("""{"pr":"garbage"}""", 21) != null)
    }
}
