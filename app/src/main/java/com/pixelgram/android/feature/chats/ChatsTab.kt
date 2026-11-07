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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
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
    val tabs = remember(snapshot.folders, snapshot.unread, allTitle, archive) {
        if (archive) {
            emptyList()
        } else {
            val main = snapshot.unread[LIST_MAIN]
            listOf(FolderTab(0, allTitle, "All", main?.chats ?: 0, main?.unmuted ?: 0)) + snapshot.folders.map { f ->
                val u = snapshot.unread[folderKey(f.id)]
                FolderTab(f.id, f.title, f.icon ?: "Custom", u?.chats ?: 0, u?.unmuted ?: 0)
            }
        }
    }
    var folder by rememberSaveable { mutableStateOf(0) }
    val pageCount = tabs.size.coerceAtLeast(1)
    val pager = rememberPagerState(initialPage = tabs.indexOfFirst { it.id == folder }.coerceAtLeast(0)) { pageCount }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager.currentPage, tabs) { tabs.getOrNull(pager.currentPage)?.let { folder = it.id } }
    Scaffold(
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
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pager,
            key = { tabs.getOrNull(it)?.id ?: -1 },
            beyondViewportPageCount = 0,
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
        ) { page ->
            val id = tabs.getOrNull(page)?.id ?: 0
            val key = when {
                archive -> LIST_ARCHIVE
                id != 0 -> folderKey(id)
                else -> LIST_MAIN
            }
            ChatListPage(graph, navigator, key, snapshot.lists[key].orEmpty())
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChatListPage(graph: AppGraph, navigator: Navigator, listKey: String, rows: List<ChatRow>) {
    val listState = rememberLazyListState()
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
            contentPadding = PaddingValues(bottom = 8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(rows, key = { it.id }, contentType = { "chat" }) { row ->
                ChatListItem(
                    row = row,
                    onClick = { navigator.open(ChatKey(row.id)) },
                    onNeedPhoto = { graph.store.requestFileAsync(row.photoFileId) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
fun ChatListItem(row: ChatRow, onClick: () -> Unit, onNeedPhoto: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box {
            Avatar(
                title = row.title,
                seed = row.id,
                path = row.photoPath,
                minithumb = row.minithumb,
                size = 56.dp,
                icon = if (row.kind == ChatKind.Saved) Icons.Rounded.Bookmark else null,
                onNeedFile = if (row.photoFileId != 0) onNeedPhoto else null,
            )
            if (row.online) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.BottomEnd).size(14.dp),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (row.kind == ChatKind.Saved) stringResource(R.string.drawer_saved) else row.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (row.unread > 0) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (row.verified) Icon(Icons.Rounded.Verified, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                if (row.muted) Icon(Icons.Rounded.VolumeOff, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                Box(Modifier.weight(1f))
                Text(chatListTime(row.date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PreviewText(row, Modifier.weight(1f))
                when {
                    row.mentions > 0 -> Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("@") }
                    row.unread > 0 -> Badge(
                        containerColor = if (row.muted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                        contentColor = if (row.muted) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.widthIn(min = 22.dp),
                    ) { Text(if (row.unread > 999) "999+" else row.unread.toString()) }
                    row.pinned -> Icon(Icons.Rounded.PushPin, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                }
            }
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
