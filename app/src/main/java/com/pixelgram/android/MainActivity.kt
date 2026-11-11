package com.pixelgram.android

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
        enableEdgeToEdge()
        setContent {
            val appearance by graph.appearance.state.collectAsState()
            val auth by graph.auth.state.collectAsState()
            PixelTheme(appearance) {
                androidx.compose.runtime.CompositionLocalProvider(LocalGraph provides graph) {
                    if (auth.step == AuthStep.Ready) AppNavigation(graph) else AuthFlow(graph.auth)
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
