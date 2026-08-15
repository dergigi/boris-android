package org.dergigi.boris.data

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class ReaderRepository(
    private val client: OkHttpClient = defaultClient,
) {
    fun fetch(url: String): ReadableContent {
        val targetUrl = UrlExtractor.normalize(url)
        val request = Request.Builder()
            .url(toProxyUrl(targetUrl))
            .header("Accept", "text/plain")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Failed to fetch readable content (${response.code})")
            }
            val text = response.body?.string().orEmpty()
            return parse(targetUrl, text)
        }
    }

    internal fun parse(targetUrl: String, text: String): ReadableContent {
        val hasMarkdownBlock = markdownBlockRegex.containsMatchIn(text)
        return if (hasMarkdownBlock) {
            val title = titleRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
            val markdown = markdownRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
            ReadableContent(
                url = targetUrl,
                title = title,
                markdown = markdown,
                publishedAt = PublishedTime.fromJinaHeader(text),
            )
        } else {
            val title = htmlTitleRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
            ReadableContent(
                url = targetUrl,
                title = title,
                html = text,
                publishedAt = PublishedTime.fromHtml(text),
            )
        }
    }

    private fun toProxyUrl(url: String): String = "https://r.jina.ai/$url"

    companion object {
        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()

        private val markdownBlockRegex = Regex("""Markdown Content:\s""", RegexOption.IGNORE_CASE)
        private val titleRegex = Regex(
            """Title:\s*(.*?)(?:\s+URL Source:|\s+Markdown Content:)""",
            RegexOption.IGNORE_CASE,
        )
        private val markdownRegex = Regex(
            """Markdown Content:\s*([\s\S]*)$""",
            RegexOption.IGNORE_CASE,
        )
        private val htmlTitleRegex = Regex(
            """<title[^>]*>(.*?)</title>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
    }
}
