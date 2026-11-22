package com.pixelgram.android.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.nav.ChatKey
import com.pixelgram.android.nav.Navigator
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.ui.components.Icon
import com.pixelgram.android.ui.components.segmentedColors
import com.pixelgram.android.util.chatListTime
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.drinkless.tdlib.TdApi

private enum class SearchTab(val title: Int) { Chats(R.string.search_tab_chats), Channels(R.string.search_tab_channels), Apps(R.string.search_tab_apps), Media(R.string.search_tab_media) }

private class Results(
    val local: List<TdApi.Chat> = emptyList(),
    val global: List<TdApi.Chat> = emptyList(),
    val messages: List<TdApi.Message> = emptyList(),
    val media: List<TdApi.Message> = emptyList(),
    val apps: List<TdApi.Chat> = emptyList(),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(graph: AppGraph, navigator: Navigator) {
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf(SearchTab.Chats) }
    var results by remember { mutableStateOf(Results()) }
    var loading by remember { mutableStateOf(false) }
    var showAllGlobal by remember(query) { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LaunchedEffect(query) {
        val text = query.trim()
        if (text.isEmpty()) {
            loading = false
            val apps = runCatching { graph.td.send(TdApi.GetTopChats(TdApi.TopChatCategoryWebAppBots(), 30)).chatIds.toList() }.getOrDefault(emptyList())
            results = Results(apps = apps.mapNotNull { runCatching { graph.td.send(TdApi.GetChat(it)) }.getOrNull() })
            return@LaunchedEffect
        }
        loading = true
        try {
            delay(250)
            results = coroutineScope {
                val local = async {
                    val ids = LinkedHashSet<Long>()
                    text.trim().removePrefix("tg://user?id=").toLongOrNull()?.takeIf { kotlin.math.abs(it) >= 1000 }?.let { raw ->
                        val user = runCatching { graph.td.send(TdApi.GetUser(raw)) }.getOrNull()
                        if (user != null) runCatching { graph.td.send(TdApi.CreatePrivateChat(raw, true)) }.getOrNull()?.let { ids.add(it.id) }
                        else listOf(raw, -raw, -1000000000000L - kotlin.math.abs(raw)).firstNotNullOfOrNull { id -> runCatching { graph.td.send(TdApi.GetChat(id)) }.getOrNull() }?.let { ids.add(it.id) }
                    }
                    runCatching { graph.td.send(TdApi.SearchChats().apply { this.query = text; limit = 50 }).chatIds }.getOrNull()?.forEach(ids::add)
                    runCatching { graph.td.send(TdApi.SearchChatsOnServer().apply { this.query = text; limit = 50 }).chatIds }.getOrNull()?.forEach(ids::add)
                    ids.mapNotNull { runCatching { graph.td.send(TdApi.GetChat(it)) }.getOrNull() }
                }
                val global = async {
                    if (text.length < 3) emptyList()
                    else runCatching { graph.td.send(TdApi.SearchPublicChats().apply { this.query = text }).chatIds.toList() }.getOrDefault(emptyList()).mapNotNull { runCatching { graph.td.send(TdApi.GetChat(it)) }.getOrNull() }
                }
                val messages = async {
                    runCatching {
                        graph.td.send(TdApi.SearchMessages().apply {
                            this.query = text
                            limit = 50
                            offset = ""
                        }).messages.toList()
                    }.getOrDefault(emptyList())
                }
                val media = async {
                    runCatching {
                        graph.td.send(TdApi.SearchMessages().apply {
                            this.query = text
                            limit = 60
                            offset = ""
                            filter = TdApi.SearchMessagesFilterPhotoAndVideo()
                        }).messages.toList()
                    }.getOrDefault(emptyList())
                }
                val l = local.await()
                val g = global.await().filter { c -> l.none { it.id == c.id } }
                Results(l, g, messages.await(), media.await())
            }
        } finally {
            loading = false
        }
    }
    Scaffold(
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.surface).padding(top = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp).padding(top = 36.dp)) {
                    IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(stringResource(R.string.search_chats)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, stringResource(R.string.action_cancel)) } },
                        singleLine = true,
                        shape = CircleShape,
                        colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                        modifier = Modifier.weight(1f).focusRequester(focus),
                    )
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    LazyRow(contentPadding = PaddingValues(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(SearchTab.entries) { t ->
                            val active = t == tab
                            Box(
                                Modifier.clip(CircleShape).background(if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent).clickable { tab = t }.padding(horizontal = 16.dp, vertical = 10.dp),
                            ) {
                                Text(stringResource(t.title), style = MaterialTheme.typography.labelLarge, color = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                SearchTab.Media -> LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxSize(), contentPadding = PaddingValues(4.dp)) {
                    items(results.media, key = { it.chatId.toString() + ":" + it.id }) { message -> MediaCell(graph, message) { open(navigator, message) } }
                }
                SearchTab.Apps -> {
                    val apps = if (query.isBlank()) results.apps else (results.local + results.global).filter { isBot(graph, it) }
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                        item { Header(stringResource(if (query.isBlank()) R.string.search_recent_apps else R.string.search_tab_apps)) }
                        items(apps, key = { it.id }) { chat -> AppRow(graph, chat, navigator) }
                    }
                }
                else -> {
                    val channelsOnly = tab == SearchTab.Channels
                    val filter: (TdApi.Chat) -> Boolean = { c -> !channelsOnly || (c.type as? TdApi.ChatTypeSupergroup)?.isChannel == true }
                    val local = results.local.filter(filter)
                    val global = results.global.filter(filter)
                    val messages = if (channelsOnly) emptyList() else results.messages
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                        items(local, key = { "l" + it.id }) { chat -> ChatRowItem(graph, chat, null) { navigator.open(ChatKey(chat.id)) } }
                        if (global.isNotEmpty()) {
                            item(key = "gh") { Header(stringResource(R.string.search_global), if (global.size > 3) stringResource(if (showAllGlobal) R.string.search_show_less else R.string.search_show_more) else null) { showAllGlobal = !showAllGlobal } }
                            items(if (showAllGlobal) global else global.take(3), key = { "g" + it.id }) { chat ->
                                ChatRowItem(graph, chat, publicSubtitle(graph, chat)) { navigator.open(ChatKey(chat.id)) }
                            }
                        }
                        if (messages.isNotEmpty()) {
                            item(key = "mh") { Header(stringResource(R.string.search_messages)) }
                            items(messages, key = { "m" + it.chatId + ":" + it.id }) { message -> MessageRow(graph, message) { open(navigator, message) } }
                        }
                        if (!loading && query.isNotBlank() && local.isEmpty() && global.isEmpty() && messages.isEmpty()) {
                            item { Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { Text(stringResource(R.string.search_nothing), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                        }
                    }
                }
            }
            if (loading) LoadingIndicator(Modifier.align(Alignment.TopCenter).padding(top = 24.dp))
        }
    }
}

private fun open(navigator: Navigator, message: TdApi.Message) {
    navigator.open(ChatKey(message.chatId))
    com.pixelgram.android.nav.PendingNav.jump = message.chatId to message.id
}

private fun isBot(graph: AppGraph, chat: TdApi.Chat): Boolean {
    val user = (chat.type as? TdApi.ChatTypePrivate)?.userId ?: return false
    return graph.store.snapshot.value.users[user]?.bot == true
}

private fun publicSubtitle(graph: AppGraph, chat: TdApi.Chat): String? {
    val user = (chat.type as? TdApi.ChatTypePrivate)?.userId
    if (user != null) return graph.store.snapshot.value.users[user]?.username?.let { "@$it" }
    return null
}

@Composable
private fun Header(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        if (action != null) Text(action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable(onClick = onAction))
    }
}

