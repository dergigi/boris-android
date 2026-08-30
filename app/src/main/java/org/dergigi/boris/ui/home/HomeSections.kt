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

    val DEFAULT = listOf(CONTINUE, YOURS, FRIENDS, FOAF, OTHERS, MOST, SHORT, LONG, RANDOM)

    /** Saved order sanitized: unknown ids dropped, missing ids appended in default order. */
    fun order(saved: List<String>): List<String> {
        val known = saved.filter { it in DEFAULT }.distinct()
        return known + DEFAULT.filterNot { it in known }
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
}
