package com.pixelgram.android.td

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import org.drinkless.tdlib.TdApi
import java.util.concurrent.ConcurrentHashMap

class PaidRequest(val chatId: Long, val title: String, val price: Long, val count: Int, val answer: CompletableDeferred<Boolean> = CompletableDeferred())

object PaidMessages {
    private class Price(val stars: Long, val at: Long)

    private val prices = ConcurrentHashMap<Long, Price>()
    val pending = MutableStateFlow<PaidRequest?>(null)
    val known = MutableStateFlow<Map<Long, Long>>(emptyMap())

    private fun prefs(context: Context) = context.getSharedPreferences("paid_messages", Context.MODE_PRIVATE)

    fun skipConfirm(context: Context, chatId: Long): Boolean = prefs(context).getBoolean("skip_$chatId", false)

    fun setSkipConfirm(context: Context, chatId: Long, value: Boolean) = prefs(context).edit().putBoolean("skip_$chatId", value).apply()

    fun forget(chatId: Long) {
        prices.remove(chatId)
    }

    suspend fun price(chatId: Long, query: suspend (TdApi.Function<*>) -> TdApi.Object?): Long {
        val now = System.currentTimeMillis()
        prices[chatId]?.takeIf { now - it.at < CacheMs }?.let { return it.stars }
        val chat = query(TdApi.GetChat(chatId)) as? TdApi.Chat ?: return 0
        val stars = when (val type = chat.type) {
            is TdApi.ChatTypePrivate -> (query(TdApi.GetUserFullInfo(type.userId)) as? TdApi.UserFullInfo)?.outgoingPaidMessageStarCount ?: 0
            is TdApi.ChatTypeSupergroup -> (query(TdApi.GetSupergroupFullInfo(type.supergroupId)) as? TdApi.SupergroupFullInfo)?.outgoingPaidMessageStarCount ?: 0
            else -> 0
        }
        prices[chatId] = Price(stars, now)
        known.value = known.value + (chatId to stars)
        return stars
    }

    suspend fun prepare(context: Context, function: TdApi.Function<*>, query: suspend (TdApi.Function<*>) -> TdApi.Object?): Boolean {
        val target: Triple<Long, Int, TdApi.MessageSendOptions?> = when (function) {
            is TdApi.SendMessage -> Triple(function.chatId, 1, function.options)
            is TdApi.SendMessageAlbum -> Triple(function.chatId, function.inputMessageContents.size, function.options)
            is TdApi.ForwardMessages -> Triple(function.chatId, function.messageIds.size, function.options)
            else -> return true
        }
        val (chatId, count, options) = target
        if (options?.schedulingState != null || (options?.paidMessageStarCount ?: 0L) > 0L) return true
        val stars = price(chatId, query)
        if (stars <= 0L) return true
        if (!skipConfirm(context, chatId)) {
            val chat = query(TdApi.GetChat(chatId)) as? TdApi.Chat
            val request = PaidRequest(chatId, chat?.title.orEmpty(), stars, count)
            pending.value?.answer?.complete(false)
            pending.value = request
            val ok = try { request.answer.await() } finally { if (pending.value === request) pending.value = null }
            if (!ok) return false
        }
        val paid = (options ?: TdApi.MessageSendOptions()).apply { paidMessageStarCount = stars * count }
        when (function) {
            is TdApi.SendMessage -> function.options = paid
            is TdApi.SendMessageAlbum -> function.options = paid
            is TdApi.ForwardMessages -> function.options = paid
            else -> Unit
        }
        return true
    }

    private const val CacheMs = 60_000L
}
