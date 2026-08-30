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

object AboutLinks {
    const val WEBSITE = "https://readwithboris.com/"
    const val FAQ = "${WEBSITE}#faq"
    const val WEBAPP = "https://read.withboris.com/"
    const val NOSTR_PROTOCOL = "https://nostr.com/"
    const val NIP_84 = "https://nostrbook.dev/kinds/9802"
    const val WHY_BORIS_IDEA =
        "https://read.withboris.com/a/naddr1qvzqqqr4gupzqmjxss3dld622uu8q25gywum9qtg4w4cv4064jmg20xsac2aam5nqqxnzd3cxqmrzv3exgmr2wfesgsmew"
    const val WHY_BORIS_READING_APP =
        "https://read.withboris.com/a/naddr1qqsxzttkd9ekjmmw94nx7u3dvykhvctvw4jj6etwv93xcety94mk2cszyphydppzm7m554ecwq4gsgaek2qk32atse2l4t9ks57dpms4mmhfxqcyqqq823c59stcz"
    const val WHY_BORIS_NAME =
        "https://relay.dergigi.com/03e174145e1f410772bb8c3e79b153ac0077fe482d7006b1f0ed67a81d475bb9.png"
    const val NPUB = "npub19802see0gnk3vjlus0dnmfdagusqrtmsxpl5yfmkwn9uvnfnqylqduhr0x"
    const val GITHUB = "https://github.com/dergigi/boris-android"
    const val BUG_REPORT = "$GITHUB/issues/new?template=bug_report.yml"
    const val FEATURE_REQUEST = "$GITHUB/issues/new?template=feature_request.yml"
    const val VISION =
        "nostr:naddr1qqxnzd3cxqmrzv3exgmr2wfeqy08wumn8ghj7mn0wd68yttsw43zuam9d3kx7unyv4ezumn9wshszyrhwden5te0dehhxarj9ekk7mf0qy88wumn8ghj7mn0wvhxcmmv9uq3zamnwvaz7tmwdaehgu3wwa5kuef0qy2hwumn8ghj7un9d3shjtnwdaehgu3wvfnj7q3qdergggklka99wwrs92yz8wdjs952h2ux2ha2ed598ngwu9w7a6fsxpqqqp65wy2vhhv"
    const val AUTHOR_NAME = "Gigi"
    const val AUTHOR_SITE = "https://dergigi.com"
    const val AUTHOR_NPUB =
        "npub1dergggklka99wwrs92yz8wdjs952h2ux2ha2ed598ngwu9w7a6fsh9xzpc"
    val nostrUrl = "https://njump.to/$NPUB"
    val authorNostrUrl = "https://njump.to/$AUTHOR_NPUB"
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
