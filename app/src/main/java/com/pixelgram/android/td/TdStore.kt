package com.pixelgram.android.td

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

enum class ChatKind { Private, Bot, Group, Channel, Secret, Saved }

@Immutable
data class ChatRow(
    val id: Long,
    val title: String,
    val kind: ChatKind,
    val userId: Long,
    val photoFileId: Int,
    val photoPath: String?,
    val minithumb: ByteArray?,
    val preview: MessagePreview?,
    val date: Int,
    val outgoing: Boolean,
    val unread: Int,
    val mentions: Int,
    val pinned: Boolean,
    val muted: Boolean,
    val verified: Boolean,
    val online: Boolean,
    val order: Long,
)

@Immutable
data class FolderInfo(val id: Int, val title: String, val icon: String?)

@Immutable
data class UnreadInfo(val chats: Int, val unmuted: Int)

@Immutable
data class UserInfo(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val username: String?,
    val phone: String,
    val online: Boolean,
    val lastSeen: Int,
    val photoFileId: Int,
    val bigPhotoFileId: Int = 0,
    val minithumb: ByteArray? = null,
    val premium: Boolean,
    val verified: Boolean,
    val bot: Boolean,
) {
    val name: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Deleted account" }
}

@Immutable
data class StoreSnapshot(
    val lists: Map<String, List<ChatRow>> = emptyMap(),
    val folders: List<FolderInfo> = emptyList(),
    val users: Map<Long, UserInfo> = emptyMap(),
    val files: Map<Int, String> = emptyMap(),
    val meId: Long = 0,
    val unread: Map<String, UnreadInfo> = emptyMap(),
)

const val LIST_MAIN = "main"
const val LIST_ARCHIVE = "archive"
fun folderKey(id: Int) = "folder:$id"

