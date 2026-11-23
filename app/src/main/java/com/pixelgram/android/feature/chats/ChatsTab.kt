package com.pixelgram.android.feature.chats

import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material3.FloatingActionButtonMenuItem
import com.pixelgram.android.nav.BackHandler
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
import androidx.compose.material.icons.rounded.PlayArrow
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
    onOpenDrawer: (() -> Unit)?,
    onCloseArchive: () -> Unit,
    onOpenArchive: () -> Unit = {},
) {
    val snapshot by graph.store.snapshot.collectAsState()
    val appearance by graph.appearance.state.collectAsState()
    val archive = baseList == LIST_ARCHIVE
    val allTitle = stringResource(R.string.folder_all)
    val folderContext = androidx.compose.ui.platform.LocalContext.current
    com.pixelgram.android.feature.settings.FolderPrefs.init(folderContext)
    val unreadTitle = stringResource(R.string.folders_unread)
    val mentionsTitle = stringResource(R.string.folders_mentions)
    val prefsOrder = com.pixelgram.android.feature.settings.FolderPrefs.order
    val unreadOn = com.pixelgram.android.feature.settings.FolderPrefs.unread
    val mentionsOn = com.pixelgram.android.feature.settings.FolderPrefs.mentions
    val mainRows = snapshot.lists[LIST_MAIN].orEmpty()
    val tabs = remember(snapshot.folders, snapshot.unread, allTitle, archive, appearance.hideAllChats, prefsOrder, unreadOn, mentionsOn, mainRows) {
        if (archive) {
            emptyList()
        } else {
            val main = snapshot.unread[LIST_MAIN]
            val byId = snapshot.folders.associateBy { it.id }
            val virtual = { id: Int -> virtualRows(mainRows, id).let { rows -> rows.size to rows.count { !it.muted } } }
            val all = com.pixelgram.android.feature.settings.FolderPrefs.arrange(com.pixelgram.android.feature.settings.FolderPrefs.all(snapshot.folders))
            all.mapNotNull { id ->
                when (id) {
                    0 -> if (!appearance.hideAllChats || snapshot.folders.isEmpty()) FolderTab(0, allTitle, "All", main?.chats ?: 0, main?.unmuted ?: 0) else null
                    com.pixelgram.android.feature.settings.FolderPrefs.UnreadId -> if (unreadOn) virtual(id).let { (c, u) -> FolderTab(id, unreadTitle, "Unread", c, u) } else null
                    com.pixelgram.android.feature.settings.FolderPrefs.MentionsId -> if (mentionsOn) virtual(id).let { (c, u) -> FolderTab(id, mentionsTitle, "Mention", c, u) } else null
                    else -> byId[id]?.let { f ->
                        val u = snapshot.unread[folderKey(f.id)]
                        FolderTab(f.id, f.title, f.icon ?: "Custom", u?.chats ?: 0, u?.unmuted ?: 0)
                    }
                }
            }
        }
    }
    var folder by rememberSaveable { mutableStateOf(0) }
    val pageCount = tabs.size.coerceAtLeast(1)
    val pager = rememberPagerState(initialPage = tabs.indexOfFirst { it.id == folder }.coerceAtLeast(0)) { pageCount }
    val scope = rememberCoroutineScope()
    val latestTabs by androidx.compose.runtime.rememberUpdatedState(tabs)
    LaunchedEffect(pager) {
        androidx.compose.runtime.snapshotFlow { pager.settledPage }.collect { page ->
            latestTabs.getOrNull(page)?.let { folder = it.id }
        }
    }
    LaunchedEffect(tabs.map { it.id }) {
        val index = tabs.indexOfFirst { it.id == folder }
        if (index >= 0 && index != pager.settledPage && !pager.isScrollInProgress) pager.scrollToPage(index)
    }
    LaunchedEffect(archive) {
        runCatching { graph.td.send(TdApi.LoadActiveStories(if (archive) TdApi.StoryListArchive() else TdApi.StoryListMain())) }
    }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val storyOwners = snapshot.stories.filter { it.archived == archive }
    val storyHeight = with(density) { StoryRowHeight.toPx() }
    val stories = remember { StoryPull(scope, storyHeight) }
    stories.hasStories = storyOwners.isNotEmpty()
    val tracker = remember { HideOnScroll(with(density) { HideDistance.toPx() }, with(density) { ShowDistance.toPx() }) }
    var newMessage by remember { mutableStateOf(false) }
    var fabMenu by remember { mutableStateOf(false) }
    var scrollTop by remember { mutableStateOf(0) }
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
                            androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() - FabLift).coerceAtLeast(0.dp),
                    )
                    .onGloballyPositioned { if (!fabMenu) origin = it.positionInRoot() },
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
                        .graphicsLayer { alpha = if (fabMenu) 0f else 1f }
                        .animateFloatingActionButton(visible && !fabMenu && anchor != null, Alignment.Center),
                ) { Icon(Icons.Rounded.PhotoCamera, stringResource(R.string.stories_new)) }
            }
        },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                        val connection by graph.store.connection.collectAsState()
                        val status = when (connection) {
                            is TdApi.ConnectionStateWaitingForNetwork -> stringResource(R.string.connection_waiting)
                            is TdApi.ConnectionStateConnecting, is TdApi.ConnectionStateConnectingToProxy -> stringResource(R.string.connection_connecting)
                            is TdApi.ConnectionStateUpdating -> stringResource(R.string.connection_updating)
                            else -> null
                        }
                        androidx.compose.animation.AnimatedContent(
                            status,
                            transitionSpec = {
                                (androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it / 2 }) togetherWith
                                    (androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { -it / 2 })
                            },
                            label = "connection-title",
                        ) { line ->
                        if (line != null) Text(line, style = MaterialTheme.typography.titleLargeEmphasized) else Text(
                            when {
                                archive -> stringResource(R.string.drawer_archive)
                                appearance.folderTitle == FolderTitle.Icons && folder != 0 -> tabs.firstOrNull { it.id == folder }?.title ?: stringResource(R.string.app_name)
                                else -> stringResource(R.string.app_name)
                            },
                            style = MaterialTheme.typography.titleLargeEmphasized,
                        )
                        }
                        if (storyOwners.isNotEmpty()) {
                            com.pixelgram.android.feature.stories.StoryStack(
                                graph,
                                storyOwners,
                                Modifier
                                    .padding(start = 10.dp)
                                    .graphicsLayer {
                                        val hidden = (stories.progress * 2f).coerceIn(0f, 1f)
                                        alpha = 1f - hidden
                                        scaleX = 1f - hidden * 0.4f
                                        scaleY = 1f - hidden * 0.4f
                                    },
                            ) { stories.expand() }
                        }
                        }
                    },
                    actions = {
                        IconButton(onClick = { navigator.open(com.pixelgram.android.nav.SearchKey) }) { Icon(Icons.Rounded.Search, stringResource(R.string.search_chats)) }
                    },
                    navigationIcon = {
                        if (archive) {
                            IconButton(onClick = onCloseArchive) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
                        } else {
                            if (onOpenDrawer != null) IconButton(onClick = onOpenDrawer) { Icon(Icons.Rounded.Menu, stringResource(R.string.action_menu)) }
                        }
                    },
                )
                if (storyOwners.isNotEmpty() || !archive) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(with(density) { (storyHeight * stories.progress).toDp() })
                            .graphicsLayer {
                                clip = true
                                alpha = stories.progress
                            },
                    ) {
                        com.pixelgram.android.feature.stories.StoriesRow(
                            graph,
                            storyOwners,
                            Modifier.requiredHeight(StoryRowHeight).align(Alignment.BottomStart).graphicsLayer {
                                val scale = 0.7f + 0.3f * stories.progress.coerceAtMost(1f)
                                scaleX = scale
                                scaleY = scale
                            },
                        )
                    }
                }
                if (tabs.size > 1) {
                    FolderChips(
                        tabs,
                        folder,
                        appearance.folderTitle,
                        onSelect = { id ->
                            val index = tabs.indexOfFirst { it.id == id }
                            if (id == folder) scrollTop++ else if (index >= 0) scope.launch { pager.animateScrollToPage(index, animationSpec = androidx.compose.animation.core.tween(260, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                        },
                        position = { pager.currentPage + pager.currentPageOffsetFraction },
                    )
                }
                val connection by graph.store.connection.collectAsState()
                androidx.compose.animation.AnimatedVisibility(
                    connection !is TdApi.ConnectionStateReady,
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut(),
                ) {
                    androidx.compose.material3.LinearWavyProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
                }
                com.pixelgram.android.ui.components.MiniPlayer()
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pager,
            key = { tabs.getOrNull(it)?.id ?: -1 },
            beyondViewportPageCount = 0,
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()).nestedScroll(stories).nestedScroll(tracker),
        ) { page ->
            val id = tabs.getOrNull(page)?.id ?: 0
            val key = when {
                archive -> LIST_ARCHIVE
                id < 0 -> "virtual:$id"
                id != 0 -> folderKey(id)
                else -> LIST_MAIN
            }
            val archived = if (key == LIST_MAIN && appearance.archivePull) snapshot.lists[LIST_ARCHIVE].orEmpty() else emptyList()
            val pageRows = if (id < 0 && !archive) remember(mainRows, id) { virtualRows(mainRows, id) } else snapshot.lists[key].orEmpty()
            ChatListPage(graph, navigator, key, pageRows, if (page == pager.currentPage) scrollTop else 0, archived, onOpenArchive, stories.takeIf { page == pager.currentPage })
        }
    }
}

