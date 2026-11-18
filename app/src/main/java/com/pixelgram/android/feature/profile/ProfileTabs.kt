package com.pixelgram.android.feature.profile

import androidx.compose.material.icons.rounded.Check
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.nav.ChatKey
import com.pixelgram.android.nav.Navigator
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.ui.components.Icon
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.drinkless.tdlib.TdApi

private enum class ProfileTab(val title: Int) {
    Members(R.string.profile_tab_members),
    Posts(R.string.profile_tab_posts),
    Gifts(R.string.profile_tab_gifts),
    Media(R.string.profile_tab_media),
    Files(R.string.profile_tab_files),
    Music(R.string.profile_tab_music),
    Voice(R.string.profile_tab_voice),
    Links(R.string.profile_tab_links),
    Groups(R.string.profile_tab_groups),
}

private fun ProfileTab.filter(): TdApi.SearchMessagesFilter? = when (this) {
    ProfileTab.Media -> TdApi.SearchMessagesFilterPhotoAndVideo()
    ProfileTab.Files -> TdApi.SearchMessagesFilterDocument()
    ProfileTab.Music -> TdApi.SearchMessagesFilterAudio()
    ProfileTab.Voice -> TdApi.SearchMessagesFilterVoiceNote()
    ProfileTab.Links -> TdApi.SearchMessagesFilterUrl()
    else -> null
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileTabs(graph: AppGraph, navigator: Navigator, userId: Long, chatId: Long, full: TdApi.UserFullInfo?, supergroupId: Long = 0L, isChannel: Boolean = false, onOpenUser: (Long) -> Unit = {}) {
    val owner: TdApi.MessageSender = if (userId != 0L) TdApi.MessageSenderUser(userId) else TdApi.MessageSenderChat(chatId)
    val tabs by produceState<List<ProfileTab>?>(null, userId, chatId, full) {
        val chat = if (userId != 0L) runCatching { graph.td.send(TdApi.CreatePrivateChat(userId, true)).id }.getOrNull() ?: 0L else chatId
        value = coroutineScope {
            ProfileTab.entries.map { tab ->
                async {
                    val present = when (tab) {
                        ProfileTab.Members -> supergroupId != 0L && !isChannel
                        ProfileTab.Posts -> runCatching { graph.td.send(TdApi.GetChatPostedToChatPageStories(if (userId != 0L) userId else chatId, 0, 1)).totalCount > 0 }.getOrDefault(false)
                        ProfileTab.Gifts -> if (userId != 0L) (full?.giftCount ?: 0) > 0 else false
                        ProfileTab.Groups -> userId != 0L && (full?.groupInCommonCount ?: 0) > 0
                        else -> chat != 0L && runCatching { graph.td.send(TdApi.GetChatMessageCount(chat, null, tab.filter(), false)).count > 0 }.getOrDefault(false)
                    }
                    tab.takeIf { present }
                }
            }.awaitAll().filterNotNull()
        }
    }
    androidx.compose.animation.AnimatedVisibility(
        visible = !tabs.isNullOrEmpty(),
        enter = androidx.compose.animation.fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) + androidx.compose.animation.expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
    ) {
        tabs?.takeIf { it.isNotEmpty() }?.let { TabsBody(graph, navigator, userId, chatId, owner, supergroupId, onOpenUser, it) }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TabsBody(graph: AppGraph, navigator: Navigator, userId: Long, chatId: Long, owner: TdApi.MessageSender, supergroupId: Long, onOpenUser: (Long) -> Unit, list: List<ProfileTab>) {
    Column {
    var selected by rememberSaveable(userId, chatId) { mutableStateOf(0) }
    val tab = list.getOrElse(selected) { list.first() }
    Spacer(Modifier.size(16.dp))
    androidx.compose.material3.PrimaryScrollableTabRow(
        selectedTabIndex = list.indexOf(tab),
        edgePadding = 16.dp,
        containerColor = Color.Transparent,
        divider = {},
    ) {
        list.forEachIndexed { index, item ->
            androidx.compose.material3.Tab(selected = item == tab, onClick = { selected = index }, text = { Text(stringResource(item.title)) })
        }
    }
    val enter = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val exit = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    AnimatedContent(tab, transitionSpec = { fadeIn(enter) togetherWith fadeOut(exit) }, label = "profile-tab") { current ->
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            when (current) {
                ProfileTab.Members -> MembersList(graph, supergroupId, chatId, onOpenUser)
                ProfileTab.Posts -> PostsGrid(graph, if (userId != 0L) userId else chatId)
                ProfileTab.Gifts -> GiftsGrid(graph, owner)
                ProfileTab.Groups -> GroupsList(graph, navigator, userId)
                ProfileTab.Media -> MediaGrid(graph, navigator, userId, chatId)
                else -> MessageList(graph, navigator, userId, chatId, current)
            }
        }
    }
    }
}

@Composable
private fun Loading() = Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    LoadingIndicator()
}

