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
    val lastMessageId: Long = 0,
    val list: String = LIST_MAIN,
    val typing: Boolean = false,
    val deleted: Boolean = false,
    val scam: Boolean = false,
    val fake: Boolean = false,
    val forum: Boolean = false,
    val official: Boolean = false,
)

@Immutable
data class FolderInfo(val id: Int, val title: String, val icon: String?)

@Immutable
data class UnreadInfo(val chats: Int, val unmuted: Int)

enum class Seen { Online, Offline, Recently, Week, Month, LongAgo }

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
    val localPhoto: String? = null,
    val contact: Boolean = false,
    val scam: Boolean = false,
    val fake: Boolean = false,
    val usernames: List<String> = emptyList(),
    val deleted: Boolean = false,
    val support: Boolean = false,
    val seen: Seen = Seen.Recently,
    val emojiStatus: Long = 0L,
) {
    val name: String get() = if (deleted) "Deleted Account" else listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Deleted Account" }
}

@Immutable
data class StoryOwner(val chatId: Long, val storyIds: List<Int>, val maxRead: Int, val order: Long, val archived: Boolean) {
    val unread: Boolean get() = storyIds.any { it > maxRead }
}

@Immutable
data class FileProgress(val downloaded: Long, val total: Long, val active: Boolean, val done: Boolean)

@Immutable
data class StoreSnapshot(
    val lists: Map<String, List<ChatRow>> = emptyMap(),
    val folders: List<FolderInfo> = emptyList(),
    val users: Map<Long, UserInfo> = emptyMap(),
    val files: Map<Int, String> = emptyMap(),
    val meId: Long = 0,
    val unread: Map<String, UnreadInfo> = emptyMap(),
    val stories: List<StoryOwner> = emptyList(),
    val progress: Map<Int, FileProgress> = emptyMap(),
)

private val OfficialSupergroupIds = setOf(4378271602L)
private const val OfficialChannel = "pixelgramclient"

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
class TdStore(private val td: TdClient, private val scope: CoroutineScope, context: android.content.Context? = null) {
    private val local = context?.getSharedPreferences("local_profiles", android.content.Context.MODE_PRIVATE)

    fun localName(userId: Long): String? = local?.getString("name_$userId", null)

    fun localPhoto(userId: Long): String? = local?.getString("photo_$userId", null)?.takeIf { java.io.File(it).isFile }

    fun setLocalName(userId: Long, name: String?) {
        local?.edit()?.apply { if (name.isNullOrBlank()) remove("name_$userId") else putString("name_$userId", name.trim()) }?.apply()
        touch()
    }

    fun setLocalPhoto(userId: Long, path: String?) {
        local?.edit()?.apply { if (path == null) remove("photo_$userId") else putString("photo_$userId", path) }?.apply()
        touch()
    }

    private val worker = Dispatchers.Default.limitedParallelism(1)
    private val chats = HashMap<Long, TdApi.Chat>()
    private val users = HashMap<Long, TdApi.User>()
    private val files = HashMap<Int, String>()
    private var folders: List<FolderInfo> = emptyList()
    private val requested = HashSet<Int>()
    private var meId = 0L
    private val unread = HashMap<String, UnreadInfo>()
    private val stories = HashMap<Long, StoryOwner>()
    private val localPhotos = HashMap<Long, String>()
    private val progress = HashMap<Int, FileProgress>()
    private val supergroups = HashMap<Long, TdApi.Supergroup>()
    private val typing = HashMap<Long, HashMap<Long, Long>>()

    private val dirty = MutableStateFlow(0L)
    private val snapshotFlow = MutableStateFlow(StoreSnapshot())
    val snapshot: StateFlow<StoreSnapshot> = snapshotFlow.asStateFlow()

    init {
        EmojiTaps.client = td
        scope.launch(worker) { td.updates.collect(::apply) }
        scope.launch(worker) { dirty.sample(50).collect { publish() } }
    }

    private fun touch() {
        dirty.value = dirty.value + 1
    }

