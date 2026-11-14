package com.pixelgram.android.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.pixelgram.android.MainActivity
import com.pixelgram.android.PixelApp
import com.pixelgram.android.R
import com.pixelgram.android.td.TdClient
import com.pixelgram.android.td.TdStore
import com.pixelgram.android.td.contentPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.util.concurrent.ConcurrentHashMap

object AppVisibility {
    @Volatile var foreground = false
    @Volatile var openChat = 0L
}

class Notifier(private val context: Context, private val td: TdClient, private val store: TdStore, scope: CoroutineScope) {
    private val manager = NotificationManagerCompat.from(context)
    private val history = ConcurrentHashMap<Long, MutableList<Line>>()
    private val scopes = ConcurrentHashMap<Class<*>, TdApi.ScopeNotificationSettings>()

    private class Line(val text: String, val time: Long, val sender: String, val senderId: Long)

    init {
        createChannels(context)
        scope.launch {
            td.updates.collect { update ->
                when (update) {
                    is TdApi.UpdateNewMessage -> runCatching { onMessage(update.message) }
                    is TdApi.UpdateScopeNotificationSettings -> scopes[update.scope.javaClass] = update.notificationSettings
                    is TdApi.UpdateChatReadInbox -> if (update.unreadCount == 0) clear(update.chatId)
                    else -> Unit
                }
            }
        }
    }

    private suspend fun scopeSettings(chat: TdApi.Chat): TdApi.ScopeNotificationSettings? {
        val scope: TdApi.NotificationSettingsScope = when (val type = chat.type) {
            is TdApi.ChatTypePrivate, is TdApi.ChatTypeSecret -> TdApi.NotificationSettingsScopePrivateChats()
            is TdApi.ChatTypeSupergroup -> if (type.isChannel) TdApi.NotificationSettingsScopeChannelChats() else TdApi.NotificationSettingsScopeGroupChats()
            else -> TdApi.NotificationSettingsScopeGroupChats()
        }
        scopes[scope.javaClass]?.let { return it }
        return runCatching { td.send(TdApi.GetScopeNotificationSettings(scope)) }.getOrNull()?.also { scopes[scope.javaClass] = it }
    }

