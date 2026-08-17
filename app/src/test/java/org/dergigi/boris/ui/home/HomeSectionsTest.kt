package org.dergigi.boris.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeSectionsTest {
    @Test
    fun orderDefaultsWhenEmpty() {
        assertEquals(HomeSections.DEFAULT, HomeSections.order(emptyList()))
    }

    @Test
    fun orderKeepsSavedOrderAndAppendsMissing() {
        assertEquals(
            listOf("most", "yours", "continue", "friends", "others", "random"),
            HomeSections.order(listOf("most", "yours")),
        )
    }

    @Test
    fun orderDropsUnknownIds() {
        assertEquals(
            HomeSections.DEFAULT,
            HomeSections.order(
                listOf("bogus", "continue", "yours", "friends", "others", "most", "random"),
            ),
        )
    }

    @Test
    fun orderAppendsRandomWhenUpgradingFromOlderSavedOrder() {
        assertEquals(
            listOf("continue", "yours", "friends", "others", "most", "random"),
            HomeSections.order(listOf("continue", "yours", "friends", "others", "most")),
        )
    }

    @Test
    fun moveSwapsNeighbors() {
        assertEquals(
            listOf("yours", "continue", "friends", "others", "most", "random"),
            HomeSections.move(HomeSections.DEFAULT, "yours", -1),
        )
        assertEquals(
            listOf("continue", "yours", "friends", "others", "random", "most"),
            HomeSections.move(HomeSections.DEFAULT, "random", -1),
        )
    }

    @Test
    fun moveClampsAtEdges() {
        assertEquals(HomeSections.DEFAULT, HomeSections.move(HomeSections.DEFAULT, "continue", -1))
        assertEquals(HomeSections.DEFAULT, HomeSections.move(HomeSections.DEFAULT, "random", 1))
        assertEquals(HomeSections.DEFAULT, HomeSections.move(HomeSections.DEFAULT, "bogus", 1))
    }
}
