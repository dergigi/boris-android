package org.dergigi.boris.ui.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.dergigi.boris.R

/** Square button beside [ReactionButton]; lights up orange once a zap went through this session. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ZapButton(
    zapped: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    val label = stringResource(R.string.zap_action)
    val longClickLabel = stringResource(R.string.zap_options)
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .size(ButtonDefaults.MinHeight)
            .clip(shape)
            .background(if (zapped) ReactionOrange.copy(alpha = 0.14f) else Color.Transparent)
            .border(1.dp, if (zapped) ReactionOrange else MaterialTheme.colorScheme.outline, shape)
            .semantics { contentDescription = label }
            .combinedClickable(
                onClick = onClick,
                onLongClickLabel = if (onLongClick == null) null else longClickLabel,
                onLongClick = onLongClick?.let {
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (zapped) Icons.Filled.Bolt else Icons.Outlined.Bolt,
            contentDescription = null,
            tint = if (zapped) ReactionOrange else ButtonDefaults.outlinedButtonColors().contentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}
