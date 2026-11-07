package com.pixelgram.android.feature.pixel

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
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import androidx.compose.foundation.gestures.detectDragGestures
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLargeEmphasized) },
                navigationIcon = { IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            when (page) {
                "appearance" -> AppearancePage(appearance) { change -> graph.appearance.update(change) }
                "general" -> GeneralPage(appearance) { change -> graph.appearance.update(change) }
                "chats" -> ChatsPage(appearance) { change -> graph.appearance.update(change) }
                "about" -> AboutPage()
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
        SettingsItem(stringResource(R.string.pixel_general), SegmentPosition.Top, stringResource(R.string.pixel_general_sub), Icons.Rounded.Tune, onClick = { open("general") })
        SettingsItem(stringResource(R.string.pixel_appearance), SegmentPosition.Middle, stringResource(R.string.pixel_appearance_sub), Icons.Rounded.Palette, onClick = { open("appearance") })
        SettingsItem(stringResource(R.string.pixel_chats), SegmentPosition.Middle, stringResource(R.string.pixel_chats_sub), Icons.Rounded.Chat, onClick = { open("chats") })
        SettingsItem(stringResource(R.string.pixel_about), SegmentPosition.Bottom, stringResource(R.string.pixel_about_sub), Icons.Rounded.Info, onClick = { open("about") })
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
internal fun ChatsPage(settings: AppearanceSettings, update: ((AppearanceSettings) -> AppearanceSettings) -> Unit) {
    var preview by remember { mutableStateOf(0) }
    val sample = listOf(
        FolderTab(0, stringResource(R.string.folder_all), "All", 15, 3),
        FolderTab(1, stringResource(R.string.pixel_folder_sample_private), "Private", 2, 0),
        FolderTab(2, stringResource(R.string.pixel_folder_sample_work), "Work"),
        FolderTab(3, stringResource(R.string.pixel_folder_sample_channels), "Channels", 1, 1),
    )
    SettingsGroup(stringResource(R.string.pixel_folder_title)) {
        androidx.compose.material3.Surface(shape = segmentShape(SegmentPosition.Top), color = MaterialTheme.colorScheme.surfaceContainer) {
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
                    shapes = if (index == 0) ButtonGroupDefaults.connectedLeadingButtonShapes() else ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(if (style == NavBarStyle.Expressive) R.string.nav_bar_expressive else R.string.nav_bar_classic))
                }
            }
        }
    }
    SettingsGroup(stringResource(R.string.pixel_nav_tabs)) {
        NavTabsEditor(settings.navTabs) { tabs -> update { it.copy(navTabs = tabs) } }
    }
    androidx.compose.animation.AnimatedVisibility(settings.navBarStyle == NavBarStyle.Expressive) {
        SettingsGroup(stringResource(R.string.pixel_capsule)) {
            CapsuleSlider(stringResource(R.string.pixel_capsule_inset), settings.capsuleInset, 4..20, SegmentPosition.Top) { v -> update { it.copy(capsuleInset = v) } }
            CapsuleSlider(stringResource(R.string.pixel_capsule_spacing), settings.capsuleSpacing, 0..16, SegmentPosition.Middle) { v -> update { it.copy(capsuleSpacing = v) } }
            CapsuleSlider(stringResource(R.string.pixel_capsule_height), settings.capsuleHeight, 56..80, SegmentPosition.Bottom) { v -> update { it.copy(capsuleHeight = v) } }
        }
    }
    SettingsGroup(stringResource(R.string.pixel_avatars)) {
        AvatarShapePicker(settings.avatarShape) { shape -> update { it.copy(avatarShape = shape) } }
        if (settings.avatarShape == AvatarShape.Custom) {
            SettingsItem(stringResource(R.string.pixel_avatar_corner), SegmentPosition.Bottom, "${settings.avatarCorner}%", Icons.Rounded.RoundedCorner)
            Slider(
                value = settings.avatarCorner.toFloat(),
                onValueChange = { value -> update { it.copy(avatarCorner = value.roundToInt()) } },
                valueRange = 0f..50f,
                steps = 9,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
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
private fun AvatarShapePicker(selected: AvatarShape, onSelect: (AvatarShape) -> Unit) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (AvatarShape.entries.indexOf(selected) - 2).coerceAtLeast(0))
    androidx.compose.material3.Surface(shape = com.pixelgram.android.ui.components.segmentShape(if (selected == AvatarShape.Custom) SegmentPosition.Top else SegmentPosition.Single), color = MaterialTheme.colorScheme.surfaceContainer) {
        LazyRow(
            state = state,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(AvatarShape.entries, key = { it.name }) { shape ->
                val active = shape == selected
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(shape.shape(28))
                        .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer)
                        .selectable(selected = active, role = Role.RadioButton) { onSelect(shape) }
                        .semantics { contentDescription = shape.name },
                    contentAlignment = Alignment.Center,
                ) {
                    if (shape == AvatarShape.Custom) {
                        Icon(
                            Icons.Rounded.RoundedCorner,
                            null,
                            tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
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
    SettingsGroup(stringResource(R.string.pixel_links)) {
        SettingsItem(stringResource(R.string.pixel_channel), SegmentPosition.Top, "@pixelgramclient", Icons.Rounded.Chat, onClick = { uri.openUri("https://t.me/pixelgramclient") })
        SettingsItem(stringResource(R.string.pixel_desktop), SegmentPosition.Middle, "pixelgram.jettax.xyz", Icons.Rounded.Language, onClick = { uri.openUri("https://pixelgram.jettax.xyz/") })
        SettingsItem(stringResource(R.string.pixel_source), SegmentPosition.Bottom, "GitHub", Icons.Rounded.Code, onClick = { uri.openUri("https://github.com/JettaXP") })
    }
    SettingsGroup(stringResource(R.string.pixel_info)) {
        SettingsItem(stringResource(R.string.pixel_version), SegmentPosition.Top, BuildConfig.VERSION_NAME, Icons.Rounded.Info)
        SettingsItem(stringResource(R.string.pixel_credits), SegmentPosition.Bottom, stringResource(R.string.pixel_credits_sub), Icons.Rounded.Palette)
    }
}

@Composable
private fun CapsuleSlider(title: String, value: Int, range: IntRange, position: SegmentPosition, onChange: (Int) -> Unit) {
    androidx.compose.material3.Surface(shape = segmentShape(position), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("$value dp", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { onChange(it.roundToInt()) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = range.last - range.first - 1,
            )
        }
    }
}

@Composable
private fun NavTabsEditor(enabled: List<MainTab>, onChange: (List<MainTab>) -> Unit) {
    val order = remember { androidx.compose.runtime.mutableStateListOf<MainTab>() }
    LaunchedEffect(enabled) {
        val full = enabled + MainTab.entries.filterNot { it in enabled }
        if (order.toList() != full && order.filter { it in enabled } != enabled || order.isEmpty()) {
            order.clear()
            order.addAll(full)
        }
    }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val rowHeight = with(density) { 72.dp.toPx() }
    var dragging by remember { mutableStateOf<MainTab?>(null) }
    var offset by remember { mutableStateOf(0f) }
    val commit = { onChange(order.filter { it in enabled || it == MainTab.Chats }) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        order.forEachIndexed { index, item ->
            val isDragged = dragging == item
            val on = item in enabled
            androidx.compose.material3.Surface(
                shape = segmentShape(segmentPosition(index, order.size)),
                color = if (isDragged) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = if (isDragged) 6.dp else 0.dp,
                modifier = Modifier
                    .zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer { translationY = if (isDragged) offset else 0f },
            ) {
                Row(Modifier.fillMaxWidth().height(70.dp).padding(start = 8.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.DragHandle,
                        stringResource(R.string.pixel_nav_drag),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(12.dp)
                            .pointerInput(item) {
                                detectDragGestures(
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
                                    },
                                ) { change, amount ->
                                    change.consume()
                                    offset += amount.y
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
                    Text(stringResource(item.label), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(start = 4.dp))
                    PixelSwitch(
                        checked = on,
                        enabled = item != MainTab.Chats,
                        onCheckedChange = { value ->
                            val next = if (value) enabled + item else enabled - item
                            onChange(order.filter { it in next })
                        },
                    )
                }
            }
        }
    }
}

private val MainTab.label: Int
    get() = when (this) {
        MainTab.Chats -> R.string.tab_chats
        MainTab.Contacts -> R.string.tab_contacts
        MainTab.Profile -> R.string.tab_profile
        MainTab.Settings -> R.string.tab_settings
    }
