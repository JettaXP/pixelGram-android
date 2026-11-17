package com.pixelgram.android.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Close
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pixelgram.android.LocalGraph
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.CustomEmojiView
import com.pixelgram.android.ui.components.EmojiStickers
import com.pixelgram.android.ui.components.Icon
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

object MediaPreview {
    var sticker by mutableStateOf<TdApi.Sticker?>(null)
    var gif by mutableStateOf<TdApi.Animation?>(null)
    var scheduling by mutableStateOf<((TdApi.MessageSchedulingState) -> Unit)?>(null)
    var captioning by mutableStateOf<TdApi.Animation?>(null)
}

object EmojiPreview {
    var request by mutableStateOf<Pair<Long, String>?>(null)
    var pack by mutableStateOf<Long?>(null)
    var client: com.pixelgram.android.td.TdClient? = null

    fun openPack(id: Long) {
        val td = client ?: return
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            val sticker = EmojiStickers.load(td, id)
            if (sticker != null && sticker.setId != 0L) pack = sticker.setId
        }
    }
}

@Composable
fun EmojiPreviewHost() {
    val graph = LocalGraph.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    EmojiPreview.client = graph.td
    EmojiPreview.request?.let { (id, alt) ->
        val close = { EmojiPreview.request = null }
        Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(interactionSource = null, indication = null, onClick = close), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp), modifier = Modifier.popIn()) {
                    CustomEmojiView(id, alt, size = 200.dp)
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 6.dp, modifier = Modifier.width(240.dp)) {
                        Column(Modifier.padding(vertical = 6.dp)) {
                            PreviewAction(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.emoji_send)) {
                                val chat = com.pixelgram.android.notify.AppVisibility.openChat
                                if (chat != 0L) scope.launch {
                                    runCatching {
                                        val premium = graph.store.snapshot.value.let { it.users[it.meId]?.premium } == true
                                        val entities = com.pixelgram.android.td.LocalPremiumEmoji.outgoing(arrayOf(TdApi.TextEntity(0, alt.length, TdApi.TextEntityTypeCustomEmoji(id))), premium)
                                        graph.td.send(TdApi.SendMessage().apply {
                                            chatId = chat
                                            inputMessageContent = TdApi.InputMessageText().apply { text = TdApi.FormattedText(alt, entities) }
                                        })
                                    }
                                }
                                close()
                            }
                            PreviewAction(Icons.Rounded.ContentCopy, stringResource(R.string.emoji_copy)) {
                                clipboard.setText(AnnotatedString(alt))
                                close()
                            }
                            PreviewAction(Icons.Rounded.GridView, stringResource(R.string.emoji_view_set)) {
                                scope.launch {
                                    val sticker = EmojiStickers.load(graph.td, id)
                                    if (sticker != null && sticker.setId != 0L) EmojiPreview.pack = sticker.setId
                                }
                                close()
                            }
                        }
                    }
                }
            }
        }
    }
    EmojiPreview.pack?.let { setId -> EmojiSetSheet(setId) { EmojiPreview.pack = null } }
    val sticker = MediaPreview.sticker
    val gif = MediaPreview.gif
    if (sticker != null || gif != null) {
        val close = {
            MediaPreview.sticker = null
            MediaPreview.gif = null
        }
        val snapshot by graph.store.snapshot.collectAsState()
        Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(interactionSource = null, indication = null, onClick = close), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp), modifier = Modifier.popIn()) {
                    if (sticker != null) StickerView(sticker, snapshot.files, { graph.store.requestFileAsync(it) }, Modifier.size(240.dp))
                    if (gif != null) {
                        val path = snapshot.files[gif.animation.id]
                        if (path == null) androidx.compose.runtime.LaunchedEffect(gif.animation.id) { graph.store.requestFileAsync(gif.animation.id) }
                        Box(Modifier.width(280.dp).aspectRatio(if (gif.height > 0) gif.width.toFloat() / gif.height else 1f).clip(RoundedCornerShape(16.dp))) {
                            if (path != null) VideoPlayer(path, muted = true, loop = true, controls = false, modifier = Modifier.fillMaxSize())
                            else coil.compose.AsyncImage(gif.minithumbnail?.data, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, shadowElevation = 6.dp, modifier = Modifier.width(260.dp)) {
                        Column(Modifier.padding(vertical = 6.dp)) {
                            val content = { if (sticker != null) stickerInput(sticker) else gifInput(gif!!) }
                            PreviewAction(Icons.AutoMirrored.Rounded.Send, stringResource(if (sticker != null) R.string.sticker_send else R.string.gif_send)) {
                                sendMedia(graph, content(), null)
                                close()
                            }
                            PreviewAction(Icons.Rounded.NotificationsOff, stringResource(R.string.send_silent)) {
                                sendMedia(graph, content(), TdApi.MessageSendOptions().apply { disableNotification = true })
                                close()
                            }
                            PreviewAction(Icons.Rounded.CalendarMonth, stringResource(R.string.media_schedule)) {
                                val input = content()
                                MediaPreview.scheduling = { state -> sendMedia(graph, input, TdApi.MessageSendOptions().apply { schedulingState = state }) }
                                close()
                            }
                            if (sticker != null) {
                                val faved by androidx.compose.runtime.produceState(false, sticker.sticker.id) {
                                    value = runCatching { graph.td.send(TdApi.GetFavoriteStickers()).stickers.any { it.sticker.id == sticker.sticker.id } }.getOrDefault(false)
                                }
                                PreviewAction(if (faved) Icons.Rounded.StarOutline else Icons.Rounded.Star, stringResource(if (faved) R.string.sticker_unfave else R.string.sticker_fave)) {
                                    scope.launch {
                                        val file = TdApi.InputFileId(sticker.sticker.id)
                                        runCatching { graph.td.send(if (faved) TdApi.RemoveFavoriteSticker(file) else TdApi.AddFavoriteSticker(file)) }
                                    }
                                    close()
                                }
                                if (sticker.setId != 0L) PreviewAction(Icons.Rounded.GridView, stringResource(R.string.sticker_view_set)) {
                                    EmojiPreview.pack = sticker.setId
                                    close()
                                }
                            }
                            if (gif != null) {
                                PreviewAction(Icons.Rounded.PlaylistAdd, stringResource(R.string.gif_add_caption)) {
                                    MediaPreview.captioning = gif
                                    close()
                                }
                                PreviewAction(Icons.Rounded.Delete, stringResource(R.string.action_delete), MaterialTheme.colorScheme.error) {
                                    scope.launch { runCatching { graph.td.send(TdApi.RemoveSavedAnimation(TdApi.InputFileId(gif.animation.id))) } }
                                    close()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    MediaPreview.scheduling?.let { action ->
        val chat = com.pixelgram.android.notify.AppVisibility.openChat
        ScheduleSheet(reminder = chat == graph.store.snapshot.value.meId, onlineName = null, onDismiss = { MediaPreview.scheduling = null }) { state ->
            MediaPreview.scheduling = null
            action(state)
        }
    }
    MediaPreview.captioning?.let { animation ->
        var caption by androidx.compose.runtime.remember(animation.animation.id) { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { MediaPreview.captioning = null },
            title = { Text(stringResource(R.string.gif_add_caption)) },
            text = { androidx.compose.material3.OutlinedTextField(caption, { caption = it }, placeholder = { Text(stringResource(R.string.send_caption_hint)) }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val input = gifInput(animation) as TdApi.InputMessageAnimation
                    input.caption = TdApi.FormattedText(caption.trim(), emptyArray())
                    sendMedia(graph, input, null)
                    MediaPreview.captioning = null
                }) { Text(stringResource(R.string.action_send)) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { MediaPreview.captioning = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private fun sendMedia(graph: com.pixelgram.android.AppGraph, content: TdApi.InputMessageContent, options: TdApi.MessageSendOptions?) {
    val chat = com.pixelgram.android.notify.AppVisibility.openChat
    if (chat == 0L) return
    kotlinx.coroutines.MainScope().launch {
        runCatching {
            graph.td.send(TdApi.SendMessage().apply {
                chatId = chat
                this.options = options
                inputMessageContent = content
            })
        }
    }
}

@Composable
private fun Modifier.popIn(): Modifier {
    val progress = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) { progress.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.72f, stiffness = 380f)) }
    return graphicsLayer {
        val p = progress.value
        scaleX = 0.55f + 0.45f * p
        scaleY = 0.55f + 0.45f * p
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * 80f
    }
}

@Composable
private fun PreviewAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color = Color.Unspecified, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint)
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint)
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun EmojiSetSheet(setId: Long, onDismiss: () -> Unit) {
    val graph = LocalGraph.current
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboard = LocalClipboardManager.current
    var set by androidx.compose.runtime.remember(setId) { mutableStateOf<TdApi.StickerSet?>(null) }
    var busy by androidx.compose.runtime.remember { mutableStateOf(false) }
    var menu by androidx.compose.runtime.remember { mutableStateOf(false) }
    var ownerCopied by androidx.compose.runtime.remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(setId) { set = runCatching { graph.td.send(TdApi.GetStickerSet(setId)) }.getOrNull() }
    androidx.compose.runtime.LaunchedEffect(menu) { if (!menu) ownerCopied = false }
    val loaded = set
    val emoji = loaded?.stickerType is TdApi.StickerTypeCustomEmoji
    val link = loaded?.let { "https://t.me/${if (emoji) "addemoji" else "addstickers"}/${it.name}" }
    val snapshot by graph.store.snapshot.collectAsState()
    val sheet = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    loaded?.title ?: stringResource(R.string.pack_title),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                Box {
                    androidx.compose.material3.IconButton(onClick = { menu = true }, enabled = link != null) {
                        Icon(Icons.Rounded.MoreVert, stringResource(R.string.action_more))
                    }
                    androidx.compose.material3.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(16.dp)) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(stringResource(R.string.pack_share)) },
                            leadingIcon = { Icon(Icons.Rounded.Share, null) },
                            onClick = {
                                menu = false
                                link?.let { url ->
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, url)
                                    context.startActivity(android.content.Intent.createChooser(intent, null))
                                }
                            },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(stringResource(R.string.pack_copy_link)) },
                            leadingIcon = { Icon(Icons.Rounded.Link, null) },
                            onClick = {
                                menu = false
                                link?.let { clipboard.setText(AnnotatedString(it)) }
                            },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(stringResource(R.string.pack_refresh)) },
                            leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                            enabled = loaded != null,
                            onClick = {
                                menu = false
                                val name = loaded?.name ?: return@DropdownMenuItem
                                scope.launch { runCatching { graph.td.send(TdApi.SearchStickerSet(name, true)) }.getOrNull()?.let { set = it } }
                            },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(stringResource(if (ownerCopied) R.string.pack_owner_copied else R.string.pack_owner)) },
                            leadingIcon = { Icon(Icons.Rounded.Person, null) },
                            enabled = loaded != null && !ownerCopied,
                            onClick = {
                                val owner = loaded?.let { packOwner(it.id) } ?: return@DropdownMenuItem
                                scope.launch {
                                    val user = runCatching { graph.td.send(TdApi.GetUser(owner)) }.getOrNull()
                                    if (user != null) {
                                        menu = false
                                        onDismiss()
                                        com.pixelgram.android.nav.PendingNav.profile = owner
                                    } else {
                                        clipboard.setText(AnnotatedString(owner.toString()))
                                        ownerCopied = true
                                    }
                                }
                            },
                        )
                    }
                }
                androidx.compose.material3.IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, stringResource(R.string.action_close)) }
            }
            if (loaded == null) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { androidx.compose.material3.LoadingIndicator() }
            } else {
                LazyVerticalGrid(
                    GridCells.Adaptive(if (emoji) 44.dp else 72.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                ) {
                    val chat = com.pixelgram.android.notify.AppVisibility.openChat
                    val sendable = !emoji && chat != 0L
                    items(loaded.stickers.toList(), key = { it.sticker.id }) { sticker ->
                        val id = (sticker.fullType as? TdApi.StickerFullTypeCustomEmoji)?.customEmojiId ?: 0L
                        Box(
                            Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).then(
                                if (sendable) Modifier.combinedClickable(onLongClick = { MediaPreview.sticker = sticker }) {
                                    scope.launch { runCatching { graph.td.send(TdApi.SendMessage().apply { chatId = chat; inputMessageContent = stickerInput(sticker) }) } }
                                    onDismiss()
                                } else Modifier,
                            ).padding(3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (id != 0L) CustomEmojiView(id, sticker.emoji, Modifier.fillMaxSize())
                            else StickerView(sticker, snapshot.files, { graph.store.requestFileAsync(it) }, Modifier.fillMaxSize(), video = false)
                        }
                    }
                }
                val installed = loaded.isInstalled && !loaded.isArchived
                Button(
                    onClick = {
                        busy = true
                        scope.launch {
                            runCatching { graph.td.send(TdApi.ChangeStickerSet(setId, !installed, false)) }
                            set = runCatching { graph.td.send(TdApi.GetStickerSet(setId)) }.getOrNull() ?: set
                            busy = false
                        }
                    },
                    enabled = !busy,
                    colors = if (installed) androidx.compose.material3.ButtonDefaults.filledTonalButtonColors() else androidx.compose.material3.ButtonDefaults.buttonColors(),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).height(52.dp),
                ) {
                    val count = loaded.stickers.size
                    Text(stringResource(if (emoji) (if (installed) R.string.emoji_set_remove else R.string.emoji_set_add) else (if (installed) R.string.pack_stickers_remove else R.string.pack_stickers_add), count))
                }
            }
        }
    }
}

private fun packOwner(setId: Long): Long {
    val high = setId ushr 32
    return if ((setId ushr 24) and 0xFF != 0L) high + 0x100000000L else high
}
