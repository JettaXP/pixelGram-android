package com.pixelgram.android.feature.settings

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.td.AccentColors
import com.pixelgram.android.td.FolderInfo
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.ui.components.Icon
import com.pixelgram.android.ui.components.PixelSwitch
import com.pixelgram.android.ui.components.SettingsGroup
import com.pixelgram.android.ui.components.SettingsItem
import com.pixelgram.android.ui.components.folderIcon
import com.pixelgram.android.ui.components.index
import com.pixelgram.android.ui.components.segmentPosition
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import kotlin.math.roundToInt

object FolderPrefs {
    const val UnreadId = -1
    const val MentionsId = -2
    const val MainToken = "main"
    private const val Prefs = "pixel_folders"
    private var prefs: android.content.SharedPreferences? = null
    var order by mutableStateOf("")
        private set
    var unread by mutableStateOf(false)
        private set
    var mentions by mutableStateOf(false)
        private set

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(Prefs, Context.MODE_PRIVATE)
        prefs = p
        order = p.getString("order", "").orEmpty()
        unread = p.getBoolean("unread", false)
        mentions = p.getBoolean("mentions", false)
    }

    fun toggleUnread(value: Boolean) {
        unread = value
        prefs?.edit()?.putBoolean("unread", value)?.apply()
    }

    fun toggleMentions(value: Boolean) {
        mentions = value
        prefs?.edit()?.putBoolean("mentions", value)?.apply()
    }

    fun token(id: Int): String = when (id) {
        0 -> MainToken
        UnreadId -> "unread"
        MentionsId -> "mentions"
        else -> "f$id"
    }

    private fun id(token: String): Int? = when (token) {
        MainToken -> 0
        "unread" -> UnreadId
        "mentions" -> MentionsId
        else -> token.removePrefix("f").toIntOrNull()
    }

    fun arrange(ids: List<Int>): List<Int> {
        val saved = order.split(',').mapNotNull { id(it.trim()) }.filter { it in ids }.distinct()
        val result = saved.toMutableList()
        ids.filter { it !in saved }.forEach { key ->
            val anchor = ids.take(ids.indexOf(key)).lastOrNull { it in result }
            result.add(if (anchor == null) 0 else result.indexOf(anchor) + 1, key)
        }
        return result
    }

    fun save(ids: List<Int>) {
        order = ids.joinToString(",") { token(it) }
        prefs?.edit()?.putString("order", order)?.apply()
    }

    fun all(folders: List<FolderInfo>): List<Int> = buildList {
        add(0)
        folders.forEach { add(it.id) }
        add(UnreadId)
        add(MentionsId)
    }
}

