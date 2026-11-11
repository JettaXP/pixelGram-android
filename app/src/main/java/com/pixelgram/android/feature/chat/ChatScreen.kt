package com.pixelgram.android.feature.chat

import java.io.File
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.rounded.Forward
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.ui.draw.blur
import androidx.compose.material.icons.rounded.FormatClear
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FormatStrikethrough
import androidx.compose.material.icons.rounded.FormatUnderlined
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.TextFormat
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ExpandLess
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
    var menuAnchor by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var selecting by remember { mutableStateOf<UiMessage?>(null) }
    var forwarding by remember { mutableStateOf<UiMessage?>(null) }
    var deleting by remember { mutableStateOf<UiMessage?>(null) }
    val appearance by graph.appearance.state.collectAsState()
    val listState = rememberLazyListState()
    val jumpScope = rememberCoroutineScope()
    var highlighted by remember { mutableStateOf<Long?>(null) }
    val jumpTo: (Long) -> Unit = { id ->
        jumpScope.launch {
            fun position() = ui.messages.indexOfFirst { it.id == id || it.parts.any { p -> p.id == id } }
            if (position() < 0) controller.reveal(id)
            kotlinx.coroutines.delay(50)
            val index = controller.state.value.messages.indexOfFirst { it.id == id || it.parts.any { p -> p.id == id } }
            if (index >= 0) {
                val shift = if (appearance.typingBubble) 1 else 0
                listState.animateScrollToItem(index + shift, -listState.layoutInfo.viewportSize.height / 3)
                highlighted = controller.state.value.messages[index].id
                kotlinx.coroutines.delay(HighlightMs)
                highlighted = null
            }
        }
    }
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
                            ServiceMessage(message, senderName(snapshot, message.senderId, chat)) { id -> snapshot.users[id]?.name ?: "" }
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
                                    onReplyClick = { jumpTo(message.replyToId) },
                                    onOpenUser = { userId -> navigator.open(ProfileKey(userId)) },
                                    onVote = { options -> controller.vote(message, options) },
                                    highlighted = highlighted == message.id,
                                    reactor = { userId, size ->
                                        val user = snapshot.users[userId]
                                        Avatar(
                                            title = user?.name.orEmpty(),
                                            seed = userId,
                                            path = user?.localPhoto ?: user?.photoFileId?.let { snapshot.files[it] },
                                            minithumb = user?.minithumb,
                                            size = size,
                                            onNeedFile = { user?.photoFileId?.let(graph.store::requestFileAsync) },
                                        )
                                    },
                                    onLongPress = { rect ->
                                        menuAnchor = rect
                                        menuFor = message
                                    },
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
            val dates = remember(ui.messages) { ui.messages.associate { it.id to it.date } }
            val topDate by remember(dates) {
                derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull { it.key is Long }?.let { dates[it.key as Long] } }
            }
            var dateVisible by remember { mutableStateOf(false) }
            LaunchedEffect(listState.isScrollInProgress) {
                if (listState.isScrollInProgress) dateVisible = true
                else {
                    kotlinx.coroutines.delay(DatePillMs)
                    dateVisible = false
                }
            }
            AnimatedVisibility(
                visible = dateVisible && topDate != null,
                enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) + androidx.compose.animation.slideInVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { -it },
                exit = fadeOut(MaterialTheme.motionScheme.defaultEffectsSpec()) + androidx.compose.animation.slideOutVertically(MaterialTheme.motionScheme.fastSpatialSpec()) { -it },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
            ) {
                var shown by remember { mutableStateOf(0) }
                topDate?.let { shown = it }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest, shadowElevation = 3.dp) {
                    Text(dayHeader(shown), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp))
                }
            }
            val showDown by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }
            AnimatedVisibility(
                visible = showDown,
                enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) + androidx.compose.animation.scaleIn(MaterialTheme.motionScheme.fastSpatialSpec(), initialScale = 0.6f),
                exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) + androidx.compose.animation.scaleOut(MaterialTheme.motionScheme.fastSpatialSpec(), targetScale = 0.6f),
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                androidx.compose.material3.SmallFloatingActionButton(
                    onClick = { jumpScope.launch { listState.animateScrollToItem(0) } },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape,
                ) { Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.chat_scroll_down)) }
            }
        }
    }
    viewer?.let { (list, index) ->
        PhotoViewer(list, index, snapshot.files, graph.store::requestFileAsync) { viewer = null }
    }
    menuFor?.let { message ->
        MessageMenu(
            message = message,
            anchor = menuAnchor,
            controller = controller,
            quick = listOf(appearance.doubleTapEmoji) + QuickReactions.filter { it != appearance.doubleTapEmoji },
            onDismiss = { menuFor = null },
            onForward = { forwarding = message },
            onDelete = { deleting = message },
            onSelect = { selecting = message },
        )
    }
    selecting?.let { message -> SelectTextDialog(message) { selecting = null } }
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
private fun ServiceMessage(message: UiMessage, sender: String, name: (Long) -> String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val described = remember(message.content, sender) { serviceText(context, message.content, sender, name) }
    Box(Modifier.fillMaxWidth().padding(vertical = 2.dp), contentAlignment = Alignment.Center) {
        SuggestionChip(
            onClick = {},
            label = { Text(described ?: message.text.ifBlank { stringResource(R.string.preview_service) }, maxLines = 3, textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
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
    onLongPress: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onPlayAudio: (TdApi.Audio) -> Unit = {},
    onReact: (String) -> Unit = {},
    onReplyClick: () -> Unit = {},
    highlighted: Boolean = false,
    reactor: (@Composable (Long, androidx.compose.ui.unit.Dp) -> Unit)? = null,
    onOpenUser: (Long) -> Unit = {},
    onVote: (IntArray) -> Unit = {},
) {
    val prefs = com.pixelgram.android.ui.theme.LocalAppearance.current
    var bounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    val big = prefs.bubbleRadius.dp
    val small = prefs.bubbleTailRadius.dp
    val shape = if (message.outgoing) {
        RoundedCornerShape(big, if (joinsAbove) small else big, if (joinsBelow) small else big, big)
    } else {
        RoundedCornerShape(if (joinsAbove) small else big, big, big, if (joinsBelow) small else big)
    }
    val baseContainer = if (message.outgoing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val container by androidx.compose.animation.animateColorAsState(
        if (highlighted) androidx.compose.ui.graphics.lerp(baseContainer, MaterialTheme.colorScheme.tertiary, 0.35f) else baseContainer,
        MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "highlight",
    )
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
            modifier = Modifier
                .widthIn(max = 320.dp)
                .onGloballyPositioned { bounds = it.boundsInWindow() }
                .clip(shape)
                .combinedClickable(onClick = {}, onLongClick = { onLongPress(bounds) }, onDoubleClick = onDoubleTap),
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
                if (reply != null) ReplyQuote(reply, replySender.orEmpty(), files, onReplyClick)
                val photos = if (message.parts.isNotEmpty()) message.parts else listOf(message).filter { it.mediaFileId != 0 && it.content is TdApi.MessagePhoto }
                if (photos.isNotEmpty()) PhotoGrid(photos, files, onNeedFile, onOpenPhoto)
                (message.content as? TdApi.MessageAnimation)?.let { GifView(it.animation, files, onNeedFile) }
                if (isRichContent(message.content)) RichContent(message, files, onNeedFile, onContainer, onOpenUser, onVote)
                (message.content as? TdApi.MessageDocument)?.let { DocumentRow(it.document, onContainer) }
                (message.content as? TdApi.MessageAudio)?.let { AudioRow(it.audio, message.outgoing) { onPlayAudio(it.audio) } }
                val meta: @Composable () -> Unit = {
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
                        MessageText(
                            text,
                            entities,
                            MaterialTheme.colorScheme.primary,
                            onContainer,
                            MaterialTheme.typography.bodyLarge.copy(fontSize = prefs.messageFontSize.sp, lineHeight = (prefs.messageFontSize * 1.4f).sp),
                            prefs.messageFontSize.toFloat(),
                            Modifier.weight(1f, fill = false),
                        )
                    }
                    if (message.reactions.isEmpty()) meta()
                }
                if (message.reactions.isNotEmpty()) {
                    ReactionFlow(Modifier.padding(start = 6.dp, end = 8.dp, bottom = 4.dp)) {
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
                                    if (reactor != null && reaction.count <= ReactorAvatars && reaction.recent.size == reaction.count) {
                                        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                                            reaction.recent.forEach { id ->
                                                Box(
                                                    Modifier
                                                        .size(ReactorSize + 2.dp)
                                                        .clip(CircleShape)
                                                        .background(if (chosen) MaterialTheme.colorScheme.primary else container)
                                                        .padding(1.dp),
                                                ) { reactor(id, ReactorSize) }
                                            }
                                        }
                                    } else {
                                        Text(reaction.count.toString(), style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                        meta()
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
private fun ReplyQuote(reply: UiMessage, sender: String, files: Map<Int, String>, onClick: () -> Unit = {}) {
    val scheme = MaterialTheme.colorScheme
    val thumb = reply.parts.firstOrNull() ?: reply.takeIf { it.mediaFileId != 0 }
    Row(
        Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
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
                    val ratio = if (single && photo.mediaWidth > 0 && photo.mediaHeight > 0) (photo.mediaWidth.toFloat() / photo.mediaHeight).coerceIn(0.8f, 1.8f) else 1f
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GifView(animation: TdApi.Animation, files: Map<Int, String>, onNeedFile: (Int) -> Unit) {
    val file = animation.animation
    val path = files[file.id] ?: file.local.path.takeIf { file.local.isDownloadingCompleted && it.isNotEmpty() }
    if (path == null) LaunchedEffect(file.id) { onNeedFile(file.id) }
    val thumb = animation.thumbnail?.file?.let { t -> files[t.id] ?: t.local.path.takeIf { t.local.isDownloadingCompleted && it.isNotEmpty() } }
    animation.thumbnail?.file?.takeIf { thumb == null && path == null }?.let { t -> LaunchedEffect(t.id) { onNeedFile(t.id) } }
    val ratio = if (animation.width > 0 && animation.height > 0) (animation.width.toFloat() / animation.height).coerceIn(0.6f, 1.9f) else 1f
    val context = LocalContext.current
    Box(
        Modifier
            .padding(2.dp)
            .widthIn(max = 300.dp)
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = thumb?.let(::File) ?: animation.minithumbnail?.data,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        val video = path?.takeIf { animation.mimeType != "image/gif" || it.endsWith(".mp4") }
        if (video != null) {
            val player = remember(video) {
                androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                    setMediaItem(androidx.media3.common.MediaItem.fromUri(android.net.Uri.fromFile(File(video))))
                    repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
                    volume = 0f
                    prepare()
                    playWhenReady = true
                }
            }
            androidx.compose.runtime.DisposableEffect(player) { onDispose { player.release() } }
            androidx.media3.ui.compose.PlayerSurface(player, Modifier.matchParentSize())
        } else if (path != null) {
            AsyncImage(model = File(path), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        } else {
            LoadingIndicator(Modifier.size(36.dp))
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.45f),
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        ) { Text("GIF", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }
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
    var draft by remember { mutableStateOf(RichDraft()) }
    val text = draft.text
    var selectionMenu by remember { mutableStateOf<SelectionMenu?>(null) }
    var linking by remember { mutableStateOf(false) }
    val toolbar = remember { SelectionToolbar { selectionMenu = it } }
    var attach by remember { mutableStateOf(false) }
    var preparing by remember { mutableStateOf<List<String>?>(null) }
    var emoji by remember { mutableStateOf(false) }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    BackHandler(enabled = emoji) { emoji = false }
    if (attach) AttachSheet(controller, onPhotos = {
        attach = false
    }) { attach = false }
    LaunchedEffect(editing?.id) { if (editing != null) draft = RichDraft.of(editing.text, editing.entities) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(10),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
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
            if (paths.isNotEmpty()) preparing = paths
        }
    }
    preparing?.let { paths ->
        SendPhotosSheet(
            paths = paths,
            caption = text,
            onDismiss = { preparing = null },
            onSend = { list, caption ->
                preparing = null
                draft = RichDraft()
                controller.sendPhotos(list, caption)
            },
        )
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
                                draft = RichDraft()
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
                        val accent = MaterialTheme.colorScheme.primary
                        val onSurface = MaterialTheme.colorScheme.onSurface
                        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalTextToolbar provides toolbar) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = draft.value,
                            onValueChange = { draft = draft.edit(it) },
                            visualTransformation = remember(draft.entities, accent, onSurface) { DraftStyles(draft.entities, accent, onSurface) },
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
                        }
                        selectionMenu?.let { menu ->
                            FormatPopup(
                                menu = menu,
                                draft = draft,
                                onFormat = { format ->
                                    if (format == Format.Link) linking = true else draft = draft.toggle(format)
                                },
                                onClear = { draft = draft.clearFormatting() },
                                onDismiss = { selectionMenu = null },
                            )
                        }
                        if (linking) {
                            LinkDialog(onDismiss = { linking = false }) { url ->
                                linking = false
                                draft = draft.toggle(Format.Link, url)
                            }
                        }
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
                        val formatted = draft.formatted()
                        if (editing != null) controller.submitEdit(formatted) else controller.send(formatted)
                        draft = RichDraft()
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
                            setOnEmojiPickedListener { item -> draft = draft.insert(item.emoji) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(EmojiPanelHeight),
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FormatPopup(menu: SelectionMenu, draft: RichDraft, onFormat: (Format) -> Unit, onClear: () -> Unit, onDismiss: () -> Unit) {
    var formatting by remember(menu) { mutableStateOf(false) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val gap = with(density) { 8.dp.roundToPx() }
    val provider = remember(menu.rect) {
        object : androidx.compose.ui.window.PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: androidx.compose.ui.unit.IntRect,
                windowSize: androidx.compose.ui.unit.IntSize,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                popupContentSize: androidx.compose.ui.unit.IntSize,
            ): androidx.compose.ui.unit.IntOffset {
                val x = (menu.rect.center.x - popupContentSize.width / 2f).toInt().coerceIn(gap, (windowSize.width - popupContentSize.width - gap).coerceAtLeast(gap))
                val above = menu.rect.top.toInt() - popupContentSize.height - gap
                val y = if (above >= gap) above else menu.rect.bottom.toInt() + gap
                return androidx.compose.ui.unit.IntOffset(x, y)
            }
        }
    }
    androidx.compose.ui.window.Popup(popupPositionProvider = provider, onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shadowElevation = 6.dp,
            tonalElevation = 2.dp,
        ) {
            val fade = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
            androidx.compose.animation.AnimatedContent(
                formatting,
                transitionSpec = {
                    (fadeIn(fade) togetherWith fadeOut(fade))
                        .using(androidx.compose.animation.SizeTransform(clip = false))
                },
                label = "format-menu",
            ) { open ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                    if (!open) {
                        menu.cut?.let { cut -> MenuText(stringResource(R.string.action_cut)) { cut(); onDismiss() } }
                        menu.copy?.let { copy -> MenuText(stringResource(R.string.action_copy)) { copy(); onDismiss() } }
                        menu.paste?.let { paste -> MenuText(stringResource(R.string.action_paste)) { paste(); onDismiss() } }
                        if (!draft.value.selection.collapsed) {
                            IconButton(onClick = { formatting = true }) { Icon(Icons.Rounded.TextFormat, stringResource(R.string.format_title)) }
                        }
                        menu.selectAll?.let { all -> MenuText(stringResource(R.string.action_select_all)) { all() } }
                    } else {
                        IconButton(onClick = { formatting = false }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
                        val items = listOf(
                            Format.Bold to Icons.Rounded.FormatBold,
                            Format.Italic to Icons.Rounded.FormatItalic,
                            Format.Underline to Icons.Rounded.FormatUnderlined,
                            Format.Strike to Icons.Rounded.FormatStrikethrough,
                            Format.Mono to Icons.Rounded.Code,
                            Format.Spoiler to Icons.Rounded.BlurOn,
                            Format.Quote to Icons.Rounded.FormatQuote,
                            Format.Link to Icons.Rounded.Link,
                        )
                        items.forEach { (format, icon) ->
                            androidx.compose.material3.IconToggleButton(
                                checked = draft.has(format),
                                onCheckedChange = { onFormat(format) },
                                shapes = androidx.compose.material3.IconButtonDefaults.toggleableShapes(),
                                colors = androidx.compose.material3.IconButtonDefaults.iconToggleButtonColors(
                                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                                    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                modifier = Modifier.size(40.dp),
                            ) { Icon(icon, format.name) }
                        }
                        IconButton(onClick = onClear) { Icon(Icons.Rounded.FormatClear, stringResource(R.string.format_clear)) }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SendPhotosSheet(paths: List<String>, caption: String, onDismiss: () -> Unit, onSend: (List<String>, String) -> Unit) {
    val items = remember(paths) { androidx.compose.runtime.mutableStateListOf<String>().apply { addAll(paths) } }
    var text by remember { mutableStateOf(caption) }
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                pluralStringResource(R.plurals.send_photos_title, items.size, items.size),
                style = MaterialTheme.typography.titleLargeEmphasized,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items.size, key = { items[it] }) { index ->
                    val path = items[index]
                    Box(Modifier.size(if (items.size == 1) 240.dp else 140.dp).animateItem()) {
                        AsyncImage(
                            model = File(path),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize().clip(RoundedCornerShape(18.dp)),
                        )
                        if (items.size > 1) {
                            Surface(
                                onClick = { items.remove(path) },
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.55f),
                                contentColor = Color.White,
                                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(28.dp),
                            ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, stringResource(R.string.action_remove), Modifier.size(18.dp)) } }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.send_caption_hint)) },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    modifier = Modifier.weight(1f),
                )
                FilledIconButton(
                    onClick = { onSend(items.toList(), text) },
                    enabled = items.isNotEmpty(),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.size(56.dp),
                ) { Icon(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.action_send)) }
            }
        }
    }
}

@Composable
private fun MenuText(label: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun LinkDialog(onDismiss: () -> Unit, onDone: (String) -> Unit) {
    var url by remember { mutableStateOf("https://") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Link, null) },
        title = { Text(stringResource(R.string.format_link)) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri),
            )
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onDone(url.trim()) }, enabled = url.trim().length > 8) { Text(stringResource(R.string.action_done)) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private val QuickReactions = listOf("❤", "👍", "👎", "🔥", "🥰", "👏", "😁", "🤔", "😢")

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MessageMenu(
    message: UiMessage,
    anchor: androidx.compose.ui.geometry.Rect,
    controller: ChatController,
    quick: List<String>,
    onDismiss: () -> Unit,
    onForward: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val canEdit by androidx.compose.runtime.produceState(false, message.id) { value = controller.canEdit(message) }
    val link by androidx.compose.runtime.produceState<String?>(null, message.id) { value = controller.link(message) }
    val shown = remember { androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } }
    var closing by remember { mutableStateOf<(() -> Unit)?>(null) }
    val close: (() -> Unit) -> Unit = { action ->
        closing = action
        shown.targetState = false
    }
    LaunchedEffect(shown.currentState, shown.targetState) {
        if (!shown.targetState && !shown.currentState) {
            closing?.invoke()
            onDismiss()
        }
    }
    val copied = stringResource(R.string.profile_copied)
    val density = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.ui.window.Popup(
        popupPositionProvider = remember {
            object : androidx.compose.ui.window.PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: androidx.compose.ui.unit.IntRect,
                    windowSize: androidx.compose.ui.unit.IntSize,
                    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                    popupContentSize: androidx.compose.ui.unit.IntSize,
                ) = androidx.compose.ui.unit.IntOffset.Zero
            }
        },
        onDismissRequest = { close {} },
        properties = androidx.compose.ui.window.PopupProperties(focusable = true, clippingEnabled = false),
    ) {
        val scrim by androidx.compose.animation.core.animateFloatAsState(if (shown.targetState) 0.4f else 0f, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "scrim")
        androidx.compose.ui.layout.Layout(
            content = {
                Box(Modifier.background(Color.Black.copy(alpha = scrim)).clickable(indication = null, interactionSource = null) { close {} })
                androidx.compose.animation.AnimatedVisibility(
                    shown,
                    enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) + androidx.compose.animation.scaleIn(MaterialTheme.motionScheme.fastSpatialSpec(), initialScale = 0.6f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (message.outgoing) 1f else 0f, 1f)),
                    exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) + androidx.compose.animation.scaleOut(MaterialTheme.motionScheme.fastSpatialSpec(), targetScale = 0.8f),
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 6.dp) {
                        androidx.compose.foundation.lazy.LazyRow(
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.widthIn(max = 320.dp),
                        ) {
                            items(quick.size) { i ->
                                val emoji = quick[i]
                                val chosen = message.reactions.any { it.emoji == emoji && it.chosen }
                                Box(
                                    Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (chosen) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .clickable { close { controller.react(message, emoji) } },
                                    contentAlignment = Alignment.Center,
                                ) { Text(emoji, fontSize = 24.sp) }
                            }
                        }
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    shown,
                    enter = fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()) + androidx.compose.animation.scaleIn(MaterialTheme.motionScheme.fastSpatialSpec(), initialScale = 0.7f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(if (message.outgoing) 1f else 0f, 0f)),
                    exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()) + androidx.compose.animation.scaleOut(MaterialTheme.motionScheme.fastSpatialSpec(), targetScale = 0.8f),
                ) {
                    val rows = buildList<Triple<androidx.compose.ui.graphics.vector.ImageVector, String, () -> Unit>> {
                        add(Triple(Icons.AutoMirrored.Rounded.Reply, context.getString(R.string.action_reply)) { close { controller.reply(message) } })
                        if (message.text.isNotBlank()) {
                            add(Triple(Icons.Rounded.ContentCopy, context.getString(R.string.action_copy)) { close { clipboard.setText(androidx.compose.ui.text.AnnotatedString(message.text)) } })
                            add(Triple(Icons.Rounded.SelectAll, context.getString(R.string.action_select_text)) { close(onSelect) })
                        }
                        link?.let { url ->
                            add(Triple(Icons.Rounded.Link, context.getString(R.string.action_copy_link)) {
                                close {
                                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(url))
                                    android.widget.Toast.makeText(context, copied, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                        if (canEdit) add(Triple(Icons.Rounded.Edit, context.getString(R.string.action_edit)) { close { controller.startEdit(message) } })
                        add(Triple(Icons.Rounded.PushPin, context.getString(R.string.action_pin)) { close { controller.pin(message) } })
                        add(Triple(Icons.AutoMirrored.Rounded.Forward, context.getString(R.string.action_forward)) { close(onForward) })
                    }
                    Column(Modifier.width(MenuWidth), verticalArrangement = Arrangement.spacedBy(MenuGap)) {
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 6.dp) {
                            Column(Modifier.padding(vertical = 6.dp)) {
                                rows.forEach { (icon, label, action) -> MenuEntry(icon, label, MaterialTheme.colorScheme.onSurface, action) }
                            }
                        }
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 6.dp) {
                            Column(Modifier.padding(vertical = 6.dp)) {
                                MenuEntry(Icons.Rounded.Delete, stringResource(R.string.action_delete), MaterialTheme.colorScheme.error) { close(onDelete) }
                            }
                        }
                    }
                }
            },
        ) { measurables, constraints ->
            val loose = constraints.copy(minWidth = 0, minHeight = 0)
            val full = measurables[0].measure(androidx.compose.ui.unit.Constraints.fixed(constraints.maxWidth, constraints.maxHeight))
            val bar = measurables[1].measure(loose)
            val menu = measurables[2].measure(loose)
            val margin = with(density) { 12.dp.roundToPx() }
            val gap = with(density) { 8.dp.roundToPx() }
            val top = with(density) { 40.dp.roundToPx() }
            val bottomLimit = constraints.maxHeight - margin
            fun x(width: Int): Int {
                val raw = if (message.outgoing) anchor.right.toInt() - width else anchor.left.toInt()
                return raw.coerceIn(margin, (constraints.maxWidth - width - margin).coerceAtLeast(margin))
            }
            val menuBelow = anchor.bottom.toInt() + gap
            val menuY: Int
            val barY: Int
            if (menuBelow + menu.height <= bottomLimit) {
                menuY = menuBelow
                barY = (anchor.top.toInt() - gap - bar.height).coerceAtLeast(top)
            } else {
                menuY = (minOf(anchor.top.toInt(), bottomLimit) - gap - menu.height).coerceAtLeast(top + bar.height + gap)
                barY = menuY - gap - bar.height
            }
            layout(constraints.maxWidth, constraints.maxHeight) {
                full.place(0, 0)
                bar.place(x(bar.width), barY)
                menu.place(x(menu.width), menuY)
            }
        }
    }
}

