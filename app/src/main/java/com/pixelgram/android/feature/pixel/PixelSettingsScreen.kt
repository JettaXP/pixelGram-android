package com.pixelgram.android.feature.pixel

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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
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
                "chats" -> ChatsPage(appearance) { change -> graph.appearance.update(change) }
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
    SettingsGroup(stringResource(R.string.pixel_bubbles)) {
        BubblePreview(settings)
        CapsuleSlider(stringResource(R.string.pixel_bubble_radius), settings.bubbleRadius, 4..28, SegmentPosition.Middle) { v -> update { it.copy(bubbleRadius = v) } }
        CapsuleSlider(stringResource(R.string.pixel_bubble_tail), settings.bubbleTailRadius, 0..18, SegmentPosition.Middle) { v -> update { it.copy(bubbleTailRadius = v) } }
        CapsuleSlider(stringResource(R.string.pixel_font_size), settings.messageFontSize, 12..24, SegmentPosition.Bottom, "sp") { v -> update { it.copy(messageFontSize = v) } }
    }
    SettingsGroup(stringResource(R.string.pixel_messages)) {
        SettingsSwitch(stringResource(R.string.pixel_seconds), settings.messageSeconds, { v -> update { it.copy(messageSeconds = v) } }, SegmentPosition.Top, "12:34 → 12:34:56")
        SettingsSwitch(stringResource(R.string.pixel_zalgo), settings.zalgoFilter, { v -> update { it.copy(zalgoFilter = v) } }, SegmentPosition.Middle, stringResource(R.string.pixel_zalgo_sub))
        SettingsItem(stringResource(R.string.pixel_double_tap), SegmentPosition.Bottom, stringResource(R.string.pixel_double_tap_sub)) {
            androidx.compose.material3.OutlinedTextField(
                value = settings.doubleTapEmoji,
                onValueChange = { v -> update { it.copy(doubleTapEmoji = v.take(8)) } },
                singleLine = true,
                modifier = Modifier.width(72.dp),
            )
        }
    }
    SettingsGroup(stringResource(R.string.pixel_chat_list)) {
        SettingsSwitch(stringResource(R.string.pixel_exact_numbers), settings.exactNumbers, { v -> update { it.copy(exactNumbers = v) } }, SegmentPosition.Top, "999+ → 1234")
        SettingsSwitch(stringResource(R.string.pixel_hide_all), settings.hideAllChats, { v -> update { it.copy(hideAllChats = v) } }, SegmentPosition.Bottom, stringResource(R.string.pixel_hide_all_sub))
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
    androidx.compose.material3.Card(
        shape = androidx.compose.material3.ListItemDefaults.segmentedShapes(0, if (selected == AvatarShape.Custom) 2 else 1).shape,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
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
private fun CapsuleSlider(title: String, value: Int, range: IntRange, position: SegmentPosition, unit: String = "dp", onChange: (Int) -> Unit) {
    val (index, count) = position.index()
    androidx.compose.material3.SegmentedListItem(
        shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(index, count),
        colors = com.pixelgram.android.ui.components.segmentedColors(),
        trailingContent = { Text("$value $unit", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) },
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
private fun NavTabsEditor(enabled: List<MainTab>, saved: List<MainTab>, onChange: (List<MainTab>, List<MainTab>) -> Unit) {
    val order = remember { androidx.compose.runtime.mutableStateListOf<MainTab>().apply { addAll(saved) } }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val gap = with(density) { androidx.compose.material3.ListItemDefaults.SegmentedGap.toPx() }
    var rowHeight by remember { mutableStateOf(with(density) { 72.dp.toPx() }) }
    var dragging by remember { mutableStateOf<MainTab?>(null) }
    var offset by remember { mutableStateOf(0f) }
    val latestEnabled by androidx.compose.runtime.rememberUpdatedState(enabled)
    val commit = { onChange(order.filter { it in latestEnabled || it == MainTab.Chats }, order.toList()) }
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
                            enabled = item != MainTab.Chats,
                            onCheckedChange = { value ->
                                val next = if (value) enabled + item else enabled - item
                                onChange(order.filter { it in next }, order.toList())
                            },
                        )
                    },
                ) {
                    Text(stringResource(item.label))
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
