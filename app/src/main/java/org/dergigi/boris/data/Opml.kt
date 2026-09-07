package org.dergigi.boris.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

/** Extracts feed URLs from an OPML subscription list. */
object Opml {
    fun feedUrls(xml: String): List<String> {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml.trimStart('\uFEFF', ' ', '\n', '\r', '\t')))
        val urls = mutableListOf<String>()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            if (!parser.name.equals("outline", ignoreCase = true)) continue
            val url = parser.getAttributeValue(null, "xmlUrl")?.trim()
            if (!url.isNullOrEmpty() &&
                (url.startsWith("http://") || url.startsWith("https://")) &&
                url !in urls
            ) {
                urls.add(url)
            }
        }
        return urls
    }

    fun export(feedUrls: List<String>): String {
        val outlines = feedUrls
            .map { it.trim() }
            .filter { it.startsWith("http://") || it.startsWith("https://") }
            .distinct()
            .joinToString(separator = "\n") { url ->
                val title = ArticleUrl.host(url) ?: url
                """    <outline text="${escape(title)}" title="${escape(title)}" type="rss" xmlUrl="${escape(url)}" />"""
            }
        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<opml version="2.0">""")
            appendLine("""  <head>""")
            appendLine("""    <title>Boris RSS feeds</title>""")
            appendLine("""  </head>""")
            appendLine("""  <body>""")
            if (outlines.isNotEmpty()) appendLine(outlines)
            appendLine("""  </body>""")
            appendLine("""</opml>""")
        }
    }

    private fun escape(value: String): String = buildString(value.length) {
        value.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(char)
            }
        }
    }
}
