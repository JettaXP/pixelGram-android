package com.pixelgram.android.td

import android.content.Context
import com.pixelgram.android.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import org.drinkless.tdlib.TdApi

class SensitiveRequest(val function: TdApi.Function<*>, val category: Int, val fields: List<Pair<String, String>>, val method: String) {
    val answer = CompletableDeferred<Boolean>()
}

object SensitiveRequests {
    private const val Prefs = "pixel_security"
    private const val Key = "confirm_sensitive"

    val pending = MutableStateFlow<SensitiveRequest?>(null)

    private val categories: Map<Class<*>, Int> = mapOf(
        TdApi.TransferGift::class.java to R.string.sensitive_gifts,
        TdApi.SellGift::class.java to R.string.sensitive_gifts,
        TdApi.UpgradeGift::class.java to R.string.sensitive_gifts,
        TdApi.SetGiftResalePrice::class.java to R.string.sensitive_gifts,
        TdApi.SendGiftPurchaseOffer::class.java to R.string.sensitive_gifts,
        TdApi.DropGiftOriginalDetails::class.java to R.string.sensitive_gifts,
        TdApi.SendGift::class.java to R.string.sensitive_gifts,
        TdApi.SendPaymentForm::class.java to R.string.sensitive_payments,
        TdApi.AddPendingPaidMessageReaction::class.java to R.string.sensitive_payments,
        TdApi.GetStarWithdrawalUrl::class.java to R.string.sensitive_payments,
        TdApi.TerminateSession::class.java to R.string.sensitive_security,
        TdApi.TerminateAllOtherSessions::class.java to R.string.sensitive_security,
        TdApi.DeleteAccount::class.java to R.string.sensitive_security,
        TdApi.SetPassword::class.java to R.string.sensitive_security,
        TdApi.ResetPassword::class.java to R.string.sensitive_security,
        TdApi.SetUsername::class.java to R.string.sensitive_security,
        TdApi.ToggleUsernameIsActive::class.java to R.string.sensitive_security,
        TdApi.DisconnectWebsite::class.java to R.string.sensitive_security,
        TdApi.DisconnectAllWebsites::class.java to R.string.sensitive_security,
        TdApi.ConfirmQrCodeAuthentication::class.java to R.string.sensitive_security,
        TdApi.SetInactiveSessionTtl::class.java to R.string.sensitive_security,
        TdApi.TransferChatOwnership::class.java to R.string.sensitive_chats,
        TdApi.DeleteChat::class.java to R.string.sensitive_chats,
        TdApi.SetSupergroupUsername::class.java to R.string.sensitive_chats,
    )

    fun enabled(context: Context): Boolean = context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).getBoolean(Key, true)

    fun setEnabled(context: Context, value: Boolean) {
        context.getSharedPreferences(Prefs, Context.MODE_PRIVATE).edit().putBoolean(Key, value).apply()
    }

    fun isSensitive(function: TdApi.Function<*>): Boolean = categories.containsKey(function.javaClass)

    suspend fun confirm(function: TdApi.Function<*>): Boolean {
        val category = categories[function.javaClass] ?: return true
        val fields = function.javaClass.fields
            .filter { !java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .mapNotNull { field ->
                val value = runCatching { field.get(function) }.getOrNull() ?: return@mapNotNull null
                val text = when (value) {
                    is String -> value.takeIf { it.isNotBlank() }?.let { if (field.name.contains("password", true)) "••••••" else it }
                    is Number -> value.toString().takeIf { it != "0" }
                    is Boolean -> if (value) "✓" else null
                    is TdApi.MessageSenderUser -> value.userId.toString()
                    is TdApi.MessageSenderChat -> value.chatId.toString()
                    is TdApi.Object -> value.javaClass.simpleName
                    else -> null
                } ?: return@mapNotNull null
                label(field.name) to text
            }
        val method = function.javaClass.simpleName.replaceFirstChar { it.lowercase() }
        val request = SensitiveRequest(function, category, fields, method)
        pending.value?.answer?.complete(false)
        pending.value = request
        return try {
            request.answer.await()
        } finally {
            if (pending.value === request) pending.value = null
        }
    }

    private fun label(name: String): String = name.replace(Regex("([a-z])([A-Z])"), "$1 $2").replaceFirstChar { it.uppercase() }.replace(" Id", " ID")
}
