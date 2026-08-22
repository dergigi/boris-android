package org.dergigi.boris.ui.about

import org.dergigi.boris.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutPagesTest {
    @Test
    fun startsWithIntroAndEndsWithCta() {
        assertEquals(AboutPage.Intro, ABOUT_PAGES.first())
        assertEquals(AboutPage.Cta, ABOUT_PAGES.last())
        assertEquals(ABOUT_FEATURES.size + 2, ABOUT_PAGES.size)
    }

    @Test
    fun ctaLinksPointAtGigiAndTheRepo() {
        assertEquals(
            "npub19802see0gnk3vjlus0dnmfdagusqtmsxpl5yfmkwn9uvnfnqylqduhr0x",
            AboutLinks.NPUB,
        )
        assertEquals("https://njump.to/${AboutLinks.NPUB}", AboutLinks.nostrUrl)
        assertEquals("https://github.com/dergigi/boris-android", AboutLinks.GITHUB)
        assertEquals(
            "https://github.com/dergigi/boris-android/issues/new?template=bug_report.yml",
            AboutLinks.BUG_REPORT,
        )
        assertEquals(
            "https://github.com/dergigi/boris-android/issues/new?template=feature_request.yml",
            AboutLinks.FEATURE_REQUEST,
        )
        assertTrue(ABOUT_FEATURES.isNotEmpty())
        assertTrue(AboutLinks.VISION.startsWith("nostr:naddr1"))
    }

    @Test
    fun faqMirrorsTheWebsite() {
        assertEquals("https://readwithboris.com/#faq", AboutLinks.FAQ)
        assertEquals(3, FAQ_ITEMS.size)
        assertEquals(R.string.faq_section_basics, FAQ_ITEMS[0].section)
        assertEquals(R.string.faq_section_app, FAQ_ITEMS[1].section)
        assertEquals(R.string.faq_section_nostr, FAQ_ITEMS[2].section)
        assertEquals(
            listOf(
                AboutLinks.NOSTR_PROTOCOL,
                AboutLinks.WHY_BORIS_IDEA,
                AboutLinks.WHY_BORIS_READING_APP,
                AboutLinks.WHY_BORIS_NAME,
                AboutLinks.NIP_84,
            ),
            FAQ_ITEMS.flatMap { item ->
                item.answer.mapNotNull { part -> (part as? FaqPart.Link)?.url }
            },
        )
        assertEquals("https://nostr.com/", AboutLinks.NOSTR_PROTOCOL)
        assertEquals("https://nostrbook.dev/kinds/9802", AboutLinks.NIP_84)
    }
}
