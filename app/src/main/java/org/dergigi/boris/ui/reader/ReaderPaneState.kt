package org.dergigi.boris.ui.reader

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
class ReaderPaneState {
    var findOpen by mutableStateOf(false)
    var outlineOpen by mutableStateOf(false)
    var highlightsOpen by mutableStateOf(false)

    fun openFind() {
        findOpen = true
        outlineOpen = false
        highlightsOpen = false
    }

    fun openOutline() {
        findOpen = false
        outlineOpen = true
        highlightsOpen = false
    }

    fun openHighlights() {
        highlightsOpen = true
    }

    fun closeFind() {
        findOpen = false
    }

    fun closeOutline() {
        outlineOpen = false
    }

    fun closeHighlights() {
        highlightsOpen = false
    }

    fun onArticleChanged() {
        outlineOpen = false
        highlightsOpen = false
    }
}
