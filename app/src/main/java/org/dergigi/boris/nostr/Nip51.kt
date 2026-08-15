package org.dergigi.boris.nostr

enum class BookmarkRefKind {
    Article,
    Url,
    Note,
}

data class BookmarkRef(
    val kind: BookmarkRefKind,
    val value: String,
)

object Nip51 {
    const val KIND = Nip01Event.KIND_BOOKMARKS

    fun publicRefs(event: Nip01Event): List<BookmarkRef> = parseTags(event.tags)

    fun hiddenRefs(plaintext: String): List<BookmarkRef> {
        val tags = parseTagArray(plaintext) ?: return emptyList()
        return parseTags(tags)
    }

    fun parseTagArray(plaintext: String): List<List<String>>? {
        val s = plaintext.trim()
        if (s.length < 2 || s.first() != '[' || s.last() != ']') return null
        return try {
            parseStringArrays(s)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseStringArrays(source: String): List<List<String>>? {
        var i = 0
        fun peek(): Char? = source.getOrNull(i)
        fun skipWs() {
            while (peek()?.isWhitespace() == true) i++
        }
        fun parseString(): String? {
            if (peek() != '"') return null
            i++
            val out = StringBuilder()
            while (i < source.length) {
                when (val c = source[i++]) {
                    '\\' -> {
                        val next = peek() ?: return null
                        i++
                        out.append(
                            when (next) {
                                '"' -> '"'
                                '\\' -> '\\'
                                '/' -> '/'
                                'n' -> '\n'
                                'r' -> '\r'
                                't' -> '\t'
                                else -> next
                            },
                        )
                    }
                    '"' -> return out.toString()
                    else -> out.append(c)
                }
            }
            return null
        }
        fun parseRow(): List<String>? {
            if (peek() != '[') return null
            i++
            skipWs()
            val row = mutableListOf<String>()
            if (peek() == ']') {
                i++
                return row
            }
            while (true) {
                skipWs()
                val value = parseString() ?: return null
                row.add(value)
                skipWs()
                when (peek()) {
                    ',' -> i++
                    ']' -> {
                        i++
                        return row
                    }
                    else -> return null
                }
            }
        }
        skipWs()
        if (peek() != '[') return null
        i++
        skipWs()
        val tags = mutableListOf<List<String>>()
        if (peek() == ']') return tags
        while (true) {
            skipWs()
            val row = parseRow() ?: return null
            tags.add(row)
            skipWs()
            when (peek()) {
                ',' -> i++
                ']' -> return tags
                else -> return null
            }
        }
    }

    fun parseTags(tags: List<List<String>>): List<BookmarkRef> {
        return tags.mapNotNull { tag ->
            val name = tag.getOrNull(0) ?: return@mapNotNull null
            val value = tag.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            when (name) {
                "a" -> articleRef(value)
                "r" -> urlRef(value)
                "e" -> BookmarkRef(BookmarkRefKind.Note, value.lowercase())
                else -> null
            }
        }
    }

    fun encodeTagArray(tags: List<List<String>>): String = buildString {
        append('[')
        tags.forEachIndexed { i, tag ->
            if (i > 0) append(',')
            append('[')
            tag.forEachIndexed { j, value ->
                if (j > 0) append(',')
                append('"')
                append(jsonEscape(value))
                append('"')
            }
            append(']')
        }
        append(']')
    }

    private fun jsonEscape(value: String): String = buildString(value.length) {
        for (c in value) {
            when (c) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(c)
            }
        }
    }

    fun containsTag(tags: List<List<String>>, tag: List<String>): Boolean {
        val name = tag.getOrNull(0) ?: return false
        val value = tag.getOrNull(1) ?: return false
        return tags.any { row ->
            row.getOrNull(0) == name && row.getOrNull(1).equals(value, ignoreCase = true)
        }
    }

    fun unsignedJson(
        publicTags: List<List<String>>,
        encryptedContent: String,
        pubkeyHex: String? = null,
        createdAt: Long = System.currentTimeMillis() / 1000,
    ): String = Nip01Event.unsignedJson(KIND, encryptedContent, publicTags, pubkeyHex, createdAt)

    fun looksEncrypted(content: String): Boolean = content.isNotBlank()

    fun isNip04(content: String): Boolean = content.contains("?iv=")

    private fun articleRef(coordinate: String): BookmarkRef? {
        val parts = coordinate.split(":", limit = 3)
        if (parts.size != 3) return null
        val kind = parts[0].toIntOrNull() ?: return null
        if (kind != Nip01Event.KIND_LONG_FORM) return null
        if (parts[1].length != 64) return null
        if (parts[2].isEmpty()) return null
        return BookmarkRef(BookmarkRefKind.Article, "$kind:${parts[1].lowercase()}:${parts[2]}")
    }

    private fun urlRef(raw: String): BookmarkRef? {
        val url = when {
            raw.startsWith("http://", ignoreCase = true) -> raw
            raw.startsWith("https://", ignoreCase = true) -> raw
            raw.contains("://") -> return null
            else -> "https://$raw"
        }
        return BookmarkRef(BookmarkRefKind.Url, url)
    }
}