@Composable
private fun MenuEntry(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, color = tint, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SelectTextDialog(message: UiMessage, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_select_text)) },
        text = {
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(message.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.verticalScroll(rememberScrollState()))
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) } },
    )
}

private val MenuWidth = 220.dp
private val MenuGap = 8.dp

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
private val ReactorSize = 18.dp
private const val DatePillMs = 1_500L
private val SpoilerBlur = 7.dp
private const val ReactorAvatars = 4
private const val HighlightMs = 1_400L
private const val QuoteLines = 3

@Composable
private fun ReactionFlow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.ui.layout.Layout(content, modifier) { measurables, constraints ->
        val gap = 4.dp.roundToPx()
        val metaGap = 8.dp.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(loose) }
        val chips = placeables.dropLast(1)
        val meta = placeables.last()
        val maxWidth = constraints.maxWidth
        val positions = mutableListOf<androidx.compose.ui.unit.IntOffset>()
        val lines = mutableListOf<Pair<Int, Int>>()
        var x = 0
        var y = 0
        var lineHeight = 0
        var widest = 0
        chips.forEach { chip ->
            if (x > 0 && x + chip.width > maxWidth) {
                lines += x - gap to lineHeight
                widest = maxOf(widest, x - gap)
                y += lineHeight + gap
                x = 0
                lineHeight = 0
            }
            positions += androidx.compose.ui.unit.IntOffset(x, y)
            x += chip.width + gap
            lineHeight = maxOf(lineHeight, chip.height)
        }
        val lastWidth = (x - gap).coerceAtLeast(0)
        val inline = lastWidth + metaGap + meta.width <= maxWidth
        val width: Int
        val height: Int
        val metaPos: androidx.compose.ui.unit.IntOffset
        if (inline) {
            width = maxOf(widest, lastWidth + metaGap + meta.width).coerceAtLeast(constraints.minWidth)
            height = y + maxOf(lineHeight, meta.height)
            metaPos = androidx.compose.ui.unit.IntOffset(width - meta.width, y + maxOf(lineHeight, meta.height) - meta.height)
        } else {
            width = maxOf(widest, lastWidth, meta.width).coerceAtLeast(constraints.minWidth)
            height = y + lineHeight + gap + meta.height
            metaPos = androidx.compose.ui.unit.IntOffset(width - meta.width, y + lineHeight + gap)
        }
        layout(width, height) {
            chips.forEachIndexed { i, chip -> chip.place(positions[i].x, positions[i].y) }
            meta.place(metaPos)
        }
    }
}

