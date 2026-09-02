package org.dergigi.boris.ui.reader

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.dergigi.boris.R
import org.dergigi.boris.data.ReadingPositionStore
import org.dergigi.boris.ui.ArticleCopyMenuItems
import org.dergigi.boris.ui.hasAlternateCopyLinks

internal enum class ReaderOverflowPage { Root, Copy, Open }

@Composable
internal fun ReaderOverflowBackItem(
    label: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.reader_menu_back),
            )
        },
        onClick = onClick,
    )
    HorizontalDivider()
}

@Composable
internal fun ReaderOverflowMenu(
    articleUrl: String,
    state: ReaderUiState,
    pane: ReaderPaneState,
    loggedIn: Boolean,
    archived: Boolean,
    galleryUrls: List<String>,
    nativeUri: String?,
    canOpenArchive: Boolean,
    scrollState: ScrollState,
    onRefresh: () -> Unit,
    onArchive: (Boolean) -> Unit,
    onOpenGallery: (List<String>, Int) -> Unit,
    onOpenReaderSettings: () -> Unit,
    onShare: () -> Unit,
    onOpenOriginal: () -> Unit,
    onOpenNative: () -> Unit,
    onOpenWayback: () -> Unit,
    onOpenArchivePh: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    var menuPage by remember { mutableStateOf(ReaderOverflowPage.Root) }
    fun dismissMenu() {
        menuOpen = false
        menuPage = ReaderOverflowPage.Root
    }
    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More")
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { dismissMenu() },
        ) {
            // Read inside the menu so progress saves (which
            // bump the store version on every scroll settle)
            // do not recompose the whole scaffold.
            val progressVersion by ReadingPositionStore.version
                .collectAsStateWithLifecycle()
            val hasProgress = remember(articleUrl, progressVersion) {
                ReadingPositionStore.fraction(articleUrl) > 0f
            }
            val copyHasExtras = remember(articleUrl) {
                hasAlternateCopyLinks(articleUrl)
            }
            val articleReady = state is ReaderUiState.Ready
            val showArticleActions = articleReady || hasProgress
            val showMarkAsRead =
                loggedIn && articleReady && !archived
            when (menuPage) {
                ReaderOverflowPage.Root -> {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.reader_share)) },
                        leadingIcon = {
                            Icon(Icons.Filled.Share, contentDescription = null)
                        },
                        onClick = {
                            dismissMenu()
                            onShare()
                        },
                    )
                    if (copyHasExtras) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reader_copy)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    contentDescription = null,
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                menuPage = ReaderOverflowPage.Copy
                            },
                        )
                    } else {
                        ArticleCopyMenuItems(
                            url = articleUrl,
                            onDismiss = { dismissMenu() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.reader_open)) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Language,
                                contentDescription = null,
                            )
                        },
                        trailingIcon = {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            menuPage = ReaderOverflowPage.Open
                        },
                    )
                    if (articleReady) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reader_find)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Search,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                dismissMenu()
                                pane.openFind()
                            },
                        )
                        if (galleryUrls.isNotEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.reader_open_gallery))
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.PhotoLibrary,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    dismissMenu()
                                    onOpenGallery(galleryUrls, 0)
                                },
                            )
                        }
                    }
                    if (showArticleActions) {
                        HorizontalDivider()
                        if (articleReady) {
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.reader_refresh))
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Refresh,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    dismissMenu()
                                    onRefresh()
                                },
                            )
                        }
                        if (hasProgress) {
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.reader_reset_progress))
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.RestartAlt,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    dismissMenu()
                                    ReadingPositionStore.reset(articleUrl)
                                    scope.launch {
                                        scrollState.animateScrollTo(0)
                                    }
                                },
                            )
                        }
                        if (showMarkAsRead) {
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.reader_mark_as_read))
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    dismissMenu()
                                    onArchive(false)
                                },
                            )
                        }
                    }
                    if (loggedIn) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(R.string.reader_settings))
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Settings,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                dismissMenu()
                                onOpenReaderSettings()
                            },
                        )
                    }
                }
                ReaderOverflowPage.Copy -> {
                    ReaderOverflowBackItem(
                        label = stringResource(R.string.reader_copy),
                        onClick = { menuPage = ReaderOverflowPage.Root },
                    )
                    ArticleCopyMenuItems(
                        url = articleUrl,
                        onDismiss = { dismissMenu() },
                    )
                }
                ReaderOverflowPage.Open -> {
                    ReaderOverflowBackItem(
                        label = stringResource(R.string.reader_open),
                        onClick = { menuPage = ReaderOverflowPage.Root },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.reader_open_in_browser))
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Language,
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            dismissMenu()
                            onOpenOriginal()
                        },
                    )
                    if (nativeUri != null) {
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(R.string.reader_open_native))
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Smartphone,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                dismissMenu()
                                onOpenNative()
                            },
                        )
                    }
                    if (canOpenArchive) {
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(R.string.reader_open_wayback))
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.History,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                dismissMenu()
                                onOpenWayback()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(R.string.reader_open_archive_ph))
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Public,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                dismissMenu()
                                onOpenArchivePh()
                            },
                        )
                    }
                }
            }
        }
    }
}
