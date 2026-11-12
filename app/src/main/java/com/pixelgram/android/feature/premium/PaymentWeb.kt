package com.pixelgram.android.feature.premium

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pixelgram.android.R
import com.pixelgram.android.ui.components.Icon
import org.json.JSONObject

private val WebSchemes = setOf("http", "https")
private val BlockedSchemes = setOf("javascript", "file", "content", "intent")

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PaymentWebDialog(
    url: String,
    title: String,
    onCredentials: (json: String) -> Unit,
    onFinished: () -> Unit,
    onDismiss: () -> Unit,
) {
    val credentials by rememberUpdatedState(onCredentials)
    val finished by rememberUpdatedState(onFinished)
    var loading by remember { mutableStateOf(true) }
    var host by remember(url) { mutableStateOf(Uri.parse(url).host.orEmpty()) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, stringResource(R.string.action_back)) }
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                            Text(host, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
                Box(Modifier.weight(1f)) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                val main = Handler(Looper.getMainLooper())
                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun postEvent(name: String, data: String) {
                                        if (name != "payment_form_submit") return
                                        val json = runCatching { JSONObject(data).getJSONObject("credentials").toString() }.getOrDefault(data)
                                        main.post { credentials(json) }
                                    }
                                }, "TelegramWebviewProxy")
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                        val uri = request.url
                                        if (uri.host == "t.me") {
                                            finished()
                                            return true
                                        }
                                        val scheme = uri.scheme?.lowercase()
                                        if (scheme in BlockedSchemes) return true
                                        if (scheme !in WebSchemes) {
                                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                                            return true
                                        }
                                        host = uri.host.orEmpty()
                                        return false
                                    }

                                    override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                                        loading = true
                                    }

                                    override fun onPageFinished(view: WebView, url: String) {
                                        loading = false
                                    }
                                }
                                loadUrl(url)
                            }
                        },
                        onRelease = { it.destroy() },
                    )
                    if (loading) LinearWavyProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
                }
            }
        }
    }
}
