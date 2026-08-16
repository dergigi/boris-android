package org.dergigi.boris.ui.library

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import org.dergigi.boris.R
import org.dergigi.boris.data.BookmarkBucket
import org.dergigi.boris.data.BookmarkItem
import org.dergigi.boris.data.BookmarkShelves
import org.dergigi.boris.ui.ArticleRow
import org.dergigi.boris.ui.TopBarMenuItem
import org.dergigi.boris.ui.TopBarMoreMenu
import org.dergigi.boris.ui.auth.AuthBar
import org.dergigi.boris.ui.auth.AuthUiState
import org.dergigi.boris.ui.auth.AuthViewModel
import org.dergigi.boris.ui.auth.NstartFooter
import org.dergigi.boris.ui.theme.BorisIcons

@Composable
fun LibraryScreen(
    onOpenArticle: (String) -> Unit,
    onOpenLibrarySettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val bucket by viewModel.bucket.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val authMessage by authViewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var bunkerUri by rememberSaveable { mutableStateOf("") }
    val decryptLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.onDecryptResult(result.resultCode, result.data)
    }
    val authLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        authViewModel.onSignerResult(result.resultCode, result.data)
    }
    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        viewModel.consumeMessage()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
        authViewModel.refresh()
    }
    LibraryScreenContent(
        state = state,
        refreshing = refreshing,
        bucket = bucket,
        authState = authState,
        authMessage = authMessage,
        bunkerUri = bunkerUri,
        onBunkerUriChange = { bunkerUri = it },
        onSelect = viewModel::select,
        onRefresh = viewModel::refresh,
        onUnlock = { viewModel.unlockPrivate()?.let(decryptLauncher::launch) },
        onOpenArticle = onOpenArticle,
        onConnect = { authViewModel.connectIntent()?.let(authLauncher::launch) },
        onConnectBunker = { authViewModel.connectBunker(bunkerUri) },
        onOpenLibrarySettings = onOpenLibrarySettings,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreenContent(
    state: LibraryUiState,
    refreshing: Boolean,
    bucket: BookmarkBucket,
    authState: AuthUiState,
    authMessage: String?,
    bunkerUri: String,
    onBunkerUriChange: (String) -> Unit,
    onSelect: (BookmarkBucket) -> Unit,
    onRefresh: () -> Unit,
    onUnlock: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onConnect: () -> Unit,
    onConnectBunker: () -> Unit,
    onOpenLibrarySettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showInfo by remember { mutableStateOf(false) }
    if (showInfo) {
        LibraryInfoDialog(onDismiss = { showInfo = false })
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                actions = {
                    IconButton(onClick = { showInfo = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.library_info),
                        )
                    }
                    TopBarMoreMenu(
                        items = listOf(
                            TopBarMenuItem(
                                label = stringResource(R.string.library_settings),
                                icon = Icons.Outlined.Settings,
                                onClick = onOpenLibrarySettings,
                            ),
                        ),
                    )
                },
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            when (state) {
                LibraryUiState.LoggedOut -> {
                    LoggedOutLibrary(
                        authState = authState,
                        authMessage = authMessage,
                        bunkerUri = bunkerUri,
                        onBunkerUriChange = onBunkerUriChange,
                        onConnect = onConnect,
                        onConnectBunker = onConnectBunker,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                LibraryUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                LibraryUiState.Error -> {
                    StatusMessage(
                        text = stringResource(R.string.library_error),
                        onRetry = onRefresh,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                is LibraryUiState.Ready -> {
                    ReadyLibrary(
                        shelves = state.shelves,
                        bucket = bucket,
                        refreshing = refreshing,
                        onSelect = onSelect,
                        onRefresh = onRefresh,
                        onUnlock = onUnlock,
                        onOpenArticle = onOpenArticle,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ReadyLibrary(
    shelves: BookmarkShelves,
    bucket: BookmarkBucket,
    refreshing: Boolean,
    onSelect: (BookmarkBucket) -> Unit,
    onRefresh: () -> Unit,
    onUnlock: () -> Unit,
    onOpenArticle: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ShelfChip(
                selected = bucket == BookmarkBucket.Private,
                label = stringResource(R.string.library_private),
                icon = Icons.Outlined.Lock,
                onClick = { onSelect(BookmarkBucket.Private) },
            )
            ShelfChip(
                selected = bucket == BookmarkBucket.Public,
                label = stringResource(R.string.library_public),
                icon = Icons.Outlined.Public,
                onClick = { onSelect(BookmarkBucket.Public) },
            )
            ShelfChip(
                selected = bucket == BookmarkBucket.Web,
                label = stringResource(R.string.library_web),
                icon = Icons.Outlined.Language,
                onClick = { onSelect(BookmarkBucket.Web) },
            )
            ShelfChip(
                selected = bucket == BookmarkBucket.Look,
                label = stringResource(R.string.library_look),
                icon = Icons.Outlined.Visibility,
                onClick = { onSelect(BookmarkBucket.Look) },
            )
            ShelfChip(
                selected = bucket == BookmarkBucket.Archive,
                label = stringResource(R.string.library_archive),
                icon = BorisIcons.Books,
                onClick = { onSelect(BookmarkBucket.Archive) },
            )
        }
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            val items = shelves.items(bucket)
            when {
                bucket == BookmarkBucket.Private && shelves.privateLocked -> {
                    StatusMessage(
                        text = stringResource(R.string.library_private_locked),
                        action = stringResource(R.string.library_unlock),
                        onRetry = onUnlock,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                items.isEmpty() -> {
                    StatusMessage(
                        text = stringResource(R.string.library_empty),
                        onRetry = onRefresh,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxSize()
                            .align(Alignment.TopCenter),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items, key = { it.id }) { item ->
                            BookmarkRow(item = item, onOpenArticle = onOpenArticle)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShelfChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    )
}

@Composable
private fun BookmarkRow(
    item: BookmarkItem,
    onOpenArticle: (String) -> Unit,
) {
    ArticleRow(
        title = item.title,
        imageUrl = item.imageUrl,
        imageFallbackIcon = Icons.Outlined.Bookmark,
        byline = item.host,
        url = item.url,
        enabled = item.url != null,
        onClick = { item.url?.let(onOpenArticle) },
    )
}

@Composable
private fun LoggedOutLibrary(
    authState: AuthUiState,
    authMessage: String?,
    bunkerUri: String,
    onBunkerUriChange: (String) -> Unit,
    onConnect: () -> Unit,
    onConnectBunker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .imePadding()
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.library_logged_out_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.library_logged_out_body),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AuthBar(
            state = authState,
            message = authMessage,
            bunkerUri = bunkerUri,
            onBunkerUriChange = onBunkerUriChange,
            onConnect = onConnect,
            onConnectBunker = onConnectBunker,
        )
        NstartFooter()
    }
}

@Composable
private fun StatusMessage(
    text: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    action: String = stringResource(R.string.feed_retry),
) {
    Column(
        modifier = modifier
            .widthIn(max = 420.dp)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(
            onClick = onRetry,
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(action)
        }
    }
}

@Composable
private fun LibraryInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.library_info_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LibraryInfoRow(
                    icon = Icons.Outlined.Lock,
                    title = stringResource(R.string.library_info_private_title),
                    body = stringResource(R.string.library_info_private),
                )
                LibraryInfoRow(
                    icon = Icons.Outlined.Public,
                    title = stringResource(R.string.library_info_public_title),
                    body = stringResource(R.string.library_info_public),
                )
                LibraryInfoRow(
                    icon = Icons.Outlined.Language,
                    title = stringResource(R.string.library_info_web_title),
                    body = stringResource(R.string.library_info_web),
                )
                LibraryInfoRow(
                    icon = Icons.Outlined.Visibility,
                    title = stringResource(R.string.library_info_look_title),
                    body = stringResource(R.string.library_info_look),
                )
                LibraryInfoRow(
                    icon = BorisIcons.Books,
                    title = stringResource(R.string.library_info_archive_title),
                    body = stringResource(R.string.library_info_archive),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.library_info_close))
            }
        },
    )
}

@Composable
private fun LibraryInfoRow(
    title: String,
    body: String,
    icon: ImageVector,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.SansSerif),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
