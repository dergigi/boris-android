package org.dergigi.boris.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.dergigi.boris.R
import org.dergigi.boris.data.UserSettings
import org.dergigi.boris.nostr.LocalRelays

private const val CITRINE_URL = "https://zapstore.dev/apps/com.greenart7c3.citrine"
private val RELAY_LINKS = listOf(
    "https://nostr.how/en/relays",
    "https://davidebtc186.substack.com/p/the-importance-of-hosting-your-own",
    "nostr:naddr1qvzqqqr4gupzq3svyhng9ld8sv44950j957j9vchdktj7cxumsep9mvvjthc2pjuqq9hyetvv9uj6um9w36hq9mgjg8",
)

@Composable
fun AirplaneModeSection(
    settings: UserSettings,
    onUpdate: (UserSettings) -> Unit,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    var citrineUp by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        while (isActive) {
            citrineUp = withContext(Dispatchers.IO) { LocalRelays.citrineReachable(force = true) }
            delay(5_000)
        }
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_airplane_mode),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            text = stringResource(R.string.settings_airplane_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = when (citrineUp) {
                null -> stringResource(R.string.settings_citrine_checking)
                true -> stringResource(R.string.settings_citrine_connected)
                false -> stringResource(R.string.settings_citrine_missing)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (citrineUp == true) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(top = 8.dp),
        )
        SettingCheckbox(
            label = stringResource(R.string.settings_use_local_relay),
            checked = settings.useLocalRelayAsCache,
            onCheckedChange = { onUpdate(settings.withBoolean("useLocalRelayAsCache", it)) },
        )
        Text(
            text = stringResource(R.string.settings_airplane_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = stringResource(R.string.settings_citrine),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable { uriHandler.openUri(CITRINE_URL) },
        )
        RelaysLearnMore(
            onOpenArticle = onOpenArticle,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun RelaysLearnMore(
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkStyle = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
    )
    val here = stringResource(R.string.settings_relays_here)
    Text(
        text = buildAnnotatedString {
            append(stringResource(R.string.settings_relays_prefix))
            RELAY_LINKS.forEachIndexed { index, url ->
                when (index) {
                    1 -> append(", ")
                    2 -> append(", and ")
                }
                withLink(
                    LinkAnnotation.Clickable(tag = "relay$index", styles = linkStyle) {
                        onOpenArticle(url)
                    },
                ) {
                    append(here)
                }
            }
            append(".")
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
