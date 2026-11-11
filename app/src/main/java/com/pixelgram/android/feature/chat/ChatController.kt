package com.pixelgram.android.feature.chat

import androidx.compose.runtime.Immutable
import com.pixelgram.android.AppGraph
import com.pixelgram.android.td.contentPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

@Immutable
data class UiMessage(
    val id: Long,
    val senderId: Long,
    val outgoing: Boolean,
    val date: Int,
    val text: String,
    val entities: List<TdApi.TextEntity>,
    val content: TdApi.MessageContent,
    val replyToId: Long,
    val pending: Boolean,
    val failed: Boolean,
    val edited: Boolean,
    val read: Boolean,
    val service: Boolean,
    val mediaFileId: Int,
    val mediaThumb: ByteArray?,
    val mediaWidth: Int,
    val mediaHeight: Int,
    val albumId: Long = 0,
    val localPath: String? = null,
    val parts: List<UiMessage> = emptyList(),
    val reactions: List<ReactionUi> = emptyList(),
)

@Immutable
data class ReactionUi(val emoji: String, val count: Int, val chosen: Boolean)

@Immutable
data class ChatUi(
    val title: String = "",
    val subtitle: String = "",
    val messages: List<UiMessage> = emptyList(),
    val loading: Boolean = true,
    val reachedEnd: Boolean = false,
    val replyTo: UiMessage? = null,
    val editing: UiMessage? = null,
    val typing: List<Long> = emptyList(),
    val replies: Map<Long, UiMessage> = emptyMap(),
    val canSend: Boolean = true,
    val members: Int = 0,
    val online: Int = 0,
    val channel: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ChatController(private val graph: AppGraph, val chatId: Long, val topicId: Int = 0) {
    private val topic: TdApi.MessageTopic? = if (topicId != 0) TdApi.MessageTopicForum(topicId) else null

    private fun inTopic(message: TdApi.Message): Boolean = topicId == 0 || (message.topicId as? TdApi.MessageTopicForum)?.forumTopicId == topicId
    private val scope = CoroutineScope(graph.scope.coroutineContext + Job())
    private val worker = Dispatchers.Default.limitedParallelism(1)
    private val messages = LinkedHashMap<Long, TdApi.Message>()
    private val replies = HashMap<Long, TdApi.Message>()
    private val loadingReplies = HashSet<Long>()
    private val actions = HashMap<Long, Long>()
    private var readOutbox = 0L
    private val ui = MutableStateFlow(ChatUi())
    val state: StateFlow<ChatUi> = ui.asStateFlow()

    init {
        val chat = graph.store.chat(chatId)
        readOutbox = chat?.lastReadOutboxMessageId ?: 0
        ui.value = ui.value.copy(title = chat?.title.orEmpty())
        graph.td.fire(TdApi.OpenChat(chatId))
        if (topicId != 0) scope.launch {
            runCatching { graph.td.send(TdApi.GetForumTopic(chatId, topicId)) }.getOrNull()?.let { ui.value = ui.value.copy(title = it.info.name) }
        }
        scope.launch(worker) { graph.td.updates.collect(::onUpdate) }
        scope.launch {
            val members = runCatching {
                when (val type = chat?.type) {
                    is TdApi.ChatTypeSupergroup -> graph.td.send(TdApi.GetSupergroupFullInfo(type.supergroupId)).memberCount
                    is TdApi.ChatTypeBasicGroup -> graph.td.send(TdApi.GetBasicGroupFullInfo(type.basicGroupId)).members.size
                    else -> 0
                }
            }.getOrDefault(0)
            ui.value = ui.value.copy(members = members, channel = (chat?.type as? TdApi.ChatTypeSupergroup)?.isChannel == true)
        }
        loadMore()
    }

    private fun onUpdate(update: TdApi.Object) {
        when (update) {
            is TdApi.UpdateNewMessage -> if (update.message.chatId == chatId && inTopic(update.message)) {
                messages[update.message.id] = update.message
                graph.td.fire(TdApi.ViewMessages(chatId, longArrayOf(update.message.id), null, true))
            } else return
            is TdApi.UpdateMessageSendSucceeded -> if (update.message.chatId == chatId) {
                messages.remove(update.oldMessageId)
                messages[update.message.id] = update.message
            } else return
            is TdApi.UpdateMessageSendFailed -> if (update.message.chatId == chatId) {
                messages.remove(update.oldMessageId)
                messages[update.message.id] = update.message
            } else return
            is TdApi.UpdateDeleteMessages -> if (update.chatId == chatId && update.isPermanent) {
                update.messageIds.forEach { messages.remove(it) }
            } else return
            is TdApi.UpdateMessageContent -> if (update.chatId == chatId) {
                messages[update.messageId]?.content = update.newContent
            } else return
            is TdApi.UpdateMessageInteractionInfo -> if (update.chatId == chatId) {
                messages[update.messageId]?.interactionInfo = update.interactionInfo
            } else return
            is TdApi.UpdateMessageEdited -> if (update.chatId == chatId) {
                messages[update.messageId]?.editDate = update.editDate
            } else return
            is TdApi.UpdateChatAction -> if (update.chatId == chatId) {
                val sender = when (val from = update.senderId) {
                    is TdApi.MessageSenderUser -> from.userId
                    is TdApi.MessageSenderChat -> from.chatId
                    else -> 0L
                }
                if (update.action is TdApi.ChatActionCancel) actions.remove(sender) else actions[sender] = System.currentTimeMillis()
                ui.value = ui.value.copy(typing = actions.keys.toList())
                scope.launch(worker) {
                    kotlinx.coroutines.delay(6000)
                    val now = System.currentTimeMillis()
                    actions.entries.removeAll { now - it.value >= 6000 }
                    ui.value = ui.value.copy(typing = actions.keys.toList())
                }
                return
            } else return
            is TdApi.UpdateChatOnlineMemberCount -> if (update.chatId == chatId) {
                ui.value = ui.value.copy(online = update.onlineMemberCount)
                return
            } else return
            is TdApi.UpdateChatReadOutbox -> if (update.chatId == chatId) {
                readOutbox = update.lastReadOutboxMessageId
            } else return
            else -> return
        }
        publish()
    }

    private fun publish() {
        val sorted = messages.values.sortedByDescending { it.id }.map { it.toUi(readOutbox) }
        val grouped = ArrayList<UiMessage>(sorted.size)
        var index = 0
        while (index < sorted.size) {
            val message = sorted[index]
            if (message.albumId == 0L) {
                grouped += message
                index++
                continue
            }
            var end = index
            while (end + 1 < sorted.size && sorted[end + 1].albumId == message.albumId) end++
            val parts = sorted.subList(index, end + 1).reversed()
            val captioned = parts.firstOrNull { it.text.isNotBlank() } ?: parts.first()
            grouped += captioned.copy(id = parts.last().id, parts = parts, replyToId = parts.first().replyToId)
            index = end + 1
        }
        val wanted = sorted.map { it.replyToId }.filter { it != 0L && it !in messages && it !in replies }
        ui.value = ui.value.copy(messages = grouped, replies = replies.mapValues { it.value.toUi(readOutbox) })
        for (id in wanted) loadReply(id)
    }

    private fun loadReply(id: Long) {
        if (!loadingReplies.add(id)) return
        val source = messages.values.firstOrNull { (it.replyTo as? TdApi.MessageReplyToMessage)?.messageId == id } ?: return
        scope.launch(worker) {
            val found = runCatching { graph.td.send(TdApi.GetRepliedMessage(chatId, source.id)) }.getOrNull()
            if (found != null) {
                replies[id] = found
                publish()
            }
        }
    }

    fun loadMore() {
        if (ui.value.reachedEnd) return
        scope.launch(worker) {
            ui.value = ui.value.copy(loading = true)
            val from = messages.keys.minOrNull() ?: 0L
            val page = runCatching {
                if (topicId != 0) graph.td.send(TdApi.GetForumTopicHistory(chatId, topicId, from, 0, 50)) else graph.td.send(TdApi.GetChatHistory(chatId, from, 0, 50, false))
            }.getOrNull()
            val list = page?.messages.orEmpty()
            for (message in list) messages[message.id] = message
            if (from == 0L && list.isNotEmpty()) {
                graph.td.fire(TdApi.ViewMessages(chatId, list.map { it.id }.toLongArray(), null, true))
            }
            ui.value = ui.value.copy(loading = false, reachedEnd = page != null && list.isEmpty() && from != 0L)
            publish()
            if (from == 0L && list.size in 1..9) loadMore()
        }
    }

    fun reply(message: UiMessage?) {
        ui.value = ui.value.copy(replyTo = message, editing = null)
    }

    fun startEdit(message: UiMessage?) {
        ui.value = ui.value.copy(editing = message, replyTo = null)
    }

    fun submitEdit(text: String) {
        val target = ui.value.editing ?: return
        ui.value = ui.value.copy(editing = null)
        val body = text.trim()
        if (body.isEmpty()) return
        scope.launch {
            runCatching {
                if (target.content is TdApi.MessageText) {
                    graph.td.send(TdApi.EditMessageText().apply {
                        chatId = this@ChatController.chatId
                        messageId = target.id
                        inputMessageContent = TdApi.InputMessageText().apply { this.text = TdApi.FormattedText(body, emptyArray()) }
                    })
                } else {
                    graph.td.send(TdApi.EditMessageCaption().apply {
                        chatId = this@ChatController.chatId
                        messageId = target.id
                        caption = TdApi.FormattedText(body, emptyArray())
                    })
                }
            }
        }
    }

    fun delete(message: UiMessage, everyone: Boolean) {
        val ids = (message.parts.map { it.id }.ifEmpty { listOf(message.id) }).toLongArray()
        scope.launch { runCatching { graph.td.send(TdApi.DeleteMessages(chatId, ids, everyone)) } }
    }

    fun forward(message: UiMessage, toChatId: Long) {
        val ids = (message.parts.map { it.id }.ifEmpty { listOf(message.id) }).toLongArray()
        scope.launch {
            runCatching {
                graph.td.send(TdApi.ForwardMessages().apply {
                    chatId = toChatId
                    fromChatId = this@ChatController.chatId
                    messageIds = ids
                })
            }
        }
    }

    fun react(message: UiMessage, emoji: String) {
        if (emoji.isBlank()) return
        if (message.reactions.any { it.emoji == emoji && it.chosen }) {
            scope.launch { runCatching { graph.td.send(TdApi.RemoveMessageReaction(chatId, message.id, TdApi.ReactionTypeEmoji(emoji))) } }
            return
        }
        scope.launch {
            runCatching {
                graph.td.send(TdApi.AddMessageReaction().apply {
                    chatId = this@ChatController.chatId
                    messageId = message.id
                    reactionType = TdApi.ReactionTypeEmoji(emoji)
                    isBig = false
                    updateRecentReactions = true
                })
            }
        }
    }

    fun pin(message: UiMessage) {
        scope.launch { runCatching { graph.td.send(TdApi.PinChatMessage(chatId, message.id, true, false)) } }
    }

    suspend fun canDeleteForEveryone(message: UiMessage): Boolean = runCatching {
        graph.td.send(TdApi.GetMessageProperties(chatId, message.id)).canBeDeletedForAllUsers
    }.getOrDefault(false)

    suspend fun canEdit(message: UiMessage): Boolean = runCatching {
        graph.td.send(TdApi.GetMessageProperties(chatId, message.id)).canBeEdited
    }.getOrDefault(false)

    fun sendPhotos(paths: List<String>, caption: String) {
        if (paths.isEmpty()) return
        val reply = ui.value.replyTo
        ui.value = ui.value.copy(replyTo = null)
        val contents = paths.mapIndexed { index, path ->
            TdApi.InputMessagePhoto().apply {
                photo = TdApi.InputPhoto().apply {
                    photo = TdApi.InputFileLocal(path)
                    addedStickerFileIds = IntArray(0)
                }
                this.caption = TdApi.FormattedText(if (index == 0) caption.trim() else "", emptyArray())
            }
        }
        scope.launch {
            runCatching {
                val replyTo = reply?.let { TdApi.InputMessageReplyToMessage().apply { messageId = it.id } }
                if (contents.size == 1) {
                    graph.td.send(TdApi.SendMessage().apply {
                        chatId = this@ChatController.chatId
                        topicId = topic
                        this.replyTo = replyTo
                        inputMessageContent = contents.first()
                    })
                } else {
                    for (chunk in contents.chunked(10)) {
                        graph.td.send(TdApi.SendMessageAlbum().apply {
                            chatId = this@ChatController.chatId
                            topicId = topic
                            this.replyTo = replyTo
                            inputMessageContents = chunk.toTypedArray()
                        })
                    }
                }
            }
        }
    }

    fun send(text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        val reply = ui.value.replyTo
        ui.value = ui.value.copy(replyTo = null)
        scope.launch {
            runCatching {
                graph.td.send(TdApi.SendMessage().apply {
                    chatId = this@ChatController.chatId
                    topicId = topic
                    replyTo = reply?.let { TdApi.InputMessageReplyToMessage().apply { messageId = it.id } }
                    inputMessageContent = TdApi.InputMessageText().apply {
                        this.text = TdApi.FormattedText(body, emptyArray())
                        clearDraft = true
                    }
                })
            }
        }
    }

    fun sendContent(content: TdApi.InputMessageContent) {
        val reply = ui.value.replyTo
        ui.value = ui.value.copy(replyTo = null)
        scope.launch {
            runCatching {
                graph.td.send(TdApi.SendMessage().apply {
                    chatId = this@ChatController.chatId
                    topicId = topic
                    replyTo = reply?.let { TdApi.InputMessageReplyToMessage().apply { messageId = it.id } }
                    inputMessageContent = content
                })
            }
        }
    }

    fun sendDocuments(paths: List<String>) {
        paths.forEach { path ->
            sendContent(TdApi.InputMessageDocument().apply {
                document = TdApi.InputDocument().apply { document = TdApi.InputFileLocal(path) }
                caption = TdApi.FormattedText("", emptyArray())
            })
        }
    }

    fun sendPoll(question: String, options: List<String>, anonymous: Boolean, multiple: Boolean) {
        sendContent(TdApi.InputMessagePoll().apply {
            this.question = TdApi.FormattedText(question.trim(), emptyArray())
            this.options = options.map { TdApi.InputPollOption().apply { text = TdApi.FormattedText(it.trim(), emptyArray()) } }.toTypedArray()
            isAnonymous = anonymous
            allowsMultipleAnswers = multiple
            allowsRevoting = true
            countryCodes = emptyArray()
            type = TdApi.InputPollTypeRegular()
        })
    }

    fun sendContact(user: com.pixelgram.android.td.UserInfo) {
        sendContent(TdApi.InputMessageContact(TdApi.Contact(user.phone, user.firstName, user.lastName, "", user.id)))
    }

    fun close() {
        graph.td.fire(TdApi.CloseChat(chatId))
        scope.cancel()
    }
}

private fun TdApi.Message.toUi(readOutbox: Long): UiMessage {
    val (text, entities) = when (val c = content) {
        is TdApi.MessageText -> c.text.text to c.text.entities.toList()
        is TdApi.MessagePhoto -> c.caption.text to c.caption.entities.toList()
        is TdApi.MessageVideo -> c.caption.text to c.caption.entities.toList()
        is TdApi.MessageAnimation -> c.caption.text to c.caption.entities.toList()
        is TdApi.MessageDocument -> c.caption.text to c.caption.entities.toList()
        is TdApi.MessageVoiceNote -> c.caption.text to c.caption.entities.toList()
        is TdApi.MessageAudio -> c.caption.text to c.caption.entities.toList()
        else -> contentPreview(content).second to emptyList()
    }
    val photo = (content as? TdApi.MessagePhoto)?.photo
    val size = photo?.sizes?.lastOrNull { it.width <= 1280 } ?: photo?.sizes?.lastOrNull()
    val sticker = (content as? TdApi.MessageSticker)?.sticker
    val sender = when (val from = senderId) {
        is TdApi.MessageSenderUser -> from.userId
        is TdApi.MessageSenderChat -> from.chatId
        else -> 0L
    }
    return UiMessage(
        id = id,
        senderId = sender,
        outgoing = isOutgoing,
        date = date,
        text = text,
        entities = entities,
        content = content,
        replyToId = (replyTo as? TdApi.MessageReplyToMessage)?.messageId ?: 0,
        pending = sendingState is TdApi.MessageSendingStatePending,
        failed = sendingState is TdApi.MessageSendingStateFailed,
        edited = editDate > 0,
        read = isOutgoing && id <= readOutbox,
        service = isService(content),
        mediaFileId = size?.photo?.id ?: (sticker?.thumbnail?.file?.id ?: 0),
        mediaThumb = photo?.minithumbnail?.data,
        mediaWidth = size?.width ?: sticker?.width ?: 0,
        mediaHeight = size?.height ?: sticker?.height ?: 0,
        albumId = if (photo != null) mediaAlbumId else 0L,
        localPath = size?.photo?.local?.takeIf { it.isDownloadingCompleted }?.path,
        reactions = interactionInfo?.reactions?.reactions.orEmpty().mapNotNull { reaction ->
            (reaction.type as? TdApi.ReactionTypeEmoji)?.let { ReactionUi(it.emoji, reaction.totalCount, reaction.isChosen) }
        },
    )
}

private fun isService(content: TdApi.MessageContent): Boolean = when (content) {
    is TdApi.MessageText, is TdApi.MessagePhoto, is TdApi.MessageVideo, is TdApi.MessageAnimation,
    is TdApi.MessageSticker, is TdApi.MessageVoiceNote, is TdApi.MessageVideoNote, is TdApi.MessageAudio,
    is TdApi.MessageDocument, is TdApi.MessagePoll, is TdApi.MessageLocation, is TdApi.MessageContact,
    is TdApi.MessageUnsupported -> false
    else -> true
}
