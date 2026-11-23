package com.pixelgram.android.feature.chat

import androidx.compose.material.icons.rounded.Lock
import com.pixelgram.android.util.chatListTime
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Web
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.runtime.key
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.layout.isImeVisible
import com.pixelgram.android.LocalGraph
import com.pixelgram.android.nav.ChatKey
import androidx.compose.ui.layout.layoutId
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.indication
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import java.io.File
import androidx.compose.foundation.border
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Visibility
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
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.GifBox
import androidx.compose.material.icons.rounded.Interests
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CameraAlt
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
import com.pixelgram.android.nav.BackHandler
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
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
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
    var selected by remember(chatId) { mutableStateOf<List<UiMessage>>(emptyList()) }
    val toggleSelected: (UiMessage) -> Unit = { m -> selected = if (selected.any { it.id == m.id }) selected.filterNot { it.id == m.id } else selected + m }
    var dragAnchor by remember { mutableStateOf<Long?>(null) }
    val chatHaptic = LocalHapticFeedback.current
    var dragBase by remember { mutableStateOf<List<UiMessage>>(emptyList()) }
    var dragY by remember { mutableStateOf<Float?>(null) }
    var listTop by remember { mutableStateOf(0f) }
    var listBottom by remember { mutableStateOf(0f) }
    com.pixelgram.android.nav.BackHandler(enabled = selected.isNotEmpty()) { selected = emptyList() }
    var selecting by remember { mutableStateOf<UiMessage?>(null) }
    var forwarding by remember { mutableStateOf<UiMessage?>(null) }
    var deleting by remember { mutableStateOf<List<UiMessage>?>(null) }
    var pinning by remember { mutableStateOf<UiMessage?>(null) }
    var forwardingAll by remember { mutableStateOf<List<UiMessage>?>(null) }
    var shooting by remember { mutableStateOf<List<UiMessage>?>(null) }
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
    LaunchedEffect(com.pixelgram.android.nav.PendingNav.jump) {
        val (target, message) = com.pixelgram.android.nav.PendingNav.jump ?: return@LaunchedEffect
        if (target != chatId) return@LaunchedEffect
        com.pixelgram.android.nav.PendingNav.jump = null
        kotlinx.coroutines.delay(300)
        jumpTo(message)
    }
    var datePicker by remember { mutableStateOf<Int?>(null) }
    datePicker?.let {
        MediaCalendar(chatId, onDismiss = { datePicker = null }) { day ->
            datePicker = null
            jumpScope.launch {
                val zone = ZoneId.systemDefault()
                val start = day.atStartOfDay(zone).toEpochSecond().toInt()
                val found = runCatching { graph.td.send(TdApi.GetChatMessageByDate(chatId, start + 86_399)) }.getOrNull() ?: return@launch
                jumpTo(found.id)
                kotlinx.coroutines.delay(400)
                val list = controller.state.value.messages
                val index = list.indexOfLast { it.date >= start && it.date < start + 86_400 }
                if (index >= 0) {
                    val shift = if (appearance.typingBubble) 1 else 0
                    listState.animateScrollToItem(index + shift, -listState.layoutInfo.viewportSize.height / 2)
                }
            }
        }
    }
    val nearTop by remember { derivedStateOf { (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= ui.messages.size - 10 } }
    LaunchedEffect(nearTop, ui.loading) { if (nearTop && !ui.loading && ui.messages.isNotEmpty()) controller.loadMore() }
    val newest = ui.messages.firstOrNull()?.id
    var seenNewest by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(newest) {
        if (newest == null) return@LaunchedEffect
        val first = seenNewest == null
        seenNewest = newest
        val own = ui.messages.firstOrNull()?.outgoing == true
        if (!first && (own || listState.firstVisibleItemIndex <= 2)) {
            if (own && listState.firstVisibleItemIndex > 20) listState.scrollToItem(0) else listState.animateScrollToItem(0)
        }
    }

    val linkOpener = com.pixelgram.android.nav.rememberLinkOpener(graph, navigator)
    val systemUri = androidx.compose.ui.platform.LocalUriHandler.current
    val chatUri = remember(linkOpener) {
        object : androidx.compose.ui.platform.UriHandler {
            override fun openUri(uri: String) {
                when {
                    uri.startsWith("tg://user?id=") -> uri.removePrefix("tg://user?id=").toLongOrNull()?.let { navigator.open(com.pixelgram.android.nav.ProfileKey(it)) }
                    uri.startsWith("mailto:") || uri.startsWith("tel:") -> runCatching { systemUri.openUri(uri) }
                    else -> linkOpener.url(uri)
                }
            }
        }
    }
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalUriHandler provides chatUri) {
    Scaffold(
        topBar = {
            if (selected.isNotEmpty()) {
                SelectionTopBar(
                    count = selected.size,
                    canEdit = selected.size == 1,
                    controller = controller,
                    first = selected.first(),
                    onClose = { selected = emptyList() },
                    onEdit = {
                        controller.startEdit(selected.first())
                        selected = emptyList()
                    },
                    onForward = { forwardingAll = selected },
                    onDelete = { deleting = selected },
                    onShot = { shooting = selected },
                )
            } else TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { navigator.open(if (saved) com.pixelgram.android.nav.SavedKey else ProfileKey(privateUserId, chatId)) },
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
                            com.pixelgram.android.ui.components.NameBadges(privateUserId, snapshot.users[privateUserId]?.verified == true || row?.verified == true, 20.dp, row?.scam == true, row?.fake == true, official = graph.store.isOfficial(chatId))
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
                    IconButton(onClick = { navigator.open(if (saved) com.pixelgram.android.nav.SavedKey else ProfileKey(privateUserId, chatId)) }, modifier = Modifier.size(48.dp)) {
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
                            kind = when (val type = chat?.type) {
                                is TdApi.ChatTypePrivate, is TdApi.ChatTypeSecret -> if (snapshot.users[privateUserId]?.bot == true) com.pixelgram.android.td.ChatKind.Bot else com.pixelgram.android.td.ChatKind.Private
                                is TdApi.ChatTypeSupergroup -> if (type.isChannel) com.pixelgram.android.td.ChatKind.Channel else com.pixelgram.android.td.ChatKind.Group
                                is TdApi.ChatTypeBasicGroup -> com.pixelgram.android.td.ChatKind.Group
                                else -> null
                            },
                            official = graph.store.isOfficial(chatId),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (selected.isNotEmpty()) {
                SelectionBottomBar(
                    canReply = selected.size == 1,
                    onReply = {
                        controller.reply(selected.first())
                        selected = emptyList()
                    },
                    onForward = { forwardingAll = selected },
                )
            } else if (snapshot.users[privateUserId]?.deleted == true) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.chat_deleted_input),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars).padding(vertical = 18.dp),
                    )
                }
            } else if (ui.needsJoin) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                    var joining by remember { mutableStateOf(false) }
                    val failed = stringResource(R.string.chat_join_failed)
                    val context = LocalContext.current
                    androidx.compose.material3.Button(
                        onClick = {
                            joining = true
                            controller.join { error ->
                                joining = false
                                android.widget.Toast.makeText(context, error ?: failed, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !joining,
                        shapes = androidx.compose.material3.ButtonDefaults.shapes(),
                        modifier = Modifier.fillMaxWidth().windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars).padding(horizontal = 16.dp, vertical = 10.dp).height(52.dp),
                    ) { Text(stringResource(if (ui.channel) R.string.profile_subscribe else R.string.profile_join), style = MaterialTheme.typography.titleMedium) }
                }
            } else if (!ui.canSend && !ui.channel) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars).padding(vertical = 18.dp, horizontal = 16.dp),
                    ) {
                        Icon(Icons.Rounded.Lock, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (ui.restrictedUntil > 0) stringResource(R.string.restricted_until, java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(java.util.Date(ui.restrictedUntil * 1000L)))
                            else stringResource(R.string.restricted_forever),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            } else if (!ui.canSend) {
                ChannelBar(graph, chatId, ui.linkedChatId) { navigator.open(ChatKey(it)) }
            } else {
                Composer(ui.replyTo, ui.editing, controller) { navigator.open(ChatKey(it)) }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val edge = with(density) { 96.dp.toPx() }
            LaunchedEffect(dragY != null) {
                if (dragY == null) {
                    dragAnchor = null
                    return@LaunchedEffect
                }
                while (true) {
                    val y = dragY ?: break
                    val anchorId = dragAnchor ?: break
                    val speed = when {
                        y < listTop + edge -> (listTop + edge - y) / edge
                        y > listBottom - edge -> -(y - (listBottom - edge)) / edge
                        else -> 0f
                    }
                    if (speed != 0f) listState.scrollBy(speed.coerceIn(-1f, 1f) * 28f)
                    val local = (y - listTop).coerceIn(1f, (listBottom - listTop - 1f).coerceAtLeast(1f))
                    val hit = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                        val top = listState.layoutInfo.viewportEndOffset - item.offset - item.size
                        local >= top && local <= top + item.size
                    }
                    val messages = controller.state.value.messages
                    val from = messages.indexOfFirst { it.id == anchorId }
                    val to = (hit?.key as? Long)?.let { key -> messages.indexOfFirst { it.id == key } } ?: -1
                    if (from >= 0 && to >= 0) {
                        val range = messages.subList(minOf(from, to), maxOf(from, to) + 1).filterNot { it.service }
                        selected = (dragBase + range).distinctBy { it.id }
                    }
                    androidx.compose.runtime.withFrameNanos { }
                }
            }
            LazyColumn(
                state = listState,
                reverseLayout = true,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxSize().onGloballyPositioned {
                    val r = it.boundsInWindow()
                    listTop = r.top
                    listBottom = r.bottom
                },
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
                        if (older == null || !sameDay(older.date, message.date)) DayChip(message.date) { datePicker = message.date }
                        if (message.service) {
                            ServiceMessage(message, senderName(snapshot, message.senderId, chat)) { id -> snapshot.users[id]?.name ?: "" }
                        } else {
                            SwipeToReply(onReply = { controller.reply(message) }) {
                                val reply = message.replyToId.takeIf { it != 0L }?.let { id ->
                                    ui.messages.firstOrNull { it.id == id || it.parts.any { p -> p.id == id } } ?: ui.replies[id]
                                }
                                val guest = message.guestCallerId != 0L
                                val group = (privateUserId == 0L && !ui.channel && !saved) || guest
                                val sender = if (group && !message.outgoing) snapshot.users[message.senderId] else null
                                if (group && !message.outgoing && sender == null && message.senderId > 0) LaunchedEffect(message.senderId) { runCatching { graph.td.send(TdApi.GetUser(message.senderId)) } }
                                if (guest && message.guestCallerId > 0 && snapshot.users[message.guestCallerId] == null) LaunchedEffect(message.guestCallerId) { runCatching { graph.td.send(TdApi.GetUser(message.guestCallerId)) } }
                                val adminTags by androidx.compose.runtime.produceState(emptyMap<Long, MemberTag>(), chatId, group, MemberTagsCache.version) {
                                    if (group && privateUserId == 0L && !ui.channel) value = MemberTagsCache.admins(graph, chatId)
                                }
                                val tag = if (group && !message.outgoing && !guest) adminTags[message.senderId] ?: message.senderTag.takeIf { it.isNotBlank() }?.let { MemberTag(it, TagKind.Member) } else null
                                val guestCaller = if (guest) snapshot.users[message.guestCallerId]?.name ?: graph.store.chat(message.guestCallerId)?.title.orEmpty() else ""
                                MessageBubble(
                                    senderTag = tag,
                                    onSenderClick = if (message.senderId != 0L) ({ navigator.open(if (message.senderId > 0) ProfileKey(message.senderId) else ProfileKey(0, message.senderId)) }) else null,
                                    onTagClick = tag?.let { t ->
                                        {
                                            val mine = message.senderId == snapshot.meId
                                            val admin = t.kind != TagKind.Member
                                            TagSheets.current = TagRequest(chatId, message.senderId, sender?.name.orEmpty(), chat?.title.orEmpty(), t, mine && (admin || chat?.permissions?.canEditTag == true))
                                        }
                                    },
                                    extras = if (message.markup is TdApi.ReplyMarkupInlineKeyboard || (ui.channel && message.comments >= 0)) {
                                        {
                                            MessageBotExtras(
                                                message = message,
                                                chatId = chatId,
                                                channel = ui.channel,
                                                embedded = true,
                                                onOpenUrl = { url -> linkOpener.url(url) },
                                                onComments = {
                                                    controller.openComments(message) { chat, root ->
                                                        navigator.open(ChatKey(chat))
                                                        com.pixelgram.android.nav.PendingNav.jump = chat to root
                                                    }
                                                },
                                            )
                                        }
                                    } else null,
                                    message = message,
                                    senderName = (if (guest) sender?.name?.let { stringResource(R.string.guest_bot_for, it, guestCaller) } else sender?.name)?.takeIf { !joinsAbove || guest },
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
                                                    modifier = Modifier.clickable { navigator.open(if (message.senderId > 0) ProfileKey(message.senderId) else ProfileKey(0, message.senderId)) },
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
                                    replySender = reply?.let { snapshot.users[it.senderId]?.name ?: graph.store.chat(it.senderId)?.title.orEmpty() },
                                    onReplyClick = { jumpTo(message.replyToId) },
                                    onOpenUser = { userId -> navigator.open(ProfileKey(userId)) },
                                    onVote = { options -> controller.vote(message, options) },
                                    onPay = { com.pixelgram.android.feature.premium.PaymentRequest.current = TdApi.InputInvoiceMessage(chatId, message.id) },
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
                                    onTap = { rect ->
                                        if (selected.isNotEmpty()) {
                                            toggleSelected(message)
                                        } else {
                                            menuAnchor = rect
                                            menuFor = message
                                        }
                                    },
                                    onLongPress = {
                                        dragBase = selected
                                        dragAnchor = message.id
                                        toggleSelected(message)
                                    },
                                    onSelectDrag = { y -> dragY = y },
                                    selected = selected.any { it.id == message.id } && message.parts.size < 2,
                                    selecting = selected.isNotEmpty(),
                                    selectedParts = selected.flatMap { s -> if (s.parts.isNotEmpty()) s.parts.map { it.id } else listOf(s.id) }.toSet(),
                                    onTogglePart = { part ->
                                        selected = selected.filterNot { it.id == message.id && it.parts.isNotEmpty() }
                                        toggleSelected(part)
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
            var topBarsHeight by remember { mutableStateOf(0) }
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
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp + with(LocalDensity.current) { topBarsHeight.toDp() }),
            ) {
                var shown by remember { mutableStateOf(0) }
                topDate?.let { shown = it }
                Surface(onClick = { datePicker = shown }, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest, shadowElevation = 3.dp) {
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
            Column(Modifier.align(Alignment.TopCenter).onSizeChanged { topBarsHeight = it.height }) {
            chat?.businessBotManageBar?.let { bar -> BusinessBotBar(graph, chatId, bar) }
            if (ui.pins.isNotEmpty()) {
                var pinIndex by remember(ui.pins) { mutableStateOf(0) }
                val latestPins by androidx.compose.runtime.rememberUpdatedState(ui.pins)
                LaunchedEffect(Unit) {
                    androidx.compose.runtime.snapshotFlow {
                        val pins = latestPins
                        val visible = listState.layoutInfo.visibleItemsInfo.firstNotNullOfOrNull { it.key as? Long }
                        if (pins.isEmpty() || visible == null) null else pins.indexOfFirst { it.id <= visible }.let { if (it < 0) pins.lastIndex else it }
                    }.collect { found -> if (found != null) pinIndex = found }
                }
                val index = pinIndex.coerceIn(0, ui.pins.lastIndex)
                PinnedBar(ui.pins, index) {
                    jumpTo(ui.pins[index].id)
                    pinIndex = (index + 1) % ui.pins.size
                }
            }
            }
        }
    }
    viewer?.let { (list, index) ->
        PhotoViewer(list, index, snapshot.files, graph.store::requestFileAsync) { viewer = null }
    }
    }
    menuFor?.let { message ->
        MessageMenu(
            message = message,
            anchor = menuAnchor,
            controller = controller,
            quick = listOf(appearance.doubleTapEmoji) + QuickReactions.filter { it != appearance.doubleTapEmoji },
            onDismiss = { menuFor = null },
            onForward = { forwarding = message },
            onDelete = { deleting = listOf(message) },
            onSelect = { selecting = message },
            onShot = { shooting = listOf(message) },
            onMultiSelect = { toggleSelected(message) },
            pinned = ui.pins.any { it.id == message.id },
            onPin = { pinning = message },
            onQuickReact = { controller.react(message, appearance.doubleTapEmoji) },
        )
    }
    selecting?.let { message -> SelectTextDialog(message) { selecting = null } }
    pinning?.let { message ->
        PinDialog(graph, chatId, onDismiss = { pinning = null }) { notify, onlyForSelf ->
            pinning = null
            controller.pin(message, notify, onlyForSelf)
        }
    }
    forwarding?.let { message ->
        ForwardPicker(
            onPick = { target ->
                ForwardDrafts.map[target] = ForwardDraft(chatId, listOf(message))
                forwarding = null
                if (target != chatId) navigator.open(ChatKey(target))
            },
            onDismiss = { forwarding = null },
        )
    }
    shooting?.let { list ->
        MessageShotDialog(list, group = privateUserId == 0L, onSend = {
            shooting = null
            selected = emptyList()
        }) { shooting = null }
    }
    deleting?.let { list ->
        DeleteDialog(list, controller, privateUserId, snapshot) { done ->
            deleting = null
            if (done) selected = emptyList()
        }
    }
    forwardingAll?.let { list ->
        ForwardPicker(
            onPick = { target ->
                ForwardDrafts.map[target] = ForwardDraft(chatId, list.sortedBy { it.id })
                forwardingAll = null
                selected = emptyList()
                if (target != chatId) navigator.open(ChatKey(target))
            },
            onDismiss = { forwardingAll = null },
        )
    }
}

private const val PressDelayMs = 90L

@Composable
private fun BubbleWithExtras(extras: (@Composable () -> Unit)?, end: Boolean, bubble: @Composable () -> Unit) {
    if (extras == null) {
        bubble()
        return
    }
    androidx.compose.ui.layout.Layout(content = {
        Box { bubble() }
        Box { extras() }
    }) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val first = measurables[0].measure(loose)
        val minKeyboard = 200.dp.roundToPx().coerceAtMost(constraints.maxWidth)
        val width = maxOf(first.width, minKeyboard)
        val second = measurables[1].measure(androidx.compose.ui.unit.Constraints.fixedWidth(width))
        layout(width, first.height + second.height) {
            first.place(if (end) width - first.width else 0, 0)
            second.place(0, first.height)
        }
    }
}

private fun senderName(snapshot: StoreSnapshot, senderId: Long, chat: TdApi.Chat?): String =
    snapshot.users[senderId]?.name ?: chat?.title.orEmpty()

@Composable
private fun subtitle(snapshot: StoreSnapshot, userId: Long): String? {
    val user = snapshot.users[userId] ?: return null
    return com.pixelgram.android.ui.components.statusLine(user)
}

private fun sameDay(a: Int, b: Int): Boolean {
    val zone = ZoneId.systemDefault()
    return Instant.ofEpochSecond(a.toLong()).atZone(zone).toLocalDate() == Instant.ofEpochSecond(b.toLong()).atZone(zone).toLocalDate()
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun WavyLine(modifier: Modifier, color: Color, reverse: Boolean = false) {
    androidx.compose.material3.LinearWavyProgressIndicator(
        progress = { 1f },
        modifier = modifier.graphicsLayer { if (reverse) scaleX = -1f },
        color = color,
        trackColor = Color.Transparent,
        stopSize = 0.dp,
        gapSize = 0.dp,
        amplitude = { 1f },
        wavelength = 16.dp,
        waveSpeed = 0.dp,
    )
}

@Composable
private fun WavyRow(content: @Composable () -> Unit) {
    val line = MaterialTheme.colorScheme.outlineVariant
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        WavyLine(Modifier.weight(1f).padding(end = 8.dp), line)
        Box(Modifier.widthIn(max = 280.dp)) { content() }
        WavyLine(Modifier.weight(1f).padding(start = 8.dp), line, reverse = true)
    }
}

@Composable
private fun DayChip(date: Int, onClick: () -> Unit = {}) {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        WavyRow {
        SuggestionChip(
            onClick = onClick,
            label = { Text(dayHeader(date)) },
            shape = androidx.compose.foundation.shape.CircleShape,
            border = null,
            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        )
        }
    }
}

