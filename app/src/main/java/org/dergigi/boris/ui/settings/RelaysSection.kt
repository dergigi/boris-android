package org.dergigi.boris.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.dergigi.boris.R
import org.dergigi.boris.data.RelativeTime
import org.dergigi.boris.data.SessionStore
import org.dergigi.boris.nostr.LocalRelays
import org.dergigi.boris.nostr.RelayHealth
import org.dergigi.boris.nostr.RelayList
import org.dergigi.boris.nostr.RelayProbe
import org.dergigi.boris.nostr.RelayQuery
import org.dergigi.boris.nostr.RelayScoreBoard

private data class RelayRowState(
    val url: String,
    val local: Boolean,
    val connected: Boolean,
    val lastSeenAt: Long?,
    val coverage: Int,
)

private data class RelaySections(
    val local: List<RelayRowState>,
    val read: List<RelayRowState>,
    val write: List<RelayRowState>,
)

private const val REFRESH_MS = 15_000L
private const val TOP_COVERAGE_RELAYS = 8

@Composable
fun RelaysSection(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var sections by remember { mutableStateOf<RelaySections?>(null) }
    LaunchedEffect(Unit) {
        while (isActive) {
            sections = withContext(Dispatchers.IO) { relaySections(context) }
            delay(REFRESH_MS)
        }
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_relays_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val current = sections
        if (current == null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(
                    text = stringResource(R.string.settings_relays_checking),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            RelayGroup(title = stringResource(R.string.settings_relays_section_read), rows = current.read)
            RelayGroup(title = stringResource(R.string.settings_relays_section_write), rows = current.write)
            RelayGroup(title = stringResource(R.string.settings_relays_section_local), rows = current.local)
        }
    }
}

@Composable
private fun RelayGroup(
    title: String,
    rows: List<RelayRowState>,
) {
    if (rows.isEmpty()) return
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
        )
        rows.forEach { row -> RelayRow(row) }
    }
}

/**
 * Status comes from observed traffic (RelayHealth) where fresh; relays without
 * recent traffic get an active probe, whose outcome also feeds RelayHealth.
 *
 * Local, read, and write are separate lists. A relay that is both read and write
 * appears in both sections. Fallbacks, NIP-66 discoveries, and outbox coverage
 * relays are read targets from Boris's point of view.
 */
private suspend fun relaySections(context: android.content.Context): RelaySections {
    val pubkey = SessionStore.load(context)?.pubkeyHex
    val list = if (pubkey != null) {
        try {
            RelayQuery.fetchRelayList(pubkey)
        } catch (_: Exception) {
            RelayList.fallback()
        }
    } else {
        RelayList.fallback()
    }
    val follows = pubkey?.let { RelayQuery.cachedContactPubkeys(it) } ?: emptySet()
    val coverage = RelayScoreBoard.coverageCounts(follows)
    val localUrls = listOf(LocalRelays.CITRINE)
        .mapNotNull(LocalRelays::resolve)
        .distinct()
    val writeUrls = list.write
        .mapNotNull(LocalRelays::resolve)
        .filterNot(LocalRelays::isLocal)
        .distinct()
    val readUrls = buildList {
        addAll(list.read)
        addAll(RelayList.FALLBACK)
        addAll(RelayQuery.discoveredRelays())
        addAll(RelayScoreBoard.topRelays(follows, TOP_COVERAGE_RELAYS))
    }.mapNotNull(LocalRelays::resolve)
        .filterNot(LocalRelays::isLocal)
        .distinct()
    val allUrls = (localUrls + readUrls + writeUrls).distinct()
    val rows = coroutineScope {
        allUrls.map { url ->
            async(Dispatchers.IO) {
                val connected = if (RelayHealth.isFresh(url)) true else probe(url)
                RelayRowState(
                    url = url,
                    local = LocalRelays.isLocal(url),
                    connected = connected,
                    lastSeenAt = RelayHealth.stats(url)?.lastOkAt?.takeIf { it > 0 }?.let { it / 1000 },
                    coverage = coverage[url] ?: 0,
                )
            }
        }.map { it.await() }
    }.associateBy { it.url }

    fun section(urls: List<String>): List<RelayRowState> =
        urls.mapNotNull { rows[it] }
            .sortedWith(
                compareByDescending<RelayRowState> { it.connected }
                    .thenByDescending { it.coverage }
                    .thenBy { it.url },
            )

    return RelaySections(
        local = section(localUrls),
        read = section(readUrls),
        write = section(writeUrls),
    )
}

private fun probe(url: String): Boolean {
    val startedAt = System.currentTimeMillis()
    val ok = RelayProbe.isReachable(url)
    if (ok) {
        RelayHealth.onConnectOk(url, System.currentTimeMillis() - startedAt)
    } else {
        RelayHealth.onConnectFail(url)
    }
    return ok
}

private val StatusGreen = Color(0xFF22C55E)
private val StatusRed = Color(0xFFEF4444)

@Composable
private fun RelayRow(row: RelayRowState) {
    val icon = when {
        row.local -> Icons.Outlined.Flight
        row.connected -> Icons.Filled.CheckCircle
        else -> Icons.Outlined.WifiOff
    }
    val tint = if (row.connected) StatusGreen else StatusRed
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .alpha(if (row.connected) 1f else 0.7f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(
                if (row.connected) R.string.settings_relay_connected else R.string.settings_relay_disconnected,
            ),
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayUrl(row.url),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (row.coverage > 0) {
                Text(
                    text = stringResource(R.string.settings_relay_coverage, row.coverage),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val seen = row.lastSeenAt
        if (!row.connected && seen != null) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = RelativeTime.label(seen),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun displayUrl(url: String): String = url
    .removePrefix("wss://")
    .removePrefix("ws://")
    .replace("127.0.0.1", "localhost")
    .trimEnd('/')