private sealed interface FolderEditTarget {
    data class Server(val id: Int?, val preset: TdApi.ChatFolder? = null) : FolderEditTarget
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FoldersSection(graph: AppGraph) {
    val context = LocalContext.current
    FolderPrefs.init(context)
    val snapshot by graph.store.snapshot.collectAsState()
    val appearance by graph.appearance.state.collectAsState()
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<FolderEditTarget?>(null) }
    var deleting by remember { mutableStateOf<Int?>(null) }
    var links by remember { mutableStateOf<FolderInfo?>(null) }
    val folders = snapshot.folders
    val byId = folders.associateBy { it.id }
    val order = FolderPrefs.arrange(FolderPrefs.all(folders))
    var dragged by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableIntStateOf(1) }
    val gap = with(androidx.compose.ui.platform.LocalDensity.current) { ListItemDefaults.SegmentedGap.toPx() }
    Text(
        stringResource(R.string.folders_about),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
    )
    SettingsGroup(stringResource(R.string.folders_mine), Modifier.animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec())) {
        order.forEachIndexed { index, id ->
            val moving = index == dragged
            val lift by animateFloatAsState(if (moving) 1.03f else 1f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "lift")
            val info = byId[id]
            val title = when (id) {
                0 -> stringResource(R.string.folder_all)
                FolderPrefs.UnreadId -> stringResource(R.string.folders_unread)
                FolderPrefs.MentionsId -> stringResource(R.string.folders_mentions)
                else -> info?.title ?: return@forEachIndexed
            }
            val subtitle = when (id) {
                0 -> stringResource(if (appearance.hideAllChats) R.string.folders_hidden else R.string.folders_all_sub)
                FolderPrefs.UnreadId -> stringResource(R.string.folders_unread_sub)
                FolderPrefs.MentionsId -> stringResource(R.string.folders_mentions_sub)
                else -> stringResource(if (info?.shareable == true) R.string.folders_shared else R.string.folders_cloud)
            }
            val icon = when (id) {
                0 -> "All"
                FolderPrefs.UnreadId -> "Unread"
                FolderPrefs.MentionsId -> "Mention"
                else -> info?.icon ?: "Custom"
            }
            val tint = info?.colorId?.takeIf { it >= 0 }?.let { AccentColors.colors(it, com.pixelgram.android.ui.theme.LocalDarkTheme.current).first() }
            val handle = Modifier.pointerInput(id, order.size) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { dragged = index; dragOffset = 0f },
                    onDragEnd = {
                        val target = (dragged + (dragOffset / (rowHeight + gap)).roundToInt()).coerceIn(0, order.lastIndex)
                        if (dragged >= 0 && target != dragged) {
                            val reordered = order.toMutableList().apply { add(target, removeAt(dragged)) }
                            FolderPrefs.save(reordered)
                            val server = reordered.filter { it > 0 }
                            val main = reordered.filter { it >= 0 }.indexOf(0).coerceAtLeast(0)
                            scope.launch {
                                runCatching { graph.td.send(TdApi.ReorderChatFolders(server.toIntArray(), main)) }
                                    .onFailure { runCatching { graph.td.send(TdApi.ReorderChatFolders(server.toIntArray(), 0)) } }
                            }
                        }
                        dragged = -1
                        dragOffset = 0f
                    },
                    onDragCancel = { dragged = -1; dragOffset = 0f },
                ) { change, drag ->
                    change.consume()
                    dragOffset += drag.y
                }
            }
            Box(
                Modifier
                    .onSizeChanged { rowHeight = it.height }
                    .zIndex(if (moving) 1f else 0f)
                    .offset { IntOffset(0, if (moving) dragOffset.roundToInt() else 0) }
                    .graphicsLayer { scaleX = lift; scaleY = lift; shadowElevation = if (moving) 12f else 0f },
            ) {
                SettingsItem(
                    title = title,
                    subtitle = subtitle,
                    position = segmentPosition(index, order.size),
                    icon = folderIcon(icon, true),
                    iconTint = tint ?: MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = when (id) {
                        0 -> ({ graph.appearance.update { it.copy(hideAllChats = !it.hideAllChats) } })
                        FolderPrefs.UnreadId -> ({ FolderPrefs.toggleUnread(!FolderPrefs.unread) })
                        FolderPrefs.MentionsId -> ({ FolderPrefs.toggleMentions(!FolderPrefs.mentions) })
                        else -> ({ editing = FolderEditTarget.Server(id) })
                    },
                ) {
                    when (id) {
                        0 -> PixelSwitch(!appearance.hideAllChats, { v -> graph.appearance.update { it.copy(hideAllChats = !v) } })
                        FolderPrefs.UnreadId -> PixelSwitch(FolderPrefs.unread, { FolderPrefs.toggleUnread(it) })
                        FolderPrefs.MentionsId -> PixelSwitch(FolderPrefs.mentions, { FolderPrefs.toggleMentions(it) })
                        else -> {
                            if (info?.shareable == true || info?.hasLinks == true) IconButton(onClick = { links = info }) {
                                Icon(Icons.Rounded.Link, stringResource(R.string.folders_links), tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { deleting = id }) { Icon(Icons.Rounded.Delete, stringResource(R.string.action_delete)) }
                        }
                    }
                    Icon(Icons.Rounded.DragIndicator, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = handle.padding(8.dp))
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        FilledTonalButton(onClick = { editing = FolderEditTarget.Server(null) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Rounded.CreateNewFolder, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.folders_create))
        }
    }
    val recommended by produceState(emptyList<TdApi.RecommendedChatFolder>(), folders.size) {
        value = runCatching { graph.td.send(TdApi.GetRecommendedChatFolders()).chatFolders.toList() }.getOrDefault(emptyList())
    }
    val local = buildList {
        if (!FolderPrefs.unread) add(FolderPrefs.UnreadId)
        if (!FolderPrefs.mentions) add(FolderPrefs.MentionsId)
    }
    if (recommended.isNotEmpty() || local.isNotEmpty()) {
        val total = recommended.size + local.size
        SettingsGroup(stringResource(R.string.folders_recommended)) {
            recommended.forEachIndexed { index, item ->
                SettingsItem(
                    title = item.folder.name.text.text,
                    subtitle = item.description,
                    position = segmentPosition(index, total),
                    icon = folderIcon(item.folder.icon?.name ?: "Custom", true),
                ) {
                    Button(onClick = { scope.launch { runCatching { graph.td.send(TdApi.CreateChatFolder(item.folder)) } } }) { Text(stringResource(R.string.folders_add)) }
                }
            }
            local.forEachIndexed { i, id ->
                SettingsItem(
                    title = stringResource(if (id == FolderPrefs.UnreadId) R.string.folders_unread else R.string.folders_mentions),
                    subtitle = stringResource(if (id == FolderPrefs.UnreadId) R.string.folders_unread_sub else R.string.folders_mentions_sub),
                    position = segmentPosition(recommended.size + i, total),
                    icon = folderIcon(if (id == FolderPrefs.UnreadId) "Unread" else "Mention", true),
                ) {
                    Button(onClick = { if (id == FolderPrefs.UnreadId) FolderPrefs.toggleUnread(true) else FolderPrefs.toggleMentions(true) }) { Text(stringResource(R.string.folders_add)) }
                }
            }
        }
    }
    Spacer(Modifier.height(24.dp))
    editing?.let { target ->
        if (target is FolderEditTarget.Server) FolderEditor(graph, target.id, onLinks = { info -> editing = null; links = info }) { editing = null }
    }
    links?.let { info -> FolderLinksSheet(graph, info) { links = null } }
    deleting?.let { id ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.folders_delete)) },
            text = { Text(byId[id]?.title.orEmpty()) },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    scope.launch { runCatching { graph.td.send(TdApi.DeleteChatFolder(id, LongArray(0))) } }
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private val EditorIcons = listOf(
    "Custom", "All", "Unread", "Unmuted", "Private", "Groups", "Channels", "Bots", "Favorite", "Love", "Home", "Work", "Study", "Book",
    "Trade", "Money", "Travel", "Airplane", "Game", "Sport", "Party", "Mask", "Crown", "Cat", "Flower", "Note", "Palette", "Light", "Like", "Setup", "Flag",
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FolderEditor(graph: AppGraph, id: Int?, onLinks: (FolderInfo) -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snapshot by graph.store.snapshot.collectAsState()
    var loaded by remember { mutableStateOf<TdApi.ChatFolder?>(null) }
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf<String?>(null) }
    var color by remember { mutableIntStateOf(-1) }
    val flags = remember { mutableStateListOf(false, false, false, false, false, false, false, false) }
    val included = remember { mutableStateListOf<Long>() }
    val excluded = remember { mutableStateListOf<Long>() }
    var picking by remember { mutableStateOf<Boolean?>(null) }
    var iconPicker by remember { mutableStateOf(false) }
    LaunchedEffect(id) {
        if (id == null) return@LaunchedEffect
        runCatching { graph.td.send(TdApi.GetChatFolder(id)) }.getOrNull()?.let {
            loaded = it
            name = it.name.text.text
            icon = it.icon?.name
            color = it.colorId
            listOf(it.includeContacts, it.includeNonContacts, it.includeGroups, it.includeChannels, it.includeBots, it.excludeMuted, it.excludeRead, it.excludeArchived)
                .forEachIndexed { i, v -> flags[i] = v }
            included.addAll((it.pinnedChatIds + it.includedChatIds).toList().distinct())
            excluded.addAll(it.excludedChatIds.toList())
        }
    }
    val valid = name.isNotBlank() && (flags.take(5).any { it } || included.isNotEmpty())
    val save = {
        val folder = TdApi.ChatFolder().apply {
            this.name = TdApi.ChatFolderName(TdApi.FormattedText(name.trim(), emptyArray()), false)
            this.icon = icon?.let { TdApi.ChatFolderIcon(it) }
            colorId = color
            val pinned = loaded?.pinnedChatIds?.filter { it in included }.orEmpty()
            pinnedChatIds = pinned.toLongArray()
            includedChatIds = included.filter { it !in pinned }.toLongArray()
            excludedChatIds = excluded.toLongArray()
            includeContacts = flags[0]
            includeNonContacts = flags[1]
            includeGroups = flags[2]
            includeChannels = flags[3]
            includeBots = flags[4]
            excludeMuted = flags[5]
            excludeRead = flags[6]
            excludeArchived = flags[7]
        }
        scope.launch { runCatching { if (id == null) graph.td.send(TdApi.CreateChatFolder(folder)) else graph.td.send(TdApi.EditChatFolder(id, folder)) } }
        onDismiss()
    }
    com.pixelgram.android.feature.security.FullDialog(stringResource(if (id == null) R.string.folders_create else R.string.folders_edit), onDismiss, fab = {
        if (valid) FloatingActionButton(onClick = save) { Icon(Icons.Rounded.Check, stringResource(R.string.action_save)) }
    }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Surface(onClick = { iconPicker = true }, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(56.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(folderIcon(icon ?: "Custom", true), null, tint = MaterialTheme.colorScheme.onSecondaryContainer) }
                }
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(name, { name = it.take(12) }, label = { Text(stringResource(R.string.folders_name)) }, singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f))
            }
            SettingsGroup(stringResource(R.string.folders_included)) {
                val types = listOf(R.string.folders_contacts, R.string.folders_non_contacts, R.string.folders_groups, R.string.folders_channels, R.string.folders_bots)
                val count = types.size + included.size + 1
                SettingsItem(stringResource(R.string.folders_add_chats), segmentPosition(0, count), icon = Icons.Rounded.Add, onClick = { picking = true })
                types.forEachIndexed { i, label ->
                    SettingsItem(stringResource(label), segmentPosition(i + 1, count), onClick = { flags[i] = !flags[i] }) {
                        PixelSwitch(flags[i], { flags[i] = it })
                    }
                }
                included.forEachIndexed { i, chatId ->
                    ChatEntry(graph, snapshot, chatId, segmentPosition(types.size + 1 + i, count)) { included.remove(chatId) }
                }
            }
            SettingsGroup(stringResource(R.string.folders_excluded)) {
                val types = listOf(R.string.folders_muted, R.string.folders_read, R.string.folders_archived)
                val count = types.size + excluded.size + 1
                SettingsItem(stringResource(R.string.folders_remove_chats), segmentPosition(0, count), icon = Icons.Rounded.Remove, onClick = { picking = false })
                types.forEachIndexed { i, label ->
                    SettingsItem(stringResource(label), segmentPosition(i + 1, count), onClick = { flags[5 + i] = !flags[5 + i] }) {
                        PixelSwitch(flags[5 + i], { flags[5 + i] = it })
                    }
                }
                excluded.forEachIndexed { i, chatId ->
                    ChatEntry(graph, snapshot, chatId, segmentPosition(types.size + 1 + i, count)) { excluded.remove(chatId) }
                }
            }
            SettingsGroup(stringResource(R.string.folders_color)) {
                val dark = com.pixelgram.android.ui.theme.LocalDarkTheme.current
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(14.dp),
                ) {
                    (listOf(-1) + (0..6)).forEach { c ->
                        val selected = color == c
                        val swatch = if (c < 0) MaterialTheme.colorScheme.surfaceContainerHighest else AccentColors.colors(c, dark).first()
                        val ring by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "ring")
                        Box(
                            Modifier.size(40.dp).border(2.dp, ring, CircleShape).padding(4.dp).clip(CircleShape).background(swatch).clickable { color = c },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (c < 0) Icon(Icons.Rounded.Remove, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            else if (selected) Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = Color.White)
                        }
                    }
                }
                Text(stringResource(R.string.folders_color_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
            if (id != null) {
                SettingsGroup(stringResource(R.string.folders_share)) {
                    SettingsItem(
                        stringResource(R.string.folders_links),
                        segmentPosition(0, 1),
                        subtitle = stringResource(R.string.folders_share_sub),
                        icon = Icons.Rounded.Link,
                        onClick = { onLinks(snapshot.folders.firstOrNull { it.id == id } ?: FolderInfo(id, name, icon)) },
                    )
                }
            }
            Spacer(Modifier.height(96.dp))
        }
    }
    picking?.let { include ->
        com.pixelgram.android.feature.security.PeoplePicker(
            graph,
            stringResource(if (include) R.string.folders_add_chats else R.string.folders_remove_chats),
            if (include) included.toList() else excluded.toList(),
            onDismiss = { picking = null },
            everything = true,
        ) { ids ->
            val target = if (include) included else excluded
            target.clear()
            target.addAll(ids)
            picking = null
        }
    }
    if (iconPicker) {
        ModalBottomSheet(onDismissRequest = { iconPicker = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            ) {
                EditorIcons.forEach { name ->
                    val selected = (icon ?: "Custom") == name
                    Surface(
                        onClick = { icon = name; iconPicker = false },
                        shape = RoundedCornerShape(if (selected) 18.dp else 14.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(52.dp),
                    ) { Box(contentAlignment = Alignment.Center) { Icon(folderIcon(name, selected), null) } }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChatEntry(graph: AppGraph, snapshot: com.pixelgram.android.td.StoreSnapshot, chatId: Long, position: com.pixelgram.android.ui.components.SegmentPosition, onRemove: () -> Unit) {
    val chat = graph.store.chat(chatId)
    val user = snapshot.users[chatId]
    val title = user?.name ?: chat?.title ?: chatId.toString()
    androidx.compose.material3.SegmentedListItem(
        onClick = {},
        shapes = ListItemDefaults.segmentedShapes(position.index().first, position.index().second),
        colors = com.pixelgram.android.ui.components.segmentedColors(),
        leadingContent = {
            Avatar(title, chatId, user?.localPhoto ?: chat?.photo?.small?.id?.let { snapshot.files[it] }, chat?.photo?.minithumbnail?.data, 40.dp, onNeedFile = { chat?.photo?.small?.id?.let(graph.store::requestFileAsync) })
        },
        trailingContent = { IconButton(onClick = onRemove) { Icon(Icons.Rounded.Remove, stringResource(R.string.action_delete)) } },
    ) { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FolderLinksSheet(graph: AppGraph, info: FolderInfo, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snapshot by graph.store.snapshot.collectAsState()
    var reload by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    val links by produceState<List<TdApi.ChatFolderInviteLink>?>(null, reload) {
        value = runCatching { graph.td.send(TdApi.GetChatFolderInviteLinks(info.id)).inviteLinks.toList() }.getOrDefault(emptyList())
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 16.dp),
        ) {
            Box(Modifier.size(76.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(folderIcon(info.icon ?: "Custom", true), null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(stringResource(R.string.folders_share_title, info.title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.folders_share_about), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            val list = links.orEmpty()
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                list.forEachIndexed { index, link ->
                    androidx.compose.material3.SegmentedListItem(
                        onClick = {
                            val clip = context.getSystemService(android.content.ClipboardManager::class.java)
                            clip?.setPrimaryClip(android.content.ClipData.newPlainText("link", link.inviteLink))
                            android.widget.Toast.makeText(context, R.string.link_copied, android.widget.Toast.LENGTH_SHORT).show()
                        },
                        shapes = ListItemDefaults.segmentedShapes(index, list.size),
                        colors = com.pixelgram.android.ui.components.segmentedColors(),
                        leadingContent = { Icon(Icons.Rounded.Link, null, tint = MaterialTheme.colorScheme.primary) },
                        supportingContent = {
                            Text(androidx.compose.ui.platform.LocalContext.current.resources.getQuantityString(R.plurals.folders_link_chats, link.chatIds.size, link.chatIds.size))
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = {
                                    context.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, link.inviteLink), null))
                                }) { Icon(Icons.Rounded.Share, null) }
                                IconButton(onClick = {
                                    scope.launch {
                                        runCatching { graph.td.send(TdApi.DeleteChatFolderInviteLink(info.id, link.inviteLink)) }
                                        reload++
                                    }
                                }) { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            }
                        },
                    ) { Text(link.name.ifBlank { link.inviteLink.removePrefix("https://") }, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Button(
                onClick = {
                    scope.launch {
                        runCatching {
                            val chats = graph.td.send(TdApi.GetChatsForChatFolderInviteLink(info.id)).chatIds
                            graph.td.send(TdApi.CreateChatFolderInviteLink(info.id, "", chats))
                        }.onSuccess { reload++; error = null }.onFailure { error = it.message }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Rounded.Add, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.folders_create_link))
            }
            if (snapshot.folders.none { it.id == info.id }) TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    }
}
