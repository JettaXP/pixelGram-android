package com.pixelgram.android.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

object EmojiPreview {
    var request by mutableStateOf<Pair<Long, String>?>(null)
    var pack by mutableStateOf<Long?>(null)
}

@Composable
fun EmojiPreviewHost() {
    val graph = LocalGraph.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    EmojiPreview.request?.let { (id, alt) ->
        val close = { EmojiPreview.request = null }
        Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(interactionSource = null, indication = null, onClick = close), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
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
}

@Composable
private fun PreviewAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmojiSetSheet(setId: Long, onDismiss: () -> Unit) {
    val graph = LocalGraph.current
    val scope = rememberCoroutineScope()
    val set by produceState<TdApi.StickerSet?>(null, setId) { value = runCatching { graph.td.send(TdApi.GetStickerSet(setId)) }.getOrNull() }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        val current = set
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp)) {
            Text(current?.title.orEmpty(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
            LazyVerticalGrid(GridCells.Fixed(8), Modifier.fillMaxWidth().height(320.dp)) {
                items(current?.stickers.orEmpty().toList(), key = { it.id }) { sticker ->
                    val id = (sticker.fullType as? TdApi.StickerFullTypeCustomEmoji)?.customEmojiId ?: 0L
                    Box(Modifier.aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                        if (id != 0L) CustomEmojiView(id, sticker.emoji, Modifier.fillMaxSize())
                        else StickerView(sticker, graph.store.snapshot.value.files, { graph.store.requestFileAsync(it) }, Modifier.fillMaxSize())
                    }
                }
            }
            if (current != null) {
                Button(
                    onClick = {
                        scope.launch { runCatching { graph.td.send(TdApi.ChangeStickerSet(setId, !current.isInstalled, false)) } }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                ) { Text(stringResource(if (current.isInstalled) R.string.emoji_set_remove else R.string.emoji_set_add, current.stickers.size)) }
            }
        }
    }
}