@Composable
private fun ServiceMessage(message: UiMessage, sender: String, name: (Long) -> String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val described = remember(message.content, sender) { serviceText(context, message.content, sender, name) }
    Box(Modifier.fillMaxWidth().padding(vertical = 2.dp), contentAlignment = Alignment.Center) {
        WavyRow {
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
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var total = androidx.compose.ui.geometry.Offset.Zero
                var active = false
                while (true) {
                    val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    val delta = change.positionChange()
                    if (!active) {
                        if (change.isConsumed) return@awaitEachGesture
                        total += delta
                        if (kotlin.math.abs(total.x) < viewConfiguration.touchSlop * 0.8f && kotlin.math.abs(total.y) < viewConfiguration.touchSlop * 1.6f) continue
                        if (total.x >= 0f || kotlin.math.abs(total.x) < kotlin.math.abs(total.y) * 0.6f) return@awaitEachGesture
                        active = true
                    }
                    val current = -offset.value
                    val resist = 1f - (current / limit).coerceIn(0f, 0.85f)
                    val next = (current - delta.x * resist).coerceIn(0f, limit)
                    change.consume()
                    scope.launch { offset.snapTo(-next) }
                    val reached = next >= trigger
                    if (reached && !armed) haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                    armed = reached
                }
                if (!active) return@awaitEachGesture
                if (armed) {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onReply()
                }
                armed = false
                scope.launch { offset.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 400f)) }
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
private val EmojiPanelHeight = 340.dp
private val ReplyLimit = 110.dp

@Composable
internal fun MessageBubble(
    selectedParts: Set<Long> = emptySet(),
    onTogglePart: ((UiMessage) -> Unit)? = null,
    senderTag: MemberTag? = null,
    onTagClick: (() -> Unit)? = null,
    onSenderClick: (() -> Unit)? = null,
    onBubbleWidth: (Int) -> Unit = {},
    extras: (@Composable () -> Unit)? = null,
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
    onSelectDrag: (Float?) -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onTap: (androidx.compose.ui.geometry.Rect) -> Unit = {},
    selected: Boolean = false,
    selecting: Boolean = false,
    onPlayAudio: (TdApi.Audio) -> Unit = {},
    onReact: (String) -> Unit = {},
    onReplyClick: () -> Unit = {},
    highlighted: Boolean = false,
    reactor: (@Composable (Long, androidx.compose.ui.unit.Dp) -> Unit)? = null,
    onOpenUser: (Long) -> Unit = {},
    onVote: (IntArray) -> Unit = {},
    onPay: () -> Unit = {},
) {
    val prefs = com.pixelgram.android.ui.theme.LocalAppearance.current
    var bounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    var rowBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    val haptic = LocalHapticFeedback.current
    val latestSelecting by androidx.compose.runtime.rememberUpdatedState(selecting)
    val latestTap by androidx.compose.runtime.rememberUpdatedState(onTap)
    val latestLongPress by androidx.compose.runtime.rememberUpdatedState(onLongPress)
    val latestDoubleTap by androidx.compose.runtime.rememberUpdatedState(onDoubleTap)
    val latestSelectDrag by androidx.compose.runtime.rememberUpdatedState(onSelectDrag)
    val big = prefs.bubbleRadius.dp
    val small = prefs.bubbleTailRadius.dp
    val shape = if (message.outgoing) {
        RoundedCornerShape(big, if (joinsAbove) small else big, if (joinsBelow) small else big, big)
    } else {
        RoundedCornerShape(if (joinsAbove) small else big, big, big, if (joinsBelow) small else big)
    }
    val bigEmoji = remember(message.text, message.entities, message.content, prefs.largeEmoji) { if (prefs.largeEmoji) bigEmoji(message) else null }
    val zalgo = prefs.zalgoFilter && remember(message.text) { com.pixelgram.android.util.stripZalgo(message.text) != message.text }
    val bare = !zalgo && (bigEmoji != null || message.content is TdApi.MessageSticker || message.content is TdApi.MessageAnimatedEmoji || message.content is TdApi.MessageVideoNote)
    val baseContainer = if (bare) Color.Transparent else if (message.outgoing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val container by androidx.compose.animation.animateColorAsState(
        if (highlighted) androidx.compose.ui.graphics.lerp(baseContainer, MaterialTheme.colorScheme.tertiary, 0.35f) else baseContainer,
        MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "highlight",
    )
    val onContainer = if (message.outgoing) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val selectionTint by androidx.compose.animation.animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
        MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "selection",
    )
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .background(selectionTint)
                .onGloballyPositioned { rowBounds = it.boundsInWindow() }
            .pointerInput(Unit) {
                    val doubleWindow = viewConfiguration.doubleTapTimeoutMillis + 60L
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (SpoilerTouch.downAt == down.uptimeMillis) return@awaitEachGesture
                        val inside = bounds.contains(androidx.compose.ui.geometry.Offset(rowBounds.left + down.position.x, rowBounds.top + down.position.y))
                        val press = androidx.compose.foundation.interaction.PressInteraction.Press(down.position)
                        val pointAt = { x: Float, y: Float -> androidx.compose.ui.geometry.Rect(rowBounds.left + x, rowBounds.top + y, rowBounds.left + x, rowBounds.top + y) }
                        var cancelled = false
                        var pressed = false
                        var up = withTimeoutOrNull(PressDelayMs) {
                            waitForUpOrCancellation().also { if (it == null) cancelled = true }
                        }
                        if (up == null && !cancelled) {
                            if (inside) {
                                interaction.tryEmit(press)
                                pressed = true
                            }
                            up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis - PressDelayMs) {
                                waitForUpOrCancellation().also { if (it == null) cancelled = true }
                            }
                        } else if (up != null && !up.isConsumed && inside) {
                            interaction.tryEmit(press)
                            pressed = true
                        }
                        if (up != null && up.isConsumed) {
                            if (pressed) interaction.tryEmit(androidx.compose.foundation.interaction.PressInteraction.Cancel(press))
                            return@awaitEachGesture
                        }
                        if (pressed) interaction.tryEmit(androidx.compose.foundation.interaction.PressInteraction.Release(press))
                        if (cancelled) return@awaitEachGesture
                        if (up == null) {
                            val current = currentEvent.changes.firstOrNull { it.id == down.id }
                            val moved = current != null && (current.position - down.position).getDistance() > viewConfiguration.touchSlop
                            if (current == null || !current.pressed || current.isConsumed || moved) return@awaitEachGesture
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            latestLongPress(pointAt(down.position.x, down.position.y))
                            current.consume()
                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                latestSelectDrag(rowBounds.top + change.position.y)
                                event.changes.forEach { it.consume() }
                            } while (change.pressed)
                            latestSelectDrag(null)
                            return@awaitEachGesture
                        }
                        up.consume()
                        if (latestSelecting || !inside) {
                            latestTap(pointAt(up.position.x, up.position.y))
                            return@awaitEachGesture
                        }
                        val second = withTimeoutOrNull(doubleWindow) { awaitFirstDown(requireUnconsumed = false) }
                        if (second != null) {
                            second.consume()
                            waitForUpOrCancellation()?.consume()
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            latestDoubleTap()
                            return@awaitEachGesture
                        }
                        latestTap(pointAt(up.position.x, up.position.y))
                    }
                }
                .padding(top = if (joinsAbove) 2.dp else 6.dp),
            horizontalArrangement = if (message.outgoing) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom,
        ) {
            androidx.compose.animation.AnimatedVisibility(
                selecting,
                enter = androidx.compose.animation.expandHorizontally() + fadeIn(),
                exit = androidx.compose.animation.shrinkHorizontally() + fadeOut(),
            ) {
                Box(Modifier.padding(start = 4.dp, end = 8.dp, bottom = 6.dp).size(24.dp), contentAlignment = Alignment.Center) {
                    if (selected) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Box(Modifier.size(24.dp).border(2.dp, MaterialTheme.colorScheme.outline, CircleShape))
                    }
                }
            }
            if (selecting && message.outgoing) Spacer(Modifier.weight(1f))
            if (senderAvatar != null) {
                Box(Modifier.size(34.dp)) { senderAvatar() }
                Spacer(Modifier.width(6.dp))
            }
            BubbleWithExtras(extras, message.outgoing) {
            Surface(
                shape = shape,
                color = container,
                contentColor = onContainer,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .onGloballyPositioned {
                        bounds = it.boundsInWindow()
                        onBubbleWidth(it.size.width)
                    }
                    .clip(shape)
                    .indication(interaction, androidx.compose.material3.ripple()),
            ) {
                WidestColumn(Modifier.padding(4.dp)) {
                    if (senderName != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp)) {
                        Text(
                            senderName,
                            style = MaterialTheme.typography.labelLarge,
                            color = com.pixelgram.android.LocalGraph.current.store.snapshot.collectAsState().value.users[message.senderId]?.let { com.pixelgram.android.td.AccentColors.colors(it.accentColorId, com.pixelgram.android.ui.theme.LocalDarkTheme.current).first() } ?: com.pixelgram.android.ui.components.avatarColor(message.senderId),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false).then(if (onSenderClick != null) Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onSenderClick) else Modifier),
                        )
                        if (senderTag != null) {
                            Spacer(Modifier.width(8.dp))
                            MemberTagLabel(senderTag, small = true, onClick = onTagClick)
                        }
                        }
                    }
                    if (message.viaBotId != 0L) {
                        val bot = com.pixelgram.android.LocalGraph.current.store.snapshot.collectAsState().value.users[message.viaBotId]?.username
                        if (bot != null) Text(
                            "via @$bot",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp).clickable { ComposerRequests.text = "@$bot " },
                        )
                    }
                    if (reply != null) Box(Modifier.layoutId(FillWidth)) { ReplyQuote(reply, replySender.orEmpty(), files, onReplyClick, Modifier.fillMaxWidth(), quote = message.replyQuote) }
                    val meta: @Composable (Color) -> Unit = { tint ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (message.views > 0) {
                                Icon(Icons.Rounded.Visibility, stringResource(R.string.message_views), Modifier.size(13.dp), tint = tint)
                                Text(compactCount(message.views, prefs.exactNumbers), style = MaterialTheme.typography.labelSmall, color = tint, modifier = Modifier.padding(end = 4.dp))
                            }
                            if (message.edited) Text(stringResource(R.string.message_edited), style = MaterialTheme.typography.labelSmall, color = tint)
                            Text(messageTime(message.date, prefs.messageSeconds), style = MaterialTheme.typography.labelSmall, color = tint)
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
                                    tint = if (message.failed) MaterialTheme.colorScheme.error else tint,
                                )
                            }
                        }
                    }
                    val photos = if (message.parts.isNotEmpty()) message.parts else listOf(message).filter { it.mediaFileId != 0 && it.content is TdApi.MessagePhoto }
                    val mediaOnly = photos.isNotEmpty() && message.text.isBlank() && message.reactions.isEmpty()
                    if (photos.isNotEmpty()) Box {
                        PhotoGrid(photos, files, onNeedFile, onOpenPhoto, if (selecting && photos.size > 1) onTogglePart else null, selectedParts)
                        if (mediaOnly) {
                            Surface(shape = RoundedCornerShape(10.dp), color = Color.Black.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                                Box(Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) { meta(Color.White) }
                            }
                        }
                    }
                    (message.content as? TdApi.MessageAnimation)?.let { GifView(it.animation, files, onNeedFile) }
                    val metaInside = message.content is TdApi.MessageVoiceNote && message.text.isBlank() && message.reactions.isEmpty()
                    if (isRichContent(message.content)) RichContent(message, files, onNeedFile, onContainer, onOpenUser, onVote, onPay, meta = if (metaInside) ({ meta(onContainer.copy(alpha = 0.7f)) }) else null)
                    (message.content as? TdApi.MessageDocument)?.let { DocumentRow(it.document, onContainer) }
                    (message.content as? TdApi.MessageAudio)?.let { AudioRow(it.audio, message.outgoing) { onPlayAudio(it.audio) } }
                    if (bigEmoji != null && !zalgo) {
                        val size = BigEmojiSizes.getOrElse(bigEmoji.size - 1) { BigEmojiSizes.last() }
                        androidx.compose.foundation.layout.FlowRow(
                            Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp, if (message.outgoing) Alignment.End else Alignment.Start),
                        ) {
                            bigEmoji.forEach { (id, alt) ->
                                if (id != null && bigEmoji.size == 1) EmojiTapEffect(alt, message.outgoing, size, files, onNeedFile) { tap -> com.pixelgram.android.ui.components.CustomEmojiView(id, alt, tap, size = size) }
                                else if (id != null) com.pixelgram.android.ui.components.CustomEmojiView(id, alt, Modifier.clickable(interactionSource = null, indication = null) { EmojiPreview.openPack(id) }, size = size)
                                else Box(Modifier.size(size), contentAlignment = Alignment.Center) {
                                    Text(alt, fontSize = with(androidx.compose.ui.platform.LocalDensity.current) { (size * 0.82f).toSp() }, lineHeight = with(androidx.compose.ui.platform.LocalDensity.current) { size.toSp() }, maxLines = 1)
                                }
                            }
                        }
                        if (message.reactions.isEmpty()) BareMeta(Modifier.layoutId(AlignEnd)) { meta(Color.White) }
                    } else if (message.text.isNotBlank()) {
                        val text = if (zalgo) com.pixelgram.android.util.stripZalgo(message.text) else message.text
                        val entities = if (zalgo) emptyList() else message.entities
                        val lastLine = remember(text) { androidx.compose.runtime.mutableFloatStateOf(-1f) }
                        TextWithMeta(lastLine.floatValue, showMeta = message.reactions.isEmpty(), modifier = Modifier.layoutId(StretchWidth).padding(start = 10.dp, end = 10.dp, top = 5.dp, bottom = if (message.reactions.isEmpty()) 5.dp else 2.dp)) {
                            MessageText(
                                text,
                                entities,
                                MaterialTheme.colorScheme.primary,
                                onContainer,
                                MaterialTheme.typography.bodyLarge.copy(fontSize = prefs.messageFontSize.sp, lineHeight = (prefs.messageFontSize * 1.3f).sp),
                                prefs.messageFontSize.toFloat(),
                                onLastLine = { lastLine.floatValue = it },
                            )
                            if (message.reactions.isEmpty()) meta(onContainer.copy(alpha = 0.7f))
                        }
                    } else if (bare && bigEmoji == null && message.reactions.isEmpty()) {
                        BareMeta(Modifier.layoutId(AlignEnd)) { meta(Color.White) }
                    } else if (message.reactions.isEmpty() && !mediaOnly && !metaInside) {
                        Box(Modifier.layoutId(AlignEnd).padding(horizontal = 8.dp, vertical = 4.dp)) { meta(onContainer.copy(alpha = 0.7f)) }
                    }
                    (message.content as? TdApi.MessageText)?.linkPreview?.let { preview -> LinkPreviewCard(preview, files, onNeedFile, onContainer) }
                    if (message.reactions.isNotEmpty()) {
                        ReactionFlow(Modifier.layoutId(StretchWidth).padding(start = 6.dp, end = 8.dp, bottom = 5.dp, top = 1.dp)) {
                            message.reactions.forEach { reaction -> androidx.compose.runtime.key(reaction.emoji) {
                                val chosen = reaction.chosen
                                val appear = remember { androidx.compose.animation.core.Animatable(if (message.reactions.size == 1 && chosen) 0f else 1f) }
                                val pop = remember { androidx.compose.animation.core.Animatable(1f) }
                                var wasChosen by remember { mutableStateOf(chosen) }
                                LaunchedEffect(Unit) { appear.animateTo(1f, androidx.compose.animation.core.spring(0.45f, 500f)) }
                                LaunchedEffect(chosen) {
                                    if (chosen && !wasChosen) {
                                        pop.snapTo(0.4f)
                                        pop.animateTo(1f, androidx.compose.animation.core.spring(0.35f, 450f))
                                    }
                                    wasChosen = chosen
                                }
                                val radius by androidx.compose.animation.core.animateDpAsState(if (chosen) 16.dp else 10.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "rr")
                                val chipColor by androidx.compose.animation.animateColorAsState(if (chosen) MaterialTheme.colorScheme.primary else onContainer.copy(alpha = 0.1f), MaterialTheme.motionScheme.fastEffectsSpec(), label = "rc")
                                val chipContent by androidx.compose.animation.animateColorAsState(if (chosen) MaterialTheme.colorScheme.onPrimary else onContainer, MaterialTheme.motionScheme.fastEffectsSpec(), label = "rt")
                                Surface(
                                    onClick = { onReact(reaction.emoji) },
                                    shape = RoundedCornerShape(radius),
                                    color = chipColor,
                                    contentColor = chipContent,
                                    modifier = Modifier.graphicsLayer { val v = appear.value; scaleX = v; scaleY = v; alpha = v.coerceIn(0f, 1f) },
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    ) {
                                        Box(Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }) { ReactionGlyph(reaction.emoji, 18.dp) }
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
                                            com.pixelgram.android.ui.components.AnimatedCounter(reaction.count, MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            } }
                            meta(onContainer.copy(alpha = 0.7f))
                        }
                    }
                }
            }
            }
        }
        if (selecting) Box(Modifier.matchParentSize().clickable(interactionSource = null, indication = null) { latestTap(rowBounds) })
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
            DocumentViewer.accepts(document.fileName) -> DocumentViewer.path = file
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
            val thumbFile = document.thumbnail?.file
            val thumbPath = thumbFile?.let { f -> snapshot.files[f.id] ?: f.local.path.takeIf { f.local.isDownloadingCompleted && it.isNotEmpty() } }
            if (thumbFile != null && thumbPath == null) LaunchedEffect(thumbFile.id) { graph.store.requestFileAsync(thumbFile.id) }
            val hasThumb = thumbFile != null || document.minithumbnail != null
            Box(Modifier.size(if (hasThumb) 56.dp else 46.dp).clip(if (hasThumb) RoundedCornerShape(12.dp) else CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                if (hasThumb) {
                    AsyncImage(model = thumbPath?.let(::File) ?: document.minithumbnail?.data, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                    Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.35f)))
                }
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

