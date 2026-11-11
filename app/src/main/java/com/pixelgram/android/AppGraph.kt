package com.pixelgram.android

import android.content.Context
import com.pixelgram.android.settings.AppearanceStore
import com.pixelgram.android.td.TdClient
import com.pixelgram.android.td.TdStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppGraph(context: Context, offline: Boolean = false) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val td = TdClient(context.applicationContext, scope, offline)
    val store = TdStore(td, scope)
    val appearance = AppearanceStore(context.applicationContext, scope)
    val auth = com.pixelgram.android.feature.auth.AuthController(td, scope)
    val remote = com.pixelgram.android.pixel.PixelRemote(context.applicationContext, scope)
    val music = com.pixelgram.android.music.MusicPlayer(context.applicationContext, td)
    val covers = com.pixelgram.android.music.CoverArt(context.applicationContext)
}

val LocalGraph = androidx.compose.runtime.staticCompositionLocalOf<AppGraph> { error("No AppGraph") }
