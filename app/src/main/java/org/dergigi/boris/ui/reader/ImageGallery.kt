package org.dergigi.boris.ui.reader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dergigi.boris.data.ImageStore
import org.dergigi.boris.data.UrlExtractor


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
    val context = LocalContext.current
    var zoomed by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    var pendingStorageAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val storagePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val action = pendingStorageAction
        pendingStorageAction = null
        if (granted) action?.invoke()
        else Toast.makeText(context, "Need storage access to save images", Toast.LENGTH_SHORT).show()
    }

    fun withStorage(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= 29 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            action()
        } else {
            pendingStorageAction = action
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    fun currentUrl(): String = urls[pagerState.currentPage]

    fun downloadCurrent() {
        if (busy) return
        withStorage {
            scope.launch {
                busy = true
                val ok = withContext(Dispatchers.IO) {
                    runCatching { ImageStore.save(context, currentUrl(), pagerState.currentPage) }.isSuccess
                }
                busy = false
                Toast.makeText(
                    context,
                    if (ok) "Saved to Pictures" else "Couldn't save this image",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    fun downloadAll() {
        if (busy) return
        withStorage {
            scope.launch {
                busy = true
                val saved = withContext(Dispatchers.IO) { ImageStore.saveAll(context, urls) }
                busy = false
                Toast.makeText(
                    context,
                    if (saved == 0) "Couldn't save images"
                    else if (saved == urls.size) "Saved $saved images"
                    else "Saved $saved of ${urls.size} images",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    fun shareCurrent() {
        if (busy) return
        scope.launch {
            busy = true
            val intent = withContext(Dispatchers.IO) {
                runCatching { ImageStore.shareIntent(context, currentUrl(), pagerState.currentPage) }.getOrNull()
            }
            busy = false
            if (intent == null) {
                Toast.makeText(context, "Couldn't share this image", Toast.LENGTH_SHORT).show()
            } else {
                context.startActivity(Intent.createChooser(intent, "Share image"))
            }
        }
    }

    fun openCurrentUrl() {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl())))
        }.onFailure {
            Toast.makeText(context, "Couldn't open this URL", Toast.LENGTH_SHORT).show()
        }
    }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    }
                    IconButton(onClick = ::downloadCurrent, enabled = !busy) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = "Download",
                            tint = Color.White,
                        )
                    }
                    IconButton(onClick = ::shareCurrent, enabled = !busy) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                        )
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }, enabled = !busy) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "More",
                                tint = Color.White,
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Download all") },
                                enabled = !busy,
                                onClick = {
                                    menuOpen = false
                                    downloadAll()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Open URL") },
                                onClick = {
                                    menuOpen = false
                                    openCurrentUrl()
                                },
                            )
                        }
                    }
                }
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
        model = UrlExtractor.preferHttps(url),
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
