package com.pixelgram.android.td

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.drinkless.tdlib.TdApi

object EmojiTaps {
    private val state = MutableStateFlow<Map<Pair<Long, Long>, TdApi.Sticker>>(emptyMap())
    val remote: StateFlow<Map<Pair<Long, Long>, TdApi.Sticker>> = state.asStateFlow()

    @Volatile
    var client: TdClient? = null

    fun received(chatId: Long, messageId: Long, sticker: TdApi.Sticker) = state.update { it + ((chatId to messageId) to sticker) }

    fun consume(key: Pair<Long, Long>) = state.update { it - key }

    suspend fun click(chatId: Long, messageId: Long): TdApi.Sticker? =
        runCatching { client?.send(TdApi.ClickAnimatedEmojiMessage(chatId, messageId)) }.getOrNull()
}
