package com.pixelgram.android.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelgram.android.LocalGraph
import com.pixelgram.android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi

object DebugFlags {
    private const val Prefs = "pixel_debug"
    fun get(context: android.content.Context, key: String) = context.getSharedPreferences(Prefs, android.content.Context.MODE_PRIVATE).getBoolean(key, false)
    fun toggle(context: android.content.Context, key: String): Boolean {
        val next = !get(context, key)
        context.getSharedPreferences(Prefs, android.content.Context.MODE_PRIVATE).edit().putBoolean(key, next).apply()
        return next
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugMenu(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val graph = LocalGraph.current
    val toast: (String) -> Unit = { android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show() }
    val done = stringResource(R.string.privacy_done)
    fun run(block: suspend () -> Unit) {
        graph.scope.launch {
            runCatching { block() }
                .onSuccess { withContext(Dispatchers.Main) { toast(done) } }
                .onFailure { withContext(Dispatchers.Main) { toast(it.message ?: "Error") } }
        }
        onDismiss()
    }
    val logs = DebugFlags.get(context, "logs")
    val webDebug = DebugFlags.get(context, "webview_debug")
    val items: List<Pair<String, () -> Unit>> = listOf(
        stringResource(R.string.debug_reload_contacts) to { run { graph.td.send(TdApi.GetContacts()) } },
        stringResource(R.string.debug_reset_imported) to { run { graph.td.send(TdApi.ClearImportedContacts()) } },
        stringResource(R.string.debug_mark_all_read) to {
            run {
                graph.store.snapshot.value.lists.values.flatten().filter { it.unread > 0 && it.lastMessageId != 0L }.distinctBy { it.id }.forEach { row ->
                    runCatching { graph.td.send(TdApi.ViewMessages(row.id, longArrayOf(row.lastMessageId), null, true)) }
                }
            }
        },
        stringResource(if (logs) R.string.debug_disable_logs else R.string.debug_enable_logs) to {
            run {
                val on = DebugFlags.toggle(context, "logs")
                graph.td.send(TdApi.SetLogVerbosityLevel(if (on) 4 else 1))
            }
        },
        stringResource(R.string.debug_clear_cache) to {
            run {
                context.cacheDir.listFiles()?.forEach { if (it.name.startsWith("send-") || it.name.startsWith("edited-") || it.name.startsWith("video-")) it.deleteRecursively() }
            }
        },
        stringResource(R.string.debug_clear_cookies) to { run { android.webkit.CookieManager.getInstance().removeAllCookies(null) } },
        stringResource(if (webDebug) R.string.debug_webview_off else R.string.debug_webview_on) to {
            run { android.webkit.WebView.setWebContentsDebuggingEnabled(DebugFlags.toggle(context, "webview_debug")) }
        },
        stringResource(R.string.debug_reload_config) to { run { graph.td.send(TdApi.GetApplicationConfig()) } },
        stringResource(R.string.debug_optimize_storage) to { run { graph.td.send(TdApi.OptimizeStorage(0, -1, -1, 0, emptyArray(), longArrayOf(), longArrayOf(), false, 0)) } },
    )
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.debug_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            items.forEach { (label, action) ->
                Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().clickable(onClick = action).padding(horizontal = 24.dp, vertical = 14.dp))
            }
        }
    }
}