private data class PatternSpot(val x: Int, val y: Int, val size: Int, val alpha: Float)

private val ReplyPattern = listOf(
    PatternSpot(62, 4, 16, 0.38f),
    PatternSpot(40, 22, 13, 0.3f),
    PatternSpot(74, 26, 12, 0.26f),
    PatternSpot(18, 6, 11, 0.2f),
    PatternSpot(4, 26, 10, 0.14f),
)

@Composable
private fun ReplyQuote(reply: UiMessage, sender: String, files: Map<Int, String>, onClick: () -> Unit = {}, modifier: Modifier = Modifier, quote: String = "") {
    val graph = com.pixelgram.android.LocalGraph.current
    val snapshot by graph.store.snapshot.collectAsState()
    if (reply.senderId > 0 && snapshot.users[reply.senderId] == null) LaunchedEffect(reply.senderId) { runCatching { graph.td.send(TdApi.GetUser(reply.senderId)) } }
    val dark = com.pixelgram.android.ui.theme.LocalDarkTheme.current
    val user = snapshot.users[reply.senderId]
    val chat = if (user == null && reply.senderId != 0L) graph.store.chat(reply.senderId) else null
    val accentId = user?.accentColorId ?: chat?.accentColorId
    val pattern = user?.backgroundEmojiId ?: chat?.backgroundCustomEmojiId ?: 0L
    val palette = accentId?.let { com.pixelgram.android.td.AccentColors.colors(it, dark) } ?: listOf(MaterialTheme.colorScheme.primary)
    val main = palette.first()
    val thumb = reply.parts.firstOrNull() ?: reply.takeIf { it.mediaFileId != 0 }
    Box(
        modifier
            .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(main.copy(alpha = 0.14f))
            .drawBehind {
                val bar = 3.dp.toPx()
                if (palette.size == 1) {
                    drawRect(main, size = androidx.compose.ui.geometry.Size(bar, size.height))
                } else {
                    val step = 6.dp.toPx()
                    var y = 0f
                    var i = 0
                    while (y < size.height) {
                        drawRect(palette[i % palette.size], topLeft = androidx.compose.ui.geometry.Offset(0f, y), size = androidx.compose.ui.geometry.Size(bar, minOf(step, size.height - y)))
                        y += step
                        i++
                    }
                }
            }
            .height(IntrinsicSize.Min),
    ) {
        if (quote.isNotBlank()) Icon(androidx.compose.material.icons.Icons.Rounded.FormatQuote, null, tint = main, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp))
        if (pattern != 0L) {
            Box(Modifier.align(Alignment.TopEnd).layout { m, _ ->
                val p = m.measure(androidx.compose.ui.unit.Constraints.fixed(92.dp.roundToPx(), 44.dp.roundToPx()))
                layout(0, 0) { p.place(-p.width, 0) }
            }) {
                ReplyPattern.forEach { (x, y, size, alpha) ->
                    Box(Modifier.offset(x = x.dp, y = y.dp).graphicsLayer { this.alpha = alpha }) {
                        com.pixelgram.android.ui.components.CustomEmojiView(pattern, "", size = size.toFloat().dp, tint = main, animate = false, forceTint = true)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val replySticker = (reply.content as? TdApi.MessageSticker)?.sticker
            if (replySticker != null) {
                val graph = com.pixelgram.android.LocalGraph.current
                StickerView(replySticker, files, { graph.store.requestFileAsync(it) }, Modifier.padding(start = 9.dp).size(36.dp), video = false)
            } else if (thumb != null) {
                AsyncImage(
                    model = thumb.localPath?.let(::File) ?: files[thumb.mediaFileId]?.let(::File) ?: thumb.mediaThumb,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.padding(start = 9.dp).size(36.dp).clip(RoundedCornerShape(8.dp)),
                )
            }
            Column(Modifier.padding(start = 11.dp, end = 10.dp, top = 6.dp, bottom = 6.dp)) {
                if (sender.isNotBlank()) Text(sender, style = MaterialTheme.typography.labelLarge, color = main, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (quote.isNotBlank()) Text(quote, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                else Text(
                    reply.text.ifBlank {
                        when (val c = reply.content) {
                            is TdApi.MessageSticker -> stringResource(R.string.preview_sticker) + " " + c.sticker.emoji
                            is TdApi.MessageVideo -> stringResource(R.string.preview_video)
                            is TdApi.MessageAnimation -> stringResource(R.string.preview_gif)
                            is TdApi.MessageVoiceNote -> stringResource(R.string.preview_voice)
                            is TdApi.MessageVideoNote -> stringResource(R.string.preview_video_note)
                            is TdApi.MessageDocument -> c.document.fileName
                            is TdApi.MessageAudio -> stringResource(R.string.preview_audio)
                            else -> if (thumb != null) stringResource(R.string.preview_photo) else stringResource(R.string.preview_service)
                        }
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun PhotoGrid(photos: List<UiMessage>, files: Map<Int, String>, onNeedFile: (Int) -> Unit, onOpen: (List<UiMessage>, Int) -> Unit, onPick: ((UiMessage) -> Unit)? = null, picked: Set<Long> = emptySet()) {
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
                    val isPicked = photo.id in picked
                    val inset by androidx.compose.animation.core.animateDpAsState(if (isPicked) 8.dp else 0.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "pick")
                    Box(Modifier.weight(1f).aspectRatio(if (single) ratio else if (row.size == 1) 1.6f else 1f)) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(path?.let(::File) ?: photo.mediaThumb).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(inset)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = if (top && left) big else small,
                                        topEnd = if (top && right) big else small,
                                        bottomStart = if (bottom && left) big else small,
                                        bottomEnd = if (bottom && right) big else small,
                                    ),
                                )
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .clickable { if (onPick != null) onPick(photo) else onOpen(photos, start + i) },
                        )
                        if (onPick != null) {
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(CircleShape)
                                    .background(if (isPicked) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.25f))
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isPicked) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
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
internal fun PhotoViewer(photos: List<UiMessage>, start: Int, files: Map<Int, String>, onNeedFile: (Int) -> Unit, onClose: () -> Unit) {
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
private fun Composer(replyTo: UiMessage?, editing: UiMessage?, controller: ChatController, onOpenChat: (Long) -> Unit) {
    val forward = ForwardDrafts.map[controller.chatId].takeIf { editing == null }
    var forwardMenu by remember { mutableStateOf(false) }
    var forwardPick by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(RichDraft()) }
    val text = draft.text
    var selectionMenu by remember { mutableStateOf<SelectionMenu?>(null) }
    var linking by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var markdown by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(true) }
    val toolbar = remember { SelectionToolbar { selectionMenu = it } }
    var attach by remember { mutableStateOf(false) }
    var preparing by remember { mutableStateOf<List<String>?>(null) }
    var emoji by remember { mutableStateOf(false) }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    BackHandler(enabled = emoji) { emoji = false }
    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) { if (imeVisible) emoji = false }
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
                        val mime = context.contentResolver.getType(uri).orEmpty()
                        val ext = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: if (mime.startsWith("video")) "mp4" else "jpg"
                        val target = java.io.File(context.cacheDir, "send-${System.currentTimeMillis()}-$index.$ext")
                        context.contentResolver.openInputStream(uri)!!.use { input -> target.outputStream().use { input.copyTo(it) } }
                        target.absolutePath
                    }.getOrNull()
                }
            }
            if (paths.isNotEmpty()) preparing = paths
        }
    }
    var camera by remember { mutableStateOf(false) }
    val filePicker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        controller.launch {
            val paths = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                uris.mapIndexedNotNull { index, uri ->
                    runCatching {
                        val name = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "file-$index"
                        val dir = java.io.File(context.cacheDir, "send-${System.currentTimeMillis()}-$index").apply { mkdirs() }
                        val target = java.io.File(dir, name)
                        context.contentResolver.openInputStream(uri)!!.use { input -> target.outputStream().use { input.copyTo(it) } }
                        target.absolutePath
                    }.getOrNull()
                }
            }
            controller.sendDocuments(paths)
        }
    }
    val sendDraftHolder = remember { mutableStateOf<((TdApi.MessageSendOptions?) -> Unit)?>(null) }
    var pendingLive by remember { mutableStateOf(0) }
    var sendOptions by remember { mutableStateOf(false) }
    var scheduling by remember { mutableStateOf<((TdApi.MessageSchedulingState) -> Unit)?>(null) }
    var showScheduled by remember { mutableStateOf(false) }
    val sendHaptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val graphForSend = com.pixelgram.android.LocalGraph.current
    val sendSnapshot by graphForSend.store.snapshot.collectAsState()
    val reminderChat = controller.chatId == sendSnapshot.meId
    val privateUser = remember(controller.chatId, sendSnapshot.meId) {
        (graphForSend.store.chat(controller.chatId)?.type as? TdApi.ChatTypePrivate)?.userId?.let { id ->
            sendSnapshot.users[id]?.takeIf { it.bot != true }?.name ?: graphForSend.store.chat(controller.chatId)?.title
        }
    }
    val scheduledList by controller.scheduled.collectAsState()
    LaunchedEffect(controller.chatId) { controller.loadScheduled() }
    val locationPermission = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) controller.sendLocation(context, pendingLive)
    }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val voiceRecorder = remember { VoiceRecorder(context) { controller.sendVoice(it) } }
    val roundRecorder = remember { VideoNoteRecorder(context, lifecycleOwner) { controller.sendVideoNote(it) } }
    var videoMode by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val voice: MediaRecorderUi = if (videoMode) roundRecorder else voiceRecorder
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { voiceRecorder.cancel(); roundRecorder.release() } }
    val micPermission = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) {}
    LaunchedEffect(videoMode) { if (videoMode && androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) roundRecorder.prepare() }
    if (roundRecorder.preview != null) androidx.compose.ui.window.Popup(properties = androidx.compose.ui.window.PopupProperties(focusable = false, clippingEnabled = false)) { VideoNoteOverlay(roundRecorder) }
    if (attach) AttachSheet(
        controller,
        onGallery = {
            attach = false
            picker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        },
        onCamera = {
            attach = false
            camera = true
        },
        onLocation = { seconds ->
            attach = false
            pendingLive = seconds
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                controller.sendLocation(context, seconds)
            } else {
                locationPermission.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        },
    ) { attach = false }
    if (camera) CameraCapture(onDismiss = { camera = false }) { shots ->
        camera = false
        if (shots.isNotEmpty()) preparing = shots
    }
    LaunchedEffect(editing?.id) { if (editing != null) draft = RichDraft.of(editing.text, editing.entities) }
    LaunchedEffect(ComposerRequests.text) {
        val request = ComposerRequests.text ?: return@LaunchedEffect
        ComposerRequests.text = null
        draft = RichDraft.of(request, emptyList())
        focus.requestFocus()
        keyboard?.show()
    }
    preparing?.let { paths ->
        SendPhotosSheet(
            paths = paths,
            caption = text,
            onDismiss = { preparing = null },
            onRound = { path ->
                preparing = null
                controller.sendVideoAsNote(path)
            },
            onSend = { list, caption ->
                preparing = null
                draft = RichDraft()
                controller.sendPhotos(list, caption)
            },
        )
    }
    val inlineBot = rememberInlineBot(draft.text)
    Column {
    ComposerSuggestions(
        controller.chatId,
        draft.text,
        draft.value.selection.start,
        inlineBot,
        onInsert = { username ->
            val cursor = draft.value.selection.start.coerceIn(0, draft.text.length)
            val start = draft.text.lastIndexOf('@', cursor - 1).coerceAtLeast(0)
            val next = draft.text.substring(0, start) + "@" + username + " " + draft.text.substring(cursor)
            draft = draft.edit(androidx.compose.ui.text.input.TextFieldValue(next, androidx.compose.ui.text.TextRange(start + username.length + 2)))
        },
        onSendResult = { queryId, resultId ->
            controller.sendInline(queryId, resultId)
            draft = RichDraft()
        },
    )
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
            AnimatedVisibility(forward != null && target == null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                val shown = forward ?: return@AnimatedVisibility
                Box {
                    ListItem(
                        modifier = Modifier.clickable { forwardMenu = true },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = { Icon(Icons.AutoMirrored.Rounded.Forward, null, tint = MaterialTheme.colorScheme.primary) },
                        overlineContent = { Text(forwardTitle(shown), color = MaterialTheme.colorScheme.primary) },
                        headlineContent = {
                            val first = shown.messages.first()
                            val snapshot by LocalGraph.current.store.snapshot.collectAsState()
                            val who = snapshot.users[first.senderId]?.name
                            val body = first.text.ifBlank { stringResource(R.string.preview_photo) }
                            Text(if (shown.hideSender || who == null) body else "$who: $body", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        trailingContent = {
                            IconButton(onClick = { ForwardDrafts.map.remove(controller.chatId) }) { Icon(Icons.Rounded.Close, stringResource(R.string.action_cancel)) }
                        },
                    )
                    if (forwardMenu) {
                        ForwardOptionsMenu(
                            draft = shown,
                            onChange = { ForwardDrafts.map[controller.chatId] = it },
                            onChangeRecipient = {
                                forwardMenu = false
                                forwardPick = true
                            },
                            onSend = {
                                forwardMenu = false
                                ForwardDrafts.map.remove(controller.chatId)
                                controller.forwardDraft(shown)
                            },
                            onCancel = {
                                forwardMenu = false
                                ForwardDrafts.map.remove(controller.chatId)
                            },
                            onDismiss = { forwardMenu = false },
                        )
                    }
                }
            }
            if (forwardPick && forward != null) {
                ForwardPicker(
                    onPick = { to ->
                        forwardPick = false
                        ForwardDrafts.map.remove(controller.chatId)
                        ForwardDrafts.map[to] = forward
                        if (to != controller.chatId) onOpenChat(to)
                    },
                    onDismiss = { forwardPick = false },
                )
            }
            val composerPrefs = com.pixelgram.android.ui.theme.LocalAppearance.current
            val slots = remember(composerPrefs.composerStyle, composerPrefs.composerLeft, composerPrefs.composerRight, composerPrefs.composerExtra) { ComposerSlots.of(composerPrefs) }
            val telegramStyle = composerPrefs.composerStyle == "telegram"
            val toggleEmoji: () -> Unit = {
                if (emoji) {
                    emoji = false
                    focus.requestFocus()
                    keyboard?.show()
                } else {
                    keyboard?.hide()
                    emoji = true
                }
            }
            val slotPad = if (telegramStyle) 0.dp else 4.dp
            val slot: @Composable (String) -> Unit = { kind ->
                when (kind) {
                    ComposerSlots.Emoji -> IconButton(onClick = toggleEmoji, modifier = Modifier.padding(vertical = slotPad)) {
                        if (telegramStyle) TgEmojiIcon(emoji, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(30.dp))
                        else Icon(if (emoji) Icons.Rounded.Keyboard else Icons.Rounded.EmojiEmotions, stringResource(R.string.chat_emoji))
                    }
                    ComposerSlots.Attach -> IconButton(onClick = { attach = true }, modifier = Modifier.padding(vertical = slotPad)) {
                        if (telegramStyle) androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(R.drawable.tg_input_attach), stringResource(R.string.chat_attach), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        else Icon(Icons.Rounded.AttachFile, stringResource(R.string.chat_attach), Modifier.graphicsLayer { rotationZ = 45f })
                    }
                    ComposerSlots.Gallery -> IconButton(
                        onClick = { picker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                        modifier = Modifier.padding(vertical = slotPad),
                    ) { Icon(Icons.Rounded.Image, stringResource(R.string.chat_attach_photo)) }
                    ComposerSlots.Stickers, ComposerSlots.Gifs -> IconButton(
                        onClick = {
                            PanelRequest.tab = if (kind == ComposerSlots.Gifs) PanelTab.Gifs else PanelTab.Stickers
                            if (!emoji) toggleEmoji()
                        },
                        modifier = Modifier.padding(vertical = slotPad),
                    ) { Icon(ComposerSlots.icon(kind), stringResource(ComposerSlots.label(kind))) }
                    ComposerSlots.File -> IconButton(onClick = { filePicker.launch(arrayOf("*/*")) }, modifier = Modifier.padding(vertical = slotPad)) {
                        Icon(ComposerSlots.icon(kind), stringResource(ComposerSlots.label(kind)))
                    }
                    ComposerSlots.Location -> IconButton(
                        onClick = {
                            pendingLive = 0
                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) controller.sendLocation(context, 0)
                            else locationPermission.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
                        },
                        modifier = Modifier.padding(vertical = slotPad),
                    ) { Icon(ComposerSlots.icon(kind), stringResource(ComposerSlots.label(kind))) }
                    ComposerSlots.Camera -> IconButton(onClick = { camera = true }, modifier = Modifier.padding(vertical = slotPad)) {
                        Icon(androidx.compose.material.icons.Icons.Rounded.PhotoCamera, stringResource(R.string.attach_camera))
                    }
                    ComposerSlots.Send -> IconButton(onClick = { if (draft.text.isNotBlank() || forward != null) sendDraftHolder.value?.invoke(null) }, modifier = Modifier.padding(vertical = slotPad)) {
                        Icon(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.action_send), tint = MaterialTheme.colorScheme.primary)
                    }
                    else -> Unit
                }
            }
            val sendDraft: (TdApi.MessageSendOptions?) -> Unit = { options ->
                val formatted = draft.formatted()
                if (editing != null) controller.submitEdit(formatted) else if (formatted.text.isNotBlank()) controller.send(formatted, markdown, options)
                if (forward != null) {
                    ForwardDrafts.map.remove(controller.chatId)
                    controller.forwardDraft(forward)
                }
                draft = RichDraft()
            }
            sendDraftHolder.value = sendDraft
            Row(Modifier.padding(if (telegramStyle) PaddingValues(horizontal = 8.dp, vertical = 6.dp) else PaddingValues(8.dp)), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(if (telegramStyle) 6.dp else 8.dp)) {
                Surface(
                    shape = if (telegramStyle) CircleShape else RoundedCornerShape(28.dp),
                    color = if (telegramStyle) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.weight(1f).heightIn(min = if (telegramStyle) 48.dp else 56.dp).animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec()),
                ) {
                    Box {
                    if (voice.active) RecordingBar(voice)
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.graphicsLayer { alpha = if (voice.active) 0f else 1f }) {
                        val graphHere = com.pixelgram.android.LocalGraph.current
                        val menuButton by androidx.compose.runtime.produceState<TdApi.BotMenuButton?>(null, controller.chatId) {
                            val bot = (graphHere.store.chat(controller.chatId)?.type as? TdApi.ChatTypePrivate)?.userId ?: return@produceState
                            if (graphHere.store.snapshot.value.users[bot]?.bot != true) return@produceState
                            value = runCatching { graphHere.td.send(TdApi.GetUserFullInfo(bot)).botInfo?.menuButton }.getOrNull()?.takeIf { it.url.isNotBlank() }
                        }
                        val menuScope = rememberCoroutineScope()
                        menuButton?.let { button ->
                            androidx.compose.material3.Button(
                                onClick = {
                                    val bot = (graphHere.store.chat(controller.chatId)?.type as? TdApi.ChatTypePrivate)?.userId ?: return@Button
                                    menuScope.launch { openWebApp(graphHere, controller.chatId, bot, button.url, button.text) }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 10.dp).height(36.dp),
                            ) {
                                Icon(Icons.Rounded.Web, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(button.text, maxLines = 1, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        slot(slots.left)
                        val accent = MaterialTheme.colorScheme.primary
                        val onSurface = MaterialTheme.colorScheme.onSurface
                        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalTextToolbar provides toolbar) {
                        val caretPrefs = com.pixelgram.android.ui.theme.LocalAppearance.current
                        var fieldLayout by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
                        var fieldFocused by remember { mutableStateOf(false) }
                        val fieldScroll = rememberScrollState()
                        val caretRequester = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
                        LaunchedEffect(draft.value.selection.end, fieldLayout) {
                            val current = fieldLayout ?: return@LaunchedEffect
                            val end = draft.value.selection.end.coerceIn(0, current.layoutInput.text.length)
                            runCatching { caretRequester.bringIntoView(current.getCursorRect(end)) }
                        }
                        val mathAnswer = remember(draft.text, caretPrefs.inlineMath) {
                            if (caretPrefs.inlineMath) com.pixelgram.android.util.InlineMath.answer(draft.text) else null
                        }?.takeIf { draft.value.selection.collapsed && draft.value.selection.end == draft.text.length }
                        val ghostFade = remember { androidx.compose.animation.core.Animatable(0f) }
                        LaunchedEffect(mathAnswer) {
                            if (mathAnswer == null) ghostFade.snapTo(0f) else ghostFade.animateTo(1f, androidx.compose.animation.core.tween(GhostFadeMs))
                        }
                        var settled by remember { mutableStateOf<IntRange?>(null) }
                        val settle = remember { androidx.compose.animation.core.Animatable(1f) }
                        LaunchedEffect(settled) {
                            if (settled == null) return@LaunchedEffect
                            settle.snapTo(0f)
                            settle.animateTo(1f, androidx.compose.animation.core.tween(GhostFadeMs))
                            settled = null
                        }
                        var trailJump by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
                        val ghostColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = GhostAlpha)
                        androidx.compose.foundation.text.BasicTextField(
                            value = draft.value,
                            onValueChange = { next ->
                                val answer = mathAnswer
                                if (answer != null && next.text == draft.text + " " && next.selection.collapsed && next.selection.end == next.text.length) {
                                    val filled = draft.text + answer + " "
                                    settled = draft.text.length until draft.text.length + answer.length
                                    trailJump++
                                    draft = draft.edit(androidx.compose.ui.text.input.TextFieldValue(filled, androidx.compose.ui.text.TextRange(filled.length)))
                                } else {
                                    draft = draft.edit(autoReplace(draft.value, next))
                                }
                            },
                            visualTransformation = DraftStyles(
                                draft.entities,
                                accent,
                                onSurface,
                                mathAnswer,
                                ghostColor.copy(alpha = GhostAlpha * ghostFade.value),
                                settled,
                                androidx.compose.ui.graphics.lerp(ghostColor, onSurface, settle.value),
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 24.sp,
                                lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center, androidx.compose.ui.text.style.LineHeightStyle.Trim.None),
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(if (caretPrefs.liquidCursor) Color.Transparent else MaterialTheme.colorScheme.primary),
                            onTextLayout = { fieldLayout = it },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                            ),
                            modifier = Modifier.weight(1f).heightIn(max = 176.dp).verticalScroll(fieldScroll).padding(vertical = if (telegramStyle) 12.dp else 16.dp).then(Modifier.bringIntoViewRequester(caretRequester)).focusRequester(focus).onFocusChanged { fieldFocused = it.isFocused },
                            decorationBox = { inner ->
                                Box {
                                    if (text.isEmpty()) Text(stringResource(R.string.chat_message_hint), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val placeholder = (inlineBot?.user?.type as? TdApi.UserTypeBot)?.inlineQueryPlaceholder
                                    val hintLayout = fieldLayout
                                    if (inlineBot != null && inlineBot.query.isEmpty() && !placeholder.isNullOrBlank() && hintLayout != null && text.isNotEmpty()) {
                                        val x = hintLayout.getHorizontalPosition(text.length.coerceAtMost(hintLayout.layoutInput.text.length), true)
                                        val startPad = with(androidx.compose.ui.platform.LocalDensity.current) { x.toDp() }
                                        Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.matchParentSize().padding(start = startPad))
                                    }
                                    inner()
                                    val layout = fieldLayout
                                    if (layout != null) {
                                        val density = androidx.compose.ui.platform.LocalDensity.current
                                        draft.entities.filter { it.type is TdApi.TextEntityTypeCustomEmoji }.forEachIndexed { index, e ->
                                            val type = e.type as TdApi.TextEntityTypeCustomEmoji
                                            val length = layout.layoutInput.text.length
                                            if (e.offset >= length) return@forEachIndexed
                                            val first = layout.getBoundingBox(e.offset.coerceIn(0, length - 1))
                                            val last = layout.getBoundingBox((e.offset + e.length - 1).coerceIn(0, length - 1))
                                            val side = first.height
                                            val left = if (last.top == first.top) (first.left + last.right) / 2f - side / 2f else first.left
                                            key(index, type.customEmojiId) {
                                                com.pixelgram.android.ui.components.CustomEmojiView(
                                                    type.customEmojiId,
                                                    draft.text.substring(e.offset.coerceIn(0, draft.text.length), (e.offset + e.length).coerceIn(0, draft.text.length)),
                                                    Modifier.offset { androidx.compose.ui.unit.IntOffset(left.toInt(), first.top.toInt()) },
                                                    size = with(density) { side.toDp() },
                                                )
                                            }
                                        }
                                    }
                                    if (caretPrefs.liquidCursor) com.pixelgram.android.ui.components.AnimatedCaret(draft.value.selection, fieldLayout, fieldFocused, caretPrefs, MaterialTheme.colorScheme.primary, Modifier.matchParentSize(), trailJump)
                                }
                            },
                        )
                        }
                        selectionMenu?.let { menu ->
                            FormatPopup(
                                menu = menu,
                                draft = draft,
                                markdown = markdown,
                                onFormat = { format ->
                                    if (format == Format.Link) linking = true else draft = draft.toggle(format)
                                },
                                onBoldItalic = {
                                    val both = draft.has(Format.Bold) && draft.has(Format.Italic)
                                    draft = if (both) draft.toggle(Format.Bold).toggle(Format.Italic) else (if (draft.has(Format.Bold)) draft else draft.toggle(Format.Bold)).let { if (it.has(Format.Italic)) it else it.toggle(Format.Italic) }
                                },
                                onDate = { picking = true },
                                onMarkdown = { markdown = !markdown },
                                onClear = { draft = draft.clearFormatting() },
                                onDismiss = { selectionMenu = null },
                            )
                        }
                        if (picking) {
                            DateEntityDialog(onDismiss = { picking = false }) { unix ->
                                picking = false
                                draft = draft.toggle(Format.Date, unix.toString())
                            }
                        }
                        if (linking) {
                            LinkDialog(onDismiss = { linking = false }) { url ->
                                linking = false
                                draft = draft.toggle(Format.Link, url)
                            }
                        }
                        androidx.compose.animation.AnimatedVisibility(scheduledList.isNotEmpty(), enter = fadeIn() + androidx.compose.animation.expandHorizontally(), exit = fadeOut() + androidx.compose.animation.shrinkHorizontally()) {
                            IconButton(onClick = { showScheduled = true }, modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(androidx.compose.material.icons.Icons.Rounded.CalendarMonth, stringResource(R.string.scheduled_title))
                            }
                        }
                        slot(slots.right)
                        androidx.compose.animation.AnimatedVisibility(text.isEmpty(), enter = fadeIn() + androidx.compose.animation.expandHorizontally(), exit = fadeOut() + androidx.compose.animation.shrinkHorizontally()) {
                            slot(slots.extra)
                        }
                    }
                    }
                }
                val ready = text.isNotBlank() || forward != null
                val corner by androidx.compose.animation.core.animateDpAsState(if (ready) 18.dp else 28.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "send")
                val spatialSend = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
                val effectsSend = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
                androidx.compose.animation.AnimatedContent(
                    !ready && editing == null,
                    transitionSpec = {
                        (androidx.compose.animation.scaleIn(spatialSend, initialScale = 0.5f) + fadeIn(effectsSend) togetherWith
                            androidx.compose.animation.scaleOut(spatialSend, targetScale = 0.5f) + fadeOut(effectsSend)).using(androidx.compose.animation.SizeTransform(clip = false))
                    },
                    contentAlignment = Alignment.Center,
                    label = "send-voice",
                ) { showVoice ->
                    if (showVoice) VoiceButton(
                        recorder = voice,
                        video = videoMode,
                        size = if (telegramStyle) 48.dp else 56.dp,
                        telegram = telegramStyle,
                        hasPermission = {
                            val mic = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            val cam = !videoMode || androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            mic && cam
                        },
                        onRequestPermission = { micPermission.launch(if (videoMode) arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.CAMERA) else arrayOf(android.Manifest.permission.RECORD_AUDIO)) },
                        onTap = {
                            videoMode = !videoMode
                            sendHaptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.SegmentTick)
                        },
                    ) else Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(if (telegramStyle) 48.dp else 56.dp).clip(if (telegramStyle) CircleShape else RoundedCornerShape(corner)).background(MaterialTheme.colorScheme.primary).combinedClickable(
                            enabled = ready,
                            onLongClick = if (editing == null) ({
                                sendHaptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                keyboard?.hide()
                                sendOptions = true
                            }) else null,
                        ) { sendDraft(null) },
                    ) {
                        if (telegramStyle && editing == null) Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_pixelgram), stringResource(R.string.action_send), Modifier.size(26.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        else Icon(if (editing != null) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.Send, stringResource(R.string.action_send), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            if (sendOptions) SendOptionsPopup(
                preview = draft.text.ifBlank { forward?.let { "…" } ?: "" },
                effects = privateUser != null,
                canWhenOnline = privateUser != null && !reminderChat,
                onDismiss = { sendOptions = false },
                onSend = { options ->
                    sendOptions = false
                    sendDraft(options)
                },
                onSchedule = {
                    sendOptions = false
                    scheduling = { state -> sendDraft(TdApi.MessageSendOptions().apply { schedulingState = state }) }
                },
            )
            scheduling?.let { action ->
                ScheduleSheet(reminder = reminderChat, onlineName = privateUser?.takeIf { !reminderChat }, onDismiss = { scheduling = null }) { state ->
                    scheduling = null
                    action(state)
                }
            }
            if (showScheduled) ScheduledMessagesScreen(controller) { showScheduled = false }
            AnimatedVisibility(emoji, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                EmojiStickerPanel(
                    onEmoji = { draft = draft.insert(it) },
                    onCustomEmoji = { id, alt -> draft = draft.insert(alt, TdApi.TextEntityTypeCustomEmoji(id)) },
                    onSticker = { controller.sendContent(stickerInput(it)) },
                    onGif = { controller.sendContent(gifInput(it)) },
                    modifier = Modifier.fillMaxWidth().height(EmojiPanelHeight),
                )
            }
        }
    }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FormatPopup(menu: SelectionMenu, draft: RichDraft, markdown: Boolean, onFormat: (Format) -> Unit, onBoldItalic: () -> Unit, onDate: () -> Unit, onMarkdown: () -> Unit, onClear: () -> Unit, onDismiss: () -> Unit) {
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
    val selected = !draft.value.selection.collapsed
    androidx.compose.ui.window.Popup(popupPositionProvider = provider, onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 6.dp) {
            val fade = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
            androidx.compose.animation.AnimatedContent(
                formatting,
                transitionSpec = { (fadeIn(fade) togetherWith fadeOut(fade)).using(androidx.compose.animation.SizeTransform(clip = false)) },
                label = "format-menu",
            ) { open ->
                if (!open) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                        menu.cut?.let { cut -> MenuText(stringResource(R.string.action_cut)) { cut(); onDismiss() } }
                        menu.copy?.let { copy -> MenuText(stringResource(R.string.action_copy)) { copy(); onDismiss() } }
                        menu.paste?.let { paste -> MenuText(stringResource(R.string.action_paste)) { paste(); onDismiss() } }
                        if (selected) MenuText(stringResource(R.string.format_quote)) { onFormat(Format.Quote); onDismiss() }
                        if (!selected) menu.selectAll?.let { all -> MenuText(stringResource(R.string.action_select_all)) { all() } }
                        IconButton(onClick = { formatting = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.format_title)) }
                    }
                } else {
                    Column(Modifier.width(IntrinsicSize.Max).padding(vertical = 6.dp)) {
                        val base = MaterialTheme.typography.bodyLarge
                        if (selected) {
                            FormatRow(stringResource(R.string.format_spoiler), base) { onFormat(Format.Spoiler) }
                            FormatRow(stringResource(R.string.format_bold), base.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) { onFormat(Format.Bold) }
                            FormatRow(stringResource(R.string.format_italic), base.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) { onFormat(Format.Italic) }
                            FormatRow(stringResource(R.string.format_bold_italic), base.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), onBoldItalic)
                            FormatRow(stringResource(R.string.format_mono), base.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)) { onFormat(Format.Mono) }
                            FormatRow(stringResource(R.string.format_code), base.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) { onFormat(Format.Code) }
                            FormatRow(stringResource(R.string.format_strike), base.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) { onFormat(Format.Strike) }
                            FormatRow(stringResource(R.string.format_underline), base.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) { onFormat(Format.Underline) }
                            FormatRow(stringResource(R.string.format_link), base) { onFormat(Format.Link) }
                            FormatRow(stringResource(R.string.format_date), base, onDate)
                            FormatRow(stringResource(R.string.format_clear), base, onClear)
                        }
                        FormatRow(stringResource(if (markdown) R.string.format_markdown_off else R.string.format_markdown_on), base, onMarkdown)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().clickable { formatting = false }.padding(horizontal = 20.dp, vertical = 12.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, modifier = Modifier.size(20.dp))
                            Text(stringResource(R.string.action_back), style = base)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatRow(label: String, style: androidx.compose.ui.text.TextStyle, onClick: () -> Unit) {
    Text(label, style = style, maxLines = 1, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp))
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SendPhotosSheet(paths: List<String>, caption: String, onDismiss: () -> Unit, onRound: (String) -> Unit = {}, onSend: (List<String>, String) -> Unit) {
    val items = remember(paths) { androidx.compose.runtime.mutableStateListOf<String>().apply { addAll(paths) } }
    var text by remember { mutableStateOf(caption) }
    val edits = remember(paths) { androidx.compose.runtime.mutableStateMapOf<String, PhotoEdit>() }
    val videoEdits = remember(paths) { androidx.compose.runtime.mutableStateMapOf<String, VideoEdit>() }
    var hd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<String?>(null) }
    var editingVideo by remember { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val single = items.singleOrNull()
    val round = items.size == 1 && single != null && isVideoPath(single) && videoEdits[single]?.round == true
    val send = {
        if (!exporting) {
            exporting = true
            val list = items.toList()
            val edited = edits.toMap()
            val videoEdited = videoEdits.toMap()
            scope.launch {
                if (round) {
                    val path = list.first()
                    val edit = videoEdited[path]!!.let { if (it.endMs < 0 || it.endMs - it.startMs > 60_000) it.copy(endMs = it.startMs + minOf(60_000L, videoInfo(path).durationMs - it.startMs)) else it }
                    val out = runCatching { exportVideo(context, path, edit) }.getOrDefault(path)
                    exporting = false
                    onRound(out)
                    return@launch
                }
                val out = list.map { path ->
                    if (isVideoPath(path)) runCatching { exportVideo(context, path, (videoEdited[path] ?: VideoEdit()).copy(round = false)) }.getOrDefault(path)
                    else kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { runCatching { PhotoRenderer.export(context, path, edited[path] ?: PhotoEdit(), hd) }.getOrDefault(path) }
                }
                exporting = false
                onSend(out, text)
            }
        }
    }
    editing?.let { path ->
        MediaEditScreen(
            path = path,
            edit = edits[path] ?: PhotoEdit(),
            hd = hd,
            caption = text,
            onCaption = { text = it },
            onHd = { hd = !hd },
            onApply = { edits[path] = it },
            onClose = { editing = null },
            onSend = {
                editing = null
                send()
            },
        )
    }
    editingVideo?.let { path ->
        VideoEditScreen(path, videoEdits[path] ?: VideoEdit(), allowRound = items.size == 1, onClose = { editingVideo = null }) {
            videoEdits[path] = it
            editingVideo = null
        }
    }
    if (editing != null || editingVideo != null) return
    val sheet = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val videos = items.count { isVideoPath(it) }
            Text(
                when {
                    round -> stringResource(R.string.send_as_round)
                    videos == 0 -> pluralStringResource(R.plurals.send_photos_title, items.size, items.size)
                    videos == items.size -> pluralStringResource(R.plurals.send_videos_title, items.size, items.size)
                    else -> pluralStringResource(R.plurals.send_media_title, items.size, items.size)
                },
                style = MaterialTheme.typography.titleLargeEmphasized,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items.size, key = { items[it] }) { index ->
                    val path = items[index]
                    val video = isVideoPath(path)
                    val shape = if (video && round) CircleShape else RoundedCornerShape(20.dp)
                    Box(Modifier.size(if (items.size == 1) 260.dp else 160.dp).animateItem().clip(shape).clickable { if (video) editingVideo = path else editing = path }) {
                        val edit = edits[path]
                        val rendered by androidx.compose.runtime.produceState<android.graphics.Bitmap?>(null, path, edit) {
                            value = if (edit == null || edit.untouched) null else kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { runCatching { PhotoRenderer.render(path, edit, 480) }.getOrNull() }
                        }
                        val start = videoEdits[path]?.startMs ?: 0L
                        val frame by androidx.compose.runtime.produceState<android.graphics.Bitmap?>(null, path, start) {
                            if (video) value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                runCatching { android.media.MediaMetadataRetriever().run { setDataSource(path); getFrameAtTime(start * 1000).also { release() } } }.getOrNull()
                            }
                        }
                        AsyncImage(
                            model = if (video) frame else rendered ?: File(path),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                        Icon(Icons.Rounded.Edit, stringResource(R.string.action_edit), tint = Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).size(36.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape).padding(8.dp))
                        if (video) {
                            Icon(androidx.compose.material.icons.Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(48.dp).background(Color.Black.copy(alpha = 0.45f), CircleShape).padding(8.dp))
                            val ve = videoEdits[path]
                            val length = remember(path) { videoInfo(path).durationMs }
                            val shown = if (ve == null) length else (if (ve.endMs > 0) ve.endMs else length) - ve.startMs
                            Text(
                                "%d:%02d".format(shown / 60000, shown / 1000 % 60),
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        if (items.size > 1) {
                            Surface(
                                onClick = { items.remove(path) },
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.55f),
                                contentColor = Color.White,
                                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(32.dp),
                            ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, stringResource(R.string.action_remove), Modifier.size(20.dp)) } }
                        }
                    }
                }
            }
            if (!round) androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.send_caption_hint)) },
                shape = RoundedCornerShape(24.dp),
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (videos < items.size) androidx.compose.material3.FilterChip(
                    selected = hd,
                    onClick = { hd = !hd },
                    label = { Text("HD", style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(52.dp),
                )
                Spacer(Modifier.weight(1f))
                FilledIconButton(
                    onClick = { send() },
                    enabled = items.isNotEmpty() && !exporting,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.size(64.dp),
                ) {
                    if (exporting) androidx.compose.material3.CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 3.dp)
                    else Icon(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.action_send), Modifier.size(28.dp))
                }
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
    onShot: () -> Unit,
    onMultiSelect: () -> Unit,
    pinned: Boolean,
    onPin: () -> Unit,
    onQuickReact: () -> Unit = {},
) {
    val openedAt = remember { System.currentTimeMillis() }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    LaunchedEffect(Unit) {
        keyboard?.hide()
        focusManager.clearFocus()
    }
    val sticker = (message.content as? TdApi.MessageSticker)?.sticker
    val faved by androidx.compose.runtime.produceState(false, message.id) { if (sticker != null) value = controller.isFavoriteSticker(sticker) }
    val canEdit by androidx.compose.runtime.produceState(false, message.id) { value = controller.canEdit(message) }
    val menuProps by androidx.compose.runtime.produceState<TdApi.MessageProperties?>(null, message.id) { value = controller.properties(message) }
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
    androidx.activity.compose.BackHandler { close {} }
    Box(Modifier.fillMaxSize().zIndex(50f)) {
        val scrim by androidx.compose.animation.core.animateFloatAsState(if (shown.targetState) 0.4f else 0f, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "scrim")
        androidx.compose.ui.layout.Layout(
            content = {
                Box(Modifier.background(Color.Black.copy(alpha = scrim)).clickable(indication = null, interactionSource = null) {
                    if (System.currentTimeMillis() - openedAt < QuickReactMs) close(onQuickReact) else close {}
                })
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
                    val props = menuProps
                    val primary = buildList<Triple<androidx.compose.ui.graphics.vector.ImageVector, String, () -> Unit>> {
                        if (props?.canBeReplied != false) add(Triple(Icons.AutoMirrored.Rounded.Reply, context.getString(R.string.action_reply)) { close { controller.reply(message) } })
                        if (message.text.isNotBlank()) add(Triple(Icons.Rounded.ContentCopy, context.getString(R.string.action_copy)) { close { clipboard.setText(androidx.compose.ui.text.AnnotatedString(message.text)) } })
                        link?.let { url -> add(Triple(Icons.Rounded.Link, context.getString(R.string.action_copy_link)) { close { clipboard.setText(androidx.compose.ui.text.AnnotatedString(url)) } }) }
                    }
                    val secondary = buildList<Triple<androidx.compose.ui.graphics.vector.ImageVector, String, () -> Unit>> {
                        if (sticker != null) {
                            add(Triple(Icons.Rounded.Star, context.getString(if (faved) R.string.sticker_unfave else R.string.sticker_fave)) { close { controller.faveSticker(sticker, !faved) } })
                            if (sticker.setId != 0L) add(Triple(Icons.Rounded.GridView, context.getString(R.string.sticker_view_set)) { close { EmojiPreview.pack = sticker.setId } })
                        }
                        if (props?.canBeForwarded != false) add(Triple(Icons.AutoMirrored.Rounded.Forward, context.getString(R.string.action_forward)) { close(onForward) })
                        if (props?.canBePinned == true) {
                            if (pinned) add(Triple(Icons.Rounded.PushPin, context.getString(R.string.action_unpin)) { close { controller.unpin(message) } })
                            else add(Triple(Icons.Rounded.PushPin, context.getString(R.string.action_pin)) { close { onPin() } })
                        }
                        if (canEdit) add(Triple(Icons.Rounded.Edit, context.getString(R.string.action_edit)) { close { controller.startEdit(message) } })
                        add(Triple(Icons.Rounded.CheckCircle, context.getString(R.string.action_select)) { close(onMultiSelect) })
                    }
                    val canDelete = props?.let { it.canBeDeletedOnlyForSelf || it.canBeDeletedForAllUsers } ?: false
                    Column(Modifier.width(MenuWidth), verticalArrangement = Arrangement.spacedBy(MenuGap)) {
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 6.dp) {
                            var details by remember { mutableStateOf(false) }
                            androidx.compose.animation.AnimatedContent(details, label = "menu-details") { open ->
                                if (open) {
                                    MessageDetails(controller.chatId, message) { details = false }
                                } else {
                                    Column(Modifier.padding(bottom = 6.dp)) {
                                        MenuHeader(controller.chatId, message) { details = true }
                                        primary.forEach { (icon, label, action) -> MenuEntry(icon, label, MaterialTheme.colorScheme.onSurface, action) }
                                    }
                                }
                            }
                        }
                        if (secondary.isNotEmpty()) Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 6.dp) {
                            Column(Modifier.padding(vertical = 6.dp)) {
                                secondary.forEach { (icon, label, action) -> MenuEntry(icon, label, MaterialTheme.colorScheme.onSurface, action) }
                            }
                        }
                        if (canDelete) Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 6.dp) {
                            Column(Modifier.padding(vertical = 6.dp)) {
                                MenuEntry(Icons.Rounded.Delete, stringResource(R.string.action_delete), MaterialTheme.colorScheme.error) { close(onDelete) }
                            }
                        }
                    }
                }
            },
        ) { measurables, constraints ->
            val loose = constraints.copy(minWidth = 0, minHeight = 0)
            val full = measurables.getOrNull(0)?.measure(androidx.compose.ui.unit.Constraints.fixed(constraints.maxWidth, constraints.maxHeight))
            val bar = measurables.getOrNull(1)?.measure(loose)
            val menu = measurables.getOrNull(2)?.measure(loose)
            if (bar == null || menu == null) {
                return@Layout layout(constraints.maxWidth, constraints.maxHeight) { full?.place(0, 0) }
            }
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
                full?.place(0, 0)
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
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, color = tint, style = MaterialTheme.typography.bodyMedium)
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

