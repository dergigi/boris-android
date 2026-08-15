package org.dergigi.boris.ui.reader

import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownImagesTest {
    @Test
    fun fullWidthScalesASmallImageUpToTheColumn() {
        val box = markdownImageBox(
            container = Size(1080f, 2000f),
            intrinsic = Size(400f, 200f),
            fullWidth = true,
            maxHeightPx = 1400f,
        )
        assertEquals(1080f, box.width)
        assertEquals(540f, box.height)
    }

    @Test
    fun fullWidthScalesAWideImageDownToTheColumn() {
        val box = markdownImageBox(
            container = Size(1080f, 2000f),
            intrinsic = Size(2160f, 1080f),
            fullWidth = true,
            maxHeightPx = 1400f,
        )
        assertEquals(1080f, box.width)
        assertEquals(540f, box.height)
    }

    @Test
    fun containedKeepsIntrinsicSizeWhenItFits() {
        val box = markdownImageBox(
            container = Size(1080f, 2000f),
            intrinsic = Size(400f, 200f),
            fullWidth = false,
            maxHeightPx = 1400f,
        )
        assertEquals(400f, box.width)
        assertEquals(200f, box.height)
    }

    @Test
    fun containedCapsHeightAtSeventyPercent() {
        val box = markdownImageBox(
            container = Size(1080f, 2000f),
            intrinsic = Size(800f, 2000f),
            fullWidth = false,
            maxHeightPx = 1400f,
        )
        assertEquals(560f, box.width, 0.01f)
        assertEquals(1400f, box.height, 0.01f)
    }
}