private class TextPart(val start: Int, val end: Int, val quote: TdApi.TextEntityType?)

private fun splitQuotes(text: String, entities: List<TdApi.TextEntity>): List<TextPart> {
    val quotes = entities.filter { it.type is TdApi.TextEntityTypeBlockQuote || it.type is TdApi.TextEntityTypeExpandableBlockQuote }.sortedBy { it.offset }
    if (quotes.isEmpty()) return listOf(TextPart(0, text.length, null))
    val parts = mutableListOf<TextPart>()
    var at = 0
    for (q in quotes) {
        val start = q.offset.coerceIn(at, text.length)
        val end = (q.offset + q.length).coerceIn(start, text.length)
        if (start > at) parts += TextPart(at, start, null)
        if (end > start) parts += TextPart(start, end, q.type)
        at = end
    }
    if (at < text.length) parts += TextPart(at, text.length, null)
    return parts
}

private fun slice(entities: List<TdApi.TextEntity>, start: Int, end: Int): List<TdApi.TextEntity> = entities.mapNotNull { e ->
    if (e.type is TdApi.TextEntityTypeBlockQuote || e.type is TdApi.TextEntityTypeExpandableBlockQuote) return@mapNotNull null
    val s = maxOf(e.offset, start)
    val t = minOf(e.offset + e.length, end)
    if (t <= s) null else TdApi.TextEntity(s - start, t - s, e.type)
}

