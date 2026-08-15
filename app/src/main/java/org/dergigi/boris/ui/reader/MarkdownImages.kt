package org.dergigi.boris.ui.reader

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified

internal fun markdownImageBox(
    container: Size,
    intrinsic: Size,
    fullWidth: Boolean,
    maxHeightPx: Float,
): Size {
    val fallback = Size(180f, 180f)
    val hasContainer = !container.isUnspecified && container.width > 0f
    val hasIntrinsic = !intrinsic.isUnspecified && intrinsic.width > 0f && intrinsic.height > 0f
    if (!hasContainer && !hasIntrinsic) return fallback
    if (!hasIntrinsic) {
        val width = if (hasContainer) container.width else fallback.width
        return Size(width, fallback.height)
    }
    val srcW = intrinsic.width
    val srcH = intrinsic.height
    if (fullWidth) {
        val width = if (hasContainer) container.width else srcW
        return Size(width, srcH * (width / srcW))
    }
    val maxWidth = if (hasContainer) container.width else srcW
    val scale = minOf(1f, maxWidth / srcW, maxHeightPx / srcH)
    return Size(srcW * scale, srcH * scale)
}
