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
import androidx.compose.material.icons.rounded.ChatBubble
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
                        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                            val icon = topic.info.icon
                            when {
                                icon.customEmojiId != 0L -> com.pixelgram.android.ui.components.CustomEmojiView(icon.customEmojiId, "", size = 36.dp)
                                topic.info.isGeneral -> Icon(Icons.Rounded.Tag, null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                else -> {
                                    val tint = androidx.compose.ui.graphics.Color(0xFF000000.toInt() or icon.color)
                                    Icon(Icons.Rounded.ChatBubble, null, Modifier.size(38.dp), tint = tint)
                                    Text(
                                        topic.info.name.trim().take(1).uppercase(),
                                        color = androidx.compose.ui.graphics.Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 3.dp),
                                    )
                                }
                            }
                            if (topic.info.isClosed) Box(
                                Modifier.align(Alignment.BottomEnd).size(18.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer).padding(2.dp),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Rounded.Lock, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    },
                    supportingContent = {
                        val last = topic.lastMessage
                        val senderUser = (last?.senderId as? TdApi.MessageSenderUser)?.userId
                        val senderChat = (last?.senderId as? TdApi.MessageSenderChat)?.chatId
                        val user = senderUser?.let { snapshot.users[it] }
                        if (senderUser != null && user == null) androidx.compose.runtime.LaunchedEffect(senderUser) { runCatching { graph.td.send(TdApi.GetUser(senderUser)) } }
                        val senderTitle = when {
                            last == null -> null
                            last.isOutgoing -> stringResource(R.string.preview_you)
                            user != null -> user.firstName
                            senderChat != null -> graph.store.chat(senderChat)?.title
                            else -> null
                        }
                        val (kind, text) = last?.let { contentPreview(it.content) } ?: (com.pixelgram.android.td.PreviewKind.Text to "")
                        val label = if (kind == com.pixelgram.android.td.PreviewKind.Text || text.isNotBlank()) text else contentPreviewLabelFor(kind)
                        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (senderTitle != null) {
                                val id = senderUser ?: senderChat ?: 0L
                                val photoId = user?.photoFileId ?: senderChat?.let { graph.store.chat(it)?.photo?.small?.id } ?: 0
                                if (last?.isOutgoing != true) Avatar(senderTitle, id, user?.localPhoto ?: snapshot.files[photoId], user?.minithumb, 18.dp, onNeedFile = { if (photoId != 0) graph.store.requestFileAsync(photoId) })
                                Text(
                                    senderTitle,
                                    color = user?.let { com.pixelgram.android.td.AccentColors.colors(it.accentColorId, com.pixelgram.android.ui.theme.LocalDarkTheme.current).first() } ?: MaterialTheme.colorScheme.primary,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                    maxLines = 1,
                                )
                                Text(":", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(label.replace('\n', ' '), maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    trailingContent = {
                        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(chatListTime(topic.lastMessage?.date ?: 0), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            when {
                                topic.unreadCount > 0 -> Badge(
                                    containerColor = if (topic.notificationSettings.muteFor > 0) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                                    contentColor = if (topic.notificationSettings.muteFor > 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onPrimary,
                                ) { Text(com.pixelgram.android.ui.theme.countLabel(topic.unreadCount, com.pixelgram.android.ui.theme.LocalAppearance.current.exactNumbers)) }
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

@Composable
private fun contentPreviewLabelFor(kind: com.pixelgram.android.td.PreviewKind): String = when (kind) {
    com.pixelgram.android.td.PreviewKind.Photo -> stringResource(R.string.preview_photo)
    com.pixelgram.android.td.PreviewKind.Video -> stringResource(R.string.preview_video)
    com.pixelgram.android.td.PreviewKind.Gif -> stringResource(R.string.preview_gif)
    com.pixelgram.android.td.PreviewKind.Sticker -> stringResource(R.string.preview_sticker)
    com.pixelgram.android.td.PreviewKind.Voice -> stringResource(R.string.preview_voice)
    com.pixelgram.android.td.PreviewKind.VideoNote -> stringResource(R.string.preview_video_note)
    com.pixelgram.android.td.PreviewKind.Audio -> stringResource(R.string.preview_audio)
    com.pixelgram.android.td.PreviewKind.File -> stringResource(R.string.preview_file)
    com.pixelgram.android.td.PreviewKind.Poll -> stringResource(R.string.preview_poll)
    com.pixelgram.android.td.PreviewKind.Location -> stringResource(R.string.preview_location)
    com.pixelgram.android.td.PreviewKind.Contact -> stringResource(R.string.preview_contact)
    else -> stringResource(R.string.preview_service)
}
