package org.dergigi.boris.ui.reader

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import org.dergigi.boris.R

@Composable
fun HighlightTextToolbar(
    selection: ReaderSelectionState,
    showHighlight: Boolean,
    onCopy: () -> Unit,
    onHighlight: () -> Unit,
    onTtsFromHere: () -> Unit,
    onSelectAll: () -> Unit,
) {
    if (!selection.toolbarReady) return
    val rect = selection.toolbarRect
    val position = remember(rect) { SelectionMenuPosition(rect) }
    val maxWidth = (LocalConfiguration.current.screenWidthDp - 16).dp
    Popup(
        popupPositionProvider = position,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            clippingEnabled = false,
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            shadowElevation = 6.dp,
            modifier = Modifier.widthIn(max = maxWidth),
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp),
            ) {
                ToolbarAction(
                    label = stringResource(android.R.string.copy),
                    onClick = onCopy,
                )
                if (showHighlight) {
                    ToolbarAction(
                        label = stringResource(R.string.highlight_action),
                        onClick = onHighlight,
                    )
                }
                ToolbarAction(
                    label = stringResource(R.string.tts_from_here),
                    onClick = onTtsFromHere,
                )
                ToolbarAction(
                    label = stringResource(android.R.string.selectAll),
                    onClick = onSelectAll,
                )
            }
        }
    }
}

@Composable
private fun ToolbarAction(
    label: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label)
    }
}

private class SelectionMenuPosition(
    private val selection: Rect,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val margin = 8
        val maxX = (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)
        val x = (selection.center.x - popupContentSize.width / 2f).toInt().coerceIn(margin, maxX)
        val above = (selection.top - popupContentSize.height - 8).toInt()
        val y = if (above >= margin) {
            above
        } else {
            val maxY = (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin)
            (selection.bottom + 8).toInt().coerceIn(margin, maxY)
        }
        return IntOffset(x, y)
    }
}
