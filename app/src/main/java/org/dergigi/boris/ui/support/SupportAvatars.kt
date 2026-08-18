package org.dergigi.boris.ui.support

import org.dergigi.boris.nostr.Profile
import org.dergigi.boris.nostr.ZapSupporter

data class SupportAvatar(
    val pubkey: String,
    val pictureUrl: String,
)

object SupportAvatars {
    const val CYCLE_MS = 21_000L

    fun from(
        supporters: List<ZapSupporter>,
        profiles: Map<String, Profile>,
    ): List<SupportAvatar> =
        supporters.mapNotNull { supporter ->
            val picture = profiles[supporter.pubkey]?.picture?.trim().orEmpty()
            if (picture.isEmpty()) {
                null
            } else {
                SupportAvatar(supporter.pubkey, picture)
            }
        }
}
