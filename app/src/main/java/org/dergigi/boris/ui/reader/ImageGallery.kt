package org.dergigi.boris.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlin.math.abs
import kotlinx.coroutines.launch

data class ImageGalleryState(
    val urls: List<String>,
    val initialIndex: Int,
)

@Composable
fun ImageGallery(
    state: ImageGalleryState,
    onDismiss: () -> Unit,
    onPageChange: (Int) -> Unit,
) {
    val urls = state.urls
    if (urls.isEmpty()) return

    val startPage = state.initialIndex.coerceIn(0, urls.lastIndex)
    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { urls.size })
    var zoomed by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    fun goTo(page: Int) {
        if (urls.size <= 1) return
        val target = page.coerceIn(0, urls.lastIndex)
        if (target == pagerState.currentPage) return
        scope.launch { pagerState.animateScrollToPage(target) }
    }

    BackHandler(onBack = onDismiss)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(pagerState.currentPage) {
        onPageChange(pagerState.currentPage)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft, Key.NavigatePrevious -> {
                        goTo(pagerState.currentPage - 1)
                        true
                    }
                    Key.DirectionRight, Key.NavigateNext -> {
                        goTo(pagerState.currentPage + 1)
                        true
                    }
                    else -> false
                }
            },
    ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = urls.size > 1 && !zoomed,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                ZoomableImage(
                    url = urls[page],
                    isCurrent = page == pagerState.currentPage,
                    onZoomedChange = { zoomed = it },
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .statusBarsPadding()
                    .align(Alignment.TopStart)
                    .padding(4.dp),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                )
            }
            if (urls.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${urls.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                )
            }
    }
}

@Composable
private fun ZoomableImage(
    url: String,
    isCurrent: Boolean,
    onZoomedChange: (Boolean) -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var lastTapAt by remember { mutableLongStateOf(0L) }

    fun applyScale(next: Float, nextOffset: Offset = offset) {
        scale = next.coerceIn(1f, 5f)
        offset = if (scale <= 1.01f) {
            scale = 1f
            Offset.Zero
        } else {
            nextOffset
        }
        onZoomedChange(scale > 1.01f)
    }

    LaunchedEffect(isCurrent) {
        if (!isCurrent) applyScale(1f)
    }

    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(url) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val startedAt = System.currentTimeMillis()
                    var pinching = false
                    var dragged = false
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.count { it.pressed }
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        if (pressed >= 2) pinching = true
                        if (abs(panChange.x) > 1f || abs(panChange.y) > 1f) dragged = true

                        val currentlyZoomed = scale > 1.01f
                        if (pinching || currentlyZoomed) {
                            event.changes.forEach { change ->
                                if (change.positionChanged()) change.consume()
                            }
                            val next = (scale * zoomChange).coerceIn(1f, 5f)
                            applyScale(next, offset + panChange)
                        }
                    } while (event.changes.any { it.pressed })

                    if (pinching && scale < 1.05f) applyScale(1f)

                    val wasTap = !pinching && !dragged &&
                        System.currentTimeMillis() - startedAt < 300
                    if (wasTap) {
                        if (startedAt - lastTapAt < 300) {
                            lastTapAt = 0L
                            if (scale > 1.01f) applyScale(1f) else applyScale(2.5f)
                        } else {
                            lastTapAt = startedAt
                        }
                    }
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
    )
}
