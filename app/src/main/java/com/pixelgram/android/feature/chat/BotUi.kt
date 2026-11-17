package com.pixelgram.android.feature.chat

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.North
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Web
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pixelgram.android.AppGraph
import com.pixelgram.android.LocalGraph
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.Avatar
import com.pixelgram.android.ui.components.Icon
import com.pixelgram.android.ui.components.Notice
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

object ComposerRequests {
    var text by mutableStateOf<String?>(null)
}

object WebApps {
    var open by mutableStateOf<Pair<String, String>?>(null)
}

@Composable
fun MessageBotExtras(
    message: UiMessage,
    chatId: Long,
    channel: Boolean,
    indent: androidx.compose.ui.unit.Dp = 0.dp,
    onOpenUrl: (String) -> Unit,
    onComments: () -> Unit,
) {
    val markup = message.markup as? TdApi.ReplyMarkupInlineKeyboard
    if (markup == null && !(channel && message.comments >= 0)) return
    Column(
        Modifier.fillMaxWidth().padding(top = 3.dp, start = 8.dp + indent, end = 8.dp).widthIn(max = 320.dp + indent),
        horizontalAlignment = if (message.outgoing) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (markup != null) InlineKeyboard(markup, message, chatId, onOpenUrl)
        if (channel && message.comments >= 0) CommentsBar(message, onComments)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun InlineKeyboard(markup: TdApi.ReplyMarkupInlineKeyboard, message: UiMessage, chatId: Long, onOpenUrl: (String) -> Unit) {
    val graph = LocalGraph.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var details by remember { mutableStateOf<TdApi.InlineKeyboardButton?>(null) }
    Column(Modifier.widthIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        markup.rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                row.forEach { button ->
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .combinedClickable(
                                onClick = { press(graph, scope, chatId, message, button, onOpenUrl) },
                                onLongClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    details = button
                                },
                            )
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(button.text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        val corner = when (button.type) {
                            is TdApi.InlineKeyboardButtonTypeUrl, is TdApi.InlineKeyboardButtonTypeLoginUrl -> Icons.Rounded.NorthEast
                            is TdApi.InlineKeyboardButtonTypeWebApp -> Icons.Rounded.Web
                            is TdApi.InlineKeyboardButtonTypeSwitchInline -> Icons.Rounded.North
                            else -> null
                        }
                        if (corner != null) Icon(corner, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp).size(12.dp))
                    }
                }
            }
        }
    }
    details?.let { button ->
        val clipboard = LocalClipboardManager.current
        ModalBottomSheet(onDismissRequest = { details = null }, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
                Text(button.text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
                SheetAction(stringResource(R.string.button_copy_title)) {
                    clipboard.setText(AnnotatedString(button.text))
                    details = null
                }
                val payload = when (val type = button.type) {
                    is TdApi.InlineKeyboardButtonTypeCallback -> R.string.button_copy_callback to String(type.data)
                    is TdApi.InlineKeyboardButtonTypeUrl -> R.string.button_copy_link to type.url
                    is TdApi.InlineKeyboardButtonTypeWebApp -> R.string.button_copy_link to type.url
                    is TdApi.InlineKeyboardButtonTypeSwitchInline -> R.string.button_copy_query to type.query
                    is TdApi.InlineKeyboardButtonTypeCopyText -> R.string.button_copy_text to type.text
                    else -> null
                }
                payload?.let { (label, value) ->
                    SheetAction(stringResource(label)) {
                        clipboard.setText(AnnotatedString(value))
                        details = null
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 16.dp),
    )
}

private fun press(graph: AppGraph, scope: kotlinx.coroutines.CoroutineScope, chatId: Long, message: UiMessage, button: TdApi.InlineKeyboardButton, onOpenUrl: (String) -> Unit) {
    when (val type = button.type) {
        is TdApi.InlineKeyboardButtonTypeUrl -> onOpenUrl(type.url)
        is TdApi.InlineKeyboardButtonTypeLoginUrl -> onOpenUrl(type.url)
        is TdApi.InlineKeyboardButtonTypeCopyText -> Unit
        is TdApi.InlineKeyboardButtonTypeSwitchInline -> {
            val bot = message.viaBotId.takeIf { it != 0L } ?: message.senderId
            val name = graph.store.snapshot.value.users[bot]?.username
            ComposerRequests.text = listOfNotNull(name?.let { "@$it" }, type.query).joinToString(" ") + " "
        }
        is TdApi.InlineKeyboardButtonTypeWebApp -> scope.launch {
            val bot = message.viaBotId.takeIf { it != 0L } ?: message.senderId
            openWebApp(graph, chatId, bot, type.url, button.text)
        }
        is TdApi.InlineKeyboardButtonTypeCallback -> scope.launch {
            val answer = runCatching { graph.td.send(TdApi.GetCallbackQueryAnswer(chatId, message.id, TdApi.CallbackQueryPayloadData(type.data))) }.getOrNull() ?: return@launch
            if (answer.url.isNotBlank()) onOpenUrl(answer.url)
            if (answer.text.isNotBlank()) Notice.show(answer.text)
        }
        is TdApi.InlineKeyboardButtonTypeCallbackGame -> scope.launch {
            val answer = runCatching { graph.td.send(TdApi.GetCallbackQueryAnswer(chatId, message.id, TdApi.CallbackQueryPayloadGame(""))) }.getOrNull() ?: return@launch
            if (answer.url.isNotBlank()) WebApps.open = button.text to answer.url
        }
        is TdApi.InlineKeyboardButtonTypeUser -> com.pixelgram.android.nav.PendingNav.link = "tg://user?id=${type.userId}"
        else -> Notice.show(button.text)
    }
}

suspend fun openWebApp(graph: AppGraph, chatId: Long, botUserId: Long, url: String, title: String) {
    val info = runCatching {
        graph.td.send(TdApi.OpenWebApp().apply {
            this.chatId = chatId
            this.botUserId = botUserId
            this.url = url
            parameters = TdApi.WebAppOpenParameters().apply {
                theme = WebAppTheme.current
                applicationName = "pixelGram"
                mode = TdApi.WebAppOpenModeFullSize()
            }
        })
    }.getOrNull()
    WebApps.open = title to (info?.url?.url ?: url)
}

object WebAppTheme {
    var current: TdApi.ThemeParameters? = null
}

@Composable
fun rememberWebAppTheme() {
    val scheme = MaterialTheme.colorScheme
    WebAppTheme.current = remember(scheme) {
        TdApi.ThemeParameters().apply {
            backgroundColor = scheme.surface.toArgb() and 0xFFFFFF
            secondaryBackgroundColor = scheme.surfaceContainer.toArgb() and 0xFFFFFF
            headerBackgroundColor = scheme.surfaceContainer.toArgb() and 0xFFFFFF
            bottomBarBackgroundColor = scheme.surfaceContainer.toArgb() and 0xFFFFFF
            sectionBackgroundColor = scheme.surfaceContainerHigh.toArgb() and 0xFFFFFF
            sectionSeparatorColor = scheme.outlineVariant.toArgb() and 0xFFFFFF
            textColor = scheme.onSurface.toArgb() and 0xFFFFFF
            accentTextColor = scheme.primary.toArgb() and 0xFFFFFF
            sectionHeaderTextColor = scheme.primary.toArgb() and 0xFFFFFF
            subtitleTextColor = scheme.onSurfaceVariant.toArgb() and 0xFFFFFF
            destructiveTextColor = scheme.error.toArgb() and 0xFFFFFF
            hintColor = scheme.onSurfaceVariant.toArgb() and 0xFFFFFF
            linkColor = scheme.primary.toArgb() and 0xFFFFFF
            buttonColor = scheme.primary.toArgb() and 0xFFFFFF
            buttonTextColor = scheme.onPrimary.toArgb() and 0xFFFFFF
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebAppHost() {
    rememberWebAppTheme()
    val (title, url) = WebApps.open ?: return
    Dialog(onDismissRequest = { WebApps.open = null }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    IconButton(onClick = { WebApps.open = null }) { Icon(Icons.Rounded.Close, stringResource(R.string.action_close)) }
                    Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = WebViewClient()
                            loadUrl(url)
                        }
                    },
                    onRelease = { it.destroy() },
                )
            }
        }
    }
}

@Composable
private fun CommentsBar(message: UiMessage, onClick: () -> Unit) {
    val graph = LocalGraph.current
    val snapshot by graph.store.snapshot.collectAsState()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            if (message.commenters.isEmpty()) {
                Icon(Icons.Rounded.ChatBubbleOutline, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            } else {
                Box {
                    message.commenters.take(3).forEachIndexed { index, id ->
                        val user = snapshot.users[id]
                        Box(Modifier.offset(x = (index * 14).dp).size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(1.5.dp)) {
                            Avatar(user?.name.orEmpty(), id, user?.photoFileId?.let { snapshot.files[it] }, user?.minithumb, 21.dp, onNeedFile = { user?.photoFileId?.let(graph.store::requestFileAsync) })
                        }
                    }
                    Box(Modifier.size(width = (24 + (message.commenters.take(3).size - 1) * 14).dp, height = 24.dp))
                }
            }
            Text(
                if (message.comments > 0) pluralStringResource(R.plurals.channel_comments_count, message.comments, message.comments) else stringResource(R.string.channel_leave_comment),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
