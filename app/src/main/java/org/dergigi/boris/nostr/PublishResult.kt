package org.dergigi.boris.nostr

data class PublishResult(
    val remoteOk: Boolean,
    val localOk: Boolean,
) {
    val accepted: Boolean get() = remoteOk || localOk
}
