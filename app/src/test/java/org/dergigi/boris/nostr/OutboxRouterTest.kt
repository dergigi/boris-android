package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutboxRouterTest {
    private val fallback = listOf("wss://fallback.example")

    @Test
    fun fallbackRelaysAlwaysGetFullAuthorList() {
        val routes = OutboxRouter.route(
            authors = listOf("AA", "bb"),
            fallbackRelays = fallback,
            writeRelaysOf = { null },
        )
        assertEquals(setOf("aa", "bb"), routes["wss://fallback.example"])
        assertEquals(1, routes.size)
    }

    @Test
    fun authorsArePlacedOnUpToThreeWriteRelays() {
        val writes = listOf(
            "wss://one.example",
            "wss://two.example",
            "wss://three.example",
            "wss://four.example",
        )
        val routes = OutboxRouter.route(
            authors = listOf("aa"),
            fallbackRelays = fallback,
            writeRelaysOf = { writes },
        )
        assertEquals(setOf("aa"), routes["wss://one.example"])
        assertEquals(setOf("aa"), routes["wss://two.example"])
        assertEquals(setOf("aa"), routes["wss://three.example"])
        assertFalse(routes.containsKey("wss://four.example"))
    }

    @Test
    fun outboxRelaySetIsCappedByCoverage() {
        val authors = (1..10).map { "author$it" }
        val routes = OutboxRouter.route(
            authors = authors,
            fallbackRelays = fallback,
            // Every author writes to shared.example plus an author-specific relay.
            writeRelaysOf = { author -> listOf("wss://shared.example", "wss://$author.example") },
            maxOutboxRelays = 3,
        )
        assertEquals(authors.toSet(), routes["wss://shared.example"])
        // fallback + capped outbox set
        assertEquals(1 + 3, routes.size)
    }

    @Test
    fun skipsLocalFallbackAndCooldownRelays() {
        val routes = OutboxRouter.route(
            authors = listOf("aa"),
            fallbackRelays = fallback,
            writeRelaysOf = {
                listOf(
                    "ws://127.0.0.1:4869",
                    "wss://fallback.example",
                    "wss://cooling.example",
                    "wss://good.example",
                )
            },
            skip = { it == "wss://cooling.example" },
        )
        assertEquals(setOf("wss://fallback.example", "wss://good.example"), routes.keys)
    }

    @Test
    fun authorTargetsAppendWriteRelaysToBase() {
        val targets = OutboxRouter.authorTargets(
            pubkeyHex = "AA",
            base = listOf("wss://fallback.example"),
            writeRelaysOf = { listOf("wss://one.example", "wss://fallback.example") },
        )
        assertEquals(listOf("wss://fallback.example", "wss://one.example"), targets)
    }

    @Test
    fun normalizesRelayUrlVariants() {
        val routes = OutboxRouter.route(
            authors = listOf("aa"),
            fallbackRelays = fallback,
            writeRelaysOf = { listOf("wss://one.example/", "wss://one.example") },
        )
        assertTrue(routes.containsKey("wss://one.example"))
        assertEquals(setOf("wss://fallback.example", "wss://one.example"), routes.keys)
    }
}
