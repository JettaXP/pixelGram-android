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

class TdClient(private val context: Context, private val scope: CoroutineScope) {
    private val updateFlow = MutableSharedFlow<TdApi.Object>(extraBufferCapacity = 4096)
    val updates: SharedFlow<TdApi.Object> = updateFlow.asSharedFlow()

    private val authFlow = MutableStateFlow<TdApi.AuthorizationState?>(null)
    val authorizationState: StateFlow<TdApi.AuthorizationState?> = authFlow.asStateFlow()

    private val client: Client

    init {
        Client.execute(TdApi.SetLogVerbosityLevel(if (BuildConfig.DEBUG) 2 else 1))
        client = Client.create(::onUpdate, null, null)
    }

    private fun onUpdate(update: TdApi.Object) {
        if (update is TdApi.UpdateAuthorizationState) {
            authFlow.value = update.authorizationState
            if (update.authorizationState is TdApi.AuthorizationStateWaitTdlibParameters) {
                scope.launch { runCatching { send(parameters()) } }
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
        apiId = BuildConfig.API_ID
        apiHash = BuildConfig.API_HASH
        systemLanguageCode = Locale.getDefault().toLanguageTag()
        deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        systemVersion = "Android ${Build.VERSION.RELEASE}"
        applicationVersion = "pixelGram ${BuildConfig.VERSION_NAME}"
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : TdApi.Object> send(function: TdApi.Function<T>): T =
        suspendCancellableCoroutine { continuation ->
            client.send(function) { result ->
                if (result is TdApi.Error) {
                    continuation.resumeWithException(TdException(result))
                } else {
                    continuation.resume(result as T)
                }
            }
        }

    fun fire(function: TdApi.Function<*>) {
        client.send(function, null)
    }
}
