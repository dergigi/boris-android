package org.dergigi.boris.ui.reader

import org.intellij.markdown.ast.ASTNode
import java.net.URI

internal data class YoutubePreview(
    val watchUrl: String,
    val thumbnailUrl: String,
)

private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
private val MARKDOWN_LINK = Regex("""^\[([^\[\]]*)\]\(([^)]+)\)$""")
private val ANGLE_LINK = Regex("""^<([^>]+)>$""")

internal fun youtubeVideoId(url: String): String? {
    val uri = try {
        URI(url.trim())
    } catch (_: Exception) {
        return null
    }
    val scheme = uri.scheme?.lowercase()
    if (scheme != "http" && scheme != "https") return null
    val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
    val path = uri.path.orEmpty()
    val id = when (host) {
        "youtu.be" -> path.trim('/').substringBefore('/')
        "youtube.com", "m.youtube.com", "music.youtube.com" -> when {
            path == "/watch" || path.startsWith("/watch/") ->
                queryValue(uri.rawQuery, "v") ?: path.trim('/').substringAfter("watch/").substringBefore('/')
            path.startsWith("/shorts/") || path.startsWith("/embed/") || path.startsWith("/live/") ->
                path.trim('/').substringAfter('/').substringBefore('/')
            else -> queryValue(uri.rawQuery, "v")
        }
        else -> null
    }
    return id?.takeIf { VIDEO_ID.matches(it) }
}

internal fun youtubePreview(url: String): YoutubePreview? {
    val trimmed = url.trim()
    val id = youtubeVideoId(trimmed) ?: return null
    return YoutubePreview(
        watchUrl = trimmed,
        thumbnailUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg",
    )
}

internal fun standaloneYoutubePreview(text: String): YoutubePreview? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    val url = MARKDOWN_LINK.matchEntire(trimmed)?.groupValues?.get(2)?.let(::markdownDestination)
        ?: ANGLE_LINK.matchEntire(trimmed)?.groupValues?.get(1)?.trim()
        ?: trimmed
    if (url.contains(Regex("\\s"))) return null
    return youtubePreview(url)
}

internal fun standaloneYoutubePreview(content: String, node: ASTNode): YoutubePreview? {
    return standaloneYoutubePreview(content.substring(node.startOffset, node.endOffset))
}

private fun markdownDestination(raw: String): String {
    var dest = raw.trim()
    val titled = Regex("""^(.*?)(?:\s+(?:"[^"]*"|'[^']*'))$""").matchEntire(dest)
    if (titled != null) dest = titled.groupValues[1].trim()
    return dest.trim('<', '>').trim()
}

private fun queryValue(query: String?, name: String): String? {
    if (query.isNullOrBlank()) return null
    return query.split('&').firstNotNullOfOrNull { part ->
        val eq = part.indexOf('=')
        if (eq <= 0) return@firstNotNullOfOrNull null
        val key = part.substring(0, eq)
        val value = part.substring(eq + 1)
        value.takeIf { key == name && it.isNotBlank() }
    }
}
