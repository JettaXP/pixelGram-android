package com.pixelgram.android.feature.chat

import java.io.File
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
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
fun ChatScreen(graph: AppGraph, chatId: Long, navigator: Navigator) {
    val controller = remember(chatId) { ChatController(graph, chatId) }
    DisposableEffect(controller) { onDispose { controller.close() } }
    val ui by controller.state.collectAsState()
    val snapshot by graph.store.snapshot.collectAsState()
    val chat = remember(chatId, snapshot) { graph.store.chat(chatId) }
    val privateUserId = (chat?.type as? org.drinkless.tdlib.TdApi.ChatTypePrivate)?.userId ?: 0L
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
                        modifier = Modifier.clickable { navigator.open(ProfileKey(privateUserId, chatId)) },
                    ) {
                        Avatar(
                            title = ui.title,
                            seed = chatId,
                            path = chat?.photo?.small?.id?.let { snapshot.files[it] },
                            minithumb = chat?.photo?.minithumbnail?.data,
                            size = 40.dp,
                            onNeedFile = { chat?.photo?.small?.id?.let(graph.store::requestFileAsync) },
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(ui.title, style = MaterialTheme.typography.titleMediumEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            subtitle(snapshot, privateUserId)?.let {
                                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navigator::back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
                },
            )
        },
        bottomBar = { Composer(ui.replyTo, controller) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
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
                                MessageBubble(
                                    message = message,
                                    joinsAbove = joinsAbove,
                                    joinsBelow = joinsBelow,
                                    filePath = snapshot.files[message.mediaFileId],
                                    onNeedFile = { graph.store.requestFileAsync(message.mediaFileId) },
                                    reply = message.replyToId.takeIf { it != 0L }?.let { id -> ui.messages.firstOrNull { it.id == id } },
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
}

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
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Text(dayHeader(date), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }
    }
}

@Composable
private fun ServiceMessage(message: UiMessage) {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainer) {
            Text(
                stringResource(R.string.preview_service),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SwipeToReply(onReply: () -> Unit, content: @Composable () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val limit = with(density) { -140.dp.toPx() }
    val threshold = with(density) { -90.dp.toPx() }
    var raw by remember { mutableFloatStateOf(0f) }
    val offset by animateFloatAsState(
        if (raw > threshold) raw * 0.3f else threshold * 0.3f + (raw - threshold) * 0.5f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "swipe-reply",
    )
    Box(
        Modifier.draggable(
            state = rememberDraggableState { delta ->
                val old = raw
                raw = (raw + delta).coerceIn(limit, 0f)
                if ((old > threshold) != (raw > threshold)) haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
            },
            orientation = Orientation.Horizontal,
            onDragStopped = {
                if (raw <= threshold) {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onReply()
                }
                raw = 0f
            },
        ),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(Modifier.offset { IntOffset(offset.roundToInt(), 0) }) { content() }
        if (raw < 0f) {
            Box(Modifier.padding(end = 16.dp).size(40.dp), contentAlignment = Alignment.Center) {
                CircularWavyProgressIndicator(progress = { (raw / threshold).coerceIn(0f, 1f) }, modifier = Modifier.size(40.dp))
                Icon(Icons.AutoMirrored.Rounded.Reply, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: UiMessage,
    joinsAbove: Boolean,
    joinsBelow: Boolean,
    filePath: String?,
    onNeedFile: () -> Unit,
    reply: UiMessage?,
) {
    val big = 20.dp
    val small = 6.dp
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
    ) {
        Surface(shape = shape, color = container, contentColor = onContainer, modifier = Modifier.widthIn(max = 320.dp)) {
            Column(Modifier.padding(4.dp)) {
                if (reply != null) ReplyQuote(reply)
                if (message.mediaFileId != 0 && message.mediaWidth > 0) {
                    if (filePath == null) LaunchedEffect(message.mediaFileId) { onNeedFile() }
                    val ratio = (message.mediaWidth.toFloat() / message.mediaHeight.coerceAtLeast(1)).coerceIn(0.5f, 2f)
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(filePath?.let(::File) ?: message.mediaThumb).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .widthIn(max = 312.dp)
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .then(Modifier.height((312 / ratio).dp))
                            .clip(RoundedCornerShape(16.dp)),
                    )
                }
                (message.content as? TdApi.MessageDocument)?.let { DocumentRow(it.document, onContainer) }
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (message.text.isNotBlank()) {
                        Text(message.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f, fill = false))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (message.edited) Text(stringResource(R.string.message_edited), style = MaterialTheme.typography.labelSmall, color = onContainer.copy(alpha = 0.7f))
                        Text(messageTime(message.date), style = MaterialTheme.typography.labelSmall, color = onContainer.copy(alpha = 0.7f))
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
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DocumentRow(document: TdApi.Document, tint: Color) {
    val graph = com.pixelgram.android.LocalGraph.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backup = com.pixelgram.android.settings.Backup.isBackup(document.fileName)
    var busy by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf<File?>(null) }
    Row(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !busy) {
                scope.launch {
                    busy = true
                    val path = runCatching { graph.td.send(TdApi.DownloadFile(document.document.id, 32, 0, 0, true)).local.path }.getOrNull()
                    busy = false
                    if (backup && !path.isNullOrEmpty()) importing = File(path)
                }
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            if (busy) LoadingIndicator(Modifier.size(32.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Icon(if (backup) Icons.Rounded.Settings else Icons.AutoMirrored.Rounded.InsertDriveFile, null, tint = MaterialTheme.colorScheme.onPrimary)
        }
        Column(Modifier.widthIn(max = 220.dp)) {
            Text(document.fileName.ifBlank { "File" }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                android.text.format.Formatter.formatShortFileSize(context, maxOf(document.document.size, document.document.expectedSize)),
                style = MaterialTheme.typography.labelMedium,
                color = tint.copy(alpha = 0.7f),
            )
        }
    }
    importing?.let { file ->
        com.pixelgram.android.ui.components.BackupImportDialog(file) { result ->
            importing = null
            if (result != null) android.widget.Toast.makeText(context, result, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun ReplyQuote(reply: UiMessage) {
    Row(
        Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(reply.text.ifBlank { "…" }, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Composer(replyTo: UiMessage?, controller: ChatController) {
    var text by rememberSaveable { mutableStateOf("") }
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
            AnimatedVisibility(replyTo != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Rounded.Reply, null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        replyTo?.text.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    )
                    IconButton(onClick = { controller.reply(null) }) { Icon(Icons.Rounded.Close, stringResource(R.string.action_cancel)) }
                }
            }
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalIconButton(
                    onClick = {
                        picker.launch(
                            androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, stringResource(R.string.chat_attach_photo))
                }
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.chat_message_hint)) },
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                    maxLines = 6,
                    modifier = Modifier.weight(1f),
                )
                FilledIconButton(
                    onClick = {
                        controller.send(text)
                        text = ""
                    },
                    enabled = text.isNotBlank(),
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.action_send))
                }
            }
        }
    }
}
