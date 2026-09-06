package org.dergigi.boris.ui.reader

import org.dergigi.boris.ui.ZapProgress

internal data class ZapFeedback(
    val dialogProgress: ZapProgress?,
    val successSats: Long?,
)

internal fun zapFeedbackFor(progress: ZapProgress?): ZapFeedback {
    if (progress !is ZapProgress.Done) return ZapFeedback(progress, null)
    return ZapFeedback(
        dialogProgress = progress.takeIf { it.failed.isNotEmpty() },
        successSats = progress.paidSats.takeIf { it > 0 },
    )
}
