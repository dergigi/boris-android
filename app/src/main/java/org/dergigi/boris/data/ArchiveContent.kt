package org.dergigi.boris.data

import org.dergigi.boris.nostr.EventCache

fun archiveContent(url: String, title: String?, imageUrl: String?): ReadableContent =
    when (val target = NostrLink.parse(url)) {
        is NostrTarget.Article -> {
            val pointer = target.ref.pointer
            val event = EventCache.latest(pointer.kind, pointer.pubkey, pointer.identifier)
            ReadableContent(
                url = target.uri,
                title = title,
                articleCoordinate = target.ref.coordinate,
                eventId = event?.id,
                authorPubkey = pointer.pubkey,
                imageUrl = imageUrl,
            )
        }
        is NostrTarget.Note -> {
            val event = EventCache.event(target.eventId)
            ReadableContent(
                url = target.uri,
                title = title,
                eventId = target.eventId,
                authorPubkey = event?.pubkey ?: target.author,
                imageUrl = imageUrl,
            )
        }
        is NostrTarget.Profile, null -> {
            ReadableContent(
                url = url,
                title = title,
                imageUrl = imageUrl,
            )
        }
    }
