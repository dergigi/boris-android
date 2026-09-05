package org.dergigi.boris.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dergigi.boris.R
import org.dergigi.boris.data.ArticleImages
import org.dergigi.boris.data.ByteSize
import org.dergigi.boris.data.CacheLimit
import org.dergigi.boris.data.CacheUsage
import org.dergigi.boris.data.OfflineDownloader
import org.dergigi.boris.data.OfflineShelf
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.ui.theme.BorisIcons

private val SHELF_TITLES = mapOf(
    OfflineShelf.Bookmarks to R.string.settings_offline_bookmarks,
    OfflineShelf.Web to R.string.settings_offline_web,
    OfflineShelf.Lookmarks to R.string.settings_offline_lookmarks,
    OfflineShelf.Archive to R.string.settings_offline_archive,
    OfflineShelf.Highlights to R.string.settings_offline_highlights,
)

private val OfflineShelf.icon: ImageVector
    @Composable get() = when (this) {
        OfflineShelf.Bookmarks -> Icons.Outlined.Bookmark
        OfflineShelf.Web -> Icons.Outlined.Language
        OfflineShelf.Lookmarks -> Icons.Outlined.Visibility
        OfflineShelf.Archive -> BorisIcons.Books
        OfflineShelf.Highlights -> BorisIcons.Highlighter
    }

@Composable
fun OfflineSection(
    settings: UserSettings,
    onUpdate: (UserSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val progress by OfflineDownloader.progress.collectAsStateWithLifecycle()
    val imageProgress by OfflineDownloader.imageProgress.collectAsStateWithLifecycle()
    var usedBytes by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { OfflineDownloader.kickoff(context) }
    LaunchedEffect(progress, imageProgress) {
        usedBytes = withContext(Dispatchers.IO) { CacheUsage.bytes(context) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_offline_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.settings_offline_available).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OfflineShelf.entries.forEach { shelf ->
            ShelfRow(
                shelf = shelf,
                enabled = settings.offlineDownloadEnabled(shelf.settingsKey),
                total = progress[shelf]?.total ?: 0,
                downloaded = progress[shelf]?.downloaded ?: 0,
                bytes = progress[shelf]?.bytes ?: 0L,
                onToggle = { on ->
                    onUpdate(settings.withBoolean(shelf.settingsKey, on))
                    if (on) OfflineDownloader.kickoff(context)
                },
            )
        }
        ImagesRow(
            enabled = settings.offlineDownloadEnabled(ArticleImages.SETTINGS_KEY),
            total = imageProgress.total,
            downloaded = imageProgress.downloaded,
            bytes = imageProgress.bytes,
            onToggle = { on ->
                onUpdate(settings.withBoolean(ArticleImages.SETTINGS_KEY, on))
                if (on) OfflineDownloader.kickoff(context)
            },
        )
        StorageLimit(usedBytes = usedBytes)
    }
}

@Composable
private fun ShelfRow(
    shelf: OfflineShelf,
    enabled: Boolean,
    total: Int,
    downloaded: Int,
    bytes: Long,
    onToggle: (Boolean) -> Unit,
) {
    val shownDownloaded = if (enabled) downloaded else 0
    val progressLabel = if (enabled && bytes > 0L) {
        stringResource(
            R.string.settings_offline_progress_sized,
            shownDownloaded,
            total,
            ByteSize.format(bytes),
        )
    } else {
        stringResource(R.string.settings_offline_progress, shownDownloaded, total)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(if (enabled) 1f else 0.5f),
            ) {
                Icon(
                    imageVector = shelf.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(SHELF_TITLES.getValue(shelf)),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.alpha(if (enabled) 1f else 0.5f),
        ) {
            LinearProgressIndicator(
                progress = { if (total > 0 && enabled) downloaded.toFloat() / total else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = progressLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ImagesRow(
    enabled: Boolean,
    total: Int,
    downloaded: Int,
    bytes: Long,
    onToggle: (Boolean) -> Unit,
) {
    val shownDownloaded = if (enabled) downloaded else 0
    val progressLabel = if (enabled && bytes > 0L) {
        stringResource(
            R.string.settings_offline_progress_sized,
            shownDownloaded,
            total,
            ByteSize.format(bytes),
        )
    } else {
        stringResource(R.string.settings_offline_progress, shownDownloaded, total)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(if (enabled) 1f else 0.5f),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(R.string.settings_offline_images),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.alpha(if (enabled) 1f else 0.5f),
        ) {
            LinearProgressIndicator(
                progress = { if (total > 0 && enabled) downloaded.toFloat() / total else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = progressLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StorageLimit(usedBytes: Long) {
    val context = LocalContext.current
    var limitMb by remember { mutableIntStateOf(CacheLimit.megabytes(context)) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.settings_offline_limit),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (usedBytes > 0L) {
            Text(
                text = stringResource(R.string.settings_offline_using, ByteSize.format(usedBytes)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CacheLimit.OPTIONS_MB.forEach { mb ->
                LimitButton(
                    label = limitLabel(mb),
                    selected = limitMb == mb,
                    onClick = {
                        limitMb = mb
                        CacheLimit.set(context, mb)
                    },
                )
            }
        }
        Text(
            text = stringResource(R.string.settings_offline_limit_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun limitLabel(mb: Int): String =
    if (mb < 1024) "$mb MB" else "${mb / 1024} GB"

@Composable
private fun LimitButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val border = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
        )
    }
}
