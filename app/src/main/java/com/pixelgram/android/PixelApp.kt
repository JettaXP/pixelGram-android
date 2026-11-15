package com.pixelgram.android

import android.app.Application
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PixelApp : Application() {
    lateinit var graph: AppGraph
        private set

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    override fun onCreate() {
        androidx.compose.foundation.ComposeFoundationFlags.isNewContextMenuEnabled = false
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                java.io.File(filesDir, CrashFile).writeText(
                    "pixelGram ${BuildConfig.VERSION_NAME} · Android ${android.os.Build.VERSION.RELEASE} · ${android.os.Build.MODEL}\n" + android.util.Log.getStackTraceString(error),
                )
            }
            previous?.uncaughtException(thread, error)
        }
        graph = AppGraph(this)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var started = 0
            override fun onActivityStarted(activity: android.app.Activity) {
                started++
                com.pixelgram.android.notify.AppVisibility.foreground = true
            }
            override fun onActivityStopped(activity: android.app.Activity) {
                started = (started - 1).coerceAtLeast(0)
                com.pixelgram.android.notify.AppVisibility.foreground = started > 0
            }
            override fun onActivityCreated(activity: android.app.Activity, state: android.os.Bundle?) = Unit
            override fun onActivityResumed(activity: android.app.Activity) = Unit
            override fun onActivityPaused(activity: android.app.Activity) = Unit
            override fun onActivitySaveInstanceState(activity: android.app.Activity, state: android.os.Bundle) = Unit
            override fun onActivityDestroyed(activity: android.app.Activity) = Unit
        })
        graph.scope.launch {
            graph.appearance.state.map { it.keepAlive }.distinctUntilChanged().collect { enabled ->
                com.pixelgram.android.notify.KeepAliveService.apply(this@PixelApp, enabled)
            }
        }
    }
}

const val CrashFile = "last_crash.txt"
