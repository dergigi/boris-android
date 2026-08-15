package org.dergigi.boris.nostr

data class Profile(
    val name: String?,
    val picture: String?,
    val about: String? = null,
) {
    companion object {
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
            val unescaped = raw.replace("\\\"", "\"").replace("\\\\", "\\")
            return unescaped.trim().ifEmpty { null }
        }
    }
}
