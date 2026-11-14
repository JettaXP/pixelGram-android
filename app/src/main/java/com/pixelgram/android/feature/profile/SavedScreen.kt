package com.pixelgram.android.feature.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.feature.chat.ChatUiMapper
import com.pixelgram.android.feature.chat.MessageBubble
import com.pixelgram.android.feature.chat.UiMessage
import com.pixelgram.android.nav.ChatKey
import com.pixelgram.android.nav.Navigator
import com.pixelgram.android.nav.SavedTopicKey
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.ui.components.Icon
import com.pixelgram.android.util.chatListTime
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

private fun sortedTopics(map: Map<Long, TdApi.SavedMessagesTopic>) =
    map.values.filter { it.order != 0L }.sortedWith(compareByDescending<TdApi.SavedMessagesTopic> { it.isPinned }.thenByDescending { it.order })

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SavedScreen(graph: AppGraph, navigator: Navigator) {
    val snapshot by graph.store.snapshot.collectAsState()
    val topicsMap by graph.store.savedTopics.collectAsState()
    val topics = remember(topicsMap) { sortedTopics(topicsMap) }
    LaunchedEffect(Unit) {
        repeat(20) {
            val more = runCatching { graph.td.send(TdApi.LoadSavedMessagesTopics(100)) }.isSuccess
            if (!more) return@LaunchedEffect
        }
    }
    var tab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf(stringResource(R.string.saved_tab_chats)) + SavedMediaTabs.map { stringResource(it) }
    val meId = snapshot.meId
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp)) {
                IconButton(onClick = { navigator.back() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
                Box(Modifier.size(48.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Bookmark, null, tint = MaterialTheme.colorScheme.onPrimary)
                }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(stringResource(R.string.drawer_saved), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(pluralStringResource(R.plurals.saved_chats, topics.size, topics.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.padding(horizontal = 12.dp)) {
                LazyRow(contentPadding = PaddingValues(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    itemsIndexed(tabs) { index, label ->
                        val active = index == tab
                        Box(
                            Modifier.clip(CircleShape)
                                .background(if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                                .clickable { tab = index }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Text(label, style = MaterialTheme.typography.labelLarge, color = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            val enter = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
            val exit = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
            AnimatedContent(tab, transitionSpec = { fadeIn(enter) togetherWith fadeOut(exit) }, label = "saved-tab", modifier = Modifier.weight(1f)) { current ->
                if (current == 0) {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                    ) {
                        itemsIndexed(topics, key = { _, it -> it.id }) { index, topic ->
                            SavedTopicRow(graph, topic, meId, index, topics.size) { navigator.open(SavedTopicKey(topic.id)) }
                        }
                    }
                } else {
                    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp).navigationBarsPadding()) {
                        SavedMediaTab(graph, navigator, meId, current - 1)
                    }
                }
            }
        }
        MediaSelectionBar(graph, navigator)
    }
}

@Composable
internal fun topicTitle(graph: AppGraph, topic: TdApi.SavedMessagesTopic): String = when (val type = topic.type) {
    is TdApi.SavedMessagesTopicTypeMyNotes -> stringResource(R.string.saved_my_notes)
    is TdApi.SavedMessagesTopicTypeAuthorHidden -> stringResource(R.string.saved_author_hidden)
    is TdApi.SavedMessagesTopicTypeSavedFromChat -> graph.store.chat(type.chatId)?.title ?: stringResource(R.string.saved_chat)
    else -> stringResource(R.string.saved_chat)
}

@Composable
internal fun TopicAvatar(graph: AppGraph, topic: TdApi.SavedMessagesTopic, size: Dp) {
    val snapshot by graph.store.snapshot.collectAsState()
    when (val type = topic.type) {
        is TdApi.SavedMessagesTopicTypeMyNotes -> Box(Modifier.size(size).clip(RoundedCornerShape(size / 4)).background(Color(0xFF26C6DA)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.StickyNote2, null, tint = Color(0xFF00363D), modifier = Modifier.size(size * 0.5f))
        }
        is TdApi.SavedMessagesTopicTypeAuthorHidden -> HiddenAuthorAvatar(size)
        is TdApi.SavedMessagesTopicTypeSavedFromChat -> {
            LaunchedEffect(type.chatId) { if (graph.store.chat(type.chatId) == null) runCatching { graph.td.send(TdApi.GetChat(type.chatId)) } }
            val chat = graph.store.chat(type.chatId)
            val photo = chat?.photo?.small
            Avatar(chat?.title.orEmpty(), type.chatId, photo?.let { snapshot.files[it.id] }, chat?.photo?.minithumbnail?.data, size, onNeedFile = { photo?.let { graph.store.requestFileAsync(it.id) } })
        }
        else -> Avatar("?", topic.id, null, null, size)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HiddenAuthorAvatar(size: Dp) {
    Box(Modifier.size(size).clip(MaterialShapes.Cookie9Sided.toShape()).background(MaterialTheme.colorScheme.error), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Close, null, tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(size * 0.5f))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SavedTopicRow(graph: AppGraph, topic: TdApi.SavedMessagesTopic, meId: Long, index: Int, count: Int, onClick: () -> Unit) {
    val last = topic.lastMessage
    val preview = last?.let { com.pixelgram.android.td.contentPreviewLabel(it.content) }.orEmpty()
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = com.pixelgram.android.ui.components.segmentedColors(),
        leadingContent = { TopicAvatar(graph, topic, 52.dp) },
        trailingContent = { last?.let { Text(chatListTime(it.date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        supportingContent = preview.takeIf { it.isNotBlank() }?.let { { Text(it.replace('\n', ' '), maxLines = 1, overflow = TextOverflow.Ellipsis) } },
    ) {
        Text(topicTitle(graph, topic), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SavedTopicScreen(graph: AppGraph, navigator: Navigator, topicId: Long) {
    val snapshot by graph.store.snapshot.collectAsState()
    val topicsMap by graph.store.savedTopics.collectAsState()
    val topic = topicsMap[topicId]
    val messages = remember(topicId) { mutableStateListOf<UiMessage>() }
    var loading by remember(topicId) { mutableStateOf(false) }
    var done by remember(topicId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val count by produceState(0, topicId) {
        value = runCatching { graph.td.send(TdApi.GetChatMessageCount(snapshot.meId, TdApi.MessageTopicSavedMessages(topicId), TdApi.SearchMessagesFilterEmpty(), false)).count }.getOrDefault(0)
    }
    val loadMore: () -> Unit = {
        if (!loading && !done) {
            loading = true
            scope.launch {
                val from = messages.lastOrNull()?.id ?: 0L
                val page = runCatching { graph.td.send(TdApi.GetSavedMessagesTopicHistory(topicId, from, 0, 50)).messages.toList() }.getOrDefault(emptyList())
                val known = messages.mapTo(HashSet()) { it.id }
                val fresh = with(ChatUiMapper) { page.filter { it.id !in known }.map { it.ui() } }
                messages.addAll(fresh)
                done = fresh.isEmpty()
                loading = false
            }
        }
    }
    val state = rememberLazyListState()
    val nearEnd by remember { derivedStateOf { (state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= state.layoutInfo.totalItemsCount - 5 } }
    LaunchedEffect(nearEnd, messages.size) { if (nearEnd) loadMore() }
    val hidden = topic?.type is TdApi.SavedMessagesTopicTypeAuthorHidden
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
        LazyColumn(
            state = state,
            reverseLayout = true,
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 96.dp, bottom = if (hidden) 96.dp else 24.dp),
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        ) {
            itemsIndexed(messages, key = { _, it -> it.id }) { index, message ->
                val below = messages.getOrNull(index - 1)
                val above = messages.getOrNull(index + 1)
                val joinsAbove = above?.senderId == message.senderId
                val joinsBelow = below?.senderId == message.senderId
                val sender = snapshot.users[message.senderId]
                MessageBubble(
                    message = message.copy(outgoing = false),
                    senderAvatar = if (hidden) ({ if (!joinsBelow) HiddenAuthorAvatar(34.dp) }) else null,
                    joinsAbove = joinsAbove,
                    joinsBelow = joinsBelow,
                    files = snapshot.files,
                    onNeedFile = graph.store::requestFileAsync,
                    onOpenPhoto = { _, _ -> },
                    reply = null,
                    replySender = null,
                    senderName = if (!joinsAbove && !hidden) sender?.name else null,
                    onTap = {
                        navigator.open(ChatKey(snapshot.meId))
                        com.pixelgram.android.nav.PendingNav.jump = snapshot.meId to message.id
                    },
                )
            }
        }
        Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statusBarsPadding().padding(horizontal = 4.dp, vertical = 8.dp)) {
                IconButton(onClick = { navigator.back() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
                if (topic != null) TopicAvatar(graph, topic, 42.dp)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(topic?.let { topicTitle(graph, it) }.orEmpty(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(pluralStringResource(R.plurals.saved_messages_count, count, count), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (hidden) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(12.dp),
            ) {
                Text(stringResource(R.string.saved_hidden_note), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
            }
        }
    }
}