    private suspend fun onMessage(message: TdApi.Message) {
        if (message.isOutgoing || message.sendingState != null) return
        if (AppVisibility.foreground && AppVisibility.openChat == message.chatId) return
        if (System.currentTimeMillis() / 1000 - message.date > StaleSeconds) return
        val chat = runCatching { td.send(TdApi.GetChat(message.chatId)) }.getOrNull() ?: store.chat(message.chatId) ?: return
        val settings = chat.notificationSettings
        val muted = if (settings.useDefaultMuteFor) (scopeSettings(chat)?.muteFor ?: 0) > 0 else settings.muteFor > 0
        if (muted) {
            val mentionsOff = if (settings.useDefaultDisableMentionNotifications) scopeSettings(chat)?.disableMentionNotifications ?: false else settings.disableMentionNotifications
            if (!message.containsUnreadMention || mentionsOff) return
        }
        if (message.content is TdApi.MessageChatJoinByLink || message.content is TdApi.MessagePinMessage && chat.type is TdApi.ChatTypeSupergroup) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= 33) return
        val senderId = (message.senderId as? TdApi.MessageSenderUser)?.userId ?: (message.senderId as? TdApi.MessageSenderChat)?.chatId ?: 0L
        val sender = store.snapshot.value.users[senderId]?.name ?: chat.title
        val (kind, text) = contentPreview(message.content)
        val body = text.ifBlank { context.getString(kindLabel(kind)) }
        val lines = history.getOrPut(message.chatId) { mutableListOf() }
        synchronized(lines) {
            lines += Line(body, message.date * 1000L, sender, senderId)
            while (lines.size > MaxLines) lines.removeAt(0)
        }
        show(chat, lines.toList())
    }

    private fun show(chat: TdApi.Chat, lines: List<Line>) {
        val group = chat.type !is TdApi.ChatTypePrivate && chat.type !is TdApi.ChatTypeSecret
        val me = Person.Builder().setName(context.getString(R.string.notify_you)).build()
        val avatar = chatAvatar(chat)
        val style = NotificationCompat.MessagingStyle(me).setGroupConversation(group)
        if (group) style.conversationTitle = chat.title
        lines.forEach { line ->
            val person = Person.Builder().setName(line.sender).setKey(line.senderId.toString())
                .apply { if (!group && avatar != null) setIcon(IconCompat.createWithBitmap(avatar)) }
                .build()
            style.addMessage(line.text, line.time, person)
        }
        val id = chat.id.hashCode()
        val open = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java).setAction(OpenChatAction).putExtra(ExtraChat, chat.id).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val replyInput = RemoteInput.Builder(KeyReply).setLabel(context.getString(R.string.notify_reply)).build()
        val reply = PendingIntent.getBroadcast(
            context, id,
            Intent(context, NotificationActions::class.java).setAction(ReplyAction).putExtra(ExtraChat, chat.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val read = PendingIntent.getBroadcast(
            context, id + 1,
            Intent(context, NotificationActions::class.java).setAction(ReadAction).putExtra(ExtraChat, chat.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, if (group) ChannelGroups else ChannelPrivate)
            .setSmallIcon(R.drawable.ic_pixelgram)
            .setColor(accentColor(context))
            .setColorized(false)
            .setStyle(style)
            .setLargeIcon(avatar)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(GroupKey)
            .setShortcutId(chat.id.toString())
            .setNumber(chat.unreadCount)
            .addAction(NotificationCompat.Action.Builder(R.drawable.ic_pixelgram, context.getString(R.string.notify_reply), reply).addRemoteInput(replyInput).setAllowGeneratedReplies(true).setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY).build())
            .addAction(NotificationCompat.Action.Builder(R.drawable.ic_pixelgram, context.getString(R.string.notify_mark_read), read).setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ).build())
            .build()
        runCatching { manager.notify(id, notification) }
        runCatching {
            manager.notify(
                SummaryId,
                NotificationCompat.Builder(context, if (group) ChannelGroups else ChannelPrivate)
                    .setSmallIcon(R.drawable.ic_pixelgram)
            .setColor(accentColor(context))
            .setColorized(false)
                    .setGroup(GroupKey)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .build(),
            )
        }
    }

    private fun chatAvatar(chat: TdApi.Chat): Bitmap? {
        val file = chat.photo?.small ?: return null
        val path = store.snapshot.value.files[file.id] ?: file.local.path.takeIf { file.local.isDownloadingCompleted && it.isNotEmpty() }
        if (path == null) {
            store.requestFileAsync(file.id)
            return null
        }
        val bitmap = runCatching { BitmapFactory.decodeFile(path) }.getOrNull() ?: return null
        return circle(bitmap)
    }

    private fun circle(source: Bitmap): Bitmap {
        val size = minOf(source.width, source.height)
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, ((size - source.width) / 2f), ((size - source.height) / 2f), paint)
        return out
    }

    fun clear(chatId: Long) {
        history.remove(chatId)
        runCatching { manager.cancel(chatId.hashCode()) }
        if (history.isEmpty()) runCatching { manager.cancel(SummaryId) }
    }

    fun replied(chatId: Long, text: String) {
        val chat = store.chat(chatId) ?: return
        val lines = history.getOrPut(chatId) { mutableListOf() }
        synchronized(lines) { lines += Line(text, System.currentTimeMillis(), context.getString(R.string.notify_you), 0L) }
        show(chat, lines.toList())
    }

    private fun kindLabel(kind: com.pixelgram.android.td.PreviewKind): Int = when (kind) {
        com.pixelgram.android.td.PreviewKind.Photo -> R.string.preview_photo
        com.pixelgram.android.td.PreviewKind.Video -> R.string.notify_video
        com.pixelgram.android.td.PreviewKind.Voice -> R.string.notify_voice
        com.pixelgram.android.td.PreviewKind.Sticker -> R.string.notify_sticker
        else -> R.string.notify_message
    }

    companion object {
        const val ChannelPrivate = "messages_private"
        const val ChannelGroups = "messages_groups"
        const val ChannelService = "connection"
        const val OpenChatAction = "com.pixelgram.android.OPEN_CHAT"
        const val ReplyAction = "com.pixelgram.android.REPLY"
        const val ReadAction = "com.pixelgram.android.READ"
        const val ExtraChat = "chat_id"
        const val KeyReply = "reply"
        private const val GroupKey = "pixelgram_messages"
        private const val SummaryId = 1
        private const val MaxLines = 7
        private const val StaleSeconds = 300

        fun createChannels(context: Context) {
            if (Build.VERSION.SDK_INT < 26) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(NotificationChannel(ChannelPrivate, context.getString(R.string.notify_channel_private), NotificationManager.IMPORTANCE_HIGH))
            manager.createNotificationChannel(NotificationChannel(ChannelGroups, context.getString(R.string.notify_channel_groups), NotificationManager.IMPORTANCE_HIGH))
            manager.createNotificationChannel(NotificationChannel(ChannelService, context.getString(R.string.notify_channel_connection), NotificationManager.IMPORTANCE_MIN).apply { setShowBadge(false) })
        }
    }
}

