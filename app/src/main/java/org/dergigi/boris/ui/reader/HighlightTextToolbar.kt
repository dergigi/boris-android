package org.dergigi.boris.ui.reader

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.AnnotatedString
import org.dergigi.boris.R
import kotlin.math.roundToInt

class HighlightTextToolbar(
    private val view: View,
    private val showHighlight: Boolean,
    private val clipboard: ClipboardManager,
    private val onHighlight: (String) -> Unit,
) : TextToolbar {
    private var actionMode: ActionMode? = null
    private val contentRect = android.graphics.Rect()
    private var copyAction: (() -> Unit)? = null
    private var selectAllAction: (() -> Unit)? = null

    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    override fun hide() {
        status = TextToolbarStatus.Hidden
        actionMode?.finish()
        actionMode = null
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        copyAction = onCopyRequested
        selectAllAction = onSelectAllRequested
        contentRect.set(
            rect.left.roundToInt(),
            rect.top.roundToInt(),
            rect.right.roundToInt().coerceAtLeast(rect.left.roundToInt() + 1),
            rect.bottom.roundToInt().coerceAtLeast(rect.top.roundToInt() + 1),
        )
        if (actionMode != null) {
            actionMode?.invalidateContentRect()
            return
        }
        val callback = object : ActionMode.Callback2() {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                if (copyAction != null) {
                    menu.add(0, MENU_COPY, 0, android.R.string.copy)
                }
                if (showHighlight) {
                    menu.add(0, MENU_HIGHLIGHT, 1, view.context.getString(R.string.highlight_action))
                }
                if (selectAllAction != null) {
                    menu.add(0, MENU_SELECT_ALL, 2, android.R.string.selectAll)
                }
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                when (item.itemId) {
                    MENU_COPY -> {
                        copyAction?.invoke()
                        mode.finish()
                    }
                    MENU_HIGHLIGHT -> {
                        val prior = clipboard.getText()
                        copyAction?.invoke()
                        val quote = clipboard.getText()?.text.orEmpty()
                        if (prior != null) {
                            clipboard.setText(prior)
                        } else {
                            clipboard.setText(AnnotatedString(""))
                        }
                        onHighlight(quote)
                        mode.finish()
                    }
                    MENU_SELECT_ALL -> selectAllAction?.invoke()
                }
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                actionMode = null
                status = TextToolbarStatus.Hidden
            }

            override fun onGetContentRect(mode: ActionMode, view: View?, outRect: android.graphics.Rect) {
                outRect.set(contentRect)
            }
        }
        actionMode = view.startActionMode(callback, ActionMode.TYPE_FLOATING)
        status = if (actionMode != null) TextToolbarStatus.Shown else TextToolbarStatus.Hidden
    }

    private companion object {
        const val MENU_COPY = 1
        const val MENU_HIGHLIGHT = 2
        const val MENU_SELECT_ALL = 3
    }
}
