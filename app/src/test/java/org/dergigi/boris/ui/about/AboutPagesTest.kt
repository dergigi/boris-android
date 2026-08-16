package org.dergigi.boris.ui.about

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
            "npub19802see0gnk3vjlus0dnmfdagusqrtmsxpl5yfmkwn9uvnfnqylqduhr0x",
            AboutLinks.NPUB,
        )
        assertEquals("https://njump.to/${AboutLinks.NPUB}", AboutLinks.nostrUrl)
        assertEquals("https://github.com/dergigi/boris-android", AboutLinks.GITHUB)
        assertTrue(ABOUT_FEATURES.isNotEmpty())
    }
}
