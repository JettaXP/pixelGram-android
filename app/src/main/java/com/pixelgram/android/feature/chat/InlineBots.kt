package com.pixelgram.android.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pixelgram.android.LocalGraph
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.ui.components.Icon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.io.File

private val InlineToken = Regex("^@([A-Za-z0-9_]{3,32}) ([\\s\\S]*)$")
private val MentionToken = Regex("(?:^|\\s)@([A-Za-z0-9_]*)$")

class InlineBot(val user: TdApi.User, val query: String)

@Composable
fun rememberInlineBot(text: String): InlineBot? {
    val graph = LocalGraph.current
    val match = InlineToken.find(text)
    val name = match?.groupValues?.get(1)
    val bot by produceState<TdApi.User?>(null, name) {
        value = null
        if (name == null) return@produceState
        val chat = runCatching { graph.td.send(TdApi.SearchPublicChat(name)) }.getOrNull() ?: return@produceState
        val id = (chat.type as? TdApi.ChatTypePrivate)?.userId ?: return@produceState
        value = runCatching { graph.td.send(TdApi.GetUser(id)) }.getOrNull()?.takeIf { (it.type as? TdApi.UserTypeBot)?.isInline == true }
    }
    val user = bot ?: return null
    return InlineBot(user, match?.groupValues?.get(2).orEmpty())
}

@Composable
fun ComposerSuggestions(
    chatId: Long,
    text: String,
    cursor: Int,
    inline: InlineBot?,
    onInsert: (String) -> Unit,
    onSendResult: (Long, String) -> Unit,
) {
    if (inline != null) {
        InlineResults(chatId, inline, onSendResult)
        return
    }
    val before = text.substring(0, cursor.coerceIn(0, text.length))
    val mention = MentionToken.find(before)?.groupValues?.get(1) ?: return
    MentionList(chatId, mention) { username -> onInsert(username) }
}

