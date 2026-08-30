package org.dergigi.boris.data

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DisplayTypeStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun missingFileDefaultsToColor() {
        DisplayTypeStore.init(folder.newFile("display-missing"))
        assertEquals(DisplayType.Color, DisplayTypeStore.type.value)
    }

    @Test
    fun einkPersistsAcrossInit() {
        val file = folder.newFile("display-type")
        DisplayTypeStore.init(file)
        DisplayTypeStore.set(DisplayType.Eink)
        assertEquals(DisplayType.Eink, DisplayTypeStore.type.value)
        DisplayTypeStore.init(file)
        assertEquals(DisplayType.Eink, DisplayTypeStore.type.value)
    }

    @Test
    fun resetReturnsToColor() {
        val file = folder.newFile("display-reset")
        DisplayTypeStore.init(file)
        DisplayTypeStore.set(DisplayType.Eink)
        DisplayTypeStore.reset()
        assertEquals(DisplayType.Color, DisplayTypeStore.type.value)
        DisplayTypeStore.init(file)
        assertEquals(DisplayType.Color, DisplayTypeStore.type.value)
    }

    @Test
    fun unknownIdIsColor() {
        assertEquals(DisplayType.Color, DisplayType.fromId("neon"))
        assertEquals(DisplayType.Color, DisplayType.fromId(null))
        assertEquals(DisplayType.Eink, DisplayType.fromId("eink"))
    }
}