private fun virtualRows(rows: List<ChatRow>, id: Int): List<ChatRow> = when (id) {
    com.pixelgram.android.feature.settings.FolderPrefs.UnreadId -> rows.filter { it.unread > 0 }
    com.pixelgram.android.feature.settings.FolderPrefs.MentionsId -> rows.filter { it.mentions > 0 || (it.kind == com.pixelgram.android.td.ChatKind.Private && it.unread > 0) }
    else -> rows
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChatListPage(graph: AppGraph, navigator: Navigator, listKey: String, rows: List<ChatRow>, scrollTop: Int = 0, archived: List<ChatRow> = emptyList(), onOpenArchive: () -> Unit = {}, stories: StoryPull? = null) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val selection = remember(listKey) { androidx.compose.runtime.mutableStateListOf<Long>() }
    val selecting = selection.isNotEmpty()
    androidx.activity.compose.BackHandler(selecting) { selection.clear() }
    LaunchedEffect(rows) { selection.retainAll(rows.map { it.id }.toSet()) }
    val hasArchive = archived.isNotEmpty()
    val listState = rememberLazyListState()
    LaunchedEffect(scrollTop) { if (scrollTop > 0) listState.animateScrollToItem(0) }
    LaunchedEffect(listKey) { if (listKey == LIST_MAIN) runCatching { graph.td.send(TdApi.LoadChats(TdApi.ChatListArchive(), 20)) } }
    var archiveShown by remember { mutableStateOf(false) }
    var pull by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val hintPx = with(androidx.compose.ui.platform.LocalDensity.current) { ArchiveHintHeight.toPx() }
    LaunchedEffect(hasArchive) {
        if (!hasArchive) {
            archiveShown = false
            pull = 0f
        }
    }
    LaunchedEffect(archiveShown) {
        if (archiveShown) {
            androidx.compose.runtime.snapshotFlow { listState.firstVisibleItemIndex }.collect { index -> if (index >= 1) archiveShown = false }
        }
    }
    if (stories != null) androidx.compose.runtime.SideEffect {
        stories.atTop = { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0 }
        stories.archivePulling = { pull > 0f }
    }
    val storiesRef = androidx.compose.runtime.rememberUpdatedState(stories)
    if (stories != null) androidx.compose.runtime.DisposableEffect(stories) {
        onDispose {
            stories.atTop = { false }
            stories.archivePulling = { false }
        }
    }
    val pullScope = rememberCoroutineScope()
    val pullConnection = remember(hasArchive, hintPx) {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            var pullStart = 0L
            var settle: kotlinx.coroutines.Job? = null

            fun atTop() = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0

            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (!hasArchive || archiveShown) return androidx.compose.ui.geometry.Offset.Zero
                if (source != androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) {
                    return if (pull > 0f) available else androidx.compose.ui.geometry.Offset.Zero
                }
                if (available.y > 0f) {
                    if (!atTop()) return androidx.compose.ui.geometry.Offset.Zero
                    val story = storiesRef.value
                    if (story != null && story.hasStories && story.progress < 0.99f) return available
                    settle?.cancel()
                    if (pullStart == 0L) pullStart = System.currentTimeMillis()
                    val k = (pull / hintPx).coerceIn(0f, 1f)
                    pull = (pull + available.y * (PullParallaxStart - k * PullParallaxEnd)).coerceAtMost(hintPx)
                    return available
                }
                if (available.y < 0f && pull > 0f) {
                    settle?.cancel()
                    val used = maxOf(available.y, -pull)
                    pull += used
                    if (pull <= 0f) {
                        pull = 0f
                        pullStart = 0L
                    }
                    return androidx.compose.ui.geometry.Offset(0f, used)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override fun onPostScroll(consumed: androidx.compose.ui.geometry.Offset, available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (hasArchive && !archiveShown && available.y > 0f) return available
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                if (pull <= 0f) {
                    pullStart = 0L
                    return androidx.compose.ui.unit.Velocity.Zero
                }
                val held = pullStart != 0L && System.currentTimeMillis() - pullStart >= PullMinMs
                pullStart = 0L
                if (pull >= hintPx * ReleaseShare && held) {
                    archiveShown = true
                    pull = 0f
                    listState.requestScrollToItem(0)
                } else {
                    settle = pullScope.launch {
                        androidx.compose.animation.core.animate(pull, 0f, animationSpec = androidx.compose.animation.core.tween(320, easing = androidx.compose.animation.core.CubicBezierEasing(0.23f, 1f, 0.32f, 1f))) { value, _ -> pull = value }
                    }
                }
                return available
            }
        }
    }
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
    } else Box(Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
        if (hasArchive) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .layout { measurable, constraints ->
                        val height = pull.toInt().coerceAtLeast(0)
                        val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = androidx.compose.ui.unit.Constraints.Infinity))
                        layout(placeable.width, height) { placeable.place(0, height - placeable.height) }
                    },
            ) {
                ArchivePullHint({ (pull / hintPx).coerceIn(0f, 1f) }, Modifier.padding(start = 12.dp, end = 12.dp, bottom = 6.dp))
            }
        }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp + com.pixelgram.android.ui.components.LocalBottomBarPadding.current),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = Modifier.fillMaxWidth().weight(1f).nestedScroll(pullConnection),
        ) {
            val pinnedCount = rows.takeWhile { it.pinned }.size
            if (hasArchive && archiveShown) item(key = "archive", contentType = "archive") {
                ArchiveRow(archived, Modifier.padding(bottom = 6.dp), onOpenArchive)
            }
            if (listKey == LIST_MAIN) item(key = "login-alert", contentType = "alert") { LoginAlert(graph) }
            itemsIndexed(rows, key = { _, it -> it.id }, contentType = { _, _ -> "chat" }) { index, row ->
                var itemBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
                val inPinned = index < pinnedCount
                if (index == pinnedCount && pinnedCount > 0) Spacer(Modifier.height(10.dp))
                ChatListItem(
                    row = row,
                    index = if (inPinned) index else index - pinnedCount,
                    count = if (inPinned) pinnedCount else rows.size - pinnedCount,
                    selected = row.id in selection,
                    onClick = {
                        if (selecting) {
                            if (row.id in selection) selection.remove(row.id) else selection.add(row.id)
                        } else navigator.open(if (row.forum) com.pixelgram.android.nav.ForumKey(row.id) else ChatKey(row.id))
                    },
                    onNeedPhoto = { graph.store.requestFileAsync(row.photoFileId) },
                    onLongClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        if (row.id in selection) selection.remove(row.id) else selection.add(row.id)
                    },
                    modifier = Modifier.animateItem().onGloballyPositioned { itemBounds = it.boundsInWindow() },
                )
            }
        }
        }
        ChatSelectionBars(graph, rows.filter { it.id in selection }, onClear = { selection.clear() })
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun androidx.compose.foundation.layout.BoxScope.ChatSelectionBars(graph: AppGraph, chosen: List<ChatRow>, onClear: () -> Unit) {
    val scope = rememberCoroutineScope()
    var last by remember { mutableStateOf(chosen) }
    if (chosen.isNotEmpty()) last = chosen
    val rows = last
    var confirmDelete by remember { mutableStateOf(false) }
    fun run(functions: List<TdApi.Function<*>>) {
        scope.launch { functions.forEach { f -> runCatching { @Suppress("UNCHECKED_CAST") graph.td.send(f as TdApi.Function<TdApi.Object>) } } }
        onClear()
    }
    fun listOf(row: ChatRow): TdApi.ChatList = when {
        row.list == LIST_ARCHIVE -> TdApi.ChatListArchive()
        row.list.startsWith("folder:") -> TdApi.ChatListFolder(row.list.removePrefix("folder:").toInt())
        else -> TdApi.ChatListMain()
    }
    val allPinned = rows.isNotEmpty() && rows.all { it.pinned }
    val allMuted = rows.isNotEmpty() && rows.all { it.muted }
    val archived = rows.firstOrNull()?.list == LIST_ARCHIVE
    val visible = chosen.isNotEmpty()
    androidx.compose.animation.AnimatedVisibility(
        visible,
        enter = androidx.compose.animation.slideInVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { -it } + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { -it } + androidx.compose.animation.fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        androidx.compose.material3.Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest, shadowElevation = 6.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                androidx.compose.material3.IconButton(onClick = onClear, shapes = androidx.compose.material3.IconButtonDefaults.shapes()) { Icon(Icons.Rounded.Close, stringResource(R.string.action_cancel)) }
                androidx.compose.animation.AnimatedContent(rows.size, label = "selected-count", modifier = Modifier.weight(1f)) { n ->
                    Text(androidx.compose.ui.res.pluralStringResource(R.plurals.selected_count, n, n), style = MaterialTheme.typography.titleMedium)
                }
                androidx.compose.material3.IconButton(onClick = { run(rows.map { TdApi.ToggleChatIsPinned(listOf(it), it.id, !allPinned) }) }, shapes = androidx.compose.material3.IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.PushPin, stringResource(if (allPinned) R.string.chat_unpin else R.string.chat_pin))
                }
                androidx.compose.material3.IconButton(onClick = {
                    run(rows.filter { it.kind != ChatKind.Saved }.map { row ->
                        TdApi.SetChatNotificationSettings(row.id, TdApi.ChatNotificationSettings().apply {
                            useDefaultMuteFor = false
                            muteFor = if (allMuted) 0 else Int.MAX_VALUE
                            useDefaultSound = true
                            useDefaultShowPreview = true
                            useDefaultMuteStories = true
                            useDefaultShowStoryPoster = true
                            useDefaultDisablePinnedMessageNotifications = true
                            useDefaultDisableMentionNotifications = true
                        })
                    })
                }, shapes = androidx.compose.material3.IconButtonDefaults.shapes()) {
                    Icon(if (allMuted) Icons.AutoMirrored.Rounded.VolumeUp else Icons.Rounded.VolumeOff, stringResource(if (allMuted) R.string.chat_unmute else R.string.chat_mute))
                }
                androidx.compose.material3.IconButton(onClick = { confirmDelete = true }, shapes = androidx.compose.material3.IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.Delete, stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    androidx.compose.animation.AnimatedVisibility(
        visible,
        enter = androidx.compose.animation.slideInVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it } + androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { it } + androidx.compose.animation.fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp + com.pixelgram.android.ui.components.LocalBottomBarPadding.current),
    ) {
        androidx.compose.material3.HorizontalFloatingToolbar(expanded = true) {
            androidx.compose.material3.TextButton(onClick = {
                run(rows.filter { it.lastMessageId != 0L }.map { TdApi.ViewMessages(it.id, longArrayOf(it.lastMessageId), null, true) } + rows.map { TdApi.ToggleChatIsMarkedAsUnread(it.id, false) })
            }) {
                Icon(Icons.Rounded.DoneAll, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.chat_mark_read))
            }
            androidx.compose.material3.TextButton(onClick = {
                run(rows.filter { it.kind != ChatKind.Saved }.map { TdApi.AddChatToList(it.id, if (archived) TdApi.ChatListMain() else TdApi.ChatListArchive()) })
            }) {
                Icon(Icons.Rounded.Archive, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (archived) R.string.chat_unarchive else R.string.chat_archive))
            }
        }
    }
    if (confirmDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Rounded.Delete, null) },
            title = { Text(stringResource(R.string.chat_delete_title)) },
            text = { Text(rows.joinToString { it.title }, maxLines = 4, overflow = TextOverflow.Ellipsis) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    confirmDelete = false
                    run(rows.map { if (it.kind == ChatKind.Group || it.kind == ChatKind.Channel) TdApi.LeaveChat(it.id) else TdApi.DeleteChatHistory(it.id, true, false) })
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatListItem(row: ChatRow, index: Int, count: Int, onClick: () -> Unit, onNeedPhoto: () -> Unit, modifier: Modifier = Modifier, onLongClick: (() -> Unit)? = null, selected: Boolean = false) {
    val container by androidx.compose.animation.animateColorAsState(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer, label = "chat-selected")
    val check by androidx.compose.animation.core.animateFloatAsState(if (selected) 1f else 0f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "chat-check")
    SegmentedListItem(
        onClick = onClick,
        onLongClick = onLongClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        modifier = modifier,
        colors = com.pixelgram.android.ui.components.segmentedColors(container),
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
                    kind = row.kind,
                    official = row.official,
                )
                if (check > 0f) {
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(22.dp).graphicsLayer { scaleX = check; scaleY = check; alpha = check }
                            .background(container, androidx.compose.foundation.shape.CircleShape)
                            .padding(2.dp)
                            .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary) }
                } else if (row.online && row.kind != ChatKind.Saved && row.userId != 777000L) {
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(16.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainer, androidx.compose.foundation.shape.CircleShape)
                            .padding(2.5.dp)
                            .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape),
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
                    ) { com.pixelgram.android.ui.components.AnimatedCounter(row.unread, androidx.compose.material3.LocalTextStyle.current, format = { n -> com.pixelgram.android.ui.theme.countLabel(n, com.pixelgram.android.ui.theme.LocalAppearance.current.exactNumbers) }) }
                    row.pinned -> Icon(Icons.Rounded.PushPin, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                }
            }
        },
        supportingContent = {
            androidx.compose.animation.Crossfade(row.typing, label = "typing") { typing ->
                if (typing) TypingLine() else PreviewText(row, Modifier.fillMaxWidth().then(with(com.pixelgram.android.feature.chat.FadeEnd) { Modifier.fade() }))
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
            NameBadges(row.userId, row.verified, 18.dp, row.scam, row.fake, official = row.official)
            if (row.muted) Icon(Icons.Rounded.VolumeOff, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
        }
    }
}