private val MenuWidth = 196.dp
private val MenuGap = 8.dp
private const val QuickReactMs = 300L

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MessageActions(message: UiMessage, controller: ChatController, onDismiss: () -> Unit, onForward: () -> Unit, onDelete: () -> Unit) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val canEdit by androidx.compose.runtime.produceState(false, message.id) { value = controller.canEdit(message) }
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
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
private fun DeleteDialog(messages: List<UiMessage>, controller: ChatController, privateUserId: Long, snapshot: StoreSnapshot, onDone: (Boolean) -> Unit) {
    val canEveryone by androidx.compose.runtime.produceState(false, messages.map { it.id }) {
        value = messages.isNotEmpty() && messages.all { controller.canDeleteForEveryone(it) }
    }
    var everyone by remember { mutableStateOf(true) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { onDone(false) },
        icon = { Icon(Icons.Rounded.Delete, null) },
        title = {
            Text(
                if (messages.size == 1) stringResource(R.string.delete_title)
                else androidx.compose.ui.res.pluralStringResource(R.plurals.delete_title_many, messages.size, messages.size),
            )
        },
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
                controller.deleteAll(messages, canEveryone && everyone)
                onDone(true)
            }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { onDone(false) }) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SelectionTopBar(
    count: Int,
    canEdit: Boolean,
    controller: ChatController,
    first: UiMessage,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onForward: () -> Unit,
    onDelete: () -> Unit,
    onShot: () -> Unit,
) {
    val editable by androidx.compose.runtime.produceState(false, first.id, canEdit) { value = canEdit && controller.canEdit(first) }
    TopAppBar(
        navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.action_cancel)) } },
        title = {
            androidx.compose.animation.AnimatedContent(count, label = "selected-count") { n ->
                Text(androidx.compose.ui.res.pluralStringResource(R.plurals.selected_count, n, n))
            }
        },
        actions = {
            if (editable) IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, stringResource(R.string.action_edit)) }
            IconButton(onClick = onShot) { Icon(Icons.Rounded.PhotoCamera, stringResource(R.string.shot_action)) }
            IconButton(onClick = onForward) { Icon(Icons.AutoMirrored.Rounded.Forward, stringResource(R.string.action_forward)) }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, stringResource(R.string.action_delete)) }
        },
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SelectionBottomBar(canReply: Boolean, onReply: () -> Unit, onForward: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.material3.FilledTonalButton(
            onClick = onReply,
            enabled = canReply,
            shapes = androidx.compose.material3.ButtonDefaults.shapes(),
            modifier = Modifier.weight(1f).height(56.dp),
        ) {
            Text(stringResource(R.string.action_reply))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Rounded.Reply, null)
        }
        androidx.compose.material3.FilledTonalButton(
            onClick = onForward,
            shapes = androidx.compose.material3.ButtonDefaults.shapes(),
            modifier = Modifier.weight(1f).height(56.dp),
        ) {
            Icon(Icons.AutoMirrored.Rounded.Forward, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_forward))
        }
    }
}

