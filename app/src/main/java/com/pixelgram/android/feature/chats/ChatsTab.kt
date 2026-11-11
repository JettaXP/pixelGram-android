package com.pixelgram.android.feature.chats

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Close
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.Badge
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.pixelgram.android.ui.components.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.nav.ChatKey
import com.pixelgram.android.nav.Navigator
import com.pixelgram.android.td.ChatKind
import com.pixelgram.android.td.ChatRow
import com.pixelgram.android.td.LIST_ARCHIVE
import com.pixelgram.android.td.LIST_MAIN
import com.pixelgram.android.td.PreviewKind
import com.pixelgram.android.td.folderKey
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.ui.components.NameBadges
import com.pixelgram.android.ui.components.FolderChips
import com.pixelgram.android.ui.components.FolderTab
import com.pixelgram.android.settings.FolderTitle
import com.pixelgram.android.util.chatListTime
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChatsTab(
    graph: AppGraph,
    navigator: Navigator,
    baseList: String,
    onOpenDrawer: () -> Unit,
    onCloseArchive: () -> Unit,
) {
    val snapshot by graph.store.snapshot.collectAsState()
    val appearance by graph.appearance.state.collectAsState()
    val archive = baseList == LIST_ARCHIVE
    val allTitle = stringResource(R.string.folder_all)
    val tabs = remember(snapshot.folders, snapshot.unread, allTitle, archive, appearance.hideAllChats) {
        if (archive) {
            emptyList()
        } else {
            val main = snapshot.unread[LIST_MAIN]
            val folders = snapshot.folders.map { f ->
                val u = snapshot.unread[folderKey(f.id)]
                FolderTab(f.id, f.title, f.icon ?: "Custom", u?.chats ?: 0, u?.unmuted ?: 0)
            }
            if (appearance.hideAllChats && folders.isNotEmpty()) folders
            else listOf(FolderTab(0, allTitle, "All", main?.chats ?: 0, main?.unmuted ?: 0)) + folders
        }
    }
    var folder by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(tabs) { if (tabs.isNotEmpty() && tabs.none { it.id == folder }) folder = tabs.first().id }
    val pageCount = tabs.size.coerceAtLeast(1)
    val pager = rememberPagerState(initialPage = tabs.indexOfFirst { it.id == folder }.coerceAtLeast(0)) { pageCount }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager.currentPage, tabs) { tabs.getOrNull(pager.currentPage)?.let { folder = it.id } }
    LaunchedEffect(archive) {
        runCatching { graph.td.send(TdApi.LoadActiveStories(if (archive) TdApi.StoryListArchive() else TdApi.StoryListMain())) }
    }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val tracker = remember { HideOnScroll(with(density) { HideDistance.toPx() }, with(density) { ShowDistance.toPx() }) }
    var newMessage by remember { mutableStateOf(false) }
    var fabMenu by remember { mutableStateOf(false) }
    var newChat by remember { mutableStateOf<Boolean?>(null) }
    BackHandler(enabled = fabMenu) { fabMenu = false }
    LaunchedEffect(tracker.hidden) { if (tracker.hidden) fabMenu = false }
    if (newMessage) NewMessageSheet(graph, navigator) { newMessage = false }
    newChat?.let { channel -> NewChatDialog(graph, navigator, channel) { newChat = null } }
    Scaffold(
        floatingActionButton = {
            val visible = !archive && !tracker.hidden
            var pencil by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
            var origin by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
            val gap = with(density) { CameraGap.roundToPx() }
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .padding(
                        bottom = (com.pixelgram.android.ui.components.LocalBottomBarPadding.current -
                            androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()).coerceAtLeast(0.dp),
                    )
                    .onGloballyPositioned { origin = it.positionInRoot() },
            ) {
                androidx.compose.material3.FloatingActionButtonMenu(
                    expanded = fabMenu,
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(top = CameraSize + CameraGap).animateFloatingActionButton(visible, Alignment.BottomEnd),
                    button = {
                        androidx.compose.material3.ToggleFloatingActionButton(
                            checked = fabMenu,
                            onCheckedChange = { fabMenu = it },
                            containerSize = androidx.compose.material3.ToggleFloatingActionButtonDefaults.containerSize(PencilSize, PencilOpenSize),
                            modifier = Modifier.onGloballyPositioned { if (!fabMenu) pencil = it.boundsInRoot() },
                        ) {
                            val progress = checkedProgress
                            Icon(
                                if (progress > 0.5f) Icons.Rounded.Close else Icons.Rounded.Edit,
                                stringResource(R.string.new_message),
                                Modifier.graphicsLayer { rotationZ = progress * 90f },
                            )
                        }
                    },
                ) {
                    listOf(
                        Triple(Icons.Rounded.Person, R.string.new_message) { newMessage = true },
                        Triple(Icons.Rounded.Groups, R.string.new_group) { newChat = false },
                        Triple(Icons.Rounded.Campaign, R.string.new_channel) { newChat = true },
                    ).forEach { (icon, label, action) ->
                        FloatingActionButtonMenuItem(
                            onClick = {
                                fabMenu = false
                                action()
                            },
                            icon = { Icon(icon, null) },
                            text = { Text(stringResource(label)) },
                        )
                    }
                }
                val anchor = pencil
                androidx.compose.material3.SmallFloatingActionButton(
                    onClick = { com.pixelgram.android.feature.stories.StoryComposer.open = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                if (anchor != null) {
                                    val x = anchor.center.x - origin.x - placeable.width / 2f
                                    val y = anchor.top - origin.y - gap - placeable.height
                                    placeable.place(x.toInt(), y.toInt())
                                }
                            }
                        }
                        .animateFloatingActionButton(visible && !fabMenu && anchor != null, Alignment.Center),
                ) { Icon(Icons.Rounded.PhotoCamera, stringResource(R.string.stories_new)) }
            }
        },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            when {
                                archive -> stringResource(R.string.drawer_archive)
                                appearance.folderTitle == FolderTitle.Icons && folder != 0 -> tabs.firstOrNull { it.id == folder }?.title ?: stringResource(R.string.app_name)
                                else -> stringResource(R.string.app_name)
                            },
                            style = MaterialTheme.typography.titleLargeEmphasized,
                        )
                    },
                    actions = {
                        IconButton(onClick = { navigator.open(com.pixelgram.android.nav.SearchKey) }) { Icon(Icons.Rounded.Search, stringResource(R.string.search_chats)) }
                    },
                    navigationIcon = {
                        if (archive) {
                            IconButton(onClick = onCloseArchive) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
                        } else {
                            IconButton(onClick = onOpenDrawer) { Icon(Icons.Rounded.Menu, stringResource(R.string.action_menu)) }
                        }
                    },
                )
                if (tabs.size > 1) {
                    FolderChips(
                        tabs,
                        folder,
                        appearance.folderTitle,
                        onSelect = { id ->
                            val index = tabs.indexOfFirst { it.id == id }
                            if (index >= 0) scope.launch { pager.animateScrollToPage(index) }
                        },
                    )
                }
                com.pixelgram.android.ui.components.MiniPlayer()
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pager,
            key = { tabs.getOrNull(it)?.id ?: -1 },
            beyondViewportPageCount = 0,
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()).nestedScroll(tracker),
        ) { page ->
            val id = tabs.getOrNull(page)?.id ?: 0
            val key = when {
                archive -> LIST_ARCHIVE
                id != 0 -> folderKey(id)
                else -> LIST_MAIN
            }
            ChatListPage(graph, navigator, key, snapshot.lists[key].orEmpty(), stories = if (key == LIST_MAIN || key == LIST_ARCHIVE) snapshot.stories.filter { it.archived == archive } else emptyList())
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChatListPage(graph: AppGraph, navigator: Navigator, listKey: String, rows: List<ChatRow>, stories: List<com.pixelgram.android.td.StoryOwner> = emptyList()) {
    var actionsFor by remember { mutableStateOf<ChatRow?>(null) }
    actionsFor?.let { row -> ChatActions(graph, row) { actionsFor = null } }
    val listState = rememberLazyListState()
    val reveal = remember { StoryReveal(listState) }
    var loading by remember(listKey) { mutableStateOf(true) }
    val tdList: TdApi.ChatList = remember(listKey) {
        when {
            listKey == LIST_ARCHIVE -> TdApi.ChatListArchive()
            listKey.startsWith("folder:") -> TdApi.ChatListFolder(listKey.removePrefix("folder:").toInt())
            else -> TdApi.ChatListMain()
        }
    }
    val scope = rememberCoroutineScope()
    val loadMore: () -> Unit = {
        scope.launch { loading = runCatching { graph.td.send(TdApi.LoadChats(tdList, 50)) }.isSuccess }
    }
    LaunchedEffect(listKey) { loadMore() }
    val nearEnd by remember(listKey) {
        derivedStateOf {
            val info = listState.layoutInfo
            info.totalItemsCount > 0 && (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 8
        }
    }
    LaunchedEffect(nearEnd, loading) { if (nearEnd && loading) loadMore() }
    if (rows.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (loading) LoadingIndicator(Modifier.size(56.dp))
            else Text(stringResource(R.string.chats_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp + com.pixelgram.android.ui.components.LocalBottomBarPadding.current),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier.fillMaxSize().nestedScroll(reveal),
        ) {
            if (listKey == LIST_MAIN || stories.isNotEmpty()) {
                item(key = "stories", contentType = "stories") {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = reveal.shown,
                        enter = androidx.compose.animation.expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + androidx.compose.animation.fadeOut(),
                    ) {
                        com.pixelgram.android.feature.stories.StoriesRow(graph, stories, Modifier.padding(bottom = 8.dp))
                    }
                }
            }
            itemsIndexed(rows, key = { _, it -> it.id }, contentType = { _, _ -> "chat" }) { index, row ->
                ChatListItem(
                    row = row,
                    index = index,
                    count = rows.size,
                    onClick = { navigator.open(ChatKey(row.id)) },
                    onNeedPhoto = { graph.store.requestFileAsync(row.photoFileId) },
                    onLongClick = { actionsFor = row },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatListItem(row: ChatRow, index: Int, count: Int, onClick: () -> Unit, onNeedPhoto: () -> Unit, modifier: Modifier = Modifier, onLongClick: (() -> Unit)? = null) {
    SegmentedListItem(
        onClick = onClick,
        onLongClick = onLongClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        modifier = modifier,
        colors = com.pixelgram.android.ui.components.segmentedColors(),
        leadingContent = {
            Box {
                Avatar(
                    title = row.title,
                    seed = row.id,
                    path = row.photoPath,
                    minithumb = row.minithumb,
                    size = 52.dp,
                    icon = when {
                        row.kind == ChatKind.Saved -> Icons.Rounded.Bookmark
                        row.deleted -> com.pixelgram.android.ui.icons.GhostIcon
                        else -> null
                    },
                    onNeedFile = if (row.photoFileId != 0) onNeedPhoto else null,
                )
                if (row.online) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.BottomEnd).size(14.dp),
                    )
                }
            }
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(chatListTime(row.date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                when {
                    row.mentions > 0 -> Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("@") }
                    row.unread > 0 -> Badge(
                        containerColor = if (row.muted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                        contentColor = if (row.muted) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.widthIn(min = 22.dp),
                    ) { Text(com.pixelgram.android.ui.theme.countLabel(row.unread, com.pixelgram.android.ui.theme.LocalAppearance.current.exactNumbers)) }
                    row.pinned -> Icon(Icons.Rounded.PushPin, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                }
            }
        },
        supportingContent = {
            androidx.compose.animation.Crossfade(row.typing, label = "typing") { typing ->
                if (typing) TypingLine() else PreviewText(row, Modifier)
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = when {
                    row.kind == ChatKind.Saved -> stringResource(R.string.drawer_saved)
                    row.deleted -> stringResource(R.string.chat_deleted_account)
                    else -> row.title
                },
                fontWeight = if (row.unread > 0) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            NameBadges(row.userId, row.verified, 16.dp)
            if (row.muted) Icon(Icons.Rounded.VolumeOff, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun PreviewText(row: ChatRow, modifier: Modifier) {
    val preview = row.preview
    val kindLabel = preview?.kind?.let { previewLabel(it) }
    val body = buildString {
        if (row.outgoing) append(stringResource(R.string.preview_you)).append(": ")
        else if (preview?.sender != null) append(preview.sender).append(": ")
        if (kindLabel != null) {
            append(kindLabel)
            if (preview.text.isNotBlank()) append(", ")
        }
        append(preview?.text?.replace('\n', ' ').orEmpty())
    }
    Text(
        body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun previewLabel(kind: PreviewKind): String? = when (kind) {
    PreviewKind.Text -> null
    PreviewKind.Photo -> stringResource(R.string.preview_photo)
    PreviewKind.Video -> stringResource(R.string.preview_video)
    PreviewKind.Gif -> stringResource(R.string.preview_gif)
    PreviewKind.Sticker -> stringResource(R.string.preview_sticker)
    PreviewKind.Voice -> stringResource(R.string.preview_voice)
    PreviewKind.VideoNote -> stringResource(R.string.preview_video_note)
    PreviewKind.Audio -> stringResource(R.string.preview_audio)
    PreviewKind.File -> stringResource(R.string.preview_file)
    PreviewKind.Poll -> stringResource(R.string.preview_poll)
    PreviewKind.Location -> stringResource(R.string.preview_location)
    PreviewKind.Contact -> stringResource(R.string.preview_contact)
    PreviewKind.Service -> stringResource(R.string.preview_service)
    PreviewKind.Other -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatActions(graph: AppGraph, row: ChatRow, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    val run: (TdApi.Function<*>) -> Unit = { function ->
        scope.launch { runCatching { graph.td.send(function as TdApi.Function<TdApi.Object>) } }
        onDismiss()
    }
    val chatList: TdApi.ChatList = when {
        row.list == LIST_ARCHIVE -> TdApi.ChatListArchive()
        row.list.startsWith("folder:") -> TdApi.ChatListFolder(row.list.removePrefix("folder:").toInt())
        else -> TdApi.ChatListMain()
    }
    if (confirmDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Rounded.Delete, null) },
            title = { Text(stringResource(if (row.kind == ChatKind.Group || row.kind == ChatKind.Channel) R.string.chat_leave_title else R.string.chat_delete_title)) },
            text = { Text(row.title) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    run(
                        if (row.kind == ChatKind.Group || row.kind == ChatKind.Channel) TdApi.LeaveChat(row.id)
                        else TdApi.DeleteChatHistory(row.id, true, false),
                    )
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        )
        return
    }
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(row.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        SheetItem(Icons.Rounded.PushPin, stringResource(if (row.pinned) R.string.chat_unpin else R.string.chat_pin)) {
            run(TdApi.ToggleChatIsPinned(chatList, row.id, !row.pinned))
        }
        if (row.kind != ChatKind.Saved) {
            SheetItem(if (row.muted) Icons.AutoMirrored.Rounded.VolumeUp else Icons.Rounded.VolumeOff, stringResource(if (row.muted) R.string.chat_unmute else R.string.chat_mute)) {
                run(TdApi.SetChatNotificationSettings(row.id, TdApi.ChatNotificationSettings().apply {
                    useDefaultMuteFor = false
                    muteFor = if (row.muted) 0 else Int.MAX_VALUE
                    useDefaultSound = true
                    useDefaultShowPreview = true
                    useDefaultMuteStories = true
                    useDefaultShowStoryPoster = true
                    useDefaultDisablePinnedMessageNotifications = true
                    useDefaultDisableMentionNotifications = true
                }))
            }
            SheetItem(Icons.Rounded.Archive, stringResource(if (row.list == LIST_ARCHIVE) R.string.chat_unarchive else R.string.chat_archive)) {
                run(TdApi.AddChatToList(row.id, if (row.list == LIST_ARCHIVE) TdApi.ChatListMain() else TdApi.ChatListArchive()))
            }
        }
        if (row.unread > 0 && row.lastMessageId != 0L) {
            SheetItem(Icons.Rounded.DoneAll, stringResource(R.string.chat_mark_read)) {
                run(TdApi.ViewMessages(row.id, longArrayOf(row.lastMessageId), null, true))
            }
        }
        SheetItem(Icons.Rounded.Delete, stringResource(if (row.kind == ChatKind.Group || row.kind == ChatKind.Channel) R.string.chat_leave else R.string.chat_delete), MaterialTheme.colorScheme.error) {
            confirmDelete = true
        }
        androidx.compose.foundation.layout.Spacer(Modifier.size(16.dp))
    }
}

@Composable
private fun SheetItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    androidx.compose.material3.ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent, headlineColor = tint, leadingIconColor = tint),
        leadingContent = { Icon(icon, null) },
        headlineContent = { Text(label) },
    )
}

private class StoryReveal(private val list: androidx.compose.foundation.lazy.LazyListState) : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
    var shown by mutableStateOf(false)

    override fun onPostScroll(
        consumed: androidx.compose.ui.geometry.Offset,
        available: androidx.compose.ui.geometry.Offset,
        source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
    ): androidx.compose.ui.geometry.Offset {
        if (available.y > RevealPull && list.firstVisibleItemIndex == 0 && list.firstVisibleItemScrollOffset == 0) shown = true
        if (consumed.y < -HideScroll) shown = false
        return androidx.compose.ui.geometry.Offset.Zero
    }
}

private const val RevealPull = 6f
private const val HideScroll = 24f

private class HideOnScroll(private val hideAt: Float, private val showAt: Float) : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
    var hidden by mutableStateOf(false)
    private var distance = 0f

    override fun onPostScroll(
        consumed: androidx.compose.ui.geometry.Offset,
        available: androidx.compose.ui.geometry.Offset,
        source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
    ): androidx.compose.ui.geometry.Offset {
        val dy = -consumed.y
        if (dy == 0f) {
            if (available.y < 0f) {
                distance = 0f
                hidden = false
            }
            return androidx.compose.ui.geometry.Offset.Zero
        }
        if (distance != 0f && (dy > 0f) != (distance > 0f)) distance = 0f
        distance += dy
        if (distance >= hideAt) hidden = true
        if (distance <= -showAt) hidden = false
        return androidx.compose.ui.geometry.Offset.Zero
    }
}

private val HideDistance = 16.dp
private val PencilSize = 52.dp
private val PencilOpenSize = 56.dp
private val CameraSize = 40.dp
private val CameraGap = 12.dp
private val ShowDistance = 48.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NewMessageSheet(graph: AppGraph, navigator: Navigator, onDismiss: () -> Unit) {
    val snapshot by graph.store.snapshot.collectAsState()
    var ids by remember { mutableStateOf<List<Long>?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { ids = runCatching { graph.td.send(TdApi.GetContacts()).userIds.toList() }.getOrDefault(emptyList()) }
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(stringResource(R.string.new_message), style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        val list = ids
        if (list == null) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
            return@ModalBottomSheet
        }
        val users = list.mapNotNull { snapshot.users[it] }.sortedWith(compareByDescending<com.pixelgram.android.td.UserInfo> { it.online }.thenBy { it.name.lowercase() })
        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            itemsIndexed(users, key = { _, it -> it.id }) { index, user ->
                SegmentedListItem(
                    onClick = {
                        scope.launch {
                            val chat = runCatching { graph.td.send(TdApi.CreatePrivateChat(user.id, false)) }.getOrNull()
                            onDismiss()
                            chat?.let { navigator.open(ChatKey(it.id)) }
                        }
                    },
                    shapes = ListItemDefaults.segmentedShapes(index, users.size),
                    colors = com.pixelgram.android.ui.components.segmentedColors(),
                    leadingContent = {
                        Avatar(user.name, user.id, snapshot.files[user.photoFileId], user.minithumb, 44.dp, onNeedFile = { graph.store.requestFileAsync(user.photoFileId) })
                    },
                    supportingContent = {
                        Text(
                            stringResource(if (user.online) R.string.chat_online else R.string.chat_last_seen_recently),
                            color = if (user.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                ) { Text(user.name) }
            }
        }
    }
}

@Composable
private fun TypingLine() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TypingDots(MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.chat_typing), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1)
    }
}

@Composable
internal fun TypingDots(color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val flow = androidx.compose.animation.core.rememberInfiniteTransition(label = "dots")
    val phase by flow.animateFloat(0f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(1200, easing = androidx.compose.animation.core.LinearEasing)), label = "phase")
    androidx.compose.foundation.Canvas(modifier.size(width = 18.dp, height = 8.dp)) {
        val r = size.height / 2.6f
        for (i in 0..2) {
            val wave = ((phase - i * 0.18f) % 1f + 1f) % 1f
            val lift = kotlin.math.sin(wave * Math.PI.toFloat()).coerceAtLeast(0f)
            drawCircle(color.copy(alpha = 0.4f + 0.6f * lift), r * (0.8f + 0.3f * lift), androidx.compose.ui.geometry.Offset(r + i * (size.width - 2 * r) / 2f, size.height / 2f))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NewChatDialog(graph: AppGraph, navigator: Navigator, channel: Boolean, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(if (channel) R.string.new_channel else R.string.new_group)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(128) },
                    label = { Text(stringResource(if (channel) R.string.new_channel_name else R.string.new_group_name)) },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                androidx.compose.material3.OutlinedTextField(
                    value = about,
                    onValueChange = { about = it.take(255) },
                    label = { Text(stringResource(R.string.new_chat_description)) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                enabled = title.isNotBlank() && !busy,
                onClick = {
                    busy = true
                    scope.launch {
                        runCatching {
                            graph.td.send(TdApi.CreateNewSupergroupChat().apply {
                                this.title = title.trim()
                                isForum = false
                                isChannel = channel
                                description = about.trim()
                            })
                        }.onSuccess {
                            onDismiss()
                            navigator.open(ChatKey(it.id))
                        }.onFailure {
                            busy = false
                            error = it.message
                        }
                    }
                },
            ) { Text(stringResource(R.string.new_chat_create)) }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.action_cancel)) } },
    )
}
