package com.pixelgram.android.nav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.rounded.Link
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
        if (SettingsLinks.matches(url)) {
            SettingsLinks.resolve(context, url)?.let { navigator.open(it) }
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
                is TdApi.InternalLinkTypeMessage -> runCatching { graph.td.send(TdApi.GetMessageLinkInfo(type.url)) }.getOrNull()?.takeIf { it.chatId != 0L }?.let { info ->
                    navigator.open(ChatKey(info.chatId))
                    info.message?.let { PendingNav.jump = info.chatId to it.id }
                } ?: browser(full)
                is TdApi.InternalLinkTypeStickerSet -> runCatching { graph.td.send(TdApi.SearchStickerSet(type.stickerSetName, false)) }.getOrNull()?.let { com.pixelgram.android.feature.chat.EmojiPreview.pack = it.id } ?: browser(full)
                is TdApi.InternalLinkTypeChatInvite -> {
                    val info = runCatching { graph.td.send(TdApi.CheckChatInviteLink(type.inviteLink)) }.getOrNull()
                    val chatId: Long? = info?.chatId?.takeIf { it != 0L } ?: runCatching {
                        graph.td.send(TdApi.JoinChatByInviteLink(type.inviteLink))
                        graph.td.send(TdApi.CheckChatInviteLink(type.inviteLink)).chatId
                    }.getOrNull()?.takeIf { it != 0L }
                    if (chatId != null) navigator.open(ChatKey(chatId)) else browser(full)
                }
                is TdApi.InternalLinkTypeBotStart -> {
                    val chat = runCatching { graph.td.send(TdApi.SearchPublicChat(type.botUsername)) }.getOrNull()
                    val bot = (chat?.type as? TdApi.ChatTypePrivate)?.userId
                    if (chat != null && bot != null) {
                        runCatching { graph.td.send(TdApi.SendBotStartMessage(bot, chat.id, type.startParameter)) }
                        navigator.open(ChatKey(chat.id))
                    } else browser(full)
                }
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
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val own = Uri.parse(url).host?.removePrefix("www.") in setOf("t.me", "telegram.me", "telegram.dog") || url.startsWith("tg:")
        if (own) {
            val browser = context.packageManager.resolveActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")), android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
            if (browser != null && browser != context.packageName && browser != "android") intent.setPackage(browser)
            else if (url.startsWith("tg:")) return
        }
        runCatching { context.startActivity(intent) }
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
    val style = TextLinkStyles(SpanStyle(color = link, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))
    for (entity in text.entities) {
        val start = entity.offset.coerceIn(0, value.length)
        val end = (entity.offset + entity.length).coerceIn(start, value.length)
        if (start == end) continue
        val part = value.substring(start, end)
        val action: (() -> Unit)? = when (val type = entity.type) {
            is TdApi.TextEntityTypeUrl -> ({ opener.url(part) })
            is TdApi.TextEntityTypeTextUrl -> ({ LinkConfirm.ask(type.url, part) })
            is TdApi.TextEntityTypeMention -> ({ opener.username(part) })
            is TdApi.TextEntityTypeMentionName -> ({ opener.user(type.userId) })
            is TdApi.TextEntityTypeEmailAddress -> ({ opener.url("mailto:$part") })
            is TdApi.TextEntityTypePhoneNumber -> ({ opener.url("tel:$part") })
            else -> null
        }
        if (action != null) addLink(LinkAnnotation.Clickable("link$start", style) { action() }, start, end)
    }
}

object LinkConfirm {
    var pending by androidx.compose.runtime.mutableStateOf<String?>(null)

    fun ask(url: String, shown: String) {
        val bare = { v: String -> v.removePrefix("https://").removePrefix("http://").removePrefix("www.").trimEnd('/') }
        if (bare(url).equals(bare(shown), ignoreCase = true) || SettingsLinks.matches(url) || com.pixelgram.android.feature.pixel.Credits.handles(url)) PendingNav.external = url else pending = url
    }
}

@androidx.compose.runtime.Composable
fun LinkConfirmHost(onOpen: (String) -> Unit) {
    val url = LinkConfirm.pending ?: return
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { LinkConfirm.pending = null },
        icon = { com.pixelgram.android.ui.components.Icon(androidx.compose.material.icons.Icons.Rounded.Link, null) },
        title = { androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(com.pixelgram.android.R.string.link_confirm_title)) },
        text = { androidx.compose.material3.Text(url, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                LinkConfirm.pending = null
                onOpen(url)
            }) { androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(com.pixelgram.android.R.string.link_confirm_open)) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = {
                context.getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(android.content.ClipData.newPlainText(null, url))
                LinkConfirm.pending = null
            }) { androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(com.pixelgram.android.R.string.action_copy)) }
        },
    )
}