private val ReactorSize = 18.dp
private const val DatePillMs = 1_500L
private val SpoilerBlur = 8.dp
private const val GhostFadeMs = 220
private const val GhostAlpha = 0.6f
private const val ReactorAvatars = 4
private const val HighlightMs = 1_400L
private const val QuoteLines = 3

@Composable
private fun TextWithMeta(lastLine: Float, showMeta: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.ui.layout.Layout(content, modifier) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val text = measurables[0].measure(loose)
        val meta = if (showMeta && measurables.size > 1) measurables[1].measure(loose) else null
        if (meta == null) return@Layout layout(maxOf(text.width, constraints.minWidth), text.height) { text.place(0, 0) }
        val gap = 8.dp.roundToPx()
        val last = if (lastLine < 0f) text.width.toFloat() else lastLine
        val inline = last + gap + meta.width <= constraints.maxWidth
        val width: Int
        val height: Int
        if (inline) {
            width = maxOf(text.width, (last + gap + meta.width).toInt(), constraints.minWidth)
            height = text.height
        } else {
            width = maxOf(text.width, meta.width, constraints.minWidth)
            height = text.height + meta.height
        }
        layout(width, height) {
            text.place(0, 0)
            meta.place(width - meta.width, height - meta.height)
        }
    }
}

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
            metaPos = androidx.compose.ui.unit.IntOffset(width - meta.width, y + (maxOf(lineHeight, meta.height) - meta.height) * 3 / 4)
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
    val quotes = entities.filter { it.type is TdApi.TextEntityTypeBlockQuote || it.type is TdApi.TextEntityTypeExpandableBlockQuote || it.type is TdApi.TextEntityTypePre || it.type is TdApi.TextEntityTypePreCode }.sortedBy { it.offset }
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
    if (e.type is TdApi.TextEntityTypeBlockQuote || e.type is TdApi.TextEntityTypeExpandableBlockQuote || e.type is TdApi.TextEntityTypePre || e.type is TdApi.TextEntityTypePreCode) return@mapNotNull null
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
    onLastLine: (Float) -> Unit = {},
) {
    val parts = remember(text, entities) { splitQuotes(text, entities) }
    if (parts.size == 1 && parts[0].quote == null) {
        PlainText(text, entities, link, content, style, fontSize, modifier, onLastLine = onLastLine)
        return
    }
    val lastPart = parts.lastOrNull { text.substring(it.start, it.end).trim('\n').isNotEmpty() }
    if (lastPart?.quote != null) onLastLine(Float.MAX_VALUE)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        parts.forEach { part ->
            val raw = text.substring(part.start, part.end)
            val trimStart = raw.length - raw.trimStart('\n').length
            val piece = raw.trim('\n')
            if (piece.isEmpty()) return@forEach
            val local = slice(entities, part.start + trimStart, part.start + trimStart + piece.length)
            val code = part.quote as? TdApi.TextEntityTypePre
            val codeLang = part.quote as? TdApi.TextEntityTypePreCode
            if (code != null || codeLang != null) {
                if (part === lastPart) onLastLine(Float.MAX_VALUE)
                com.pixelgram.android.ui.components.CodeBlock(raw.trim('\n'), codeLang?.language.orEmpty(), style)
            } else if (part.quote == null) {
                PlainText(piece, local, link, content, style, fontSize, onLastLine = if (part === lastPart) onLastLine else ({}))
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
    onLastLine: (Float) -> Unit = {},
) {
    @Suppress("NAME_SHADOWING")
    val entities = remember(text, entities) { withAutoLinks(text, entities) }
    val report: (androidx.compose.ui.text.TextLayoutResult) -> Unit = { layout -> onLastLine(layout.getLineRight(layout.lineCount - 1)) }
    val inline = remember(text, entities) { com.pixelgram.android.ui.components.customEmojiSlots(entities, text) }
    val hasSpoiler = remember(entities) { entities.any { it.type is TdApi.TextEntityTypeSpoiler } }
    val strikes = remember(text, entities) { entities.filter { it.type is TdApi.TextEntityTypeStrikethrough }.map { it.offset until (it.offset + it.length) } }
    var strikeLayout by remember(text) { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    val waveColor = content
    val wave = if (strikes.isEmpty()) Modifier else Modifier.drawWithContent {
        drawContent()
        strikeLayout?.let { l -> strikes.forEach { drawWavyStrike(l, it, waveColor) } }
    }
    val reportAll: (androidx.compose.ui.text.TextLayoutResult) -> Unit = { strikeLayout = it; report(it) }

    if (!hasSpoiler) {
        com.pixelgram.android.ui.components.OverlayEmojiText(formatted(text, entities, link, content), inline, style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis, modifier = modifier.then(wave), onTextLayout = reportAll)
        return
    }
    var revealed by remember(text) { mutableStateOf(false) }
    val blurSupported = android.os.Build.VERSION.SDK_INT >= 31
    val radius by androidx.compose.animation.core.animateDpAsState(if (revealed) 0.dp else SpoilerBlur, MaterialTheme.motionScheme.slowEffectsSpec(), label = "spoiler")
    var spoilerLayout by remember(text) { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val ranges = remember(entities) { entities.filter { it.type is TdApi.TextEntityTypeSpoiler }.map { it.offset until (it.offset + it.length) } }
    val path = remember(spoilerLayout, ranges) {
        androidx.compose.ui.graphics.Path().apply { spoilerLayout?.let { l -> ranges.forEach { addPath(spoilerPill(l, it.first, it.last + 1, density)) } } }
    }
    val trackLayout: (androidx.compose.ui.text.TextLayoutResult) -> Unit = { spoilerLayout = it; report(it) }
    Box(
        modifier.pointerInput(revealed) {
            if (revealed) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                SpoilerTouch.downAt = down.uptimeMillis
                val up = waitForUpOrCancellation()
                if (up != null) {
                    up.consume()
                    revealed = true
                }
            }
        },
    ) {
        when {
            revealed && radius == 0.dp -> com.pixelgram.android.ui.components.OverlayEmojiText(formatted(text, entities, link, content, SpoilerMode.Shown), inline, style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis, onTextLayout = reportAll, modifier = wave)
            !blurSupported -> com.pixelgram.android.ui.components.OverlayEmojiText(formatted(text, entities, link, content, SpoilerMode.Block), inline, style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis, onTextLayout = report)
            else -> {
                val strength = radius / SpoilerBlur
                val shape = remember(path) {
                    object : androidx.compose.ui.graphics.Shape {
                        override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density) = androidx.compose.ui.graphics.Outline.Generic(path)
                    }
                }
                com.pixelgram.android.ui.components.OverlayEmojiText(formatted(text, entities, link, content, SpoilerMode.Hidden), inline, style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis, onTextLayout = trackLayout)
                Text(
                    formatted(text, entities, link, content, SpoilerMode.Shown),
                    style = style,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.graphicsLayer {
                        clip = true
                        this.shape = shape
                        alpha = strength
                    }.blur(SpoilerBlur * strength, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded),
                )
                if (!revealed) SpoilerShimmer(path, content, Modifier.matchParentSize())
            }
        }
    }
}

object SpoilerTouch {
    var downAt = -1L
}

@Composable
private fun SpoilerShimmer(path: androidx.compose.ui.graphics.Path, tint: Color, modifier: Modifier) {
    val sweep by androidx.compose.runtime.produceState(0f) {
        val start = System.nanoTime()
        while (true) {
            androidx.compose.runtime.withFrameNanos { now -> value = ((now - start) / 1_000_000L % ShimmerMs) / ShimmerMs.toFloat() }
        }
    }
    Spacer(
        modifier.drawBehind {
            val bounds = path.getBounds()
            if (bounds.isEmpty) return@drawBehind
            val band = maxOf(bounds.width * ShimmerBand, ShimmerMinBand.toPx())
            val x = bounds.left - band + sweep * (bounds.width + band * 2)
            clipPath(path) {
                drawRect(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(Color.Transparent, tint.copy(alpha = ShimmerAlpha), Color.Transparent),
                        start = androidx.compose.ui.geometry.Offset(x - band, bounds.top),
                        end = androidx.compose.ui.geometry.Offset(x + band, bounds.bottom),
                    ),
                )
            }
        },
    )
}

private fun spoilerPill(layout: androidx.compose.ui.text.TextLayoutResult, start: Int, end: Int, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Path {
    val path = androidx.compose.ui.graphics.Path()
    val length = layout.layoutInput.text.length
    if (length == 0 || start >= end) return path
    val from = start.coerceIn(0, length)
    val to = end.coerceIn(0, length)
    with(density) {
        val firstLine = layout.getLineForOffset(from)
        val lastLine = layout.getLineForOffset(maxOf(from, to - 1))
        for (line in firstLine..lastLine) {
            val first = line == firstLine
            val last = line == lastLine
            val charStart = if (first) from else layout.getLineStart(line)
            var charEnd = if (last) to else layout.getLineEnd(line, visibleEnd = true)
            if (charEnd > charStart && layout.layoutInput.text[charEnd - 1] == '\n') charEnd -= 1
            if (charStart >= charEnd && firstLine != lastLine) continue
            val startBox = layout.getBoundingBox(charStart)
            val endBox = layout.getBoundingBox(maxOf(charStart, charEnd - 1))
            val left = if (first) startBox.left else layout.getLineLeft(line)
            val right = if (last) endBox.right else layout.getLineRight(line)
            val lineTop = layout.getLineTop(line)
            val lineBottom = layout.getLineBottom(line)
            val centerY = (lineTop + lineBottom) / 2f
            val half = (lineBottom - lineTop) / 2f - PillInset.toPx()
            val round = minOf(half, PillRadius.toPx())
            val startRadius = if (first) round else 2.dp.toPx()
            val endRadius = if (last) round else 2.dp.toPx()
            val pad = 3.dp.toPx()
            path.addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = minOf(left, right) - pad,
                    top = centerY - half,
                    right = maxOf(left, right) + pad,
                    bottom = centerY + half,
                    topLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(startRadius),
                    bottomLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(startRadius),
                    topRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(endRadius),
                    bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(endRadius),
                ),
            )
        }
    }
    return path
}

