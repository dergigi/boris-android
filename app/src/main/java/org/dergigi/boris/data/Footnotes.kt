package org.dergigi.boris.data

object Footnotes {
    private val REFERENCE = Regex("""\[\^([^\]\s]+)](?!:)""")
    private val DEFINITION = Regex("""^\[\^([^\]\s]+)]:[ \t]*(.*)$""")
    private val FENCE = Regex("""(?s)(?:```|~~~)[^\n]*\n.*?(?:```|~~~)""")
    private val INLINE_CODE = Regex("""`+[^`]+`+""")
    private const val SUPER = "⁰¹²³⁴⁵⁶⁷⁸⁹"

    fun expand(markdown: String): String {
        if (!markdown.contains("[^")) return markdown
        val (protected, restore) = protectCode(markdown)
        val (withoutDefs, defs) = extractDefinitions(protected)
        if (defs.isEmpty()) return markdown
        val used = linkedMapOf<String, Int>()
        val withRefs = REFERENCE.replace(withoutDefs) { match ->
            val id = match.groupValues[1]
            if (id !in defs) return@replace match.value
            val n = used.getOrPut(id) { used.size + 1 }
            superscript(n)
        }
        if (used.isEmpty()) return markdown
        val notes = used.entries.joinToString("\n\n") { (id, n) ->
            listItem(n, defs.getValue(id))
        }
        return restore("${withRefs.trimEnd()}\n\n---\n\n$notes\n")
    }

    fun superscript(n: Int): String =
        n.toString().map { SUPER[it - '0'] }.joinToString("")

    private fun listItem(n: Int, body: String): String {
        val lines = body.trim().split('\n')
        val first = "$n. ${lines.first()}"
        val rest = lines.drop(1).joinToString("\n") { line ->
            if (line.isBlank()) "" else "    $line"
        }
        return if (rest.isBlank()) first else "$first\n$rest"
    }

    private fun extractDefinitions(text: String): Pair<String, Map<String, String>> {
        val defs = linkedMapOf<String, String>()
        val lines = text.split('\n')
        val out = StringBuilder()
        var i = 0
        while (i < lines.size) {
            val match = DEFINITION.matchEntire(lines[i])
            if (match != null) {
                val body = StringBuilder(match.groupValues[2])
                i++
                while (i < lines.size) {
                    val line = lines[i]
                    if (line.isBlank()) {
                        val next = lines.drop(i + 1).firstOrNull { it.isNotBlank() }
                        if (next != null && next.first().isWhitespace()) {
                            body.append('\n')
                            i++
                            continue
                        }
                        break
                    }
                    if (line.first().isWhitespace()) {
                        body.append('\n').append(line.trimStart())
                        i++
                        continue
                    }
                    break
                }
                defs[match.groupValues[1]] = body.toString()
                continue
            }
            if (out.isNotEmpty()) out.append('\n')
            out.append(lines[i])
            i++
        }
        return out.toString() to defs
    }

    private fun protectCode(text: String): Pair<String, (String) -> String> {
        val slots = mutableListOf<String>()
        fun stash(match: MatchResult): String {
            slots += match.value
            return "\u0000${slots.lastIndex}\u0000"
        }
        val fenced = FENCE.replace(text, ::stash)
        val protected = INLINE_CODE.replace(fenced, ::stash)
        return protected to { restored ->
            var next = restored
            slots.indices.reversed().forEach { i ->
                next = next.replace("\u0000$i\u0000", slots[i])
            }
            next
        }
    }
}
