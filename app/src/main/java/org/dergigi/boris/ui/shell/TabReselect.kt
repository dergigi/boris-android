package org.dergigi.boris.ui.shell

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Tapping the already-selected bottom tab scrolls that screen back to the top (#215). */
object TabReselect {
    private val _events = MutableSharedFlow<MainTab>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun emit(tab: MainTab) {
        _events.tryEmit(tab)
    }
}

@Composable
fun ScrollToTopOnTabReselect(
    tab: MainTab,
    listState: LazyListState? = null,
    scrollState: ScrollState? = null,
) {
    LaunchedEffect(tab, listState, scrollState) {
        TabReselect.events.collect { requested ->
            if (requested != tab) return@collect
            listState?.animateScrollToItem(0)
            scrollState?.animateScrollTo(0)
        }
    }
}