private fun TdApi.ChatList.key(): String = when (this) {
    is TdApi.ChatListMain -> LIST_MAIN
    is TdApi.ChatListArchive -> LIST_ARCHIVE
    is TdApi.ChatListFolder -> folderKey(chatFolderId)
    else -> "other"
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class TdStore(private val td: TdClient, private val scope: CoroutineScope) {
    private val worker = Dispatchers.Default.limitedParallelism(1)
    private val chats = HashMap<Long, TdApi.Chat>()
    private val users = HashMap<Long, TdApi.User>()
    private val files = HashMap<Int, String>()
    private var folders: List<FolderInfo> = emptyList()
    private val requested = HashSet<Int>()
    private var meId = 0L
    private val unread = HashMap<String, UnreadInfo>()

    private val dirty = MutableStateFlow(0L)
    private val snapshotFlow = MutableStateFlow(StoreSnapshot())
    val snapshot: StateFlow<StoreSnapshot> = snapshotFlow.asStateFlow()

    init {
        scope.launch(worker) { td.updates.collect(::apply) }
        scope.launch(worker) { dirty.sample(50).collect { publish() } }
    }

    private fun touch() {
        dirty.value = dirty.value + 1
    }

    private fun apply(update: TdApi.Object) {
        when (update) {
            is TdApi.UpdateNewChat -> chats[update.chat.id] = update.chat
            is TdApi.UpdateChatTitle -> chats[update.chatId]?.title = update.title
            is TdApi.UpdateChatPhoto -> chats[update.chatId]?.photo = update.photo
            is TdApi.UpdateChatLastMessage -> chats[update.chatId]?.let {
                it.lastMessage = update.lastMessage
                for (position in update.positions) setPosition(it, position)
            }
            is TdApi.UpdateChatPosition -> chats[update.chatId]?.let { setPosition(it, update.position) }
            is TdApi.UpdateChatReadInbox -> chats[update.chatId]?.let {
                it.lastReadInboxMessageId = update.lastReadInboxMessageId
                it.unreadCount = update.unreadCount
            }
            is TdApi.UpdateChatReadOutbox -> chats[update.chatId]?.lastReadOutboxMessageId = update.lastReadOutboxMessageId
            is TdApi.UpdateChatUnreadMentionCount -> chats[update.chatId]?.unreadMentionCount = update.unreadMentionCount
            is TdApi.UpdateChatNotificationSettings -> chats[update.chatId]?.notificationSettings = update.notificationSettings
            is TdApi.UpdateUser -> users[update.user.id] = update.user
            is TdApi.UpdateUserStatus -> users[update.userId]?.status = update.status
            is TdApi.UpdateFile -> {
                val file = update.file
                if (file.local.isDownloadingCompleted) files[file.id] = file.local.path
            }
            is TdApi.UpdateChatFolders -> folders = update.chatFolders.map {
                FolderInfo(it.id, it.name.text.text, it.icon?.name)
            }
            is TdApi.UpdateUnreadChatCount -> unread[update.chatList.key()] = UnreadInfo(update.unreadCount, update.unreadUnmutedCount)
            is TdApi.UpdateOption -> if (update.name == "my_id") {
                meId = (update.value as? TdApi.OptionValueInteger)?.value ?: 0
            }
            else -> return
        }
        touch()
    }

    private fun setPosition(chat: TdApi.Chat, position: TdApi.ChatPosition) {
        val key = position.list.key()
        val rest = chat.positions.filterNot { it.list.key() == key }
        chat.positions = if (position.order == 0L) rest.toTypedArray() else (rest + position).toTypedArray()
    }

    private fun publish() {
        val lists = HashMap<String, MutableList<ChatRow>>()
        for (chat in chats.values) {
            for (position in chat.positions) {
                if (position.order == 0L) continue
                lists.getOrPut(position.list.key()) { ArrayList() } += row(chat, position)
            }
        }
        val sorted = lists.mapValues { (_, rows) -> rows.sortedByDescending { it.order } }
        val userInfo = users.mapValues { (_, user) -> user.info() }
        snapshotFlow.value = StoreSnapshot(sorted, folders, userInfo, HashMap(files), meId, HashMap(unread))
        val wanted = sorted[LIST_MAIN].orEmpty().take(60).mapNotNull { it.photoFileId.takeIf { id -> id != 0 } }
        for (id in wanted) requestFile(id)
    }

    private fun row(chat: TdApi.Chat, position: TdApi.ChatPosition): ChatRow {
        val type = chat.type
        val userId = when (type) {
            is TdApi.ChatTypePrivate -> type.userId
            is TdApi.ChatTypeSecret -> type.userId
            else -> 0L
        }
        val user = users[userId]
        val kind = when {
            userId != 0L && userId == meId -> ChatKind.Saved
            type is TdApi.ChatTypeSecret -> ChatKind.Secret
            type is TdApi.ChatTypePrivate && user?.type is TdApi.UserTypeBot -> ChatKind.Bot
            type is TdApi.ChatTypePrivate -> ChatKind.Private
            type is TdApi.ChatTypeSupergroup && type.isChannel -> ChatKind.Channel
            else -> ChatKind.Group
        }
        val photoId = chat.photo?.small?.id ?: 0
        val last = chat.lastMessage
        return ChatRow(
            id = chat.id,
            title = chat.title,
            kind = kind,
            userId = userId,
            photoFileId = photoId,
            photoPath = files[photoId],
            minithumb = chat.photo?.minithumbnail?.data,
            preview = last?.let { previewOf(it, chat, users) },
            date = last?.date ?: 0,
            outgoing = last?.isOutgoing == true,
            unread = chat.unreadCount,
            mentions = chat.unreadMentionCount,
            pinned = position.isPinned,
            muted = chat.notificationSettings.muteFor > 0,
            verified = user?.verificationStatus?.isVerified == true,
            online = user?.status is TdApi.UserStatusOnline,
            order = position.order,
        )
    }

    fun requestFile(fileId: Int, priority: Int = 8) {
        if (fileId == 0 || fileId in files || !requested.add(fileId)) return
        scope.launch(worker) {
            val file = runCatching { td.send(TdApi.DownloadFile(fileId, priority, 0, 0, false)) }.getOrNull()
            if (file != null && file.local.isDownloadingCompleted && file.local.path.isNotEmpty()) {
                files[file.id] = file.local.path
                touch()
            }
        }
    }

    fun requestFileAsync(fileId: Int) {
        scope.launch(worker) { requestFile(fileId, 16) }
    }

    fun chat(chatId: Long): TdApi.Chat? = chats[chatId]
}

private fun TdApi.User.info() = UserInfo(
    id = id,
    firstName = firstName,
    lastName = lastName,
    username = usernames?.activeUsernames?.firstOrNull(),
    phone = phoneNumber,
    online = status is TdApi.UserStatusOnline,
    lastSeen = (status as? TdApi.UserStatusOffline)?.wasOnline ?: 0,
    photoFileId = profilePhoto?.small?.id ?: 0,
    bigPhotoFileId = profilePhoto?.big?.id ?: 0,
    minithumb = profilePhoto?.minithumbnail?.data,
    premium = isPremium,
    verified = verificationStatus?.isVerified == true,
    bot = type is TdApi.UserTypeBot,
)