private class AlbumThumb(val thumb: ByteArray?, val fileId: Int, val playable: Boolean)

private class AlbumInfo(val thumbs: List<AlbumThumb>, val count: Int, val videos: Int)

private val albumCache = java.util.concurrent.ConcurrentHashMap<Long, AlbumInfo>()

@Composable
private fun PreviewText(row: ChatRow, modifier: Modifier) {
    val preview = row.preview
    val graph = com.pixelgram.android.LocalGraph.current
    val album by androidx.compose.runtime.produceState(preview?.albumId?.takeIf { it != 0L }?.let { albumCache[it] }, preview?.albumId, preview?.messageId) {
        val albumId = preview?.albumId ?: 0L
        if (albumId == 0L) return@produceState
        val history = runCatching { graph.td.send(TdApi.GetChatHistory(preview!!.chatId, preview.messageId, 0, 12, false)).messages.toList() }.getOrDefault(emptyList())
        val parts = history.filter { it.mediaAlbumId == albumId }.reversed()
        if (parts.size < 2) return@produceState
        val thumbs = parts.map { m ->
            when (val c = m.content) {
                is TdApi.MessagePhoto -> AlbumThumb(c.photo.minithumbnail?.data, (c.photo.sizes.firstOrNull { it.type == "m" } ?: c.photo.sizes.firstOrNull())?.photo?.id ?: 0, false)
                is TdApi.MessageVideo -> AlbumThumb(c.video.minithumbnail?.data, c.video.thumbnail?.takeIf { it.format is TdApi.ThumbnailFormatJpeg }?.file?.id ?: 0, true)
                else -> AlbumThumb(null, 0, false)
            }
        }
        val info = AlbumInfo(thumbs, parts.size, parts.count { it.content is TdApi.MessageVideo })
        albumCache[albumId] = info
        value = info
    }
    val kindLabel = preview?.kind?.let { previewLabel(it) }
    val albumLabel = album?.let { a ->
        when {
            a.videos == 0 -> androidx.compose.ui.res.pluralStringResource(R.plurals.preview_photos, a.count, a.count)
            a.videos == a.count -> androidx.compose.ui.res.pluralStringResource(R.plurals.preview_videos, a.count, a.count)
            else -> androidx.compose.ui.res.pluralStringResource(R.plurals.preview_media, a.count, a.count)
        }
    }
    val thumbs: List<AlbumThumb> = album?.thumbs?.take(3) ?: if (preview != null && (preview.thumb != null || preview.thumbFileId != 0)) listOf(AlbumThumb(preview.thumb, preview.thumbFileId, preview.playable)) else emptyList()
    val hasThumb = thumbs.isNotEmpty()
    val forward = preview?.forwarded == true
    val prefix = buildString {
        if (row.outgoing && row.kind != ChatKind.Saved) append(stringResource(R.string.preview_you)).append(": ")
        else if (preview?.sender != null) append(preview.sender).append(": ")
        if (forward) append("\u21AA ")
        if (hasThumb) {
            repeat(thumbs.size) { append("\u2B1B") }
            append(" ")
        }
        val label = albumLabel ?: kindLabel
        if (label != null && !(hasThumb && preview?.text.orEmpty().isNotBlank())) {
            append(label)
            if (preview?.text.orEmpty().isNotBlank()) append(", ")
        }
    }
    val raw = preview?.text.orEmpty()
    val body = prefix + raw.replace('\n', ' ')
    val shifted = preview?.entities.orEmpty().map { org.drinkless.tdlib.TdApi.TextEntity(it.offset + prefix.length, it.length, it.type) }
    val annotated = androidx.compose.ui.text.buildAnnotatedString {
        append(body)
        shifted.forEach { e ->
            val type = e.type as? org.drinkless.tdlib.TdApi.TextEntityTypeCustomEmoji ?: return@forEach
            val start = e.offset.coerceIn(0, body.length)
            val end = (e.offset + e.length).coerceIn(start, body.length)
            if (end > start) addStringAnnotation(com.pixelgram.android.ui.components.CustomEmojiTag, com.pixelgram.android.ui.components.customEmojiKey(type.customEmojiId), start, end)
        }
    }
    val style = MaterialTheme.typography.bodyMedium
    val round = preview?.round == true
    val slots = ArrayList<com.pixelgram.android.ui.components.OverlaySlot>()
    if (forward) {
        val at = body.indexOf('\u21AA')
        if (at >= 0) slots += com.pixelgram.android.ui.components.OverlaySlot(at, at + 1) { side ->
            Icon(Icons.AutoMirrored.Rounded.Redo, null, Modifier.size(side), tint = MaterialTheme.colorScheme.primary)
        }
    }
    if (hasThumb) {
        val first = body.indexOf('\u2B1B')
        thumbs.forEachIndexed { i, t ->
            val at = first + i
            slots += com.pixelgram.android.ui.components.OverlaySlot(at, at + 1) { side ->
                val snapshot by graph.store.snapshot.collectAsState()
                val path = t.fileId.takeIf { it != 0 }?.let { snapshot.files[it] }
                if (path == null && t.fileId != 0) androidx.compose.runtime.LaunchedEffect(t.fileId) { graph.store.requestFileAsync(t.fileId) }
                val shape = if (round) androidx.compose.foundation.shape.CircleShape else androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                Box(Modifier.size(side).padding(1.dp).clip(shape), contentAlignment = Alignment.Center) {
                    coil.compose.AsyncImage(model = path?.let { java.io.File(it) } ?: t.thumb, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    if (t.playable) Icon(androidx.compose.material.icons.Icons.Rounded.PlayArrow, null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.fillMaxSize(0.7f))
                }
            }
        }
    }
    com.pixelgram.android.ui.components.OverlayEmojiText(
        annotated,
        com.pixelgram.android.ui.components.customEmojiSlots(shifted, body, clickable = false) + slots,
        style = style,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        softWrap = false,
        maxLines = 1,
        overflow = TextOverflow.Clip,
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
    PreviewKind.Gift -> "🎁 " + stringResource(R.string.preview_gift)
    PreviewKind.Other -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatActions(graph: AppGraph, row: ChatRow, anchor: androidx.compose.ui.geometry.Rect, onDismiss: () -> Unit) {
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
    val entries = buildList<Triple<androidx.compose.ui.graphics.vector.ImageVector, String, () -> Unit>> {
        add(Triple(Icons.Rounded.PushPin, stringResource(if (row.pinned) R.string.chat_unpin else R.string.chat_pin)) { run(TdApi.ToggleChatIsPinned(chatList, row.id, !row.pinned)) })
        if (row.kind != ChatKind.Saved) {
            add(Triple(if (row.muted) Icons.AutoMirrored.Rounded.VolumeUp else Icons.Rounded.VolumeOff, stringResource(if (row.muted) R.string.chat_unmute else R.string.chat_mute)) {
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
            })
            add(Triple(Icons.Rounded.Archive, stringResource(if (row.list == LIST_ARCHIVE) R.string.chat_unarchive else R.string.chat_archive)) {
                run(TdApi.AddChatToList(row.id, if (row.list == LIST_ARCHIVE) TdApi.ChatListMain() else TdApi.ChatListArchive()))
            })
        }
        if (row.unread > 0 && row.lastMessageId != 0L) {
            add(Triple(Icons.Rounded.DoneAll, stringResource(R.string.chat_mark_read)) { run(TdApi.ViewMessages(row.id, longArrayOf(row.lastMessageId), null, true)) })
        }
    }
    val shown = remember { androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } }
    val density = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.ui.window.Popup(
        popupPositionProvider = remember(anchor) {
            object : androidx.compose.ui.window.PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: androidx.compose.ui.unit.IntRect,
                    windowSize: androidx.compose.ui.unit.IntSize,
                    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                    popupContentSize: androidx.compose.ui.unit.IntSize,
                ): androidx.compose.ui.unit.IntOffset {
                    val margin = with(density) { 12.dp.roundToPx() }
                    val x = (anchor.right.toInt() - popupContentSize.width - margin).coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin))
                    val below = anchor.bottom.toInt() + with(density) { 4.dp.roundToPx() }
                    val y = if (below + popupContentSize.height <= windowSize.height - margin) below else (anchor.top.toInt() - popupContentSize.height - with(density) { 4.dp.roundToPx() }).coerceAtLeast(margin)
                    return androidx.compose.ui.unit.IntOffset(x, y)
                }
            }
        },
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.PopupProperties(focusable = true),
    ) {
        androidx.compose.animation.AnimatedVisibility(
            shown,
            enter = androidx.compose.animation.fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) + androidx.compose.animation.scaleIn(MaterialTheme.motionScheme.fastSpatialSpec(), initialScale = 0.8f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)),
        ) {
            androidx.compose.foundation.layout.Column(Modifier.width(240.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 6.dp) {
                    androidx.compose.foundation.layout.Column(Modifier.padding(vertical = 6.dp)) {
                        entries.forEach { (icon, label, action) -> MenuRow(icon, label, MaterialTheme.colorScheme.onSurface, action) }
                    }
                }
                androidx.compose.material3.Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 6.dp) {
                    androidx.compose.foundation.layout.Column(Modifier.padding(vertical = 6.dp)) {
                        MenuRow(Icons.Rounded.Delete, stringResource(if (row.kind == ChatKind.Group || row.kind == ChatKind.Channel) R.string.chat_leave else R.string.chat_delete), MaterialTheme.colorScheme.error) { confirmDelete = true }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, color = tint, style = MaterialTheme.typography.bodyLarge)
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

private class StoryPull(private val scope: kotlinx.coroutines.CoroutineScope, private val height: Float) : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
    private val anim = androidx.compose.animation.core.Animatable(0f)
    val progress: Float get() = anim.value
    var hasStories = false
    var atTop: () -> Boolean = { false }
    var archivePulling: () -> Boolean = { false }

    fun expand() {
        scope.launch { anim.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.75f, stiffness = 380f)) }
    }

    override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
        if (archivePulling()) return androidx.compose.ui.geometry.Offset.Zero
        if (available.y < 0f && anim.value > 0f) {
            val delta = available.y / height
            val next = (anim.value + delta).coerceAtLeast(0f)
            val used = (next - anim.value) * height
            scope.launch { anim.snapTo(next) }
            return androidx.compose.ui.geometry.Offset(0f, used)
        }
        if (available.y > 0f && hasStories && anim.value < 1f && source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput && atTop()) {
            val next = (anim.value + available.y / height).coerceAtMost(1f)
            val used = (next - anim.value) * height
            scope.launch { anim.snapTo(next) }
            return androidx.compose.ui.geometry.Offset(0f, used)
        }
        return androidx.compose.ui.geometry.Offset.Zero
    }

    override fun onPostScroll(consumed: androidx.compose.ui.geometry.Offset, available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
        if (available.y > 0f && hasStories && !archivePulling() && atTop() && source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) {
            val resist = if (anim.value >= 1f) 0.25f else 1f
            val next = (anim.value + available.y * resist / height).coerceAtMost(OverPull)
            scope.launch { anim.snapTo(next) }
            return available
        }
        return androidx.compose.ui.geometry.Offset.Zero
    }

    override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
        val value = anim.value
        if (value <= 0f || archivePulling()) return androidx.compose.ui.unit.Velocity.Zero
        val target = when {
            available.y < -FlingSwitch -> 0f
            available.y > FlingSwitch -> 1f
            value > 0.5f -> 1f
            else -> 0f
        }
        if (value != target) {
            anim.animateTo(target, androidx.compose.animation.core.spring(dampingRatio = 0.75f, stiffness = 380f))
            return if (target == 0f || available.y > 0f) available else androidx.compose.ui.unit.Velocity.Zero
        }
        return androidx.compose.ui.unit.Velocity.Zero
    }
}

