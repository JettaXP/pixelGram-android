package com.pixelgram.android.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.nav.ChatKey
import com.pixelgram.android.nav.Navigator
import com.pixelgram.android.nav.ProfileKey
import com.pixelgram.android.td.contentPreview
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.ui.components.Icon
import com.pixelgram.android.util.chatListTime
import org.drinkless.tdlib.TdApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ForumScreen(graph: AppGraph, chatId: Long, navigator: Navigator) {
    val snapshot by graph.store.snapshot.collectAsState()
    val chat = graph.store.chat(chatId)
    val topics by produceState<List<TdApi.ForumTopic>?>(null, chatId) {
        value = runCatching { graph.td.send(TdApi.GetForumTopics(chatId, "", 0, 0, 0, 100)).topics.toList() }.getOrDefault(emptyList())
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chat?.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) } },
                actions = {
                    IconButton(onClick = { navigator.open(ProfileKey(0, chatId)) }) {
                        val photo = chat?.photo?.small?.id ?: 0
                        Avatar(chat?.title.orEmpty(), chatId, snapshot.files[photo], chat?.photo?.minithumbnail?.data, 36.dp, onNeedFile = { graph.store.requestFileAsync(photo) })
                    }
                },
            )
        },
    ) { padding ->
        val list = topics
        if (list == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { LoadingIndicator() }
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = padding.calculateTopPadding() + 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            itemsIndexed(list, key = { _, it -> it.info.forumTopicId }) { index, topic ->
                SegmentedListItem(
                    onClick = { navigator.open(ChatKey(chatId, topic.info.forumTopicId)) },
                    shapes = ListItemDefaults.segmentedShapes(index, list.size),
                    colors = com.pixelgram.android.ui.components.segmentedColors(),
                    leadingContent = {
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(com.pixelgram.android.ui.components.avatarColor(topic.info.icon.color.toLong())),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(if (topic.info.isClosed) Icons.Rounded.Lock else Icons.Rounded.Tag, null, tint = MaterialTheme.colorScheme.surface)
                        }
                    },
                    supportingContent = {
                        val preview = topic.lastMessage?.let { contentPreview(it.content).second }.orEmpty()
                        Text(preview.replace('\n', ' '), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    trailingContent = {
                        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(chatListTime(topic.lastMessage?.date ?: 0), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            when {
                                topic.unreadCount > 0 -> Badge(containerColor = MaterialTheme.colorScheme.primary) { Text(topic.unreadCount.toString()) }
                                topic.isPinned -> Icon(Icons.Rounded.PushPin, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    },
                ) {
                    Text(topic.info.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
