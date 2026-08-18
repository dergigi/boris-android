package org.dergigi.boris.ui.support

import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.nostr.ZapSupporter
import org.junit.Assert.assertEquals
import org.junit.Test

class SupportAvatarsTest {
    @Test
    fun skipsSupportersWithoutPictures() {
        val withPic = ZapSupporter("aa".repeat(32), 21_000, 1, legend = false)
        val without = ZapSupporter("bb".repeat(32), 3_000, 1, legend = false)
        val avatars = SupportAvatars.from(
            supporters = listOf(withPic, without),
            profiles = mapOf(
                withPic.pubkey to Profile(name = "Ada", picture = "https://example.com/a.png"),
                without.pubkey to Profile(name = "Bob", picture = "  "),
            ),
        )
        assertEquals(listOf(SupportAvatar(withPic.pubkey, "https://example.com/a.png")), avatars)
    }

    @Test
    fun keepsMeaningfulSupporterOrder() {
        val first = ZapSupporter("aa".repeat(32), 70_000, 2, legend = true)
        val second = ZapSupporter("bb".repeat(32), 3_000, 1, legend = false)
        val avatars = SupportAvatars.from(
            supporters = listOf(first, second),
            profiles = mapOf(
                first.pubkey to Profile(name = "Ada", picture = "https://example.com/a.png"),
                second.pubkey to Profile(name = "Bob", picture = "https://example.com/b.png"),
            ),
        )
        assertEquals(
            listOf(first.pubkey, second.pubkey),
            avatars.map { it.pubkey },
        )
    }
}
