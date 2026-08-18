package org.dergigi.boris.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dergigi.boris.R
import org.dergigi.boris.data.NostrLink
import org.dergigi.boris.data.UrlExtractor
import org.dergigi.boris.tts.TtsPlayback
import org.dergigi.boris.tts.TtsSession
import org.dergigi.boris.tts.TtsSpeed
import org.dergigi.boris.ui.settings.SettingsViewModel

/**
 * Shows the slim mini player (D-17) while an article session is active. Previews
 * keep the session null, and only a non-blank article url composes the bar, so
 * Settings preview never shows it.
 */
@Composable
fun TtsMiniPlayerHost(
    currentArticleUrl: String?,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
    showCurrentArticle: Boolean = false,
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val session by TtsPlayback.session.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val active = session?.takeIf {
        it.url.isNotBlank() && (showCurrentArticle || !isSameArticle(it.url, currentArticleUrl))
    }
    LaunchedEffect(active?.url, settings.ttsFollowAlong) {
        if (active != null) TtsPlayback.setFollowAlongEnabled(settings.ttsFollowAlong)
    }
    // Keep the last session around so the 200ms fade-out still has content.
    var shown by remember { mutableStateOf(active) }
    if (active != null) shown = active
    AnimatedVisibility(
        visible = active != null,
        enter = fadeIn(tween(FADE_MS)),
        exit = fadeOut(tween(FADE_MS)),
        modifier = modifier,
    ) {
        shown?.let { current ->
            TtsMiniPlayer(
                session = current,
                onOpenArticle = onOpenArticle,
                onCycleSpeed = {
                    // D-18: same presets and ttsDefaultSpeed key as Settings.
                    val next = TtsSpeed.cycle(current.rate)
                    settingsViewModel.update { it.withDouble("ttsDefaultSpeed", next) }
                    TtsPlayback.setRate(next)
                },
                onSelectSpeed = { speed ->
                    val next = TtsSpeed.snap(speed)
                    settingsViewModel.update { it.withDouble("ttsDefaultSpeed", next) }
                    TtsPlayback.setRate(next)
                },
                followAlongEnabled = settings.ttsFollowAlong,
                onToggleFollowAlong = {
                    val enabled = !settings.ttsFollowAlong
                    settingsViewModel.update { it.withBoolean("ttsFollowAlong", enabled) }
                    TtsPlayback.setFollowAlongEnabled(enabled)
                },
            )
        }
    }
}

/** The route url may be un-normalized (pasted) or a nostr link; session urls are canonical. */
private fun isSameArticle(sessionUrl: String, routeUrl: String?): Boolean {
    if (routeUrl.isNullOrBlank()) return false
    if (sessionUrl == routeUrl) return true
    if (sessionUrl == UrlExtractor.normalize(routeUrl)) return true
    return NostrLink.parse(routeUrl)?.uri == sessionUrl
}

/**
 * Slim 56dp in-app player (D-17, D-18): title, speed cycle, paragraph transport.
 * No progress bar, artwork, author line, or stop button; Stop stays on the
 * system notification.
 */
@Composable
fun TtsMiniPlayer(
    session: TtsSession,
    onOpenArticle: (String) -> Unit,
    onCycleSpeed: () -> Unit,
    onSelectSpeed: (Double) -> Unit,
    followAlongEnabled: Boolean,
    onToggleFollowAlong: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val openArticle = stringResource(R.string.tts_open_article)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
    ) {
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = session.title.ifBlank { stringResource(R.string.tts_mini_player_fallback) },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenArticle(session.url) }
                    .semantics { contentDescription = openArticle },
            )
            SpeedChip(
                rate = session.rate,
                onCycle = onCycleSpeed,
                onSelect = onSelectSpeed,
            )
            IconButton(
                onClick = onToggleFollowAlong,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.CenterFocusStrong,
                    contentDescription = stringResource(
                        if (followAlongEnabled) {
                            R.string.tts_disable_follow_along
                        } else {
                            R.string.tts_enable_follow_along
                        },
                    ),
                    tint = if (followAlongEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            IconButton(onClick = { TtsPlayback.skip(-1) }, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Filled.FastRewind,
                    contentDescription = stringResource(R.string.tts_previous_paragraph),
                )
            }
            IconButton(
                onClick = { if (session.playing) TtsPlayback.pause() else TtsPlayback.resume() },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = if (session.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        if (session.playing) R.string.tts_pause_playback else R.string.tts_resume_playback,
                    ),
                )
            }
            IconButton(onClick = { TtsPlayback.skip(1) }, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Filled.FastForward,
                    contentDescription = stringResource(R.string.tts_next_paragraph),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpeedChip(
    rate: Double,
    onCycle: () -> Unit,
    onSelect: (Double) -> Unit,
) {
    val description = stringResource(R.string.tts_cycle_speed)
    val shape = RoundedCornerShape(8.dp)
    val selected = TtsSpeed.snap(rate)
    var menuOpen by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .combinedClickable(
                onClick = onCycle,
                onLongClick = { menuOpen = true },
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rateLabel(rate),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .clip(shape)
                .border(1.dp, MaterialTheme.colorScheme.outline, shape)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
        ) {
            TtsSpeed.PRESETS.forEach { speed ->
                DropdownMenuItem(
                    text = { Text(rateLabel(speed)) },
                    trailingIcon = if (speed == selected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                            )
                        }
                    } else {
                        null
                    },
                    onClick = {
                        menuOpen = false
                        onSelect(speed)
                    },
                )
            }
        }
    }
}

private fun rateLabel(rate: Double): String =
    if (rate % 1.0 == 0.0) "${rate.toInt()}x" else "${rate}x"

private const val FADE_MS = 200
