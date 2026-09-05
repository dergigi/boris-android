package org.dergigi.boris.ui.reader

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.dergigi.boris.data.ReaderMargin

internal fun readerHorizontalPadding(margin: ReaderMargin): Dp = when (margin) {
    ReaderMargin.Compact -> 8.dp
    ReaderMargin.Default -> 20.dp
    ReaderMargin.Comfortable -> 32.dp
}