@Composable
private fun MentionList(chatId: Long, query: String, onPick: (String) -> Unit) {
    val graph = LocalGraph.current
    val snapshot by graph.store.snapshot.collectAsState()
    val chat = remember(chatId) { graph.store.chat(chatId) }
    val group = chat?.type is TdApi.ChatTypeBasicGroup || (chat?.type as? TdApi.ChatTypeSupergroup)?.isChannel == false
    val users by produceState(emptyList<TdApi.User>(), chatId, query) {
        delay(120)
        val bots = runCatching { graph.td.send(TdApi.GetTopChats(TdApi.TopChatCategoryInlineBots(), 20)).chatIds.toList() }.getOrDefault(emptyList())
            .mapNotNull { id -> runCatching { graph.td.send(TdApi.GetUser(id)) }.getOrNull() }
        val members = if (group) runCatching { graph.td.send(TdApi.SearchChatMembers(chatId, query, 50, null)).members.toList() }.getOrDefault(emptyList())
            .mapNotNull { (it.memberId as? TdApi.MessageSenderUser)?.userId }
            .mapNotNull { id -> runCatching { graph.td.send(TdApi.GetUser(id)) }.getOrNull() } else emptyList()
        val q = query.lowercase()
        val matchedBots = bots.filter { u -> q.isEmpty() || u.usernames?.activeUsernames?.any { it.lowercase().startsWith(q) } == true || "${u.firstName} ${u.lastName}".lowercase().contains(q) }
        value = (matchedBots + members.filter { it.usernames?.activeUsernames?.isNotEmpty() == true }).distinctBy { it.id }.take(30)
    }
    if (users.isEmpty()) return
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 6.dp, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        LazyColumn(Modifier.heightIn(max = 280.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
            items(users, key = { it.id }) { user ->
                val username = user.usernames?.activeUsernames?.firstOrNull().orEmpty()
                val info = snapshot.users[user.id]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onPick(username) }.padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Avatar("${user.firstName} ${user.lastName}".trim(), user.id, info?.photoFileId?.let { snapshot.files[it] }, info?.minithumb, 36.dp, onNeedFile = { info?.photoFileId?.let(graph.store::requestFileAsync) })
                    Text("${user.firstName} ${user.lastName}".trim(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Text("@$username", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun InlineResults(chatId: Long, bot: InlineBot, onSend: (Long, String) -> Unit) {
    val graph = LocalGraph.current
    var results by remember(bot.user.id) { mutableStateOf<TdApi.InlineQueryResults?>(null) }
    LaunchedEffect(bot.user.id, bot.query) {
        delay(300)
        results = runCatching { graph.td.send(TdApi.GetInlineQueryResults(bot.user.id, chatId, null, bot.query, "")) }.getOrNull()
    }
    val current = results ?: return
    if (current.results.isEmpty() && current.button == null) return
    val visual = current.results.isNotEmpty() && current.results.all { it is TdApi.InlineQueryResultPhoto || it is TdApi.InlineQueryResultAnimation || it is TdApi.InlineQueryResultSticker }
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 6.dp, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Column {
            current.button?.let { button ->
                Text(
                    button.text,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().clickable {
                        (button.type as? TdApi.InlineQueryResultsButtonTypeWebApp)?.let { web ->
                            kotlinx.coroutines.MainScope().launchSafe { openWebApp(graph, chatId, bot.user.id, web.url, button.text) }
                        }
                    }.background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            if (visual) {
                LazyVerticalGrid(GridCells.Fixed(3), Modifier.heightIn(max = 340.dp), contentPadding = PaddingValues(4.dp)) {
                    items(current.results.toList(), key = { id(it) }) { result ->
                        Box(Modifier.padding(2.dp).aspectRatio(1f).clip(RoundedCornerShape(8.dp)).clickable { onSend(current.inlineQueryId, id(result)) }) {
                            ResultThumb(result, Modifier.fillMaxSize())
                        }
                    }
                }
            } else {
                LazyColumn(Modifier.heightIn(max = 340.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                    items(current.results.toList(), key = { id(it) }) { result ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().clickable { onSend(current.inlineQueryId, id(result)) }.padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                ResultThumb(result, Modifier.fillMaxSize())
                            }
                            Column(Modifier.weight(1f)) {
                                val (title, sub) = texts(result)
                                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun kotlinx.coroutines.CoroutineScope.launchSafe(block: suspend () -> Unit) { launch { runCatching { block() } } }

private fun id(result: TdApi.InlineQueryResult): String = when (result) {
    is TdApi.InlineQueryResultArticle -> result.id
    is TdApi.InlineQueryResultPhoto -> result.id
    is TdApi.InlineQueryResultAnimation -> result.id
    is TdApi.InlineQueryResultSticker -> result.id
    is TdApi.InlineQueryResultAudio -> result.id
    is TdApi.InlineQueryResultVoiceNote -> result.id
    is TdApi.InlineQueryResultDocument -> result.id
    is TdApi.InlineQueryResultVideo -> result.id
    is TdApi.InlineQueryResultContact -> result.id
    is TdApi.InlineQueryResultLocation -> result.id
    is TdApi.InlineQueryResultVenue -> result.id
    is TdApi.InlineQueryResultGame -> result.id
    else -> result.hashCode().toString()
}

private fun texts(result: TdApi.InlineQueryResult): Pair<String, String> = when (result) {
    is TdApi.InlineQueryResultArticle -> result.title to result.description
    is TdApi.InlineQueryResultAudio -> result.audio.title.ifBlank { result.audio.fileName } to result.audio.performer
    is TdApi.InlineQueryResultVoiceNote -> result.title to ""
    is TdApi.InlineQueryResultDocument -> result.title to result.description
    is TdApi.InlineQueryResultVideo -> result.title to result.description
    is TdApi.InlineQueryResultContact -> "${result.contact.firstName} ${result.contact.lastName}".trim() to result.contact.phoneNumber
    is TdApi.InlineQueryResultLocation -> result.title to ""
    is TdApi.InlineQueryResultVenue -> result.venue.title to result.venue.address
    is TdApi.InlineQueryResultGame -> result.game.title to result.game.description
    is TdApi.InlineQueryResultPhoto -> result.title to result.description
    is TdApi.InlineQueryResultAnimation -> result.title to ""
    else -> "" to ""
}

@Composable
private fun ResultThumb(result: TdApi.InlineQueryResult, modifier: Modifier) {
    val graph = LocalGraph.current
    val snapshot by graph.store.snapshot.collectAsState()
    val file: TdApi.File? = when (result) {
        is TdApi.InlineQueryResultArticle -> result.thumbnail?.file
        is TdApi.InlineQueryResultPhoto -> result.photo.sizes.firstOrNull { it.width >= 160 }?.photo ?: result.photo.sizes.lastOrNull()?.photo
        is TdApi.InlineQueryResultAnimation -> result.animation.thumbnail?.takeIf { it.format is TdApi.ThumbnailFormatJpeg }?.file
        is TdApi.InlineQueryResultVideo -> result.video.thumbnail?.file
        is TdApi.InlineQueryResultDocument -> result.document.thumbnail?.file
        else -> null
    }
    if (result is TdApi.InlineQueryResultSticker) {
        StickerView(result.sticker, snapshot.files, { graph.store.requestFileAsync(it) }, modifier)
        return
    }
    val path = file?.let { snapshot.files[it.id] }
    if (file != null && path == null) LaunchedEffect(file.id) { graph.store.requestFileAsync(file.id) }
    if (path != null) {
        AsyncImage(File(path), null, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        val icon = when (result) {
            is TdApi.InlineQueryResultAudio, is TdApi.InlineQueryResultVoiceNote -> Icons.Rounded.MusicNote
            is TdApi.InlineQueryResultDocument -> Icons.Rounded.Download
            else -> Icons.Rounded.Description
        }
        Box(modifier, contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
    }
}
