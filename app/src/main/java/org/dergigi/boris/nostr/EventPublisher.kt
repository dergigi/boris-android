package org.dergigi.boris.nostr

object EventPublisher {
    fun publish(pubkeyHex: String, event: Nip01Event): PublishResult {
        val write = try {
            RelayQuery.fetchRelayList(pubkeyHex).write
        } catch (_: Exception) {
            RelayList.FALLBACK
        }
        val result = try {
            RelayQuery.publish(write, event)
        } catch (_: Exception) {
            cacheLocally(event)
            PublishResult(remoteOk = false, localOk = false)
        }
        if (result.remoteOk) OfflineOutbox.remove(event.id) else OfflineOutbox.add(event)
        return result
    }

    private fun cacheLocally(event: Nip01Event) {
        if (event.kind == Nip01Event.KIND_DELETION) {
            EventCache.applyDeletion(event)
        }
        EventCache.put(event)
    }
}
