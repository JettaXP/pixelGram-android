package com.pixelgram.android.nav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.pixelgram.android.R

internal val PixelSettingsIndex: List<Pair<String, Int>> = listOf(
    "general" to R.string.pixel_phones,
    "general" to R.string.pixel_phone_details,
    "chats" to R.string.folder_all,
    "chats" to R.string.pixel_folder_sample_private,
    "chats" to R.string.pixel_folder_sample_work,
    "chats" to R.string.pixel_folder_sample_channels,
    "chats" to R.string.pixel_bubbles,
    "chats" to R.string.pixel_bubble_radius,
    "chats" to R.string.pixel_bubble_tail,
    "chats" to R.string.pixel_font_size,
    "chats" to R.string.pixel_messages,
    "chats" to R.string.pixel_seconds,
    "chats" to R.string.pixel_zalgo,
    "chats" to R.string.pixel_typing_bubble,
    "chats" to R.string.pixel_double_tap,
    "chats" to R.string.notify_settings,
    "chats" to R.string.notify_keep_alive,
    "chats" to R.string.pixel_chat_list,
    "chats" to R.string.pixel_exact_numbers,
    "chats" to R.string.pixel_hide_all,
    "chats" to R.string.pixel_folder_title,
    "chats" to R.string.folder_title_icons_hint,
    "chats" to R.string.cursor_title,
    "chats" to R.string.cursor_animated,
    "chats" to R.string.cursor_style,
    "chats" to R.string.cursor_smooth,
    "chats" to R.string.cursor_stretch,
    "chats" to R.string.cursor_handle,
    "chats" to R.string.cursor_pull,
    "chats" to R.string.cursor_speed,
    "chats" to R.string.cursor_trail_front,
    "chats" to R.string.cursor_trail_end,
    "chats" to R.string.cursor_trail_start,
    "chats" to R.string.cursor_blink,
    "chats" to R.string.cursor_width,
    "chats" to R.string.cursor_blink_cycle,
    "chats" to R.string.cursor_blink_smooth,
    "appearance" to R.string.pixel_theme,
    "appearance" to R.string.pixel_dynamic_color,
    "appearance" to R.string.pixel_monochrome,
    "appearance" to R.string.pixel_dynamic_icon,
    "appearance" to R.string.pixel_nav_bar,
    "appearance" to R.string.pixel_panels,
    "appearance" to R.string.pixel_show_drawer,
    "appearance" to R.string.pixel_show_bottom_bar,
    "appearance" to R.string.pixel_icon_pack,
    "appearance" to R.string.pixel_nav_tabs,
    "appearance" to R.string.pixel_drawer_items,
    "appearance" to R.string.pixel_capsule,
    "appearance" to R.string.pixel_capsule_inset,
    "appearance" to R.string.pixel_capsule_spacing,
    "appearance" to R.string.pixel_capsule_height,
    "appearance" to R.string.pixel_avatars,
    "appearance" to R.string.pixel_avatar_corner,
    "appearance" to R.string.pixel_privacy,
    "appearance" to R.string.pixel_hide_phone,
    "about" to R.string.pixel_about_text,
    "about" to R.string.support_title,
    "about" to R.string.support_entry,
    "about" to R.string.pixel_links,
    "about" to R.string.pixel_channel,
    "about" to R.string.pixel_desktop,
    "about" to R.string.pixel_source,
    "about" to R.string.pixel_info,
    "about" to R.string.pixel_version,
    "about" to R.string.pixel_credits,
    "about" to R.string.reset_all_done,
    "about" to R.string.reset_all,
    "about" to R.string.reset_all_title,
    "about" to R.string.reset_all_text,
    "about" to R.string.action_reset,
    "about" to R.string.action_cancel,
    "about" to R.string.update_checking,
    "about" to R.string.update_latest,
    "about" to R.string.update_failed,
    "about" to R.string.update_download,
    "about" to R.string.update_install,
    "about" to R.string.update_check,
    "general" to R.string.pixel_general,
    "appearance" to R.string.pixel_appearance,
    "chats" to R.string.pixel_chats,
    "about" to R.string.pixel_about,
)

object SettingsLinks {
    private const val Base = "https://t.me/pixelSettings?s="
    private val Pattern = Regex("(?:https?://)?(?:www\\.)?t\\.me/pixelSettings/?\\?s=([A-Za-z0-9]+)", RegexOption.IGNORE_CASE)
    private var english: Map<Int, String> = emptyMap()
    private var localized: Map<String, Int> = emptyMap()
    private var cachedLocale: String? = null

    private fun targets(): List<Pair<String, Int>> = PixelSettingsIndex +
        listOf("pixel" to R.string.pixel_settings, "premium" to R.string.premium_title, "stars" to R.string.stars_title, "business" to R.string.business_title) +
        com.pixelgram.android.feature.settings.SettingsSection.entries.map { "section:" + it.name to it.title }

    private fun prepare(context: android.content.Context) {
        val locale = context.resources.configuration.locales[0].toLanguageTag()
        if (cachedLocale == locale && english.isNotEmpty()) return
        val config = android.content.res.Configuration(context.resources.configuration).apply { setLocale(java.util.Locale.ENGLISH) }
        val en = context.createConfigurationContext(config).resources
        val ids = targets().map { it.second }.distinct()
        english = ids.associateWith { runCatching { en.getString(it) }.getOrDefault("") }
        localized = ids.associateBy { runCatching { context.getString(it) }.getOrDefault("") }
        cachedLocale = locale
    }

    fun slug(text: String): String {
        val words = text.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotEmpty() }
        return words.mapIndexed { index, word -> if (index == 0) word.lowercase() else word.lowercase().replaceFirstChar { it.uppercase() } }.joinToString("")
    }

    fun forTitle(context: android.content.Context, title: String): String? {
        prepare(context)
        val id = localized[title] ?: return null
        val name = english[id]?.takeIf { it.isNotBlank() } ?: return null
        return Base + slug(name)
    }

    fun matches(url: String): Boolean = Pattern.containsMatchIn(url)

    fun resolve(context: android.content.Context, url: String): androidx.navigation3.runtime.NavKey? {
        val key = Pattern.find(url)?.groupValues?.get(1) ?: return null
        prepare(context)
        val hit = targets().firstOrNull { (_, id) -> slug(english[id].orEmpty()).equals(key, ignoreCase = true) } ?: return PixelSettingsKey()
        highlight = hit.second
        return when {
            hit.first == "pixel" -> PixelSettingsKey()
            hit.first == "premium" -> PremiumKey
            hit.first == "stars" -> StarsKey
            hit.first == "business" -> BusinessKey
            hit.first.startsWith("section:") -> SettingsSectionKey(hit.first.removePrefix("section:"))
            hit.second in listOf(R.string.pixel_general, R.string.pixel_appearance, R.string.pixel_chats, R.string.pixel_about) -> PixelSettingsKey(hit.first)
            else -> PixelSettingsKey(hit.first)
        }
    }

    var highlight by androidx.compose.runtime.mutableStateOf<Int?>(null)
}