private const val ShimmerMs = 1_800
private const val ShimmerBand = 0.35f
private const val ShimmerAlpha = 0.38f
private val ShimmerMinBand = 28.dp
private val PillRadius = 10.dp
private val PillInset = 1.dp

@Composable
private fun QuoteBlock(expandable: Boolean, content: Color, body: @Composable (Int) -> Unit) {
    var expanded by remember { mutableStateOf(!expandable) }
    val accent = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.14f))
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

private fun bigEmoji(message: UiMessage): List<Pair<Long?, String>>? {
    if (message.content !is TdApi.MessageText) return null
    val text = message.text
    if (text.isBlank()) return null
    val custom = message.entities.filter { it.type is TdApi.TextEntityTypeCustomEmoji }.sortedBy { it.offset }
    val out = ArrayList<Pair<Long?, String>>()
    var cursor = 0
    fun plain(until: Int): Boolean {
        val chunk = text.substring(cursor, until)
        val it = java.text.BreakIterator.getCharacterInstance()
        it.setText(chunk)
        var start = it.first()
        var end = it.next()
        while (end != java.text.BreakIterator.DONE) {
            val g = chunk.substring(start, end)
            if (g.isNotBlank()) {
                if (!com.pixelgram.android.td.LocalPremiumEmoji.isEmoji(g)) return false
                out += null to g
            }
            start = end
            end = it.next()
        }
        return true
    }
    for (e in custom) {
        val start = e.offset.coerceIn(cursor, text.length)
        if (!plain(start)) return null
        val end = (e.offset + e.length).coerceIn(start, text.length)
        out += (e.type as TdApi.TextEntityTypeCustomEmoji).customEmojiId to text.substring(start, end)
        cursor = end
    }
    if (!plain(text.length)) return null
    if (out.isEmpty() || out.size > BigEmojiMax) return null
    return out
}

