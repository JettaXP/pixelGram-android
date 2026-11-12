package com.pixelgram.android.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pixelgram.android.ui.theme.AvatarShape
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ThemeMode { System, Light, Dark }

enum class NavBarStyle { Expressive, Classic }

enum class DrawerEntry { Saved, Archive, Contacts, Pixel, Settings, Theme }

enum class IconPack { Material, Telegram, Solar, Remix }

enum class FolderTitle { NamesAndIcons, Names, Icons, ActiveName }

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = Build.VERSION.SDK_INT >= 31,
    val dynamicIcon: Boolean = false,
    val monochrome: Boolean = false,
    val avatarShape: AvatarShape = AvatarShape.Circle,
    val avatarCorner: Int = 50,
    val hidePhone: Boolean = true,
    val phoneDetails: Boolean = true,
    val navBarStyle: NavBarStyle = NavBarStyle.Expressive,
    val iconPack: IconPack = IconPack.Material,
    val folderTitle: FolderTitle = FolderTitle.NamesAndIcons,
    val capsuleInset: Int = 12,
    val capsuleSpacing: Int = 4,
    val capsuleHeight: Int = 64,
    val navTabs: List<com.pixelgram.android.feature.main.MainTab> = com.pixelgram.android.feature.main.MainTab.entries,
    val navOrder: List<com.pixelgram.android.feature.main.MainTab> = com.pixelgram.android.feature.main.MainTab.entries,
    val drawerItems: List<DrawerEntry> = DrawerEntry.entries,
    val drawerOrder: List<DrawerEntry> = DrawerEntry.entries,
    val messageSeconds: Boolean = false,
    val exactNumbers: Boolean = false,
    val zalgoFilter: Boolean = false,
    val hideAllChats: Boolean = false,
    val showDrawer: Boolean = true,
    val typingBubble: Boolean = true,
    val keepAlive: Boolean = false,
    val showBottomBar: Boolean = true,
    val bubbleRadius: Int = 18,
    val bubbleTailRadius: Int = 4,
    val messageFontSize: Int = 16,
    val doubleTapEmoji: String = "❤",
)

private val Context.store by preferencesDataStore("appearance")

class AppearanceStore(private val context: Context, private val scope: CoroutineScope) {
    private object Keys {
        val Theme = stringPreferencesKey("theme")
        val Dynamic = booleanPreferencesKey("dynamic")
        val DynamicIcon = booleanPreferencesKey("dynamic_icon")
        val Mono = booleanPreferencesKey("monochrome")
        val Shape = stringPreferencesKey("avatar_shape")
        val Corner = intPreferencesKey("avatar_corner")
        val HidePhone = booleanPreferencesKey("hide_phone")
        val PhoneDetails = booleanPreferencesKey("phone_details")
        val NavBar = stringPreferencesKey("nav_bar")
        val Pack = stringPreferencesKey("icon_pack")
        val FolderTitleMode = stringPreferencesKey("folder_title")
        val CapsuleInset = intPreferencesKey("capsule_inset")
        val CapsuleSpacing = intPreferencesKey("capsule_spacing")
        val CapsuleHeight = intPreferencesKey("capsule_height")
        val NavTabs = stringPreferencesKey("nav_tabs")
        val NavOrder = stringPreferencesKey("nav_order")
        val DrawerItems = stringPreferencesKey("drawer_items")
        val DrawerOrder = stringPreferencesKey("drawer_order")
        val K_messageSeconds = booleanPreferencesKey("message_seconds")
        val K_exactNumbers = booleanPreferencesKey("exact_numbers")
        val K_zalgoFilter = booleanPreferencesKey("zalgo_filter")
        val K_hideAllChats = booleanPreferencesKey("hide_all_chats")
        val K_drawer = booleanPreferencesKey("show_drawer")
        val K_typingBubble = booleanPreferencesKey("typing_bubble")
        val K_keepAlive = booleanPreferencesKey("keep_alive")
        val K_bottomBar = booleanPreferencesKey("show_bottom_bar")
        val K_bubbleRadius = intPreferencesKey("bubble_radius")
        val K_bubbleTailRadius = intPreferencesKey("bubble_tail")
        val K_messageFontSize = intPreferencesKey("message_font_size")
        val K_doubleTapEmoji = stringPreferencesKey("double_tap_emoji")
        val booleans = listOf(Dynamic, DynamicIcon, Mono, HidePhone, PhoneDetails, K_messageSeconds, K_exactNumbers, K_zalgoFilter, K_hideAllChats, K_drawer, K_bottomBar, K_typingBubble, K_keepAlive)
        val ints = listOf(Corner, CapsuleInset, CapsuleSpacing, CapsuleHeight, K_bubbleRadius, K_bubbleTailRadius, K_messageFontSize)
        val strings = listOf(Theme, Shape, NavBar, Pack, FolderTitleMode, NavTabs, NavOrder, DrawerItems, DrawerOrder, K_doubleTapEmoji)
    }

