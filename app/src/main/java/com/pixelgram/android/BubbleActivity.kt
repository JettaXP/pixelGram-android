package com.pixelgram.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.pixelgram.android.feature.auth.AuthStep
import com.pixelgram.android.nav.AppNavigation
import com.pixelgram.android.nav.ChatKey
import com.pixelgram.android.ui.theme.PixelTheme

class BubbleActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.pixelgram.android.feature.settings.AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val graph = (application as PixelApp).graph
        val chatId = intent?.getLongExtra(com.pixelgram.android.notify.Notifier.ExtraChat, 0L) ?: 0L
        if (chatId == 0L) {
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            val appearance by graph.appearance.state.collectAsState()
            val auth by graph.auth.state.collectAsState()
            PixelTheme(appearance) {
                androidx.compose.runtime.CompositionLocalProvider(LocalGraph provides graph) {
                    if (auth.step == AuthStep.Ready) AppNavigation(graph, listOf(ChatKey(chatId)))
                }
            }
        }
    }
}
