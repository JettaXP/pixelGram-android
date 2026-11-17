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
    val replyQuote: String = "",
    val guestCallerId: Long = 0L,
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
    val views: Int = 0,
    val chatId: Long = 0,
    val comments: Int = -1,
    val commenters: List<Long> = emptyList(),
    val viaBotId: Long = 0,
    val markup: TdApi.ReplyMarkup? = null,
)

@Immutable
data class ReactionUi(val emoji: String, val count: Int, val chosen: Boolean, val recent: List<Long> = emptyList())

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
    val pinned: UiMessage? = null,
    val pins: List<UiMessage> = emptyList(),
    val linkedChatId: Long = 0,
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
        if (topicId == 0) com.pixelgram.android.notify.AppVisibility.openChat = chatId
        graph.notifier.clear(chatId)
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
            (chat?.type as? TdApi.ChatTypeSupergroup)?.let { type ->
                val group = runCatching { graph.td.send(TdApi.GetSupergroup(type.supergroupId)) }.getOrNull()
                val full = runCatching { graph.td.send(TdApi.GetSupergroupFullInfo(type.supergroupId)) }.getOrNull()
                val canPost = when (val status = group?.status) {
                    is TdApi.ChatMemberStatusCreator -> true
                    is TdApi.ChatMemberStatusAdministrator -> !type.isChannel || status.rights.canPostMessages
                    is TdApi.ChatMemberStatusRestricted -> !type.isChannel && status.permissions.canSendBasicMessages
                    is TdApi.ChatMemberStatusLeft, is TdApi.ChatMemberStatusBanned -> !type.isChannel && chat.permissions.canSendBasicMessages
                    else -> !type.isChannel && chat.permissions.canSendBasicMessages
                }
                ui.value = ui.value.copy(canSend = canPost, linkedChatId = if (type.isChannel) full?.linkedChatId ?: 0 else 0)
            }
        }
        loadPinned()
        loadMore()
    }

    private fun loadPinned() {
        if (topicId != 0) return
        scope.launch {
            val message = runCatching { graph.td.send(TdApi.GetChatPinnedMessage(chatId)) }.getOrNull()
            val all = runCatching { graph.td.send(TdApi.SearchChatMessages(chatId, null, "", null, 0, 0, 100, TdApi.SearchMessagesFilterPinned())).messages.toList() }.getOrDefault(emptyList())
            val pins = (all.ifEmpty { listOfNotNull(message) }).sortedByDescending { it.id }.map { it.toUi(readOutbox) }
            ui.value = ui.value.copy(pinned = message?.toUi(readOutbox), pins = pins)
        }
    }

    private fun onUpdate(update: TdApi.Object) {
        when (update) {
            is TdApi.UpdateMessageIsPinned -> if (update.chatId == chatId) loadPinned()
            is TdApi.UpdateNewMessage -> if (update.message.chatId == chatId && update.message.schedulingState != null) {
                loadScheduled()
                return
            } else if (update.message.chatId == chatId && inTopic(update.message)) {
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
                messages[update.messageId]?.replyMarkup = update.replyMarkup
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

    suspend fun reveal(id: Long): Boolean = kotlinx.coroutines.withContext(worker) {
        var pages = 0
        while (messages.keys.none { it == id } && pages < RevealPages) {
            val from = messages.keys.minOrNull() ?: 0L
            val page = runCatching {
                if (topicId != 0) graph.td.send(TdApi.GetForumTopicHistory(chatId, topicId, from, 0, 100)) else graph.td.send(TdApi.GetChatHistory(chatId, from, 0, 100, false))
            }.getOrNull()
            val list = page?.messages.orEmpty()
            if (list.isEmpty()) break
            for (message in list) messages[message.id] = message
            pages++
        }
        publish()
        id in messages
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

    fun submitEdit(formatted: TdApi.FormattedText) {
        val target = ui.value.editing ?: return
        ui.value = ui.value.copy(editing = null)
        if (formatted.text.isEmpty()) return
        scope.launch {
            runCatching {
                if (target.content is TdApi.MessageText) {
                    graph.td.send(TdApi.EditMessageText().apply {
                        chatId = this@ChatController.chatId
                        messageId = target.id
                        inputMessageContent = TdApi.InputMessageText().apply { this.text = formatted }
                    })
                } else {
                    graph.td.send(TdApi.EditMessageCaption().apply {
                        chatId = this@ChatController.chatId
                        messageId = target.id
                        caption = formatted
                    })
                }
            }
        }
    }

    fun deleteAll(messages: List<UiMessage>, everyone: Boolean) {
        val ids = messages.flatMap { m -> m.parts.map { it.id }.ifEmpty { listOf(m.id) } }.distinct().toLongArray()
        if (ids.isEmpty()) return
        scope.launch { runCatching { graph.td.send(TdApi.DeleteMessages(chatId, ids, everyone)) } }
    }

    fun forwardAll(messages: List<UiMessage>, toChatId: Long) {
        val ids = messages.flatMap { m -> m.parts.map { it.id }.ifEmpty { listOf(m.id) } }.distinct().sorted().toLongArray()
        if (ids.isEmpty()) return
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

    fun sendInline(queryId: Long, resultId: String) {
        val reply = ui.value.replyTo
        ui.value = ui.value.copy(replyTo = null)
        scope.launch {
            runCatching {
                graph.td.send(TdApi.SendInlineQueryResultMessage().apply {
                    chatId = this@ChatController.chatId
                    replyTo = reply?.let { TdApi.InputMessageReplyToMessage().apply { messageId = it.id } }
                    this.queryId = queryId
                    this.resultId = resultId
                })
            }
        }
    }

    fun forwardDraft(draft: ForwardDraft) {
        scope.launch { runCatching { sendForward(graph, chatId, draft) } }
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

    fun openComments(message: UiMessage, open: (Long, Long) -> Unit) {
        scope.launch {
            val info = runCatching { graph.td.send(TdApi.GetMessageThread(chatId, message.id)) }.getOrNull() ?: return@launch
            val root = info.messages.lastOrNull()?.id ?: info.messageThreadId
            open(info.chatId, root)
        }
    }

    fun react(message: UiMessage, emoji: String) {
        if (emoji.isBlank()) return
        val type: TdApi.ReactionType = emoji.removePrefix("ce:").toLongOrNull()?.takeIf { emoji.startsWith("ce:") }?.let { TdApi.ReactionTypeCustomEmoji(it) } ?: TdApi.ReactionTypeEmoji(emoji)
        if (message.reactions.any { it.emoji == emoji && it.chosen }) {
            scope.launch { runCatching { graph.td.send(TdApi.RemoveMessageReaction(chatId, message.id, type)) } }
            return
        }
        scope.launch {
            runCatching {
                graph.td.send(TdApi.AddMessageReaction().apply {
                    chatId = this@ChatController.chatId
                    messageId = message.id
                    reactionType = type
                    isBig = false
                    updateRecentReactions = true
                })
            }
        }
    }

    suspend fun link(message: UiMessage): String? = runCatching {
        graph.td.send(TdApi.GetMessageLink().apply {
            chatId = this@ChatController.chatId
            messageId = message.id
            forAlbum = message.parts.size > 1
        }).link
    }.getOrNull()

    fun vote(message: UiMessage, options: IntArray) {
        scope.launch { runCatching { graph.td.send(TdApi.SetPollAnswer(chatId, message.id, options)) } }
    }

    fun pin(message: UiMessage) {
        scope.launch { runCatching { graph.td.send(TdApi.PinChatMessage(chatId, message.id, true, false)) } }
    }

    fun unpin(message: UiMessage) {
        scope.launch { runCatching { graph.td.send(TdApi.UnpinChatMessage(chatId, message.id)) } }
    }

    fun faveSticker(sticker: TdApi.Sticker, add: Boolean) {
        scope.launch {
            runCatching {
                val file = TdApi.InputFileId(sticker.sticker.id)
                graph.td.send(if (add) TdApi.AddFavoriteSticker(file) else TdApi.RemoveFavoriteSticker(file))
            }
        }
    }

    suspend fun isFavoriteSticker(sticker: TdApi.Sticker): Boolean = runCatching {
        graph.td.send(TdApi.GetFavoriteStickers()).stickers.any { it.sticker.id == sticker.sticker.id }
    }.getOrDefault(false)

    suspend fun canDeleteForEveryone(message: UiMessage): Boolean = runCatching {
        graph.td.send(TdApi.GetMessageProperties(chatId, message.id)).canBeDeletedForAllUsers
    }.getOrDefault(false)

    suspend fun properties(message: UiMessage): TdApi.MessageProperties? = runCatching { graph.td.send(TdApi.GetMessageProperties(chatId, message.id)) }.getOrNull()

    fun pin(message: UiMessage, notify: Boolean, onlyForSelf: Boolean) {
        scope.launch { runCatching { graph.td.send(TdApi.PinChatMessage(chatId, message.id, !notify, onlyForSelf)) } }
    }

    suspend fun canEdit(message: UiMessage): Boolean = runCatching {
        graph.td.send(TdApi.GetMessageProperties(chatId, message.id)).canBeEdited
    }.getOrDefault(false)

    fun launch(block: suspend () -> Unit) {
        scope.launch { block() }
    }

    private fun videoContent(path: String, caption: String): TdApi.InputMessageContent {
        val retriever = android.media.MediaMetadataRetriever()
        var width = 0
        var height = 0
        var duration = 0
        runCatching {
            retriever.setDataSource(path)
            val rotation = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val w = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            if (rotation == 90 || rotation == 270) { width = h; height = w } else { width = w; height = h }
            duration = ((retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) / 1000L).toInt()
        }
        runCatching { retriever.release() }
        return TdApi.InputMessageVideo().apply {
            video = TdApi.InputVideo().apply {
                video = TdApi.InputFileLocal(path)
                addedStickerFileIds = IntArray(0)
                this.duration = duration
                this.width = width
                this.height = height
                supportsStreaming = true
            }
            this.caption = TdApi.FormattedText(caption, emptyArray())
        }
    }

    fun sendPhotos(paths: List<String>, caption: String) {
        if (paths.isEmpty()) return
        val reply = ui.value.replyTo
        ui.value = ui.value.copy(replyTo = null)
        val contents = paths.mapIndexed { index, path ->
            if (isVideoPath(path)) return@mapIndexed videoContent(path, if (index == 0) caption.trim() else "")
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            runCatching { android.graphics.BitmapFactory.decodeFile(path, bounds) }
            TdApi.InputMessagePhoto().apply {
                photo = TdApi.InputPhoto().apply {
                    photo = TdApi.InputFileLocal(path)
                    addedStickerFileIds = IntArray(0)
                    width = bounds.outWidth.coerceAtLeast(0)
                    height = bounds.outHeight.coerceAtLeast(0)
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

    fun send(text: String) = send(TdApi.FormattedText(text.trim(), emptyArray()))

    fun send(formatted: TdApi.FormattedText, markdown: Boolean = false, options: TdApi.MessageSendOptions? = null) {
        if (formatted.text.isEmpty()) return
        val reply = ui.value.replyTo
        ui.value = ui.value.copy(replyTo = null)
        scope.launch {
            val formatted = if (markdown) runCatching { graph.td.send(TdApi.ParseMarkdown(formatted)) }.getOrDefault(formatted) else formatted
            runCatching {
                graph.td.send(TdApi.SendMessage().apply {
                    chatId = this@ChatController.chatId
                    topicId = topic
                    replyTo = reply?.let { TdApi.InputMessageReplyToMessage().apply { messageId = it.id } }
                    this.options = options
                    inputMessageContent = TdApi.InputMessageText().apply {
                        val premium = graph.store.snapshot.value.let { it.users[it.meId]?.premium } == true
                        this.text = TdApi.FormattedText(formatted.text, com.pixelgram.android.td.LocalPremiumEmoji.outgoing(formatted.entities, premium))
                        clearDraft = true
                    }
                })
            }
        }
    }

    fun sendContent(content: TdApi.InputMessageContent, options: TdApi.MessageSendOptions? = null) {
        val reply = ui.value.replyTo
        ui.value = ui.value.copy(replyTo = null)
        scope.launch {
            runCatching {
                graph.td.send(TdApi.SendMessage().apply {
                    chatId = this@ChatController.chatId
                    topicId = topic
                    replyTo = reply?.let { TdApi.InputMessageReplyToMessage().apply { messageId = it.id } }
                    this.options = options
                    inputMessageContent = content
                })
            }
        }
    }

    val scheduled = kotlinx.coroutines.flow.MutableStateFlow<List<TdApi.Message>>(emptyList())

    fun loadScheduled() {
        scope.launch {
            scheduled.value = runCatching { graph.td.send(TdApi.GetChatScheduledMessages(chatId)).messages.toList() }.getOrDefault(emptyList()).sortedBy { (it.schedulingState as? TdApi.MessageSchedulingStateSendAtDate)?.sendDate ?: Int.MAX_VALUE }
        }
    }

    fun reschedule(messageId: Long, state: TdApi.MessageSchedulingState?) {
        scope.launch {
            runCatching { graph.td.send(TdApi.EditMessageSchedulingState(chatId, messageId, state)) }
            kotlinx.coroutines.delay(300)
            loadScheduled()
        }
    }

    fun deleteScheduled(messageId: Long) {
        scope.launch {
            runCatching { graph.td.send(TdApi.DeleteMessages(chatId, longArrayOf(messageId), true)) }
            loadScheduled()
        }
    }

    fun editScheduledText(messageId: Long, text: String) {
        scope.launch {
            runCatching { graph.td.send(TdApi.EditMessageText(chatId, messageId, null, TdApi.InputMessageText().apply { this.text = TdApi.FormattedText(text, emptyArray()) })) }
            loadScheduled()
        }
    }

    fun sendVideoAsNote(path: String) {
        val retriever = android.media.MediaMetadataRetriever()
        var duration = 0
        var side = 384
        runCatching {
            retriever.setDataSource(path)
            duration = ((retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) / 1000L).toInt()
            val w = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            if (w > 0 && h > 0) side = minOf(w, h, 640)
        }
        runCatching { retriever.release() }
        sendContent(TdApi.InputMessageVideoNote().apply {
            videoNote = TdApi.InputVideoNote().apply {
                videoNote = TdApi.InputFileLocal(path)
                this.duration = duration.coerceIn(1, 60)
                length = side
            }
        })
    }

    fun sendVideoNote(note: VideoNote) {
        sendContent(TdApi.InputMessageVideoNote().apply {
            videoNote = TdApi.InputVideoNote().apply {
                videoNote = TdApi.InputFileLocal(note.path)
                duration = note.duration
                length = note.length
            }
        })
    }

    fun sendVoice(recording: VoiceRecording) {
        sendContent(TdApi.InputMessageVoiceNote().apply {
            voiceNote = TdApi.InputVoiceNote().apply {
                voiceNote = TdApi.InputFileLocal(recording.path)
                duration = recording.duration
                waveform = recording.waveform
            }
            caption = TdApi.FormattedText("", emptyArray())
        })
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun sendLocation(context: android.content.Context, livePeriod: Int) {
        val manager = context.getSystemService(android.location.LocationManager::class.java) ?: return
        val provider = listOf(android.location.LocationManager.GPS_PROVIDER, android.location.LocationManager.NETWORK_PROVIDER).firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
            ?: android.location.LocationManager.PASSIVE_PROVIDER
        val send: (android.location.Location) -> Unit = { location ->
            val point = TdApi.Location(location.latitude, location.longitude, location.accuracy.toDouble())
            sendContent(
                if (livePeriod > 0) TdApi.InputMessageLiveLocation(TdApi.LiveLocation(point, livePeriod, 0, 0))
                else TdApi.InputMessageLocation(point),
            )
        }
        val last = runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            runCatching {
                manager.getCurrentLocation(provider, null, context.mainExecutor) { location -> (location ?: last)?.let(send) }
            }.onFailure { last?.let(send) }
        } else {
            last?.let(send) ?: runCatching {
                @Suppress("DEPRECATION")
                manager.requestSingleUpdate(provider, { location -> send(location) }, android.os.Looper.getMainLooper())
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
        if (com.pixelgram.android.notify.AppVisibility.openChat == chatId) com.pixelgram.android.notify.AppVisibility.openChat = 0L
        graph.td.fire(TdApi.CloseChat(chatId))
        scope.cancel()
    }
}

internal fun TdApi.Message.toUi(readOutbox: Long): UiMessage {
    val (text, entities) = when (val c = content) {
        is TdApi.MessageText -> c.text.text to c.text.entities.toList()
        is TdApi.MessagePhoto -> c.caption.text to c.caption.entities.toList()
        is TdApi.MessageVideo -> c.caption.text to c.caption.entities.toList()
        is TdApi.MessageAnimation -> c.caption.text to c.caption.entities.toList()
        is TdApi.MessageDocument -> c.caption.text to c.caption.entities.toList()
        is TdApi.MessageVoiceNote -> c.caption.text to c.caption.entities.toList()
        is TdApi.MessageAudio -> c.caption.text to c.caption.entities.toList()
        is TdApi.MessageVideoNote, is TdApi.MessageSticker -> "" to emptyList()
        else -> (if (isRichContent(content) || isService(content)) "" else contentPreview(content).second) to emptyList()
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
        outgoing = isOutgoing && !isChannelPost,
        date = date,
        text = text,
        entities = com.pixelgram.android.td.LocalPremiumEmoji.incoming(text, entities),
        content = content,
        replyToId = (replyTo as? TdApi.MessageReplyToMessage)?.messageId ?: 0,
        replyQuote = (replyTo as? TdApi.MessageReplyToMessage)?.quote?.text?.text.orEmpty(),
        guestCallerId = when (val caller = guestBotCallerId) {
            is TdApi.MessageSenderUser -> caller.userId
            is TdApi.MessageSenderChat -> caller.chatId
            else -> 0L
        },
        pending = sendingState is TdApi.MessageSendingStatePending,
        failed = sendingState is TdApi.MessageSendingStateFailed,
        edited = editDate > 0,
        read = isOutgoing && !isChannelPost && id <= readOutbox,
        service = isService(content),
        mediaFileId = size?.photo?.id ?: (sticker?.thumbnail?.file?.id ?: 0),
        mediaThumb = photo?.minithumbnail?.data,
        mediaWidth = size?.width ?: sticker?.width ?: 0,
        mediaHeight = size?.height ?: sticker?.height ?: 0,
        albumId = if (photo != null) mediaAlbumId else 0L,
        localPath = size?.photo?.local?.takeIf { it.isDownloadingCompleted }?.path,
        reactions = interactionInfo?.reactions?.reactions.orEmpty().mapNotNull { reaction ->
            val key = when (val type = reaction.type) {
                is TdApi.ReactionTypeEmoji -> type.emoji
                is TdApi.ReactionTypeCustomEmoji -> "ce:" + type.customEmojiId
                else -> null
            }
            key?.let { ReactionUi(it, reaction.totalCount, reaction.isChosen, reaction.recentSenderIds.orEmpty().mapNotNull { sender -> (sender as? TdApi.MessageSenderUser)?.userId }) }
        },
        views = if (isChannelPost) interactionInfo?.viewCount ?: 0 else 0,
        comments = if (isChannelPost) interactionInfo?.replyInfo?.replyCount ?: -1 else -1,
        commenters = interactionInfo?.replyInfo?.recentReplierIds?.mapNotNull { (it as? TdApi.MessageSenderUser)?.userId ?: (it as? TdApi.MessageSenderChat)?.chatId }.orEmpty(),
        viaBotId = viaBotUserId,
        markup = replyMarkup,
        chatId = chatId,
    )
}

private fun isService(content: TdApi.MessageContent): Boolean = when (content) {
    is TdApi.MessageText, is TdApi.MessagePhoto, is TdApi.MessageVideo, is TdApi.MessageAnimation,
    is TdApi.MessageSticker, is TdApi.MessageVoiceNote, is TdApi.MessageVideoNote, is TdApi.MessageAudio,
    is TdApi.MessageDocument, is TdApi.MessagePoll, is TdApi.MessageLocation, is TdApi.MessageContact,
    is TdApi.MessageUnsupported -> false
    else -> !isRichContent(content)
}

private const val RevealPages = 30

object ChatUiMapper {
    fun TdApi.Message.ui(): UiMessage = toUi(Long.MAX_VALUE).copy(chatId = chatId)
}

internal fun isVideoPath(path: String): Boolean = path.substringAfterLast('.', "").lowercase() in setOf("mp4", "mov", "webm", "mkv", "3gp", "m4v")