    suspend fun exportValues(): Map<String, String> =
        context.store.data.first().asMap().mapKeys { it.key.name }.mapValues { it.value.toString() }

    suspend fun importValues(values: Map<String, String>): Boolean {
        var applied = 0
        context.store.edit { p ->
            for (key in Keys.booleans) values[key.name]?.toBooleanStrictOrNull()?.let { p[key] = it; applied++ }
            for (key in Keys.ints) values[key.name]?.toIntOrNull()?.let { p[key] = it; applied++ }
            for (key in Keys.strings) values[key.name]?.let { p[key] = it; applied++ }
        }
        applyLauncherIcon(read(context.store.data.first()).dynamicIcon)
        return applied > 0
    }

    val state: StateFlow<AppearanceSettings> = context.store.data
        .map(::read)
        .stateIn(scope, SharingStarted.Eagerly, AppearanceSettings())

    private fun read(p: Preferences) = AppearanceSettings(
        themeMode = ThemeMode.entries.firstOrNull { it.name == p[Keys.Theme] } ?: ThemeMode.System,
        dynamicColor = p[Keys.Dynamic] ?: (Build.VERSION.SDK_INT >= 31),
        dynamicIcon = p[Keys.DynamicIcon] ?: false,
        monochrome = p[Keys.Mono] ?: false,
        avatarShape = AvatarShape.entries.firstOrNull { it.name == p[Keys.Shape] } ?: AvatarShape.Circle,
        avatarCorner = p[Keys.Corner] ?: 50,
        hidePhone = p[Keys.HidePhone] ?: true,
        phoneDetails = p[Keys.PhoneDetails] ?: true,
        navBarStyle = NavBarStyle.entries.firstOrNull { it.name == p[Keys.NavBar] } ?: NavBarStyle.Expressive,
        iconPack = IconPack.entries.firstOrNull { it.name == p[Keys.Pack] } ?: IconPack.Material,
        folderTitle = FolderTitle.entries.firstOrNull { it.name == p[Keys.FolderTitleMode] } ?: FolderTitle.NamesAndIcons,
        capsuleInset = p[Keys.CapsuleInset] ?: 12,
        capsuleSpacing = p[Keys.CapsuleSpacing] ?: 4,
        capsuleHeight = p[Keys.CapsuleHeight] ?: 64,
        navTabs = p[Keys.NavTabs]?.split(',')?.mapNotNull { name -> com.pixelgram.android.feature.main.MainTab.entries.firstOrNull { it.name == name } }
            ?.let { if (com.pixelgram.android.feature.main.MainTab.Chats in it) it else listOf(com.pixelgram.android.feature.main.MainTab.Chats) + it }
            ?: com.pixelgram.android.feature.main.MainTab.entries,
        navOrder = (p[Keys.NavOrder] ?: p[Keys.NavTabs])?.split(',')?.mapNotNull { name -> com.pixelgram.android.feature.main.MainTab.entries.firstOrNull { it.name == name } }
            ?.let { saved -> saved + com.pixelgram.android.feature.main.MainTab.entries.filterNot { it in saved } } ?: com.pixelgram.android.feature.main.MainTab.entries,
        drawerItems = p[Keys.DrawerItems]?.split(',')?.mapNotNull { name -> DrawerEntry.entries.firstOrNull { it.name == name } }
            ?.let { if (DrawerEntry.Settings in it) it else it + DrawerEntry.Settings } ?: DrawerEntry.entries,
        drawerOrder = p[Keys.DrawerOrder]?.split(',')?.mapNotNull { name -> DrawerEntry.entries.firstOrNull { it.name == name } }
            ?.let { saved -> saved + DrawerEntry.entries.filterNot { it in saved } } ?: DrawerEntry.entries,
        messageSeconds = p[Keys.K_messageSeconds] ?: false,
        exactNumbers = p[Keys.K_exactNumbers] ?: false,
        zalgoFilter = p[Keys.K_zalgoFilter] ?: false,
        hideAllChats = p[Keys.K_hideAllChats] ?: false,
        showDrawer = p[Keys.K_drawer] ?: true,
        typingBubble = p[Keys.K_typingBubble] ?: true,
        keepAlive = p[Keys.K_keepAlive] ?: false,
        showBottomBar = p[Keys.K_bottomBar] ?: true,
        bubbleRadius = p[Keys.K_bubbleRadius] ?: 18,
        bubbleTailRadius = p[Keys.K_bubbleTailRadius] ?: 4,
        messageFontSize = p[Keys.K_messageFontSize] ?: 16,
        doubleTapEmoji = p[Keys.K_doubleTapEmoji] ?: "❤",
    )

