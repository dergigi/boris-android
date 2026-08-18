package org.dergigi.boris.nostr

data class Profile(
    val name: String?,
    val picture: String?,
    val about: String? = null,
) {
    companion object {
        fun displayName(pubkeyHex: String, profile: Profile?): String {
            profile?.name?.takeIf { it.isNotBlank() }?.let { return it }
            return shortNpub(pubkeyHex)
        }

        fun shortNpub(pubkeyHex: String): String {
            return try {
                val npub = Nip19.npubEncode(pubkeyHex)
                if (npub.length > 16) npub.take(12) + "…" else npub
            } catch (_: Exception) {
                pubkeyHex.take(8)
            }
        }

        fun parse(content: String): Profile {
            val display = jsonString(content, "display_name")
            val name = jsonString(content, "name")
            val picture = jsonString(content, "picture")
            val about = jsonString(content, "about")
            return Profile(
                name = display ?: name,
                picture = picture?.takeIf {
                    it.startsWith("http://") || it.startsWith("https://")
                },
                about = about,
            )
        }

        private fun jsonString(content: String, key: String): String? {
            val pattern = Regex(""""${Regex.escape(key)}"\s*:\s*"((?:\\.|[^"\\])*)"""")
            val raw = pattern.find(content)?.groupValues?.getOrNull(1) ?: return null
            val unescaped = unescapeJsonString(raw)
            return unescaped.trim().ifEmpty { null }
        }

        private fun unescapeJsonString(raw: String): String {
            val out = StringBuilder(raw.length)
            var i = 0
            while (i < raw.length) {
                val ch = raw[i]
                if (ch != '\\' || i == raw.lastIndex) {
                    out.append(ch)
                    i++
                    continue
                }
                when (val escaped = raw[++i]) {
                    '"' -> out.append('"')
                    '\\' -> out.append('\\')
                    '/' -> out.append('/')
                    'b' -> out.append('\b')
                    'f' -> out.append('\u000C')
                    'n' -> out.append('\n')
                    'r' -> out.append('\r')
                    't' -> out.append('\t')
                    'u' -> {
                        val hex = raw.substringOrNull(i + 1, i + 5)
                        val code = hex?.toIntOrNull(16)
                        if (code == null) {
                            out.append("\\u")
                        } else {
                            out.append(code.toChar())
                            i += 4
                        }
                    }
                    else -> out.append(escaped)
                }
                i++
            }
            return out.toString()
        }

        private fun String.substringOrNull(startIndex: Int, endIndex: Int): String? =
            if (startIndex >= 0 && endIndex <= length && startIndex <= endIndex) {
                substring(startIndex, endIndex)
            } else {
                null
            }
    }
}
