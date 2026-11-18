package com.pixelgram.android.td

import androidx.compose.runtime.Immutable
import org.drinkless.tdlib.TdApi

enum class PreviewKind { Gift, Text, Photo, Video, Gif, Sticker, Voice, VideoNote, Audio, File, Poll, Location, Contact, Service, Other }

@Immutable
data class MessagePreview(val sender: String?, val kind: PreviewKind, val text: String, val entities: List<TdApi.TextEntity> = emptyList(), val thumb: ByteArray? = null, val thumbFileId: Int = 0, val playable: Boolean = false, val forwarded: Boolean = false, val round: Boolean = false, val albumId: Long = 0L, val messageId: Long = 0L, val chatId: Long = 0L)

internal fun previewOf(message: TdApi.Message, chat: TdApi.Chat, users: Map<Long, TdApi.User>): MessagePreview {
    val sender = when (val from = message.senderId) {
        is TdApi.MessageSenderUser -> users[from.userId]?.firstName
        else -> null
    }.takeIf { chat.type is TdApi.ChatTypeBasicGroup || (chat.type as? TdApi.ChatTypeSupergroup)?.isChannel == false }
    val (kind, text) = contentPreview(message.content)
    val formatted = when (val c = message.content) {
        is TdApi.MessageText -> c.text
        is TdApi.MessagePhoto -> c.caption
        is TdApi.MessageVideo -> c.caption
        is TdApi.MessageAnimation -> c.caption
        is TdApi.MessageVoiceNote -> c.caption
        is TdApi.MessageDocument -> c.caption.takeIf { it.text.isNotBlank() }
        else -> null
    }
    val entities = formatted?.let { f -> LocalPremiumEmoji.incoming(f.text, f.entities.toList()).filter { it.type is TdApi.TextEntityTypeCustomEmoji } }.orEmpty()
    val (thumb, thumbFile) = when (val c = message.content) {
        is TdApi.MessagePhoto -> c.photo.minithumbnail?.data to (c.photo.sizes.firstOrNull { it.type == "m" } ?: c.photo.sizes.firstOrNull())?.photo?.id
        is TdApi.MessageVideo -> c.video.minithumbnail?.data to c.video.thumbnail?.takeIf { it.format is TdApi.ThumbnailFormatJpeg }?.file?.id
        is TdApi.MessageAnimation -> c.animation.minithumbnail?.data to c.animation.thumbnail?.takeIf { it.format is TdApi.ThumbnailFormatJpeg }?.file?.id
        is TdApi.MessageVideoNote -> c.videoNote.minithumbnail?.data to c.videoNote.thumbnail?.takeIf { it.format is TdApi.ThumbnailFormatJpeg }?.file?.id
        is TdApi.MessageGift -> null to c.gift.sticker.thumbnail?.takeIf { it.format !is TdApi.ThumbnailFormatTgs }?.file?.id
        is TdApi.MessageUpgradedGift -> null to c.gift.model?.sticker?.thumbnail?.takeIf { it.format !is TdApi.ThumbnailFormatTgs }?.file?.id
        else -> null to null
    }
    val playable = message.content is TdApi.MessageVideo || message.content is TdApi.MessageVideoNote
    return MessagePreview(if (message.isOutgoing) null else sender, kind, text, entities, thumb, thumbFile ?: 0, playable, message.forwardInfo != null, message.content is TdApi.MessageVideoNote, message.mediaAlbumId, message.id, message.chatId)
}

internal fun contentPreview(content: TdApi.MessageContent): Pair<PreviewKind, String> = when (content) {
    is TdApi.MessageText -> PreviewKind.Text to content.text.text
    is TdApi.MessagePhoto -> PreviewKind.Photo to content.caption.text
    is TdApi.MessageVideo -> PreviewKind.Video to content.caption.text
    is TdApi.MessageAnimation -> PreviewKind.Gif to content.caption.text
    is TdApi.MessageSticker -> PreviewKind.Sticker to content.sticker.emoji
    is TdApi.MessageAnimatedEmoji -> PreviewKind.Text to content.emoji
    is TdApi.MessageVoiceNote -> PreviewKind.Voice to content.caption.text
    is TdApi.MessageVideoNote -> PreviewKind.VideoNote to ""
    is TdApi.MessageAudio -> PreviewKind.Audio to listOf(content.audio.performer, content.audio.title).filter { it.isNotBlank() }.joinToString(" – ")
    is TdApi.MessageDocument -> PreviewKind.File to (content.caption.text.ifBlank { content.document.fileName })
    is TdApi.MessagePoll -> PreviewKind.Poll to content.poll.question.text
    is TdApi.MessageLocation -> PreviewKind.Location to ""
    is TdApi.MessageGift -> PreviewKind.Gift to content.text.text
    is TdApi.MessageUpgradedGift -> PreviewKind.Gift to content.gift.title + " #" + content.gift.number
    is TdApi.MessageContact -> PreviewKind.Contact to listOf(content.contact.firstName, content.contact.lastName).joinToString(" ").trim()
    else -> PreviewKind.Service to ""
}

internal fun contentPreviewLabel(content: TdApi.MessageContent): String = contentPreview(content).second
