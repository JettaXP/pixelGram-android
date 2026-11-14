package com.pixelgram.android.feature.pixel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelgram.android.AppGraph
import com.pixelgram.android.LocalGraph
import com.pixelgram.android.R
import com.pixelgram.android.feature.chat.EmojiTab
import com.pixelgram.android.pixel.CustomBadge
import com.pixelgram.android.ui.components.CustomEmojiView
import com.pixelgram.android.ui.components.Icon
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

private const val BadgeBot = "pixelGramCI_bot"
private const val MaxText = 64

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BadgeEditorSheet(userId: Long, current: CustomBadge?, onDismiss: () -> Unit) {
    val graph = LocalGraph.current
    var picked by remember { mutableStateOf(current) }
    var text by remember { mutableStateOf(current?.text.orEmpty()) }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.badge_editor_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(56.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                    val badge = picked
                    when {
                        badge == null -> Unit
                        badge.customEmojiId != 0L -> CustomEmojiView(badge.customEmojiId, badge.emoji, size = 36.dp)
                        else -> Text(badge.emoji, style = MaterialTheme.typography.headlineMedium)
                    }
                }
                TextField(
                    value = text,
                    onValueChange = { text = it.take(MaxText) },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.badge_editor_caption)) },
                    supportingText = { Text("${text.length}/$MaxText") },
                    shape = RoundedCornerShape(20.dp),
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                    modifier = Modifier.weight(1f),
                )
            }
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth().height(320.dp)) {
                EmojiTab(
                    onEmoji = { picked = CustomBadge(it, 0L, text) },
                    onCustomEmoji = { id, alt -> picked = CustomBadge(alt.ifBlank { "⭐" }, id, text) },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                OutlinedButton(
                    onClick = {
                        graph.remote.setOwn(userId, null)
                        sendBadge(graph, null)
                        onDismiss()
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.action_reset), modifier = Modifier.padding(start = 6.dp))
                }
                Button(
                    onClick = {
                        picked?.let { badge ->
                            val result = badge.copy(text = text.trim())
                            graph.remote.setOwn(userId, result)
                            sendBadge(graph, result)
                        }
                        onDismiss()
                    },
                    enabled = picked != null,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}

private fun sendBadge(graph: AppGraph, badge: CustomBadge?) {
    graph.scope.launch {
        runCatching {
            val chat = graph.td.send(TdApi.SearchPublicChat(BadgeBot))
            val command = if (badge == null) "/badge" else "/badge ${badge.customEmojiId} ${badge.emoji} ${badge.text}".trimEnd()
            graph.td.send(TdApi.SendMessage().apply {
                chatId = chat.id
                inputMessageContent = TdApi.InputMessageText().apply { text = TdApi.FormattedText(command, emptyArray()) }
            })
            graph.td.send(TdApi.SetChatNotificationSettings(chat.id, TdApi.ChatNotificationSettings().apply {
                useDefaultMuteFor = false
                muteFor = Int.MAX_VALUE
                useDefaultSound = true
                useDefaultShowPreview = true
                useDefaultMuteStories = true
                useDefaultShowStoryPoster = true
                useDefaultDisablePinnedMessageNotifications = true
                useDefaultDisableMentionNotifications = true
            }))
        }
    }
}
