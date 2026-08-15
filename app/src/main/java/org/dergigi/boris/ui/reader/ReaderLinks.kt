package org.dergigi.boris.ui.reader

import org.dergigi.boris.data.UrlExtractor

sealed interface ReaderLinkAction {
    data object Ignore : ReaderLinkAction
    data class OpenInReader(val url: String) : ReaderLinkAction
    data class OpenExternal(val url: String) : ReaderLinkAction
}

internal fun readerLinkAction(
    uri: String,
    currentUrl: String,
    openInReader: Boolean,
): ReaderLinkAction {
    val article = UrlExtractor.articleUrl(uri, currentUrl)
    if (article != null && article == currentUrl) return ReaderLinkAction.Ignore
    if (openInReader && article != null) return ReaderLinkAction.OpenInReader(article)
    return ReaderLinkAction.OpenExternal(article ?: uri)
}