@Composable
private fun MessageText(
    text: String,
    entities: List<TdApi.TextEntity>,
    link: Color,
    content: Color,
    style: androidx.compose.ui.text.TextStyle,
    fontSize: Float,
    modifier: Modifier = Modifier,
) {
    val parts = remember(text, entities) { splitQuotes(text, entities) }
    if (parts.size == 1 && parts[0].quote == null) {
        PlainText(text, entities, link, content, style, fontSize, modifier)
        return
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        parts.forEach { part ->
            val raw = text.substring(part.start, part.end)
            val trimStart = raw.length - raw.trimStart('\n').length
            val piece = raw.trim('\n')
            if (piece.isEmpty()) return@forEach
            val local = slice(entities, part.start + trimStart, part.start + trimStart + piece.length)
            if (part.quote == null) {
                PlainText(piece, local, link, content, style, fontSize)
            } else {
                QuoteBlock(part.quote is TdApi.TextEntityTypeExpandableBlockQuote, content) { lines ->
                    PlainText(piece, local, link, content, style, fontSize, maxLines = lines)
                }
            }
        }
    }
}

@Composable
private fun PlainText(
    text: String,
    entities: List<TdApi.TextEntity>,
    link: Color,
    content: Color,
    style: androidx.compose.ui.text.TextStyle,
    fontSize: Float,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) {
    val inline = remember(text, entities, fontSize) { com.pixelgram.android.ui.components.customEmojiContent(entities, text, (fontSize * 1.25f).sp) }
    val hasSpoiler = remember(entities) { entities.any { it.type is TdApi.TextEntityTypeSpoiler } }
    if (!hasSpoiler) {
        Text(formatted(text, entities, link, content), style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis, inlineContent = inline, modifier = modifier)
        return
    }
    var revealed by remember(text) { mutableStateOf(false) }
    val blurSupported = android.os.Build.VERSION.SDK_INT >= 31
    val radius by androidx.compose.animation.core.animateDpAsState(if (revealed) 0.dp else SpoilerBlur, MaterialTheme.motionScheme.slowEffectsSpec(), label = "spoiler")
    Box(modifier.clickable(enabled = !revealed, indication = null, interactionSource = null) { revealed = true }) {
        when {
            revealed && radius == 0.dp -> Text(formatted(text, entities, link, content, SpoilerMode.Shown), style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis, inlineContent = inline)
            !blurSupported -> Text(formatted(text, entities, link, content, SpoilerMode.Block), style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis, inlineContent = inline)
            else -> {
                Text(formatted(text, entities, link, content, SpoilerMode.Hidden), style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis, inlineContent = inline)
                Text(
                    formatted(text, entities, link, content, SpoilerMode.Only),
                    style = style,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.blur(radius, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded),
                )
            }
        }
    }
}

