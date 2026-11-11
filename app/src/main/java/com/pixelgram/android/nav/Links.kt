package com.pixelgram.android.nav

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import com.pixelgram.android.AppGraph
import com.pixelgram.android.feature.pixel.Credits
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

class LinkOpener(private val graph: AppGraph, private val navigator: Navigator, private val context: Context, private val launch: (suspend () -> Unit) -> Unit) {
    fun url(url: String) {
        if (Credits.handles(url)) {
            Credits.open = true
            return
        }
        if (url.startsWith("mailto:") || url.startsWith("tel:")) {
            browser(url)
            return
        }
        val full = if (url.contains("://")) url else "https://$url"
        launch {
            val type = runCatching { graph.td.send(TdApi.GetInternalLinkType(full)) }.getOrNull()
            when (type) {
                is TdApi.InternalLinkTypePublicChat -> openUsername(type.chatUsername)
                is TdApi.InternalLinkTypeUserPhoneNumber -> runCatching { graph.td.send(TdApi.SearchUserByPhoneNumber(type.phoneNumber, true)) }.getOrNull()?.let { navigator.open(ProfileKey(it.id)) } ?: browser(full)
                is TdApi.InternalLinkTypeMessage -> runCatching { graph.td.send(TdApi.GetMessageLinkInfo(type.url)) }.getOrNull()?.takeIf { it.chatId != 0L }?.let { navigator.open(ChatKey(it.chatId)) } ?: browser(full)
                else -> browser(full)
            }
        }
    }

    fun username(name: String) = launch { openUsername(name.removePrefix("@")) }

    fun user(userId: Long) = navigator.open(ProfileKey(userId))

    private suspend fun openUsername(name: String) {
        val chat = runCatching { graph.td.send(TdApi.SearchPublicChat(name)) }.getOrNull()
        if (chat == null) {
            browser("https://t.me/$name")
            return
        }
        when (val type = chat.type) {
            is TdApi.ChatTypePrivate -> navigator.open(ProfileKey(type.userId, chat.id))
            else -> navigator.open(ChatKey(chat.id))
        }
    }

    private fun browser(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

@Composable
fun rememberLinkOpener(graph: AppGraph, navigator: Navigator): LinkOpener {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return androidx.compose.runtime.remember(graph, navigator, context) { LinkOpener(graph, navigator, context) { block -> scope.launch { block() } } }
}

fun linkedText(text: TdApi.FormattedText, link: Color, opener: LinkOpener): AnnotatedString = buildAnnotatedString {
    val value = text.text
    append(value)
    val style = TextLinkStyles(SpanStyle(color = link))
    for (entity in text.entities) {
        val start = entity.offset.coerceIn(0, value.length)
        val end = (entity.offset + entity.length).coerceIn(start, value.length)
        if (start == end) continue
        val part = value.substring(start, end)
        val action: (() -> Unit)? = when (val type = entity.type) {
            is TdApi.TextEntityTypeUrl -> ({ opener.url(part) })
            is TdApi.TextEntityTypeTextUrl -> ({ opener.url(type.url) })
            is TdApi.TextEntityTypeMention -> ({ opener.username(part) })
            is TdApi.TextEntityTypeMentionName -> ({ opener.user(type.userId) })
            is TdApi.TextEntityTypeEmailAddress -> ({ opener.url("mailto:$part") })
            is TdApi.TextEntityTypePhoneNumber -> ({ opener.url("tel:$part") })
            else -> null
        }
        if (action != null) addLink(LinkAnnotation.Clickable("link$start", style) { action() }, start, end)
    }
}
