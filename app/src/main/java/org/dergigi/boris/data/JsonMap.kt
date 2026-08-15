package org.dergigi.boris.data

internal sealed class JsonValue {
    data class Str(val value: String) : JsonValue()
    data class Num(val raw: String) : JsonValue()
    data class Bool(val value: Boolean) : JsonValue()
    data object Null : JsonValue()
    data class Raw(val json: String) : JsonValue()
}

internal object JsonMap {
    fun parseObject(json: String): Map<String, JsonValue>? {
        val parser = Parser(json)
        return try {
            parser.skipWs()
            val map = parser.parseObject()
            parser.skipWs()
            if (parser.remaining()) null else map
        } catch (_: Exception) {
            null
        }
    }

    fun stringify(map: Map<String, JsonValue>): String = buildString {
        append('{')
        map.entries.forEachIndexed { i, (key, value) ->
            if (i > 0) append(',')
            append('"')
            append(escape(key))
            append("\":")
            append(stringifyValue(value))
        }
        append('}')
    }

    private fun stringifyValue(value: JsonValue): String = when (value) {
        is JsonValue.Str -> "\"${escape(value.value)}\""
        is JsonValue.Num -> value.raw
        is JsonValue.Bool -> if (value.value) "true" else "false"
        JsonValue.Null -> "null"
        is JsonValue.Raw -> value.json
    }

    private fun escape(value: String): String = buildString(value.length) {
        for (c in value) {
            when (c) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000c' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(c)
            }
        }
    }

    private class Parser(private val s: String) {
        private var i = 0

        fun remaining(): Boolean = i < s.length

        fun skipWs() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        fun parseObject(): LinkedHashMap<String, JsonValue> {
            expect('{')
            val out = LinkedHashMap<String, JsonValue>()
            skipWs()
            if (at('}')) {
                i++
                return out
            }
            while (true) {
                skipWs()
                val key = parseString()
                skipWs()
                expect(':')
                skipWs()
                out[key] = parseValue()
                skipWs()
                when {
                    at(',') -> i++
                    at('}') -> {
                        i++
                        return out
                    }
                    else -> error("object")
                }
            }
        }

        private fun parseValue(): JsonValue {
            skipWs()
            if (!remaining()) error("value")
            return when (s[i]) {
                '"' -> JsonValue.Str(parseString())
                '{' -> JsonValue.Raw(takeBalanced('{', '}'))
                '[' -> JsonValue.Raw(takeBalanced('[', ']'))
                't' -> {
                    expectWord("true")
                    JsonValue.Bool(true)
                }
                'f' -> {
                    expectWord("false")
                    JsonValue.Bool(false)
                }
                'n' -> {
                    expectWord("null")
                    JsonValue.Null
                }
                '-', in '0'..'9' -> JsonValue.Num(parseNumber())
                else -> error("value")
            }
        }

        private fun parseString(): String {
            expect('"')
            val out = StringBuilder()
            while (i < s.length) {
                when (val c = s[i++]) {
                    '"' -> return out.toString()
                    '\\' -> {
                        if (i >= s.length) error("string")
                        out.append(unescape(s[i++]))
                    }
                    else -> out.append(c)
                }
            }
            error("string")
        }

        private fun unescape(c: Char): Char = when (c) {
            '"', '\\', '/' -> c
            'b' -> '\b'
            'f' -> '\u000c'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> {
                if (i + 4 > s.length) error("unicode")
                val hex = s.substring(i, i + 4)
                i += 4
                hex.toInt(16).toChar()
            }
            else -> c
        }

        private fun parseNumber(): String {
            val start = i
            if (at('-')) i++
            if (at('0')) {
                i++
            } else {
                if (!remaining() || s[i] !in '1'..'9') error("number")
                while (remaining() && s[i] in '0'..'9') i++
            }
            if (at('.')) {
                i++
                if (!remaining() || s[i] !in '0'..'9') error("number")
                while (remaining() && s[i] in '0'..'9') i++
            }
            if (remaining() && (s[i] == 'e' || s[i] == 'E')) {
                i++
                if (remaining() && (s[i] == '+' || s[i] == '-')) i++
                if (!remaining() || s[i] !in '0'..'9') error("number")
                while (remaining() && s[i] in '0'..'9') i++
            }
            return s.substring(start, i)
        }

        private fun takeBalanced(open: Char, close: Char): String {
            val start = i
            expect(open)
            var depth = 1
            var inString = false
            var escape = false
            while (i < s.length && depth > 0) {
                val c = s[i++]
                if (inString) {
                    when {
                        escape -> escape = false
                        c == '\\' -> escape = true
                        c == '"' -> inString = false
                    }
                } else {
                    when (c) {
                        '"' -> inString = true
                        open -> depth++
                        close -> depth--
                    }
                }
            }
            if (depth != 0) error("balanced")
            return s.substring(start, i)
        }

        private fun expect(c: Char) {
            skipWs()
            if (!at(c)) error(c.toString())
            i++
        }

        private fun expectWord(word: String) {
            if (!s.startsWith(word, i)) error(word)
            i += word.length
        }

        private fun at(c: Char): Boolean = remaining() && s[i] == c

        private fun error(what: String): Nothing = throw IllegalArgumentException(what)
    }
}