private val StoryRowHeight = 104.dp
private const val OverPull = 1.15f
private const val FlingSwitch = 600f

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
private val FabLift = 12.dp
private val ShowDistance = 48.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NewMessageSheet(graph: AppGraph, navigator: Navigator, onDismiss: () -> Unit) {
    val snapshot by graph.store.snapshot.collectAsState()
    var ids by remember { mutableStateOf<List<Long>?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { ids = runCatching { graph.td.send(TdApi.GetContacts()).userIds.toList() }.getOrDefault(emptyList()) }
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
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
    val phase by flow.animateFloat(0f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(1100, easing = androidx.compose.animation.core.LinearEasing)), label = "phase")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically, modifier = modifier.height(14.dp)) {
        repeat(3) { dot ->
            Box(
                Modifier
                    .size(6.dp)
                    .graphicsLayer {
                        val local = ((phase - dot * 0.16f) % 1f + 1f) % 1f
                        val wave = if (local < 0.5f) kotlin.math.sin(local * 2 * Math.PI).toFloat() else 0f
                        translationY = -wave * 4.dp.toPx()
                        alpha = 0.45f + 0.55f * wave
                    }
                    .background(color, androidx.compose.foundation.shape.CircleShape),
            )
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private const val ReleaseShare = 0.85f
private const val PullParallaxStart = 0.45f
private const val PullParallaxEnd = 0.25f
private const val PullMinMs = 200L
private val ArchiveHintHeight = 82.dp

private suspend fun unconfirmedId(graph: AppGraph, session: TdApi.UnconfirmedSession): Long? {
    val sessions = graph.td.send(TdApi.GetActiveSessions()).sessions
    return (sessions.firstOrNull { it.isUnconfirmed && it.deviceModel == session.deviceModel } ?: sessions.firstOrNull { it.isUnconfirmed })?.id
}

@Composable
private fun LoginAlert(graph: AppGraph) {
    val session by graph.store.unconfirmedSession.collectAsState()
    val scope = rememberCoroutineScope()
    androidx.compose.animation.AnimatedVisibility(session != null, enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()) {
        var last by remember { mutableStateOf(session) }
        session?.let { last = it }
        val current = last ?: return@AnimatedVisibility
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        ) {
            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.login_alert_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.login_alert_text, current.deviceModel, current.location),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.FilledTonalButton(onClick = {
                        scope.launch { runCatching { unconfirmedId(graph, current)?.let { graph.td.send(TdApi.ConfirmSession(it)) } } }
                        graph.store.unconfirmedSession.value = null
                    }) { Text(stringResource(R.string.login_alert_yes)) }
                    androidx.compose.material3.Button(
                        onClick = {
                            scope.launch { runCatching { unconfirmedId(graph, current)?.let { graph.td.send(TdApi.TerminateSession(it)) } } }
                            graph.store.unconfirmedSession.value = null
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                    ) { Text(stringResource(R.string.login_alert_no)) }
                }
            }
        }
    }
}

