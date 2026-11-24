package com.pixelgram.android.feature.pixel

import com.pixelgram.android.ui.components.settingLink
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.SystemUpdate
import com.pixelgram.android.settings.DrawerEntry
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Download
import kotlinx.coroutines.launch
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.pixelgram.android.ui.components.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.sp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.BuildConfig
import com.pixelgram.android.R
import com.pixelgram.android.nav.Navigator
import com.pixelgram.android.nav.PixelSettingsKey
import com.pixelgram.android.settings.AppearanceSettings
import com.pixelgram.android.settings.ThemeMode
import com.pixelgram.android.settings.NavBarStyle
import com.pixelgram.android.feature.main.MainTab
import com.pixelgram.android.ui.components.PixelSwitch
import com.pixelgram.android.ui.components.segmentPosition
import com.pixelgram.android.ui.components.index
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.runtime.LaunchedEffect
import com.pixelgram.android.settings.FolderTitle
import com.pixelgram.android.ui.components.FolderChips
import com.pixelgram.android.ui.components.FolderTab
import com.pixelgram.android.ui.components.segmentShape
import com.pixelgram.android.ui.components.SegmentPosition
import com.pixelgram.android.ui.components.SettingsGroup
import com.pixelgram.android.ui.components.SettingsItem
import com.pixelgram.android.ui.components.SettingsSwitch
import com.pixelgram.android.ui.theme.AvatarShape
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelSettingsScreen(graph: AppGraph, page: String, navigator: Navigator) {
    val appearance by graph.appearance.state.collectAsState()
    val title = when (page) {
        "general" -> stringResource(R.string.pixel_general)
        "appearance" -> stringResource(R.string.pixel_appearance)
        "chats" -> stringResource(R.string.pixel_chats)
        "about" -> stringResource(R.string.pixel_about)
        else -> stringResource(R.string.pixel_settings)
    }
    val collapse = androidx.compose.material3.TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(collapse.nestedScrollConnection),
        topBar = {
            androidx.compose.material3.MediumFlexibleTopAppBar(
                scrollBehavior = collapse,
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            when (page) {
                "appearance" -> AppearancePage(appearance) { change -> graph.appearance.update(change) }
                "general" -> GeneralPage(appearance) { change -> graph.appearance.update(change) }
                "chats" -> ChatsPage(appearance, { navigator.open(it) }) { change -> graph.appearance.update(change) }
                "about" -> {
                    AboutPage()
                    BackupSection()
                }
                else -> HomePage { navigator.open(PixelSettingsKey(it)) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PixelLogo(size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier.size(size).clip(MaterialShapes.Cookie12Sided.toShape()).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_pixelgram),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

@Composable
internal fun HomePage(open: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        PixelLogo(104.dp)
        Spacer(Modifier.height(16.dp))
        Text("pixelGram", style = MaterialTheme.typography.headlineMediumEmphasized, color = MaterialTheme.colorScheme.onSurface)
        Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    SettingsGroup(stringResource(R.string.pixel_categories)) {
        SettingsItem(stringResource(R.string.pixel_general), SegmentPosition.Top, stringResource(R.string.pixel_general_sub), Icons.Rounded.Tune, onClick = { open("general") }, link = "pg://pixel/general")
        SettingsItem(stringResource(R.string.pixel_appearance), SegmentPosition.Middle, stringResource(R.string.pixel_appearance_sub), Icons.Rounded.Palette, onClick = { open("appearance") }, link = "pg://pixel/appearance")
        SettingsItem(stringResource(R.string.pixel_chats), SegmentPosition.Middle, stringResource(R.string.pixel_chats_sub), Icons.Rounded.Chat, onClick = { open("chats") }, link = "pg://pixel/chats")
        SettingsItem(stringResource(R.string.pixel_about), SegmentPosition.Bottom, stringResource(R.string.pixel_about_sub), Icons.Rounded.Info, onClick = { open("about") }, link = "pg://pixel/about")
    }
}

@Composable
internal fun GeneralPage(settings: AppearanceSettings, update: ((AppearanceSettings) -> AppearanceSettings) -> Unit) {
    SettingsGroup(stringResource(R.string.pixel_phones)) {
        SettingsSwitch(
            stringResource(R.string.pixel_phone_details),
            settings.phoneDetails,
            { value -> update { it.copy(phoneDetails = value) } },
            SegmentPosition.Single,
            stringResource(R.string.pixel_phone_details_sub),
            Icons.Rounded.Phone,
        )
    }
}

@Composable
internal fun ChatsPage(settings: AppearanceSettings, open: (androidx.navigation3.runtime.NavKey) -> Unit = {}, update: ((AppearanceSettings) -> AppearanceSettings) -> Unit) {
    var preview by remember { mutableStateOf(0) }
    val sample = listOf(
        FolderTab(0, stringResource(R.string.folder_all), "All", 15, 3),
        FolderTab(1, stringResource(R.string.pixel_folder_sample_private), "Private", 2, 0),
        FolderTab(2, stringResource(R.string.pixel_folder_sample_work), "Work"),
        FolderTab(3, stringResource(R.string.pixel_folder_sample_channels), "Channels", 1, 1),
    )
    SettingsGroup(stringResource(R.string.pixel_bubbles)) {
        BubblePreview(settings)
        CapsuleSlider(stringResource(R.string.pixel_bubble_radius), settings.bubbleRadius, 4..28, SegmentPosition.Middle, default = SliderDefaults.bubbleRadius) { v -> update { it.copy(bubbleRadius = v) } }
        CapsuleSlider(stringResource(R.string.pixel_bubble_tail), settings.bubbleTailRadius, 0..18, SegmentPosition.Middle, default = SliderDefaults.bubbleTailRadius) { v -> update { it.copy(bubbleTailRadius = v) } }
        CapsuleSlider(stringResource(R.string.pixel_font_size), settings.messageFontSize, 12..24, SegmentPosition.Bottom, "sp", default = SliderDefaults.messageFontSize) { v -> update { it.copy(messageFontSize = v) } }
    }
    SettingsGroup {
        SettingsItem(stringResource(R.string.stickers_title), SegmentPosition.Single, stringResource(R.string.stickers_sub), androidx.compose.material.icons.Icons.Rounded.EmojiEmotions, onClick = { open(com.pixelgram.android.nav.StickersKey) })
    }
    ComposerSettings(settings, update)
    CursorSettings(settings, update)
    SettingsGroup(stringResource(R.string.pixel_messages)) {
        SettingsSwitch(stringResource(R.string.pixel_seconds), settings.messageSeconds, { v -> update { it.copy(messageSeconds = v) } }, SegmentPosition.Top, "12:34 → 12:34:56")
        SettingsSwitch(stringResource(R.string.pixel_zalgo), settings.zalgoFilter, { v -> update { it.copy(zalgoFilter = v) } }, SegmentPosition.Middle, stringResource(R.string.pixel_zalgo_sub))
        SettingsSwitch(stringResource(R.string.pixel_typing_bubble), settings.typingBubble, { v -> update { it.copy(typingBubble = v) } }, SegmentPosition.Middle, stringResource(R.string.pixel_typing_bubble_sub))
        SettingsSwitch(stringResource(R.string.pixel_inline_math), settings.inlineMath, { v -> update { it.copy(inlineMath = v) } }, SegmentPosition.Middle, stringResource(R.string.pixel_inline_math_sub))
        SettingsItem(stringResource(R.string.pixel_double_tap), SegmentPosition.Bottom, stringResource(R.string.pixel_double_tap_sub), onClick = { open(com.pixelgram.android.nav.QuickReactionKey) }) {
            com.pixelgram.android.feature.chat.ReactionGlyph(settings.doubleTapEmoji, 26.dp)
        }
    }
    SettingsGroup(stringResource(R.string.notify_settings)) {
        SettingsSwitch(stringResource(R.string.notify_keep_alive), settings.keepAlive, { v -> update { it.copy(keepAlive = v) } }, SegmentPosition.Single, stringResource(R.string.notify_keep_alive_sub))
    }
    SettingsGroup(stringResource(R.string.pixel_chat_list)) {
        SettingsSwitch(stringResource(R.string.pixel_exact_numbers), settings.exactNumbers, { v -> update { it.copy(exactNumbers = v) } }, SegmentPosition.Top, "999+ → 1234")
        SettingsSwitch(stringResource(R.string.pixel_archive_pull), settings.archivePull, { v -> update { it.copy(archivePull = v) } }, SegmentPosition.Bottom, stringResource(R.string.pixel_archive_pull_sub))
    }
    SettingsGroup(stringResource(R.string.pixel_folder_title)) {
        androidx.compose.material3.Card(
            shape = androidx.compose.material3.ListItemDefaults.segmentedShapes(0, 3).shape,
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            FolderChips(sample, preview, settings.folderTitle, onSelect = { preview = it }, modifier = Modifier.padding(vertical = 10.dp))
        }
        val modes = FolderTitle.entries
        modes.forEachIndexed { index, mode ->
            SettingsItem(
                title = stringResource(
                    when (mode) {
                        FolderTitle.NamesAndIcons -> R.string.folder_title_names_icons
                        FolderTitle.Names -> R.string.folder_title_names
                        FolderTitle.Icons -> R.string.folder_title_icons
                        FolderTitle.ActiveName -> R.string.folder_title_active
                    },
                ),
                position = if (index == modes.lastIndex) SegmentPosition.Bottom else SegmentPosition.Middle,
                onClick = { update { it.copy(folderTitle = mode) } },
            ) {
                androidx.compose.material3.RadioButton(selected = settings.folderTitle == mode, onClick = { update { it.copy(folderTitle = mode) } })
            }
        }
    }
    androidx.compose.animation.AnimatedVisibility(settings.folderTitle == FolderTitle.Icons) {
        Text(
            stringResource(R.string.folder_title_icons_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AppearancePage(settings: AppearanceSettings, update: ((AppearanceSettings) -> AppearanceSettings) -> Unit) {
    FontSettings(settings, update)
    EmojiStyleSettings(settings, update)
    SettingsGroup(stringResource(R.string.pixel_theme)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            val modes = ThemeMode.entries
            modes.forEachIndexed { index, mode ->
                ToggleButton(
                    checked = settings.themeMode == mode,
                    onCheckedChange = { update { it.copy(themeMode = mode) } },
                    colors = androidx.compose.material3.ToggleButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        stringResource(
                            when (mode) {
                                ThemeMode.System -> R.string.theme_system
                                ThemeMode.Light -> R.string.theme_light
                                ThemeMode.Dark -> R.string.theme_dark
                            },
                        ),
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        SettingsSwitch(
            stringResource(R.string.pixel_dynamic_color),
            settings.dynamicColor,
            { value -> update { it.copy(dynamicColor = value, monochrome = if (value) false else it.monochrome) } },
            SegmentPosition.Top,
            stringResource(R.string.pixel_dynamic_color_sub),
            Icons.Rounded.Wallpaper,
            enabled = android.os.Build.VERSION.SDK_INT >= 31,
        )
        SettingsSwitch(
            stringResource(R.string.pixel_monochrome),
            settings.monochrome,
            { value -> update { it.copy(monochrome = value, dynamicColor = if (value) false else it.dynamicColor) } },
            SegmentPosition.Middle,
            stringResource(R.string.pixel_monochrome_sub),
            Icons.Rounded.Contrast,
        )
        SettingsSwitch(
            stringResource(R.string.pixel_dynamic_icon),
            settings.dynamicIcon,
            { value -> update { it.copy(dynamicIcon = value) } },
            SegmentPosition.Bottom,
            stringResource(R.string.pixel_dynamic_icon_sub),
            Icons.Rounded.Palette,
            enabled = android.os.Build.VERSION.SDK_INT >= 31,
        )
    }
    SettingsGroup(stringResource(R.string.pixel_nav_bar)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            val styles = NavBarStyle.entries
            styles.forEachIndexed { index, style ->
                ToggleButton(
                    checked = settings.navBarStyle == style,
                    onCheckedChange = { update { it.copy(navBarStyle = style) } },
                    colors = androidx.compose.material3.ToggleButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                    shapes = if (index == 0) ButtonGroupDefaults.connectedLeadingButtonShapes() else ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(if (style == NavBarStyle.Expressive) R.string.nav_bar_expressive else R.string.nav_bar_classic))
                }
            }
        }
    }
    SettingsGroup(stringResource(R.string.pixel_panels)) {
        SettingsSwitch(stringResource(R.string.pixel_show_drawer), settings.showDrawer, { v -> update { it.copy(showDrawer = v) } }, SegmentPosition.Top, stringResource(R.string.pixel_show_drawer_sub), enabled = settings.showBottomBar || !settings.showDrawer)
        SettingsSwitch(stringResource(R.string.pixel_show_bottom_bar), settings.showBottomBar, { v -> update { it.copy(showBottomBar = v) } }, SegmentPosition.Bottom, stringResource(R.string.pixel_show_bottom_bar_sub), enabled = settings.showDrawer || !settings.showBottomBar)
    }
    SettingsGroup(stringResource(R.string.pixel_icon_pack)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            val packs = com.pixelgram.android.settings.IconPack.entries
            packs.forEachIndexed { index, pack ->
                ToggleButton(
                    checked = settings.iconPack == pack,
                    onCheckedChange = { update { it.copy(iconPack = pack) } },
                    colors = androidx.compose.material3.ToggleButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        packs.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(pack.name, maxLines = 1)
                }
            }
        }
    }
    SettingsGroup(stringResource(R.string.pixel_nav_tabs)) {
        NavTabsEditor(settings.navTabs, settings.navOrder) { tabs, order -> update { it.copy(navTabs = tabs, navOrder = order) } }
    }
    androidx.compose.animation.AnimatedVisibility(settings.showDrawer) {
        SettingsGroup(stringResource(R.string.pixel_drawer_items)) {
            DrawerEditor(settings.drawerItems, settings.drawerOrder) { items, order -> update { it.copy(drawerItems = items, drawerOrder = order) } }
        }
    }
    androidx.compose.animation.AnimatedVisibility(settings.navBarStyle == NavBarStyle.Expressive) {
        SettingsGroup(stringResource(R.string.pixel_capsule)) {
            CapsuleSlider(stringResource(R.string.pixel_capsule_inset), settings.capsuleInset, 4..20, SegmentPosition.Top, default = SliderDefaults.capsuleInset) { v -> update { it.copy(capsuleInset = v) } }
            CapsuleSlider(stringResource(R.string.pixel_capsule_spacing), settings.capsuleSpacing, 0..16, SegmentPosition.Middle, default = SliderDefaults.capsuleSpacing) { v -> update { it.copy(capsuleSpacing = v) } }
            CapsuleSlider(stringResource(R.string.pixel_capsule_height), settings.capsuleHeight, 56..80, SegmentPosition.Bottom, default = SliderDefaults.capsuleHeight) { v -> update { it.copy(capsuleHeight = v) } }
        }
    }
    SettingsGroup(stringResource(R.string.pixel_avatars)) {
        var kind by remember { mutableStateOf<com.pixelgram.android.td.ChatKind?>(null) }
        val current = kind?.let { settings.kindShapes[it.name] } ?: settings.avatarShape
        AvatarKindPicker(settings, kind) { kind = it }
        AvatarShapePicker(current, settings.avatarCorner, kind, settings.kindShapes[kind?.name] != null, onReset = { update { s -> s.copy(kindShapes = s.kindShapes - kind!!.name) } }, last = current != AvatarShape.Custom) { shape ->
            val target = kind
            update { if (target == null) it.copy(avatarShape = shape) else it.copy(kindShapes = it.kindShapes + (target.name to shape)) }
        }
        androidx.compose.animation.AnimatedVisibility(
            current == AvatarShape.Custom,
            enter = androidx.compose.animation.expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + androidx.compose.animation.fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = androidx.compose.animation.shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + androidx.compose.animation.fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()),
        ) {
            androidx.compose.material3.Card(
                shape = androidx.compose.material3.ListItemDefaults.segmentedShapes(1, 2).shape,
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.padding(top = androidx.compose.material3.ListItemDefaults.SegmentedGap),
            ) {
                androidx.compose.foundation.layout.Column(Modifier.padding(vertical = 12.dp)) {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.RoundedCorner, null)
                        Text(stringResource(R.string.pixel_avatar_corner), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(start = 16.dp))
                        Text("${settings.avatarCorner}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Slider(
                        value = settings.avatarCorner.toFloat(),
                        onValueChange = { value -> update { it.copy(avatarCorner = value.roundToInt()) } },
                        valueRange = 0f..50f,
                        steps = 9,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
    SettingsGroup(stringResource(R.string.pixel_privacy)) {
        SettingsSwitch(
            stringResource(R.string.pixel_hide_phone),
            settings.hidePhone,
            { value -> update { it.copy(hidePhone = value) } },
            SegmentPosition.Single,
            stringResource(R.string.pixel_hide_phone_sub),
            Icons.Rounded.Phone,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AvatarKindPicker(settings: com.pixelgram.android.settings.AppearanceSettings, kind: com.pixelgram.android.td.ChatKind?, onSelect: (com.pixelgram.android.td.ChatKind?) -> Unit) {
    val entries = listOf<com.pixelgram.android.td.ChatKind?>(null) + com.pixelgram.android.ui.theme.AvatarKinds
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        items(entries.size) { i ->
            val entry = entries[i]
            val custom = entry?.let { settings.kindShapes[it.name] }
            androidx.compose.material3.FilterChip(
                selected = kind == entry,
                onClick = { onSelect(entry) },
                label = { Text(stringResource(kindLabel(entry))) },
                leadingIcon = {
                    androidx.compose.animation.AnimatedVisibility(
                        custom != null,
                        enter = androidx.compose.animation.scaleIn(MaterialTheme.motionScheme.fastSpatialSpec()) + androidx.compose.animation.expandHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()),
                        exit = androidx.compose.animation.scaleOut(MaterialTheme.motionScheme.fastSpatialSpec()) + androidx.compose.animation.shrinkHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()),
                    ) {
                        var last by remember { mutableStateOf(custom ?: AvatarShape.Circle) }
                        if (custom != null) last = custom
                        Box(Modifier.size(16.dp).clip(com.pixelgram.android.ui.theme.animatedAvatarShape(last, settings.avatarCorner)).background(MaterialTheme.colorScheme.primary))
                    }
                },
            )
        }
    }
}

private fun kindLabel(kind: com.pixelgram.android.td.ChatKind?): Int = when (kind) {
    null -> R.string.pixel_avatar_kind_all
    com.pixelgram.android.td.ChatKind.Bot -> R.string.pixel_avatar_kind_bots
    com.pixelgram.android.td.ChatKind.Group -> R.string.pixel_avatar_kind_groups
    com.pixelgram.android.td.ChatKind.Channel -> R.string.pixel_avatar_kind_channels
    else -> R.string.pixel_avatar_kind_private
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AvatarShapePicker(selected: AvatarShape, corner: Int, kind: com.pixelgram.android.td.ChatKind?, overridden: Boolean, onReset: () -> Unit, last: Boolean, onSelect: (AvatarShape) -> Unit) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (AvatarShape.entries.indexOf(selected) - 2).coerceAtLeast(0))
    val bottomRadius by androidx.compose.animation.core.animateDpAsState(if (last) 24.dp else 4.dp, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "picker-corner")
    androidx.compose.material3.Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp, 24.dp, bottomRadius, bottomRadius),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(com.pixelgram.android.ui.theme.animatedAvatarShape(selected, corner)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(androidx.compose.material.icons.Icons.Rounded.Face, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
            }
            androidx.compose.foundation.layout.Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(stringResource(kindLabel(kind)), style = MaterialTheme.typography.titleMedium)
                androidx.compose.animation.AnimatedContent(kind != null && !overridden, label = "kind-same") { same ->
                    Text(
                        stringResource(if (same) R.string.pixel_avatar_kind_same else if (kind == null) R.string.pixel_avatar_kind_all_sub else R.string.pixel_avatar_kind_own),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                kind != null && overridden,
                enter = androidx.compose.animation.scaleIn(MaterialTheme.motionScheme.fastSpatialSpec()) + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.scaleOut(MaterialTheme.motionScheme.fastSpatialSpec()) + androidx.compose.animation.fadeOut(),
            ) {
                androidx.compose.material3.FilledTonalIconButton(onClick = onReset) { Icon(androidx.compose.material.icons.Icons.Rounded.Refresh, stringResource(R.string.pixel_avatar_kind_reset)) }
            }
        }
        LazyRow(
            state = state,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(AvatarShape.entries, key = { it.name }) { shape ->
                val active = shape == selected
                val tileShape = shape.shape(28)
                androidx.compose.material3.FilledTonalIconToggleButton(
                    checked = active,
                    onCheckedChange = { onSelect(shape) },
                    shapes = androidx.compose.material3.IconToggleButtonShapes(tileShape, tileShape, tileShape),
                    modifier = Modifier.size(52.dp).semantics { contentDescription = shape.name },
                ) {
                    if (shape == AvatarShape.Custom) Icon(Icons.Rounded.RoundedCorner, null)
                }
            }
        }
    }
}

@Composable
internal fun AboutPage() {
    val uri = LocalUriHandler.current
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PixelLogo(88.dp)
        Text(stringResource(R.string.pixel_about_text), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    UpdateCard()
    SettingsGroup(stringResource(R.string.support_title)) {
        SettingsItem(stringResource(R.string.support_entry), SegmentPosition.Single, stringResource(R.string.support_entry_sub), Icons.Rounded.Favorite, onClick = { SupportProject.open = true })
    }
    SettingsGroup(stringResource(R.string.pixel_links)) {
        SettingsItem(stringResource(R.string.pixel_channel), SegmentPosition.Top, "@pixelgramclient", Icons.Rounded.Chat, onClick = { uri.openUri("https://t.me/pixelgramclient") })
        SettingsItem(stringResource(R.string.pixel_desktop), SegmentPosition.Middle, "pixelgram.jettax.xyz", Icons.Rounded.Language, onClick = { uri.openUri("https://pixelgram.jettax.xyz/") })
        SettingsItem(stringResource(R.string.pixel_source), SegmentPosition.Bottom, "GitHub", Icons.Rounded.Code, onClick = { uri.openUri("https://github.com/JettaXP") })
    }
    SettingsGroup(stringResource(R.string.pixel_info)) {
        SettingsItem(stringResource(R.string.pixel_version), SegmentPosition.Top, stringResource(R.string.update_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE), Icons.Rounded.Info)
        SettingsItem(stringResource(R.string.pixel_credits), SegmentPosition.Bottom, stringResource(R.string.pixel_credits_sub), Icons.Rounded.Palette)
    }
    ResetAllItem()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ResetAllItem() {
    val graph = com.pixelgram.android.LocalGraph.current
    val done = stringResource(R.string.reset_all_done)
    var confirm by remember { mutableStateOf(false) }
    SettingsGroup(null) {
        SettingsItem(
            stringResource(R.string.reset_all),
            SegmentPosition.Single,
            stringResource(R.string.reset_all_sub),
            Icons.Rounded.RestartAlt,
            iconBackground = MaterialTheme.colorScheme.errorContainer,
            iconTint = MaterialTheme.colorScheme.onErrorContainer,
            onClick = { confirm = true },
        )
    }
    if (confirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirm = false },
            icon = { Icon(Icons.Rounded.RestartAlt, null) },
            title = { Text(stringResource(R.string.reset_all_title)) },
            text = { Text(stringResource(R.string.reset_all_text)) },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        confirm = false
                        graph.appearance.update { AppearanceSettings() }
                        com.pixelgram.android.ui.components.Notice.show(done)
                    },
                    shapes = androidx.compose.material3.ButtonDefaults.shapes(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                ) { Text(stringResource(R.string.action_reset)) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun BackupSection() {
    val graph = com.pixelgram.android.LocalGraph.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var exporting by remember { mutableStateOf<java.io.File?>(null) }
    var importing by remember { mutableStateOf<java.io.File?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    val failed = stringResource(R.string.backup_failed, "backup")
    val notBackup = stringResource(R.string.backup_not_backup)
    val sentFormat = stringResource(R.string.backup_sent, "%s")
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val name = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                        if (c.moveToFirst()) c.getString(0) else null
                    } ?: "import.backup"
                    val target = java.io.File(context.cacheDir, name.substringAfterLast('/'))
                    context.contentResolver.openInputStream(uri)!!.use { input -> target.outputStream().use { input.copyTo(it) } }
                    target
                }.getOrNull()
            }
            when {
                file == null -> status = failed
                !com.pixelgram.android.settings.Backup.isBackup(file.name) -> status = notBackup
                else -> importing = file
            }
        }
    }
    SettingsGroup(stringResource(R.string.backup)) {
        SettingsItem(
            stringResource(R.string.backup_export),
            SegmentPosition.Top,
            stringResource(R.string.backup_export_sub),
            Icons.Rounded.Upload,
            onClick = {
                scope.launch {
                    exporting = com.pixelgram.android.settings.Backup.export(context, graph.appearance)
                    if (exporting == null) status = failed
                }
            },
        )
        SettingsItem(
            stringResource(R.string.backup_import_item),
            SegmentPosition.Bottom,
            stringResource(R.string.backup_import_sub),
            Icons.Rounded.Download,
            onClick = { picker.launch(arrayOf("*/*")) },
        )
    }
    Text(
        status ?: stringResource(R.string.backup_sub),
        style = MaterialTheme.typography.bodyMedium,
        color = if (status != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp),
    )
    exporting?.let { file ->
        com.pixelgram.android.ui.components.ChatPickerSheet(
            onPick = { chatId ->
                exporting = null
                scope.launch {
                    runCatching { com.pixelgram.android.ui.components.sendDocument(graph, chatId, file) }
                        .onSuccess { status = sentFormat.replace("%s", file.name) }
                        .onFailure { status = failed }
                }
            },
            onDismiss = { exporting = null },
        )
    }
    importing?.let { file ->
        com.pixelgram.android.ui.components.BackupImportDialog(file) { result ->
            importing = null
            if (result != null) status = result
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CapsuleSlider(title: String, value: Int, range: IntRange, position: SegmentPosition, unit: String = "dp", default: Int? = null, onChange: (Int) -> Unit) {
    val (index, count) = position.index()
    androidx.compose.material3.SegmentedListItem(
        shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(index, count),
        colors = com.pixelgram.android.ui.components.segmentedColors(),
        modifier = Modifier.settingLink(title),
        trailingContent = { SliderValue("$value $unit", value, default, onChange) },
        supportingContent = {
            Slider(
                value = value.toFloat(),
                onValueChange = { onChange(it.roundToInt()) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = range.last - range.first - 1,
            )
        },
    ) {
        Text(title)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun <T : Any> ReorderEditor(
    enabled: List<T>,
    saved: List<T>,
    locked: T,
    label: @Composable (T) -> String,
    onChange: (List<T>, List<T>) -> Unit,
) {
    val order = remember { androidx.compose.runtime.mutableStateListOf<T>().apply { addAll(saved) } }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val gap = with(density) { androidx.compose.material3.ListItemDefaults.SegmentedGap.toPx() }
    var rowHeight by remember { mutableStateOf(with(density) { 72.dp.toPx() }) }
    var dragging by remember { mutableStateOf<T?>(null) }
    var offset by remember { mutableStateOf(0f) }
    val latestEnabled by androidx.compose.runtime.rememberUpdatedState(enabled)
    val commit = { onChange(order.filter { it in latestEnabled || it == locked }, order.toList()) }
    val spring = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntOffset>()
    Column(verticalArrangement = Arrangement.spacedBy(androidx.compose.material3.ListItemDefaults.SegmentedGap)) {
        order.forEachIndexed { index, item ->
            androidx.compose.runtime.key(item) {
                val isDragged = dragging == item
                val on = item in enabled
                val lift by androidx.compose.animation.core.animateFloatAsState(if (isDragged) 1f else 0f, label = "lift")
                androidx.compose.material3.SegmentedListItem(
                    shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(index, order.size),
                    colors = if (isDragged) {
                        com.pixelgram.android.ui.components.segmentedColors(MaterialTheme.colorScheme.surfaceContainerHighest)
                    } else {
                        com.pixelgram.android.ui.components.segmentedColors()
                    },
                    modifier = Modifier
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragged) offset else 0f
                            shadowElevation = lift * 12f
                            scaleX = 1f + lift * 0.02f
                            scaleY = 1f + lift * 0.02f
                        }
                        .onSizeChanged { rowHeight = it.height.toFloat() + gap },
                    leadingContent = {
                        Icon(
                            Icons.Rounded.DragHandle,
                            stringResource(R.string.pixel_nav_drag),
                            Modifier
                                .size(40.dp)
                                .padding(8.dp)
                                .pointerInput(item) {
                                    detectVerticalDragGestures(
                                        onDragStart = {
                                            dragging = item
                                            offset = 0f
                                        },
                                        onDragEnd = {
                                            dragging = null
                                            offset = 0f
                                            commit()
                                        },
                                        onDragCancel = {
                                            dragging = null
                                            offset = 0f
                                            commit()
                                        },
                                    ) { change, amount ->
                                        change.consume()
                                        offset += amount
                                        val from = order.indexOf(item)
                                        if (offset > rowHeight / 2 && from < order.lastIndex) {
                                            order.add(from + 1, order.removeAt(from))
                                            offset -= rowHeight
                                        } else if (offset < -rowHeight / 2 && from > 0) {
                                            order.add(from - 1, order.removeAt(from))
                                            offset += rowHeight
                                        }
                                    }
                                },
                        )
                    },
                    trailingContent = {
                        PixelSwitch(
                            checked = on,
                            enabled = item != locked,
                            onCheckedChange = { value ->
                                val next = if (value) enabled + item else enabled - item
                                onChange(order.filter { it in next }, order.toList())
                            },
                        )
                    },
                ) {
                    Text(label(item))
                }
            }
        }
    }
}

@Composable
private fun NavTabsEditor(enabled: List<MainTab>, saved: List<MainTab>, onChange: (List<MainTab>, List<MainTab>) -> Unit) =
    ReorderEditor(enabled, saved, MainTab.Chats, { stringResource(it.label) }, onChange)

@Composable
private fun DrawerEditor(enabled: List<DrawerEntry>, saved: List<DrawerEntry>, onChange: (List<DrawerEntry>, List<DrawerEntry>) -> Unit) =
    ReorderEditor(enabled, saved, DrawerEntry.Settings, { stringResource(it.label) }, onChange)

private val DrawerEntry.label: Int
    get() = when (this) {
        DrawerEntry.Saved -> R.string.drawer_saved
        DrawerEntry.Archive -> R.string.drawer_archive
        DrawerEntry.Contacts -> R.string.tab_contacts
        DrawerEntry.Pixel -> R.string.pixel_settings
        DrawerEntry.Settings -> R.string.tab_settings
        DrawerEntry.Theme -> R.string.drawer_theme
        DrawerEntry.Lock -> R.string.passcode_lock_now
    }

private val MainTab.label: Int
    get() = when (this) {
        MainTab.Chats -> R.string.tab_chats
        MainTab.Contacts -> R.string.tab_contacts
        MainTab.Profile -> R.string.tab_profile
        MainTab.Settings -> R.string.tab_settings
    }

@Composable
private fun BubblePreview(settings: AppearanceSettings) {
    val big = settings.bubbleRadius.dp
    val small = settings.bubbleTailRadius.dp
    androidx.compose.material3.Card(
        shape = androidx.compose.material3.ListItemDefaults.segmentedShapes(0, 3).shape,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val style = MaterialTheme.typography.bodyLarge.copy(fontSize = settings.messageFontSize.sp)
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(big, big, big, small),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) { Text(stringResource(R.string.pixel_preview_in), style = style, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(big, small, big, big),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) { Text(stringResource(R.string.pixel_preview_out), style = style, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(big, small, big, big),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        com.pixelgram.android.util.messageTime((System.currentTimeMillis() / 1000).toInt(), settings.messageSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val graph = com.pixelgram.android.LocalGraph.current
    val updater = com.pixelgram.android.update.Updater
    val state = updater.state
    androidx.compose.runtime.LaunchedEffect(Unit) { if (state is com.pixelgram.android.update.UpdateState.Idle) updater.check(graph.scope) }
    androidx.compose.material3.Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(20.dp).animateContentSize(androidx.compose.animation.core.spring(dampingRatio = 1f, stiffness = 500f)), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.animation.AnimatedContent(
                targetState = state,
                contentKey = { it::class },
                transitionSpec = {
                    (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220, delayMillis = 60)) + androidx.compose.animation.scaleIn(androidx.compose.animation.core.tween(220, delayMillis = 60), initialScale = 0.96f)) togetherWith
                        androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120)) using androidx.compose.animation.SizeTransform(clip = false) { _, _ -> androidx.compose.animation.core.spring(dampingRatio = 1f, stiffness = 500f) }
                },
                label = "update-header",
            ) { state ->
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (state is com.pixelgram.android.update.UpdateState.Latest) Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.size(30.dp).clip(androidx.compose.material3.MaterialShapes.Cookie9Sided.toShape()).background(androidx.compose.ui.graphics.Color(0xFF3DDC84)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Check, null, tint = androidx.compose.ui.graphics.Color(0xFF0B2A12), modifier = Modifier.size(20.dp)) }
                    } else Box(
                        Modifier.size(48.dp).clip(androidx.compose.material3.MaterialShapes.Cookie9Sided.toShape()).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (state) {
                            is com.pixelgram.android.update.UpdateState.Checking -> androidx.compose.material3.LoadingIndicator(Modifier.size(32.dp))
                            else -> Icon(
                                when (state) {
                                    is com.pixelgram.android.update.UpdateState.Latest -> Icons.Rounded.CheckCircle
                                    is com.pixelgram.android.update.UpdateState.Failed -> Icons.Rounded.ErrorOutline
                                    else -> Icons.Rounded.SystemUpdate
                                },
                                null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        val title = when (state) {
                            is com.pixelgram.android.update.UpdateState.Idle, is com.pixelgram.android.update.UpdateState.Checking -> stringResource(R.string.update_checking)
                            is com.pixelgram.android.update.UpdateState.Latest -> stringResource(R.string.update_latest)
                            is com.pixelgram.android.update.UpdateState.Available -> stringResource(R.string.update_available, state.version)
                            is com.pixelgram.android.update.UpdateState.Downloading -> stringResource(R.string.update_downloading, state.version)
                            is com.pixelgram.android.update.UpdateState.Ready -> stringResource(R.string.update_ready, state.version)
                            is com.pixelgram.android.update.UpdateState.Failed -> stringResource(R.string.update_failed)
                        }
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        val sub = when (state) {
                            is com.pixelgram.android.update.UpdateState.Available -> if (state.size > 0) "%.1f MB".format(state.size / 1048576.0) else null
                            is com.pixelgram.android.update.UpdateState.Downloading -> "${(state.progress * 100).toInt()}%"
                            is com.pixelgram.android.update.UpdateState.Failed -> state.message
                            else -> stringResource(R.string.update_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
                        }
                        if (sub != null) Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (state is com.pixelgram.android.update.UpdateState.Available && state.notes.isNotBlank()) {
                androidx.compose.material3.Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Text(state.notes, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(12.dp))
                }
            }
            if (state is com.pixelgram.android.update.UpdateState.Downloading) {
                androidx.compose.material3.LinearWavyProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                when (state) {
                    is com.pixelgram.android.update.UpdateState.Available -> androidx.compose.material3.Button(
                        onClick = { updater.download(context, graph.scope) },
                        shapes = androidx.compose.material3.ButtonDefaults.shapes(),
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.update_download)) }
                    is com.pixelgram.android.update.UpdateState.Downloading -> androidx.compose.material3.OutlinedButton(
                        onClick = { updater.cancel() },
                        shapes = androidx.compose.material3.ButtonDefaults.shapes(),
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.action_cancel)) }
                    is com.pixelgram.android.update.UpdateState.Ready -> androidx.compose.material3.Button(
                        onClick = { updater.install(context, state.file) },
                        shapes = androidx.compose.material3.ButtonDefaults.shapes(),
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.update_install)) }
                    else -> androidx.compose.material3.FilledTonalButton(
                        onClick = { updater.check(graph.scope) },
                        enabled = state !is com.pixelgram.android.update.UpdateState.Checking,
                        shapes = androidx.compose.material3.ButtonDefaults.shapes(),
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.update_check)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CursorSettings(settings: AppearanceSettings, update: ((AppearanceSettings) -> AppearanceSettings) -> Unit) {
    SettingsGroup(stringResource(R.string.cursor_title)) {
        SettingsSwitch(stringResource(R.string.cursor_animated), settings.liquidCursor, { v -> update { it.copy(liquidCursor = v) } }, if (settings.liquidCursor) SegmentPosition.Top else SegmentPosition.Single, stringResource(R.string.cursor_animated_sub))
        androidx.compose.animation.AnimatedVisibility(settings.liquidCursor) {
            Column(verticalArrangement = Arrangement.spacedBy(androidx.compose.material3.ListItemDefaults.SegmentedGap)) {
                androidx.compose.material3.SegmentedListItem(
                    shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(1, 3),
                    colors = com.pixelgram.android.ui.components.segmentedColors(),
                    supportingContent = {
                        Column {
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(androidx.compose.material3.ButtonGroupDefaults.ConnectedSpaceBetween)) {
                                listOf(false to R.string.cursor_liquid, true to R.string.cursor_kitty).forEachIndexed { index, (kitty, label) ->
                                    androidx.compose.material3.ToggleButton(
                                        checked = settings.kittyCursor == kitty,
                                        onCheckedChange = { update { s -> s.copy(kittyCursor = kitty) } },
                                        colors = androidx.compose.material3.ToggleButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                                        shapes = if (index == 0) androidx.compose.material3.ButtonGroupDefaults.connectedLeadingButtonShapes() else androidx.compose.material3.ButtonGroupDefaults.connectedTrailingButtonShapes(),
                                        modifier = Modifier.weight(1f),
                                    ) { Text(stringResource(label)) }
                                }
                            }
                            Text(stringResource(if (settings.kittyCursor) R.string.cursor_kitty_sub else R.string.cursor_liquid_sub), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                        }
                    },
                ) { Text(stringResource(R.string.cursor_style)) }
                if (!settings.kittyCursor) {
                    SettingsSwitch(stringResource(R.string.cursor_smooth), settings.smoothCursor, { v -> update { it.copy(smoothCursor = v) } }, SegmentPosition.Middle, stringResource(R.string.cursor_smooth_sub))
                    CursorSlider(stringResource(R.string.cursor_stretch), settings.cursorStretch, 0..100, "", default = SliderDefaults.cursorStretch) { v -> update { it.copy(cursorStretch = v) } }
                    CursorSlider(stringResource(R.string.cursor_handle), settings.handleStretch, 0..100, "%", default = SliderDefaults.handleStretch) { v -> update { it.copy(handleStretch = v) } }
                    CursorSlider(stringResource(R.string.cursor_pull), settings.sidePull, 0..100, "%", default = SliderDefaults.sidePull) { v -> update { it.copy(sidePull = v) } }
                    CursorSlider(stringResource(R.string.cursor_speed), settings.cursorSpeed, 1..100, "", default = SliderDefaults.cursorSpeed) { v -> update { it.copy(cursorSpeed = v) } }
                } else {
                    CursorSlider(stringResource(R.string.cursor_trail_front), settings.trailFast, 20..500, " ms", 10, default = SliderDefaults.trailFast) { v -> update { it.copy(trailFast = v) } }
                    CursorSlider(stringResource(R.string.cursor_trail_end), settings.trailSlow, 50..1000, " ms", 10, default = SliderDefaults.trailSlow) { v -> update { it.copy(trailSlow = v) } }
                    CursorSlider(stringResource(R.string.cursor_trail_start), settings.trailThreshold, 0..10, "", default = SliderDefaults.trailThreshold) { v -> update { it.copy(trailThreshold = v) } }
                }
                SettingsSwitch(stringResource(R.string.cursor_blink), settings.cursorBlink, { v -> update { it.copy(cursorBlink = v) } }, SegmentPosition.Middle)
                CursorSlider(stringResource(R.string.cursor_width), settings.cursorWidth, 1..6, " dp", default = SliderDefaults.cursorWidth) { v -> update { it.copy(cursorWidth = v) } }
                CursorSlider(stringResource(R.string.cursor_blink_cycle), settings.blinkCycle, 300..2000, " ms", 100, default = SliderDefaults.blinkCycle) { v -> update { it.copy(blinkCycle = v) } }
                CursorSlider(stringResource(R.string.cursor_blink_smooth), settings.blinkSmoothness, 0..100, "%", last = true, default = SliderDefaults.blinkSmoothness) { v -> update { it.copy(blinkSmoothness = v) } }
            }
        }
    }
}

@Composable
private fun CursorSlider(title: String, value: Int, range: IntRange, unit: String, step: Int = 1, last: Boolean = false, default: Int? = null, onChange: (Int) -> Unit) {
    androidx.compose.material3.SegmentedListItem(
        shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(if (last) 2 else 1, 3),
        colors = com.pixelgram.android.ui.components.segmentedColors(),
        modifier = Modifier.settingLink(title),
        trailingContent = { SliderValue("$value$unit", value, default, onChange) },
        supportingContent = {
            Slider(
                value = value.toFloat(),
                onValueChange = { onChange(((it / step).roundToInt() * step).coerceIn(range.first, range.last)) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
            )
        },
    ) { Text(title) }
}

private val SliderDefaults = com.pixelgram.android.settings.AppearanceSettings()

@Composable
private fun SliderValue(label: String, value: Int, default: Int?, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            androidx.compose.animation.AnimatedVisibility(default != null && value != default, enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(), exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()) {
                IconButton(onClick = { default?.let(onChange) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.RestartAlt, stringResource(R.string.action_reset), modifier = Modifier.size(20.dp))
                }
            }
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ComposerSettings(settings: AppearanceSettings, update: ((AppearanceSettings) -> AppearanceSettings) -> Unit) {
    val slots = com.pixelgram.android.feature.chat.ComposerSlots.of(settings)
    val telegram = settings.composerStyle == "telegram"
    SettingsGroup(stringResource(R.string.composer_title)) {
        androidx.compose.material3.Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            androidx.compose.foundation.layout.Column {
                androidx.compose.foundation.layout.Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(androidx.compose.material3.ButtonGroupDefaults.ConnectedSpaceBetween),
                ) {
                    val options = listOf("messages" to R.string.composer_style_messages, "telegram" to R.string.composer_style_telegram)
                    options.forEachIndexed { index, (key, label) ->
                        val selected = settings.composerStyle == key
                        androidx.compose.material3.ToggleButton(
                            checked = selected,
                            onCheckedChange = { update { it.copy(composerStyle = key, composerLeft = "", composerRight = "", composerExtra = "") } },
                            shapes = if (index == 0) androidx.compose.material3.ButtonGroupDefaults.connectedLeadingButtonShapes() else androidx.compose.material3.ButtonGroupDefaults.connectedTrailingButtonShapes(),
                            colors = androidx.compose.material3.ToggleButtonDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                checkedContentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            modifier = Modifier.weight(1f).height(androidx.compose.material3.ButtonDefaults.MediumContainerHeight),
                        ) {
                            if (selected) {
                                Icon(androidx.compose.material.icons.Icons.Rounded.Check, null, Modifier.size(18.dp))
                                androidx.compose.foundation.layout.Spacer(Modifier.width(6.dp))
                            }
                            Text(stringResource(label), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        }
                    }
                }
                ComposerPreview(telegram, slots)
                androidx.compose.foundation.layout.Box(Modifier.padding(start = 16.dp, top = 8.dp).fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)))
                SlotRow(stringResource(R.string.composer_left), stringResource(R.string.composer_left_sub), slots.left, MaterialTheme.colorScheme.tertiary) { v -> update { it.copy(composerLeft = v) } }
                SlotRow(stringResource(R.string.composer_right), stringResource(R.string.composer_right_sub), slots.right, MaterialTheme.colorScheme.secondary) { v -> update { it.copy(composerRight = v) } }
                SlotRow(stringResource(R.string.composer_extra), stringResource(R.string.composer_extra_sub), slots.extra, MaterialTheme.colorScheme.primary) { v -> update { it.copy(composerExtra = v) } }
            }
        }
    }
}

@Composable
private fun ComposerPreview(telegram: Boolean, slots: com.pixelgram.android.feature.chat.ComposerSlots.Set) {
    val scheme = MaterialTheme.colorScheme
    val none = com.pixelgram.android.feature.chat.ComposerSlots.None
    androidx.compose.material3.Surface(
        color = scheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(12.dp).animateContentSize(),
        ) {
            androidx.compose.material3.Surface(
                shape = if (telegram) androidx.compose.foundation.shape.CircleShape else androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                color = if (telegram) scheme.surfaceContainerHigh else scheme.surfaceContainerHighest,
                modifier = Modifier.weight(1f).height(if (telegram) 48.dp else 56.dp),
            ) {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    if (slots.left != none) PreviewSlot(slots.left)
                    Text(stringResource(R.string.chat_message_hint), color = scheme.onSurfaceVariant, maxLines = 1, modifier = Modifier.weight(1f).padding(start = if (slots.left == none) 16.dp else 0.dp))
                    if (slots.right != none) PreviewSlot(slots.right)
                    if (slots.extra != none) PreviewSlot(slots.extra)
                }
            }
            androidx.compose.foundation.layout.Box(
                Modifier.size(if (telegram) 48.dp else 56.dp)
                    .clip(if (telegram) androidx.compose.foundation.shape.CircleShape else androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
                    .background(scheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                if (telegram) Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_pixelgram), null, Modifier.size(24.dp), tint = scheme.onPrimary)
                else Icon(androidx.compose.material.icons.Icons.Rounded.Mic, null, tint = scheme.onPrimary)
            }
        }
    }
}

@Composable
private fun PreviewSlot(kind: String) {
    androidx.compose.foundation.layout.Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Icon(com.pixelgram.android.feature.chat.ComposerSlots.icon(kind), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SlotRow(title: String, subtitle: String, value: String, tint: androidx.compose.ui.graphics.Color, onPick: (String) -> Unit) {
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { open = true }.padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        androidx.compose.foundation.layout.Box(Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(com.pixelgram.android.feature.chat.ComposerSlots.icon(value), null, tint = tint)
        }
        androidx.compose.foundation.layout.Spacer(Modifier.width(16.dp))
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(com.pixelgram.android.feature.chat.ComposerSlots.label(value)), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Icon(androidx.compose.material.icons.Icons.Rounded.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.primary)
            }
            androidx.compose.material3.DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.heightIn(max = 360.dp),
            ) {
                com.pixelgram.android.feature.chat.ComposerSlots.All.forEach { kind ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(stringResource(com.pixelgram.android.feature.chat.ComposerSlots.label(kind))) },
                        leadingIcon = { Icon(com.pixelgram.android.feature.chat.ComposerSlots.icon(kind), null) },
                        trailingIcon = if (kind == value) ({ Icon(androidx.compose.material.icons.Icons.Rounded.Check, null, Modifier.size(20.dp)) }) else null,
                        onClick = {
                            open = false
                            onPick(kind)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FontSettings(settings: AppearanceSettings, update: ((AppearanceSettings) -> AppearanceSettings) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val failed = stringResource(R.string.font_import_failed)
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (com.pixelgram.android.ui.theme.AppFonts.importFont(context, uri)) update { it.copy(appFont = com.pixelgram.android.ui.theme.AppFonts.CustomKey, fontStamp = it.fontStamp + 1) }
        else android.widget.Toast.makeText(context, failed, android.widget.Toast.LENGTH_SHORT).show()
    }
    val options = listOf(
        com.pixelgram.android.ui.theme.AppFonts.GoogleSansKey to R.string.font_google_sans,
        com.pixelgram.android.ui.theme.AppFonts.FlexKey to R.string.font_google_sans_flex,
        com.pixelgram.android.ui.theme.AppFonts.SystemKey to R.string.font_system,
        com.pixelgram.android.ui.theme.AppFonts.CustomKey to R.string.font_custom,
    )
    SettingsGroup(stringResource(R.string.font_title)) {
        options.forEachIndexed { index, (key, label) ->
            val family = androidx.compose.runtime.remember(key, settings.fontStamp) {
                com.pixelgram.android.ui.theme.AppFonts.family(context, key, com.pixelgram.android.ui.theme.AppFonts.EmojiSystem)
            }
            val selected = settings.appFont == key
            SettingsItem(
                stringResource(label),
                segmentPosition(index, options.size),
                if (key == com.pixelgram.android.ui.theme.AppFonts.CustomKey) stringResource(R.string.font_custom_sub) else null,
                onClick = {
                    if (key == com.pixelgram.android.ui.theme.AppFonts.CustomKey) picker.launch(arrayOf("font/*", "application/x-font-ttf", "application/x-font-otf", "application/octet-stream"))
                    else update { it.copy(appFont = key) }
                },
                headlineContent = { Text(stringResource(label), fontFamily = family, style = MaterialTheme.typography.bodyLarge) },
            ) {
                androidx.compose.material3.RadioButton(selected = selected, onClick = null)
            }
        }
    }
}

@Composable
private fun EmojiStyleSettings(settings: AppearanceSettings, update: ((AppearanceSettings) -> AppearanceSettings) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val progress by com.pixelgram.android.ui.theme.AppFonts.progress.collectAsState()
    val failed = stringResource(R.string.emoji_style_failed)
    var stamp by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val options = listOf(
        com.pixelgram.android.ui.theme.AppFonts.EmojiSystem to R.string.emoji_style_system,
        com.pixelgram.android.ui.theme.AppFonts.EmojiNoto to R.string.emoji_style_noto,
        com.pixelgram.android.ui.theme.AppFonts.EmojiNoto3D to R.string.emoji_style_noto3d,
        com.pixelgram.android.ui.theme.AppFonts.EmojiApple to R.string.emoji_style_apple,
    )
    SettingsGroup(stringResource(R.string.emoji_style_title)) {
        options.forEachIndexed { index, (key, label) ->
            val ready = androidx.compose.runtime.remember(key, stamp) { com.pixelgram.android.ui.theme.AppFonts.emojiReady(context, key) }
            val family = androidx.compose.runtime.remember(key, stamp, ready) {
                if (ready) com.pixelgram.android.ui.theme.AppFonts.family(context, com.pixelgram.android.ui.theme.AppFonts.SystemKey, key) else null
            }
            val loading = progress?.first == key
            SettingsItem(
                stringResource(label),
                segmentPosition(index, options.size),
                null,
                onClick = {
                    if (ready) update { it.copy(emojiStyle = key, fontStamp = it.fontStamp + 1) }
                    else if (progress == null) scope.launch {
                        val ok = com.pixelgram.android.ui.theme.AppFonts.download(context, key)
                        stamp++
                        if (ok) update { it.copy(emojiStyle = key, fontStamp = it.fontStamp + 1) }
                        else android.widget.Toast.makeText(context, failed, android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                headlineContent = {
                    androidx.compose.foundation.layout.Column {
                        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
                        if (family != null) Text("\uD83D\uDE00\uD83D\uDE02\uD83E\uDD70\uD83D\uDC4D\uD83D\uDD25\u2764\uFE0F", fontFamily = family, style = MaterialTheme.typography.titleLarge)
                        else if (loading) androidx.compose.material3.LinearWavyProgressIndicator(progress = { progress?.second ?: 0f }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                        else Text(stringResource(R.string.emoji_style_download), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
            ) {
                androidx.compose.material3.RadioButton(selected = settings.emojiStyle == key, onClick = null)
            }
        }
    }
}