@Composable
private fun <T> Grid(items: List<T>, columns: Int = 3, cell: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        items.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                row.forEach { Box(Modifier.weight(1f).aspectRatio(1f)) { cell(it) } }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun cellShape(index: Int, count: Int, columns: Int): RoundedCornerShape {
    val big = 20.dp
    val small = 4.dp
    val rows = (count + columns - 1) / columns
    val row = index / columns
    val col = index % columns
    val lastInRow = col == columns - 1 || index == count - 1
    return RoundedCornerShape(
        topStart = if (row == 0 && col == 0) big else small,
        topEnd = if (row == 0 && lastInRow && (col == columns - 1 || rows == 1)) big else small,
        bottomStart = if (row == rows - 1 && col == 0) big else small,
        bottomEnd = if (row == rows - 1 && lastInRow) big else small,
    )
}

object MediaSelection {
    val items = androidx.compose.runtime.mutableStateListOf<TdApi.Message>()
    fun toggle(message: TdApi.Message) {
        val index = items.indexOfFirst { it.id == message.id && it.chatId == message.chatId }
        if (index >= 0) items.removeAt(index) else items.add(message)
    }
    fun has(message: TdApi.Message) = items.any { it.id == message.id && it.chatId == message.chatId }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun Thumb(graph: AppGraph, file: TdApi.File?, mini: ByteArray?, shape: RoundedCornerShape, video: Boolean, selected: Boolean = false, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    val snapshot by graph.store.snapshot.collectAsState()
    val path = file?.let { snapshot.files[it.id] ?: it.local.path.takeIf { p -> it.local.isDownloadingCompleted && p.isNotEmpty() } }
    if (file != null && path == null) androidx.compose.runtime.LaunchedEffect(file.id) { graph.store.requestFileAsync(file.id) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val inset by androidx.compose.animation.core.animateDpAsState(if (selected) 8.dp else 0.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "sel")
    Box(Modifier.fillMaxSize().padding(inset).clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh).combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick?.let { long -> { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); long() } },
    )) {
        AsyncImage(model = path?.let { java.io.File(it) } ?: mini, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (video) {
            Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.45f), contentColor = Color.White, modifier = Modifier.align(Alignment.Center).size(32.dp)) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.padding(4.dp))
            }
        }
        if (selected) {
            Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun TdApi.Photo.thumb(): TdApi.File? = sizes.sortedBy { it.width }.firstOrNull { it.width >= 240 }?.photo ?: sizes.maxByOrNull { it.width }?.photo

@Composable
private fun PostsGrid(graph: AppGraph, chatId: Long) {
    val stories by produceState<List<TdApi.Story>?>(null, chatId) {
        value = runCatching { graph.td.send(TdApi.GetChatPostedToChatPageStories(chatId, 0, 30)).stories.toList() }.getOrNull() ?: emptyList()
    }
    val list = stories ?: return Loading()
    Grid(list.withIndex().toList()) { (index, story) ->
        val shape = cellShape(index, list.size, 3)
        when (val content = story.content) {
            is TdApi.StoryContentPhoto -> Thumb(graph, content.photo.thumb(), content.photo.minithumbnail?.data, shape, false) { com.pixelgram.android.feature.stories.StoryViewer.openList(story.posterChatId, list.map { it.id }, index) }
            is TdApi.StoryContentVideo -> Thumb(graph, content.video.thumbnail?.file, content.video.minithumbnail?.data, shape, true) { com.pixelgram.android.feature.stories.StoryViewer.openList(story.posterChatId, list.map { it.id }, index) }
            else -> Box(Modifier.fillMaxSize().clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh))
        }
    }
}

