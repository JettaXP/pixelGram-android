package com.pixelgram.android

import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.pixelgram.android.feature.auth.AuthFlow
import com.pixelgram.android.feature.auth.AuthStep
import com.pixelgram.android.nav.AppNavigation
import com.pixelgram.android.ui.theme.PixelTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.pixelgram.android.feature.settings.AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val graph = (application as PixelApp).graph
        installSplashScreen().setKeepOnScreenCondition {
            graph.auth.state.value.step == AuthStep.Loading
        }
        super.onCreate(savedInstanceState)
        graph.music.cast.init()
        handle(intent)
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {}.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        enableEdgeToEdge()
        setContent {
            val appearance by graph.appearance.state.collectAsState()
            val auth by graph.auth.state.collectAsState()
            PixelTheme(appearance) {
                androidx.compose.runtime.CompositionLocalProvider(LocalGraph provides graph) {
                    if (auth.step == AuthStep.Ready) AppNavigation(graph) else AuthFlow(graph.auth)
                    CrashReport()
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: android.content.Intent?) {
        if (intent?.action == android.content.Intent.ACTION_VIEW) intent.dataString?.let { com.pixelgram.android.nav.PendingNav.external = it }
        if (intent?.action == com.pixelgram.android.notify.Notifier.OpenChatAction) {
            val chatId = intent.getLongExtra(com.pixelgram.android.notify.Notifier.ExtraChat, 0L)
            if (chatId != 0L) com.pixelgram.android.nav.PendingNav.chat = chatId
        }
        if (intent?.action == com.pixelgram.android.music.OpenPlayerAction) {
            com.pixelgram.android.feature.player.FullPlayer.view = com.pixelgram.android.feature.player.PlayerView.Queue
            com.pixelgram.android.feature.player.FullPlayer.open = true
        }
    }
}

@androidx.compose.runtime.Composable
private fun CrashReport() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val file = androidx.compose.runtime.remember { java.io.File(context.filesDir, CrashFile) }
    var text by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(runCatching { file.takeIf { it.isFile }?.readText() }.getOrNull()) }
    val shown = text ?: return
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val close = {
        file.delete()
        text = null
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = close,
        title = { androidx.compose.material3.Text(context.getString(R.string.crash_title)) },
        text = {
            androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.heightIn(max = 320.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                androidx.compose.material3.Text(shown, style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(onClick = {
                clipboard.setText(androidx.compose.ui.text.AnnotatedString(shown))
                close()
            }) { androidx.compose.material3.Text(context.getString(R.string.crash_copy)) }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = close) { androidx.compose.material3.Text(context.getString(R.string.action_close)) } },
    )
}