    private fun apply(update: TdApi.Object) {
        when (update) {
            is TdApi.UpdateNewChat -> chats[update.chat.id] = update.chat
            is TdApi.UpdateAnimatedEmojiMessageClicked -> EmojiTaps.received(update.chatId, update.messageId, update.sticker)
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
            is TdApi.UpdateSupergroup -> supergroups[update.supergroup.id] = update.supergroup
            is TdApi.UpdateUserStatus -> users[update.userId]?.status = update.status
            is TdApi.UpdateFile -> {
                val file = update.file
                progress[file.id] = FileProgress(file.local.downloadedSize, maxOf(file.size, file.expectedSize), file.local.isDownloadingActive, file.local.isDownloadingCompleted)
                if (file.local.isDownloadingCompleted) files[file.id] = file.local.path else files.remove(file.id)
            }
            is TdApi.UpdateChatFolders -> folders = update.chatFolders.map {
                FolderInfo(it.id, it.name.text.text, it.icon?.name)
            }
            is TdApi.UpdateUnreadChatCount -> unread[update.chatList.key()] = UnreadInfo(update.unreadCount, update.unreadUnmutedCount)
            is TdApi.UpdateChatAction -> {
                val sender = when (val from = update.senderId) {
                    is TdApi.MessageSenderUser -> from.userId
                    is TdApi.MessageSenderChat -> from.chatId
                    else -> 0L
                }
                val actions = typing.getOrPut(update.chatId) { HashMap() }
                if (update.action is TdApi.ChatActionCancel) actions.remove(sender) else actions[sender] = System.currentTimeMillis()
                if (actions.isEmpty()) typing.remove(update.chatId)
                scope.launch(worker) {
                    kotlinx.coroutines.delay(TypingTimeoutMs + 100)
                    val now = System.currentTimeMillis()
                    typing.values.forEach { map -> map.entries.removeAll { now - it.value >= TypingTimeoutMs } }
                    typing.entries.removeAll { it.value.isEmpty() }
                    touch()
                }
            }
            is TdApi.UpdateChatActiveStories -> {
                val active = update.activeStories
                val list = active.list
                if (list == null || active.stories.isEmpty()) {
                    stories.remove(active.chatId)
                } else {
                    stories[active.chatId] = StoryOwner(active.chatId, active.stories.map { it.storyId }, active.maxReadStoryId, active.order, list is TdApi.StoryListArchive)
                }
            }
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
        localPhotos.clear()
        users.keys.forEach { id -> localPhoto(id)?.let { localPhotos[id] = it } }
        val lists = HashMap<String, MutableList<ChatRow>>()
        for (chat in chats.values) {
            for (position in chat.positions) {
                if (position.order == 0L) continue
                lists.getOrPut(position.list.key()) { ArrayList() } += row(chat, position)
            }
        }
        val sorted = lists.mapValues { (_, rows) -> rows.sortedByDescending { it.order } }
        val userInfo = users.mapValues { (id, user) ->
            val info = user.info()
            val name = localName(id)
            val named = if (name != null) info.copy(firstName = name, lastName = "") else info
            localPhoto(id)?.let { named.copy(localPhoto = it) } ?: named
        }

        val storyRow = stories.values.filter { !it.archived }.sortedWith(compareByDescending<StoryOwner> { it.chatId == meId }.thenByDescending { it.order })
        snapshotFlow.value = StoreSnapshot(sorted, folders, userInfo, HashMap(files), meId, HashMap(unread), storyRow, HashMap(progress))
        for (owner in storyRow.take(20)) chats[owner.chatId]?.photo?.small?.id?.let { requestFile(it) }
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
            title = localName(userId).takeIf { userId != 0L } ?: chat.title,
            kind = kind,
            userId = userId,
            photoFileId = photoId,
            photoPath = localPhotos[userId] ?: files[photoId],
            minithumb = chat.photo?.minithumbnail?.data,
            preview = last?.let { previewOf(it, chat, users) },
            date = last?.date ?: 0,
            outgoing = last?.isOutgoing == true && last.isChannelPost != true,
            unread = chat.unreadCount,
            mentions = chat.unreadMentionCount,
            pinned = position.isPinned,
            muted = chat.notificationSettings.muteFor > 0,
            verified = user?.verificationStatus?.isVerified == true || (type as? TdApi.ChatTypeSupergroup)?.let { supergroups[it.supergroupId]?.verificationStatus?.isVerified } == true,
            scam = user?.verificationStatus?.isScam == true || (type as? TdApi.ChatTypeSupergroup)?.let { supergroups[it.supergroupId]?.verificationStatus?.isScam } == true,
            fake = user?.verificationStatus?.isFake == true || (type as? TdApi.ChatTypeSupergroup)?.let { supergroups[it.supergroupId]?.verificationStatus?.isFake } == true,
            online = user?.status is TdApi.UserStatusOnline,
            order = position.order,
            lastMessageId = last?.id ?: 0,
            list = position.list.key(),
            typing = typing[chat.id]?.isNotEmpty() == true,
            deleted = user?.type is TdApi.UserTypeDeleted,
            forum = (type as? TdApi.ChatTypeSupergroup)?.let { supergroups[it.supergroupId]?.isForum } == true,
            official = isOfficial(chat.id),
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

    fun isOfficial(chatId: Long): Boolean {
        val id = (chats[chatId]?.type as? TdApi.ChatTypeSupergroup)?.supergroupId ?: return false
        if (id in OfficialSupergroupIds) return true
        return supergroups[id]?.usernames?.activeUsernames?.any { it.equals(OfficialChannel, ignoreCase = true) } == true
    }

    fun download(fileId: Int) {
        if (fileId == 0) return
        scope.launch(worker) {
            requested.remove(fileId)
            runCatching { td.send(TdApi.DownloadFile(fileId, 32, 0, 0, false)) }
        }
    }

    fun cancelDownload(fileId: Int) {
        scope.launch(worker) {
            runCatching { td.send(TdApi.CancelDownloadFile(fileId, false)) }
            progress[fileId]?.let { progress[fileId] = it.copy(active = false) }
            requested.remove(fileId)
            touch()
        }
    }

    fun markStoriesRead(chatId: Long, storyId: Int) {
        scope.launch(worker) {
            val owner = stories[chatId] ?: return@launch
            if (storyId > owner.maxRead) {
                stories[chatId] = owner.copy(maxRead = storyId)
                touch()
            }
        }
    }
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
    deleted = type is TdApi.UserTypeDeleted,
    contact = isContact,
    scam = verificationStatus?.isScam == true,
    fake = verificationStatus?.isFake == true,
    usernames = usernames?.activeUsernames?.toList().orEmpty(),
    support = isSupport,
    emojiStatus = (emojiStatus?.type as? TdApi.EmojiStatusTypeCustomEmoji)?.customEmojiId ?: 0L,
    seen = when (status) {
        is TdApi.UserStatusOnline -> Seen.Online
        is TdApi.UserStatusOffline -> Seen.Offline
        is TdApi.UserStatusLastWeek -> Seen.Week
        is TdApi.UserStatusLastMonth -> Seen.Month
        is TdApi.UserStatusEmpty -> Seen.LongAgo
        else -> Seen.Recently
    },
)

private const val TypingTimeoutMs = 6_000L
