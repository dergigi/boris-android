package org.dergigi.boris.ui.home

object HomeSections {
    const val CONTINUE = "continue"
    const val YOURS = "yours"
    const val FRIENDS = "friends"
    const val FOAF = "foaf"
    const val OTHERS = "others"
    const val MOST = "most"
    const val SHORT = "short"
    const val LONG = "long"
    const val RANDOM = "random"

    val DEFAULT = listOf(CONTINUE, SHORT, LONG, RANDOM, YOURS)
    val EXPLORE_DEFAULT = listOf(FRIENDS, FOAF, OTHERS, MOST)

    /** Saved order sanitized: unknown ids dropped, missing ids appended in default order. */
    fun order(saved: List<String>): List<String> {
        return orderFor(saved, DEFAULT)
    }

    /** Discovery order for sections that moved from Home to Explore. */
    fun exploreOrder(saved: List<String>): List<String> {
        return orderFor(saved, EXPLORE_DEFAULT)
    }

    /**
     * Prefer a dedicated Explore order. If that list is empty, reuse any
     * discovery ids still sitting in the older Home order.
     */
    fun exploreOrder(exploreSaved: List<String>, homeSaved: List<String>): List<String> {
        return exploreOrder(exploreSaved.ifEmpty { homeSaved })
    }

    fun move(order: List<String>, id: String, delta: Int): List<String> {
        val from = order.indexOf(id)
        if (from < 0) return order
        val to = from + delta
        if (to !in order.indices) return order
        val out = order.toMutableList()
        out.removeAt(from)
        out.add(to, id)
        return out
    }

    private fun orderFor(saved: List<String>, default: List<String>): List<String> {
        val known = saved.filter { it in default }.distinct()
        return known + default.filterNot { it in known }
    }
}
