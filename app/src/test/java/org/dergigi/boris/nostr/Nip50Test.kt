package org.dergigi.boris.nostr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Nip50Test {
    @Test
    fun searchFilterUsesNip50SearchField() {
        val filter = RelayQuery.nip50SearchFilter("  nostr   search  ", limit = 25)!!
        val kinds = filter.getJSONArray("kinds")

        assertEquals("nostr search", filter.getString("search"))
        assertEquals(25, filter.getInt("limit"))
        assertEquals(4, kinds.length())
        assertEquals(Nip01Event.KIND_METADATA, kinds.getInt(0))
        assertEquals(Nip01Event.KIND_HIGHLIGHT, kinds.getInt(1))
        assertEquals(Nip01Event.KIND_LONG_FORM, kinds.getInt(2))
        assertEquals(Nip01Event.KIND_WEB_BOOKMARK, kinds.getInt(3))
    }

    @Test
    fun searchFilterIgnoresTooShortQueries() {
        assertNull(RelayQuery.nip50SearchFilter(" n "))
    }

    @Test
    fun searchFilterCapsLimit() {
        val filter = RelayQuery.nip50SearchFilter("nostr", limit = 500)!!
        assertEquals(80, filter.getInt("limit"))
    }
}
