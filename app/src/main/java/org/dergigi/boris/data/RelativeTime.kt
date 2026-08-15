package org.dergigi.boris.data

object RelativeTime {
    fun label(epochSeconds: Long, nowSeconds: Long = System.currentTimeMillis() / 1000): String {
        val delta = (nowSeconds - epochSeconds).coerceAtLeast(0)
        return when {
            delta < 60 -> "now"
            delta < 3_600 -> "${delta / 60}m"
            delta < 86_400 -> "${delta / 3_600}h"
            delta < 2_592_000 -> "${delta / 86_400}d"
            delta < 31_536_000 -> "${delta / 2_592_000}mo"
            else -> "${delta / 31_536_000}y"
        }
    }
}
