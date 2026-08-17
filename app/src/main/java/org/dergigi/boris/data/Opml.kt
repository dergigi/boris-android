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
}