@Composable
private fun GiftsGrid(graph: AppGraph, owner: TdApi.MessageSender) {
    val changed = com.pixelgram.android.feature.chat.GiftSheets.changed
    val gifts by produceState<List<TdApi.ReceivedGift>?>(null, owner, changed) {
        value = runCatching {
            graph.td.send(TdApi.GetReceivedGifts("", owner, 0, false, false, false, false, false, false, false, false, false, "", 60)).gifts.toList()
        }.getOrNull() ?: emptyList()
    }
    val list = gifts ?: return Loading()
    val snapshot by graph.store.snapshot.collectAsState()
    val own = (owner as? TdApi.MessageSenderUser)?.userId == snapshot.meId
    Grid(list.withIndex().toList()) { (index, gift) ->
        val shape = cellShape(index, list.size, 3)
        val upgraded = (gift.gift as? TdApi.SentGiftUpgraded)?.gift
        Box(
            Modifier.fillMaxSize().clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable { com.pixelgram.android.feature.chat.GiftSheets.current = com.pixelgram.android.feature.chat.GiftTarget.Received(gift, own) },
            contentAlignment = Alignment.Center,
        ) {
            if (upgraded != null) {
                com.pixelgram.android.feature.chat.UniqueGiftArt(upgraded, Modifier.fillMaxSize(), shape = shape, modelScale = 0.66f)
                Text(
                    "#" + "%,d".format(upgraded.number),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(50)).background(com.pixelgram.android.feature.chat.giftColor(upgraded.backdrop?.colors?.symbolColor ?: 0x333333).copy(alpha = 0.85f)).padding(horizontal = 6.dp, vertical = 2.dp),
                )
            } else {
                val sticker = (gift.gift as? TdApi.SentGiftRegular)?.gift?.sticker
                if (sticker != null) com.pixelgram.android.feature.chat.StickerView(sticker, snapshot.files, { graph.store.requestFileAsync(it) }, Modifier.fillMaxSize(0.7f))
                (gift.gift as? TdApi.SentGiftRegular)?.gift?.let { g ->
                    Text("⭐ " + g.starCount, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.BottomCenter).padding(6.dp))
                }
            }
            val sender = (gift.senderId as? TdApi.MessageSenderUser)?.userId
            if (sender != null) {
                val user = snapshot.users[sender]
                Box(Modifier.align(Alignment.TopStart).padding(6.dp)) {
                    Avatar(user?.name.orEmpty(), sender, user?.photoFileId?.let { snapshot.files[it] }, user?.minithumb, 22.dp, onNeedFile = { user?.photoFileId?.let(graph.store::requestFileAsync) })
                }
            }
        }
    }
}