@Composable
private fun ArchivePullHint(pulled: () -> Float, modifier: Modifier) {
    var latched by remember { mutableStateOf(false) }
    val release by remember {
        derivedStateOf {
            val value = pulled()
            if (!latched && value >= ReleaseShare) latched = true
            if (latched && value < ReleaseShare - 0.1f) latched = false
            latched
        }
    }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    LaunchedEffect(release) { if (release) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.SegmentTick) }
    val container by androidx.compose.animation.animateColorAsState(if (release) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest, label = "pull-bg")
    val content = if (release) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val rotation by androidx.compose.animation.core.animateFloatAsState(if (release) 180f else 0f, MaterialTheme.motionScheme.fastSpatialSpec(), label = "pull-arrow")
    Box(
        modifier.fillMaxWidth().height(76.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp)).background(container),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.align(Alignment.CenterStart).padding(start = 22.dp).size(width = 28.dp, height = 52.dp)) {
            Box(Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.CircleShape).background(content.copy(alpha = 0.2f)))
            val travel = with(androidx.compose.ui.platform.LocalDensity.current) { 24.dp.toPx() }
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer { translationY = travel * (pulled() / ReleaseShare).coerceIn(0f, 1f) }
                    .size(28.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(content),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.ArrowDownward, null, Modifier.size(18.dp).graphicsLayer { rotationZ = rotation }, tint = container)
            }
        }
        androidx.compose.animation.AnimatedContent(release, label = "pull-text") { r ->
            Text(stringResource(if (r) R.string.archive_release else R.string.archive_pull), style = MaterialTheme.typography.titleMedium, color = content)
        }
    }
}

@Composable
private fun ArchiveRow(archived: List<ChatRow>, modifier: Modifier, onClick: () -> Unit) {
    val unread = archived.sumOf { it.unread }
    val names = archived.filter { it.unread > 0 }.ifEmpty { archived }.take(6).joinToString(", ") { it.title }
    androidx.compose.material3.SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(0, 1),
        colors = com.pixelgram.android.ui.components.segmentedColors(),
        modifier = modifier.fillMaxWidth(),
        leadingContent = {
            Box(Modifier.size(52.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Archive, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        },
        supportingContent = { Text(names, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
        trailingContent = {
            if (unread > 0) Box(Modifier.clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.outline).padding(horizontal = 7.dp, vertical = 2.dp)) {
                com.pixelgram.android.ui.components.AnimatedCounter(unread, MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.surface)
            }
        },
    ) { Text(stringResource(R.string.drawer_archive), style = MaterialTheme.typography.titleMedium) }
}
