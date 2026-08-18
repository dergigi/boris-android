package org.dergigi.boris.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R
import org.dergigi.boris.ui.about.AboutLinks
import org.dergigi.boris.ui.reader.openWeblink
import org.dergigi.boris.ui.theme.BorisIcons
import org.dergigi.boris.ui.theme.HighlightFriends
import org.dergigi.boris.ui.theme.HighlightOther

@Composable
fun AboutSettingsSection(
    openInBoris: Boolean,
    onOpenArticle: (String) -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenAuthorProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val linkTint = SettingsTints.About
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_about_blurb),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        AboutActionRow(
            label = stringResource(R.string.settings_about_tutorial),
            subtitle = stringResource(R.string.settings_about_tutorial_summary),
            icon = Icons.AutoMirrored.Outlined.MenuBook,
            tint = linkTint,
            onClick = onOpenTutorial,
        )
        AboutActionRow(
            label = stringResource(R.string.settings_about_vision),
            subtitle = stringResource(R.string.settings_about_vision_summary),
            icon = BorisIcons.Highlighter,
            tint = HighlightOther,
            onClick = { onOpenArticle(AboutLinks.VISION) },
        )
        AboutActionRow(
            label = stringResource(R.string.support_title),
            subtitle = stringResource(R.string.settings_about_support_summary),
            icon = Icons.Filled.Favorite,
            tint = HighlightFriends,
            onClick = onOpenSupport,
        )

        Text(
            text = stringResource(R.string.settings_about_links),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
        )
        AboutActionRow(
            label = stringResource(R.string.settings_about_website),
            subtitle = AboutLinks.WEBSITE.removePrefix("https://").trimEnd('/'),
            icon = Icons.Outlined.Language,
            tint = linkTint,
            trailing = Icons.AutoMirrored.Outlined.OpenInNew,
            onClick = {
                openWeblink(AboutLinks.WEBSITE, openInBoris, onOpenArticle, uriHandler::openUri)
            },
        )
        AboutActionRow(
            label = stringResource(R.string.settings_about_webapp),
            subtitle = AboutLinks.WEBAPP.removePrefix("https://").trimEnd('/'),
            icon = Icons.Outlined.Language,
            tint = linkTint,
            trailing = Icons.AutoMirrored.Outlined.OpenInNew,
            onClick = {
                openWeblink(AboutLinks.WEBAPP, openInBoris, onOpenArticle, uriHandler::openUri)
            },
        )
        AboutActionRow(
            label = stringResource(R.string.settings_about_github),
            subtitle = "dergigi/boris-android",
            icon = Icons.Outlined.Code,
            tint = linkTint,
            trailing = Icons.AutoMirrored.Outlined.OpenInNew,
            onClick = {
                openWeblink(AboutLinks.GITHUB, openInBoris, onOpenArticle, uriHandler::openUri)
            },
        )
        AboutActionRow(
            label = stringResource(R.string.settings_about_author),
            subtitle = "${AboutLinks.AUTHOR_NAME} · dergigi.com",
            icon = Icons.Outlined.Person,
            tint = linkTint,
            trailing = Icons.AutoMirrored.Outlined.OpenInNew,
            onClick = {
                openWeblink(AboutLinks.AUTHOR_SITE, openInBoris, onOpenArticle, uriHandler::openUri)
            },
        )
        AboutActionRow(
            label = stringResource(R.string.settings_about_author_nostr),
            subtitle = AboutLinks.AUTHOR_NPUB.take(16) + "…",
            painter = painterResource(R.drawable.ic_nostr),
            tint = linkTint,
            onClick = onOpenAuthorProfile,
        )

        SettingsVersionFooter(
            openInBoris = openInBoris,
            onOpenArticle = onOpenArticle,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun AboutActionRow(
    label: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    painter: Painter? = null,
    trailing: ImageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        } else if (painter != null) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = trailing,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}
