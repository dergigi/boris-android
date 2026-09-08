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
            listOf("yours", "continue", "short", "long", "random"),
            HomeSections.order(listOf("most", "yours")),
        )
    }

    @Test
    fun orderDropsUnknownIds() {
        assertEquals(
            listOf("continue", "yours", "short", "long", "random"),
            HomeSections.order(
                listOf("bogus", "continue", "yours", "friends", "others", "most", "short", "long", "random"),
            ),
        )
    }

    @Test
    fun orderAppendsNewSectionsWhenUpgradingFromOlderSavedOrder() {
        assertEquals(
            listOf("continue", "yours", "short", "long", "random"),
            HomeSections.order(listOf("continue", "yours", "friends", "others", "most")),
        )
        assertEquals(
            listOf("continue", "yours", "random", "short", "long"),
            HomeSections.order(listOf("continue", "yours", "friends", "others", "most", "random")),
        )
    }

    @Test
    fun exploreOrderKeepsDiscoverySections() {
        assertEquals(
            listOf(
                "most",
                "friends",
                "liked_friends",
                "read_friends",
                "foaf",
                "liked_foaf",
                "read_foaf",
                "others",
                "liked_others",
                "read_others",
            ),
            HomeSections.exploreOrder(listOf("continue", "most", "friends")),
        )
    }

    @Test
    fun exploreOrderPrefersDedicatedListThenFallsBackToHome() {
        assertEquals(
            HomeSections.EXPLORE_DEFAULT,
            HomeSections.exploreOrder(emptyList(), emptyList()),
        )
        assertEquals(
            listOf(
                "most",
                "friends",
                "liked_friends",
                "read_friends",
                "foaf",
                "liked_foaf",
                "read_foaf",
                "others",
                "liked_others",
                "read_others",
            ),
            HomeSections.exploreOrder(emptyList(), listOf("continue", "most", "friends")),
        )
        assertEquals(
            listOf(
                "others",
                "foaf",
                "friends",
                "liked_friends",
                "read_friends",
                "liked_foaf",
                "read_foaf",
                "liked_others",
                "read_others",
                "most",
            ),
            HomeSections.exploreOrder(listOf("others", "foaf"), listOf("most", "friends")),
        )
    }

    @Test
    fun hiddenDropsUnknownIds() {
        assertEquals(
            listOf("random", "continue"),
            HomeSections.hidden(listOf("bogus", "random", "friends", "continue", "random"), HomeSections.DEFAULT),
        )
    }

    @Test
    fun visibleFiltersHiddenIds() {
        assertEquals(
            listOf("continue", "long", "yours"),
            HomeSections.visible(HomeSections.DEFAULT, listOf("short", "random", "friends")),
        )
        assertEquals(HomeSections.DEFAULT, HomeSections.visible(HomeSections.DEFAULT, emptyList()))
    }

    @Test
    fun toggleHiddenAddsAndRemovesKnownIds() {
        assertEquals(
            listOf("random"),
            HomeSections.toggleHidden(emptyList(), "random", HomeSections.DEFAULT),
        )
        assertEquals(
            emptyList<String>(),
            HomeSections.toggleHidden(listOf("random"), "random", HomeSections.DEFAULT),
        )
        assertEquals(
            listOf("random"),
            HomeSections.toggleHidden(listOf("random", "friends"), "bogus", HomeSections.DEFAULT),
        )
    }

    @Test
    fun moveSwapsNeighbors() {
        assertEquals(
            listOf("continue", "short", "long", "yours", "random"),
            HomeSections.move(HomeSections.DEFAULT, "yours", -1),
        )
        assertEquals(
            listOf("continue", "short", "random", "long", "yours"),
            HomeSections.move(HomeSections.DEFAULT, "random", -1),
        )
    }

    @Test
    fun moveClampsAtEdges() {
        assertEquals(HomeSections.DEFAULT, HomeSections.move(HomeSections.DEFAULT, "continue", -1))
        assertEquals(HomeSections.DEFAULT, HomeSections.move(HomeSections.DEFAULT, "yours", 1))
        assertEquals(HomeSections.DEFAULT, HomeSections.move(HomeSections.DEFAULT, "bogus", 1))
    }
}