@Composable
private fun BareMeta(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(10.dp), color = Color.Black.copy(alpha = 0.4f), modifier = modifier.padding(4.dp)) {
        Box(Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) { content() }
    }
}

private const val BigEmojiMax = 7
private val BigEmojiSizes = listOf(112.dp, 88.dp, 72.dp, 60.dp, 52.dp, 46.dp, 40.dp)

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
                is TdApi.TextEntityTypeStrikethrough -> Unit
                is TdApi.TextEntityTypeCode, is TdApi.TextEntityTypePre, is TdApi.TextEntityTypePreCode ->
                    addStyle(androidx.compose.ui.text.SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, background = content.copy(alpha = 0.1f)), start, end)
                is TdApi.TextEntityTypeSpoiler -> when (spoiler) {
                    SpoilerMode.Block -> addStyle(androidx.compose.ui.text.SpanStyle(color = Color.Transparent, background = content.copy(alpha = 0.35f)), start, end)
                    SpoilerMode.Hidden -> addStyle(androidx.compose.ui.text.SpanStyle(color = Color.Transparent), start, end)
                    SpoilerMode.Only -> addStyle(androidx.compose.ui.text.SpanStyle(color = content), start, end)
                    SpoilerMode.Shown -> addStyle(androidx.compose.ui.text.SpanStyle(background = content.copy(alpha = 0.08f)), start, end)
                }
                is TdApi.TextEntityTypeUrl -> if (!com.pixelgram.android.feature.pixel.Credits.handles(text.substring(start, end))) addLink(androidx.compose.ui.text.LinkAnnotation.Url(text.substring(start, end).let { if (it.contains("://")) it else "https://$it" }, androidx.compose.ui.text.TextLinkStyles(androidx.compose.ui.text.SpanStyle(color = link, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))), start, end)
                is TdApi.TextEntityTypeTextUrl -> if (com.pixelgram.android.feature.pixel.Credits.handles(type.url)) addLink(androidx.compose.ui.text.LinkAnnotation.Clickable("tg-credits", androidx.compose.ui.text.TextLinkStyles(androidx.compose.ui.text.SpanStyle(color = link, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))) { com.pixelgram.android.feature.pixel.Credits.open = true }, start, end) else addLink(androidx.compose.ui.text.LinkAnnotation.Clickable(type.url, androidx.compose.ui.text.TextLinkStyles(androidx.compose.ui.text.SpanStyle(color = link, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))) { com.pixelgram.android.nav.LinkConfirm.ask(type.url, text.substring(start, end)) }, start, end)
                is TdApi.TextEntityTypeCustomEmoji -> addStringAnnotation(com.pixelgram.android.ui.components.CustomEmojiTag, com.pixelgram.android.ui.components.customEmojiKey(type.customEmojiId), start, end)
                is TdApi.TextEntityTypeMention -> addLink(androidx.compose.ui.text.LinkAnnotation.Url("https://t.me/" + text.substring(start, end).removePrefix("@"), androidx.compose.ui.text.TextLinkStyles(androidx.compose.ui.text.SpanStyle(color = link))), start, end)
                is TdApi.TextEntityTypeMentionName -> addLink(androidx.compose.ui.text.LinkAnnotation.Url("tg://user?id=" + type.userId, androidx.compose.ui.text.TextLinkStyles(androidx.compose.ui.text.SpanStyle(color = link))), start, end)
                is TdApi.TextEntityTypeEmailAddress -> addLink(androidx.compose.ui.text.LinkAnnotation.Url("mailto:" + text.substring(start, end), androidx.compose.ui.text.TextLinkStyles(androidx.compose.ui.text.SpanStyle(color = link, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))), start, end)
                is TdApi.TextEntityTypePhoneNumber -> addLink(androidx.compose.ui.text.LinkAnnotation.Url("tel:" + text.substring(start, end), androidx.compose.ui.text.TextLinkStyles(androidx.compose.ui.text.SpanStyle(color = link, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))), start, end)
                is TdApi.TextEntityTypeDateTime -> {
                    val shown = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(java.util.Date(type.unixTime * 1000L))
                    addLink(androidx.compose.ui.text.LinkAnnotation.Clickable("date$start", androidx.compose.ui.text.TextLinkStyles(androidx.compose.ui.text.SpanStyle(color = link, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))) { com.pixelgram.android.ui.components.Notice.show(shown) }, start, end)
                }
                is TdApi.TextEntityTypeHashtag, is TdApi.TextEntityTypeBotCommand ->
                    addStyle(androidx.compose.ui.text.SpanStyle(color = link), start, end)
                else -> Unit
            }
        }
        com.pixelgram.android.feature.pixel.Credits.LinkPattern.findAll(text).forEach { match ->
            addLink(
                androidx.compose.ui.text.LinkAnnotation.Clickable(
                    "tg-credits",
                    androidx.compose.ui.text.TextLinkStyles(androidx.compose.ui.text.SpanStyle(color = link, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)),
                ) { com.pixelgram.android.feature.pixel.Credits.open = true },
                match.range.first,
                match.range.last + 1,
            )
        }
        com.pixelgram.android.nav.PixelLinks.Pattern.findAll(text).forEach { match ->
            addLink(
                androidx.compose.ui.text.LinkAnnotation.Clickable(
                    "pg-link",
                    androidx.compose.ui.text.TextLinkStyles(androidx.compose.ui.text.SpanStyle(color = link, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)),
                ) { com.pixelgram.android.nav.PendingNav.link = match.value },
                match.range.first,
                match.range.last + 1,
            )
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
private fun AttachSheet(controller: ChatController, onGallery: () -> Unit, onCamera: () -> Unit, onLocation: (Int) -> Unit, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var poll by remember { mutableStateOf(false) }
    var contact by remember { mutableStateOf(false) }
    fun copy(uris: List<android.net.Uri>, send: (List<String>) -> Unit) {
        controller.launch {
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
    var live by remember { mutableStateOf(false) }
    if (live) {
        LivePeriodDialog(onDismiss = { live = false; onDismiss() }) { seconds ->
            live = false
            onLocation(seconds)
        }
        return
    }
    val sheet = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        val items = listOf(
            Triple(Icons.Rounded.Image, R.string.attach_gallery) { onGallery() },
            Triple(androidx.compose.material.icons.Icons.Rounded.PhotoCamera, R.string.attach_camera) { onCamera() },
            Triple(Icons.Rounded.AttachFile, R.string.attach_files) { files.launch(arrayOf("*/*")) },
            Triple(androidx.compose.material.icons.Icons.Rounded.MyLocation, R.string.attach_live_location) { live = true },
            Triple(androidx.compose.material.icons.Icons.Rounded.Person, R.string.attach_contacts) { contact = true },
            Triple(Icons.Rounded.Poll, R.string.attach_poll) { poll = true },
            Triple(androidx.compose.material.icons.Icons.Rounded.Place, R.string.attach_location) { onLocation(0) },
        )
        androidx.compose.foundation.layout.FlowRow(
            maxItemsInEachRow = 4,
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 4.dp, bottom = 28.dp),
        ) {
            val perms = controller.state.value.permissions
            val allowed = listOf(
                perms == null || perms.canSendPhotos || perms.canSendVideos,
                perms == null || perms.canSendPhotos,
                perms == null || perms.canSendDocuments,
                perms == null || perms.canSendBasicMessages,
                perms == null || perms.canSendBasicMessages,
                perms == null || perms.canSendPolls,
                perms == null || perms.canSendBasicMessages,
            )
            val restrictedText = stringResource(R.string.restricted_media)
            val context = androidx.compose.ui.platform.LocalContext.current
            items.forEachIndexed { index, (icon, label, rawAction) ->
                val enabled = allowed.getOrElse(index) { true }
                val action = if (enabled) rawAction else ({ android.widget.Toast.makeText(context, restrictedText, android.widget.Toast.LENGTH_SHORT).show() })
                val appear = remember { androidx.compose.animation.core.Animatable(0f) }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(index * 25L)
                    appear.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = 500f))
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).graphicsLayer {
                        alpha = appear.value.coerceIn(0f, 1f) * (if (enabled) 1f else 0.38f)
                        translationY = (1f - appear.value) * 24.dp.toPx()
                    },
                ) {
                    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    val corner by androidx.compose.animation.core.animateDpAsState(if (pressed) 14.dp else 28.dp, androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 800f), label = "tile-corner")
                    Box(
                        Modifier.size(width = 76.dp, height = 56.dp).clip(RoundedCornerShape(corner)).background(MaterialTheme.colorScheme.surfaceContainerLowest).clickable(interaction, androidx.compose.material3.ripple(), onClick = action),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, null, tint = if (index == 3) androidx.compose.ui.graphics.Color(0xFF2EBD59) else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(26.dp))
                    }
                    Text(stringResource(label), style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 2)
                }
            }
            repeat((4 - items.size % 4) % 4) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun LivePeriodDialog(onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val options = listOf(15 * 60 to R.string.live_15m, 60 * 60 to R.string.live_1h, 8 * 60 * 60 to R.string.live_8h, 0x7FFFFFFF to R.string.live_forever)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.attach_live_location)) },
        text = {
            Column {
                options.forEach { (seconds, label) ->
                    Text(
                        stringResource(label),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onPick(seconds) }.padding(horizontal = 8.dp, vertical = 14.dp),
                    )
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
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

private fun compactCount(value: Int, exact: Boolean): String = when {
    exact || value < 1_000 -> value.toString()
    value < 1_000_000 -> String.format(java.util.Locale.getDefault(), "%.1fK", value / 1_000f).replace(".0K", "K").replace(",0K", "K")
    else -> String.format(java.util.Locale.getDefault(), "%.1fM", value / 1_000_000f).replace(".0M", "M").replace(",0M", "M")
}

private const val FillWidth = "fill-width"
private const val StretchWidth = "stretch-width"

private const val AlignEnd = "align-end"

@Composable
private fun WidestColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.ui.layout.Layout(content = content, modifier = modifier) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = arrayOfNulls<androidx.compose.ui.layout.Placeable>(measurables.size)
        var widest = 0
        measurables.forEachIndexed { i, m ->
            if (m.layoutId != FillWidth && m.layoutId != StretchWidth) {
                val p = m.measure(loose)
                placeables[i] = p
                widest = maxOf(widest, p.width)
            }
        }
        measurables.forEach { m ->
            if (m.layoutId == StretchWidth) widest = maxOf(widest, m.maxIntrinsicWidth(constraints.maxHeight.coerceAtMost(10_000)).coerceAtMost(constraints.maxWidth))
        }
        var full = widest
        measurables.forEach { m ->
            if (m.layoutId == FillWidth) full = maxOf(full, m.maxIntrinsicWidth(constraints.maxHeight.coerceAtMost(10_000)).coerceAtMost(constraints.maxWidth))
        }
        full = full.coerceAtMost(constraints.maxWidth)
        measurables.forEachIndexed { i, m ->
            if (m.layoutId == FillWidth || m.layoutId == StretchWidth) {
                placeables[i] = m.measure(androidx.compose.ui.unit.Constraints.fixedWidth(full).copy(maxHeight = constraints.maxHeight))
            }
        }
        val width = maxOf(placeables.maxOfOrNull { it?.width ?: 0 } ?: 0, constraints.minWidth)
        val height = placeables.sumOf { it?.height ?: 0 }
        layout(width, height) {
            var y = 0
            measurables.forEachIndexed { i, m ->
                val p = placeables[i] ?: return@forEachIndexed
                val x = if (m.layoutId == AlignEnd) width - p.width else 0
                p.place(x, y)
                y += p.height
            }
        }
    }
}

