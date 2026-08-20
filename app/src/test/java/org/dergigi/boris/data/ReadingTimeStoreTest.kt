package org.dergigi.boris.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingTimeStoreTest {
    @After
    fun tearDown() {
        ReadingTimeStore.clear()
    }

    @Test
    fun putAndGet() {
        ReadingTimeStore.clear()
        ReadingTimeStore.put("https://example.com/post", 12)
        assertEquals(12, ReadingTimeStore.get("https://example.com/post"))
        assertNull(ReadingTimeStore.get("https://example.com/other"))
    }

    @Test
    fun ignoresNonPositiveMinutes() {
        ReadingTimeStore.clear()
        ReadingTimeStore.put("https://example.com/post", 0)
        assertNull(ReadingTimeStore.get("https://example.com/post"))
    }

    @Test
    fun matchesNormalizedAlias() {
        ReadingTimeStore.clear()
        ReadingTimeStore.put("https://www.example.com/post?utm_source=x", 7)
        assertEquals(7, ReadingTimeStore.get("https://example.com/post"))
    }
}
