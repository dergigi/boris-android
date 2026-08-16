package org.dergigi.boris.ui.about

import androidx.annotation.StringRes
import org.dergigi.boris.R

internal data class AboutFeature(
    val asset: String,
    @StringRes val title: Int,
    val paragraphs: List<Int>,
)

internal sealed class AboutPage {
    data object Intro : AboutPage()
    data class Feature(val feature: AboutFeature) : AboutPage()
    data object Cta : AboutPage()
}

internal object AboutLinks {
    const val NPUB = "npub19802see0gnk3vjlus0dnmfdagusqrtmsxpl5yfmkwn9uvnfnqylqduhr0x"
    const val GITHUB = "https://github.com/dergigi/boris-android"
    const val BUG_REPORT = "$GITHUB/issues/new?template=bug_report.yml"
    const val FEATURE_REQUEST = "$GITHUB/issues/new?template=feature_request.yml"
    const val VALUE = "https://dergigi.com/value/"
    val nostrUrl = "https://njump.to/$NPUB"
}

internal val ABOUT_FEATURES = listOf(
    AboutFeature(
        asset = "read-anywhere.svg",
        title = R.string.about_read_anywhere_title,
        paragraphs = listOf(R.string.about_read_anywhere_1, R.string.about_read_anywhere_2),
    ),
    AboutFeature(
        asset = "distraction-free.svg",
        title = R.string.about_distraction_free_title,
        paragraphs = listOf(R.string.about_distraction_free_1, R.string.about_distraction_free_2),
    ),
    AboutFeature(
        asset = "airplane-mode.svg",
        title = R.string.about_airplane_mode_title,
        paragraphs = listOf(R.string.about_airplane_mode_1),
    ),
    AboutFeature(
        asset = "swarm-highlights.svg",
        title = R.string.about_social_highlights_title,
        paragraphs = listOf(R.string.about_social_highlights_1, R.string.about_social_highlights_2),
    ),
    AboutFeature(
        asset = "reading-list.svg",
        title = R.string.about_lists_title,
        paragraphs = listOf(R.string.about_lists_1, R.string.about_lists_2),
    ),
    AboutFeature(
        asset = "zap-splits.svg",
        title = R.string.about_zap_splits_title,
        paragraphs = listOf(R.string.about_zap_splits_1, R.string.about_zap_splits_2),
    ),
    AboutFeature(
        asset = "comforting-colors.svg",
        title = R.string.about_comforting_colors_title,
        paragraphs = listOf(R.string.about_comforting_colors_1),
    ),
    AboutFeature(
        asset = "peace-of-mind.svg",
        title = R.string.about_peace_of_mind_title,
        paragraphs = listOf(R.string.about_peace_of_mind_1, R.string.about_peace_of_mind_2),
    ),
    AboutFeature(
        asset = "free-forever.svg",
        title = R.string.about_free_title,
        paragraphs = listOf(R.string.about_free_1, R.string.about_free_2_before),
    ),
)

internal val ABOUT_PAGES: List<AboutPage> = buildList {
    add(AboutPage.Intro)
    ABOUT_FEATURES.forEach { add(AboutPage.Feature(it)) }
    add(AboutPage.Cta)
}
