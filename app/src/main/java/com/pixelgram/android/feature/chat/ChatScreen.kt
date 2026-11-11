package com.pixelgram.android.feature.chat

import java.io.File
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.rounded.Forward
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Download
import androidx.activity.compose.BackHandler
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.toShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Poll
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material.icons.rounded.Bookmark
import org.drinkless.tdlib.TdApi
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Settings
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import com.pixelgram.android.ui.components.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pixelgram.android.AppGraph
import com.pixelgram.android.R
import com.pixelgram.android.nav.Navigator
import com.pixelgram.android.nav.ProfileKey
import com.pixelgram.android.td.StoreSnapshot
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.util.dayHeader
import com.pixelgram.android.util.messageTime
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatScreen(graph: AppGraph, chatId: Long, navigator: Navigator, topicId: Int = 0) {
    val controller = remember(chatId, topicId) { ChatController(graph, chatId, topicId) }
    DisposableEffect(controller) { onDispose { controller.close() } }
    val ui by controller.state.collectAsState()
    val snapshot by graph.store.snapshot.collectAsState()
    val chat = remember(chatId, snapshot) { graph.store.chat(chatId) }
    val privateUserId = (chat?.type as? org.drinkless.tdlib.TdApi.ChatTypePrivate)?.userId ?: 0L
    val saved = privateUserId != 0L && privateUserId == snapshot.meId
    var viewer by remember { mutableStateOf<Pair<List<UiMessage>, Int>?>(null) }
    var menuFor by remember { mutableStateOf<UiMessage?>(null) }
    var forwarding by remember { mutableStateOf<UiMessage?>(null) }
    var deleting by remember { mutableStateOf<UiMessage?>(null) }
    val appearance by graph.appearance.state.collectAsState()
    val listState = rememberLazyListState()
    val nearTop by remember { derivedStateOf { (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= ui.messages.size - 10 } }
    LaunchedEffect(nearTop, ui.loading) { if (nearTop && !ui.loading && ui.messages.isNotEmpty()) controller.loadMore() }
    val newest = ui.messages.firstOrNull()?.id
    LaunchedEffect(newest) { if (listState.firstVisibleItemIndex <= 2) listState.animateScrollToItem(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = if (saved) Modifier else Modifier.clickable { navigator.open(ProfileKey(privateUserId, chatId)) },
                    ) {
                        Text(
                            when {
                                saved -> stringResource(R.string.drawer_saved)
                                snapshot.users[privateUserId]?.deleted == true -> stringResource(R.string.chat_deleted_account)
                                else -> snapshot.users[privateUserId]?.name ?: ui.title
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (!saved) {
                            val row = snapshot.lists.values.firstNotNullOfOrNull { list -> list.firstOrNull { it.id == chatId } }
                            com.pixelgram.android.ui.components.NameBadges(privateUserId, snapshot.users[privateUserId]?.verified == true || row?.verified == true, 20.dp, row?.scam == true, row?.fake == true)
                        }
                    }
                },
                subtitle = {
                    when {
                        saved -> Unit
                        ui.typing.isNotEmpty() -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            com.pixelgram.android.feature.chats.TypingDots(MaterialTheme.colorScheme.primary)
                            Text(
                                if (privateUserId != 0L || ui.typing.size > 1) stringResource(R.string.chat_typing)
                                else stringResource(R.string.chat_typing_named, snapshot.users[ui.typing.first()]?.firstName.orEmpty()),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                            )
                        }
                        privateUserId != 0L -> snapshot.users[privateUserId]?.let { Text(com.pixelgram.android.ui.components.statusLine(it), maxLines = 1) }
                        ui.members > 0 -> Text(
                            buildString {
                                append(androidx.compose.ui.res.pluralStringResource(if (ui.channel) R.plurals.chat_subscribers else R.plurals.chat_members, ui.members, ui.members))
                                if (ui.online > 1 && !ui.channel) append(", ").append(androidx.compose.ui.res.pluralStringResource(R.plurals.chat_online_count, ui.online, ui.online))
                            },
                            maxLines = 1,
                        )
                    }
                },
                titleHorizontalAlignment = Alignment.CenterHorizontally,
                navigationIcon = {
                    IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
                },
                actions = {
                    IconButton(onClick = { if (!saved) navigator.open(ProfileKey(privateUserId, chatId)) }, enabled = !saved, modifier = Modifier.size(48.dp)) {
                        Avatar(
                            title = ui.title,
                            seed = chatId,
                            path = if (saved) null else snapshot.users[privateUserId]?.localPhoto ?: chat?.photo?.small?.id?.let { snapshot.files[it] },
                            minithumb = if (saved) null else chat?.photo?.minithumbnail?.data,
                            size = 40.dp,
                            icon = when {
                                saved -> Icons.Rounded.Bookmark
                                snapshot.users[privateUserId]?.deleted == true -> com.pixelgram.android.ui.icons.GhostIcon
                                else -> null
                            },
                            onNeedFile = if (saved) null else ({ chat?.photo?.small?.id?.let(graph.store::requestFileAsync) }),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (snapshot.users[privateUserId]?.deleted == true) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.chat_deleted_input),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars).padding(vertical = 18.dp),
                    )
                }
            } else {
                Composer(ui.replyTo, ui.editing, controller)
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (appearance.typingBubble) {
                    item(key = "typing", contentType = "typing") {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = ui.typing.isNotEmpty(),
                            enter = androidx.compose.animation.expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + androidx.compose.animation.scaleIn(MaterialTheme.motionScheme.defaultSpatialSpec(), initialScale = 0.6f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f)) + fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + androidx.compose.animation.scaleOut(targetScale = 0.6f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f)) + fadeOut(),
                        ) {
                            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(appearance.bubbleRadius.dp, appearance.bubbleRadius.dp, appearance.bubbleRadius.dp, appearance.bubbleTailRadius.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                ) {
                                    com.pixelgram.android.feature.chats.TypingDots(MaterialTheme.colorScheme.onSurfaceVariant, Modifier.padding(horizontal = 16.dp, vertical = 14.dp).size(width = 28.dp, height = 10.dp))
                                }
                            }
                        }
                    }
                }
                itemsIndexed(ui.messages, key = { _, m -> m.id }, contentType = { _, m -> if (m.service) "service" else "bubble" }) { index, message ->
                    val older = ui.messages.getOrNull(index + 1)
                    val newer = ui.messages.getOrNull(index - 1)
                    val joinsAbove = older != null && !older.service && older.senderId == message.senderId && message.date - older.date < 300
                    val joinsBelow = newer != null && !newer.service && newer.senderId == message.senderId && newer.date - message.date < 300
                    Column(Modifier.animateItem()) {
                        if (older == null || !sameDay(older.date, message.date)) DayChip(message.date)
                        if (message.service) {
                            ServiceMessage(message)
                        } else {
                            SwipeToReply(onReply = { controller.reply(message) }) {
                                val reply = message.replyToId.takeIf { it != 0L }?.let { id ->
                                    ui.messages.firstOrNull { it.id == id || it.parts.any { p -> p.id == id } } ?: ui.replies[id]
                                }
                                val group = privateUserId == 0L && !ui.channel && !saved
                                val sender = if (group && !message.outgoing) snapshot.users[message.senderId] else null
                                MessageBubble(
                                    message = message,
                                    senderName = sender?.name?.takeIf { !joinsAbove },
                                    senderAvatar = if (group && !message.outgoing) {
                                        {
                                            if (!joinsBelow) {
                                                Avatar(
                                                    title = sender?.name.orEmpty(),
                                                    seed = message.senderId,
                                                    path = sender?.localPhoto ?: sender?.photoFileId?.let { snapshot.files[it] },
                                                    minithumb = sender?.minithumb,
                                                    size = 34.dp,
                                                    onNeedFile = { sender?.photoFileId?.let(graph.store::requestFileAsync) },
                                                    modifier = Modifier.clickable { navigator.open(ProfileKey(message.senderId)) },
                                                )
                                            }
                                        }
                                    } else {
                                        null
                                    },
                                    joinsAbove = joinsAbove,
                                    joinsBelow = joinsBelow,
                                    files = snapshot.files,
                                    onNeedFile = graph.store::requestFileAsync,
                                    onOpenPhoto = { list, index -> viewer = list to index },
                                    reply = reply,
                                    replySender = reply?.let { senderName(snapshot, it.senderId, chat) },
                                    onLongPress = { menuFor = message },
                                    onDoubleTap = { controller.react(message, appearance.doubleTapEmoji) },
                                    onReact = { emoji -> controller.react(message, emoji) },
                                    onPlayAudio = { audio ->
                                        val all = ui.messages.reversed().mapNotNull { (it.content as? TdApi.MessageAudio)?.audio }
                                        val start = all.indexOfFirst { it.audio.id == audio.audio.id }.coerceAtLeast(0)
                                        if (graph.music.state.value.track?.fileId == audio.audio.id) graph.music.toggle()
                                        else graph.music.play(all.ifEmpty { listOf(audio) }, start, ui.title)
                                    },
                                )
                            }
                        }
                    }
                }
                if (ui.loading) {
                    item(key = "loading") {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
                    }
                }
            }
        }
    }
    viewer?.let { (list, index) ->
        PhotoViewer(list, index, snapshot.files, graph.store::requestFileAsync) { viewer = null }
    }
    menuFor?.let { message ->
        MessageActions(
            message = message,
            controller = controller,
            onDismiss = { menuFor = null },
            onForward = { forwarding = message },
            onDelete = { deleting = message },
        )
    }
    forwarding?.let { message ->
        com.pixelgram.android.ui.components.ChatPickerSheet(
            onPick = { target ->
                controller.forward(message, target)
                forwarding = null
            },
            onDismiss = { forwarding = null },
            title = stringResource(R.string.forward_to),
        )
    }
    deleting?.let { message ->
        DeleteDialog(message, controller, privateUserId, snapshot) { deleting = null }
    }
}