@Composable
private fun QuoteBlock(expandable: Boolean, content: Color, body: @Composable (Int) -> Unit) {
    var expanded by remember { mutableStateOf(!expandable) }
    val accent = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.12f))
            .then(if (expandable) Modifier.clickable { expanded = !expanded } else Modifier)
            .drawBehind { drawRect(accent, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)) }
            .padding(start = 11.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
            .animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.weight(1f, fill = false)) { body(if (expanded) Int.MAX_VALUE else QuoteLines) }
            Icon(
                if (expandable) (if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore) else Icons.Rounded.FormatQuote,
                null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

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

private enum class SpoilerMode { Block, Hidden, Only, Shown }

private fun formatted(text: String, entities: List<TdApi.TextEntity>, link: Color, content: Color, spoiler: SpoilerMode = SpoilerMode.Block): androidx.compose.ui.text.AnnotatedString =
    androidx.compose.ui.text.buildAnnotatedString {
        append(text)
        if (spoiler == SpoilerMode.Only) addStyle(androidx.compose.ui.text.SpanStyle(color = Color.Transparent), 0, text.length)
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
                is TdApi.TextEntityTypeSpoiler -> when (spoiler) {
                    SpoilerMode.Block -> addStyle(androidx.compose.ui.text.SpanStyle(color = Color.Transparent, background = content.copy(alpha = 0.35f)), start, end)
                    SpoilerMode.Hidden -> addStyle(androidx.compose.ui.text.SpanStyle(color = Color.Transparent), start, end)
                    SpoilerMode.Only -> addStyle(androidx.compose.ui.text.SpanStyle(color = content), start, end)
                    SpoilerMode.Shown -> addStyle(androidx.compose.ui.text.SpanStyle(background = content.copy(alpha = 0.08f)), start, end)
                }
                is TdApi.TextEntityTypeUrl -> addLink(androidx.compose.ui.text.LinkAnnotation.Url(text.substring(start, end).let { if (it.contains("://")) it else "https://$it" }, androidx.compose.ui.text.TextLinkStyles(androidx.compose.ui.text.SpanStyle(color = link, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))), start, end)
                is TdApi.TextEntityTypeTextUrl -> addLink(androidx.compose.ui.text.LinkAnnotation.Url(type.url, androidx.compose.ui.text.TextLinkStyles(androidx.compose.ui.text.SpanStyle(color = link, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))), start, end)
                is TdApi.TextEntityTypeCustomEmoji -> addStringAnnotation(com.pixelgram.android.ui.components.CustomEmojiTag, com.pixelgram.android.ui.components.customEmojiKey(type.customEmojiId), start, end)
                is TdApi.TextEntityTypeMention, is TdApi.TextEntityTypeMentionName, is TdApi.TextEntityTypeHashtag, is TdApi.TextEntityTypeEmailAddress, is TdApi.TextEntityTypePhoneNumber, is TdApi.TextEntityTypeBotCommand ->
                    addStyle(androidx.compose.ui.text.SpanStyle(color = link), start, end)
                else -> Unit
            }
        }
        com.pixelgram.android.feature.pixel.SupportProject.LinkPattern.findAll(text).forEach { match ->
            addLink(
                androidx.compose.ui.text.LinkAnnotation.Clickable(
                    "pg-support",
                    androidx.compose.ui.text.TextLinkStyles(androidx.compose.ui.text.SpanStyle(color = link, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)),
                ) { com.pixelgram.android.feature.pixel.SupportProject.open = true },
                match.range.first,
                match.range.last + 1,
            )
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
