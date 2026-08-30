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
            listOf("most", "yours", "continue", "friends", "foaf", "others", "short", "long", "random"),
            HomeSections.order(listOf("most", "yours")),
        )
    }

    @Test
    fun orderDropsUnknownIds() {
        assertEquals(
            listOf("continue", "yours", "friends", "others", "most", "short", "long", "random", "foaf"),
            HomeSections.order(
                listOf("bogus", "continue", "yours", "friends", "others", "most", "short", "long", "random"),
            ),
        )
    }

    @Test
    fun orderAppendsNewSectionsWhenUpgradingFromOlderSavedOrder() {
        assertEquals(
            listOf("continue", "yours", "friends", "others", "most", "foaf", "short", "long", "random"),
            HomeSections.order(listOf("continue", "yours", "friends", "others", "most")),
        )
        assertEquals(
            listOf("continue", "yours", "friends", "others", "most", "random", "foaf", "short", "long"),
            HomeSections.order(listOf("continue", "yours", "friends", "others", "most", "random")),
        )
    }

    @Test
    fun moveSwapsNeighbors() {
        assertEquals(
            listOf("yours", "continue", "friends", "foaf", "others", "most", "short", "long", "random"),
            HomeSections.move(HomeSections.DEFAULT, "yours", -1),
        )
        assertEquals(
            listOf("continue", "yours", "friends", "foaf", "others", "most", "short", "random", "long"),
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
