package org.dergigi.boris.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dergigi.boris.R
import org.dergigi.boris.data.ContinueReading
import org.dergigi.boris.data.NostrLink
import org.dergigi.boris.data.ReadingPositionStore
import org.dergigi.boris.data.XcancelLink

@Composable
fun ArticleActionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    title: String?,
    url: String,
    loggedIn: Boolean,
    archived: Boolean,
    onListen: () -> Unit,
    onMarkAsRead: () -> Unit,
) {
    val context = LocalContext.current
    val nativeUri = remember(url) { NostrLink.parse(url)?.uri }
    val progressVersion by ReadingPositionStore.version.collectAsStateWithLifecycle()
    val fraction = remember(url, progressVersion) {
        ReadingPositionStore.fraction(url)
    }
    val continueListening = ContinueReading.inProgress(fraction)
    val hasProgress = fraction > 0f
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.reader_share)) },
            leadingIcon = {
                Icon(Icons.Filled.Share, contentDescription = null)
            },
            onClick = {
                onDismiss()
                shareArticleLink(context, title, url)
            },
        )
        ArticleCopyMenuItems(url = url, onDismiss = onDismiss)
        DropdownMenuItem(
            text = { Text(stringResource(R.string.reader_open_original)) },
            leadingIcon = {
                Icon(Icons.Filled.OpenInBrowser, contentDescription = null)
            },
            onClick = {
                onDismiss()
                openOriginalArticle(context, url)
            },
        )
        if (nativeUri != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reader_open_native)) },
                leadingIcon = {
                    Icon(Icons.Outlined.Smartphone, contentDescription = null)
                },
                onClick = {
                    onDismiss()
                    openExternalUri(context, nativeUri)
                },
            )
        }
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(
                        if (continueListening) {
                            R.string.home_continue_listening
                        } else {
                            R.string.home_start_listening
                        },
                    ),
                )
            },
            leadingIcon = {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
            },
            onClick = {
                onDismiss()
                onListen()
            },
        )
        if (hasProgress) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reader_reset_progress)) },
                leadingIcon = {
                    Icon(Icons.Outlined.RestartAlt, contentDescription = null)
                },
                onClick = {
                    onDismiss()
                    ReadingPositionStore.reset(url)
                },
            )
        }
        if (loggedIn && !archived) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.reader_mark_as_read)) },
                leadingIcon = {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                },
                onClick = {
                    onDismiss()
                    onMarkAsRead()
                },
            )
        }
    }
}

fun hasAlternateCopyLinks(url: String): Boolean =
    NostrLink.njumpCopyUrl(url) != null || XcancelLink.copyUrl(url) != null

@Composable
fun ArticleCopyMenuItems(
    url: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val njumpUrl = remember(url) { NostrLink.njumpCopyUrl(url) }
    val xcancelUrl = remember(url) { XcancelLink.copyUrl(url) }
    DropdownMenuItem(
        text = { Text(stringResource(R.string.reader_copy_link)) },
        leadingIcon = {
            Icon(Icons.Filled.ContentCopy, contentDescription = null)
        },
        onClick = {
            onDismiss()
            copyArticleLink(context, clipboard, url)
        },
    )
    if (njumpUrl != null) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.reader_copy_njump_link)) },
            leadingIcon = {
                Icon(Icons.Filled.ContentCopy, contentDescription = null)
            },
            onClick = {
                onDismiss()
                copyPlainLink(context, clipboard, njumpUrl)
            },
        )
    }
    if (xcancelUrl != null) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.reader_copy_xcancel_link)) },
            leadingIcon = {
                Icon(Icons.Filled.ContentCopy, contentDescription = null)
            },
            onClick = {
                onDismiss()
                copyPlainLink(context, clipboard, xcancelUrl)
            },
        )
    }
}