class NotificationActions : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val graph = (context.applicationContext as PixelApp).graph
        val chatId = intent.getLongExtra(Notifier.ExtraChat, 0L).takeIf { it != 0L } ?: return
        val pending = goAsync()
        graph.scope.launch {
            runCatching {
                when (intent.action) {
                    Notifier.ReplyAction -> {
                        val text = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(Notifier.KeyReply)?.toString()?.trim().orEmpty()
                        if (text.isNotEmpty()) {
                            graph.td.send(TdApi.SendMessage().apply {
                                this.chatId = chatId
                                inputMessageContent = TdApi.InputMessageText().apply { this.text = TdApi.FormattedText(text, emptyArray()) }
                            })
                            markRead(graph, chatId)
                            graph.notifier.clear(chatId)
                        }
                    }
                    Notifier.ReadAction -> {
                        markRead(graph, chatId)
                        graph.notifier.clear(chatId)
                    }
                }
            }
            pending.finish()
        }
    }

    private suspend fun markRead(graph: com.pixelgram.android.AppGraph, chatId: Long) {
        val last = graph.store.chat(chatId)?.lastMessage?.id ?: return
        graph.td.send(TdApi.OpenChat(chatId))
        graph.td.send(TdApi.ViewMessages(chatId, longArrayOf(last), null, true))
        graph.td.send(TdApi.CloseChat(chatId))
    }
}

class KeepAliveService : android.app.Service() {
    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifier.createChannels(this)
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, Notifier.ChannelService)
            .setSmallIcon(R.drawable.ic_pixelgram)
            .setColor(accentColor(this))
            .setColorized(false)
            .setContentTitle(getString(R.string.notify_connected))
            .setContentText(getString(R.string.notify_connected_sub))
            .setContentIntent(open)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        runCatching {
            if (Build.VERSION.SDK_INT >= 34) startForeground(ServiceId, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING)
            else startForeground(ServiceId, notification)
        }
        return START_STICKY
    }

    companion object {
        private const val ServiceId = 7

        fun apply(context: Context, enabled: Boolean) {
            val intent = Intent(context, KeepAliveService::class.java)
            runCatching { if (enabled) ContextCompat.startForegroundService(context, intent) else context.stopService(intent) }
        }
    }
}

internal fun accentColor(context: Context): Int =
    if (Build.VERSION.SDK_INT >= 31) context.getColor(android.R.color.system_accent1_400) else 0xFF7C9A5A.toInt()