    fun update(change: (AppearanceSettings) -> AppearanceSettings) {
        scope.launch {
            context.store.edit { p ->
                val next = change(read(p))
                p[Keys.Theme] = next.themeMode.name
                p[Keys.Dynamic] = next.dynamicColor
                p[Keys.DynamicIcon] = next.dynamicIcon
                p[Keys.Mono] = next.monochrome
                p[Keys.Shape] = next.avatarShape.name
                p[Keys.Corner] = next.avatarCorner
                p[Keys.HidePhone] = next.hidePhone
                p[Keys.PhoneDetails] = next.phoneDetails
                p[Keys.NavBar] = next.navBarStyle.name
                p[Keys.Pack] = next.iconPack.name
                p[Keys.FolderTitleMode] = next.folderTitle.name
                p[Keys.CapsuleInset] = next.capsuleInset
                p[Keys.CapsuleSpacing] = next.capsuleSpacing
                p[Keys.CapsuleHeight] = next.capsuleHeight
                p[Keys.NavTabs] = next.navTabs.joinToString(",") { it.name }
                p[Keys.NavOrder] = next.navOrder.joinToString(",") { it.name }
                p[Keys.DrawerItems] = next.drawerItems.joinToString(",") { it.name }
                p[Keys.DrawerOrder] = next.drawerOrder.joinToString(",") { it.name }
                p[Keys.K_messageSeconds] = next.messageSeconds
                p[Keys.K_exactNumbers] = next.exactNumbers
                p[Keys.K_zalgoFilter] = next.zalgoFilter
                p[Keys.K_hideAllChats] = next.hideAllChats
                p[Keys.K_drawer] = next.showDrawer
                p[Keys.K_typingBubble] = next.typingBubble
                p[Keys.K_keepAlive] = next.keepAlive
                p[Keys.K_bottomBar] = next.showBottomBar
                p[Keys.K_bubbleRadius] = next.bubbleRadius
                p[Keys.K_bubbleTailRadius] = next.bubbleTailRadius
                p[Keys.K_messageFontSize] = next.messageFontSize
                p[Keys.K_doubleTapEmoji] = next.doubleTapEmoji
            }
            applyLauncherIcon(state.value.dynamicIcon)
        }
    }

    private fun applyLauncherIcon(dynamic: Boolean) {
        val pm = context.packageManager
        fun set(alias: String, enabled: Boolean) {
            val component = ComponentName(context, "com.pixelgram.android.$alias")
            val wanted = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            if (pm.getComponentEnabledSetting(component) != wanted) pm.setComponentEnabledSetting(component, wanted, PackageManager.DONT_KILL_APP)
        }
        set("LauncherDynamic", dynamic)
        set("LauncherDefault", !dynamic)
    }
}