@Composable
private fun ChatAvatar(graph: AppGraph, chat: TdApi.Chat, size: androidx.compose.ui.unit.Dp) {
    val snapshot by graph.store.snapshot.collectAsState()
    val photoId = chat.photo?.small?.id ?: 0
    Avatar(chat.title, chat.id, snapshot.files[photoId], chat.photo?.minithumbnail?.data, size, onNeedFile = { if (photoId != 0) graph.store.requestFileAsync(photoId) })
}

@Composable
private fun ChatRowItem(graph: AppGraph, chat: TdApi.Chat, subtitle: String?, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp)) {
        ChatAvatar(graph, chat, 48.dp)
        Column(Modifier.weight(1f)) {
            Text(chat.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1)
        }
    }
}

@Composable
private fun MessageRow(graph: AppGraph, message: TdApi.Message, onClick: () -> Unit) {
    val chat = graph.store.chat(message.chatId)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (chat != null) ChatAvatar(graph, chat, 48.dp)
        Column(Modifier.weight(1f)) {
            Row {
                Text(chat?.title.orEmpty(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(chatListTime(message.date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(com.pixelgram.android.td.contentPreviewLabel(message.content).replace('\n', ' '), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MediaCell(graph: AppGraph, message: TdApi.Message, onClick: () -> Unit) {
    val snapshot by graph.store.snapshot.collectAsState()
    val (file, mini) = when (val c = message.content) {
        is TdApi.MessagePhoto -> (c.photo.sizes.firstOrNull { it.width >= 240 } ?: c.photo.sizes.lastOrNull())?.photo to c.photo.minithumbnail?.data
        is TdApi.MessageVideo -> c.video.thumbnail?.file to c.video.minithumbnail?.data
        else -> null to null
    }
    val path = file?.let { snapshot.files[it.id] }
    if (file != null && path == null) LaunchedEffect(file.id) { graph.store.requestFileAsync(file.id) }
    Box(Modifier.padding(2.dp).aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable(onClick = onClick)) {
        coil.compose.AsyncImage(model = path?.let { java.io.File(it) } ?: mini, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppRow(graph: AppGraph, chat: TdApi.Chat, navigator: Navigator) {
    val snapshot by graph.store.snapshot.collectAsState()
    val user = (chat.type as? TdApi.ChatTypePrivate)?.userId?.let { snapshot.users[it] }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth().clickable { navigator.open(ChatKey(chat.id)) }.padding(horizontal = 16.dp, vertical = 8.dp)) {
        ChatAvatar(graph, chat, 48.dp)
        Column(Modifier.weight(1f)) {
            Text(chat.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            user?.username?.let { Text("@$it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
        }
        Button(onClick = { navigator.open(ChatKey(chat.id)) }, shapes = androidx.compose.material3.ButtonDefaults.shapes()) { Text(stringResource(R.string.search_open)) }
    }
}
