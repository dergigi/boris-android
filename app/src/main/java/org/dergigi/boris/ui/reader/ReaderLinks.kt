package org.dergigi.boris.ui.reader

import org.dergigi.boris.data.NostrLink
import org.dergigi.boris.data.NostrTarget
import org.dergigi.boris.data.UrlExtractor
import org.dergigi.boris.nostr.HintedRelays

sealed interface ReaderLinkAction {
    data object Ignore : ReaderLinkAction
    data class OpenInReader(val url: String) : ReaderLinkAction
    data class OpenExternal(val url: String) : ReaderLinkAction
    data class OpenProfile(val pubkeyHex: String) : ReaderLinkAction
}

internal fun readerLinkAction(
    uri: String,
    currentUrl: String,
    openInReader: Boolean,
): ReaderLinkAction {
    when (val target = NostrLink.parse(uri)) {
        is NostrTarget.Profile -> {
            HintedRelays.remember(target.pubkeyHex, target.relays)
            return ReaderLinkAction.OpenProfile(target.pubkeyHex)
        }
        is NostrTarget.Article, is NostrTarget.Note, null -> Unit
    }
    val article = UrlExtractor.articleUrl(uri, currentUrl)
    if (article != null && article == currentUrl) return ReaderLinkAction.Ignore
    val target = article ?: uri
    if (UrlExtractor.isImageUrl(target)) return ReaderLinkAction.OpenInReader(target)
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
        is ReaderLinkAction.OpenProfile -> Unit
    }
}
