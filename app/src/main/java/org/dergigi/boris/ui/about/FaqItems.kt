package org.dergigi.boris.ui.about

import androidx.annotation.StringRes
import org.dergigi.boris.R

internal sealed class FaqPart {
    data class Text(@StringRes val res: Int) : FaqPart()
    data class Link(@StringRes val res: Int, val url: String) : FaqPart()
}

internal data class FaqItem(
    @StringRes val section: Int,
    @StringRes val question: Int,
    val answer: List<FaqPart>,
)

/** Mirror of https://readwithboris.com/#faq. Update this list when the website FAQ changes. */
internal val FAQ_ITEMS = listOf(
    FaqItem(
        section = R.string.faq_section_basics,
        question = R.string.faq_nostr_q,
        answer = listOf(
            FaqPart.Text(R.string.faq_nostr_before),
            FaqPart.Link(R.string.faq_nostr_link, AboutLinks.NOSTR_PROTOCOL),
            FaqPart.Text(R.string.faq_nostr_after),
        ),
    ),
    FaqItem(
        section = R.string.faq_section_app,
        question = R.string.faq_why_q,
        answer = listOf(
            FaqPart.Text(R.string.faq_why_before),
            FaqPart.Link(R.string.faq_why_idea, AboutLinks.WHY_BORIS_IDEA),
            FaqPart.Text(R.string.faq_why_for),
            FaqPart.Link(R.string.faq_why_reading_app, AboutLinks.WHY_BORIS_READING_APP),
            FaqPart.Text(R.string.faq_why_middle),
            FaqPart.Link(R.string.faq_why_name, AboutLinks.WHY_BORIS_NAME),
            FaqPart.Text(R.string.faq_why_after),
        ),
    ),
    FaqItem(
        section = R.string.faq_section_nostr,
        question = R.string.faq_privacy_q,
        answer = listOf(
            FaqPart.Text(R.string.faq_privacy_before),
            FaqPart.Link(R.string.faq_privacy_link, AboutLinks.NIP_84),
            FaqPart.Text(R.string.faq_privacy_after),
        ),
    ),
)
