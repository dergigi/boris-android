package org.dergigi.boris.ui.settings

import org.dergigi.boris.R
import org.dergigi.boris.data.LocalSearch

object SettingsSearch {
    fun filterGroups(
        groups: List<List<SettingsCategory>>,
        query: String,
        textsFor: (SettingsCategory) -> List<String>,
    ): List<List<SettingsCategory>> {
        val needle = LocalSearch.normalize(query)
        if (needle.isEmpty()) return groups
        return groups.mapNotNull { group ->
            group.filter { category ->
                LocalSearch.matches(needle, *textsFor(category).toTypedArray())
            }.takeIf { it.isNotEmpty() }
        }
    }

    fun termRes(category: SettingsCategory): List<Int> =
        listOf(category.titleRes, category.subtitleRes) + EXTRA_TERMS[category].orEmpty()

    private val EXTRA_TERMS: Map<SettingsCategory, List<Int>> = mapOf(
        SettingsCategory.Appearance to listOf(
            R.string.settings_theme,
            R.string.settings_dark_theme,
            R.string.settings_light_theme,
            R.string.settings_theme_light,
            R.string.settings_theme_dark,
            R.string.settings_theme_system,
            R.string.settings_theme_black,
            R.string.settings_theme_midnight,
            R.string.settings_theme_charcoal,
            R.string.settings_theme_paper_white,
            R.string.settings_theme_sepia,
            R.string.settings_theme_ivory,
        ),
        SettingsCategory.Reading to listOf(
            R.string.settings_reading_font,
            R.string.settings_font_size,
            R.string.settings_paragraph_alignment,
            R.string.settings_align_left,
            R.string.settings_align_justify,
            R.string.settings_link_color,
            R.string.settings_open_links_in_reader,
        ),
        SettingsCategory.Tts to listOf(
            R.string.tts_speed_label,
            R.string.tts_language_label,
            R.string.tts_follow_along,
            R.string.tts_open_settings,
            R.string.tts_lang_system,
            R.string.tts_lang_content,
        ),
        SettingsCategory.Media to listOf(
            R.string.settings_full_width_images,
        ),
        SettingsCategory.Highlights to listOf(
            R.string.settings_show_highlights,
            R.string.settings_highlight_style,
            R.string.settings_style_marker,
            R.string.settings_style_underline,
            R.string.settings_color_mine,
            R.string.settings_color_friends,
            R.string.settings_color_nostrverse,
            R.string.settings_highlight_visibility,
            R.string.settings_visibility_nostrverse,
            R.string.settings_visibility_friends,
            R.string.settings_visibility_mine,
        ),
        SettingsCategory.ZapSplits to listOf(
            R.string.settings_zap_enabled,
            R.string.settings_zap_presets,
            R.string.settings_zap_your_share,
            R.string.settings_zap_author_share,
            R.string.settings_zap_boris_share,
            R.string.settings_zap_note,
        ),
        SettingsCategory.Home to listOf(
            R.string.settings_hide_archived,
            R.string.settings_hide_completed,
            R.string.settings_hide_nsfw,
            R.string.settings_nsfw_warn,
            R.string.settings_home_sections,
            R.string.settings_home_sections_intro,
            R.string.home_continue_reading,
            R.string.home_random_articles,
            R.string.home_short_reads,
            R.string.home_long_reads,
        ),
        SettingsCategory.Library to listOf(
            R.string.settings_library_default_view,
            R.string.settings_library_default_view_note,
            R.string.settings_library_default_save,
            R.string.settings_library_default_save_note,
            R.string.library_private,
            R.string.library_public,
            R.string.library_web,
            R.string.library_look,
            R.string.library_archive,
        ),
        SettingsCategory.Feed to listOf(
            R.string.settings_feed_default_view,
            R.string.settings_feed_default_view_note,
            R.string.settings_feed_scope,
            R.string.feed_scope_nostrverse,
            R.string.feed_scope_friends,
            R.string.feed_scope_mine,
            R.string.settings_rss_feeds,
            R.string.settings_rss_url_hint,
            R.string.settings_rss_import,
            R.string.settings_rss_note,
        ),
        SettingsCategory.Scroll to listOf(
            R.string.settings_hide_top_bar,
            R.string.settings_volume_scroll,
            R.string.settings_volume_scroll_amount,
            R.string.settings_sync_reading_position,
            R.string.settings_auto_scroll_position,
            R.string.settings_auto_archive_complete,
            R.string.settings_archive_closes_reader,
        ),
        SettingsCategory.Relays to listOf(
            R.string.settings_relays_intro,
            R.string.settings_relays_section_read,
            R.string.settings_relays_section_write,
            R.string.settings_relays_section_local,
        ),
        SettingsCategory.Airplane to listOf(
            R.string.settings_airplane_body,
            R.string.settings_use_local_relay,
            R.string.settings_citrine,
            R.string.settings_offline_intro,
            R.string.settings_offline_available,
            R.string.settings_offline_bookmarks,
            R.string.settings_offline_images,
            R.string.settings_offline_limit,
            R.string.settings_offline_limit_note,
        ),
        SettingsCategory.About to listOf(
            R.string.settings_about_blurb,
            R.string.settings_about_tutorial,
            R.string.settings_about_tutorial_summary,
            R.string.settings_about_faq,
            R.string.settings_about_faq_summary,
            R.string.settings_about_vision,
            R.string.settings_about_vision_summary,
            R.string.settings_about_support_summary,
            R.string.settings_about_github,
            R.string.settings_about_github_issue,
            R.string.settings_about_website,
            R.string.settings_about_webapp,
            R.string.settings_about_author,
            R.string.settings_about_author_nostr,
            R.string.support_title,
        ),
    )
}
