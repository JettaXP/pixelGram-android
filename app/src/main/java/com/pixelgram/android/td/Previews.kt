package com.pixelgram.android.td

import androidx.compose.runtime.Immutable
import org.drinkless.tdlib.TdApi

enum class PreviewKind { Text, Photo, Video, Gif, Sticker, Voice, VideoNote, Audio, File, Poll, Location, Contact, Service, Other }

@Immutable
data class MessagePreview(val sender: String?, val kind: PreviewKind, val text: String)

internal fun previewOf(message: TdApi.Message, chat: TdApi.Chat, users: Map<Long, TdApi.User>): MessagePreview {
    val sender = when (val from = message.senderId) {
        is TdApi.MessageSenderUser -> users[from.userId]?.firstName
        else -> null
    }.takeIf { chat.type is TdApi.ChatTypeBasicGroup || (chat.type as? TdApi.ChatTypeSupergroup)?.isChannel == false }
    val (kind, text) = contentPreview(message.content)
    return MessagePreview(if (message.isOutgoing) null else sender, kind, text)
}

internal fun contentPreview(content: TdApi.MessageContent): Pair<PreviewKind, String> = when (content) {
    is TdApi.MessageText -> PreviewKind.Text to content.text.text
    is TdApi.MessagePhoto -> PreviewKind.Photo to content.caption.text
    is TdApi.MessageVideo -> PreviewKind.Video to content.caption.text
    is TdApi.MessageAnimation -> PreviewKind.Gif to content.caption.text
    is TdApi.MessageSticker -> PreviewKind.Sticker to content.sticker.emoji
    is TdApi.MessageVoiceNote -> PreviewKind.Voice to content.caption.text
    is TdApi.MessageVideoNote -> PreviewKind.VideoNote to ""
    is TdApi.MessageAudio -> PreviewKind.Audio to listOf(content.audio.performer, content.audio.title).filter { it.isNotBlank() }.joinToString(" – ")
    is TdApi.MessageDocument -> PreviewKind.File to (content.caption.text.ifBlank { content.document.fileName })
    is TdApi.MessagePoll -> PreviewKind.Poll to content.poll.question.text
    is TdApi.MessageLocation -> PreviewKind.Location to ""
    is TdApi.MessageContact -> PreviewKind.Contact to listOf(content.contact.firstName, content.contact.lastName).joinToString(" ").trim()
    else -> PreviewKind.Service to ""
}
