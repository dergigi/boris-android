package org.dergigi.boris.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dergigi.boris.R
import org.dergigi.boris.data.FeatureSuggestion
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.ui.about.AboutLinks
import org.dergigi.boris.ui.openExternalUri
import org.dergigi.boris.ui.reader.openWeblink
import org.dergigi.boris.ui.theme.BorisIcons
import org.dergigi.boris.ui.theme.HighlightFriends
import org.dergigi.boris.ui.theme.HighlightOther

@Composable
fun AboutSettingsSection(
    settings: UserSettings,
    onUpdate: (UserSettings) -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenFeatures: () -> Unit,
    onOpenFaq: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenAuthorProfile: () -> Unit,
    onRecommendBoris: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val linkTint = SettingsTints.About
    val openInBoris = settings.openLinksInReader
    var showFeatureSuggestion by remember { mutableStateOf(false) }
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

        AboutSectionTitle(stringResource(R.string.settings_about_learn))
        AboutActionRow(
            label = stringResource(R.string.settings_about_tutorial),
            subtitle = stringResource(R.string.settings_about_tutorial_summary),
            icon = Icons.AutoMirrored.Outlined.MenuBook,
            tint = linkTint,
            onClick = onOpenTutorial,
        )
        AboutActionRow(
            label = stringResource(R.string.settings_about_features),
            subtitle = stringResource(R.string.settings_about_features_summary),
            icon = Icons.Outlined.Lightbulb,
            tint = linkTint,
            onClick = onOpenFeatures,
        )
        AboutActionRow(
            label = stringResource(R.string.settings_about_faq),
            subtitle = stringResource(R.string.settings_about_faq_summary),
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            tint = linkTint,
            onClick = onOpenFaq,
        )
        AboutActionRow(
            label = stringResource(R.string.settings_about_vision),
            subtitle = stringResource(R.string.settings_about_vision_summary),
            icon = BorisIcons.Highlighter,
            tint = HighlightOther,
            onClick = { onOpenArticle(AboutLinks.VISION) },
        )

        AboutSectionTitle(
            text = stringResource(R.string.settings_about_community),
            modifier = Modifier.padding(top = 16.dp),
        )
        AboutActionRow(
            label = stringResource(R.string.support_title),
            subtitle = stringResource(R.string.settings_about_support_summary),
            icon = Icons.Filled.Favorite,
            tint = HighlightFriends,
            onClick = onOpenSupport,
        )
        AboutActionRow(
            label = stringResource(R.string.settings_about_recommend),
            subtitle = stringResource(R.string.settings_about_recommend_summary),
            painter = painterResource(R.drawable.ic_nostr),
            tint = linkTint,
            onClick = onRecommendBoris,
        )
        AboutActionRow(
            label = stringResource(R.string.about_cta_bug),
            subtitle = stringResource(R.string.settings_about_github_issue),
            icon = Icons.Outlined.BugReport,
            tint = linkTint,
            trailing = Icons.AutoMirrored.Outlined.OpenInNew,
            onClick = { openExternalUri(context, AboutLinks.BUG_REPORT) },
        )
        SettingCheckbox(
            label = stringResource(R.string.settings_offer_crash_reports),
            checked = settings.offerCrashReports,
            onCheckedChange = { onUpdate(settings.withBoolean("offerCrashReports", it)) },
        )
        AboutActionRow(
            label = stringResource(R.string.about_cta_feature),
            subtitle = stringResource(R.string.settings_about_feature_dm_summary),
            icon = Icons.Outlined.Lightbulb,
            tint = linkTint,
            onClick = { showFeatureSuggestion = true },
        )

        AboutSectionTitle(
            text = stringResource(R.string.settings_about_links),
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
    if (showFeatureSuggestion) {
        FeatureSuggestionDialog(onDismiss = { showFeatureSuggestion = false })
    }
}

@Composable
private fun FeatureSuggestionDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var suggestion by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val trimmed = suggestion.trim()
    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        title = { Text(stringResource(R.string.feature_suggestion_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.feature_suggestion_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = suggestion,
                    onValueChange = {
                        suggestion = it
                        failed = false
                    },
                    label = { Text(stringResource(R.string.feature_suggestion_label)) },
                    minLines = 4,
                    maxLines = 8,
                    enabled = !sending,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (failed) {
                    Text(
                        text = stringResource(R.string.feature_suggestion_failed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = trimmed.isNotBlank() && !sending,
                onClick = {
                    sending = true
                    failed = false
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            runCatching { FeatureSuggestion.send(trimmed) }.getOrDefault(false)
                        }
                        sending = false
                        if (ok) {
                            Toast.makeText(
                                context,
                                R.string.feature_suggestion_sent,
                                Toast.LENGTH_SHORT,
                            ).show()
                            onDismiss()
                        } else {
                            failed = true
                        }
                    }
                },
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp).padding(2.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.feature_suggestion_send))
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !sending, onClick = onDismiss) {
                Text(stringResource(R.string.settings_reset_cancel))
            }
        },
    )
}

@Composable
private fun AboutSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(bottom = 4.dp),
    )
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
