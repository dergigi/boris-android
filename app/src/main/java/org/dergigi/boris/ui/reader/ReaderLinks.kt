package org.dergigi.boris.ui.reader

import org.dergigi.boris.data.NostrProfile
import org.dergigi.boris.data.NostrLink
import org.dergigi.boris.data.UrlExtractor
import org.dergigi.boris.nostr.Nip19

sealed interface ReaderLinkAction {
    data object Ignore : ReaderLinkAction
    data class OpenInReader(val url: String) : ReaderLinkAction
    data class OpenExternal(val url: String) : ReaderLinkAction
    data class OpenProfile(val pubkey: String) : ReaderLinkAction
}

internal fun readerLinkAction(
    uri: String,
    currentUrl: String,
    openInReader: Boolean,
): ReaderLinkAction {
    NostrProfile.parse(uri)?.let { return ReaderLinkAction.OpenProfile(it.pubkey) }
    val article = UrlExtractor.articleUrl(uri, currentUrl)
    if (article != null && article == currentUrl) return ReaderLinkAction.Ignore
    if (openInReader && article != null) return ReaderLinkAction.OpenInReader(article)
    return ReaderLinkAction.OpenExternal(article ?: uri)
}

internal fun openWeblink(
    url: String,
    openInBoris: Boolean,
    onOpenArticle: (String) -> Unit,
    onOpenExternal: (String) -> Unit,
) {
    when (val action = readerLinkAction(url, currentUrl = "", openInReader = openInBoris)) {
        ReaderLinkAction.Ignore -> Unit
        is ReaderLinkAction.OpenInReader -> onOpenArticle(action.url)
        is ReaderLinkAction.OpenExternal -> onOpenExternal(action.url)
        is ReaderLinkAction.OpenProfile -> {
            onOpenExternal(NostrLink.gatewayUrl(Nip19.npubEncode(action.pubkey)))
        }
    }
}
