package com.pixelgram.android

import android.content.Context
import com.pixelgram.android.settings.AppearanceStore
import com.pixelgram.android.td.TdClient
import com.pixelgram.android.td.TdStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppGraph(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val td = TdClient(context.applicationContext, scope)
    val store = TdStore(td, scope)
    val appearance = AppearanceStore(context.applicationContext, scope)
    val auth = com.pixelgram.android.feature.auth.AuthController(td, scope)
}
