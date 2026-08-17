package org.dergigi.boris.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.dergigi.boris.BuildConfig
import org.dergigi.boris.R
import org.dergigi.boris.ui.about.AboutLinks
import org.dergigi.boris.ui.reader.openWeblink

@Composable
fun SettingsVersionFooter(
    openInBoris: Boolean,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val version = BuildConfig.VERSION_NAME
    val commit = BuildConfig.GIT_COMMIT
    val shortCommit = if (commit.length > 7) commit.take(7) else commit
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 4.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.settings_version, version),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable {
                openWeblink(
                    "${AboutLinks.GITHUB}/releases/tag/v$version",
                    openInBoris,
                    onOpenArticle,
                    uriHandler::openUri,
                )
            },
        )
        Text(
            text = "·",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = shortCommit,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable(enabled = commit != "unknown") {
                openWeblink(
                    "${AboutLinks.GITHUB}/commit/$commit",
                    openInBoris,
                    onOpenArticle,
                    uriHandler::openUri,
                )
            },
        )
    }
}
