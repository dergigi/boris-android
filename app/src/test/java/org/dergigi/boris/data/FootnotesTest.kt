package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FootnotesTest {
    @Test
    fun leavesPlainMarkdownAlone() {
        val src = "Just a paragraph.\n\nAnd another."
        assertEquals(src, Footnotes.expand(src))
    }

    @Test
    fun expandsNumberedAndNamedFootnotes() {
        val src = """
            Here's a simple footnote,[^1] and here's a longer one.[^bignote]

            [^1]: This is the first footnote.

            [^bignote]: Here's one with multiple paragraphs and code.

                Indent paragraphs to include them in the footnote.

                Add as many paragraphs as you like.
        """.trimIndent()
        val out = Footnotes.expand(src)
        assertTrue(out.contains("Here's a simple footnote,¹ and here's a longer one.²"))
        assertFalse(out.contains("[^1]"))
        assertFalse(out.contains("[^bignote]"))
        assertTrue(out.contains("1. This is the first footnote."))
        assertTrue(out.contains("2. Here's one with multiple paragraphs and code."))
        assertTrue(out.contains("    Indent paragraphs to include them in the footnote."))
    }

    @Test
    fun numbersByFirstReferenceAndReusesTheSameMark() {
        val src = """
            First[^b] then[^a] then[^b] again.

            [^a]: Alpha
            [^b]: Bravo
        """.trimIndent()
        val out = Footnotes.expand(src)
        assertTrue(out.contains("First¹ then² then¹ again."))
        assertTrue(out.contains("1. Bravo"))
        assertTrue(out.contains("2. Alpha"))
    }

    @Test
    fun keepsUnknownReferencesAndDropsUnusedDefinitions() {
        val src = """
            Known[^1] and missing[^ghost].

            [^1]: Present
            [^unused]: Never referenced
        """.trimIndent()
        val out = Footnotes.expand(src)
        assertTrue(out.contains("Known¹ and missing[^ghost]."))
        assertTrue(out.contains("1. Present"))
        assertFalse(out.contains("Never referenced"))
    }

    @Test
    fun doesNotTouchFootnotesInsideCode() {
        val src = """
            Real one.[^1]

            ```
            Example[^1]
            [^1]: not a definition
            ```

            Inline `[^1]` stays.

            [^1]: Actual note
        """.trimIndent()
        val out = Footnotes.expand(src)
        assertTrue(out.contains("Real one.¹"))
        assertTrue(out.contains("Example[^1]"))
        assertTrue(out.contains("[^1]: not a definition"))
        assertTrue(out.contains("Inline `[^1]` stays."))
        assertTrue(out.contains("1. Actual note"))
    }
}