@Composable
private fun MembersList(graph: AppGraph, supergroupId: Long, chatId: Long, onOpenUser: (Long) -> Unit) {
    val tagVersion = com.pixelgram.android.feature.chat.MemberTagsCache.version
    val members by produceState<List<TdApi.ChatMember>?>(null, supergroupId, tagVersion) {
        value = runCatching { graph.td.send(TdApi.GetSupergroupMembers(supergroupId, TdApi.SupergroupMembersFilterRecent(), 0, 200)).members.toList() }.getOrNull() ?: emptyList()
    }
    val list = members ?: return Loading()
    val snapshot by graph.store.snapshot.collectAsState()
    val owner = stringResource(R.string.profile_owner)
    val admin = stringResource(R.string.profile_admin)
    Column(verticalArrangement = Arrangement.spacedBy(androidx.compose.material3.ListItemDefaults.SegmentedGap)) {
        list.forEachIndexed { index, member ->
            val id = (member.memberId as? TdApi.MessageSenderUser)?.userId ?: (member.memberId as? TdApi.MessageSenderChat)?.chatId ?: 0L
            val user = snapshot.users[id]
            if (user == null && member.memberId is TdApi.MessageSenderUser) androidx.compose.runtime.LaunchedEffect(id) { runCatching { graph.td.send(TdApi.GetUser(id)) } }
            val tag = com.pixelgram.android.feature.chat.memberTag(member)
            val admin = member.status is TdApi.ChatMemberStatusCreator || member.status is TdApi.ChatMemberStatusAdministrator
            androidx.compose.material3.SegmentedListItem(
                onClick = { if (member.memberId is TdApi.MessageSenderUser) onOpenUser(id) },
                shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(index, list.size),
                colors = com.pixelgram.android.ui.components.segmentedColors(MaterialTheme.colorScheme.surfaceContainerHigh),
                leadingContent = {
                    Avatar(user?.name.orEmpty(), id, user?.photoFileId?.let { snapshot.files[it] }, user?.minithumb, 44.dp, onNeedFile = { user?.photoFileId?.let(graph.store::requestFileAsync) })
                },
                supportingContent = user?.let { u ->
                    {
                        val line = if (u.bot && admin) stringResource(R.string.bot_reads_all) else com.pixelgram.android.ui.components.statusLine(u)
                        Text(line, color = if (u.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                },
                trailingContent = tag?.let { t ->
                    {
                        val chatTitle = graph.store.chat(chatId)?.title.orEmpty()
                        val mine = id == snapshot.meId
                        val canEdit = mine && (admin || graph.store.chat(chatId)?.permissions?.canEditTag == true)
                        com.pixelgram.android.feature.chat.MemberTagLabel(t) {
                            com.pixelgram.android.feature.chat.TagSheets.current = com.pixelgram.android.feature.chat.TagRequest(chatId, id, user?.name.orEmpty(), chatTitle, t, canEdit)
                        }
                    }
                },
            ) { Text(user?.name ?: "…", maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

@Composable
private fun GroupsList(graph: AppGraph, navigator: Navigator, userId: Long) {
    val chats by produceState<List<TdApi.Chat>?>(null, userId) {
        value = runCatching {
            graph.td.send(TdApi.GetGroupsInCommon(userId, 0, 100)).chatIds.toList().mapNotNull { id: Long -> runCatching { graph.td.send(TdApi.GetChat(id)) }.getOrNull() }
        }.getOrNull() ?: emptyList()
    }
    val list = chats ?: return Loading()
    val snapshot by graph.store.snapshot.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(androidx.compose.material3.ListItemDefaults.SegmentedGap)) {
        list.forEachIndexed { index, chat ->
            androidx.compose.material3.SegmentedListItem(
                onClick = { navigator.open(ChatKey(chat.id)) },
                shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(index, list.size),
                colors = com.pixelgram.android.ui.components.segmentedColors(MaterialTheme.colorScheme.surfaceContainerHigh),
                leadingContent = {
                    val photo = chat.photo?.small
                    Avatar(chat.title, chat.id, photo?.let { snapshot.files[it.id] }, chat.photo?.minithumbnail?.data, 40.dp, onNeedFile = { photo?.let { graph.store.requestFileAsync(it.id) } })
                },
            ) { Text(chat.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

private suspend fun search(graph: AppGraph, userId: Long, chatId: Long, filter: TdApi.SearchMessagesFilter?): List<TdApi.Message> {
    val chat = if (userId != 0L) runCatching { graph.td.send(TdApi.CreatePrivateChat(userId, true)).id }.getOrNull() ?: return emptyList() else chatId
    return runCatching { graph.td.send(TdApi.SearchChatMessages(chat, null, "", null, 0, 0, 60, filter)).messages.toList() }.getOrNull() ?: emptyList()
}

@Composable
private fun MediaGrid(graph: AppGraph, navigator: Navigator, userId: Long, chatId: Long) {
    val messages by produceState<List<TdApi.Message>?>(null, userId, chatId) { value = search(graph, userId, chatId, ProfileTab.Media.filter()) }
    val list = messages ?: return Loading()
    var viewer by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Int?>(null) }
    val ui = androidx.compose.runtime.remember(list) { with(com.pixelgram.android.feature.chat.ChatUiMapper) { list.map { it.ui() } } }
    val snapshot by graph.store.snapshot.collectAsState()
    viewer?.let { start ->
        val photos = ui.filter { it.content is TdApi.MessagePhoto }
        val at = photos.indexOfFirst { it.id == ui[start].id }
        if (at >= 0) com.pixelgram.android.feature.chat.PhotoViewer(photos, at, snapshot.files, graph.store::requestFileAsync) { viewer = null }
        else { viewer = null; navigator.open(ChatKey(list[start].chatId)); com.pixelgram.android.nav.PendingNav.jump = list[start].chatId to list[start].id }
    }
    Grid(list.withIndex().toList()) { (index, message) ->
        val shape = cellShape(index, list.size, 3)
        val selecting = MediaSelection.items.isNotEmpty()
        val open = { if (selecting) MediaSelection.toggle(message) else viewer = index }
        val long = { MediaSelection.toggle(message) }
        val selected = MediaSelection.has(message)
        when (val content = message.content) {
            is TdApi.MessagePhoto -> Thumb(graph, content.photo.thumb(), content.photo.minithumbnail?.data, shape, false, selected, long, open)
            is TdApi.MessageVideo -> Thumb(graph, content.video.thumbnail?.file, content.video.minithumbnail?.data, shape, true, selected, long, open)
            else -> Box(Modifier.fillMaxSize().clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh))
        }
    }
}

@Composable
private fun MessageList(graph: AppGraph, navigator: Navigator, userId: Long, chatId: Long, tab: ProfileTab) {
    val messages by produceState<List<TdApi.Message>?>(null, userId, chatId, tab) { value = search(graph, userId, chatId, tab.filter()) }
    val list = messages ?: return Loading()
    Column(verticalArrangement = Arrangement.spacedBy(androidx.compose.material3.ListItemDefaults.SegmentedGap)) {
        list.forEachIndexed { index, message ->
            val (icon, title, subtitle) = when (val c = message.content) {
                is TdApi.MessageDocument -> Triple(Icons.Rounded.InsertDriveFile, c.document.fileName, size(c.document.document.size))
                is TdApi.MessageAudio -> Triple(Icons.Rounded.MusicNote, c.audio.title.ifBlank { c.audio.fileName }, c.audio.performer)
                is TdApi.MessageVoiceNote -> Triple(Icons.Rounded.Mic, com.pixelgram.android.feature.chat.duration(c.voiceNote.duration), date(message.date))
                is TdApi.MessageText -> {
                    val url = c.text.entities.firstNotNullOfOrNull { e ->
                        (e.type as? TdApi.TextEntityTypeTextUrl)?.url ?: if (e.type is TdApi.TextEntityTypeUrl) c.text.text.substring(e.offset, e.offset + e.length) else null
                    }
                    Triple(Icons.Rounded.Link, url ?: c.text.text, c.linkPreview?.title?.takeIf { it.isNotBlank() } ?: c.text.text)
                }
                else -> Triple(Icons.Rounded.Link, "", "")
            }
            androidx.compose.material3.SegmentedListItem(
                onClick = {
                    if (MediaSelection.items.isNotEmpty()) MediaSelection.toggle(message) else {
                        navigator.open(ChatKey(message.chatId))
                        com.pixelgram.android.nav.PendingNav.jump = message.chatId to message.id
                    }
                },
                onLongClick = { MediaSelection.toggle(message) },
                selected = MediaSelection.has(message),
                shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(index, list.size),
                colors = com.pixelgram.android.ui.components.segmentedColors(MaterialTheme.colorScheme.surfaceContainerHigh),
                leadingContent = {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                supportingContent = subtitle.takeIf { it.isNotBlank() }?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
            ) { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

private fun size(bytes: Long): String = when {
    bytes >= 1 shl 30 -> "%.1f GB".format(bytes / (1 shl 30).toDouble())
    bytes >= 1 shl 20 -> "%.1f MB".format(bytes / (1 shl 20).toDouble())
    bytes >= 1 shl 10 -> "%d KB".format(bytes / (1 shl 10))
    else -> "$bytes B"
}

private fun date(seconds: Int): String = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(seconds * 1000L))

internal val SavedMediaTabs = listOf(R.string.profile_tab_media, R.string.profile_tab_files, R.string.profile_tab_links, R.string.profile_tab_music, R.string.profile_tab_voice)

@Composable
internal fun SavedMediaTab(graph: AppGraph, navigator: Navigator, chatId: Long, index: Int) {
    when (index) {
        0 -> MediaGrid(graph, navigator, 0L, chatId)
        1 -> MessageList(graph, navigator, 0L, chatId, ProfileTab.Files)
        2 -> MessageList(graph, navigator, 0L, chatId, ProfileTab.Links)
        3 -> MessageList(graph, navigator, 0L, chatId, ProfileTab.Music)
        else -> MessageList(graph, navigator, 0L, chatId, ProfileTab.Voice)
    }
}
