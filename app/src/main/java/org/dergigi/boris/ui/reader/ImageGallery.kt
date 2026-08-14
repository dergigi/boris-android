package org.dergigi.boris.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage

data class ImageGalleryState(
    val urls: List<String>,
    val initialIndex: Int,
)

@Composable
fun ImageGalleryDialog(
    state: ImageGalleryState,
    onDismiss: () -> Unit,
) {
    val urls = state.urls
    if (urls.isEmpty()) return

    val startPage = state.initialIndex.coerceIn(0, urls.lastIndex)
    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { urls.size })
    var zoomed by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
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
}

@Composable
private fun ZoomableImage(
    url: String,
    isCurrent: Boolean,
    onZoomedChange: (Boolean) -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    fun applyScale(next: Float, nextOffset: Offset = offset) {
        scale = next.coerceIn(1f, 5f)
        offset = if (scale == 1f) Offset.Zero else nextOffset
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
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.01f) applyScale(1f) else applyScale(2.5f)
                    },
                )
            }
            .pointerInput(url) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val next = (scale * zoom).coerceIn(1f, 5f)
                    applyScale(next, if (next == 1f) Offset.Zero else offset + pan)
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