private fun senderName(snapshot: StoreSnapshot, senderId: Long, chat: TdApi.Chat?): String =
    snapshot.users[senderId]?.name ?: chat?.title.orEmpty()

@Composable
private fun subtitle(snapshot: StoreSnapshot, userId: Long): String? {
    val user = snapshot.users[userId] ?: return null
    return when {
        user.bot -> stringResource(R.string.chat_bot)
        user.online -> stringResource(R.string.chat_online)
        user.lastSeen > 0 -> stringResource(R.string.chat_last_seen, com.pixelgram.android.util.chatListTime(user.lastSeen), messageTime(user.lastSeen))
        else -> stringResource(R.string.chat_last_seen_recently)
    }
}

private fun sameDay(a: Int, b: Int): Boolean {
    val zone = ZoneId.systemDefault()
    return Instant.ofEpochSecond(a.toLong()).atZone(zone).toLocalDate() == Instant.ofEpochSecond(b.toLong()).atZone(zone).toLocalDate()
}

@Composable
private fun DayChip(date: Int) {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        SuggestionChip(
            onClick = {},
            label = { Text(dayHeader(date)) },
            shape = androidx.compose.foundation.shape.CircleShape,
            border = null,
            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        )
    }
}

@Composable
private fun ServiceMessage(message: UiMessage) {
    Box(Modifier.fillMaxWidth().padding(vertical = 2.dp), contentAlignment = Alignment.Center) {
        SuggestionChip(
            onClick = {},
            label = { Text(message.text.ifBlank { stringResource(R.string.preview_service) }, maxLines = 2) },
            shape = androidx.compose.foundation.shape.CircleShape,
            border = null,
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SwipeToReply(onReply: () -> Unit, content: @Composable () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val offset = remember { androidx.compose.animation.core.Animatable(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val trigger = with(density) { ReplyTrigger.toPx() }
    val limit = with(density) { ReplyLimit.toPx() }
    var armed by remember { mutableStateOf(false) }
    val progress = (-offset.value / trigger).coerceIn(0f, 1f)
    Box(
        Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (armed) {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onReply()
                    }
                    armed = false
                    scope.launch { offset.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 400f)) }
                },
                onDragCancel = {
                    armed = false
                    scope.launch { offset.animateTo(0f) }
                },
            ) { change, amount ->
                val current = -offset.value
                val resist = 1f - (current / limit).coerceIn(0f, 0.85f)
                val next = (current - amount * resist).coerceIn(0f, limit)
                if (next != current) change.consume()
                scope.launch { offset.snapTo(-next) }
                val reached = next >= trigger
                if (reached && !armed) haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                armed = reached
            }
        },
    ) {
        if (progress > 0f) {
            Box(
                Modifier.matchParentSize().padding(end = 12.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(Modifier.size(40.dp).graphicsLayer { scaleX = 0.6f + 0.4f * progress; scaleY = 0.6f + 0.4f * progress; alpha = progress }, contentAlignment = Alignment.Center) {
                    CircularWavyProgressIndicator(progress = { progress }, modifier = Modifier.size(40.dp))
                    Icon(Icons.AutoMirrored.Rounded.Reply, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Box(Modifier.graphicsLayer { translationX = offset.value }) { content() }
    }
}

private val ReplyTrigger = 64.dp
private val EmojiPanelHeight = 300.dp
private val ReplyLimit = 110.dp

@Composable
internal fun MessageBubble(
    message: UiMessage,
    senderName: String? = null,
    senderAvatar: (@Composable () -> Unit)? = null,
    joinsAbove: Boolean,
    joinsBelow: Boolean,
    files: Map<Int, String>,
    onNeedFile: (Int) -> Unit,
    onOpenPhoto: (List<UiMessage>, Int) -> Unit,
    reply: UiMessage?,
    replySender: String?,
    onLongPress: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onPlayAudio: (TdApi.Audio) -> Unit = {},
    onReact: (String) -> Unit = {},
) {
    val prefs = com.pixelgram.android.ui.theme.LocalAppearance.current
    val big = prefs.bubbleRadius.dp
    val small = prefs.bubbleTailRadius.dp
    val shape = if (message.outgoing) {
        RoundedCornerShape(big, if (joinsAbove) small else big, if (joinsBelow) small else big, big)
    } else {
        RoundedCornerShape(if (joinsAbove) small else big, big, big, if (joinsBelow) small else big)
    }
    val container = if (message.outgoing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val onContainer = if (message.outgoing) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier.fillMaxWidth().padding(top = if (joinsAbove) 2.dp else 6.dp),
        horizontalArrangement = if (message.outgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (senderAvatar != null) {
            Box(Modifier.size(34.dp)) { senderAvatar() }
            Spacer(Modifier.width(6.dp))
        }
        Surface(
            shape = shape,
            color = container,
            contentColor = onContainer,
            modifier = Modifier.widthIn(max = 320.dp).clip(shape).combinedClickable(onClick = {}, onLongClick = onLongPress, onDoubleClick = onDoubleTap),
        ) {
            Column(Modifier.padding(4.dp)) {
                if (senderName != null) {
                    Text(
                        senderName,
                        style = MaterialTheme.typography.labelLarge,
                        color = com.pixelgram.android.ui.components.avatarColor(message.senderId),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp),
                    )
                }
                if (reply != null) ReplyQuote(reply, replySender.orEmpty(), files)
                val photos = if (message.parts.isNotEmpty()) message.parts else listOf(message).filter { it.mediaFileId != 0 && it.mediaWidth > 0 }
                if (photos.isNotEmpty()) PhotoGrid(photos, files, onNeedFile, onOpenPhoto)
                (message.content as? TdApi.MessageDocument)?.let { DocumentRow(it.document, onContainer) }
                (message.content as? TdApi.MessageAudio)?.let { AudioRow(it.audio, message.outgoing) { onPlayAudio(it.audio) } }
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val bigEmoji = remember(message.text, message.entities) { bigCustomEmoji(message.text, message.entities) }
                    if (bigEmoji != null && !prefs.zalgoFilter) {
                        Row(Modifier.weight(1f, fill = false), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            bigEmoji.forEach { (id, alt) -> com.pixelgram.android.ui.components.CustomEmojiView(id, alt, size = BigEmojiSize) }
                        }
                    } else if (message.text.isNotBlank()) {
                        val text = if (prefs.zalgoFilter) com.pixelgram.android.util.stripZalgo(message.text) else message.text
                        val entities = if (prefs.zalgoFilter) emptyList() else message.entities
                        Text(
                            formatted(text, entities, MaterialTheme.colorScheme.primary, onContainer),
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = prefs.messageFontSize.sp, lineHeight = (prefs.messageFontSize * 1.4f).sp),
                            inlineContent = remember(text, entities, prefs.messageFontSize) { com.pixelgram.android.ui.components.customEmojiContent(entities, text, (prefs.messageFontSize * 1.25f).sp) },
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (message.edited) Text(stringResource(R.string.message_edited), style = MaterialTheme.typography.labelSmall, color = onContainer.copy(alpha = 0.7f))
                        Text(messageTime(message.date, prefs.messageSeconds), style = MaterialTheme.typography.labelSmall, color = onContainer.copy(alpha = 0.7f))
                        if (message.outgoing) {
                            Icon(
                                when {
                                    message.failed -> Icons.Rounded.ErrorOutline
                                    message.pending -> Icons.Rounded.Schedule
                                    message.read -> Icons.Rounded.DoneAll
                                    else -> Icons.Rounded.Done
                                },
                                null,
                                Modifier.size(14.dp),
                                tint = if (message.failed) MaterialTheme.colorScheme.error else onContainer.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
                if (message.reactions.isNotEmpty()) {
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 4.dp),
                    ) {
                        message.reactions.forEach { reaction ->
                            val chosen = reaction.chosen
                            Surface(
                                onClick = { onReact(reaction.emoji) },
                                shape = RoundedCornerShape(if (chosen) 16.dp else 10.dp),
                                color = if (chosen) MaterialTheme.colorScheme.primary else onContainer.copy(alpha = 0.1f),
                                contentColor = if (chosen) MaterialTheme.colorScheme.onPrimary else onContainer,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    Text(reaction.emoji, style = MaterialTheme.typography.bodyMedium)
                                    Text(reaction.count.toString(), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AudioRow(audio: TdApi.Audio, outgoing: Boolean, onPlay: () -> Unit) {
    val music = com.pixelgram.android.LocalGraph.current.music
    val state by music.state.collectAsState()
    val current = state.track?.fileId == audio.audio.id
    val playing = current && state.playing
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp).widthIn(min = 220.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
            if (current && state.duration > 0) {
                androidx.compose.material3.CircularWavyProgressIndicator(
                    progress = { (state.position.toFloat() / state.duration).coerceIn(0f, 1f) },
                    modifier = Modifier.size(48.dp),
                    color = if (outgoing) scheme.onPrimaryContainer else scheme.primary,
                    amplitude = { if (playing) 1f else 0f },
                )
            }
            androidx.compose.material3.FilledIconButton(
                onClick = onPlay,
                shapes = androidx.compose.material3.IconButtonDefaults.shapes(),
                colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (outgoing) scheme.onPrimaryContainer else scheme.primary,
                    contentColor = if (outgoing) scheme.primaryContainer else scheme.onPrimary,
                ),
                modifier = Modifier.size(40.dp),
            ) {
                com.pixelgram.android.feature.player.PlayPauseMorph(playing, 24.dp)
            }
        }
        Column(Modifier.weight(1f, fill = false)) {
            Text(audio.title.ifBlank { audio.fileName }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(
                listOf(audio.performer, com.pixelgram.android.feature.player.formatTime(audio.duration * 1000L)).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DocumentRow(document: TdApi.Document, tint: Color) {
    val graph = com.pixelgram.android.LocalGraph.current
    val context = LocalContext.current
    val backup = com.pixelgram.android.settings.Backup.isBackup(document.fileName)
    val apk = document.fileName.endsWith(".apk", ignoreCase = true)
    var importing by remember { mutableStateOf<File?>(null) }
    val snapshot by graph.store.snapshot.collectAsState()
    val fileId = document.document.id
    val progress = snapshot.progress[fileId]
    val path = snapshot.files[fileId] ?: document.document.local.path.takeIf { document.document.local.isDownloadingCompleted && it.isNotEmpty() }
    val downloading = path == null && progress?.active == true
    val total = progress?.total?.takeIf { it > 0 } ?: maxOf(document.document.size, document.document.expectedSize)
    val done = progress?.downloaded ?: document.document.local.downloadedSize
    val noApp = stringResource(R.string.file_no_app)
    val open: (String) -> Unit = { file ->
        when {
            backup -> importing = File(file)
            else -> runCatching {
                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", File(file))
                val type = if (apk) "application/vnd.android.package-archive" else context.contentResolver.getType(uri)
                    ?: android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(File(file).extension.lowercase()) ?: "*/*"
                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW).setDataAndType(uri, type).addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            }.onFailure { android.widget.Toast.makeText(context, noApp, android.widget.Toast.LENGTH_SHORT).show() }
        }
    }
    ListItem(
        modifier = Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(16.dp)).clickable {
            when {
                path != null -> open(path)
                downloading -> graph.store.cancelDownload(fileId)
                else -> graph.store.download(fileId)
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent, headlineColor = tint, supportingColor = tint.copy(alpha = 0.7f)),
        leadingContent = {
            Box(Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                val onPrimary = MaterialTheme.colorScheme.onPrimary
                val swapFade = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
                val swapScale = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
                androidx.compose.animation.AnimatedContent(
                    targetState = when {
                        downloading -> 1
                        path == null -> 0
                        else -> 2
                    },
                    transitionSpec = {
                        (fadeIn(swapFade) + androidx.compose.animation.scaleIn(swapScale, initialScale = 0.6f)) togetherWith
                            (fadeOut(swapFade) + androidx.compose.animation.scaleOut(swapScale, targetScale = 0.6f))
                    },
                    contentAlignment = Alignment.Center,
                    label = "file-icon",
                ) { stage ->
                    when (stage) {
                        1 -> Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            val shown by androidx.compose.animation.core.animateFloatAsState(
                                (if (total > 0) done.toFloat() / total else 0f).coerceIn(0.02f, 1f),
                                MaterialTheme.motionScheme.defaultEffectsSpec(),
                                label = "file-progress",
                            )
                            CircularWavyProgressIndicator(
                                progress = { shown },
                                modifier = Modifier.size(40.dp),
                                color = onPrimary,
                                trackColor = onPrimary.copy(alpha = 0.25f),
                                amplitude = { value -> if (value < 0.8f) 1f else ((1f - value) / 0.2f).coerceIn(0f, 1f) },
                            )
                            Icon(Icons.Rounded.Close, null, tint = onPrimary, modifier = Modifier.size(17.dp))
                        }
                        0 -> Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            CircularWavyProgressIndicator(
                                progress = { 0f },
                                modifier = Modifier.size(40.dp),
                                color = onPrimary,
                                trackColor = onPrimary.copy(alpha = 0.45f),
                                waveSpeed = 0.dp,
                            )
                            Icon(Icons.Rounded.ArrowDownward, null, tint = onPrimary, modifier = Modifier.size(20.dp))
                        }
                        else -> Icon(
                            when {
                                backup -> Icons.Rounded.Settings
                                apk -> Icons.Rounded.Android
                                else -> Icons.AutoMirrored.Rounded.InsertDriveFile
                            },
                            null,
                            tint = onPrimary,
                        )
                    }
                }
            }
        },
        headlineContent = { Text(document.fileName.ifBlank { "File" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            val size = android.text.format.Formatter.formatShortFileSize(context, total)
            Text(if (downloading) "${android.text.format.Formatter.formatShortFileSize(context, done)} / $size" else size)
        },
    )
    importing?.let { file ->
        com.pixelgram.android.ui.components.BackupImportDialog(file) { result ->
            importing = null
            if (result != null) android.widget.Toast.makeText(context, result, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun ReplyQuote(reply: UiMessage, sender: String, files: Map<Int, String>) {
    val scheme = MaterialTheme.colorScheme
    val thumb = reply.parts.firstOrNull() ?: reply.takeIf { it.mediaFileId != 0 }
    Row(
        Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.primary.copy(alpha = 0.12f))
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(scheme.primary))
        if (thumb != null) {
            AsyncImage(
                model = thumb.localPath?.let(::File) ?: files[thumb.mediaFileId]?.let(::File) ?: thumb.mediaThumb,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.padding(start = 8.dp).size(36.dp).clip(RoundedCornerShape(8.dp)),
            )
        }
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            if (sender.isNotBlank()) Text(sender, style = MaterialTheme.typography.labelLarge, color = scheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                reply.text.ifBlank { if (thumb != null) stringResource(R.string.preview_photo) else stringResource(R.string.preview_service) },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun PhotoGrid(photos: List<UiMessage>, files: Map<Int, String>, onNeedFile: (Int) -> Unit, onOpen: (List<UiMessage>, Int) -> Unit) {
    val rows = when (photos.size) {
        1 -> listOf(photos)
        2 -> listOf(photos)
        3 -> listOf(photos.take(1), photos.drop(1))
        4 -> photos.chunked(2)
        else -> listOf(photos.take(2)) + photos.drop(2).chunked(3)
    }
    Column(Modifier.widthIn(max = 312.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        var offset = 0
        rows.forEachIndexed { rowIndex, row ->
            val start = offset
            offset += row.size
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEachIndexed { i, photo ->
                    val path = photo.localPath ?: files[photo.mediaFileId]
                    if (path == null) LaunchedEffect(photo.mediaFileId) { onNeedFile(photo.mediaFileId) }
                    val single = photos.size == 1
                    val ratio = if (single) (photo.mediaWidth.toFloat() / photo.mediaHeight.coerceAtLeast(1)).coerceIn(0.8f, 1.8f) else 1f
                    val top = rowIndex == 0
                    val bottom = rowIndex == rows.lastIndex
                    val left = i == 0
                    val right = i == row.lastIndex
                    val big = 16.dp
                    val small = 4.dp
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(path?.let(::File) ?: photo.mediaThumb).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(if (single) ratio else if (row.size == 1) 1.6f else 1f)
                            .clip(
                                RoundedCornerShape(
                                    topStart = if (top && left) big else small,
                                    topEnd = if (top && right) big else small,
                                    bottomStart = if (bottom && left) big else small,
                                    bottomEnd = if (bottom && right) big else small,
                                ),
                            )
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .clickable { onOpen(photos, start + i) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoViewer(photos: List<UiMessage>, start: Int, files: Map<Int, String>, onNeedFile: (Int) -> Unit, onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val pager = androidx.compose.foundation.pager.rememberPagerState(initialPage = start) { photos.size }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            androidx.compose.foundation.pager.HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                val photo = photos[page]
                val path = photo.localPath ?: files[photo.mediaFileId]
                if (path == null) LaunchedEffect(photo.mediaFileId) { onNeedFile(photo.mediaFileId) }
                var scale by remember { mutableStateOf(1f) }
                var pan by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                AsyncImage(
                    model = path?.let(::File) ?: photo.mediaThumb,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(page) {
                            detectTransformGestures { _, move, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                pan = if (scale > 1f) pan + move else androidx.compose.ui.geometry.Offset.Zero
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = pan.x
                            translationY = pan.y
                        },
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(8.dp)) {
                Icon(Icons.Rounded.Close, stringResource(R.string.action_close), tint = Color.White)
            }
            if (photos.size > 1) {
                Text(
                    "${pager.currentPage + 1} / ${photos.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.statusBars).padding(top = 20.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Composer(replyTo: UiMessage?, editing: UiMessage?, controller: ChatController) {
    var text by rememberSaveable { mutableStateOf("") }
    var attach by remember { mutableStateOf(false) }
    var emoji by remember { mutableStateOf(false) }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    BackHandler(enabled = emoji) { emoji = false }
    if (attach) AttachSheet(controller, onPhotos = {
        attach = false
    }) { attach = false }
    LaunchedEffect(editing?.id) { if (editing != null) text = editing.text }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(10),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val caption = text
        text = ""
        scope.launch {
            val paths = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                uris.mapIndexedNotNull { index, uri ->
                    runCatching {
                        val target = java.io.File(context.cacheDir, "send-${System.currentTimeMillis()}-$index.jpg")
                        context.contentResolver.openInputStream(uri)!!.use { input -> target.outputStream().use { input.copyTo(it) } }
                        target.absolutePath
                    }.getOrNull()
                }
            }
            controller.sendPhotos(paths, caption)
        }
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars).imePadding()) {
            val target = editing ?: replyTo
            AnimatedVisibility(target != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { Icon(if (editing != null) Icons.Rounded.Edit else Icons.AutoMirrored.Rounded.Reply, null, tint = MaterialTheme.colorScheme.primary) },
                    overlineContent = { Text(stringResource(if (editing != null) R.string.chat_editing else R.string.chat_replying)) },
                    headlineContent = { Text(target?.text.orEmpty().ifBlank { stringResource(R.string.preview_photo) }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    trailingContent = {
                        IconButton(onClick = {
                            if (editing != null) {
                                controller.startEdit(null)
                                text = ""
                            } else {
                                controller.reply(null)
                            }
                        }) { Icon(Icons.Rounded.Close, stringResource(R.string.action_cancel)) }
                    },
                )
            }
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        IconButton(onClick = { attach = true }, modifier = Modifier.padding(vertical = 4.dp)) {
                            Icon(Icons.Rounded.AttachFile, stringResource(R.string.chat_attach), Modifier.graphicsLayer { rotationZ = 45f })
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = text,
                            onValueChange = { text = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                            maxLines = 6,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                            ),
                            modifier = Modifier.weight(1f).padding(vertical = 16.dp).focusRequester(focus),
                            decorationBox = { inner ->
                                Box {
                                    if (text.isEmpty()) Text(stringResource(R.string.chat_message_hint), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    inner()
                                }
                            },
                        )
                        IconButton(
                            onClick = {
                                if (emoji) {
                                    emoji = false
                                    focus.requestFocus()
                                    keyboard?.show()
                                } else {
                                    keyboard?.hide()
                                    emoji = true
                                }
                            },
                            modifier = Modifier.padding(vertical = 4.dp),
                        ) {
                            Icon(if (emoji) Icons.Rounded.Keyboard else Icons.Rounded.EmojiEmotions, stringResource(R.string.chat_emoji))
                        }
                        androidx.compose.animation.AnimatedVisibility(text.isEmpty(), enter = fadeIn() + androidx.compose.animation.expandHorizontally(), exit = fadeOut() + androidx.compose.animation.shrinkHorizontally()) {
                            IconButton(
                                onClick = { picker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                modifier = Modifier.padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
                            ) { Icon(Icons.Rounded.Image, stringResource(R.string.chat_attach_photo)) }
                        }
                    }
                }
                val ready = text.isNotBlank()
                val corner by androidx.compose.animation.core.animateDpAsState(if (ready) 18.dp else 28.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "send")
                FilledIconButton(
                    onClick = {
                        if (editing != null) controller.submitEdit(text) else controller.send(text)
                        text = ""
                    },
                    enabled = ready,
                    shape = RoundedCornerShape(corner),
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(if (editing != null) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.Send, stringResource(R.string.action_send))
                }
            }
            AnimatedVisibility(emoji, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                val dark = com.pixelgram.android.ui.theme.LocalDarkTheme.current
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { context ->
                        val themed = android.view.ContextThemeWrapper(context, if (dark) com.google.android.material.R.style.Theme_Material3_Dark_NoActionBar else com.google.android.material.R.style.Theme_Material3_Light_NoActionBar)
                        androidx.emoji2.emojipicker.EmojiPickerView(themed).apply {
                            emojiGridColumns = 8
                            setOnEmojiPickedListener { item -> text += item.emoji }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(EmojiPanelHeight),
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MessageActions(message: UiMessage, controller: ChatController, onDismiss: () -> Unit, onForward: () -> Unit, onDelete: () -> Unit) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val canEdit by androidx.compose.runtime.produceState(false, message.id) { value = controller.canEdit(message) }
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        val close: (() -> Unit) -> Unit = { action ->
            action()
            onDismiss()
        }
        ActionItem(Icons.AutoMirrored.Rounded.Reply, stringResource(R.string.action_reply)) { close { controller.reply(message) } }
        if (message.text.isNotBlank()) {
            ActionItem(Icons.Rounded.ContentCopy, stringResource(R.string.action_copy)) {
                close { clipboard.setText(androidx.compose.ui.text.AnnotatedString(message.text)) }
            }
        }
        if (canEdit) ActionItem(Icons.Rounded.Edit, stringResource(R.string.action_edit)) { close { controller.startEdit(message) } }
        ActionItem(Icons.AutoMirrored.Rounded.Forward, stringResource(R.string.action_forward)) { close(onForward) }
        ActionItem(Icons.Rounded.PushPin, stringResource(R.string.action_pin)) { close { controller.pin(message) } }
        ActionItem(Icons.Rounded.Delete, stringResource(R.string.action_delete), MaterialTheme.colorScheme.error) { close(onDelete) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ActionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent, headlineColor = tint, leadingIconColor = tint),
        leadingContent = { Icon(icon, null) },
        headlineContent = { Text(label) },
    )
}

@Composable
private fun DeleteDialog(message: UiMessage, controller: ChatController, privateUserId: Long, snapshot: StoreSnapshot, onDone: () -> Unit) {
    val canEveryone by androidx.compose.runtime.produceState(false, message.id) { value = controller.canDeleteForEveryone(message) }
    var everyone by remember { mutableStateOf(true) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDone,
        icon = { Icon(Icons.Rounded.Delete, null) },
        title = { Text(stringResource(R.string.delete_title)) },
        text = {
            if (canEveryone) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { everyone = !everyone },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.Checkbox(checked = everyone, onCheckedChange = { everyone = it })
                    val name = snapshot.users[privateUserId]?.firstName
                    Text(if (name != null) stringResource(R.string.delete_for_user, name) else stringResource(R.string.delete_for_everyone))
                }
            } else {
                Text(stringResource(R.string.delete_text))
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                controller.delete(message, canEveryone && everyone)
                onDone()
            }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDone) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private val BigEmojiSize = 64.dp

private fun bigCustomEmoji(text: String, entities: List<TdApi.TextEntity>): List<Pair<Long, String>>? {
    val emoji = entities.filter { it.type is TdApi.TextEntityTypeCustomEmoji }.sortedBy { it.offset }
    if (emoji.isEmpty() || emoji.size > 3) return null
    val rest = StringBuilder(text)
    emoji.asReversed().forEach { e ->
        val start = e.offset.coerceIn(0, rest.length)
        rest.delete(start, (e.offset + e.length).coerceIn(start, rest.length))
    }
    if (rest.isNotBlank()) return null
    return emoji.map { e -> (e.type as TdApi.TextEntityTypeCustomEmoji).customEmojiId to text.substring(e.offset.coerceIn(0, text.length), (e.offset + e.length).coerceIn(0, text.length)) }
}

private fun formatted(text: String, entities: List<TdApi.TextEntity>, link: Color, content: Color): androidx.compose.ui.text.AnnotatedString =
    androidx.compose.ui.text.buildAnnotatedString {
        append(text)
        for (entity in entities) {
            val start = entity.offset.coerceIn(0, text.length)
            val end = (entity.offset + entity.length).coerceIn(start, text.length)
            if (start == end) continue
            when (val type = entity.type) {
                is TdApi.TextEntityTypeBold -> addStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), start, end)
                is TdApi.TextEntityTypeItalic -> addStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), start, end)
                is TdApi.TextEntityTypeUnderline -> addStyle(androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), start, end)
                is TdApi.TextEntityTypeStrikethrough -> addStyle(androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough), start, end)
                is TdApi.TextEntityTypeCode, is TdApi.TextEntityTypePre, is TdApi.TextEntityTypePreCode ->
                    addStyle(androidx.compose.ui.text.SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, background = content.copy(alpha = 0.1f)), start, end)
                is TdApi.TextEntityTypeSpoiler -> addStyle(androidx.compose.ui.text.SpanStyle(color = Color.Transparent, background = content.copy(alpha = 0.35f)), start, end)
                is TdApi.TextEntityTypeUrl -> addLink(androidx.compose.ui.text.LinkAnnotation.Url(text.substring(start, end).let { if (it.contains("://")) it else "https://$it" }, androidx.compose.ui.text.TextLinkStyles(androidx.compose.ui.text.SpanStyle(color = link, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))), start, end)
                is TdApi.TextEntityTypeTextUrl -> addLink(androidx.compose.ui.text.LinkAnnotation.Url(type.url, androidx.compose.ui.text.TextLinkStyles(androidx.compose.ui.text.SpanStyle(color = link, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))), start, end)
                is TdApi.TextEntityTypeCustomEmoji -> addStringAnnotation(com.pixelgram.android.ui.components.CustomEmojiTag, com.pixelgram.android.ui.components.customEmojiKey(type.customEmojiId), start, end)
                is TdApi.TextEntityTypeMention, is TdApi.TextEntityTypeMentionName, is TdApi.TextEntityTypeHashtag, is TdApi.TextEntityTypeEmailAddress, is TdApi.TextEntityTypePhoneNumber, is TdApi.TextEntityTypeBotCommand ->
                    addStyle(androidx.compose.ui.text.SpanStyle(color = link), start, end)
                else -> Unit
            }
        }
    }

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AttachSheet(controller: ChatController, onPhotos: () -> Unit, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var poll by remember { mutableStateOf(false) }
    var contact by remember { mutableStateOf(false) }
    fun copy(uris: List<android.net.Uri>, send: (List<String>) -> Unit) {
        scope.launch {
            val paths = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                uris.mapIndexedNotNull { index, uri ->
                    runCatching {
                        val name = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
                            ?: "file-$index"
                        val dir = java.io.File(context.cacheDir, "send-${System.currentTimeMillis()}-$index").apply { mkdirs() }
                        val target = java.io.File(dir, name)
                        context.contentResolver.openInputStream(uri)!!.use { input -> target.outputStream().use { input.copyTo(it) } }
                        target.absolutePath
                    }.getOrNull()
                }
            }
            send(paths)
        }
    }
    val gallery = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) copy(uris) { controller.sendPhotos(it, "") }
        onDismiss()
    }
    val files = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) copy(uris) { controller.sendDocuments(it) }
        onDismiss()
    }
    if (poll) {
        PollDialog(onDismiss = { poll = false; onDismiss() }) { question, options, anonymous, multiple ->
            controller.sendPoll(question, options, anonymous, multiple)
            poll = false
            onDismiss()
        }
        return
    }
    if (contact) {
        com.pixelgram.android.ui.components.ContactPickerSheet(onDismiss = { contact = false; onDismiss() }) { user ->
            controller.sendContact(user)
            contact = false
            onDismiss()
        }
        return
    }
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        val items = listOf(
            Triple(Icons.Rounded.Image, R.string.attach_gallery) { gallery.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
            Triple(Icons.Rounded.Description, R.string.attach_file) { files.launch(arrayOf("*/*")) },
            Triple(Icons.Rounded.Poll, R.string.attach_poll) { poll = true },
            Triple(Icons.Rounded.Person, R.string.attach_contact) { contact = true },
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            items.forEachIndexed { index, (icon, label, action) ->
                val shape = when (index) {
                    0 -> androidx.compose.material3.MaterialShapes.Cookie9Sided
                    1 -> androidx.compose.material3.MaterialShapes.Clover4Leaf
                    2 -> androidx.compose.material3.MaterialShapes.Sunny
                    else -> androidx.compose.material3.MaterialShapes.Pentagon
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = action).padding(8.dp)) {
                    Box(
                        Modifier.size(60.dp).clip(shape.toShape()).background(
                            when (index % 3) {
                                0 -> MaterialTheme.colorScheme.primaryContainer
                                1 -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.tertiaryContainer
                            },
                        ),
                        contentAlignment = Alignment.Center,
                    ) { Icon(icon, null) }
                    Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun PollDialog(onDismiss: () -> Unit, onSend: (String, List<String>, Boolean, Boolean) -> Unit) {
    var question by remember { mutableStateOf("") }
    val options = remember { androidx.compose.runtime.mutableStateListOf("", "") }
    var anonymous by remember { mutableStateOf(true) }
    var multiple by remember { mutableStateOf(false) }
    val filled = options.map { it.trim() }.filter { it.isNotEmpty() }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.attach_poll)) },
        text = {
            Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedTextField(question, { question = it.take(255) }, label = { Text(stringResource(R.string.poll_question)) }, modifier = Modifier.fillMaxWidth())
                options.forEachIndexed { index, value ->
                    androidx.compose.material3.OutlinedTextField(
                        value,
                        {
                            options[index] = it.take(100)
                            if (index == options.lastIndex && it.isNotBlank() && options.size < 10) options.add("")
                        },
                        label = { Text(stringResource(R.string.poll_option, index + 1)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.poll_anonymous), Modifier.weight(1f))
                    com.pixelgram.android.ui.components.PixelSwitch(anonymous, { anonymous = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.poll_multiple), Modifier.weight(1f))
                    com.pixelgram.android.ui.components.PixelSwitch(multiple, { multiple = it })
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(enabled = question.isNotBlank() && filled.size >= 2, onClick = { onSend(question, filled, anonymous, multiple) }) { Text(stringResource(R.string.action_send)) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