@Composable
private fun BusinessBotBar(graph: AppGraph, chatId: Long, bar: TdApi.BusinessBotManageBar) {
    val scope = rememberCoroutineScope()
    val snapshot by graph.store.snapshot.collectAsState()
    val bot = snapshot.users[bar.botUserId]
    val botName = bot?.let { listOf(it.firstName, it.lastName).filter { p -> p.isNotBlank() }.joinToString(" ") }.orEmpty()
    var menu by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)) {
            Avatar(botName, bar.botUserId, bot?.photoFileId?.let { snapshot.files[it] }, bot?.minithumb, 36.dp, onNeedFile = bot?.photoFileId?.takeIf { it != 0 }?.let { { graph.store.requestFile(it) } })
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(botName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(if (bar.isBotPaused) R.string.business_bot_paused else if (bar.canBotReply) R.string.business_bot_manages else R.string.business_bot_reads),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            androidx.compose.material3.FilledTonalButton(
                onClick = { scope.launch { runCatching { graph.td.send(TdApi.ToggleBusinessConnectedBotChatIsPaused(chatId, !bar.isBotPaused)) } } },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
                modifier = Modifier.height(36.dp),
            ) { Text(stringResource(if (bar.isBotPaused) R.string.business_bot_resume else R.string.business_bot_pause)) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, null) }
                com.pixelgram.android.ui.components.ExpressiveMenu(
                    menu,
                    { menu = false },
                    listOf(
                        listOfNotNull(
                            bar.manageUrl.takeIf { it.isNotBlank() }?.let { url ->
                                com.pixelgram.android.ui.components.MenuEntry(Icons.Rounded.Settings, stringResource(R.string.business_bot_manage)) { com.pixelgram.android.nav.PendingNav.external = url }
                            },
                        ),
                        listOf(
                            com.pixelgram.android.ui.components.MenuEntry(Icons.Rounded.Close, stringResource(R.string.business_bot_remove), error = true) {
                                scope.launch { runCatching { graph.td.send(TdApi.RemoveBusinessConnectedBotFromChat(chatId)) } }
                            },
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
private fun PinnedBar(pins: List<UiMessage>, index: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val message = pins.getOrNull(index) ?: return
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            PinIndicator(pins.size, index)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (pins.size > 1) "${stringResource(R.string.chat_pinned)} ${pins.size - index}" else stringResource(R.string.chat_pinned),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (pins.size > 1) Text(" / ${pins.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
                val spatial = MaterialTheme.motionScheme.fastSpatialSpec<androidx.compose.ui.unit.IntOffset>()
                val effects = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
                androidx.compose.animation.AnimatedContent(
                    message.id,
                    transitionSpec = {
                        (androidx.compose.animation.slideInVertically(spatial) { it / 2 } + fadeIn(effects))
                            .togetherWith(androidx.compose.animation.slideOutVertically(spatial) { -it / 2 } + fadeOut(effects))
                    },
                    label = "pin",
                ) { id ->
                    val shown = pins.firstOrNull { it.id == id } ?: message
                    val text = shown.text.replace('\n', ' ').ifBlank { com.pixelgram.android.td.contentPreviewLabel(shown.content) }
                    com.pixelgram.android.ui.components.OverlayEmojiText(
                        androidx.compose.ui.text.AnnotatedString(text),
                        com.pixelgram.android.ui.components.customEmojiSlots(shown.entities, text),
                        style = MaterialTheme.typography.bodyMedium,
                        softWrap = false,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.fillMaxWidth().fadeEnd(),
                    )
                }
            }
            Icon(Icons.Rounded.PushPin, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PinIndicator(count: Int, index: Int) {
    val visible = minOf(count, 4)
    val gap = 2.dp
    val segment = (34.dp - gap * (visible - 1)) / visible
    val top = minOf(maxOf(index - visible + 1, 0) + 0, maxOf(count - visible, 0))
    val active = (index - top).coerceIn(0, visible - 1)
    Column(Modifier.width(3.dp).height(34.dp), verticalArrangement = Arrangement.spacedBy(gap)) {
        repeat(visible) { i ->
            val on = visible == 1 || i == visible - 1 - active
            val color by androidx.compose.animation.animateColorAsState(
                if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                MaterialTheme.motionScheme.fastEffectsSpec(),
                label = "pin-seg",
            )
            Box(Modifier.width(3.dp).height(segment).clip(RoundedCornerShape(2.dp)).background(color))
        }
    }
}

object FadeEnd {
    fun Modifier.fade(): Modifier = fadeEnd()
}

internal fun Modifier.fadeEnd(fade: androidx.compose.ui.unit.Dp = 24.dp): Modifier = this
    .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
    .then(Modifier.drawWithContent {
        drawContent()
        val w = fade.toPx().coerceAtMost(size.width)
        drawRect(
            androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = size.width - w, endX = size.width),
            topLeft = androidx.compose.ui.geometry.Offset(size.width - w, 0f),
            size = androidx.compose.ui.geometry.Size(w, size.height),
            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn,
        )
    })

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DateEntityDialog(onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val dateState = androidx.compose.material3.rememberDatePickerState(System.currentTimeMillis())
    val timeState = androidx.compose.material3.rememberTimePickerState(is24Hour = true)
    var step by remember { mutableStateOf(0) }
    if (step == 0) {
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = { androidx.compose.material3.TextButton(onClick = { step = 1 }) { Text(stringResource(R.string.action_next)) } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        ) { androidx.compose.material3.DatePicker(dateState) }
    } else {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onDismiss,
            text = { androidx.compose.material3.TimePicker(timeState) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val day = dateState.selectedDateMillis ?: System.currentTimeMillis()
                    val local = java.time.Instant.ofEpochMilli(day).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                    val at = local.atTime(timeState.hour, timeState.minute).atZone(java.time.ZoneId.systemDefault())
                    onPick(at.toEpochSecond().toInt())
                }) { Text(stringResource(R.string.action_done)) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private fun autoReplace(old: androidx.compose.ui.text.input.TextFieldValue, next: androidx.compose.ui.text.input.TextFieldValue): androidx.compose.ui.text.input.TextFieldValue {
    if (next.text.length != old.text.length + 1 || !next.selection.collapsed) return next
    val at = next.selection.start
    if (at < 2) return next
    val pair = next.text.substring(at - 2, at)
    val replacement = when (pair) {
        "--" -> "—"
        ">>" -> "»"
        "<<" -> "«"
        else -> return next
    }
    val text = next.text.substring(0, at - 2) + replacement + next.text.substring(at)
    return androidx.compose.ui.text.input.TextFieldValue(text, androidx.compose.ui.text.TextRange(at - 1))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWavyStrike(layout: androidx.compose.ui.text.TextLayoutResult, range: IntRange, color: Color) {
    val length = layout.layoutInput.text.length
    val from = range.first.coerceIn(0, length)
    val to = (range.last + 1).coerceIn(0, length)
    if (from >= to) return
    val amplitude = 1.6.dp.toPx()
    val wavelength = 7.dp.toPx()
    val fontPx = layout.layoutInput.style.fontSize.toPx()
    for (line in layout.getLineForOffset(from)..layout.getLineForOffset(to - 1)) {
        val charStart = maxOf(from, layout.getLineStart(line))
        val charEnd = minOf(to, layout.getLineEnd(line, visibleEnd = true))
        if (charStart >= charEnd) continue
        val left = layout.getBoundingBox(charStart).left
        val right = layout.getBoundingBox(charEnd - 1).right
        val y = layout.getLineBaseline(line) - fontPx * 0.3f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(left, y)
            var x = left
            while (x <= right) {
                lineTo(x, y + amplitude * kotlin.math.sin(2 * Math.PI * (x - left) / wavelength).toFloat())
                x += 1f
            }
        }
        drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

private val AutoLink = Regex("""(?:https?://|www\.|t\.me/)[^\s<>()]+|(?<![\w@])@[A-Za-z][A-Za-z0-9_]{3,31}""")

private fun withAutoLinks(text: String, entities: List<TdApi.TextEntity>): List<TdApi.TextEntity> {
    val covered = BooleanArray(text.length)
    entities.forEach { e -> for (i in e.offset.coerceAtLeast(0) until (e.offset + e.length).coerceAtMost(text.length)) covered[i] = true }
    val extra = AutoLink.findAll(text).mapNotNull { m ->
        var end = m.range.last + 1
        while (end > m.range.first && text[end - 1] in ".,!?:;\"'") end--
        val start = m.range.first
        if (end <= start || (start until end).any { covered[it] }) return@mapNotNull null
        val type: TdApi.TextEntityType = if (text[start] == '@') TdApi.TextEntityTypeMention() else TdApi.TextEntityTypeUrl()
        TdApi.TextEntity(start, end - start, type)
    }.toList()
    return if (extra.isEmpty()) entities else (entities + extra).sortedBy { it.offset }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChannelBar(graph: com.pixelgram.android.AppGraph, chatId: Long, linkedChatId: Long, onOpenChat: (Long) -> Unit) {
    val snapshot by graph.store.snapshot.collectAsState()
    val chat = remember(chatId, snapshot) { graph.store.chat(chatId) }
    val muted = (chat?.notificationSettings?.muteFor ?: 0) > 0
    val scope = rememberCoroutineScope()
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars).padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            androidx.compose.material3.FilledTonalButton(
                onClick = {
                    scope.launch {
                        runCatching {
                            graph.td.send(TdApi.SetChatNotificationSettings(chatId, TdApi.ChatNotificationSettings().apply {
                                useDefaultMuteFor = false
                                muteFor = if (muted) 0 else Int.MAX_VALUE
                                useDefaultSound = true
                                useDefaultShowPreview = true
                                useDefaultMuteStories = true
                                useDefaultShowStoryPoster = true
                                useDefaultDisablePinnedMessageNotifications = true
                                useDefaultDisableMentionNotifications = true
                            }))
                        }
                    }
                },
                shapes = androidx.compose.material3.ButtonDefaults.shapes(),
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Icon(if (muted) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (muted) R.string.chat_unmute else R.string.chat_mute), style = MaterialTheme.typography.titleMedium)
            }
            if (linkedChatId != 0L) {
                androidx.compose.material3.FilledTonalButton(
                    onClick = { onOpenChat(linkedChatId) },
                    shapes = androidx.compose.material3.ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Chat, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.channel_discussion), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

private fun Modifier.clampHeight(max: androidx.compose.ui.unit.Dp): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(maxHeight = androidx.compose.ui.unit.Constraints.Infinity))
    val height = minOf(placeable.height, max.roundToPx(), constraints.maxHeight)
    layout(placeable.width, height) { placeable.place(0, 0) }
}

private class MenuInfo(val read: Int = 0, val reactions: List<TdApi.AddedReaction> = emptyList(), val viewers: List<TdApi.MessageViewer> = emptyList(), val total: Int = 0)

@Composable
private fun rememberMenuInfo(chatId: Long, message: UiMessage): MenuInfo {
    val graph = com.pixelgram.android.LocalGraph.current
    val info by androidx.compose.runtime.produceState(MenuInfo(), chatId, message.id) {
        val chat = graph.store.chat(chatId)
        val private = chat?.type is TdApi.ChatTypePrivate
        val read = if (private && message.outgoing) (runCatching { graph.td.send(TdApi.GetMessageReadDate(chatId, message.id)) }.getOrNull() as? TdApi.MessageReadDateRead)?.readDate ?: 0 else 0
        val added = if (!private) runCatching { graph.td.send(TdApi.GetMessageAddedReactions(chatId, message.id, null, "", 50)) }.getOrNull() else null
        val viewers = if (!private && message.outgoing) runCatching { graph.td.send(TdApi.GetMessageViewers(chatId, message.id)).viewers.toList() }.getOrDefault(emptyList()) else emptyList()
        value = MenuInfo(read, added?.reactions?.toList().orEmpty(), viewers, added?.totalCount ?: 0)
    }
    return info
}

@Composable
private fun MenuHeader(chatId: Long, message: UiMessage, onDetails: () -> Unit) {
    val info = rememberMenuInfo(chatId, message)
    val graph = com.pixelgram.android.LocalGraph.current
    val snapshot by graph.store.snapshot.collectAsState()
    val reactionCount = maxOf(info.total, message.reactions.sumOf { it.count })
    val people = (info.reactions.mapNotNull { (it.senderId as? TdApi.MessageSenderUser)?.userId } + info.viewers.map { it.userId }).distinct()
    val label = when {
        info.read > 0 -> chatListTime(info.read) + " " + messageTime(info.read, true)
        reactionCount > 0 -> pluralStringResource(R.plurals.menu_reactions, reactionCount, reactionCount)
        info.viewers.isNotEmpty() -> pluralStringResource(R.plurals.menu_seen, info.viewers.size, info.viewers.size)
        else -> return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().clickable(enabled = people.isNotEmpty(), onClick = onDetails).padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(if (info.read > 0) Icons.Rounded.DoneAll else if (reactionCount > 0) Icons.Rounded.FavoriteBorder else Icons.Rounded.Visibility, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.weight(1f))
        Box {
            people.take(3).forEachIndexed { index, id ->
                val user = snapshot.users[id]
                Box(Modifier.offset(x = (index * 12).dp).size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer).padding(1.dp)) {
                    com.pixelgram.android.ui.components.Avatar(user?.name.orEmpty(), id, user?.photoFileId?.let { snapshot.files[it] }, user?.minithumb, 20.dp, onNeedFile = { user?.photoFileId?.let(graph.store::requestFileAsync) })
                }
            }
            Spacer(Modifier.size(width = if (people.isEmpty()) 0.dp else (22 + (people.take(3).size - 1) * 12).dp, height = 22.dp))
        }
    }
    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest, thickness = 6.dp)
}

@Composable
private fun MessageDetails(chatId: Long, message: UiMessage, onBack: () -> Unit) {
    val info = rememberMenuInfo(chatId, message)
    val graph = com.pixelgram.android.LocalGraph.current
    val snapshot by graph.store.snapshot.collectAsState()
    Column(Modifier.padding(bottom = 6.dp).heightIn(max = 360.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onBack).padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.action_back), style = MaterialTheme.typography.bodyMedium)
        }
        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest, thickness = 6.dp)
        val entries = info.reactions.mapNotNull { r -> (r.senderId as? TdApi.MessageSenderUser)?.userId?.let { Triple(it, r.date, (r.type as? TdApi.ReactionTypeEmoji)?.emoji) } } +
            info.viewers.filter { v -> info.reactions.none { (it.senderId as? TdApi.MessageSenderUser)?.userId == v.userId } }.map { Triple(it.userId, it.viewDate, null) }
        androidx.compose.foundation.lazy.LazyColumn {
            items(entries.size) { i ->
                val (id, date, emoji) = entries[i]
                val user = snapshot.users[id]
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    com.pixelgram.android.ui.components.Avatar(user?.name.orEmpty(), id, user?.photoFileId?.let { snapshot.files[it] }, user?.minithumb, 36.dp, onNeedFile = { user?.photoFileId?.let(graph.store::requestFileAsync) })
                    Column(Modifier.weight(1f)) {
                        Text(user?.name.orEmpty(), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (date > 0) Text(chatListTime(date) + " " + messageTime(date, false), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (emoji != null) Text(emoji, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

@Composable
internal fun ReactionGlyph(key: String, size: androidx.compose.ui.unit.Dp) {
    val custom = key.removePrefix("ce:").toLongOrNull()?.takeIf { key.startsWith("ce:") }
    if (custom != null) com.pixelgram.android.ui.components.CustomEmojiView(custom, "⭐", size = size)
    else Text(key, fontSize = with(androidx.compose.ui.platform.LocalDensity.current) { (size * 0.85f).toSp() })
}

object ComposerSlots {
    const val None = "none"
    const val Emoji = "emoji"
    const val Stickers = "stickers"
    const val Gifs = "gifs"
    const val Attach = "attach"
    const val Gallery = "gallery"
    const val File = "file"
    const val Location = "location"
    const val Camera = "camera"
    const val Send = "send"
    val All = listOf(None, Emoji, Stickers, Gifs, Attach, Gallery, File, Location, Camera, Send)

    class Set(val left: String, val right: String, val extra: String)

    fun of(prefs: com.pixelgram.android.settings.AppearanceSettings): Set {
        val telegram = prefs.composerStyle == "telegram"
        return Set(
            left = prefs.composerLeft.ifBlank { if (telegram) Emoji else Attach },
            right = prefs.composerRight.ifBlank { if (telegram) Attach else Emoji },
            extra = prefs.composerExtra.ifBlank { if (telegram) None else Gallery },
        )
    }

    fun icon(kind: String): androidx.compose.ui.graphics.vector.ImageVector = when (kind) {
        Emoji -> androidx.compose.material.icons.Icons.Rounded.EmojiEmotions
        Stickers -> androidx.compose.material.icons.Icons.Rounded.Interests
        Gifs -> androidx.compose.material.icons.Icons.Rounded.GifBox
        Attach -> androidx.compose.material.icons.Icons.Rounded.AttachFile
        Gallery -> androidx.compose.material.icons.Icons.Rounded.PhotoLibrary
        File -> androidx.compose.material.icons.Icons.AutoMirrored.Rounded.InsertDriveFile
        Location -> androidx.compose.material.icons.Icons.Rounded.LocationOn
        Camera -> androidx.compose.material.icons.Icons.Rounded.PhotoCamera
        Send -> androidx.compose.material.icons.Icons.AutoMirrored.Rounded.Send
        else -> androidx.compose.material.icons.Icons.Rounded.Block
    }

    fun label(kind: String): Int = when (kind) {
        Emoji -> R.string.slot_emoji
        Stickers -> R.string.slot_stickers
        Gifs -> R.string.slot_gifs
        Attach -> R.string.slot_attach
        Gallery -> R.string.slot_gallery
        File -> R.string.slot_file
        Location -> R.string.slot_location
        Camera -> R.string.slot_camera
        Send -> R.string.slot_send
        else -> R.string.slot_none
    }
}

@Composable
private fun PinDialog(graph: com.pixelgram.android.AppGraph, chatId: Long, onDismiss: () -> Unit, onPin: (Boolean, Boolean) -> Unit) {
    val chat = remember(chatId) { graph.store.chat(chatId) }
    val meId = graph.store.snapshot.value.meId
    val type = chat?.type
    val saved = chatId == meId
    val privateName = (type as? TdApi.ChatTypePrivate)?.takeIf { !saved }?.let { chat.title }
    val channel = (type as? TdApi.ChatTypeSupergroup)?.isChannel == true
    var checked by remember { mutableStateOf(true) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.PushPin, null) },
        title = { Text(stringResource(R.string.pin_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(when {
                    saved || privateName != null -> R.string.pin_text_chat
                    channel -> R.string.pin_text_channel
                    else -> R.string.pin_text_group
                }))
                if (!saved) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { checked = !checked }) {
                        androidx.compose.material3.Checkbox(checked, { checked = it })
                        Text(if (privateName != null) stringResource(R.string.pin_also_for, privateName) else stringResource(R.string.pin_notify))
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                if (privateName != null) onPin(false, !checked) else onPin(!saved && checked, false)
            }) { Text(stringResource(R.string.action_pin)) }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

