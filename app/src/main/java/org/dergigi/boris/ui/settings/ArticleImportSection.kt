package org.dergigi.boris.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dergigi.boris.R
import org.dergigi.boris.data.ImportedArticles

@Composable
internal fun ArticleImportSection(viewModel: ArticleImportViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val version by ImportedArticles.version.collectAsStateWithLifecycle()
    val count = remember(version) { ImportedArticles.items().size }
    val uriHandler = LocalUriHandler.current
    var confirmClear by remember { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val showDetails = expanded
    val readerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.choose(it, ArticleImportSource.Readwise) }
    }
    val pocketLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.choose(it, ArticleImportSource.Pocket) }
    }
    Column(
        modifier = Modifier.padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.article_import_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = if (count > 0 && !showDetails) {
                        stringResource(R.string.article_import_summary_with_count, count)
                    } else {
                        stringResource(R.string.article_import_summary)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = if (showDetails) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = stringResource(
                        if (showDetails) R.string.article_import_hide_options else R.string.article_import_show_options,
                    ),
                )
            }
        }
        if (showDetails) {
            ImportOption(
                title = stringResource(R.string.readwise_import_title),
                summary = stringResource(R.string.readwise_import_summary),
                button = stringResource(R.string.readwise_choose_csv),
                enabled = !state.busy,
                onChoose = { readerLauncher.launch(arrayOf("*/*")) },
            )
            TextButton(onClick = { uriHandler.openUri("https://docs.readwise.io/reader/docs/faqs/exporting") }) {
                Text(stringResource(R.string.readwise_export_help))
            }
            ImportOption(
                title = stringResource(R.string.pocket_import_title),
                summary = stringResource(R.string.pocket_import_summary),
                button = stringResource(R.string.pocket_choose_csv),
                enabled = !state.busy,
                onChoose = { pocketLauncher.launch(arrayOf("*/*")) },
            )
            state.source?.let { source ->
                Text(stringResource(
                    if (source == ArticleImportSource.Pocket) R.string.pocket_selected
                    else R.string.readwise_selected,
                ))
            }
            state.preview?.let { export ->
                Text(stringResource(R.string.article_import_preview, export.articles.size, export.skipped))
                if (!state.busy && export.articles.isNotEmpty()) {
                    TextButton(onClick = viewModel::start) {
                        Text(
                            stringResource(
                                if (state.progress == null) {
                                    R.string.article_import_start
                                } else {
                                    R.string.article_import_retry
                                },
                            ),
                        )
                    }
                }
            }
            if (state.busy) {
                LinearProgressIndicator()
                TextButton(onClick = viewModel::stop) { Text(stringResource(R.string.article_import_stop)) }
            }
            state.progress?.let { progress ->
                Text(stringResource(
                    R.string.article_import_progress, progress.processed, progress.total,
                    progress.imported, progress.duplicates, progress.failed.size,
                ))
                if (!state.busy && progress.processed < progress.total) {
                    Text(stringResource(R.string.article_import_stopped))
                }
                if (!state.busy && progress.failed.isNotEmpty()) {
                    Text(stringResource(R.string.article_import_failed_hint))
                    progress.failed.take(10).forEach {
                        Text(it.title ?: it.url, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (count > 0) {
                Text(stringResource(R.string.article_import_local_count, count))
                TextButton(enabled = !state.busy, onClick = { confirmClear = true }) {
                    Text(stringResource(R.string.article_import_remove))
                }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.article_import_remove)) },
            text = { Text(stringResource(R.string.article_import_remove_note)) },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; viewModel.clearImported() }) {
                    Text(stringResource(R.string.article_import_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun ImportOption(
    title: String,
    summary: String,
    button: String,
    enabled: Boolean,
    onChoose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.UploadFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(enabled = enabled, onClick = onChoose) {
            Text(button)
        }
    }
}
