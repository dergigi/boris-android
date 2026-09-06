package org.dergigi.boris.ui.reader

import org.dergigi.boris.ui.ZapProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ZapFeedbackTest {
    @Test
    fun successOnlyZapDismissesDialogAndReportsToastAmount() {
        val feedback = zapFeedbackFor(ZapProgress.Done(paidSats = 21, failed = emptyList()))

        assertNull(feedback.dialogProgress)
        assertEquals(21L, feedback.successSats)
    }

    @Test
    fun partialZapKeepsFailureDialogAndReportsToastAmount() {
        val done = ZapProgress.Done(paidSats = 21, failed = listOf("Alice"))
        val feedback = zapFeedbackFor(done)

        assertSame(done, feedback.dialogProgress)
        assertEquals(21L, feedback.successSats)
    }

    @Test
    fun failedZapKeepsFailureDialogWithoutSuccessToast() {
        val done = ZapProgress.Done(paidSats = 0, failed = listOf("Alice"))
        val feedback = zapFeedbackFor(done)

        assertSame(done, feedback.dialogProgress)
        assertNull(feedback.successSats)
    }

    @Test
    fun activeZapProgressStaysInDialog() {
        val progress = ZapProgress.Paying(index = 0, total = 2)
        val feedback = zapFeedbackFor(progress)

        assertSame(progress, feedback.dialogProgress)
        assertNull(feedback.successSats)
    }
}
