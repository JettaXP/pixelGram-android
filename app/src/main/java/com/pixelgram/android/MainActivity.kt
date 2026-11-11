package com.pixelgram.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.ui.unit.dp
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
        enableEdgeToEdge()
        setContent {
            val appearance by graph.appearance.state.collectAsState()
            val auth by graph.auth.state.collectAsState()
            PixelTheme(appearance) {
                androidx.compose.runtime.CompositionLocalProvider(LocalGraph provides graph) {
                    if (auth.step == AuthStep.Ready) AppNavigation(graph) else AuthFlow(graph.auth)
                    CrashReportDialog()
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: android.content.Intent?) {
        if (intent?.action == com.pixelgram.android.music.OpenPlayerAction) {
            com.pixelgram.android.feature.player.FullPlayer.view = com.pixelgram.android.feature.player.PlayerView.Queue
            com.pixelgram.android.feature.player.FullPlayer.open = true
        }
    }
}

@androidx.compose.runtime.Composable
private fun CrashReportDialog() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var report by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(CrashLog.take(context)) }
    val text = report ?: return
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { report = null },
        icon = { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.BugReport, null) },
        title = { androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(R.string.crash_title)) },
        text = {
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(R.string.crash_text))
                androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(12.dp))
                androidx.compose.material3.Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = androidx.compose.ui.Modifier.heightIn(max = 240.dp),
                ) {
                    androidx.compose.material3.Text(
                        text,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        modifier = androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(12.dp),
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, text)
                context.startActivity(android.content.Intent.createChooser(send, null))
                report = null
            }) { androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(R.string.action_share)) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = {
                clipboard.setText(androidx.compose.ui.text.AnnotatedString(text))
                report = null
            }) { androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(R.string.action_copy)) }
        },
    )
}
