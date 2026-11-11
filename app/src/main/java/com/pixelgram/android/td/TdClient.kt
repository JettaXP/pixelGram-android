package com.pixelgram.android.td

import android.content.Context
import android.os.Build
import com.pixelgram.android.BuildConfig
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

class TdException(val error: TdApi.Error) : Exception("${error.code}: ${error.message}")

class TdClient(private val context: Context, private val scope: CoroutineScope, offline: Boolean = false) {
    private val updateFlow = MutableSharedFlow<TdApi.Object>(extraBufferCapacity = 4096)
    val updates: SharedFlow<TdApi.Object> = updateFlow.asSharedFlow()

    private val authFlow = MutableStateFlow<TdApi.AuthorizationState?>(null)
    val authorizationState: StateFlow<TdApi.AuthorizationState?> = authFlow.asStateFlow()

    private val keys = context.getSharedPreferences("tdlib_keys", Context.MODE_PRIVATE)
    private val keysFlow = MutableStateFlow(apiId() != 0 && apiHash().isNotBlank())
    val hasKeys: StateFlow<Boolean> = keysFlow.asStateFlow()
    private var waitingForKeys = false

    private fun apiId(): Int = BuildConfig.API_ID.takeIf { it != 0 } ?: keys.getInt("api_id", 0)

    private fun apiHash(): String = BuildConfig.API_HASH.ifBlank { keys.getString("api_hash", "").orEmpty() }

    private val client: Client? = if (offline) {
        null
    } else {
        Client.execute(TdApi.SetLogVerbosityLevel(if (BuildConfig.DEBUG) 2 else 1))
        Client.create(::onUpdate, null, null)
    }

    var offlineReply: ((TdApi.Function<*>) -> TdApi.Object?)? = null

    fun emit(update: TdApi.Object) = onUpdate(update)

    fun setKeys(id: Int, hash: String) {
        keys.edit().putInt("api_id", id).putString("api_hash", hash).apply()
        val send = synchronized(this) {
            keysFlow.value = true
            waitingForKeys.also { waitingForKeys = false }
        }
        if (send) scope.launch { runCatching { send(parameters()) } }
    }

    private fun onUpdate(update: TdApi.Object) {
        if (update is TdApi.UpdateAuthorizationState) {
            authFlow.value = update.authorizationState
            if (update.authorizationState is TdApi.AuthorizationStateWaitTdlibParameters) {
                val ready = synchronized(this) { keysFlow.value.also { if (!it) waitingForKeys = true } }
                if (ready) scope.launch { runCatching { send(parameters()) } }
            }
        }
        updateFlow.tryEmit(update)
    }

    private fun parameters() = TdApi.SetTdlibParameters().apply {
        val root = File(context.filesDir, "tdlib")
        useTestDc = false
        databaseDirectory = root.absolutePath
        filesDirectory = File(context.filesDir, "tdlib-files").absolutePath
        databaseEncryptionKey = ByteArray(0)
        useFileDatabase = true
        useChatInfoDatabase = true
        useMessageDatabase = true
        useSecretChats = true
        apiId = apiId()
        apiHash = apiHash()
        systemLanguageCode = Locale.getDefault().language.ifBlank { "en" }
        deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        systemVersion = "Android ${Build.VERSION.RELEASE}"
        applicationVersion = BuildConfig.VERSION_NAME
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : TdApi.Object> send(function: TdApi.Function<T>): T =
        suspendCancellableCoroutine { continuation ->
            val client = client ?: return@suspendCancellableCoroutine when (val reply = offlineReply?.invoke(function)) {
                null, is TdApi.Error -> continuation.resumeWithException(TdException(reply as? TdApi.Error ?: TdApi.Error(406, "OFFLINE")))
                else -> continuation.resume(reply as T)
            }
            client.send(function) { result ->
                if (result is TdApi.Error) {
                    continuation.resumeWithException(TdException(result))
                } else {
                    continuation.resume(result as T)
                }
            }
        }

    fun fire(function: TdApi.Function<*>) {
        client?.send(function, null)
    }
}
